package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.ListBooks
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.Page
import com.tayra.languages.core.domain.model.PageBookmark
import com.tayra.languages.core.domain.model.Sentence
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.NewPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class BookRepositoryImpl(private val provider: DatabaseProvider) : BookRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    override fun observeBooks(archived: Boolean): Flow<List<BookListItem>> = flow {
        db().booksQueries.listBooks(archived).asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(rows.map { it.toListItem() })
        }
    }

    override fun observeBook(id: Long): Flow<Book?> = flow {
        val database = db()
        database.booksQueries.selectById(id).asFlow().mapToOneOrNull(databaseDispatcher).collect { row ->
            emit(row?.toDomain(tagsFor(database, id)))
        }
    }

    override suspend fun getBook(id: Long): Book? = withContext(databaseDispatcher) {
        val database = db()
        database.booksQueries.selectById(id).awaitAsOneOrNull()?.toDomain(tagsFor(database, id))
    }

    override suspend fun getBooks(): List<Book> = withContext(databaseDispatcher) {
        val database = db()
        database.booksQueries.selectAll().awaitAsList().map { it.toDomain(tagsFor(database, it.id)) }
    }

    override suspend fun findByTitle(title: String, languageId: Long): Book? = withContext(databaseDispatcher) {
        val database = db()
        database.booksQueries.selectByTitle(title, languageId).awaitAsOneOrNull()?.let { it.toDomain(tagsFor(database, it.id)) }
    }

    override suspend fun insertBook(book: Book, pages: List<NewPage>): Long = withContext(databaseDispatcher) {
        val database = db()
        val q = database.booksQueries
        database.transactionWithResult {
            q.insert(
                languageId = book.languageId,
                title = book.title,
                sourceUri = book.sourceUri,
                currentPageId = null,
                archived = book.archived,
                audioFilename = book.audioFilename,
                audioCurrentPos = book.audioCurrentPos,
                audioBookmarks = book.audioBookmarks,
            )
            val id = q.lastInsertId().awaitAsOne()
            pages.forEachIndexed { index, page ->
                q.insertPage(bookId = id, pageOrder = (index + 1).toLong(), text = page.text, wordCount = page.wordCount.toLong())
            }
            saveTags(database, id, book.tags)
            id
        }
    }

    override suspend fun updateBook(book: Book) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.booksQueries.update(
                id = book.id,
                title = book.title,
                sourceUri = book.sourceUri,
                audioFilename = book.audioFilename,
                audioCurrentPos = book.audioCurrentPos,
                audioBookmarks = book.audioBookmarks,
            )
            saveTags(database, book.id, book.tags)
        }
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        withContext(databaseDispatcher) {
            db().booksQueries.setArchived(archived = archived, id = id)
        }
    }

    override suspend fun deleteBook(id: Long) {
        withContext(databaseDispatcher) {
            db().booksQueries.delete(id)
        }
    }

    override suspend fun allBookTags(): List<String> = withContext(databaseDispatcher) {
        db().booksQueries.selectAllTags().awaitAsList().map { it.text }
    }

    override suspend fun getPages(bookId: Long): List<Page> = withContext(databaseDispatcher) {
        db().booksQueries.selectPages(bookId).awaitAsList().map { it.toDomain() }
    }

    override suspend fun getPage(bookId: Long, order: Int): Page? = withContext(databaseDispatcher) {
        db().booksQueries.selectPage(bookId, order.toLong()).awaitAsOneOrNull()?.toDomain()
    }

    override suspend fun getPageById(pageId: Long): Page? = withContext(databaseDispatcher) {
        db().booksQueries.selectPageById(pageId).awaitAsOneOrNull()?.toDomain()
    }

    override suspend fun pageCount(bookId: Long): Int = withContext(databaseDispatcher) {
        db().booksQueries.countPages(bookId).awaitAsOne().toInt()
    }

    override suspend fun insertPage(bookId: Long, order: Int, page: NewPage): Long = withContext(databaseDispatcher) {
        val database = db()
        val q = database.booksQueries
        database.transactionWithResult {
            q.shiftPagesFrom(bookId = bookId, fromOrder = order.toLong())
            q.insertPage(bookId = bookId, pageOrder = order.toLong(), text = page.text, wordCount = page.wordCount.toLong())
            q.lastInsertId().awaitAsOne()
        }
    }

    override suspend fun updatePageText(pageId: Long, text: String, wordCount: Int) {
        withContext(databaseDispatcher) {
            db().booksQueries.updatePageText(text = text, wordCount = wordCount.toLong(), id = pageId)
        }
    }

    override suspend fun deletePage(pageId: Long) = withContext(databaseDispatcher) {
        val database = db()
        val q = database.booksQueries
        database.transaction {
            val page = q.selectPageById(pageId).awaitAsOneOrNull() ?: return@transaction
            q.deletePage(pageId)
            q.shiftPagesDownAfter(bookId = page.book_id, afterOrder = page.page_order)
            val book = q.selectById(page.book_id).awaitAsOneOrNull()
            if (book?.current_page_id == pageId) {
                val replacement = q.selectPage(page.book_id, maxOf(1L, page.page_order - 1)).awaitAsOneOrNull()
                    ?: q.selectPage(page.book_id, page.page_order).awaitAsOneOrNull()
                q.setCurrentPage(pageId = replacement?.id, id = page.book_id)
            }
        }
    }

    override suspend fun setCurrentPage(bookId: Long, pageId: Long) {
        withContext(databaseDispatcher) {
            db().booksQueries.setCurrentPage(pageId = pageId, id = bookId)
        }
    }

    override suspend fun setPageStartDate(pageId: Long, date: Instant) {
        withContext(databaseDispatcher) {
            db().booksQueries.setPageStartDate(startDate = date.toEpochMillis(), id = pageId)
        }
    }

    override suspend fun setPageReadDate(pageId: Long, date: Instant) {
        withContext(databaseDispatcher) {
            db().booksQueries.setPageReadDate(readDate = date.toEpochMillis(), id = pageId)
        }
    }

    override suspend fun replaceSentences(pageId: Long, sentences: List<Sentence>) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.booksQueries.deleteSentences(pageId)
            for (sentence in sentences) {
                database.booksQueries.insertSentence(
                    pageId = pageId,
                    sentenceOrder = sentence.order.toLong(),
                    text = sentence.text,
                    textLc = sentence.textLc,
                )
            }
        }
    }

    override suspend fun getStats(bookId: Long): BookStats? = withContext(databaseDispatcher) {
        db().booksQueries.selectStats(bookId).awaitAsOneOrNull()?.let {
            BookStats(
                distinctTerms = it.distinct_terms.toInt(),
                distinctUnknowns = it.distinct_unknowns.toInt(),
                unknownPercent = it.unknown_percent.toInt(),
                statusDistribution = decodeDistribution(it.status_distribution),
            )
        }
    }

    override suspend fun saveStats(bookId: Long, stats: BookStats) {
        withContext(databaseDispatcher) {
            db().booksQueries.upsertStats(
                bookId = bookId,
                distinctTerms = stats.distinctTerms.toLong(),
                distinctUnknowns = stats.distinctUnknowns.toLong(),
                unknownPercent = stats.unknownPercent.toLong(),
                statusDistribution = encodeDistribution(stats.statusDistribution),
            )
        }
    }

    override suspend fun clearStats(bookId: Long) {
        withContext(databaseDispatcher) {
            db().booksQueries.deleteStats(bookId)
        }
    }

    override suspend fun bookIdsWithoutStats(): List<Long> = withContext(databaseDispatcher) {
        db().booksQueries.bookIdsWithoutStats().awaitAsList()
    }

    override fun observeBookmarks(bookId: Long): Flow<List<PageBookmark>> = flow {
        db().booksQueries.selectBookmarks(bookId).asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(rows.map { PageBookmark(it.id, it.page_id, it.page_order.toInt(), it.title) })
        }
    }

    override suspend fun addBookmark(pageId: Long, title: String): Long = withContext(databaseDispatcher) {
        val database = db()
        database.transactionWithResult {
            database.booksQueries.insertBookmark(pageId, title)
            database.booksQueries.lastInsertId().awaitAsOne()
        }
    }

    override suspend fun renameBookmark(id: Long, title: String) {
        withContext(databaseDispatcher) {
            db().booksQueries.renameBookmark(title = title, id = id)
        }
    }

    override suspend fun deleteBookmark(id: Long) {
        withContext(databaseDispatcher) {
            db().booksQueries.deleteBookmark(id)
        }
    }

    private suspend fun tagsFor(database: TayraDatabase, bookId: Long): List<String> =
        database.booksQueries.selectTagsForBook(bookId).awaitAsList().map { it.text }

    private suspend fun saveTags(database: TayraDatabase, bookId: Long, tags: List<String>) {
        val q = database.booksQueries
        q.deleteTagMap(bookId)
        for (tag in tags) {
            val existing = q.selectTagByText(tag).awaitAsOneOrNull()
            val tagId = existing?.id ?: run {
                q.insertTag(tag, "")
                q.lastInsertId().awaitAsOne()
            }
            q.insertTagMap(bookId, tagId)
        }
    }

    private fun ListBooks.toListItem(): BookListItem = BookListItem(
        id = id,
        title = title,
        languageId = language_id,
        languageName = language_name,
        tags = tag_list.split("|").filter { it.isNotEmpty() },
        currentPage = current_page.toInt(),
        pageCount = page_count.toInt(),
        wordCount = word_count.toInt(),
        lastOpened = last_opened?.toInstant(),
        isCompleted = last_page_read_date != null,
        isArchived = archived,
        stats = if (distinct_terms != null && distinct_unknowns != null && unknown_percent != null && status_distribution != null) {
            BookStats(distinct_terms.toInt(), distinct_unknowns.toInt(), unknown_percent.toInt(), decodeDistribution(status_distribution))
        } else {
            null
        },
        sourceUri = source_uri,
    )

    private companion object {
        val distributionSerializer = MapSerializer(Int.serializer(), Int.serializer())

        fun encodeDistribution(distribution: Map<TermStatus, Int>): String =
            Json.encodeToString(distributionSerializer, distribution.mapKeys { it.key.value })

        fun decodeDistribution(json: String): Map<TermStatus, Int> = runCatching {
            Json.decodeFromString(distributionSerializer, json)
                .mapNotNull { (k, v) -> TermStatus.fromValueOrNull(k)?.let { it to v } }
                .toMap()
        }.getOrDefault(emptyMap())
    }
}
