package com.tayra.languages.core.domain.morphology

/** Yomitan's Chinese reading helpers (ext/js/language/zh, GPL-3.0, Copyright (C) 2024-2026 Yomitan Authors). */
object Chinese {
    private val pinyinSeparators = Regex("[\\s・:'’-]|//")

    /** Pinyin as readings are matched: composed, lowercase, without spaces or syllable marks ("Wéi jī" as "wéijī"). */
    fun normalizePinyin(text: String): String = Unicode.nfc(text).lowercase().replace(pinyinSeparators, "")

    /** Pinyin without its tone marks too ("nǐhǎo" as "nihao"), for readings typed without them. */
    fun tonelessPinyin(text: String): String = Unicode.nfc(Unicode.nfd(normalizePinyin(text)).filter { it !in '̀'..'ͯ' })

    /**
     * The syllables of pinyin with tone marks, spaced or joined ("wǒmen", "nǐ hǎo"), each with its tone,
     * 1 to 4 or 5 for the neutral tone; what is not a syllable (a space, an apostrophe) has tone 0.
     * Joined syllables are cut at the longest syllable of standard pinyin, as Zhongwen's table lists them.
     */
    fun syllables(pinyin: String): List<PinyinSyllable> {
        val text = Unicode.nfc(pinyin)
        val plain = StringBuilder()
        val tones = IntArray(text.length)
        for ((i, c) in text.withIndex()) {
            val decomposed = Unicode.nfd(c.toString())
            plain.append(Unicode.nfc(decomposed.filter { it !in TONE_MARKS.keys || decomposed.length == 1 }).lowercase().firstOrNull() ?: c)
            tones[i] = decomposed.firstNotNullOfOrNull { TONE_MARKS[it] } ?: 0
        }
        val lower = plain.toString()
        // The cut with the fewest syllables, a letter no syllable takes costing most and an erhua r
        // more than a syllable, so "nǚér" is nǚ·ér rather than nüe·r and "yìdiǎnr" yì·diǎn·r.
        val cost = IntArray(text.length + 1) { if (it == 0) 0 else Int.MAX_VALUE }
        val from = IntArray(text.length + 1)
        for (i in 0 until text.length) {
            if (cost[i] == Int.MAX_VALUE) continue
            fun step(end: Int, price: Int) {
                if (cost[i] + price < cost[end]) {
                    cost[end] = cost[i] + price
                    from[end] = i
                }
            }
            step(i + 1, if (lower[i].isLetter()) UNKNOWN else 0)
            if (lower[i] == 'r' && i > 0 && lower[i - 1].isLetter()) step(i + 1, ERHUA)
            for (end in i + 1..minOf(text.length, i + MAX_SYLLABLE)) if (lower.substring(i, end) in SYLLABLES) step(end, 1)
        }
        val cuts = generateSequence(text.length) { if (it == 0) null else from[it] }.toList().reversed()
        return cuts.zipWithNext { start, end ->
            val part = text.substring(start, end)
            val known = lower.substring(start, end).let { it in SYLLABLES || (it == "r" && start > 0 && lower[start - 1].isLetter()) }
            if (!known) PinyinSyllable(part, 0) else PinyinSyllable(part, (start until end).map { tones[it] }.firstOrNull { it != 0 } ?: 5)
        }
    }

    /** Pinyin written in zhuyin (bopomofo), syllable by syllable: "nǐ hǎo" as "ㄋㄧˇ ㄏㄠˇ"; what it cannot write stays as it is. */
    fun zhuyin(pinyin: String): String = syllables(pinyin).mapNotNull { syllable ->
        when {
            syllable.tone == 0 -> syllable.text.takeIf { it.isNotBlank() && it !in "'’-" }
            else -> {
                val key = withoutTone(syllable.text)
                val letters = Zhuyin.bySyllable[key] ?: if (key == "r") "ㄦ" else null
                when {
                    letters == null -> syllable.text
                    syllable.tone == 5 -> "˙$letters"
                    else -> letters + Zhuyin.toneMarks[syllable.tone - 1]
                }
            }
        }
    }.joinToString(" ")

    /** A syllable without its tone mark, keeping ü's dots: "nǚ" as "nü". */
    private fun withoutTone(text: String): String = Unicode.nfc(Unicode.nfd(text).filter { it !in TONE_MARKS.keys }).lowercase()

    private const val UNKNOWN = 10
    private const val ERHUA = 2

    private val TONE_MARKS = mapOf('\u0304' to 1, '\u0301' to 2, '\u030C' to 3, '\u0300' to 4)
    private val SYLLABLES: Set<String> get() = Zhuyin.bySyllable.keys
    private const val MAX_SYLLABLE = 6

    /** Whether the text has any Chinese character, as Yomitan's isStringPartiallyChinese tells. */
    fun isPartlyChinese(text: String): Boolean = text.any { c ->
        c.code in 0x4e00..0x9fff || c.code in 0x3400..0x4dbf || c.code in 0xf900..0xfaff || c.code in 0x3000..0x303f ||
            c.code in 0x3100..0x312f || c.code in 0x31a0..0x31bf || c.isSurrogate()
    }
}

/** A syllable of pinyin as it is written, with its tone: 1 to 4, 5 neutral, 0 for what is not a syllable. */
data class PinyinSyllable(val text: String, val tone: Int)
