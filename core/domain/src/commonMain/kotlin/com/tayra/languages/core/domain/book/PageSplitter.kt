package com.tayra.languages.core.domain.book

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.PageSplitMode
import com.tayra.languages.core.domain.parse.PARAGRAPH_MARK
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.parseTokens

/**
 * Splits a book's full text into pages of roughly [wordsPerPage] words, breaking at
 * sentence or paragraph boundaries. Lines consisting only of `---` force a page break.
 */
object PageSplitter {

    fun split(text: String, language: Language, splitBy: PageSplitMode, wordsPerPage: Int): List<String> {
        val pages = mutableListOf<String>()
        for (segment in splitAtPageBreaks(text)) {
            val tokens = language.parseTokens(segment)
            for (group in groupTokens(tokens, splitBy, wordsPerPage)) {
                val pageText = group.joinToString("") { it.token }
                    .replace("\r", "")
                    .replace(PARAGRAPH_MARK, "\n")
                    .trim()
                if (pageText.isNotEmpty()) pages.add(pageText)
            }
        }
        return pages
    }

    /** Breaks the full text at lines consisting of `---` only. */
    fun splitAtPageBreaks(text: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        for (line in text.split("\n")) {
            if (line.trim() == "---") {
                segments.add(current.toString().trim())
                current.clear()
            } else {
                current.append(line).append('\n')
            }
        }
        if (current.isNotEmpty()) segments.add(current.toString().trim())
        return segments
    }

    /**
     * Groups tokens by sentence or paragraph, each group holding at least [threshold]
     * word tokens (except possibly the last).
     */
    fun groupTokens(tokens: List<ParsedToken>, splitBy: PageSplitMode, threshold: Int): List<List<ParsedToken>> {
        val groups = mutableListOf<List<ParsedToken>>()
        var currentGroup = mutableListOf<ParsedToken>()
        var buffer = mutableListOf<ParsedToken>()

        fun isDelimiter(token: ParsedToken) = when (splitBy) {
            PageSplitMode.SENTENCES -> token.isEndOfSentence
            PageSplitMode.PARAGRAPHS -> token.isEndOfParagraph
        }

        for (token in tokens) {
            buffer.add(token)
            if (isDelimiter(token)) {
                currentGroup.addAll(buffer)
                buffer = mutableListOf()
                val wordCount = currentGroup.count { it.isWord }
                if (wordCount > threshold) {
                    groups.add(trimParagraphMarks(currentGroup))
                    currentGroup = mutableListOf()
                }
            }
        }
        if (buffer.isNotEmpty()) currentGroup.addAll(buffer)
        val last = trimParagraphMarks(currentGroup)
        if (last.isNotEmpty()) groups.add(last)
        return groups
    }

    private fun trimParagraphMarks(tokens: List<ParsedToken>): List<ParsedToken> =
        tokens.dropWhile { it.isEndOfParagraph }.dropLastWhile { it.isEndOfParagraph }
}
