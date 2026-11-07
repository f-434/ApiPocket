package com.apipocket.data.variable

import com.apipocket.data.model.ApiEnvironment

/**
 * Resolves `{{variable}}` placeholders using the variables of the active environment.
 * Unresolved placeholders are left untouched.
 */
class VariableResolver {

    private val pattern = Regex("""\{\{\s*([\w.\-]+)\s*\}\}""")

    fun resolve(text: String, variables: Map<String, String>): String {
        if (text.isEmpty() || variables.isEmpty() || !text.contains("{{")) return text
        return pattern.replace(text) { match ->
            variables[match.groupValues[1]] ?: match.value
        }
    }

    /** Returns placeholder names in [text] that are not present in [variables]. */
    fun unresolved(text: String, variables: Map<String, String>): Set<String> {
        if (text.isEmpty() || !text.contains("{{")) return emptySet()
        return pattern.findAll(text)
            .map { it.groupValues[1] }
            .filter { it !in variables }
            .toSet()
    }

    fun variablesOf(environment: ApiEnvironment?): Map<String, String> {
        if (environment == null) return emptyMap()
        return environment.variables
            .filter { it.enabled && it.key.isNotBlank() }
            .associate { it.key.trim() to it.value }
    }
}
