package com.tayra.languages.core.domain.morphology

/** A language as Yomitan describes it: the ids of the [TextProcessors] run on a word before and after deinflecting it. */
internal data class YomitanLanguage(val code: String, val name: String, val preprocessors: List<String>, val postprocessors: List<String>)

/**
 * Yomitan's text processors, by id: each gives the spellings a word may be looked up under, the
 * word itself first (a text with "ё" is also tried with "е", a capitalised one in lowercase).
 * Ported from Yomitan's ext/js/language (GPL-3.0, Copyright (C) 2024-2026 Yomitan Authors).
 */
internal object TextProcessors {
    private val combiningMarks = Regex("[\u0300-\u036f]")

    val byId: Map<String, (String) -> List<String>> = mapOf(
        "decapitalize" to { s -> listOf(s, s.lowercase()) },
        "capitalizeFirstLetter" to { s -> listOf(s, s.take(1).uppercase() + s.drop(1)) },
        "removeAlphabeticDiacritics" to { s -> listOf(s, Unicode.nfd(s).replace(combiningMarks, "")) },
        // German
        "eszettPreprocessor" to { s -> listOf(s, s.replace("ẞ", "SS").replace("ß", "ss"), s.replace("SS", "ẞ").replace("ss", "ß")) },
        // French
        "apostropheVariants" to { s -> listOf(s, s.replace('\'', '\u2019'), s.replace('\u2019', '\'')) },
        // Modern Greek
        "removeDoubleAcuteAccents" to { s -> listOf(s, removeDoubleAcuteAccents(s)) },
        // Latin
        "processDiphtongs" to { s ->
            listOf(
                s,
                s.replace("æ", "ae").replace("Æ", "AE").replace("œ", "oe").replace("Œ", "OE"),
                s.replace("ae", "æ").replace("AE", "Æ").replace("oe", "œ").replace("OE", "Œ"),
            )
        },
    )

    /** Keeps the first acute accent of a word and drops the others ("πρόσωπό" to "πρόσωπο"). */
    internal fun removeDoubleAcuteAccents(word: String): String {
        val decomposed = Unicode.nfd(word)
        val first = decomposed.indexOf('\u0301')
        return Unicode.nfc(decomposed.filterIndexed { i, c -> c != '\u0301' || i == first })
    }
}
