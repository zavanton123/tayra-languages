package com.tayra.languages.core.domain.morphology

/**
 * Hangul syllables taken apart into their letters (jamo) and put together again, as Yomitan's
 * Korean rules expect: a port of Hangul.js's disassemble and assemble (MIT, Copyright 2017 Jaemin Jo),
 * which split complex vowels and final clusters too ("왔" is ㅇ ㅗ ㅏ ㅆ).
 */
internal object Hangul {
    private const val OFFSET = 0xAC00
    private val CHO = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    private val JUNG = listOf(
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅗㅏ", "ㅗㅐ",
        "ㅗㅣ", "ㅛ", "ㅜ", "ㅜㅓ", "ㅜㅔ", "ㅜㅣ", "ㅠ", "ㅡ", "ㅡㅣ", "ㅣ",
    )
    private val JONG = listOf(
        "", "ㄱ", "ㄲ", "ㄱㅅ", "ㄴ", "ㄴㅈ", "ㄴㅎ", "ㄷ", "ㄹ", "ㄹㄱ", "ㄹㅁ", "ㄹㅂ", "ㄹㅅ", "ㄹㅌ", "ㄹㅍ", "ㄹㅎ", "ㅁ",
        "ㅂ", "ㅂㅅ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
    )
    private const val CONSONANTS = "ㄱㄲㄳㄴㄵㄶㄷㄸㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅃㅄㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val COMPLETE_CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val COMPLETE_JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    /** The final consonants, after the empty one at index 0. */
    private const val COMPLETE_JONG = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
    private val COMPLEX_CONSONANTS = mapOf(
        "ㄱㅅ" to 'ㄳ', "ㄴㅈ" to 'ㄵ', "ㄴㅎ" to 'ㄶ', "ㄹㄱ" to 'ㄺ', "ㄹㅁ" to 'ㄻ', "ㄹㅂ" to 'ㄼ',
        "ㄹㅅ" to 'ㄽ', "ㄹㅌ" to 'ㄾ', "ㄹㅍ" to 'ㄿ', "ㄹㅎ" to 'ㅀ', "ㅂㅅ" to 'ㅄ',
    )
    private val COMPLEX_VOWELS = mapOf("ㅗㅏ" to 'ㅘ', "ㅗㅐ" to 'ㅙ', "ㅗㅣ" to 'ㅚ', "ㅜㅓ" to 'ㅝ', "ㅜㅔ" to 'ㅞ', "ㅜㅣ" to 'ㅟ', "ㅡㅣ" to 'ㅢ')

    private fun isConsonant(c: Char) = c in CONSONANTS
    private fun isCho(c: Char) = c in COMPLETE_CHO
    private fun isJung(c: Char) = c in COMPLETE_JUNG
    private fun isJong(c: Char) = c in COMPLETE_JONG
    private fun choIndex(c: Char) = COMPLETE_CHO.indexOf(c)
    private fun jungIndex(c: Char) = COMPLETE_JUNG.indexOf(c)
    private fun jongIndex(c: Char) = COMPLETE_JONG.indexOf(c) + 1

    /** The vowel two vowels join into, or the null character when they do not (as Hangul.js gives). */
    private fun jungJoined(a: Char, b: Char): Char? = COMPLEX_VOWELS["$a$b"]
    private fun jongJoined(a: Char, b: Char): Char? = COMPLEX_CONSONANTS["$a$b"]

    fun disassemble(text: String): String = buildString {
        for (c in text) {
            when {
                c.code in OFFSET..0xD7A3 -> {
                    val code = c.code - OFFSET
                    val jong = code % 28
                    val jung = (code - jong) / 28 % 21
                    val cho = (code - jong) / 28 / 21
                    append(CHO[cho]).append(JUNG[jung])
                    if (jong > 0) append(JONG[jong])
                }
                isConsonant(c) -> append(if (isCho(c)) CHO[choIndex(c)] else JONG[jongIndex(c)])
                isJung(c) -> append(JUNG[jungIndex(c)])
                else -> append(c)
            }
        }
    }

