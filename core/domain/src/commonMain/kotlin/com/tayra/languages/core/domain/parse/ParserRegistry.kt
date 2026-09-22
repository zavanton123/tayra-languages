package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language

/**
 * Registry of the parsers available on this platform, keyed by [Language.parserType].
 */
object ParserRegistry {
    private val parsers: Map<String, TextParser> = linkedMapOf(
        "spacedel" to SpaceDelimitedParser(),
        "turkish" to TurkishParser(),
        "classicalchinese" to ClassicalChineseParser(),
    )

    /** Parser types with their display names. */
    val supported: List<Pair<String, String>> get() = parsers.map { (key, parser) -> key to parser.name }

    fun isSupported(parserType: String): Boolean = parserType in parsers

    fun parser(parserType: String): TextParser =
        parsers[parserType] ?: throw IllegalArgumentException("Unsupported parser type '$parserType'")
}

val Language.isSupported: Boolean get() = ParserRegistry.isSupported(parserType)

val Language.parser: TextParser get() = ParserRegistry.parser(parserType)

fun Language.parseTokens(text: String): List<ParsedToken> = parser.parse(text, this).numbered()

fun Language.lowercase(text: String): String = parser.lowercase(text)
