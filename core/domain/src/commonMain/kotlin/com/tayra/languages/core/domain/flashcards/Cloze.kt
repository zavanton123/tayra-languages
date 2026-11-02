package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.morphology.Unicode
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
     * Where a card blanks [term] out of [sentence]: the form chosen for it by hand, its family's forms, then a word that only
     * ends differently ("turma" for "turmas"), as an example chosen for the card may hold; then a
     * word the dictionary's [lemmas] tie to it ("temia" for "temer"), and last the word that begins
     * most like it ("sobrou" for "sobrar").
     */
    suspend fun cardRange(sentence: String, term: Term, terms: TermRepository, lemmas: (suspend (formLc: String) -> List<String>)? = null): IntRange? =
        term.cardForm?.let { range(sentence, it) } ?: familyRange(sentence, term, terms) ?: nearRange(sentence, term.displayText)
            ?: lemmas?.let { lemmaRange(sentence, term.displayText, it) } ?: sharedBeginningRange(sentence, term.displayText)

    /** The first word of [sentence] that shares a dictionary form with the single word [word], either being a form of the other or both of a third. */
    suspend fun lemmaRange(sentence: String, word: String, lemmas: suspend (formLc: String) -> List<String>): IntRange? {
        val needle = word.trim().lowercase()
        if (needle.isEmpty() || needle.any(Char::isWhitespace)) return null
        suspend fun forms(lc: String) = (lemmas(lc).map { it.lowercase() } + lc).toSet()
        val own = forms(needle)
        return WORD.findAll(sentence).firstOrNull { match -> forms(match.value.lowercase()).any { it in own } }?.range
    }

    /**
     * The word of [sentence] whose beginning, accents aside, is most like the single word [word]'s:
     * at least three letters in common and half the longer of the two, the length nearer the word's
     * on a tie.
     * Scripts written without spaces are left alone, a run of their text being no word.
     */
    fun sharedBeginningRange(sentence: String, word: String): IntRange? {
        val needle = plain(word.trim())
        if (needle.isEmpty() || needle.any { it.isWhitespace() || writtenWithoutSpaces(it) }) return null
        return WORD.findAll(sentence)
            .map { match -> match to plain(match.value).let { it.commonPrefixWith(needle).length to it.length } }
            .filter { (_, fit) -> fit.first >= SHARED_MIN && fit.first * 2 >= maxOf(fit.second, needle.length) }
            .map { (match, fit) -> match to (fit.first to abs(fit.second - needle.length)) }
            .maxWithOrNull(compareBy<Pair<MatchResult, Pair<Int, Int>>> { it.second.first }.thenByDescending { it.second.second })
            ?.first?.range
    }

    /** Lowercase, without the accents its letters carry. */
    private fun plain(text: String): String = Unicode.nfd(text.lowercase()).filter { Unicode.combiningClass(it) == 0 }

    private fun writtenWithoutSpaces(c: Char): Boolean = c.code in 0x3040..0x30FF || c.code in 0x3400..0x9FFF || c.code in 0xF900..0xFAFF ||
        c.code in 0x0E00..0x0EFF || c.code in 0x1000..0x109F || c.code in 0x1780..0x17FF

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
    private const val SHARED_MIN = 3
}
