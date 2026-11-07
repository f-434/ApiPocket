package com.apipocket

import com.apipocket.util.JsonFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun prettyPrintsValidJson() {
        val result = JsonFormatter.pretty("""{"a":1,"b":[true,null]}""")
        assertTrue(result.isSuccess)
        val pretty = result.getOrThrow()
        assertTrue(pretty.contains("\n"))
        assertTrue(pretty.contains("\"a\""))
        assertTrue(pretty.contains("true"))
    }

    @Test
    fun invalidJsonFails() {
        assertTrue(JsonFormatter.pretty("{oops").isFailure)
        assertFalse(JsonFormatter.isValid("{oops"))
    }

    @Test
    fun detectsJsonContent() {
        assertTrue(JsonFormatter.looksLikeJson("{\"a\":1}", "application/json"))
        assertTrue(JsonFormatter.looksLikeJson("  [1,2]", null))
        assertFalse(JsonFormatter.looksLikeJson("plain text", "text/plain"))
    }
}
