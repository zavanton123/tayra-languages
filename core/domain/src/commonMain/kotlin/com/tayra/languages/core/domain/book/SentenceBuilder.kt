package com.tayra.languages.core.domain.book

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Sentence
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.parse.parseTokens

/**
 * Builds the sentences of a page. Sentences are stored so that term usages can be
 * found later ("sentences" tab of the term form).
 */
object SentenceBuilder {

    fun build(text: String, language: Language): List<Sentence> = fromTokens(language.parseTokens(text), language)

    fun fromTokens(tokens: List<ParsedToken>, language: Language): List<Sentence> {
        val sentences = mutableListOf<Sentence>()
        var current = mutableListOf<ParsedToken>()
        fun flush() {
            if (current.isNotEmpty()) {
                val joined = ZWS_STRING + current.joinToString(ZWS_STRING) { it.token }.trim(' ') + ZWS_STRING
                sentences.add(Sentence(order = sentences.size + 1, text = joined, textLc = language.lowercase(joined)))
            }
            current = mutableListOf()
        }
        for (token in tokens) {
            current.add(token)
            if (token.isEndOfSentence) flush()
        }
        flush()
        return sentences
    }

    fun wordCount(text: String, language: Language): Int =
        if (text.isBlank()) 0 else language.parseTokens(text).count { it.isWord }
}
