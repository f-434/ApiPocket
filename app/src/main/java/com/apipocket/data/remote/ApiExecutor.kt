package com.apipocket.data.remote

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.apipocket.data.model.ApiKeyLocation
import com.apipocket.data.model.AuthType
import com.apipocket.data.model.BodyType
import com.apipocket.data.model.HttpMethod
import com.apipocket.data.model.RequestConfig
import com.apipocket.data.variable.VariableResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class ApiExecutor(
    private val context: Context,
    private val client: OkHttpClient,
    private val resolver: VariableResolver
) {
    @Volatile
    private var activeCall: Call? = null

    fun cancelActiveCall() {
        activeCall?.cancel()
    }

    suspend fun execute(
        config: RequestConfig,
        variables: Map<String, String>,
        timeoutSeconds: Int
    ): ApiResult = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        var call: Call? = null
        try {
            val url = buildUrl(config, variables)
            val body = buildBody(config, variables)
            val request = buildRequest(config, variables, url, body)
            call = client.newCall(request)
            activeCall = call
            call.timeout().timeout(timeoutSeconds.coerceIn(1, 300).toLong(), TimeUnit.SECONDS)
            call.execute().use { response ->
                val (bytes, truncated) = readLimited(response.body.byteStream(), MAX_BODY_BYTES)
                val charset = response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val durationMs = (System.nanoTime() - startedAt) / 1_000_000
                ApiResult.Success(
                    ApiResponse(
                        statusCode = response.code,
                        statusMessage = response.message,
                        protocol = response.protocol.name,
                        headers = (0 until response.headers.size).map {
                            response.headers.name(it) to response.headers.value(it)
                        },
                        contentType = response.header("Content-Type"),
                        body = bytes.toString(charset),
                        bodyBytes = bytes.size.toLong(),
                        truncated = truncated,
                        durationMs = durationMs
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiExecutionException) {
            ApiResult.Failure(e.error, elapsedMs(startedAt))
        } catch (e: UnknownHostException) {
            ApiResult.Failure(ApiError.Dns, elapsedMs(startedAt))
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiError.Timeout, elapsedMs(startedAt))
        } catch (e: ConnectException) {
            ApiResult.Failure(ApiError.Connect, elapsedMs(startedAt))
        } catch (e: SSLException) {
            ApiResult.Failure(ApiError.Ssl, elapsedMs(startedAt))
        } catch (e: IllegalArgumentException) {
            ApiResult.Failure(ApiError.InvalidHeader(e.message ?: ""), elapsedMs(startedAt))
        } catch (e: IOException) {
            val error = if (e.message?.contains("Canceled", ignoreCase = true) == true) {
                ApiError.Cancelled
            } else {
                ApiError.Unknown(e.message)
            }
            ApiResult.Failure(error, elapsedMs(startedAt))
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message), elapsedMs(startedAt))
        } finally {
            activeCall = null
        }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun buildUrl(config: RequestConfig, variables: Map<String, String>): HttpUrl {
        val raw = resolver.resolve(config.url.trim(), variables)
        if (raw.isBlank()) throw ApiExecutionException(ApiError.InvalidUrl)
        val withScheme = if (raw.contains("://")) raw else "http://$raw"
        val parsed = withScheme.toHttpUrlOrNull() ?: throw ApiExecutionException(ApiError.InvalidUrl)
        val builder = parsed.newBuilder()
        config.params.filter { it.enabled && it.key.isNotBlank() }.forEach { param ->
            builder.addQueryParameter(
                resolver.resolve(param.key, variables),
                resolver.resolve(param.value, variables)
            )
        }
        val auth = config.auth
        if (auth.type == AuthType.API_KEY && auth.keyLocation == ApiKeyLocation.QUERY) {
            val name = resolver.resolve(auth.keyName, variables)
            if (name.isNotBlank()) {
                builder.addQueryParameter(name, resolver.resolve(auth.keyValue, variables))
            }
        }
        return builder.build()
    }

    private fun buildRequest(
        config: RequestConfig,
        variables: Map<String, String>,
        url: HttpUrl,
        body: RequestBody?
    ): Request {
        val builder = Request.Builder().url(url)
        val explicitHeaders = mutableSetOf<String>()
        config.headers.filter { it.enabled && it.key.isNotBlank() }.forEach { header ->
            val name = resolver.resolve(header.key, variables).trim()
            val value = resolver.resolve(header.value, variables)
            explicitHeaders += name.lowercase()
            builder.addHeader(name, value)
        }

        val auth = config.auth
        when (auth.type) {
            AuthType.NONE -> Unit
            AuthType.BEARER -> {
                val token = resolver.resolve(auth.token, variables)
                if (token.isNotBlank() && "authorization" !in explicitHeaders) {
                    builder.header("Authorization", "Bearer $token")
                }
            }
            AuthType.BASIC -> {
                if ("authorization" !in explicitHeaders) {
                    val credentials = "${resolver.resolve(auth.username, variables)}:${resolver.resolve(auth.password, variables)}"
                    val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                    builder.header("Authorization", "Basic $encoded")
                }
            }
            AuthType.API_KEY -> {
                if (auth.keyLocation == ApiKeyLocation.HEADER) {
                    val name = resolver.resolve(auth.keyName, variables)
                    if (name.isNotBlank() && name.lowercase() !in explicitHeaders) {
                        builder.header(name, resolver.resolve(auth.keyValue, variables))
                    }
                }
            }
        }

        val method = config.method
        val requestBody = when (method) {
            HttpMethod.GET, HttpMethod.HEAD -> null
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH ->
                body ?: ByteArray(0).toRequestBody(null)
            HttpMethod.DELETE, HttpMethod.OPTIONS -> body
        }
        return builder.method(method.name, requestBody).build()
    }

    private fun buildBody(config: RequestConfig, variables: Map<String, String>): RequestBody? {
        return when (config.body.type) {
            BodyType.NONE -> null
            BodyType.JSON -> resolver.resolve(config.body.json, variables)
                .toRequestBody(JSON_MEDIA_TYPE)
            BodyType.RAW -> {
                val contentType = config.body.rawContentType.ifBlank { "text/plain" }
                resolver.resolve(config.body.raw, variables)
                    .toRequestBody(contentType.toMediaTypeOrNull() ?: TEXT_MEDIA_TYPE)
            }
            BodyType.X_WWW_FORM_URLENCODED -> {
                val form = FormBody.Builder()
                config.body.urlEncoded.filter { it.enabled && it.key.isNotBlank() }.forEach { field ->
                    form.add(
                        resolver.resolve(field.key, variables),
                        resolver.resolve(field.value, variables)
                    )
                }
                form.build()
            }
            BodyType.FORM_DATA -> {
                val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
                config.body.formData.filter { it.enabled && it.key.isNotBlank() }.forEach { field ->
                    val name = resolver.resolve(field.key, variables)
                    if (field.isFile) {
                        val uriString = field.uri
                        if (uriString.isNullOrBlank()) {
                            throw ApiExecutionException(ApiError.FileAccess(field.fileName))
                        }
                        val uri = Uri.parse(uriString)
                        ensureReadable(uri, field.fileName)
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileName = field.fileName ?: displayName(uri) ?: "file"
                        multipart.addFormDataPart(
                            name,
                            fileName,
                            ContentUriRequestBody(context.contentResolver, uri, mime.toMediaType(), queryFileSize(uri))
                        )
                    } else {
                        multipart.addFormDataPart(name, resolver.resolve(field.value, variables))
                    }
                }
                multipart.build()
            }
        }
    }

    private fun ensureReadable(uri: Uri, fileName: String?) {
        try {
            context.contentResolver.openInputStream(uri)?.use { } ?: throw IOException("No stream")
        } catch (e: Exception) {
            throw ApiExecutionException(ApiError.FileAccess(fileName))
        }
    }

    private fun displayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }

    private fun queryFileSize(uri: Uri): Long {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L }
        }.getOrNull() ?: -1L
    }

    private fun readLimited(stream: InputStream?, maxBytes: Long): Pair<ByteArray, Boolean> {
        if (stream == null) return ByteArray(0) to false
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        var truncated = false
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            if (total + read > maxBytes) {
                val remaining = (maxBytes - total).toInt()
                if (remaining > 0) output.write(buffer, 0, remaining)
                truncated = true
                break
            }
            output.write(buffer, 0, read)
            total += read
        }
        return output.toByteArray() to truncated
    }

    private class ContentUriRequestBody(
        private val contentResolver: ContentResolver,
        private val uri: Uri,
        private val mediaType: MediaType,
        private val size: Long
    ) : RequestBody() {
        override fun contentType(): MediaType = mediaType
        override fun contentLength(): Long = size
        override fun writeTo(sink: BufferedSink) {
            val input = contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
            input.use { sink.writeAll(it.source()) }
        }
    }

    private class ApiExecutionException(val error: ApiError) : Exception()

    private companion object {
        const val MAX_BODY_BYTES = 5L * 1024 * 1024
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val TEXT_MEDIA_TYPE = "text/plain".toMediaType()
    }
}
