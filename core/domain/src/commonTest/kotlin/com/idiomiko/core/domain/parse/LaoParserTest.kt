package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LaoParserTest {
    private val lao = LanguageDefinitions.all.single { it.language.name == "Lao" }.language

    @AfterTest
    fun forget() = Lexicons.set("lo", null)

    private fun words(text: String) = lao.parseTokens(text).filter { it.isWord }.map { it.token }

    /** The dictionary's ຕາ "eye", ຢາ "medicine" and its letters ວ and ດ end inside the next syllable: never taken there. */
    @Test
    fun aWordNeverEndsInsideASyllable() {
        Lexicons.set("lo", Lexicon(listOf("ລາວ", "ຍັງ", "ຕາ", "ຢາ", "ວ", "ດ", "ທາງ", "ໄປ", "ມື້", "ທຸ", "ກມ", "ເພີ່ມ", "ພີ່", "ຂ້າ")))
        // Letters no word covers go a syllable at a time: ຢາກ and ຕາມ, not ຢາ + ກ and ຕາ + ມ.
        assertEquals(listOf("ລາວ", "ຍັງ", "ຢາກ", "ໄປ", "ຕາມ", "ທາງ"), words("ລາວຍັງຢາກໄປຕາມທາງ"))
        // And are one word together, as a name is.
        assertEquals(listOf("ສົມພອນ", "ໄປ"), words("ສົມພອນໄປ"))
        // A letter with no vowel after it ends the syllable before it: ທຸກ + ມື້, never ທຸ + ກມ "km" + ື້.
        assertEquals(listOf("ທຸກ", "ມື້"), words("ທຸກມື້"))
        // Nor is a word read out of the middle of one: ເພີ່ມ, not ເ + ພີ່ + ມ; ຂ້າງ, not ຂ້າ "kill" + ງ.
        assertEquals(listOf("ເພີ່ມ", "ຂ້າງ"), words("ເພີ່ມຂ້າງ"))
        // A letter alone is no word, though the dictionary has it: ດ້ວຍ is one word, not ດ + ້ວຍ.
        assertEquals(listOf("ລາວ", "ດ້ວຍ"), words("ລາວດ້ວຍ"))
    }

    /** ວ and ອ after a letter are its vowel or its second letter (ຄວາມ, ສວນ, ຄອງ); ຫ goes with the letter it leads. */
    @Test
    fun syllablesKeepTheirLettersTogether() {
        Lexicons.set("lo", Lexicon(listOf("ຂອງ", "ຄວາມ", "ຢູ່", "ສວນ", "ຫາ", "ກວ່າ", "ຫາກ", "ວ່າ", "ວາມ", "ຫຼາຍ", "ຄົນ", "ງາມ"), mapOf("ຫາກ" to 300, "ວ່າ" to 10, "ຫາ" to 50, "ກວ່າ" to 900)))
        assertEquals(listOf("ຂອງ", "ຄວາມ"), words("ຂອງຄວາມ"))
        assertEquals(listOf("ຢູ່", "ສວນ"), words("ຢູ່ສວນ"))
        assertEquals(listOf("ຫຼາຍ", "ຄົນ", "ງາມ"), words("ຫຼາຍຄົນງາມ"))
        // The letter after a vowel written before it has its vowel: ເທດ ends with ດ, and a word may end there.
        Lexicons.set("lo", Lexicon(listOf("ປະ", "ປະເທດ", "ໄທ", "ເທດໄທ"), mapOf("ປະເທດ" to 150, "ໄທ" to 40, "ປະ" to 900)))
        assertEquals(listOf("ປະເທດ", "ໄທ"), words("ປະເທດໄທ"))
        // Where the syllables can be cut either way, the likelier words win: ຫາກ + ວ່າ "if", not ຫາ + ກວ່າ.
        assertEquals(listOf("ຫາກ", "ວ່າ"), words("ຫາກວ່າ"))
    }

    /** ບໍ່ joins no word after it, ວ່າ and ຢູ່ none before them, however the lexicon has them; ກໍ່ begins words of its own. */
    @Test
    fun grammaticalWordsStandApart() {
        val ranks = mapOf("ບໍ່" to 20, "ດີ" to 100, "ບໍ່ດີ" to 3000, "ຄິດ" to 300, "ວ່າ" to 10, "ຄິດວ່າ" to 900, "ອາໃສ" to 800, "ຢູ່" to 30, "ອາໃສຢູ່" to 2000)
        Lexicons.set("lo", Lexicon(ranks.keys + "ຕໍ່ໄປ" + "ຕໍ່" + "ໄປ" + "ກໍ່" + "ສ້າງ" + "ກໍ່ສ້າງ", ranks))
        assertEquals(listOf("ບໍ່", "ດີ"), words("ບໍ່ດີ"))
        assertEquals(listOf("ຄິດ", "ວ່າ"), words("ຄິດວ່າ"))
        assertEquals(listOf("ອາໃສ", "ຢູ່"), words("ອາໃສຢູ່"))
        // Other words the lexicon has together stay one word.
        assertEquals(listOf("ຕໍ່ໄປ"), words("ຕໍ່ໄປ"))
        assertEquals(listOf("ກໍ່ສ້າງ"), words("ກໍ່ສ້າງ"))
    }

    /** Text is read in today's spelling however it was typed, and so is the lexicon's; ໆ and the Lao digits are no words. */
    @Test
    fun todaysSpellingAndNoWords() {
        Lexicons.set("lo", Lexicon(listOf("ໜັງສື", "ນ້ຳ", "ຫຼາຍ", "ສຳລັບ", "ເດັກ", "ນ້ອຍ", "ປີ").map(LaoParser()::lexiconWord)))
        assertEquals(listOf("ໜັງສື", "ສຳລັບ", "ນ້ຳ", "ຫຼາຍ"), words("ຫນັງສືສໍາລັບນໍ້າຫລາຍ"))
        assertEquals(listOf("ເດັກ", "ນ້ອຍ", "ປີ"), words("ເດັກນ້ອຍໆ ໒໐໒໖ ປີ"))
        assertEquals("ໜັງສື", lao.lowercase("ຫນັງສື"))
        assertEquals("ໝາ", LaoParser().lexiconWord("ຫມາ"))
    }
}
