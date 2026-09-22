package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING

/**
 * A general parser for space-delimited languages such as English, French or Spanish.
 */
open class SpaceDelimitedParser : TextParser {

    override val name: String get() = "Space Delimited"

    override fun parse(text: String, language: Language): List<ParsedToken> {
        val cleanText = text
            .replace(MULTIPLE_SPACES, " ")
            .replace(ZWS_STRING, "")
        return parseToTokens(cleanText, language)
    }

    private fun parseToTokens(text: String, language: Language): List<ParsedToken> {
        val prepared = applyCharacterSubstitutions(text, language.characterSubstitutions)
            .replace("\r\n", "\n")
            .replace('{', '[')
            .replace('}', ']')

        val tokens = mutableListOf<ParsedToken>()
        val paragraphs = prepared.split("\n")
        paragraphs.forEachIndexed { index, paragraph ->
            parseParagraph(paragraph, language, tokens)
            if (index != paragraphs.lastIndex) {
                tokens.add(ParsedToken(PARAGRAPH_MARK, isWord = false, isEndOfSentence = true))
            }
        }
        return tokens
    }

    private fun parseParagraph(text: String, language: Language, tokens: MutableList<ParsedToken>) {
        val termChars = normalizeRegexEscapes(language.wordCharacters.trim()).ifEmpty { DEFAULT_WORD_CHARACTERS }
        val splitExceptions = language.exceptionsSplitSentences.replace(".", "\\.")
        val pattern = if (splitExceptions.isBlank()) "([$termChars]*)" else "($splitExceptions|[$termChars]*)"
        val wordRegex = Regex(pattern, RegexOption.IGNORE_CASE)

        val splitChars = language.regexpSplitSentences.trim().ifEmpty { DEFAULT_SPLIT_SENTENCES }
        val endOfSentenceRegex = Regex("[${escapeForCharacterClass(splitChars)}]")

        fun addNonWords(s: String) {
            if (s.isEmpty()) return
            tokens.add(ParsedToken(s, isWord = false, isEndOfSentence = endOfSentenceRegex.containsMatchIn(s)))
        }

        var position = 0
        for (match in wordRegex.findAll(text)) {
            if (match.value.isEmpty()) continue
            addNonWords(text.substring(position, match.range.first))
            tokens.add(ParsedToken(match.value, isWord = true))
            position = match.range.last + 1
        }
        addNonWords(text.substring(position))
    }

    companion object {
        private val MULTIPLE_SPACES = Regex(" +")

        /** Letters, marks, modifier symbols and format characters. */
        const val DEFAULT_WORD_CHARACTERS = """\p{L}\p{M}\p{Sk}\p{Cf}"""

        /** Common sentence terminators, including the CJK full stop and question/exclamation marks. */
        const val DEFAULT_SPLIT_SENTENCES = ".!?:。！？؟۔।॥"
    }
}

/** Handles the Turkish dotted/dotless i when lowercasing. */
class TurkishParser : SpaceDelimitedParser() {
    override val name: String get() = "Turkish"

    override fun lowercase(text: String): String =
        text.replace("İ", "i").replace("I", "ı").lowercase()
}