    fun assemble(text: String): String {
        val array = disassemble(text)
        val result = StringBuilder()
        var completeIndex = -1
        var stage = 0
        var previous = '\u0000'
        var jongMerged = false

        fun syllable(cho: Char, jung: Char, jong: Char?): Char =
            ((choIndex(cho) * 21 + jungIndex(jung)) * 28 + (jong?.let { jongIndex(it) } ?: 0) + OFFSET).toChar()

        fun makeHangul(index: Int) {
            jongMerged = false
            if (completeIndex + 1 > index) return
            var cho = '\u0000'
            var jung1 = '\u0000'
            var jong1: Char? = null
            var hangul = ""
            var step = 1
            while (true) {
                val c = array[completeIndex + step]
                when (step) {
                    1 -> {
                        cho = c
                        if (isJung(cho)) {
                            if (completeIndex + step + 1 <= index && isJung(array[completeIndex + step + 1])) {
                                result.append(jungJoined(cho, array[completeIndex + step + 1]) ?: '\u0000')
                            } else {
                                result.append(c)
                            }
                            completeIndex = index
                            return
                        } else if (!isCho(cho)) {
                            result.append(c)
                            completeIndex = index
                            return
                        }
                        hangul = c.toString()
                    }
                    2 -> {
                        jung1 = c
                        if (isCho(jung1)) {
                            result.append(jongJoined(cho, jung1) ?: '\u0000')
                            completeIndex = index
                            return
                        }
                        hangul = syllable(cho, jung1, null).toString()
                    }
                    3 -> {
                        val joined = jungJoined(jung1, c)
                        if (joined != null) jung1 = joined else jong1 = c
                        hangul = syllable(cho, jung1, jong1).toString()
                    }
                    4 -> {
                        jong1 = jong1?.let { jongJoined(it, c) } ?: c
                        hangul = syllable(cho, jung1, jong1).toString()
                    }
                    5 -> {
                        jong1 = jong1?.let { jongJoined(it, c) }
                        hangul = syllable(cho, jung1, jong1).toString()
                    }
                }
                if (completeIndex + step >= index) {
                    result.append(hangul)
                    completeIndex = index
                    return
                }
                step++
            }
        }

        var i = 0
        while (i < array.length) {
            val code = array[i]
            if (!isCho(code) && !isJung(code) && !isJong(code)) {
                makeHangul(i - 1)
                makeHangul(i)
                stage = 0
                i++
                continue
            }
            when (stage) {
                0 -> if (isCho(code)) stage = 1 else if (isJung(code)) stage = 4
                1 -> if (isJung(code)) {
                    stage = 2
                } else if (jongJoined(previous, code) != null) {
                    stage = 5
                } else {
                    makeHangul(i - 1)
                }
                2 -> if (isJong(code)) {
                    stage = 3
                } else if (isJung(code)) {
                    if (jungJoined(previous, code) == null) {
                        makeHangul(i - 1)
                        stage = 4
                    }
                } else {
                    makeHangul(i - 1)
                    stage = 1
                }
                3 -> if (isJong(code)) {
                    if (!jongMerged && jongJoined(previous, code) != null) {
                        jongMerged = true
                    } else {
                        makeHangul(i - 1)
                        stage = 1
                    }
                } else if (isCho(code)) {
                    makeHangul(i - 1)
                    stage = 1
                } else if (isJung(code)) {
                    makeHangul(i - 2)
                    stage = 2
                }
                4 -> if (isJung(code)) {
                    if (jungJoined(previous, code) != null) {
                        makeHangul(i)
                        stage = 0
                    } else {
                        makeHangul(i - 1)
                    }
                } else {
                    makeHangul(i - 1)
                    stage = 1
                }
                5 -> if (isJung(code)) {
                    makeHangul(i - 2)
                    stage = 2
                } else {
                    makeHangul(i - 1)
                    stage = 1
                }
            }
            previous = code
            i++
        }
        makeHangul(i - 1)
        return result.toString()
    }
}
