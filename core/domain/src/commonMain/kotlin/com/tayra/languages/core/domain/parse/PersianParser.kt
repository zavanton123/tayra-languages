package com.tayra.languages.core.domain.parse

/**
 * Persian: space-delimited, with the zero-width non-joiner inside a word (می‌روم and کتاب‌ها are one word each; the
 * language's word characters hold it). Words are matched in Persian letters, ك as ک and ي or ى as ی, and without marks
 * (واقعاً as واقعا, خانهٔ as خانه), so a word has one status however a text writes it. Enclitics stay on their word.
 *
 * tools/frequency_persian.py folds words the same way (persian_fold); keep the two the same.
 */
class PersianParser : SpaceDelimitedParser() {

    override val name: String get() = "Persian"

    override fun lowercase(text: String): String = fold(text).lowercase()

    companion object {
        const val ZWNJ = '‌'

        fun fold(text: String): String {
            val composed = COMPOSED.fold(text) { written, (spread, letter) -> written.replace(spread, letter) }
            val out = StringBuilder(composed.length)
            for (c in composed) {
                when {
                    isMark(c) -> Unit
                    c == 'ك' -> out.append('ک')
                    c == 'ي' || c == 'ى' -> out.append('ی')
                    c == 'ۀ' || c == 'ھ' || c == 'ە' -> out.append('ه')
                    else -> out.append(c)
                }
            }
            return out.toString().trim(ZWNJ)
        }

        /** [fold], and without the non-joiner, as the dictionary packs' forms tables write a word. */
        fun packForm(text: String): String = fold(text).replace(ZWNJ.toString(), "")

        private fun isMark(c: Char): Boolean = c in 'ً'..'ٟ' || c == 'ٰ' || c == 'ـ'

        /** A long vowel or hamza seat written as a letter and a combining sign. */
        private val COMPOSED = listOf(
            "آ" to "آ", "أ" to "أ", "إ" to "إ", "ؤ" to "ؤ", "یٔ" to "ئ", "ئ" to "ئ", "ىٔ" to "ئ",
        )
    }
}
