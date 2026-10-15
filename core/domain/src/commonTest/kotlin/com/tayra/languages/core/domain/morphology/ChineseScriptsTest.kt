package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

class ChineseScriptsTest {

    private val toTraditional = ChineseConverter(
        listOf(
            listOf(ChineseConverter.parse("头发\t頭髮\n发现\t發現\n"), ChineseConverter.parse("发\t發 髮\n头\t頭\n现\t現\n们\t們\n为\t爲 為\n")),
            listOf(emptyMap(), ChineseConverter.parse("爲\t為\n")),
        ),
    )

    @Test
    fun phrasesComeBeforeCharactersAndStagesChain() {
        assertEquals("頭髮", toTraditional.convert("头发"))
        assertEquals("發現", toTraditional.convert("发现"))
        assertEquals("我們為", toTraditional.convert("我们为"), "為 is Taiwan's spelling of OpenCC's 爲")
        assertEquals("abc 𠮟", toTraditional.convert("abc 𠮟"), "what the tables lack is copied, a surrogate pair whole")
    }

    @Test
    fun aDictionaryIsReadBackwards() {
        assertEquals(mapOf("為" to "爲"), ChineseConverter.reversed(ChineseConverter.parse("# comment\n爲\t為\n")))
    }
}
