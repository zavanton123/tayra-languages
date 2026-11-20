package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language

/**
 * Hausa (Boko, the Latin alphabet) has the hooked letters ɓ ɗ ƙ ƴ, which much text writes with an apostrophe: 'b, 'd, k'
 * and 'y ('yan, k'asa, 'dan). Words are matched with the hooked letters, whichever way they are written: 'yan is ƴan, k'asa
 * ƙasa. An apostrophe inside a word is its glottal stop (sa'a, jama'a, Jumma'a) and stays in it; one at a word's edge is a
 * quotation mark and no part of the word, but for the one before b, d or y that writes the hooked letter.
 *
 * tools/build_dictionary.py and tools/frequency_hausa.py fold words the same way (hausa_fold), and tools/check_course.py
 * reads them so; keep them the same.
 */
class HausaParser : SpaceDelimitedParser() {

    override val name: String get() = "Hausa"

    override fun parse(text: String, language: Language): List<ParsedToken> =
        super.parse(text, language).flatMap { if (it.isWord) cut(it.token) else listOf(it) }

    override fun lowercase(text: String): String = fold(text).lowercase()

    /** A run of word characters as its word, with the apostrophes at its edges apart but for one writing a hooked letter. */
    private fun cut(run: String): List<ParsedToken> {
        var start = run.indexOfFirst { it != APOSTROPHE }
        if (start < 0) return listOf(ParsedToken(run, isWord = false))
        if (start > 0 && run[start].lowercaseChar() in HOOKED_AFTER) start--
        val end = run.indexOfLast { it != APOSTROPHE } + 1
        val out = mutableListOf<ParsedToken>()
        if (start > 0) out += ParsedToken(run.substring(0, start), isWord = false)
        out += ParsedToken(run.substring(start, end), isWord = true)
        if (end < run.length) out += ParsedToken(run.substring(end), isWord = false)
        return out
    }

    companion object {
        private const val APOSTROPHE = '\''
        private const val HOOKED_AFTER = "bdy"
        private val APOSTROPHES = Regex("[’‘ʼ´`]")
        private val K_HOOK = Regex("([kK])'(?=\\p{L})")
        private val HOOK_FIRST = Regex("'([bBdDyY])")
        private val HOOKED = mapOf('b' to "ɓ", 'B' to "Ɓ", 'd' to "ɗ", 'D' to "Ɗ", 'y' to "ƴ", 'Y' to "Ƴ")

        /** The word with its hooked letters for the apostrophe spellings: k' as ƙ, 'b as ɓ, 'd as ɗ, 'y as ƴ. */
        fun fold(text: String): String {
            if (text.none { it == APOSTROPHE || it in "’‘ʼ´`" }) return text
            val straight = APOSTROPHES.replace(text, "'")
            val k = K_HOOK.replace(straight) { if (it.groupValues[1] == "k") "ƙ" else "Ƙ" }
            return HOOK_FIRST.replace(k) { HOOKED.getValue(it.groupValues[1][0]) }
        }
    }
}
