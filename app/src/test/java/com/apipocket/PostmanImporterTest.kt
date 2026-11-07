package com.apipocket

import com.apipocket.data.model.AuthType
import com.apipocket.data.model.BodyType
import com.apipocket.data.model.HttpMethod
import com.apipocket.data.transfer.PostmanImporter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PostmanImporterTest {

    private val sample = """
        {
          "info": { "name": "Demo", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Users",
              "item": [
                {
                  "name": "Get user",
                  "request": {
                    "method": "GET",
                    "header": [ { "key": "Accept", "value": "application/json" } ],
                    "url": {
                      "raw": "https://api.example.com/users/{{id}}",
                      "query": [ { "key": "page", "value": "1" } ]
                    }
                  }
                },
                {
                  "name": "Create user",
                  "request": {
                    "method": "POST",
                    "url": "https://api.example.com/users",
                    "body": {
                      "mode": "raw",
                      "raw": "{\"name\":\"a\"}",
                      "options": { "raw": { "language": "json" } }
                    },
                    "auth": {
                      "type": "bearer",
                      "bearer": [ { "key": "token", "value": "{{token}}" } ]
                    }
                  }
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun importsFoldersAsCollections() {
        val root = Json.parseToJsonElement(sample).jsonObject
        val result = PostmanImporter().import(root)

        assertEquals(1, result.collections.size)
        assertEquals(2, result.requestCount)
        assertEquals("Users", result.collections.first().name)

        val collection = result.collections.first()
        val getRequest = collection.requests.first { it.name == "Get user" }
        assertEquals(HttpMethod.GET, getRequest.request.method)
        assertEquals("https://api.example.com/users/{{id}}", getRequest.request.url)
        assertEquals(1, getRequest.request.params.size)
        assertEquals("page", getRequest.request.params.first().key)
        assertEquals("Accept", getRequest.request.headers.first().key)

        val postRequest = collection.requests.first { it.name == "Create user" }
        assertEquals(HttpMethod.POST, postRequest.request.method)
        assertEquals(BodyType.JSON, postRequest.request.body.type)
        assertEquals(AuthType.BEARER, postRequest.request.auth.type)
        assertEquals("{{token}}", postRequest.request.auth.token)
        assertNotNull(postRequest.request.body.json)
    }
}
