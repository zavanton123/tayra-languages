package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.TestLanguages
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.model.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceDelimitedParserTest {

    private val parser = SpaceDelimitedParser()

    private fun tokensAsString(text: String, language: Language): String =
        parser.parse(text, language).joinToString("") { if (it.isWord) "[${it.token}]" else it.token }

    private fun tokenTriples(text: String, language: Language) =
        parser.parse(text, language).map { Triple(it.token, it.isWord, it.isEndOfSentence) }

    @Test
    fun endOfSentenceIsMarked() {
        val expected = listOf(
            Triple("Tengo", true, false), Triple(" ", false, false), Triple("un", true, false),
            Triple(" ", false, false), Triple("gato", true, false), Triple(".", false, true),
            Triple("¶", false, true),
            Triple("Tengo", true, false), Triple(" ", false, false), Triple("dos", true, false),
            Triple(".", false, true),
        )
        assertEquals(expected, tokenTriples("Tengo un gato.\nTengo dos.", TestLanguages.spanish))
    }

    @Test
    fun exceptionsDoNotSplitSentences() {
        val expected = listOf(
            Triple("1. ", false, true), Triple("Mrs.", true, false), Triple(" ", false, false),
            Triple("Jones", true, false), Triple(" ", false, false), Triple("is", true, false),
            Triple(" ", false, false), Triple("here", true, false), Triple(".", false, true),
        )
        assertEquals(expected, tokenTriples("1. Mrs. Jones is here.", TestLanguages.english))
    }

    @Test
    fun multiDotExceptionIsOneToken() {
        val spanish = TestLanguages.spanish.copy(exceptionsSplitSentences = "EE.UU.")
        assertEquals("[Estamos] [en] [EE.UU.] [hola].", tokensAsString("Estamos en EE.UU. hola.", spanish))
        assertEquals("[EE.UU.]", tokensAsString("EE.UU.", spanish))
    }

    @Test
    fun aTrailingBarInTheExceptionsKeepsTheWords() {
        val dutch = TestLanguages.spanish.copy(exceptionsSplitSentences = "mr.|nr.|")
        assertEquals("[Ik] [woon] [in] [Amsterdam], [nr.] 5.", tokensAsString("Ik woon in Amsterdam, nr. 5.", dutch))
    }

    @Test
    fun quickChecks() {
        val english = TestLanguages.english
        assertEquals("[test]", tokensAsString("test", english))
        assertEquals("[test].", tokensAsString("test.", english))
        assertEquals("\"[test].\"", tokensAsString("\"test.\"", english))
        assertEquals("[Hi] [there].", tokensAsString("Hi there.", english))
        assertEquals("[Hi] [there]. [Goodbye].", tokensAsString("Hi there.  Goodbye.", english))
        assertEquals("[Hi].¶[Goodbye].", tokensAsString("Hi.\nGoodbye.", english))
        assertEquals("[He]123[llo].", tokensAsString("He123llo.", english))
        assertEquals("1234", tokensAsString("1234", english))
        assertEquals("1234.[Hello]", tokensAsString("1234.Hello", english))
        assertEquals("[Tengo] [que] [y] [qué].", tokensAsString("Tengo que y qué.", TestLanguages.spanish))
    }

    @Test
    fun characterSubstitutionsApplied() {
        assertEquals("[l]'[homme]…", tokensAsString("l’homme...", TestLanguages.english))
    }

    @Test
    fun zeroWidthNonJoinerRetainedWhenWordCharacter() {
        assertEquals("[Brot‌zeit]", tokensAsString("Brot‌zeit", TestLanguages.german))
    }

    @Test
    fun defaultWordPatternHandlesUnicodeLetters() {
        val generic = TestLanguages.generic
        assertEquals("[Saint] [Denis] [be] [my] [speed]!—[donc] [vôtre] [est] [France], [et] [vous] [êtes] [mienne].",
            tokensAsString("Saint Denis be my speed!—donc vôtre est France, et vous êtes mienne.", generic))
        assertEquals("[Встреча] [с] [медведем].", tokensAsString("Встреча с медведем.", generic))
        assertEquals("[नमस‍ते]", tokensAsString("नमस‍ते", generic))
        assertEquals("[日本語]。", tokensAsString("日本語。", generic))
    }

    @Test
    fun afrikaansWordsKeepTheirAccentsAndApostrophes() {
        val afrikaans = LanguageDefinitions.all.first { it.name == "Afrikaans" }.language
        assertEquals(
            "[Sy] [kom] [ná] [die] [ete]. [Dít] [is] ['n] [appèl] [vir] [ma's] [oor] [die] [brûe] [en] [ŉ] [wîe].",
            tokensAsString("Sy kom ná die ete. Dít is ’n appèl vir ma's oor die brûe en ŉ wîe.", afrikaans),
        )
    }

    @Test
    fun nepaliKeepsPostpositionsJoinedEyelashRaAndNoDigits() {
        val nepali = LanguageDefinitions.all.single { it.name == "Nepali" }.language
        assertEquals(
            "[रामको] [घरमा] २०८० [सालमा] [गर्\u200Dयो]। [तपाईंलाई] [कस्तो] [छ]?",
            tokensAsString("रामको घरमा २०८० सालमा गर्\u200Dयो। तपाईंलाई कस्तो छ?", nepali),
        )
    }

    @Test
    fun amharicWordsAreCutAtItsWordSpaceAndKeepTheirGeminationMark() {
        val amharic = LanguageDefinitions.all.first { it.name == "Amharic" }.language
        assertEquals("[ልጁ]፡[ቤት]፡[ገባ]። [ሰ፟ማ]፣ [ፚ]!", tokensAsString("ልጁ፡ቤት፡ገባ። ሰ፟ማ፣ ፚ!", amharic))
        assertEquals(listOf(true, false), parser.parse("ገባ። ቤቱ", amharic).filter { !it.isWord }.map { it.isEndOfSentence })
    }

    @Test
    fun legacyHexEscapesAreConverted() {
        val arabic = TestLanguages.generic.copy(wordCharacters = "\\x{0600}-\\x{06FF}")
        assertEquals("[مرحبا] [بكم]", tokensAsString("مرحبا بكم", arabic))
    }

    @Test
    fun turkishLowercase() {
        assertEquals("ırmak iyi", TurkishParser().lowercase("IRMAK İYİ"))
    }

    @Test
    fun marathiKeepsVowelSignsAndTheEyelashRaInWordsAndLeavesNumbersOut() {
        val marathi = LanguageDefinitions.all.single { it.name == "Marathi" }.language
        assertEquals(
            "[दुसऱ्या] [दिवशी] [ती] [शाळेत] [गेली]. [मी] १० [वाजता] [घरी] [आलो]।",
            tokensAsString("दुसऱ्या दिवशी ती शाळेत गेली. मी १० वाजता घरी आलो।", marathi),
        )
        // An eyelash ra typed with the joiner (र + virama + ZWJ) is one word too.
        assertEquals("[दुसर\u094D\u200Dया]", tokensAsString("दुसर\u094D\u200Dया", marathi))
    }

    @Test
    fun malayCutsReduplicationsAndKeepsTitles() {
        val malay = LanguageDefinitions.all.single { it.name == "Malay" }.language
        assertEquals(
            "[Dr.] [Aminah] [membeli] [buku]-[buku] [baharu]. [Kanak]-[kanak] [suka]!",
            tokensAsString("Dr. Aminah membeli buku-buku baharu. Kanak-kanak suka!", malay),
        )
    }

    @Test
    fun sinhalaKeepsItsConjunctsAndDropsTheNonJoiner() {
        val sinhala = LanguageDefinitions.all.single { it.name == "Sinhala" }.language
        // The joiner after al-lakuna writes ප්‍ර and ය්‍ය as one letter of the word; the non-joiner after it (දක්‌වා) is no break.
        assertEquals(
            "[ප්\u200Dරශ්නය] [අවශ්\u200Dය] [නම්] [ගෙදර] [දක්වා] [එන්න]! [ඔයා] [කොහෙද]?",
            tokensAsString("ප්\u200Dරශ්නය අවශ්\u200Dය නම් ගෙදර දක්\u200Cවා එන්න! ඔයා කොහෙද?", sinhala),
        )
    }

    @Test
    fun zuluKeepsPrefixedNamesWholeAndCutsAtHyphens() {
        val zulu = LanguageDefinitions.all.single { it.name == "Zulu" }.language
        assertEquals(
            "[uMnu.] [Dlamini] [uhlala] [eGoli] [noThemba]. [Ngakho]-[ke] [ufunda] [isiZulu] [e]-[KwaZulu]-[Natal]!",
            tokensAsString("uMnu. Dlamini uhlala eGoli noThemba. Ngakho-ke ufunda isiZulu e-KwaZulu-Natal!", zulu),
        )
    }

    @Test
    fun azerbaijaniLowercase() {
        assertEquals("ışıq indi", AzerbaijaniParser().lowercase("Işıq İndi"))
        assertEquals("indi", AzerbaijaniParser().lowercase("i\u0307ndi"), "the dot an İ left when lowercased as other languages do")
        assertEquals("azerbaijani", LanguageDefinitions.all.first { it.name == "Azerbaijani" }.language.parserType)
    }

    @Test
    fun tamilKeepsVowelSignsAndViramaInTheWord() {
        val tamil = LanguageDefinitions.all.single { it.name == "Tamil" }.language
        assertEquals(
            "[நான்] [பள்ளிக்குப்] [போகிறேன்]. [அம்மா] [வீட்டில்] [இருக்கிறார்]!",
            tokensAsString("நான் பள்ளிக்குப் போகிறேன். அம்மா வீட்டில் இருக்கிறார்!", tamil),
        )
    }

    @Test
    fun gujaratiKeepsVowelSignsViramaAndEndingsInTheWord() {
        val gujarati = LanguageDefinitions.all.single { it.name == "Gujarati" }.language
        assertEquals(
            "[રામનો] [ભાઈ] [ઘરમાં] [છે]. [ડૉ.] [મહેતા] [સ્કૂલે] [ગયાં]!",
            tokensAsString("રામનો ભાઈ ઘરમાં છે. ડૉ. મહેતા સ્કૂલે ગયાં!", gujarati),
        )
    }

    @Test
    fun numberedAssignsSentences() {
        val tokens = parser.parse("Hi there. Bye.", TestLanguages.english).numbered()
        assertEquals(listOf("Hi", " ", "there", ". ", "Bye", "."), tokens.map { it.token })
        assertEquals(listOf(0, 0, 0, 0, 1, 1), tokens.map { it.sentenceNumber })
        assertEquals((1..6).toList(), tokens.map { it.order })
    }
}
