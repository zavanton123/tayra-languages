package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.TestLanguages
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassicalChineseParserTest {
    @Test
    fun eachCharacterIsAToken() {
        val tokens = ClassicalChineseParser().parse("學而時習之。\n不亦說乎", TestLanguages.classicalChinese)
        assertEquals(listOf("學", "而", "時", "習", "之", "。", "¶", "不", "亦", "說", "乎"), tokens.map { it.token })
        assertEquals(listOf(true, true, true, true, true, false, false, true, true, true, true), tokens.map { it.isWord })
        assertEquals(listOf(false, false, false, false, false, true, true, false, false, false, false), tokens.map { it.isEndOfSentence })
    }
}
