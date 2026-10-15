package com.tayra.languages.core.domain.parse

import kotlin.concurrent.Volatile

/** The words a language written without spaces is cut into, lowercase. */
class Lexicon(words: Collection<String>) {
    private val words: Set<String> = words.toHashSet()

    /** The longest word, in characters: no longer stretch is ever tried. */
    val maxLength: Int = words.maxOfOrNull { it.length }?.coerceAtMost(MAX_WORD) ?: 0

    val size: Int get() = words.size

    operator fun contains(word: String): Boolean = word in words

    private companion object {
        const val MAX_WORD = 16
    }
}

/**
 * The lexicons of the languages read without spaces, by language code, set by the
 * LexiconService before a text is parsed; a parser without its lexicon still parses, more coarsely.
 */
object Lexicons {
    @Volatile
    private var byLanguage: Map<String, Lexicon> = emptyMap()

    operator fun get(code: String): Lexicon? = byLanguage[code]

    fun set(code: String, lexicon: Lexicon?) {
        byLanguage = if (lexicon == null) byLanguage - code else byLanguage + (code to lexicon)
    }
}
