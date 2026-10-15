package com.tayra.languages.core.domain.morphology

/**
 * Yomitan's Japanese text conversions (ext/js/language/ja, GPL-3.0, Copyright (C) 2023-2026
 * Yomitan Authors): kana and width variants, combining marks, emphatic spellings, romaji and
 * kanji variants.
 */
internal object Japanese {
    private const val HIRAGANA_FIRST = 0x3041
    private const val HIRAGANA_LAST = 0x3096
    private const val KATAKANA_FIRST = 0x30a1
    private const val KATAKANA_LAST = 0x30f6
    private const val SMALL_KA = 'ヵ'
    private const val SMALL_KE = 'ヶ'
    private const val PROLONGED = 'ー'
    private const val SMALL_TSU_HIRAGANA = 'っ'
    private const val SMALL_TSU_KATAKANA = 'ッ'

    private val vowels: Map<Char, String> = listOf(
        "a" to "ぁあかがさざただなはばぱまゃやらゎわヵァアカガサザタダナハバパマャヤラヮワヵヷ",
        "i" to "ぃいきぎしじちぢにひびぴみりゐィイキギシジチヂニヒビピミリヰヸ",
        "u" to "ぅうくぐすずっつづぬふぶぷむゅゆるゥウクグスズッツヅヌフブプムュユルヴ",
        "e" to "ぇえけげせぜてでねへべぺめれゑヶェエケゲセゼテデネヘベペメレヱヶヹ",
        "o" to "ぉおこごそぞとどのほぼぽもょよろをォオコゴソゾトドノホボポモョヨロヲヺ",
        "" to "のノ",
    ).flatMap { (vowel, kana) -> kana.map { it to vowel } }.toMap()

    private val halfWidth: Map<Char, String> = listOf(
        "･・--", "ｦヲヺ-", "ｧァ--", "ｨィ--", "ｩゥ--", "ｪェ--", "ｫォ--", "ｬャ--", "ｭュ--", "ｮョ--", "ｯッ--", "ｰー--",
        "ｱア--", "ｲイ--", "ｳウヴ-", "ｴエ--", "ｵオ--", "ｶカガ-", "ｷキギ-", "ｸクグ-", "ｹケゲ-", "ｺコゴ-", "ｻサザ-",
        "ｼシジ-", "ｽスズ-", "ｾセゼ-", "ｿソゾ-", "ﾀタダ-", "ﾁチヂ-", "ﾂツヅ-", "ﾃテデ-", "ﾄトド-", "ﾅナ--", "ﾆニ--",
        "ﾇヌ--", "ﾈネ--", "ﾉノ--", "ﾊハバパ", "ﾋヒビピ", "ﾌフブプ", "ﾍヘベペ", "ﾎホボポ", "ﾏマ--", "ﾐミ--", "ﾑム--",
        "ﾒメ--", "ﾓモ--", "ﾔヤ--", "ﾕユ--", "ﾖヨ--", "ﾗラ--", "ﾘリ--", "ﾙル--", "ﾚレ--", "ﾛロ--", "ﾜワ--", "ﾝン--",
    ).associate { it[0] to it.substring(1) }

    private val romaji: List<Pair<String, String>> by lazy {
        romajiToHiragana.split('\n').filter { it.isNotEmpty() }.map { it.substringBefore('\t') to it.substringAfter('\t') }
    }

    private val variants: Map<String, String> by lazy {
        kanjiVariants.split('\n').filter { it.isNotEmpty() }.associate { it.substringBefore('\t') to it.substringAfter('\t') }
    }

    private fun prolongedHiragana(previous: Char): Char? = when (vowels[previous]) {
        "a" -> 'あ'
        "i" -> 'い'
        "u" -> 'う'
        "e" -> 'え'
        "o" -> 'う'
        else -> null
    }

    fun katakanaToHiragana(text: String, keepProlongedSoundMarks: Boolean = false): String = buildString {
        for (c in text) {
            when {
                c == SMALL_KA || c == SMALL_KE -> append(c)
                c == PROLONGED -> append(if (!keepProlongedSoundMarks && isNotEmpty()) prolongedHiragana(last()) ?: c else c)
                c.code in KATAKANA_FIRST..KATAKANA_LAST -> append((c.code - KATAKANA_FIRST + HIRAGANA_FIRST).toChar())
                else -> append(c)
            }
        }
    }

    fun hiraganaToKatakana(text: String): String = buildString {
        for (c in text) append(if (c.code in HIRAGANA_FIRST..HIRAGANA_LAST) (c.code - HIRAGANA_FIRST + KATAKANA_FIRST).toChar() else c)
    }

    fun alphanumericToFullWidth(text: String): String = buildString {
        for (c in text) {
            append(
                when (c) {
                    in '0'..'9' -> (c.code + 0xff10 - 0x30).toChar()
                    in 'A'..'Z' -> (c.code + 0xff21 - 0x41).toChar()
                    in 'a'..'z' -> (c.code + 0xff41 - 0x61).toChar()
                    else -> c
                },
            )
        }
    }

