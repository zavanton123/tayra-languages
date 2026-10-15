package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.morphology.Morphology

/**
 * Japanese: the longest stretch that is a word of the lexicon, or an inflection of one by
 * Yomitan's Japanese rules ("食べました" for "食べる"), as Yomitan scans Japanese text.
 */
class JapaneseParser : SegmentingParser("Japanese", "ja", joinUnknown = false) {
    /** "食べさせられませんでした" is eight kana longer than "食べる". */
    override val inflectionLength: Int = 10

    override fun matches(text: String, lexicon: Lexicon): Boolean {
        if (text in lexicon) return true
        // Inflections end in kana; a stretch ending in a kanji is a word or nothing.
        if (text.length < 2 || !text.last().isKana()) return false
        val morphology = Morphology.forLanguage("ja") ?: return false
        return morphology.candidates(text).any { it.text in lexicon }
    }

    private fun Char.isKana(): Boolean = this in '぀'..'ヿ'
}
