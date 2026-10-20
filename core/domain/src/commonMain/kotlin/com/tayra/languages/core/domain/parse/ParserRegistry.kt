package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.morphology.ChineseScripts
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.model.Language

/**
 * Registry of the parsers available on this platform, keyed by [Language.parserType].
 */
object ParserRegistry {
    private val parsers: Map<String, TextParser> = linkedMapOf(
        "spacedel" to SpaceDelimitedParser(),
        "turkish" to TurkishParser(),
        "classicalchinese" to ClassicalChineseParser(),
        "thai" to SegmentingParser("Thai", "th", joinUnknown = true),
        "lao" to SegmentingParser("Lao", "lo", joinUnknown = true),
        "khmer" to SegmentingParser("Khmer", "km", joinUnknown = true),
        "mandarin" to SegmentingParser("Mandarin Chinese", "zh", joinUnknown = false),
        "cantonese" to SegmentingParser("Cantonese", "yue", joinUnknown = false),
        "japanese" to JapaneseParser(),
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

/**
 * The form words are matched and saved by: lowercase, and for Chinese read in a script, in simplified
 * characters whichever script it is shown in, so a saved word is the same one in both scripts.
 */
fun Language.lowercase(text: String): String =
    parser.lowercase(text).let { lc -> if (chineseScript != null) ChineseScripts.convert(lc, ChineseScript.SIMPLIFIED) else lc }
