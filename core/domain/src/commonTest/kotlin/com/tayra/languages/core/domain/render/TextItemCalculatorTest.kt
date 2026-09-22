package com.tayra.languages.core.domain.render

import com.tayra.languages.core.domain.TestLanguages
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.numbered
import com.tayra.languages.core.domain.term.TermTextNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals

class TextItemCalculatorTest {

    private val english = TestLanguages.english

    private fun tokens(vararg data: String): List<ParsedToken> =
        data.map { ParsedToken(it, isWord = it.isNotBlank() && it != ".") }.numbered()

    private fun terms(vararg texts: String): List<Term> =
        texts.mapIndexed { i, t -> TermTextNormalizer.termFromText(english, t).copy(id = i + 1L) }

    private fun render(tokens: List<ParsedToken>, terms: List<Term>): Pair<String, String> {
        val items = TextItemCalculator.calculate(tokens, terms, english).items
        val all = items.joinToString("") { "[${it.text}-${it.tokenCount}]" }.replace(ZWS_STRING, "")
        val shown = items.joinToString("") { "[${it.displayText}-${it.tokenCount}]" }.replace(ZWS_STRING, "")
        return all to shown
    }

    @Test
    fun simpleRender() {
        val (all, _) = render(tokens("some", " ", "data", " ", "here", "."), emptyList())
        assertEquals("[some-1][ -1][data-1][ -1][here-1][.-1]", all)
    }

    @Test
    fun nonMatchingAndPartialTermsAreIgnored() {
        val toks = tokens("some", " ", "data", " ", "here", ".")
        assertEquals("[some-1][ -1][data-1][ -1][here-1][.-1]", render(toks, terms("ignoreme")).first)
        assertEquals("[some-1][ -1][data-1][ -1][here-1][.-1]", render(toks, terms("data he")).first)
    }

    @Test
    fun multiwordTermsCoverOtherItems() {
        val (all, _) = render(tokens("some", " ", "data", " ", "here", "."), terms("data here"))
        assertEquals("[some-1][ -1][data here-3][.-1]", all)
    }

    @Test
    fun caseNotConsideredForMatches() {
        val (all, _) = render(tokens("Some", " ", "DATA", " ", "here", "."), terms("data here"))
        assertEquals("[Some-1][ -1][DATA here-3][.-1]", all)
    }

    @Test
    fun overlappingTermsShowRemainingTokens() {
        val toks = tokens("A", " ", "B", " ", "C", " ", "D", " ", "E")
        val (all, shown) = render(toks, terms("B C", "C D E"))
        assertEquals("[A-1][ -1][B C-3][C D E-5]", all)
        assertEquals("[A-1][ -1][B C-3][ D E-5]", shown)
    }

    @Test
    fun documentedExample() {
        val toks = tokens("A", " ", "B", " ", "C", " ", "D", " ", "E", " ", "F", " ", "G", " ", "H", " ", "I")
        val (_, shown) = render(toks, terms("B C", "E F G H I", "F G", "C D E"))
        assertEquals("[A-1][ -1][B C-3][ D E-5][ F G H I-9]", shown)
    }

    @Test
    fun unknownWordsGetStatusZeroTerms() {
        val result = TextItemCalculator.calculate(tokens("Hello", " ", "there"), terms("hello"), english)
        assertEquals(listOf("there"), result.newTerms.map { it.text })
        assertEquals(TermStatus.UNKNOWN, result.newTerms.single().status)
        assertEquals(listOf(TermStatus.NEW_1, TermStatus.UNKNOWN), result.items.filter { it.isWord }.map { it.status })
    }

    @Test
    fun paragraphsAndSentencesAreGrouped() {
        val toks = listOf(
            ParsedToken("Hi", true), ParsedToken(".", false, true), ParsedToken(" ", false), ParsedToken("Bye", true),
            ParsedToken(".", false, true), ParsedToken("¶", false, true), ParsedToken("Next", true),
        ).numbered()
        val page = TextItemCalculator.toPage(TextItemCalculator.calculate(toks, emptyList(), english).items)
        assertEquals(2, page.paragraphs.size)
        assertEquals(listOf(0, 1), page.paragraphs[0].sentences.map { it.number })
        assertEquals("Next", page.paragraphs[1].sentences.single().items.single().text)
        assertEquals(true, page.paragraphs[0].sentences[1].items.first().isSentenceStart)
    }
}
