package com.idiomiko.core.domain.parse

/**
 * Pashto: space-delimited, written in the Arabic letters with its own (ټ ډ ړ ږ ښ ګ ڼ ځ څ ې ۍ ئ), as Afghan texts spell it
 * and as Pakistani and older ones do. Words are matched with the spellings of one letter folded together: ك as ک, گ as ګ,
 * the other forms of he (ۀ ە ہ ھ) as ه, Urdu's ٹ ڈ ڑ as ټ ډ ړ, and all its yes (ی ى ې ے ۍ ئ ۓ) as ي, so چې, چي and چی are
 * one word, as are ډېر and ډير or سړی and سړي; without vowel signs, tatweel or the non-joiner. ښ, ږ, ځ, څ and ڼ stay apart
 * from ش, ژ, ز, ص and ن.
 *
 * tools/frequency_pashto.py folds words the same way (pashto_fold); keep the two the same.
 */
class PashtoParser : SpaceDelimitedParser() {

    override val name: String get() = "Pashto"

    override fun lowercase(text: String): String = fold(text).lowercase()

    companion object {
        private const val YE = '\u064A'

        fun fold(text: String): String {
            val composed = COMPOSED.fold(text) { written, (spread, letter) -> written.replace(spread, letter) }
            val out = StringBuilder(composed.length)
            for (c in composed) {
                if (isMark(c)) continue
                out.append(LETTERS[c] ?: c)
            }
            return out.toString()
        }

        private fun isMark(c: Char): Boolean = c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u0640' || c == '\u200C'

        /** A long vowel or hamza seat written as a letter and a combining sign. */
        private val COMPOSED = listOf(
            "\u0627\u0653" to "\u0622", "\u0627\u0654" to "\u0623", "\u0627\u0655" to "\u0625", "\u0648\u0654" to "\u0624",
        )

        private val LETTERS = mapOf(
            '\u0643' to '\u06A9', '\u06AF' to '\u06AB',
            '\u06C0' to '\u0647', '\u06D5' to '\u0647', '\u06C1' to '\u0647', '\u06BE' to '\u0647',
            '\u0679' to '\u067C', '\u0688' to '\u0689', '\u0691' to '\u0693',
            '\u06CC' to YE, '\u0649' to YE, '\u06D0' to YE, '\u06D2' to YE, '\u06CD' to YE, '\u0626' to YE, '\u06D3' to YE,
        )
    }
}
