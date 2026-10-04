package com.tayra.languages.core.domain.flashcards

/** Finding the word a card asks for inside the sentence it was read in. */
object Cloze {
    /** Where [word] first stands as a whole word in [sentence], ignoring case; null when it does not. */
    fun range(sentence: String, word: String): IntRange? {
        if (word.isEmpty()) return null
        var from = 0
        while (true) {
            val at = sentence.indexOf(word, from, ignoreCase = true)
            if (at < 0) return null
            val end = at + word.length
            val whole = (at == 0 || !sentence[at - 1].isLetterOrDigit()) && (end == sentence.length || !sentence[end].isLetterOrDigit())
            if (whole) return at until end
            from = end
        }
    }
}
