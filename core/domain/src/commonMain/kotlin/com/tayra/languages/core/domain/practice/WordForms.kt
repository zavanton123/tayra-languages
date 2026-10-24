package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.FrequencyWord
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.TermRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The forms of a word, without the network: those the language's frequency list counts under the
 * same word ("esqueci", "esquecer" and "esqueço" under one), and those the learner linked to the
 * saved word as its forms or its base. Also the words' frequency ranks, for wrong choices as
 * common as the right one.
 */
class WordForms(private val frequency: FrequencyLists?, private val terms: TermRepository) {
    /** A list's words by each of their forms, and in rank order. */
    private class Index(val byForm: Map<String, List<FrequencyWord>>, val words: List<FrequencyWord>)

    private val indexes = mutableMapOf<String, Index?>()
    private val lock = Mutex()

    /** The forms of [text] in [language], lowercase, [text] among them. */
    suspend fun of(text: String, language: Language): Set<String> {
        val key = language.lowercase(text)
        // A linked form the list does not count still brings in the forms of the word it is linked to.
        val known = listOf(key) + linked(key, language)
        val byForm = index(language)?.byForm.orEmpty()
        val counted = known.flatMap { form -> byForm[form].orEmpty().flatMap { it.forms + it.word.lowercase() } }
        return (known + counted).filter { it.isNotBlank() }.toSet()
    }

    /** How common [text] is in [language]: 1 for the most common word, null when the list has no such word. */
    suspend fun rank(text: String, language: Language): Int? = index(language)?.byForm?.get(language.lowercase(text))?.minOf { it.rank }

    /** The words of [language]'s list from [from] to [to] in rank, most common first. */
    suspend fun ranked(language: Language, from: Int, to: Int): List<String> =
        index(language)?.words?.filter { it.rank in from..to }?.map { it.word }.orEmpty()

    private suspend fun linked(key: String, language: Language): List<String> = try {
        val term = terms.findByTextLc(language.id, key)
        if (term == null) {
            emptyList()
        } else {
            val parents = terms.parents(term.id)
            (terms.children(term.id) + parents + parents.flatMap { terms.children(it.id) }).map { language.lowercase(it.displayText) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun index(language: Language): Index? {
        val code = LanguageCodes.codeFor(language.name) ?: return null
        return lock.withLock {
            if (code !in indexes) {
                val list = try {
                    frequency?.list(code)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                indexes[code] = list?.let { found ->
                    val byForm = HashMap<String, MutableList<FrequencyWord>>()
                    for (word in found.words) for (form in (word.forms + word.word.lowercase()).toSet()) byForm.getOrPut(form) { ArrayList(1) }.add(word)
                    Index(byForm, found.words)
                }
            }
            indexes[code]
        }
    }
}
