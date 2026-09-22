package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language

/**
 * Splits text into tokens according to language rules.
 */
interface TextParser {
    /** Parser name for display in the UI. */
    val name: String

    fun parse(text: String, language: Language): List<ParsedToken>

    /** Pronunciation of the text, when the parser can derive it. */
    fun reading(text: String): String? = null

    /**
     * Lowercases the text. Most languages can use the built-in operation,
     * but some (like Turkish) need special handling.
     */
    fun lowercase(text: String): String = text.lowercase()
}

/** Applies the language's `from=to|from=to` character substitutions. */
internal fun applyCharacterSubstitutions(text: String, substitutions: String): String {
    var result = text
    substitutions.split("|").forEach { replacement ->
        val parts = replacement.trim().split("=")
        if (parts.size >= 2) {
            val from = parts[0].trim()
            val to = parts[1].trim()
            if (from.isNotEmpty()) result = result.replace(from, to)
        }
    }
    return result
}

/**
 * Old Lute (php) word character patterns could contain `\x{0600}`; convert
 * these to the `\u0600` form understood by all Kotlin regex engines.
 */
internal fun normalizeRegexEscapes(pattern: String): String =
    Regex("""\\x\{([0-9A-Fa-f]+)\}""").replace(pattern) { match ->
        val hex = match.groupValues[1].padStart(4, '0')
        "\\u$hex"
    }

/** Escapes a character for use inside a regex character class, portably across engines. */
internal fun escapeForCharacterClass(text: String): String = buildString {
    for (ch in text) {
        if (ch in "\\]^-[/") append('\\')
        append(ch)
    }
}
