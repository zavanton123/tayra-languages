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

/**
 * How a morphological analyser (MeCab's way) cuts a stretch of letters: [pieces] are the offsets
 * where its morphemes start, [words] those where a word starts, a word being a morpheme that can
 * stand alone with the pieces after it that cannot (食べ・まし・た; 選ん・で・ください).
 */
class Analysis(val pieces: Set<Int>, val words: Set<Int>)

/** A language's morphological analyser; null when it cannot tell, as before it has loaded. */
fun interface WordBoundaries {
    fun of(run: String): Analysis?
}

/** The analysers of the languages that have one on this platform, by code: Japanese on desktop. */
object Segmenters {
    private val byCode = HashMap<String, WordBoundaries>()

    operator fun get(code: String): WordBoundaries? = byCode[code]

    fun set(code: String, boundaries: WordBoundaries?) {
        if (boundaries == null) byCode.remove(code) else byCode[code] = boundaries
    }
}
