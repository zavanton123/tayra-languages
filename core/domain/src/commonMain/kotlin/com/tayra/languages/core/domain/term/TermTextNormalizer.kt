package com.tayra.languages.core.domain.term

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.PARAGRAPH_MARK
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.parse.parser

/**
 * Converts user-entered term text into the canonical tokenised form.
 */
object TermTextNormalizer {

    /** The tokenised text, tokens joined by ZWS, with paragraph marks removed. */
    fun normalize(language: Language, text: String): String {
        val cleaned = text.trim()
            .replace(ZWS_STRING, "")
            .replace(' ', ' ')
        val tokens = language.parser.parse(cleaned, language).filter { it.token != PARAGRAPH_MARK }
        return tokens.joinToString(ZWS_STRING) { it.token }
    }

    fun tokenCount(normalizedText: String): Int =
        if (normalizedText.isEmpty()) 0 else normalizedText.split(ZWS_STRING).size

    /** Builds a term specification (unsaved) by parsing the given text. */
    fun termFromText(language: Language, text: String): Term {
        val normalized = normalize(language, text)
        return Term(
            languageId = language.id,
            text = normalized,
            textLc = language.lowercase(normalized),
            romanization = language.parser.reading(normalized),
            tokenCount = tokenCount(normalized),
        )
    }

    /**
     * Builds a term for an already-parsed token, without re-parsing: some parsers
     * return different tokens for a word given out of context.
     */
    fun termFromToken(language: Language, token: String): Term = Term(
        languageId = language.id,
        text = token,
        textLc = language.lowercase(token),
        romanization = language.parser.reading(token),
        tokenCount = tokenCount(token),
    )
}
