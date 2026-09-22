package com.tayra.languages.core.domain.parse

const val PARAGRAPH_MARK = "¶"

/**
 * A single token of a parsed text.
 *
 * [order] is the 1-based position in the parsed text, [sentenceNumber] the 0-based
 * index of the sentence the token belongs to.
 */
data class ParsedToken(
    val token: String,
    val isWord: Boolean,
    val isEndOfSentence: Boolean = false,
    val order: Int = 0,
    val sentenceNumber: Int = 0,
) {
    val isEndOfParagraph: Boolean get() = token.trim() == PARAGRAPH_MARK
}

/** Assigns [ParsedToken.order] and [ParsedToken.sentenceNumber] sequentially. */
fun List<ParsedToken>.numbered(): List<ParsedToken> {
    var sentence = 0
    return mapIndexed { index, token ->
        val numberedToken = token.copy(order = index + 1, sentenceNumber = sentence)
        if (token.isEndOfSentence) sentence++
        numberedToken
    }
}
