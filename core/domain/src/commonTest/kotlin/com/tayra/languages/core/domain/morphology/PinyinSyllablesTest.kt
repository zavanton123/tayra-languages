package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

class PinyinSyllablesTest {

    @Test
    fun spacedAndJoinedPinyinIsCutIntoSyllablesWithTheirTones() {
        assertEquals(listOf("nǐ" to 3, " " to 0, "hǎo" to 3), Chinese.syllables("nǐ hǎo").map { it.text to it.tone })
        assertEquals(listOf("wǒ" to 3, "men" to 5), Chinese.syllables("wǒmen").map { it.text to it.tone })
        assertEquals(listOf("yín" to 2, "háng" to 2), Chinese.syllables("yínháng").map { it.text to it.tone })
        assertEquals(listOf("nǚ" to 3, "ér" to 2), Chinese.syllables("nǚér").map { it.text to it.tone })
        assertEquals(listOf("Běi" to 3, "jīng" to 1), Chinese.syllables("Běijīng").map { it.text to it.tone })
        assertEquals(listOf("diàn" to 4, "nǎo" to 3), Chinese.syllables("diànnǎo").map { it.text to it.tone })
        assertEquals(listOf("yì" to 4, "diǎn" to 3, "r" to 5), Chinese.syllables("yìdiǎnr").map { it.text to it.tone })
    }

    @Test
    fun pinyinIsWrittenInZhuyin() {
        assertEquals("ㄋㄧˇ ㄏㄠˇ", Chinese.zhuyin("nǐ hǎo"))
        assertEquals("ㄨㄛˇ ˙ㄇㄣ", Chinese.zhuyin("wǒmen"))
        assertEquals("ㄋㄩˇ ㄦˊ", Chinese.zhuyin("nǚ ér"))
        assertEquals("ㄓㄨㄥ ㄨㄣˊ", Chinese.zhuyin("zhōngwén"))
    }
}
