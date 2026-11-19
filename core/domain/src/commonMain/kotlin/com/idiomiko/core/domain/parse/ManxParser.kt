package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language

/**
 * Manx writes a short word onto the next with an apostrophe (t'eh "is he", v'ad, s'mie, dt'ennym) and 'n "the" and 's
 * "and" onto the word before (ta'n, jeh'n, chammah's): each is a word of its own. An apostrophe at a word's edge is a
 * quotation mark, or stands for the a- of ayns ('sy, 'syn "in the"), and no part of the word; nor is the h-, n- or t- a
 * mutation puts before a vowel (ny h-ellanyn, yn t-ushtey).
 *
 * tools/build_frequency_list.py counts words the same way (manx_words) and tools/check_course.py reads them so (manx_tokens);
 * keep the three the same.
 */
class ManxParser : SpaceDelimitedParser() {

    override val name: String get() = "Manx"

    override fun parse(text: String, language: Language): List<ParsedToken> {
        val runs = super.parse(text, language)
        return runs.flatMapIndexed { i, token ->
            when {
                !token.isWord -> listOf(token)
                isMutation(token, runs.getOrNull(i + 1), runs.getOrNull(i + 2)) -> listOf(token.copy(isWord = false))
                else -> cut(token.token)
            }
        }
    }

    private fun isMutation(token: ParsedToken, hyphen: ParsedToken?, next: ParsedToken?): Boolean =
        token.token.lowercase() in MUTATIONS && hyphen?.token == "-" && next != null && next.isWord && next.token.first().lowercaseChar() in VOWELS

    /** A run of word characters as words, with the apostrophes at its edges and at its pieces' edges apart. */
    private fun cut(run: String): List<ParsedToken> {
        val start = run.indexOfFirst { it != APOSTROPHE }
        if (start < 0) return listOf(ParsedToken(run, isWord = false))
        val end = run.indexOfLast { it != APOSTROPHE } + 1
        val out = mutableListOf<ParsedToken>()
        if (start > 0) out += ParsedToken(run.substring(0, start), isWord = false)
        out += pieces(run.substring(start, end))
        if (end < run.length) out += ParsedToken(run.substring(end), isWord = false)
        return out
    }

    private fun pieces(word: String): List<ParsedToken> {
        val lower = word.lowercase()
        PREFIXES.firstOrNull { lower.startsWith(it) && word.length > it.length }?.let {
            return listOf(ParsedToken(word.substring(0, it.length), isWord = true)) + cut(word.substring(it.length))
        }
        SUFFIXES.firstOrNull { lower.endsWith(it) && word.length > it.length }?.let {
            return cut(word.dropLast(it.length)) + ParsedToken(word.takeLast(it.length), isWord = true)
        }
        return listOf(ParsedToken(word, isWord = true))
    }

    private companion object {
        const val APOSTROPHE = '\''
        val PREFIXES = listOf("dt'", "t'", "v'", "s'", "b'", "d'", "m'")
        val SUFFIXES = listOf("'n", "'s")
        val MUTATIONS = setOf("h", "n", "t")
        const val VOWELS = "aeiouyàáâèéêìíîòóôùúû"
    }
}
