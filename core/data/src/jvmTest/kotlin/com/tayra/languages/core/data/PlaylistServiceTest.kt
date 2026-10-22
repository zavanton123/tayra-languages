package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PlaylistRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.playlists.ListeningPosition
import com.tayra.languages.core.domain.playlists.ListeningTime
import com.tayra.languages.core.domain.playlists.PlaylistDraft
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.domain.playlists.PlaylistTextKind
import com.tayra.languages.core.domain.playlists.PlaylistValidationException
import com.tayra.languages.core.domain.playlists.wavSeconds
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Playlists hold texts of the library in an order of their own, and lose a text only when the library does. */
class PlaylistServiceTest {
    private val file = File.createTempFile("tayra-playlists", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val courses = CourseService(books, languages, bookService, CourseRepositoryImpl(provider), TestCourses.source)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val time = ListeningTime(settings)
    private val service = PlaylistService(PlaylistRepositoryImpl(provider), courses, languages, time)

    private suspend fun portuguese() = languages.save(Language(name = "Portuguese")).also { courses.seedSamples() }

    private suspend fun book(language: Long, title: String, text: String = "O lobo dorme.") =
        bookService.create(BookDraft(languageId = language, title = title, text = text))

    @Test
    fun aPlaylistHoldsTextsLessonsAndWholeCoursesInOrder() = runTest {
        val language = portuguese()
        val lobo = book(language, "O lobo")
        val course = TestCourses.ALL[1]
        val id = service.create(
            language,
            PlaylistDraft("  Morning Portuguese ", "  For the bus.  ", cover = 2),
            listOf(PlaylistSource.Text(lobo), PlaylistSource.WholeCourse(course.id), PlaylistSource.Lesson(TestCourses.ALL[0].id, TestCourses.ALL[0].lessons[2].id)),
        )
        val playlist = service.observePlaylist(id).first()!!
        assertEquals("Morning Portuguese", playlist.name)
        assertEquals("For the bus.", playlist.description)
        assertEquals(2, playlist.cover)
        assertEquals(listOf("O lobo", "Lição 1", "Lição 2", "Lição 3"), playlist.items.map { it.title })
        assertEquals(listOf(PlaylistTextKind.BOOK, PlaylistTextKind.LESSON, PlaylistTextKind.LESSON, PlaylistTextKind.LESSON), playlist.items.map { it.kind })
        assertEquals("O lobo dorme.".length, playlist.items[0].length)
        assertNull(playlist.position)

        // A lesson added is made a text, but not opened: the course is still not started.
        assertEquals(LessonStatus.NOT_STARTED, courses.observeCourse(course.id).first()!!.status)

        // A text already in the playlist keeps its place.
        service.add(id, listOf(PlaylistSource.Text(lobo), PlaylistSource.WholeCourse(course.id)))
        assertEquals(4, service.playlist(id)!!.items.size)
        assertEquals(listOf(id), service.observePlaylists(language).first().map { it.id })
    }

    @Test
    fun aPlaylistNeedsANameOfAtMostSixtyCharacters() = runTest {
        val language = portuguese()
        assertFailsWith<PlaylistValidationException> { service.create(language, PlaylistDraft("   ")) }
        assertFailsWith<PlaylistValidationException> { service.create(language, PlaylistDraft("a".repeat(61))) }
        val id = service.create(language, PlaylistDraft("a".repeat(60)))
        service.update(id, PlaylistDraft("Evening", "Quiet texts", cover = 1))
        val edited = service.playlist(id)!!
        assertEquals(listOf("Evening", "Quiet texts", "1"), listOf(edited.name, edited.description, edited.cover.toString()))
        assertTrue(service.playlist(id)!!.items.isEmpty(), "an empty playlist is fine")
    }

    @Test
    fun itemsAreReorderedAndRemovedAndThePlaylistGoesWithoutItsTexts() = runTest {
        val language = portuguese()
        val (a, b, c) = listOf("A", "B", "C").map { book(language, it) }
        val id = service.create(language, PlaylistDraft("Mine"), listOf(a, b, c).map { PlaylistSource.Text(it) })
        val items = service.playlist(id)!!.items
        service.move(id, items[2].id, 0)
        assertEquals(listOf("C", "A", "B"), service.playlist(id)!!.items.map { it.title })
        service.reorder(id, listOf(items[1].id))
        assertEquals(listOf("B", "C", "A"), service.playlist(id)!!.items.map { it.title }, "items not named keep their order after the named ones")
        service.remove(items[0].id)
        assertEquals(listOf("B", "C"), service.playlist(id)!!.items.map { it.title })

        // The reader's playlist menu puts the open text in and takes it out.
        service.setContains(id, a, contains = true)
        assertEquals(listOf("B", "C", "A"), service.playlist(id)!!.items.map { it.title })
        service.setContains(id, a, contains = false)
        assertEquals(listOf("B", "C"), service.playlist(id)!!.items.map { it.title })

        service.delete(id)
        assertNull(service.playlist(id))
        assertEquals(listOf("A", "B", "C"), books.getBooks().map { it.title }.sorted(), "the texts stay in the library")
    }

    @Test
    fun anItemGoesWithItsBookAndWithTheCoursePackItCameFrom() = runTest {
        val language = portuguese()
        val lobo = book(language, "O lobo")
        val course = TestCourses.ALL[2]
        val id = service.create(language, PlaylistDraft("Mine"), listOf(PlaylistSource.Text(lobo), PlaylistSource.WholeCourse(course.id)))
        val observed = service.observePlaylist(id)
        assertEquals(3, observed.first()!!.items.size)

        bookService.delete(lobo)
        assertEquals(listOf("Lição 1", "Lição 2"), observed.first()!!.items.map { it.title })

        // Removing a course pack deletes its sample courses and the texts made from their lessons.
        courses.removeSamples(setOf(course.id))
        assertTrue(observed.first()!!.items.isEmpty())
    }

    @Test
    fun theListeningPlaceIsKeptApartAndForgottenWithItsItem() = runTest {
        val language = portuguese()
        val (a, b) = listOf("A", "B").map { book(language, it) }
        val repository = PlaylistRepositoryImpl(provider)
        val id = service.create(language, PlaylistDraft("Mine"), listOf(a, b).map { PlaylistSource.Text(it) })
        val items = service.playlist(id)!!.items
        repository.savePosition(id, ListeningPosition(items[1].id, 1, 3), playedAt = kotlin.time.Instant.parse("2026-10-22T10:00:00Z"))
        repository.setFinished(items[0].id, kotlin.time.Instant.parse("2026-10-22T09:59:00Z"))
        val playing = service.playlist(id)!!
        assertEquals(ListeningPosition(items[1].id, 1, 3), playing.position)
        assertEquals("B", playing.currentItem?.title)
        assertEquals(1, playing.finishedCount)
        assertEquals(0.5f, playing.progress)
        assertEquals(kotlin.time.Instant.parse("2026-10-22T10:00:00Z"), playing.lastPlayedAt)
        // Listening is no reading: the texts were never opened.
        assertTrue(books.getPages(a).all { it.startDate == null && it.readDate == null })

        service.remove(items[1].id)
        assertNull(service.playlist(id)!!.position, "a place in a removed item is no place")
        repository.clearFinished(id)
        assertEquals(0, service.playlist(id)!!.finishedCount)
    }

    @Test
    fun listeningTimeIsGuessedFromTheTextAndThenFromWhatWasHeard() = runTest {
        val language = portuguese()
        // 140 characters at the guessed 14 a second.
        assertEquals(10.seconds, service.duration(140, "pt"))
        settings.update { it.copy(speechSpeed = 2f) }
        assertEquals(5.seconds, service.duration(140, "pt"))
        assertEquals(14.seconds, service.duration(140, "ja"), "a script of syllables and words is said more slowly")

        // Heard: 300 characters took 30 s at double speed, so 5 a second at normal speed.
        time.record("pt", characters = 300, seconds = 30.0, speed = 2f)
        assertEquals(14.seconds, service.duration(140, "pt"))

        val lobo = book(language, "O lobo", "a".repeat(700))
        val id = service.create(language, PlaylistDraft("Mine"), listOf(PlaylistSource.Text(lobo)))
        assertEquals(70.seconds, service.duration(service.playlist(id)!!, "pt"))
        assertEquals("pt", service.languageCode(language))
    }

    @Test
    fun theLibraryOffersBooksNewsAndCoursesButNotLessonTexts() = runTest {
        val language = portuguese()
        val lobo = book(language, "O lobo")
        courses.openLesson(TestCourses.ALL[0].id, TestCourses.ALL[0].lessons[0].id)
        val library = service.library(language)
        assertEquals(listOf(lobo), library.texts.map { it.bookId })
        assertEquals(TestCourses.ALL.map { it.id }.toSet(), library.courses.map { it.id }.toSet())
    }

    @Test
    fun aWavFileTellsHowLongItPlays() {
        // 16-bit mono at 22,050 Hz: 44,100 bytes a second, here 1.5 s.
        val data = 66_150
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(22_050); putInt(44_100); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data)
        }.array()
        assertEquals(1.5, wavSeconds(header + ByteArray(data)))
        assertNull(wavSeconds("not audio".toByteArray()))
    }

    @Test
    fun aDatabaseFromBeforePlaylistsGetsThem() = runTest {
        val language = portuguese()
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.createStatement().execute("DROP TABLE playlist_items")
            c.createStatement().execute("DROP TABLE playlists")
            c.createStatement().execute("PRAGMA user_version = 25")
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val again = PlaylistService(PlaylistRepositoryImpl(reopened), courses, languages, time)
        val id = again.create(language, PlaylistDraft("After the update"), listOf(PlaylistSource.Text(book(language, "O lobo"))))
        assertEquals(listOf("O lobo"), again.playlist(id)!!.items.map { it.title })
    }
}
