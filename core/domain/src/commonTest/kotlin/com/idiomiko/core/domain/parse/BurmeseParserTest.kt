package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BurmeseParserTest {
    private val burmese = LanguageDefinitions.all.single { it.language.name == "Burmese" }.language

    @AfterTest
    fun forget() = Lexicons.set("my", null)

    private fun words(text: String) = burmese.parseTokens(text).filter { it.isWord }.map { it.token }

    /** The lexicon's တ "one", ယ, က and ဘာ "what" never end inside a syllable: not before a sign, an asat or after a virama. */
    @Test
    fun aWordNeverEndsInsideASyllable() {
        Lexicons.set("my", Lexicon(listOf("သူ", "က", "တ", "ယ", "ကျောင်း", "သွား", "တယ်", "ကမ္ဘာ", "ဘာ", "ကြီး", "မင်္ဂလာ", "ပါ", "မ")))
        // ကျောင်း is no က + ျောင်း, and တယ် no တ + ယ်.
        assertEquals(listOf("သူ", "ကျောင်း", "သွား", "တယ်"), words("သူကျောင်းသွားတယ်"))
        // The stacked ဘ of ကမ္ဘာ belongs to its syllable: no ကမ် + ဘာ, no က + မ္ဘာ.
        assertEquals(listOf("ကမ္ဘာ", "ကြီး"), words("ကမ္ဘာကြီး"))
        // The kinzi stays with its syllable: no မ + င်္ဂလာ.
        assertEquals(listOf("မင်္ဂလာ", "ပါ"), words("မင်္ဂလာပါ"))
    }

    /** Letters no word covers go a syllable at a time and are one word together, as a name the lexicon lacks is. */
    @Test
    fun lettersNoWordCoversAreOneWord() {
        Lexicons.set("my", Lexicon(listOf("သွား", "တယ်", "ဂျ")))
        assertEquals(listOf("ဂျွန်", "သွား", "တယ်"), words("ဂျွန်သွားတယ်"))
    }

    /** Where the syllables can be cut either way, the likelier words win. */
    @Test
    fun theLikelierWordsWin() {
        val ranks = mapOf("ဒီ" to 20, "နေ့" to 120, "ဒီနေ့" to 150, "ကျွန်တော်" to 30, "ကျွန်" to 4000, "တော်" to 300, "စား" to 90, "တော်စား" to 40000)
        Lexicons.set("my", Lexicon(ranks.keys, ranks))
        assertEquals(listOf("ဒီနေ့"), words("ဒီနေ့"))
        assertEquals(listOf("ကျွန်တော်", "စား"), words("ကျွန်တော်စား"))
    }

    /** Text is read Unicode-normalised, and so is the lexicon; digits are no words, and ။ ends a sentence. */
    @Test
    fun normalisedTextDigitsAndSentences() {
        Lexicons.set("my", Lexicon(listOf("တောင်", "မြင့်", "ဝင်", "ခုနှစ်", "အိမ်", "ထဲ").map(BurmeseParser()::lexiconWord)))
        // Asat before dot below is read as dot below before asat, as NFC orders them.
        assertEquals(listOf("တောင်", "မြင့်"), words("တောင်မြင\u103A\u1037"))
        // ၀ (zero) between letters is the ဝ (wa) it is mistyped for; in a number it stays a digit.
        assertEquals(listOf("အိမ်", "ထဲ", "ဝင်"), words("အိမ်ထဲ၀င်"))
        assertEquals(listOf("ခုနှစ်"), words("၂၀၂၆ ခုနှစ်"))
        assertEquals("ဝင်", burmese.lowercase("၀င်"))
        assertEquals("ဦး", BurmeseParser().lexiconWord("ဦး"))
        val tokens = burmese.parseTokens("တောင်မြင့်။ ဝင်")
        assertTrue(tokens.single { "။" in it.token }.isEndOfSentence)
    }
}
