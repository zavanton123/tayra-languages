package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ReadingService
import kotlinx.coroutines.flow.first

/**
 * The words about to be read: those on the page reached in the text of a language read last, a
 * book of the library or a lesson not finished, and on the few pages after it. A form whose
 * family is learned with one card stands for that card's word.
 */
class ReadingWords(
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val terms: TermRepository,
    private val cards: FlashcardRepository,
) {
    /** The terms whose cards hold the words ahead in [languageId]'s text read last; none without one. */
    suspend fun termIds(languageId: Long): Set<Long> {
        val (bookId, page) = lastRead(languageId) ?: return emptySet()
        val language = languages.getById(languageId) ?: return emptySet()
        val text = (page.coerceAtLeast(1) until page.coerceAtLeast(1) + PAGES).mapNotNull { books.getPage(bookId, it)?.text }.joinToString("\n")
        if (text.isBlank()) return emptySet()
        val found = reading.findTermsInText(text, language)
        return found.flatMapTo(mutableSetOf()) { term ->
            if (cards.get(term.id) != null) listOf(term.id) else terms.parents(term.id).map { it.id }
        }
    }

    /** The text of [languageId] opened last and not finished, with the page reached in it. */
    private suspend fun lastRead(languageId: Long): Pair<Long, Int>? {
        val library = books.observeBooks(archived = false).first()
            .filter { it.languageId == languageId && it.lastOpened != null && !it.isCompleted }
            .map { Triple(it.id, it.currentPage, it.lastOpened!!) }
        // Lesson texts are kept out of the library list; their language is their book's.
        val lessons = books.observeLessonBooks().first().values
            .filter { it.lastOpened != null && !it.isCompleted }
            .mapNotNull { lesson -> books.getBook(lesson.bookId)?.takeIf { it.languageId == languageId }?.let { Triple(lesson.bookId, lesson.currentPage, lesson.lastOpened!!) } }
        return (library + lessons).maxByOrNull { it.third }?.let { it.first to it.second }
    }

    private companion object {
        /** The page reached and the pages after it that count as about to be read. */
        const val PAGES = 6
    }
}
