package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.morphology.Unicode

/**
 * Yoruba marks its tones with the grave and the acute, and ẹ, ọ and ṣ with a dot below; tone tells words apart (ọkọ
 * "husband", ọkọ̀ "vehicle", oko "farm"), so a word keeps its marks. Text is read in NFC, the form the frequency list,
 * the dictionary pack and the courses are written in, whether it came typed with combining marks (e + U+0323 + U+0301)
 * or precomposed: there is no single letter for ẹ́, so it stays ẹ and a combining acute, and the word characters
 * (tools/languages/Yoruba.yaml) take the combining marks inside a word. The vertical line below (U+0329) some older
 * typing puts for the dot is read as the dot.
 *
 * tools/frequency_yoruba.py (yoruba_standard) and tools/check_course.py read words the same way; keep the three the same.
 */
class YorubaParser : SpaceDelimitedParser() {

    override val name: String get() = "Yoruba"

    override fun parse(text: String, language: Language): List<ParsedToken> = super.parse(standard(text), language)

    override fun lowercase(text: String): String = standard(text.lowercase())

    internal companion object {
        private const val LINE_BELOW = '̩'
        private const val DOT_BELOW = '̣'

        fun standard(text: String): String = Unicode.nfc(text.replace(LINE_BELOW, DOT_BELOW))
    }
}
