package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertTrue

/** The spellings each language's ported text processors give a word. */
class TextProcessorsTest {
    private fun spellings(code: String, word: String) = Morphology.forLanguage(code)!!.preprocess(word)

    @Test
    fun german() {
        assertTrue("Straße" in spellings("de", "Strasse"))
        assertTrue("Strasse" in spellings("de", "Straße"))
    }

    @Test
    fun french() {
        assertTrue("l\u2019homme" in spellings("fr", "l'homme"))
    }

    @Test
    fun greek() {
        assertTrue("πρόσωπο" in spellings("el", "πρόσωπό"))
    }

    @Test
    fun latin() {
        assertTrue("puellæ" in spellings("la", "puellae"))
        assertTrue("caelum" in spellings("la", "cælum"))
    }

    @Test
    fun ukrainian() {
        assertTrue("мама" in spellings("uk", "ма\u0301ма"))
        assertTrue("п'ять" in spellings("uk", "п\u2019ять"))
    }

    @Test
    fun russian() {
        assertTrue("еще" in spellings("ru", "ещё"))
        assertTrue("ёлка" in spellings("ru", "елка"))
        assertTrue("слово" in spellings("ru", "сло\u0301во"))
    }

    @Test
    fun serboCroatian() {
        assertTrue("čas" in spellings("sr", "cas"))
        assertTrue("đak" in spellings("hr", "djak"))
        assertTrue(spellings("sr", "pa\u030fs").any { Unicode.nfc(it) == "pas" })
    }

    @Test
    fun italian() {
        assertTrue("Italia" in spellings("it", "dell'Italia"))
    }

    @Test
    fun ancientGreek() {
        assertTrue("λογος" in spellings("grc", "logos"))
    }

    @Test
    fun vietnamese() {
        assertTrue("hòa" in spellings("vi", "hoà"))
        assertTrue("hoà" in spellings("vi", "hòa"))
    }

    @Test
    fun arabic() {
        assertTrue("ولد" in spellings("ar", "وَلَدَ"))
        assertTrue("لكن" in spellings("ar", "لـكن"))
        assertTrue("أكبر" in spellings("ar", "اكبر"))
        assertTrue("ي" in spellings("ar", "\ufef4"))
    }

    @Test
    fun egyptianArabic() {
        assertTrue("لغة" in spellings("arz", "لغه"))
    }

    @Test
    fun yiddish() {
        assertTrue(spellings("yi", "פֿאָתּ").single() == "פאת")
    }

    @Test
    fun assyrian() {
        assertTrue("ܟܬܒ" in spellings("aii", "ܟܵܬܹܒ݂"))
    }

    @Test
    fun chinese() {
        assertTrue("\u4e00" in spellings("zh", "\u2f00"))
        assertTrue("\u4e00" in spellings("yue", "\u2f00"))
    }
}
