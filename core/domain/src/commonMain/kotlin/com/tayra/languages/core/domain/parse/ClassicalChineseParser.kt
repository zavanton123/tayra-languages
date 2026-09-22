package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language

/**
 * Parser for languages where every character is a word (e.g. Classical Chinese).
 */
class ClassicalChineseParser : TextParser {

    override val name: String get() = "Classical Chinese"

    override fun parse(text: String, language: Language): List<ParsedToken> {
        val prepared = applyCharacterSubstitutions(text.replace(SPACES, ""), language.characterSubstitutions)
            .replace("\r\n", "\n")
            .replace('{', '[')
            .replace('}', ']')
            .replace("\n", PARAGRAPH_MARK)
            .trim()

        val wordRegex = Regex("[${normalizeRegexEscapes(language.wordCharacters)}]")
        val splitChars = language.regexpSplitSentences

        return codePoints(prepared).map { ch ->
            val isEndOfSentence = ch == PARAGRAPH_MARK || (ch.length == 1 && ch[0] in splitChars)
            ParsedToken(ch, isWord = wordRegex.matches(ch), isEndOfSentence = isEndOfSentence)
        }
    }

    private fun codePoints(text: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                result.add(text.substring(i, i + 2))
                i += 2
            } else {
                result.add(ch.toString())
                i++
            }
        }
        return result
    }

    private companion object {
        val SPACES = Regex("[ \t]+")
    }
}
