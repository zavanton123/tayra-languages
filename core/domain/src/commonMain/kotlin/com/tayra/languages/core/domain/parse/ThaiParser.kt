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
 * Some grammatical words always stand apart, though the lexicon has them joined to the word next to them: the negator
 * ไม่, the particle ก็ and ตอน "at the time of" before a word (ไม่ดี is ไม่ + ดี, ก็ดี ก็ + ดี, ตอนเย็น ตอน + เย็น), and
 * after one the complementizer ว่า, the aspect word อยู่ and the pronouns เรา, ฉัน and เธอ (คิดว่า is คิด + ว่า,
 * อาศัยอยู่ อาศัย + อยู่, ของเรา ของ + เรา). A word of the lexicon that is one of them and words of the lexicon, cut
 * between syllables, is no word: it is read as its words.
 *
 * tools/build_frequency_list.py (thai_break, segmenter) and tools/check_course.py (thai_spans) cut Thai the same way; keep
 * the three the same.
 */
class ThaiParser : SegmentingParser("Thai", "th", joinUnknown = true, byFrequency = true) {

    override fun canBreak(run: String, at: Int): Boolean =
        at <= 0 || at >= run.length ||
            run[at] !in FOLLOWING && run[at - 1] !in LEADING && !(run[at] in LED && run[at - 1] == 'ห' && at >= 2 && run[at - 2] in LEADING)

    override fun standsApart(c: Char): Boolean = c == MAI_YAMOK || c in THAI_DIGITS

    override fun matches(text: String, lexicon: Lexicon): Boolean = text in lexicon && !joinsGrammaticalWord(text, lexicon)

    /** Whether [word] is one of [BEFORE] and words after it, or words and one of [AFTER], cut between syllables. */
    private fun joinsGrammaticalWord(word: String, lexicon: Lexicon): Boolean =
        BEFORE.any { word.length > it.length && word.startsWith(it) && canBreak(word, it.length) && cut(word, it.length, word.length, lexicon) } ||
            AFTER.any { word.length > it.length && word.endsWith(it) && canBreak(word, word.length - it.length) && cut(word, 0, word.length - it.length, lexicon) }

    /** Whether [word] from [from] to [to] can be cut between syllables into words of the lexicon, none a letter alone. */
    private fun cut(word: String, from: Int, to: Int, lexicon: Lexicon): Boolean {
        val reached = BooleanArray(to + 1)
        reached[from] = true
        for (start in from until to) {
            if (!reached[start]) continue
            for (end in start + 1..to) {
                val piece = word.substring(start, end)
                if (canBreak(word, end) && piece in lexicon && (piece in LETTER_WORDS || !LETTER.matches(piece))) reached[end] = true
            }
        }
        return reached[to]
    }

    private companion object {
        /** The vowels and marks written after the letter they go with. */
        const val FOLLOWING = "ะัาำิีึืฺุูๅ็่้๊๋์ํ๎"

        /** The vowels written before the letter they go with. */
        const val LEADING = "เแโใไ"

        /** The letters a silent ห leads, which take its tone. */
        const val LED = "งญนมยรลว"

        /** The grammatical words that join no word after them, and those that join none before them. */
        val BEFORE = listOf("ไม่", "ก็", "ตอน")
        val AFTER = listOf("ว่า", "อยู่", "เรา", "ฉัน", "เธอ")

        /** A letter alone, with its marks: the dictionary has every one, but they are no words, ก็ and ณ aside. */
        val LETTER = Regex("[ก-ฮ][\u0E47-\u0E4D]*")
        val LETTER_WORDS = setOf("ก็", "ณ")

        const val MAI_YAMOK = 'ๆ'
        val THAI_DIGITS = '๐'..'๙'
    }
}
