package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.morphology.Unicode

/**
 * Ancient Greek: a word is matched and saved ([lowercase]) with the accent it has standing alone. The grave a final
 * syllable takes before another word (καλὸς ἀνήρ) is the acute (καλός), and the second accent an enclitic throws back
 * onto the word before it (ἄνθρωπός τις, εἶπέν μοι) is left out (ἄνθρωπος, εἶπεν), so each word has one status however
 * the sentence accents it. Breathings, the circumflex, the iota subscript and the diaeresis stay: they tell words apart
 * (ἤ, ἡ, ᾗ).
 *
 * tools/build_frequency_list.py (greek_folded) and tools/check_course.py fold words the same way; keep the three the same.
 */
class AncientGreekParser : SpaceDelimitedParser() {

    override val name: String get() = "Ancient Greek"

    override fun lowercase(text: String): String = folded(text.lowercase())

    internal companion object {
        private const val GRAVE = '\u0300'
        private const val ACUTE = '\u0301'
        private const val CIRCUMFLEX = '\u0342'

        /** [text] with each word's grave as the acute and without an enclitic's second accent; a phrase word by word. */
        fun folded(text: String): String {
            val decomposed = Unicode.nfd(text)
            val out = StringBuilder(decomposed.length)
            var start = 0
            while (start < decomposed.length) {
                var end = start
                while (end < decomposed.length && isWordChar(decomposed[end])) end++
                if (end == start) {
                    out.append(decomposed[start])
                    start++
                } else {
                    out.append(foldedWord(decomposed.substring(start, end)))
                    start = end
                }
            }
            return Unicode.nfc(out.toString())
        }

        private fun isWordChar(c: Char): Boolean = c.isLetter() || c in '\u0300'..'\u036F'

        private fun foldedWord(word: String): String {
            val acute = word.replace(GRAVE, ACUTE)
            val accents = acute.count { it == ACUTE || it == CIRCUMFLEX }
            val last = acute.indexOfLast { it == ACUTE || it == CIRCUMFLEX }
            return if (accents > 1 && acute[last] == ACUTE) acute.removeRange(last, last + 1) else acute
        }
    }
}
