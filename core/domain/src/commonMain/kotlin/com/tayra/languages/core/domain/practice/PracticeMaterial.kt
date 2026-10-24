package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.flashcards.Cloze
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ReadingService

/** Where a practice takes its words from. */
sealed interface PracticeSource {
    /** The words being learned on a page of a book. */
    data class Page(val bookId: Long, val page: Int) : PracticeSource

    /** The words being learned anywhere in a book or lesson, each in a sentence of it. */
    data class Book(val bookId: Long) : PracticeSource

    /** Words chosen in the vocabulary, by their ids. */
    data class Words(val termIds: List<Long>) : PracticeSource
}

/**
 * The words a practice asks about, in their language, with other words of the text as wrong
 * choices ([pool]) and the meanings of other saved words as wrong choices of meaning ([meanings]).
 */
class PracticeMaterial(val language: Language, val title: String, val words: List<PracticeWord>, val pool: List<String>, val meanings: List<String> = emptyList())

/** Gathers the words of a practice, each in a sentence, from where the practice was started. */
class PracticeMaterialLoader(
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val terms: TermRepository,
) {
    suspend fun load(source: PracticeSource): PracticeMaterial? {
        val material = when (source) {
            is PracticeSource.Page -> pageWords(source)
            is PracticeSource.Book -> bookWords(source)
            is PracticeSource.Words -> chosenWords(source)
        } ?: return null
        return PracticeMaterial(material.language, material.title, material.words, material.pool, meanings(material.language))
    }

    /** The meanings of saved words of the language, being learned or known. */
    private suspend fun meanings(language: Language): List<String> =
        terms.list(TermListFilter(languageId = language.id, minStatus = TermStatus.NEW_1, maxStatus = TermStatus.WELL_KNOWN), TermListSort(), 0, POOL_WORDS).items
            .mapNotNull { it.translation?.trim()?.takeIf(String::isNotEmpty) }

    private suspend fun pageWords(source: PracticeSource.Page): PracticeMaterial? {
        val book = books.getBook(source.bookId)
        val lang = book?.let { languages.getById(it.languageId) }
        val page = books.getPage(source.bookId, source.page)
        if (book == null || lang == null || page == null) return null
        val rendered = reading.renderPage(page.text, lang)
        return PracticeMaterial(lang, book.title, learningWords(rendered), rendered.words.map { it.renderText }.distinctBy { it.lowercase() })
    }

    /** Every page's words being learned, each in the first sentence it comes in; the book's other words are the wrong choices. */
    private suspend fun bookWords(source: PracticeSource.Book): PracticeMaterial? {
        val book = books.getBook(source.bookId)
        val lang = book?.let { languages.getById(it.languageId) } ?: return null
        val pages = (1..books.pageCount(source.bookId)).mapNotNull { books.getPage(source.bookId, it) }.map { reading.renderPage(it.text, lang) }
        val words = pages.flatMap(::learningWords).distinctBy { it.text.lowercase() }
        return PracticeMaterial(lang, book.title, words, pages.flatMap { it.words }.map { it.renderText }.distinctBy { it.lowercase() })
    }

    private fun learningWords(rendered: RenderedPage): List<PracticeWord> =
        rendered.paragraphs.flatMap { it.sentences }.flatMap { sentence ->
            val text = sentence.displayText
            sentence.items.filter { it.isWord && it.status.isLearning }.mapNotNull { item ->
                val word = item.renderText
                Cloze.range(text, word)?.let { PracticeWord(word, item.term?.translation?.trim()?.takeIf(String::isNotEmpty), text, it) }
            }
        }.distinctBy { it.text.lowercase() }

    /** The chosen words that were saved with a sentence, each in it; the language's other words being learned stand in for the page's. */
    private suspend fun chosenWords(source: PracticeSource.Words): PracticeMaterial? {
        val chosen = terms.getByIds(source.termIds)
        val lang = chosen.firstOrNull()?.let { languages.getById(it.languageId) } ?: return null
        val words = chosen.filter { it.languageId == lang.id }.mapNotNull { term ->
            val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val range = Cloze.familyRange(sentence, term, terms) ?: return@mapNotNull null
            PracticeWord(sentence.substring(range), term.translation?.trim()?.takeIf(String::isNotEmpty), sentence, range)
        }.distinctBy { it.text.lowercase() }
        val pool = terms.list(TermListFilter(languageId = lang.id, minStatus = TermStatus.NEW_1, maxStatus = TermStatus.LEARNING_4), TermListSort(), 0, POOL_WORDS).items.map { it.displayText }
        return PracticeMaterial(lang, "", words, pool)
    }

    private companion object {
        /** How many other words being learned are fetched as wrong choices for chosen words. */
        const val POOL_WORDS = 60
    }
}
