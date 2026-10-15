package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.parse.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageParsingTest {

    /** Each language's word characters and sentence exceptions make a pattern its parser can use. */
    @Test
    fun everyLanguageParsesAText() {
        val failing = (LanguageDefinitions.all.map { it.language }).mapNotNull { language ->
            runCatching<Any> { language.parser.parse("A text. Another one", language) }.exceptionOrNull()?.let { "${language.name}: ${it.message?.lineSequence()?.first()}" }
        }
        assertTrue(failing.isEmpty(), failing.joinToString("\n"))
    }

    /** Tibetan's words are its syllables' letters, parted by the tsheg. */
    @Test
    fun tibetanWordsArePartedByTheTsheg() {
        val tibetan = LanguageDefinitions.all.first { it.name == "Tibetan" }.language
        assertEquals(listOf("ང", "བོད", "ཡིག", "སློབ"), tibetan.parser.parse("ང་བོད་ཡིག་སློབ།", tibetan).filter { it.isWord }.map { it.token })
    }

    /** Characters beyond the Basic Multilingual Plane, written as Python writes them, are word characters. */
    @Test
    fun wordCharactersReachBeyondTheBasicPlane() {
        val language = com.tayra.languages.core.domain.model.Language(name = "Test", wordCharacters = "a-z\\U00020000-\\U0002A6DF")
        assertEquals(listOf("ab\uD842\uDFB7c"), language.parser.parse("ab\uD842\uDFB7c!", language).filter { it.isWord }.map { it.token })
    }
}