    fun fullWidthAlphanumericToNormal(text: String): String = buildString {
        for (c in text) {
            append(
                when (c.code) {
                    in 0xff10..0xff19 -> (c.code - (0xff10 - 0x30)).toChar()
                    in 0xff21..0xff3a -> (c.code - (0xff21 - 0x41)).toChar()
                    in 0xff41..0xff5a -> (c.code - (0xff41 - 0x61)).toChar()
                    else -> c
                },
            )
        }
    }

    fun halfWidthKanaToFullWidth(text: String): String = buildString {
        var i = 0
        while (i < text.length) {
            val mapping = halfWidth[text[i]]
            if (mapping == null) {
                append(text[i])
                i++
                continue
            }
            var index = when (text.getOrNull(i + 1)) {
                'ﾞ' -> 1
                'ﾟ' -> 2
                else -> 0
            }
            if (index > 0) {
                if (mapping[index] == '-') index = 0 else i++
            }
            append(mapping[index])
            i++
        }
    }

    private fun dakutenAllowed(c: Int) = c in 0x304b..0x3068 || c in 0x306f..0x307b || c in 0x30ab..0x30c8 || c in 0x30cf..0x30db
    private fun handakutenAllowed(c: Int) = c in 0x306f..0x307b || c in 0x30cf..0x30db

    /** Kana followed by a combining (han)dakuten as the precomposed kana ("ト" + "゙" as "ド"). */
    fun normalizeCombiningCharacters(text: String): String {
        val result = StringBuilder()
        var i = text.length - 1
        while (i > 0) {
            val previous = text[i - 1].code
            when {
                text[i] == '゙' && dakutenAllowed(previous) -> {
                    result.insert(0, (previous + 1).toChar())
                    i -= 2
                }
                text[i] == '゚' && handakutenAllowed(previous) -> {
                    result.insert(0, (previous + 2).toChar())
                    i -= 2
                }
                else -> {
                    result.insert(0, text[i])
                    i--
                }
            }
        }
        if (i == 0) result.insert(0, text[0])
        return result.toString()
    }

    private fun isEmphatic(c: Char) = c == SMALL_TSU_HIRAGANA || c == SMALL_TSU_KATAKANA || c == PROLONGED

    /** Repeated small tsu and long-vowel marks as one ("すっっごーーい" as "すっごーい"), or none at all when [full]. */
    fun collapseEmphaticSequences(text: String, full: Boolean): String {
        var left = 0
        while (left < text.length && isEmphatic(text[left])) left++
        var right = text.length - 1
        while (right >= 0 && isEmphatic(text[right])) right--
        if (left > right) return text
        val middle = StringBuilder()
        var collapsed: Char? = null
        for (i in left..right) {
            val c = text[i]
            if (isEmphatic(c)) {
                if (collapsed != c) {
                    collapsed = c
                    if (!full) middle.append(c)
                }
            } else {
                collapsed = null
                middle.append(c)
            }
        }
        return text.substring(0, left) + middle + text.substring(right + 1)
    }

    /** Romaji in a word as hiragana ("yomichan" as "よみちゃん"). */
    fun alphabeticToKana(text: String): String {
        val part = StringBuilder()
        val result = StringBuilder()
        for (c in text) {
            val letter = when (c.code) {
                in 0x41..0x5a -> (c.code + 0x20).toChar()
                in 0x61..0x7a -> c
                in 0xff21..0xff3a -> (c.code - 0xff21 + 0x61).toChar()
                in 0xff41..0xff5a -> (c.code - 0xff41 + 0x61).toChar()
                0x2d, 0xff0d -> '-'
                else -> null
            }
            if (letter == null) {
                if (part.isNotEmpty()) {
                    result.append(romajiToKana(part.toString()))
                    part.clear()
                }
                result.append(c)
            } else {
                part.append(letter)
            }
        }
        if (part.isNotEmpty()) result.append(romajiToKana(part.toString()))
        return result.toString()
    }

    private val sokuonHiragana = Regex("っ[a-z](?=っ)")
    private val sokuonKatakana = Regex("ッ[A-Z](?=ッ)")

    private fun romajiToKana(text: String): String {
        var result = text.lowercase()
        for ((r, kana) in romaji) result = result.replace(r, kana)
        return sokuonKatakana.replace(sokuonHiragana.replace(result, "っっ"), "ッッ")
    }

    /** Variant kanji as their standard forms (萬 as 万). */
    fun standardizeKanji(text: String): String {
        if (text.none { it.code >= 0x3400 }) return text
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val length = if (text[i].isHighSurrogate() && i + 1 < text.length) 2 else 1
            val c = text.substring(i, i + length)
            result.append(variants[c] ?: c)
            i += length
        }
        return result.toString()
    }
}
