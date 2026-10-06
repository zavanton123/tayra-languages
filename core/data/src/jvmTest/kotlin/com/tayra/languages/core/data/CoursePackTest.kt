package com.tayra.languages.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.coursepack.CoursePackRepository
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.InstalledCoursePacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A course pack is downloaded, its courses written for the language, and removed again with them. */
class CoursePackTest {
    private val pack = CoursePacks.forLanguage("pt").single()

    /** Builds a gzip-compressed pack the way tools/build_courses.py lays it out. */
    private fun buildPack(format: Int = CoursePack.FORMAT): ByteArray {
        val file = File.createTempFile("tayra-pack-src", ".sqlite").also { it.delete() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        listOf(
            "CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE courses (id TEXT NOT NULL PRIMARY KEY, position INTEGER NOT NULL, rank_up_to INTEGER, level TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, topic TEXT NOT NULL, tags TEXT NOT NULL)",
            "CREATE TABLE lessons (id TEXT NOT NULL PRIMARY KEY, course_id TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, new_words TEXT NOT NULL, tags TEXT NOT NULL, text TEXT NOT NULL)",
            "CREATE INDEX lessons_course ON lessons(course_id, position)",
            "INSERT INTO meta VALUES ('format', '$format'), ('language', 'pt')",
            """INSERT INTO courses VALUES
                ('pt-mini-0100', 0, 100, 'A1', 'Em casa', 'Stories at home.', 'Home', 'tayra'),
                ('pt-mini-0200', 1, 200, 'A2', 'O dia a dia', 'Daily routine.', 'Daily life', 'tayra')""",
            """INSERT INTO lessons VALUES
                ('pt-mini-0100-02', 'pt-mini-0100', 1, 'O gato', 'A cat.', 'gato casa', 'tayra', 'O gato está em casa.'),
                ('pt-mini-0100-01', 'pt-mini-0100', 0, 'A casa nova', 'A new house.', 'casa nova', 'tayra', 'A Ana tem uma casa nova.'),
                ('pt-mini-0200-01', 'pt-mini-0200', 0, 'A manhã', 'Morning.', 'manhã', 'tayra', 'De manhã, o Pedro acorda cedo.')""",
            "PRAGMA user_version = $format",
        ).forEach { driver.execute(null, it, 0) }
        driver.close()
        val packed = ByteArrayOutputStream()
        GZIPOutputStream(packed).use { it.write(file.readBytes()) }
        file.delete()
        return packed.toByteArray()
    }

    private class Env(body: ByteArray?) {
        val directory: File = Files.createTempDirectory("tayra-packs").toFile()
        private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-packs", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        private val client = HttpClient(MockEngine { if (body == null) respondError(HttpStatusCode.NotFound) else respond(body) })
        val store = CoursePackRepository(CoursePackFiles(DictionaryDownloader(client), directory))
        val courses = CourseService(books, languages, BookService(books, languages), CourseRepositoryImpl(provider), InstalledCoursePacks(store))
        val packs = CoursePackService(store, courses)
    }

    @Test
    fun aDownloadedPackWritesItsCoursesAndRemovingTakesThemAway() = runTest {
        val env = Env(buildPack())
        val pt = env.languages.save(Language(name = "Portuguese"))
        env.courses.seedSamples()
        env.packs.refresh()
        assertEquals(PackState.NotInstalled, env.packs.packs.value.single().state)
        assertEquals(emptyList(), env.courses.observeCourses(pt).first())

        env.packs.download(pack)
        assertIs<PackState.Installed>(env.packs.packs.value.single().state)
        val courses = env.courses.observeCourses(pt).first().map { it.course }
        assertEquals(listOf("pt-mini-0100", "pt-mini-0200"), courses.map { it.id })
        val first = courses.first()
        assertEquals(CourseLevel.A1, first.level)
        assertEquals(100, first.rankUpTo)
        assertTrue(first.builtIn, "the pack's courses are samples")
        assertEquals(listOf("A casa nova", "O gato"), first.lessons.map { it.title }, "lessons in their order")
        assertEquals(listOf("casa", "nova"), first.lessons.first().newWords)
        assertEquals(listOf("tayra"), first.tags, "the app's own courses are tagged")
        assertEquals(listOf("tayra"), first.lessons.first().tags)

        val own = env.courses.createCourse(pt, CourseDraft("Minhas leituras"))
        val opened = assertNotNull(env.courses.openLesson("pt-mini-0100", "pt-mini-0100-01"))
        assertEquals(listOf("tayra"), env.books.getBook(opened)!!.tags, "the lesson's text carries its tags")
        env.packs.remove(pack)
        assertEquals(PackState.NotInstalled, env.packs.packs.value.single().state)
        assertEquals(listOf(own), env.courses.observeCourses(pt).first().map { it.course.id }, "the reader's own course stays")
        assertNull(env.books.getBook(opened), "the text read from a lesson goes with it")
        assertTrue(env.directory.listFiles().orEmpty().isEmpty(), "the file is deleted")

        env.packs.download(pack)
        assertEquals(listOf("pt-mini-0100", "pt-mini-0200", own), env.courses.observeCourses(pt).first().map { it.course.id }, "installing again restores them")
    }

    @Test
    fun aLanguageAddedLaterGetsTheInstalledCourses() = runTest {
        val env = Env(buildPack())
        env.packs.download(pack)
        val pt = env.languages.save(Language(name = "Portuguese"))
        env.courses.seedSamples()
        assertEquals(2, env.courses.observeCourses(pt).first().size)
    }

    @Test
    fun aPackOfAnotherFormatOrAFailedDownloadAddsNothing() = runTest {
        val newer = Env(buildPack(format = CoursePack.FORMAT + 1))
        val pt = newer.languages.save(Language(name = "Portuguese"))
        newer.packs.download(pack)
        assertIs<PackState.Failed>(newer.packs.packs.value.single().state)
        assertEquals(emptyList(), newer.courses.observeCourses(pt).first())

        val missing = Env(null)
        missing.packs.download(pack)
        assertIs<PackState.Failed>(missing.packs.packs.value.single().state)
    }
}
