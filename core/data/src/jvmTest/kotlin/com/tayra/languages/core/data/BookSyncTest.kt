package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.sync.SyncEngine
import com.tayra.languages.core.data.sync.SyncTransport
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.WordsReadEntry
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Texts and reading through the account: a text made on one device arrives whole with how far it
 * was read; a text every device has from a pack or a course carries only the reading, and waits
 * for the pack to be installed; the same text made on two devices becomes one.
 */
class BookSyncTest {

    private class Account : SyncTransport {
        val records = linkedMapOf<Pair<String, String>, Pair<SyncRecord, Long>>()
        var seq = 0L
        override fun syncAccount() = "http://localhost me@example.com"
        override suspend fun push(request: PushRequest): PushResponse {
            for (incoming in request.records) {
                val id = incoming.kind to incoming.key
                val stored = records[id]?.first
                val merged = if (stored == null) incoming else SyncMerge.merge(stored, incoming)
                if (merged != stored) records[id] = merged to ++seq
            }
            return PushResponse(seq, System.currentTimeMillis())
        }
        override suspend fun pull(since: Long, limit: Int): PullResponse {
            val after = records.values.filter { it.second > since }.sortedBy { it.second }
            val page = after.take(limit)
            return PullResponse(page.map { it.first }, page.lastOrNull()?.second ?: since, more = after.size > limit)
        }
    }

