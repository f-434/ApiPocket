package com.apipocket.di

import android.content.Context
import com.apipocket.data.local.AppRepository
import com.apipocket.data.local.appDataStore
import com.apipocket.data.remote.ApiExecutor
import com.apipocket.data.transfer.TransferService
import com.apipocket.data.variable.VariableResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val variableResolver = VariableResolver()

    val repository = AppRepository(appContext.appDataStore, scope)

    val executor = ApiExecutor(appContext, okHttpClient, variableResolver)

    val transferService = TransferService(appContext)
}
