package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.model.Language

/**
 * Burmese: a stretch of letters cut into its likeliest words by how common they are, as [LaoParser] cuts Lao (spaces in
 * Burmese part phrases, not words), but never inside a syllable. A word does not end before a vowel sign, medial, tone
 * mark, asat or virama written on its letter (U+102B–U+103F and the other Myanmar signs: ကျောင်း is no က + ျောင်း), nor
 * after a virama, whose consonant belongs to the syllable before it (ကမ္ဘာ), nor before a consonant that an asat or a
 * virama after it closes (တင် is no တ + င်, and the kinzi of မင်္ဂလာ stays with မ). The Myanmar digits stand apart; ၊ and ။
 * are punctuation, ။ ending a sentence, and ၌ ၍ ၎ ၏ letters.
 *
 * Text is read Unicode-normalised ([normalise]): dot below before asat, as NFC orders them, ဥ + ီ as ဦ, and ၀ (zero)
 * between letters as the ဝ (wa) it is mistyped for (၀င် is ဝင် "enter"); the lexicon's words are spelled so too
 * ([lexiconWord]). Text in Zawgyi, the older font encoding, is not Unicode Burmese and is not read as Burmese words.
 *
 * tools/frequency_burmese.py (my_break, my_normalise), tools/build_frequency_list.py (segmenter) and tools/check_course.py
 * (burmese_spans) cut Burmese the same way; keep them the same.
 */
class BurmeseParser : SegmentingParser("Burmese", "my", joinUnknown = true, byFrequency = true) {

    override fun parse(text: String, language: Language): List<ParsedToken> = super.parse(normalise(text), language)

    override fun lowercase(text: String): String = normalise(text).lowercase()

    override fun lexiconWord(word: String): String = normalise(word)

    override fun canBreak(run: String, at: Int): Boolean {
        if (at <= 0 || at >= run.length) return true
        if (isSign(run[at]) || run[at - 1] == VIRAMA) return false
        var after = at + 1
        if (after < run.length && run[after] == DOT_BELOW) after++
        return !(after < run.length && (run[after] == ASAT || run[after] == VIRAMA))
    }

    override fun standsApart(c: Char): Boolean = c in DIGITS

    companion object {
        private const val ASAT = '\u103A'
        private const val VIRAMA = '\u1039'
        private const val DOT_BELOW = '\u1037'
        private const val ZERO = '\u1040'
        private const val WA = '\u101D'
        private val DIGITS = '\u1040'..'\u1049'

        /** The signs written on a letter: Burmese's (U+102B–U+103F), and the Pali, Mon and Shan ones of the block. */
        private val SIGNS = listOf(
            '\u102B'..'\u103F', '\u1056'..'\u1059', '\u105E'..'\u1060', '\u1062'..'\u1064', '\u1067'..'\u106D',
            '\u1071'..'\u1074', '\u1082'..'\u108D', '\u108F'..'\u108F', '\u109A'..'\u109D',
        )

        private fun isSign(c: Char): Boolean = SIGNS.any { c in it }

        private fun isLetterOrSign(c: Char?): Boolean = c != null && c in '\u1000'..'\u103F'

        private fun isDigit(c: Char?): Boolean = c != null && c in DIGITS

        /** [text] Unicode-normalised: asat + dot below as dot below + asat, ဥ + ီ as ဦ, ၀ next to a letter and no digit as ဝ. */
        fun normalise(text: String): String {
            if (text.none { it in '\u1000'..'\u109F' }) return text
            val ordered = text.replace("\u103A\u1037", "\u1037\u103A").replace("\u1025\u102E", "\u1026")
            if (ZERO !in ordered) return ordered
            val out = StringBuilder(ordered)
            for (i in ordered.indices) {
                if (ordered[i] != ZERO) continue
                val before = ordered.getOrNull(i - 1)
                val after = ordered.getOrNull(i + 1)
                if (isDigit(before) || isDigit(after)) continue
                if (isLetterOrSign(before) || isLetterOrSign(after)) out[i] = WA
            }
            return out.toString()
        }
    }
}
