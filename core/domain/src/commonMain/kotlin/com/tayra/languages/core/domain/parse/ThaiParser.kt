package com.tayra.languages.core.domain.parse

/**
 * Thai: a stretch of letters cut into its likeliest words by how common they are, as [SegmentingParser] cuts Chinese
 * (มีมติ is มี + มติ "has a resolution", not มีม "meme" + ติ), or into the longest the lexicon knows from the left
 * while it has no frequency list, but never inside a syllable. A word does not end before a vowel or tone mark written
 * after its letter (ต่อมา is ต่อ + มา, not ต่อม "gland" + า; มีน้ำ is มี + น้ำ), nor after a vowel written before one
 * (เ, แ, โ, ใ, ไ), nor between such a vowel's silent ห and the letter it leads (เท่าไหร่ ends in ไหร่, not ไห "jar" + ร่);
 * letters no word covers go a syllable at a time, together one word. ๆ, which repeats the word before it, and the Thai
 * digits are no words.
 *
 * tools/build_frequency_list.py and tools/check_course.py cut Thai the same way (thai_segmenter); keep the three the same.
 */
class ThaiParser : SegmentingParser("Thai", "th", joinUnknown = true, byFrequency = true) {

    override fun canBreak(run: String, at: Int): Boolean =
        at <= 0 || at >= run.length ||
            run[at] !in FOLLOWING && run[at - 1] !in LEADING && !(run[at] in LED && run[at - 1] == 'ห' && at >= 2 && run[at - 2] in LEADING)

    override fun standsApart(c: Char): Boolean = c == MAI_YAMOK || c in THAI_DIGITS

    private companion object {
        /** The vowels and marks written after the letter they go with. */
        const val FOLLOWING = "ะัาำิีึืฺุูๅ็่้๊๋์ํ๎"

        /** The vowels written before the letter they go with. */
        const val LEADING = "เแโใไ"

        /** The letters a silent ห leads, which take its tone. */
        const val LED = "งญนมยรลว"

        const val MAI_YAMOK = 'ๆ'
        val THAI_DIGITS = '๐'..'๙'
    }
}
