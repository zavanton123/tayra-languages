package com.idiomiko.core.domain.parse

/**
 * Khmer: a stretch of letters cut into its likeliest words by how common they are, as [LaoParser] cuts Lao (ឆ្កែបាត់ is ឆ្កែ
 * "dog" + បាត់ "lost", not the longest word first, ឆ្កែបា + ត់), but never inside a syllable.
 * A word does not end before a vowel sign or another sign written on its letter (U+17B6–U+17D3, U+17DD: កម្រិត is not កម
 * "scabies" + ្រិត), nor between a coeng (U+17D2) and the consonant it writes under the letter before. A consonant alone is
 * no word, though the dictionary has each letter: letters no word covers go a syllable at a time and are one word together.
 * ។ ៕ ៖, ៗ (which repeats the word before it), the other signs U+17D4–U+17DB and the Khmer digits are no words, so ។ and ៕
 * end a sentence.
 *
 * Subscript ដ and ត (្ដ, ្ត) look alike and are written either way (សប្ដាហ៍, សប្តាហ៍): a word is matched and saved with ្ត
 * ([lowercase]), and the lexicon has its words so ([lexiconWord]). So is a subscript រ, written before or after the subscript
 * it goes with (សាស្រ្ត, សាស្ត្រ): it is matched and saved after it, as the dictionaries write it.
 *
 * tools/build_frequency_list.py (khmer_break, khmer_word, segmenter) and tools/check_course.py (khmer_spans) cut Khmer the
 * same way; keep the three the same.
 */
class KhmerParser : SegmentingParser("Khmer", "km", joinUnknown = true, byFrequency = true) {

    override fun canBreak(run: String, at: Int): Boolean =
        at <= 0 || at >= run.length || !isSign(run[at]) && run[at - 1] != COENG

    override fun standsApart(c: Char): Boolean = c in APART || c in DIGITS || c in LEK_ATTAK

    override fun lowercase(text: String): String = fold(text.lowercase())

    override fun lexiconWord(word: String): String = fold(word)

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

        /** A subscript រ written before the subscript after it, which looks the same. */
        private val RO_FIRST = Regex("\u17D2\u179A\u17D2([\u1780-\u1799\u179B-\u17A2])")

        /** [text] with each subscript ដ written as the subscript ត it looks like, and each subscript រ after the subscript beside it. */
        fun fold(text: String): String = RO_FIRST.replace(text.replace(SUBSCRIPT_DA, SUBSCRIPT_TA), "\u17D2$1\u17D2\u179A")

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
