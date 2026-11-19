package com.idiomiko.core.domain.parse

/**
 * Urdu: space-delimited, written in the Persian letters and its own (ٹ ڈ ڑ ں ے), the non-joiner sometimes inside a word.
 * Words are matched in Urdu letters without marks: ك as ک, ي and ى as ی, the Arabic ه and ە as ہ, ۂ and ۀ as ہ, ۓ as ئے,
 * without vowel signs, tatweel or the non-joiner. Unlike Persian, ھ (do-chashmi he, which writes aspiration: بھی, تھا)
 * and ہ are letters of their own, as are ے and ی.
 *
 * tools/frequency_urdu.py folds words the same way (urdu_fold); keep the two the same.
 */
class UrduParser : SpaceDelimitedParser() {

    override val name: String get() = "Urdu"

    override fun lowercase(text: String): String = fold(text).lowercase()

    companion object {
        private const val ZWNJ = '\u200C'

        fun fold(text: String): String {
            val composed = COMPOSED.fold(text) { written, (spread, letter) -> written.replace(spread, letter) }
            val out = StringBuilder(composed.length)
            for (c in composed) {
                when {
                    isMark(c) || c == ZWNJ -> Unit
                    c == '\u0643' -> out.append('\u06A9')
                    c == '\u064A' || c == '\u0649' -> out.append('\u06CC')
                    c == '\u0647' || c == '\u06D5' -> out.append('\u06C1')
                    else -> out.append(c)
                }
            }
            return out.toString()
        }

        private fun isMark(c: Char): Boolean = c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u0640'

        /** A long vowel or hamza seat written as a letter and a combining sign, and the letters with hamza Urdu writes otherwise. */
        private val COMPOSED = listOf(
            "\u0627\u0653" to "\u0622", "\u0627\u0654" to "\u0623", "\u0627\u0655" to "\u0625", "\u0648\u0654" to "\u0624",
            "\u06CC\u0654" to "\u0626", "\u064A\u0654" to "\u0626", "\u0649\u0654" to "\u0626",
            "\u06D2\u0654" to "\u0626\u06D2", "\u06D3" to "\u0626\u06D2",
            "\u06C1\u0654" to "\u06C1", "\u06C2" to "\u06C1", "\u06C0" to "\u06C1",
        )
    }
}
