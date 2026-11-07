package com.apipocket

import com.apipocket.data.model.ApiEnvironment
import com.apipocket.data.model.KeyValue
import com.apipocket.data.variable.VariableResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VariableResolverTest {

    private val resolver = VariableResolver()

    @Test
    fun resolvesKnownVariables() {
        val result = resolver.resolve(
            "https://{{host}}/users/{{ id }}",
            mapOf("host" to "api.example.com", "id" to "42")
        )
        assertEquals("https://api.example.com/users/42", result)
    }

    @Test
    fun leavesUnknownVariablesUntouched() {
        val result = resolver.resolve("{{base}}/{{missing}}", mapOf("base" to "https://dev"))
        assertEquals("https://dev/{{missing}}", result)
    }

    @Test
    fun reportsUnresolvedNames() {
        val unresolved = resolver.unresolved(
            "{{a}} {{b}} {{a}}",
            mapOf("a" to "1")
        )
        assertEquals(setOf("b"), unresolved)
    }

    @Test
    fun variablesOfFiltersDisabledAndBlankKeys() {
        val environment = ApiEnvironment(
            name = "dev",
            variables = listOf(
                KeyValue(key = "host", value = "dev.example.com", enabled = true),
                KeyValue(key = "disabled", value = "x", enabled = false),
                KeyValue(key = "  ", value = "y", enabled = true),
                KeyValue(key = "token", value = "abc", enabled = true)
            )
        )
        val variables = resolver.variablesOf(environment)
        assertEquals("dev.example.com", variables["host"])
        assertEquals("abc", variables["token"])
        assertTrue("disabled" !in variables)
    }

    @Test
    fun nullEnvironmentProducesEmptyMap() {
        assertTrue(resolver.variablesOf(null).isEmpty())
    }
}
