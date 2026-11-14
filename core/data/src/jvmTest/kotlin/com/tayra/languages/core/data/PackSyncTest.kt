package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.sync.ServicePacks
import com.tayra.languages.core.data.sync.SyncEngine
import com.tayra.languages.core.data.sync.SyncTransport
import com.tayra.languages.core.data.sync.SyncedPacks
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStore
import com.tayra.languages.core.domain.books.BookPacks
import com.tayra.languages.core.domain.books.PackedBook
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStore
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.InstalledCoursePacks
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.DriverManager
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Packs through the account: a pack installed on one device is downloaded on the others by sync,
 * and the reading of its lessons and books lands there at once; one removed on purpose is removed
 * everywhere; one that cannot be downloaded leaves its reading waiting for the next sync.
 */
class PackSyncTest {

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

    /** The Portuguese course pack, downloaded from nowhere; [offline] makes downloading fail. */
    private class CourseStore : CoursePackStore {
        var installed = false
        var offline = false
        val downloads = mutableListOf<String>()
        override suspend fun installedSize(pack: CoursePack): Long? = if (installed && pack.id == COURSE_PACK) 1_024L else null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) {
            if (offline) error("No connection")
            if (pack.id != COURSE_PACK) error("Not found")
            downloads += pack.id
            installed = true
        }
        override suspend fun remove(pack: CoursePack) { if (pack.id == COURSE_PACK) installed = false }
        override suspend fun courseIds(pack: CoursePack): Set<String> = courses(pack).map { it.id }.toSet()
        override suspend fun courses(pack: CoursePack): List<Course> = if (installed && pack.id == COURSE_PACK) COURSES else emptyList()
    }

    private class BookStore : BookPackStore {
        var installed = false
        var offline = false
        override suspend fun installedSize(pack: BookPack): Long? = if (installed && pack.id == BOOK_PACK) 2_048L else null
        override suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) {
            if (offline) error("No connection")
            if (pack.id != BOOK_PACK) error("Not found")
            installed = true
        }
        override suspend fun remove(pack: BookPack) { if (pack.id == BOOK_PACK) installed = false }
        override suspend fun books(pack: BookPack): List<PackedBook> = if (installed && pack.id == BOOK_PACK) BOOKS else emptyList()
    }

    /** A device; [tracksPacks] false for a version from before packs were synced. */
    private class Device(account: Account, tracksPacks: Boolean = true) {
        val file: File = File.createTempFile("tayra-pack-sync", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val bookService = BookService(books, languages)
        val settings = MapSettings()
        val choices = SyncedPacks(settings)
        val courseStore = CourseStore()
        val bookStore = BookStore()
        val courses = CourseService(books, languages, bookService, CourseRepositoryImpl(provider), InstalledCoursePacks(courseStore))
        val coursePacks = CoursePackService(courseStore, courses, choices.takeIf { tracksPacks })
        val bookPacks = BookPackService(bookStore, books, bookService, languages, choices.takeIf { tracksPacks })
        val engine = SyncEngine(
            provider, account, settings, addLanguage = { if (it == "pt") portuguese() },
            packs = choices.takeIf { tracksPacks }, installer = ServicePacks(coursePacks, bookPacks).takeIf { tracksPacks },
            openLesson = { course, lesson -> if (tracksPacks) courses.openLesson(course, lesson) },
        )

        suspend fun portuguese(): Long = languages.getAll().firstOrNull { it.name == "Portuguese" }?.id ?: languages.save(Language(name = "Portuguese"))

        /** Reads the lesson's or pack book's one page, finished at [at]. */
        suspend fun read(bookId: Long, at: Long) {
            val page = books.getPage(bookId, 1)!!
            books.setPageReadDate(page.id, Instant.fromEpochMilliseconds(at))
            books.setCurrentPage(bookId, page.id)
        }

        suspend fun readLesson(lessonId: String, at: Long) = read(courses.openLesson(COURSES.single().id, lessonId)!!, at)

        fun rows(query: String): List<String> = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") }).use { c ->
            c.createStatement().executeQuery(query).use { r -> buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) } }
        }

        fun lessonReading() = rows(
            "SELECT cl.lesson_id, cp.page_order, p.read_date FROM course_lessons cl JOIN books b ON b.id = cl.book_id " +
                "LEFT JOIN pages cp ON cp.id = b.current_page_id JOIN pages p ON p.book_id = b.id ORDER BY cl.lesson_id",
        )

        fun bookReading() = rows(
            "SELECT b.source_uri, cp.page_order, p.read_date FROM books b LEFT JOIN pages cp ON cp.id = b.current_page_id " +
                "JOIN pages p ON p.book_id = b.id WHERE b.source_uri IS NOT NULL ORDER BY b.source_uri",
        )

        fun pending() = rows("SELECT count(*) FROM sync_pending").single().toInt()

        fun courseState() = coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.state
        fun bookState() = bookPacks.packs.value.single { it.pack.id == BOOK_PACK }.state
    }

    private val account = Account()

    @Test
    fun aPackInstalledOnOneDeviceIsInstalledOnTheOtherWithItsReading() = runBlocking<Unit> {
        val desktop = Device(account)
        val phone = Device(account)
        desktop.portuguese()
        // The course pack was installed before packs were synced; the book pack after.
        desktop.courseStore.installed = true
        desktop.courses.seedSamples()
        desktop.bookPacks.download(BookPacks.all.single { it.id == BOOK_PACK })
        desktop.readLesson("pt-mini-0100-01", 1_000)
        desktop.readLesson("pt-mini-0100-02", 2_000)
        desktop.read(desktop.books.getBooks().single { it.sourceUri == SOURCE }.id, 3_000)

        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(PackState.Installed(1_024L), phone.courseState())
        assertEquals(PackState.Installed(2_048L), phone.bookState())
        assertEquals(listOf("pt-mini-0100-01|1|1000", "pt-mini-0100-02|1|2000"), phone.lessonReading(), "every lesson read has its reading, unopened")
        assertEquals(desktop.lessonReading(), phone.lessonReading())
        assertEquals(desktop.bookReading(), phone.bookReading())
        assertEquals(0, phone.pending())

        // The phone takes the times the packs were chosen at, and sends nothing back.
        assertEquals(0, phone.engine.syncNow().sent)
        assertEquals(0, phone.engine.syncNow().changed)
        assertEquals(listOf(COURSE_PACK), phone.courseStore.downloads)
    }

    @Test
    fun aPackRemovedOnPurposeIsRemovedEverywhere() = runBlocking<Unit> {
        val desktop = Device(account)
        val phone = Device(account)
        desktop.portuguese()
        desktop.coursePacks.download(desktop.coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.pack)
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(PackState.Installed(1_024L), phone.courseState())

        phone.coursePacks.remove(phone.coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.pack)
        phone.engine.syncNow()
        desktop.engine.syncNow()
        assertEquals(PackState.NotInstalled, desktop.courseState())
        assertEquals(emptyList(), desktop.rows("SELECT id FROM courses"))

        // Neither device brings it back.
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(PackState.NotInstalled, desktop.courseState())
        assertEquals(PackState.NotInstalled, phone.courseState())
        assertEquals(listOf(COURSE_PACK), desktop.courseStore.downloads)
    }

    @Test
    fun aPackThatCannotBeDownloadedLeavesItsReadingWaiting() = runBlocking<Unit> {
        val desktop = Device(account)
        val phone = Device(account)
        desktop.portuguese()
        desktop.coursePacks.download(desktop.coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.pack)
        desktop.readLesson("pt-mini-0100-01", 1_000)
        desktop.engine.syncNow()

        phone.courseStore.offline = true
        phone.engine.syncNow()
        assertTrue(phone.courseState() is PackState.Failed)
        assertEquals(emptyList(), phone.lessonReading())
        assertTrue(phone.pending() >= 3, "the pack, the lesson's text and its page wait")

        phone.courseStore.offline = false
        phone.engine.syncNow()
        assertEquals(PackState.Installed(1_024L), phone.courseState())
        assertEquals(listOf("pt-mini-0100-01|1|1000"), phone.lessonReading())
        assertEquals(0, phone.pending())
    }

    @Test
    fun theReadingOfAVersionThatSendsNoPacksBringsItsPacks() = runBlocking<Unit> {
        val desktop = Device(account, tracksPacks = false)
        val phone = Device(account)
        desktop.portuguese()
        desktop.coursePacks.download(desktop.coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.pack)
        desktop.bookPacks.download(BookPacks.all.single { it.id == BOOK_PACK })
        desktop.readLesson("pt-mini-0100-02", 2_000)
        desktop.read(desktop.books.getBooks().single { it.sourceUri == SOURCE }.id, 3_000)
        desktop.engine.syncNow()
        assertEquals(emptyList(), account.records.keys.filter { it.first == "pack" })

        phone.engine.syncNow()
        assertEquals(PackState.Installed(1_024L), phone.courseState(), "the pack of the lesson read")
        assertEquals(PackState.Installed(2_048L), phone.bookState(), "the pack named in the book's address")
        assertEquals(listOf("pt-mini-0100-02|1|2000"), phone.lessonReading())
        assertEquals(desktop.bookReading(), phone.bookReading())
        assertEquals(0, phone.pending())

        // Removed on the phone on purpose, the reading waiting for it does not bring it back.
        phone.coursePacks.remove(phone.coursePacks.packs.value.single { it.pack.id == COURSE_PACK }.pack)
        desktop.readLesson("pt-mini-0100-01", 4_000)
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(PackState.NotInstalled, phone.courseState())
        assertEquals(listOf(COURSE_PACK), phone.courseStore.downloads)
    }

    private companion object {
        const val COURSE_PACK = "courses-pt"
        const val BOOK_PACK = "books-pt"
        const val SOURCE = "${BookPacks.SOURCE_PREFIX}$BOOK_PACK/pt-lobo"
        val COURSES = listOf(
            Course(
                "pt-mini-0100", "pt", "Em casa", "Stories at home.", CourseLevel.A1, "Home",
                listOf(Lesson("pt-mini-0100-01", "A casa nova", "A new house.", "A Ana tem uma casa nova."), Lesson("pt-mini-0100-02", "O gato", "A cat.", "O gato está em casa.")),
                rankUpTo = 100,
            ),
        )
        val BOOKS = listOf(PackedBook("pt-lobo", "A2", "O lobo", "Wikilivros", "A wolf.", "https://pt.wikibooks.org", "CC BY-SA 4.0", emptyList(), "O lobo dorme na mata."))
    }
}
