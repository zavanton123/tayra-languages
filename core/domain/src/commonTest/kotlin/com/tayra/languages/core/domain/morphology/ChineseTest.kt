package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

/** Yomitan's own cases for normalizePinyin (test/language/chinese-reading-normalizer.test.js). */
class ChineseTest {
    @Test
    fun pinyinIsNormalisedAsYomitanDoes() {
        listOf(
            "rìwén" to "rìwén", "Rì wén" to "rìwén", "Wéi jī Bǎi kē" to "wéijībǎikē", "wán:zhěng" to "wánzhěng",
            "fān・yì" to "fānyì", "fān//yì" to "fānyì", "fān’yì" to "fānyì", "fān'yì" to "fānyì", "fān-yì" to "fānyì",
        ).forEach { (input, expected) -> assertEquals(expected, Chinese.normalizePinyin(input), input) }
        assertEquals("nihao", Chinese.tonelessPinyin("Nǐ hǎo"))
    }
}
