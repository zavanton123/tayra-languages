package com.tayra.languages.core.domain.morphology

import kotlin.math.pow

/**
 * What a Japanese reader may want spelled out about a stretch of text, as 10ten Japanese Reader shows
 * it (src/content/numbers.ts, dates.ts and measure.ts, GPL-3.0, Copyright Brian Birtles and
 * contributors): a number written in kanji, a year of a Japanese era, a floor area in mats.
 */
sealed interface JapaneseNote {
    /** The text the note is about. */
    val source: String

    data class Number(override val source: String, val value: Double) : JapaneseNote

    /** [year] 0 stands for 元年, the era's first year; [gregorianYear] is the Western year. */
    data class EraYear(override val source: String, val era: String, val reading: String, val year: Int, val gregorianYear: Int, val month: Int?, val day: Int?) : JapaneseNote

    /** A floor area in mats (畳, 帖) or square metres, with what it comes to in the other. */
    data class Area(override val source: String, val value: Double, val inMats: Boolean, val converted: Double, val regional: List<Pair<String, Double>>) : JapaneseNote
}

object JapaneseNotes {

    /** The notes on the text starting at [start] of [text], or ending in an era just before it ("昭和" before "五十六年"). */
    fun at(text: String, start: Int): List<JapaneseNote> {
        if (start !in text.indices) return emptyList()
        val notes = mutableListOf<JapaneseNote>()
        // An era name may begin a little before the word clicked, which is often its year.
        val eraStart = (maxOf(0, start - MAX_ERA_LENGTH)..start).firstOrNull { from -> parseEraYear(text.substring(from))?.let { it.source.length > start - from } == true }
        eraStart?.let { from -> parseEraYear(text.substring(from))?.let { notes += it } }
        val rest = text.substring(start)
        area(rest)?.let { notes += it }
        if (notes.none { it is JapaneseNote.EraYear }) number(rest)?.let { notes += it }
        return notes
    }

    // --- Numbers -------------------------------------------------------------------------------

    private val digits: Map<Char, Int> = buildMap {
        "〇一二三四五六七八九".forEachIndexed { i, c -> put(c, i) }
        "０１２３４５６７８９".forEachIndexed { i, c -> put(c, i) }
        "0123456789".forEachIndexed { i, c -> put(c, i) }
        for (c in ".。．") put(c, -1)
    }
    private val powers: Map<Char, Long> = mapOf('十' to 10L, '百' to 100L, '千' to 1000L, '万' to 10_000L, '億' to 100_000_000L, '兆' to 1_000_000_000_000L)

    /** The value of a number written with kanji, full-width or ASCII digits, and powers of ten ("五十六", "8万8千", "39,800万"); null when it is none. */
    fun parseNumber(input: String): Double? {
        val text = input.replace(",", "").replace("、", "")
        if (text.isEmpty()) return null
        // A run of digits alone ("二二一") is read digit by digit.
        val plain = text.map { digits[it] }
        if (plain.none { it == null }) {
            return plain.joinToString("") { if (it == -1) "." else it.toString() }.toDoubleOrNull()
        }
        val values: List<Double> = text.map { c -> digits[c]?.toDouble() ?: powers[c]?.toDouble() ?: return null }
        // Digits followed by powers of ten ("39,800万", "11,786百万").
        val lastDigit = values.indexOfLast { it < 100 }
        if (lastDigit in 0 until values.lastIndex && values.subList(0, lastDigit + 1).all { it >= 0 && it < 10 } && values.subList(lastDigit + 1, values.size).all { it >= 100 }) {
            return values.subList(0, lastDigit + 1).fold(0.0) { acc, d -> acc * 10 + d } * values.subList(lastDigit + 1, values.size).fold(1.0) { acc, p -> acc * p }
        }
        var numbers = values.toMutableList()
        var result = 0.0
        while (numbers.size > 1) {
            val first = numbers[0]
            val second = numbers[1]
            if (first < 10 && second < 10) {
                // A run of digits, perhaps with a decimal point ("43.2万").
                var decimal = 0
                while (numbers.size > 1 && numbers[1] < 10) {
                    when {
                        numbers[1] == -1.0 -> {
                            if (decimal != 0) return null
                            decimal = 1
                            numbers.removeAt(1)
                        }
                        decimal != 0 -> numbers = (listOf(numbers[0] + numbers[1] * 10.0.pow(-decimal++)) + numbers.drop(2)).toMutableList()
                        else -> numbers = (listOf(numbers[0] * 10 + numbers[1]) + numbers.drop(2)).toMutableList()
                    }
                }
                continue
            }
            if (!validSequence(first, second)) return null
            if (second < first) {
                if (numbers.drop(2).any { it > first }) {
                    val third = numbers[2]
                    numbers = if (first < third || third < second) {
                        (listOf(first + second) + numbers.drop(2)).toMutableList()
                    } else {
                        (listOf(first, second * third) + numbers.drop(3)).toMutableList()
                    }
                } else {
                    result += first
                    numbers = numbers.drop(1).toMutableList()
                }
            } else {
                numbers = (listOf(first * second) + numbers.drop(2)).toMutableList()
            }
        }
        return result + (numbers.firstOrNull() ?: 0.0)
    }

    private fun validSequence(c1: Double, c2: Double): Boolean {
        if (c2 >= 10_000 && c1 < c2) return true
        if (c1 >= 10_000 && c2 <= 1000) return true
        if (c1 >= 100 && c2 < c1 && c2 >= 10 && c2 <= 1000) return true
        // Not 一十 or 一百.
        if (c1 == 1.0 && (c2 == 10.0 || c2 == 100.0)) return false
        return (c1 < 10) != (c2 < 10)
    }

