package com.tayra.languages.core.domain.parse

/**
 * Khmer: a stretch of letters cut into the longest words the lexicon knows from the left, as [SegmentingParser] cuts it,
 * but never inside a syllable. A word does not end before a vowel sign or another sign written on its letter
 * (U+17B6–U+17D3, U+17DD: កម្រិត is not កម "scabies" + ្រិត), nor between a coeng (U+17D2) and the consonant it writes
 * under the letter before. A consonant alone is no word, though the dictionary has each letter: letters no word covers
 * go a syllable at a time and are one word together. ។ ៕ ៖, ៗ (which repeats the word before it), the other signs
 * U+17D4–U+17DB and the Khmer digits are no words, so ។ and ៕ end a sentence.
 *
 * Subscript ដ and ត (្ដ, ្ត) look alike and are written either way (សប្ដាហ៍, សប្តាហ៍): a word is matched and saved with ្ត
 * ([lowercase]), and found in the lexicon in either spelling.
 *
 * tools/build_frequency_list.py (khmer_break, khmer_word) and tools/check_course.py (khmer_spans) cut Khmer the same
 * way; keep the three the same.
 */
class KhmerParser : SegmentingParser("Khmer", "km", joinUnknown = true) {

    override fun canBreak(run: String, at: Int): Boolean =
        at <= 0 || at >= run.length || !isSign(run[at]) && run[at - 1] != COENG

    override fun standsApart(c: Char): Boolean = c in APART || c in DIGITS || c in LEK_ATTAK

    override fun lowercase(text: String): String = fold(text.lowercase())

    override fun matches(text: String, lexicon: Lexicon): Boolean =
        !(text.length == 1 && text[0] in CONSONANTS) && spellings(text).any { it in lexicon }

    companion object {
        private const val COENG = '្'
        private const val SUBSCRIPT_DA = "្ដ"
        private const val SUBSCRIPT_TA = "្ត"
        private val CONSONANTS = 'ក'..'អ'
        private val APART = '។'..'៛'
        private val DIGITS = '០'..'៩'
        private val LEK_ATTAK = '៰'..'៹'

        /** Whether [c] is a vowel sign or another sign written on the letter before it, the coeng among them. */
        fun isSign(c: Char): Boolean = c in 'ា'..'៓' || c == '៝'

        /** [text] with each subscript ដ written as the subscript ត it looks like. */
        fun fold(text: String): String = text.replace(SUBSCRIPT_DA, SUBSCRIPT_TA)

        /** The spellings of [text] with each subscript ត or ដ written either way, [text] first. */
        fun spellings(text: String): List<String> {
            val folded = fold(text)
            var out = listOf("")
            var i = 0
            while (i < folded.length) {
                out = if (folded.startsWith(SUBSCRIPT_TA, i)) {
                    i += 2
                    out.flatMap { listOf(it + SUBSCRIPT_TA, it + SUBSCRIPT_DA) }
                } else {
                    val c = folded[i++]
                    out.map { it + c }
                }
            }
            return (listOf(text) + out).distinct()
        }
    }
}
