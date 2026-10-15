package com.tayra.languages.core.domain.morphology

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 10ten Japanese Reader's number, era and area cases (the tests beside src/content/numbers.ts, dates.ts and measure.ts, GPL-3.0). */
class JapaneseNotesTest {

    @Test
    fun numbersAreReadAsTenTenReadsThem() {
        val cases = mapOf(
            "〇" to 0.0, "一" to 1.0, "五六" to 56.0, "五十六" to 56.0, "五百十六" to 516.0, "五十万一" to 500001.0,
            "五億五万一" to 500050001.0, "五万億五万一" to 5000000050001.0, "五十万億百万一" to 50000001000001.0,
            "8万8千" to 88000.0, "８万８千" to 88000.0, "9万8800" to 98800.0, "100億" to 10000000000.0, "100億123万3" to 10001230003.0,
            "七万九千〇五" to 79005.0, "七二一三〇六四九" to 72130649.0, "８３０" to 830.0, "830" to 830.0, "43.2" to 43.2,
            "４３．２" to 43.2, "４３。２" to 43.2, "43.2万" to 432000.0, "4.2万" to 42000.0, "0.2万" to 2000.0, "54.321万" to 543210.0,
            "1.2億" to 120000000.0, "39,800万" to 398000000.0, "11,786百万" to 11786000000.0, "123,456,789百万" to 123456789000000.0,
        )
        for ((text, value) in cases) {
            val parsed = JapaneseNotes.parseNumber(text)
            assertTrue(parsed != null && abs(parsed - value) < 1e-6 * maxOf(1.0, value), "$text is $value, not $parsed")
        }
        for (bogus in listOf("七十二百一", "40k", "abc", "")) assertNull(JapaneseNotes.parseNumber(bogus), bogus)
    }

    @Test
    fun aNumberNoteNeedsTwoCharactersAndAKanji() {
        for (text in listOf("1", "0", "二", "十", "〇", "12", "１２")) assertNull(JapaneseNotes.number(text), text)
        assertEquals(JapaneseNote.Number("二十", 20.0), JapaneseNotes.number("二十歳"))
        assertEquals(JapaneseNote.Number("3万", 30000.0), JapaneseNotes.number("3万円"))
    }

    @Test
    fun eraYearsBecomeWesternYears() {
        assertEquals(JapaneseNote.EraYear("昭和56年", "昭和", "しょうわ", 56, 1981, null, null), JapaneseNotes.parseEraYear("昭和56年に"))
        assertEquals(1981, JapaneseNotes.parseEraYear("昭和五十六年")?.gregorianYear)
        assertEquals(JapaneseNote.EraYear("令和元年", "令和", "れいわ", 0, 2019, null, null), JapaneseNotes.parseEraYear("令和元年"))
        assertEquals(2019, JapaneseNotes.parseEraYear("平成三十一年")?.gregorianYear)
        assertEquals(1909, JapaneseNotes.parseEraYear("明治四十二年")?.gregorianYear)
        assertEquals(2020, JapaneseNotes.parseEraYear("㋿2年")?.gregorianYear)
        val dated = JapaneseNotes.parseEraYear("平成31年4月30日")!!
        assertEquals(Triple(2019, 4, 30), Triple(dated.gregorianYear, dated.month, dated.day))
        assertNull(JapaneseNotes.parseEraYear("令和元"), "元 needs its 年")
        assertNull(JapaneseNotes.parseEraYear("明治三年"), "the old calendar was still in use")
        assertNull(JapaneseNotes.parseEraYear("令"))
    }

    @Test
    fun floorAreasConvertBetweenMatsAndSquareMetres() {
        val half = JapaneseNotes.area("四畳半の部屋")!!
        assertEquals("四畳半", half.source)
        assertEquals(4.5, half.value)
        assertTrue(abs(half.converted - 7.29) < 1e-9)
        assertEquals(listOf("京間", "中京間", "江戸間", "団地間"), half.regional.map { it.first })
        assertEquals(1.5, JapaneseNotes.area("1.5 帖 です")!!.value)
        assertTrue(JapaneseNotes.area("1.5 帖")!!.regional.isEmpty(), "a 帖 is 1.62 m² everywhere")
        val metres = JapaneseNotes.area("22.5平米")!!
        assertEquals(false, metres.inMats)
        assertTrue(abs(metres.converted - 22.5 / 1.62) < 1e-9)
        assertEquals(12.0, JapaneseNotes.area("十二平方米")!!.value)
        assertNull(JapaneseNotes.area("畳む"))
    }

    @Test
    fun aWordInAnEraYearGetsTheYearsNote() {
        val sentence = "昭和五十六年に生まれた。"
        assertEquals(listOf<JapaneseNote>(JapaneseNotes.parseEraYear(sentence)!!), JapaneseNotes.at(sentence, 2))
        assertEquals(listOf<JapaneseNote>(JapaneseNote.Number("五十六", 56.0)), JapaneseNotes.at("彼は五十六歳だ", 2))
        assertTrue(JapaneseNotes.at("生まれた", 0).isEmpty())
    }
}