    private val numberRegex = Regex(
        "^(?:[一二三四五六七八九十百千万億兆京][0-9.,０-９。．、〇一二三四五六七八九十百千万億兆京]+)" +
            "|^(?:[0-9,０-９、]+(?:[.。．][0-9０-９]+)?[〇一二三四五六七八九十百千万億兆京][0-9.,０-９。．、〇一二三四五六七八九十百千万億兆京]*)",
    )

    /** A number of at least two characters, one of them a kanji, at the start of [text]. */
    fun number(text: String): JapaneseNote.Number? {
        val source = numberRegex.find(text)?.value ?: return null
        val value = parseNumber(source)?.takeIf { it != 0.0 } ?: return null
        return JapaneseNote.Number(source, value)
    }

    // --- Era years -----------------------------------------------------------------------------

    /** The eras since the calendar became the Western one (1873), with the three earliest Yomitan treats alike, by name: reading and first year. */
    private val eras: Map<String, Pair<String, Int>> = mapOf(
        "大化" to ("たいか" to 645), "白雉" to ("はくち" to 650), "朱鳥" to ("しゅちょう" to 686),
        "明治" to ("めいじ" to 1868), "大正" to ("たいしょう" to 1912), "昭和" to ("しょうわ" to 1926),
        "平成" to ("へいせい" to 1989), "令和" to ("れいわ" to 2019),
    )
    private val eraAliases = mapOf("㍾" to "明治", "㍽" to "大正", "㍼" to "昭和", "㍻" to "平成", "㋿" to "令和")
    private val MAX_ERA_LENGTH = eras.keys.maxOf { it.length }

    private const val NUMERALS = "0-9０-９〇一二三四五六七八九十百"
    private val eraDate = Regex("^(${(eras.keys + eraAliases.keys).joinToString("|")})\\s*(?:([$NUMERALS]+)\\s*(?:年|歳)?|(元)\\s*(?:年|歳))(?:\\s*([$NUMERALS]+)\\s*月(?:\\s*([$NUMERALS]+)\\s*日)?)?")

    /** A year of a modern era at the start of [text] ("昭和56年", "令和元年", "平成三十一年四月三十日"), with the Western year. */
    fun parseEraYear(text: String): JapaneseNote.EraYear? {
        val match = eraDate.find(text) ?: return null
        val era = eraAliases[match.groupValues[1]] ?: match.groupValues[1]
        val (reading, first) = eras[era] ?: return null
        val year = when {
            match.groupValues[3].isNotEmpty() -> 0
            else -> parseNumber(match.groupValues[2])?.toInt()?.takeIf { it >= 1 } ?: return null
        }
        // Meiji's first years were still counted by the old calendar.
        if (era == "明治" && year in 1..5) return null
        val month = match.groupValues[4].takeIf { it.isNotEmpty() }?.let { parseNumber(it)?.toInt() }?.takeIf { it in 1..12 }
        val day = if (month != null) match.groupValues[5].takeIf { it.isNotEmpty() }?.let { parseNumber(it)?.toInt() }?.takeIf { it in 1..31 } else null
        // Without a valid month, the match ends with the year.
        val source = if (month == null) {
            Regex("^(${(eras.keys + eraAliases.keys).joinToString("|")})\\s*(?:[$NUMERALS]+\\s*(?:年|歳)?|元\\s*(?:年|歳))").find(text)!!.value
        } else {
            match.value
        }
        return JapaneseNote.EraYear(source, era, reading, year, first + maxOf(year, 1) - 1, month, day)
    }

    // --- Floor areas ---------------------------------------------------------------------------

    private val mats = Regex("^([0-9.０-９。．〇一二三四五六七八九十百]+)\\s*(畳|帖)(半?)")
    private val squareMetres = Regex("^([0-9.０-９。．〇一二三四五六七八九十百千万]+)\\s*(㎡|m2|m²|平方メートル|平方ﾒｰﾄﾙ|平方㍍|平㍍|平米|平方米)")

    /** The mats of the regions of Japan, in square metres: a 畳 is one of these, a 帖 is 1.62 m². */
    private val regionalMats = listOf("京間" to 1.82405, "中京間" to 1.6562, "江戸間" to 1.5488, "団地間" to 1.445)

    /** A floor area at the start of [text], in mats ("四畳半", "6帖") or square metres ("22.5平米"), converted to the other. */
    fun area(text: String): JapaneseNote.Area? {
        mats.find(text)?.let { match ->
            val value = (parseNumber(match.groupValues[1]) ?: return null) + if (match.groupValues[3] == "半") 0.5 else 0.0
            val regional = if (match.groupValues[2] == "畳") regionalMats.map { (name, ratio) -> name to value * ratio } else emptyList()
            return JapaneseNote.Area(match.value, value, inMats = true, converted = value * JOU, regional = regional)
        }
        squareMetres.find(text)?.let { match ->
            val value = parseNumber(match.groupValues[1]) ?: return null
            return JapaneseNote.Area(match.value, value, inMats = false, converted = value / JOU, regional = regionalMats.map { (name, ratio) -> name to value / ratio })
        }
        return null
    }

    /** Square metres in a 帖, the mat of real-estate listings. */
    private const val JOU = 1.62
}
