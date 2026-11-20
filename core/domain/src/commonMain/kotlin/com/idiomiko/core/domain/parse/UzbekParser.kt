package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language

/**
 * Uzbek writes oʻ and gʻ with a turned comma (U+02BB) and the tutuq belgisi (ʼ, U+02BC) between letters (maʼno, sanʼat), but
 * texts often type either as ' ` ´ ‘ or ’. Before the text is cut into words each such mark is written as the one it stands
 * for, so that o'zbek, o‘zbek and oʻzbek are one word: after o or g and before a letter, the turned comma (soʻz, togʻri);
 * after a g ending a word, but for the ng of keling, the turned comma too (bogʻ, togʻ); between two other letters, the tutuq
 * belgisi (ma'no is maʼno). Any other mark is left for a quotation mark. Both letters are word characters, so a word keeps
 * them. Each mark becomes one character, so the text keeps its length.
 *
 * tools/build_frequency_list.py counts words the same way (frequency_uzbek.normalise), and tools/check_course.py reads them
 * so; keep the three the same.
 */
class UzbekParser : SpaceDelimitedParser() {

    override val name: String get() = "Uzbek"

    override fun parse(text: String, language: Language): List<ParsedToken> = super.parse(normalized(text), language)

    override fun lowercase(text: String): String = normalized(text).lowercase()

    companion object {
        const val TURNED_COMMA = 'ʻ'
        const val TUTUQ = 'ʼ'
        private const val MARKS = "'`´‘’ʻʼ"

        /** [text] with each apostrophe-like mark of oʻ, gʻ and the tutuq belgisi written as that letter. */
        fun normalized(text: String): String {
            if (text.none { it in MARKS }) return text
            val out = text.toCharArray()
            for (i in text.indices) {
                if (text[i] !in MARKS || i == 0) continue
                val before = text[i - 1]
                val afterLetter = text.getOrNull(i + 1)?.isLetter() == true
                out[i] = when {
                    before in "oOgG" && afterLetter -> TURNED_COMMA
                    before in "gG" && text.getOrNull(i - 2)?.let { it in "nN" } != true && text.getOrNull(i - 2)?.isLetter() == true ->
                        TURNED_COMMA
                    before.isLetter() && afterLetter && before !in "oOgG" -> TUTUQ
                    else -> text[i]
                }
            }
            return out.concatToString()
        }
    }
}