    private class Device(account: Account) {
        val file: File = File.createTempFile("tayra-book-sync", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val service = BookService(books, languages)
        val engine = SyncEngine(provider, account, MapSettings(), addLanguage = { if (it == "pt") portuguese() })

        suspend fun portuguese(): Long = languages.getAll().firstOrNull { it.name == "Portuguese" }?.id ?: languages.save(Language(name = "Portuguese"))

        suspend fun text(title: String, text: String, source: String = "", tags: List<String> = emptyList()): Long =
            service.create(BookDraft(languageId = portuguese(), title = title, text = text, sourceUri = source, tags = tags, wordsPerPage = 1))

        fun rows(query: String): List<String> = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") }).use { c: Connection ->
            c.createStatement().executeQuery(query).use { r -> buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) } }
        }

        fun bookId(title: String) = rows("SELECT id FROM books WHERE title = '$title'").single().toLong()
    }

    private val account = Account()
    private val phone = Device(account)
    private val desktop = Device(account)

    private suspend fun syncBoth() {
        phone.engine.syncNow()
        desktop.engine.syncNow()
        phone.engine.syncNow()
    }

    @Test
    fun aTextMadeOnOneDeviceArrivesWholeWithItsReading() = runBlocking<Unit> {
        desktop.portuguese()
        val id = phone.text("O lobo", "O lobo dorme. O gato come. A casa é azul.", tags = listOf("story", "a1"))
        val second = phone.books.getPage(id, 2)!!
        phone.books.setPageReadDate(phone.books.getPage(id, 1)!!.id, Instant.fromEpochMilliseconds(1_000))
        phone.books.setPageStartDate(second.id, Instant.fromEpochMilliseconds(2_000))
        phone.books.setCurrentPage(id, second.id)
        phone.books.addBookmark(second.id, "O gato")
        WordsReadRepositoryImpl(phone.provider).add(WordsReadEntry(phone.portuguese(), phone.books.getPage(id, 1)!!.id, Instant.fromEpochMilliseconds(1_000), 3))

        syncBoth()
        val copy = desktop.bookId("O lobo")
        assertEquals(phone.rows("SELECT page_order, text, word_count, start_date, read_date FROM pages WHERE book_id = $id ORDER BY page_order"),
            desktop.rows("SELECT page_order, text, word_count, start_date, read_date FROM pages WHERE book_id = $copy ORDER BY page_order"))
        assertEquals(listOf("2"), desktop.rows("SELECT p.page_order FROM books b JOIN pages p ON p.id = b.current_page_id WHERE b.id = $copy"))
        assertEquals(listOf("a1", "story"), desktop.rows("SELECT t.text FROM book_tag_map m JOIN book_tags t ON t.id = m.tag_id WHERE m.book_id = $copy ORDER BY t.text"))
        assertEquals(listOf("2|O gato"), desktop.rows("SELECT p.page_order, bm.title FROM page_bookmarks bm JOIN pages p ON p.id = bm.page_id WHERE p.book_id = $copy"))
        assertEquals(listOf("1000|3|1"), desktop.rows("SELECT w.read_at, w.word_count, p.page_order FROM words_read w JOIN pages p ON p.id = w.page_id"))
        assertEquals(phone.rows("SELECT sync_id FROM books WHERE id = $id"), desktop.rows("SELECT sync_id FROM books WHERE id = $copy"))
        assertEquals(0, desktop.engine.syncNow().changed, "nothing left to take")

        // Reading on, and the deletion of the text, reach the other device too.
        desktop.books.setPageReadDate(desktop.books.getPage(copy, 2)!!.id, Instant.fromEpochMilliseconds(3_000))
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(listOf("3000"), phone.rows("SELECT read_date FROM pages WHERE book_id = $id AND page_order = 2"))
        phone.service.delete(id)
        syncBoth()
        assertEquals(emptyList(), desktop.rows("SELECT id FROM books WHERE title = 'O lobo'"))
    }

    @Test
    fun theSameTextMadeOnBothDevicesBecomesOne() = runBlocking<Unit> {
        phone.text("Tutorial", "Bem-vindo ao Tayra. Leia e aprenda palavras.")
        desktop.text("Tutorial", "Bem-vindo ao Tayra. Leia e aprenda palavras.")
        desktop.text("Notas", "Outro texto meu.")
        syncBoth()
        desktop.engine.syncNow()
        for (device in listOf(phone, desktop)) {
            assertEquals(listOf("Notas", "Tutorial"), device.rows("SELECT title FROM books ORDER BY title"))
        }
        assertEquals(phone.rows("SELECT sync_id FROM books WHERE title = 'Tutorial'"), desktop.rows("SELECT sync_id FROM books WHERE title = 'Tutorial'"))
    }

    @Test
    fun aTutorialCarriesOnlyItsReadingAndIsNotBroughtBack() = runBlocking<Unit> {
        val tutorial = Tutorials.forLanguage("Portuguese")!!
        val mine = phone.text(tutorial.title, tutorial.text)
        val theirs = desktop.text(tutorial.title, tutorial.text)
        val third = Device(account)
        third.portuguese()
        desktop.engine.syncNow()
        assertEquals(emptyList(), account.records.keys.filter { it.first == "book" }, "a tutorial not read yet is no one's change")

        phone.books.setCurrentPage(mine, phone.books.getPage(mine, 2)!!.id)
        syncBoth()
        third.engine.syncNow()
        assertEquals(listOf("2"), desktop.rows("SELECT p.page_order FROM books b JOIN pages p ON p.id = b.current_page_id WHERE b.id = $theirs"))
        assertEquals(emptyList(), third.rows("SELECT title FROM books"), "a device without the tutorial does not get it back")
    }

    @Test
    fun aPacksTextCarriesOnlyItsReadingAndWaitsForThePack() = runBlocking<Unit> {
        val source = "tayra-books://books-pt/pt-lobo"
        val id = phone.text("O lobo", "O lobo dorme. O gato come. A casa é azul.", source = source)
        val third = phone.books.getPage(id, 3)!!
        phone.books.setPageStartDate(third.id, Instant.fromEpochMilliseconds(5_000))
        phone.books.setCurrentPage(id, third.id)
        phone.books.addBookmark(third.id, "A casa")
        phone.engine.syncNow()
        assertEquals(null, account.records["page" to "uri:$source/3"]!!.first.data!!["text"], "a pack's text is not sent")

        desktop.engine.syncNow()
        assertEquals(emptyList(), desktop.rows("SELECT id FROM books"), "not made from its reading")
        assertEquals(3, desktop.rows("SELECT count(*) FROM sync_pending").single().toInt(), "the book, its page opened and its bookmark wait")

        // The pack is installed on the desktop: the reading waiting for it is applied at the next sync.
        val copy = desktop.text("O lobo", "O lobo dorme. O gato come. A casa é azul.", source = source)
        desktop.engine.syncNow()
        assertEquals(listOf("3"), desktop.rows("SELECT p.page_order FROM books b JOIN pages p ON p.id = b.current_page_id WHERE b.id = $copy"))
        assertEquals(listOf("5000"), desktop.rows("SELECT start_date FROM pages WHERE book_id = $copy AND page_order = 3"))
        assertEquals(listOf("A casa"), desktop.rows("SELECT title FROM page_bookmarks"))
        assertEquals(0, desktop.rows("SELECT count(*) FROM sync_pending").single().toInt())

        // Deleting the bookmark on one device deletes it on the other; removing the pack does not.
        val bookmark = phone.rows("SELECT id FROM page_bookmarks").single().toLong()
        phone.books.deleteBookmark(bookmark)
        phone.service.delete(id)
        syncBoth()
        desktop.engine.syncNow()
        assertEquals(emptyList(), desktop.rows("SELECT title FROM page_bookmarks"))
        assertEquals(listOf("O lobo"), desktop.rows("SELECT title FROM books"))
    }

    @Test
    fun aLessonsTextCarriesItsReadingByTheLesson() = runBlocking<Unit> {
        val id = phone.text("Lição 1", "O lobo dorme. O gato come.")
        phone.books.linkLesson("pack-pt-a1-lesson-1", id)
        phone.books.setCurrentPage(id, phone.books.getPage(id, 2)!!.id)
        val copy = desktop.text("Lição 1", "O lobo dorme. O gato come.")
        desktop.books.linkLesson("pack-pt-a1-lesson-1", copy)
        syncBoth()
        assertEquals(listOf("Lição 1"), desktop.rows("SELECT title FROM books"), "not made twice")
        assertEquals(listOf("2"), desktop.rows("SELECT p.page_order FROM books b JOIN pages p ON p.id = b.current_page_id"))
    }
}
