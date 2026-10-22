package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.parse.withoutEmptyExceptions
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository

/**
 * Mends the languages saved with an empty choice in their sentence-split exceptions (Danish, Dutch, Romanian and
 * others came with a list ending in "|"), under which no word was found. The word counts, sentences and stats of
 * their books, worked out then, are worked out again, and the language is saved without the empty choice. Run at
 * start: it changes nothing once no language has one.
 */
class EmptyExceptionsMigration(
    private val languages: LanguageRepository,
    private val books: BookRepository,
) {
    /** The ids of the languages mended. */
    suspend fun migrate(): List<Long> {
        val mended = mutableListOf<Long>()
        for (language in languages.getAll()) {
            val exceptions = withoutEmptyExceptions(language.exceptionsSplitSentences)
            if (exceptions == language.exceptionsSplitSentences) continue
            val fixed = language.copy(exceptionsSplitSentences = exceptions)
            for (book in books.getBooks().filter { it.languageId == language.id }) {
                for (page in books.getPages(book.id)) {
                    books.updatePageText(page.id, page.text, SentenceBuilder.wordCount(page.text, fixed))
                    books.replaceSentences(page.id, SentenceBuilder.build(page.text, fixed))
                }
                books.clearStats(book.id)
            }
            // Saved last, so a start cut short does the books again.
            languages.save(fixed)
            mended += language.id
        }
        return mended
    }
}
