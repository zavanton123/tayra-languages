package com.tayra.languages.core.domain.render

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.term.TermTextNormalizer

/**
 * Given parsed tokens and the terms found in them, determines which [TextItem]s are
 * actually rendered, resolving overlaps between multi-word terms.
 *
 * Example: for tokens `A B C D E F G H I` and terms "B C", "E F G H I", "F G", "C D E",
 * the rendered items are `[A][B C][-D E][-F G H I]` where `-` marks tokens hidden by
 * an earlier, overlapping term.
 */
object TextItemCalculator {

    /**
     * @param tokens the page tokens (with sentence numbers assigned).
     * @param terms the saved terms present in the tokens.
     * @return the items to render, plus the new (unsaved, status 0) terms created for
     *   word tokens without a saved term.
     */
    fun calculate(tokens: List<ParsedToken>, terms: List<Term>, language: Language): Result {
        val newTerms = createMissingUnknownTerms(tokens, terms, language)
        val allTerms = terms + newTerms
        val termsByTextLc = allTerms.associateBy { it.textLc }

        val tokensOrig = tokens.map { it.token }
        val tokensLc = tokensOrig.map { language.lowercase(it) }

        val items = mutableListOf<TextItem>()
        fun addItem(index: Int, count: Int) {
            val textOrig = if (count > 1) tokensOrig.subList(index, index + count).joinToString(ZWS_STRING) else tokensOrig[index]
            val textLc = tokensLc.subList(index, index + count).joinToString(ZWS_STRING)
            items.add(
                TextItem(
                    index = index,
                    text = textOrig,
                    textLc = textLc,
                    tokenCount = count,
                    sentenceNumber = tokens[index].sentenceNumber,
                    term = termsByTextLc[textLc],
                ),
            )
        }

        for (index in tokens.indices) addItem(index, 1)

        val multiwordIndex = MultiwordTermIndex(allTerms.filter { it.tokenCount > 1 }.map { it.textLc })
        for ((textLc, index) in multiwordIndex.findAll(tokensLc)) {
            addItem(index, termsByTextLc.getValue(textLc).tokenCount)
        }

        // Sort by index, then decreasing token count, so the longest term starting at a
        // position takes precedence when "written out" to the output array.
        val sorted = items.sortedWith(compareBy({ it.index }, { -it.tokenCount }))
        val output = IntArray(tokens.size) { -1 }
        for (slot in sorted.indices.reversed()) {
            val item = sorted[slot]
            for (c in item.index until item.index + item.tokenCount) output[c] = slot
        }

        val counts = IntArray(sorted.size)
        for (slot in output) if (slot >= 0) counts[slot]++

        val rendered = sorted.filterIndexed { slot, item ->
            item.displayCount = counts[slot]
            counts[slot] > 0
        }

        var paragraph = 0
        for (item in rendered) {
            item.paragraphNumber = paragraph
            if (item.isParagraphMark) paragraph++
        }

        return Result(rendered, newTerms)
    }

    /** Creates status-0 terms for word tokens that have no saved term, using the case of the last instance. */
    private fun createMissingUnknownTerms(tokens: List<ParsedToken>, terms: List<Term>, language: Language): List<Term> {
        val existing = terms.mapTo(HashSet()) { it.textLc }
        val missingByLc = LinkedHashMap<String, String>()
        for (token in tokens) {
            if (!token.isWord) continue
            val lc = language.lowercase(token.token)
            if (lc !in existing) missingByLc[lc] = token.token
        }
        return missingByLc.values.map { TermTextNormalizer.termFromToken(language, it).copy(status = TermStatus.UNKNOWN) }
    }

    /** Groups rendered items into paragraphs and sentences. */
    fun toPage(items: List<TextItem>): RenderedPage {
        val paragraphs = mutableListOf<RenderedParagraph>()
        var current = mutableListOf<TextItem>()
        fun flush() {
            paragraphs.add(RenderedParagraph(paragraphs.size, groupBySentence(current)))
            current = mutableListOf()
        }
        for (item in items) {
            if (item.isParagraphMark) flush() else current.add(item)
        }
        if (current.isNotEmpty()) flush()
        return RenderedPage(paragraphs)
    }

    private fun groupBySentence(items: List<TextItem>): List<RenderedSentence> {
        val sentences = mutableListOf<RenderedSentence>()
        var currentNumber: Int? = null
        var current = mutableListOf<TextItem>()
        for (item in items) {
            if (currentNumber != null && item.sentenceNumber != currentNumber) {
                sentences.add(RenderedSentence(currentNumber, current))
                current = mutableListOf()
            }
            currentNumber = item.sentenceNumber
            current.add(item)
        }
        if (currentNumber != null) sentences.add(RenderedSentence(currentNumber, current))
        sentences.forEach { sentence -> sentence.items.firstOrNull()?.isSentenceStart = true }
        return sentences
    }

    data class Result(val items: List<TextItem>, val newTerms: List<Term>)
}
