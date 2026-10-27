package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.TermRepository
import kotlin.math.abs

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

    /**
     * Where [term] stands in [sentence], or failing that a form of its family: a parent, then a
     * parent's other children and its own. A sentence reaches a term through its family too (a
     * lemma's sentence goes to the forms that follow it), so the form read in it may be another one.
     */
    suspend fun familyRange(sentence: String, term: Term, terms: TermRepository): IntRange? {
        range(sentence, term.displayText)?.let { return it }
        term.parents.forEach { parent -> range(sentence, parent.displayText)?.let { return it } }
        val relatives = term.parents.flatMap { terms.children(it.id) } + terms.children(term.id)
        relatives.forEach { relative -> if (relative.id != term.id) range(sentence, relative.displayText)?.let { return it } }
        return null
    }

    /**
     * Where a card blanks [term] out of [sentence]: its family's forms first, then a word that only
     * ends differently ("turma" for "turmas"), as an example chosen for the card may hold.
     */
    suspend fun cardRange(sentence: String, term: Term, terms: TermRepository): IntRange? =
        familyRange(sentence, term, terms) ?: nearRange(sentence, term.displayText)

    /** The first whole word of [sentence] that starts like the single word [word] and is at most two letters longer or shorter. */
    fun nearRange(sentence: String, word: String): IntRange? {
        val needle = word.trim().lowercase()
        if (needle.length < NEAR_MIN || needle.any(Char::isWhitespace)) return null
        val stem = needle.dropLast(1)
        return WORD.findAll(sentence).firstOrNull { match ->
            val lower = match.value.lowercase()
            lower.startsWith(stem) && abs(lower.length - needle.length) <= NEAR_SLACK
        }?.range
    }

    private val WORD = Regex("""[\p{L}\p{M}\p{Nd}'’-]+""")
    private const val NEAR_MIN = 5
    private const val NEAR_SLACK = 2
}
