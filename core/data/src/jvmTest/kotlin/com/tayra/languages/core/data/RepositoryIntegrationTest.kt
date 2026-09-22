package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.DatabaseMaintenanceImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermValidationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryIntegrationTest {

    private class Env {
        val file: File = File.createTempFile("tayra-test", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val wordsRead = WordsReadRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages)
        val readingService = ReadingService(books, languages, terms, wordsRead, termService)
        val bookService = BookService(books, languages)
        val statsService = BookStatsService(books, languages, settings, readingService)
        val languageService = LanguageService(languages, bookService, settings)
        val popups = TermPopupBuilder(terms, languages, settings, readingService)
        val demo = DemoDataService(DatabaseMaintenanceImpl(provider), languages, books, languageService, settings)

        suspend fun english(): Long = languages.save(
            Language(
                name = "English",
                dictionaries = listOf(
                    LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.EMBEDDED, url = "https://x/[LUTE]"),
                    LanguageDictionary(useFor = DictionaryUse.SENTENCES, type = DictionaryType.POPUP, url = "https://y/[LUTE]"),
                ),
            ),
        )
    }

    @Test
    fun languageRoundTrip() = runTest {
        val env = Env()
        val id = env.english()
        val loaded = env.languages.getById(id)
        assertNotNull(loaded)
        assertEquals("English", loaded.name)
        assertEquals(2, loaded.dictionaries.size)
        assertEquals(DictionaryUse.SENTENCES, loaded.dictionaries[1].useFor)
        assertEquals(listOf("English"), env.languages.observeAll().first().map { it.name })
    }

    @Test
    fun bookIsSplitAndRendered() = runTest {
        val env = Env()
        val langId = env.english()
        val bookId = env.bookService.create(
            BookDraft(languageId = langId, title = "Test", text = "Here is a dog. Here is a cat.\n\nAnother paragraph.", tags = listOf("demo")),
        )
        val page = env.readingService.openPage(bookId, 1, trackOpen = true)
        assertEquals(1, page.pageCount)
        assertEquals(3, page.rendered.paragraphs.size, "blank line becomes an empty paragraph")
        val words = page.rendered.words
        assertTrue(words.all { it.termId != null }, "all words get persisted terms")
        assertTrue(words.all { it.status == TermStatus.UNKNOWN })
        assertEquals(setOf("here", "is", "a", "dog", "cat", "another", "paragraph"), words.map { it.textLc }.toSet())

        val list = env.books.observeBooks(archived = false).first()
        assertEquals(1, list.size)
        assertEquals(listOf("demo"), list[0].tags)
        assertEquals(10, list[0].wordCount)
        assertNotNull(list[0].lastOpened)

        val stats = env.statsService.stats(bookId)
        assertNotNull(stats)
        assertEquals(7, stats.distinctTerms)
        assertEquals(100, stats.unknownPercent)
    }

    @Test
    fun termSaveWithParentAndSync() = runTest {
        val env = Env()
        val langId = env.english()
        val draft = env.termService.findOrNew(langId, "dogs").copy(
            translation = "plural of dog",
            status = TermStatus.LEARNING_3,
            statusExplicitlySet = true,
            parents = listOf("dog"),
            syncStatus = true,
            tags = listOf("noun"),
        )
        val id = env.termService.save(draft)
        val saved = env.terms.getById(id)
        assertNotNull(saved)
        assertEquals(listOf("noun"), saved.tags)
        assertEquals(listOf("dog"), saved.parents.map { it.text })
        assertTrue(saved.syncStatus)

        val parent = env.termService.find(langId, "dog")
        assertNotNull(parent)
        assertEquals(TermStatus.LEARNING_3, parent.status)
        assertEquals("plural of dog", parent.translation)
        assertEquals(listOf("noun"), parent.tags)

        // Changing the parent status updates the following child.
        env.termService.setStatus(listOf(parent.id), TermStatus.LEARNED)
        assertEquals(TermStatus.LEARNED, env.terms.getById(id)?.status)

        // Duplicate detection.
        val dup = assertFailsWith<TermValidationException> {
            env.termService.save(TermDraft(languageId = langId, text = "DOGS", originalText = ""))
        }
        assertNotNull(dup.duplicateOf)

        // Case-only change is allowed.
        env.termService.save(env.termService.load(id).copy(text = "Dogs"))
        assertEquals("Dogs", env.terms.getById(id)?.text)

        val matches = env.termService.search(langId, "do")
        assertEquals(listOf("dog", "Dogs"), matches.map { it.text })
        assertTrue(matches[0].hasChildren)
    }

    @Test
    fun multiwordTermsRenderAndListFilter() = runTest {
        val env = Env()
        val langId = env.english()
        val bookId = env.bookService.create(BookDraft(languageId = langId, title = "MW", text = "The black cat sleeps."))
        env.termService.save(env.termService.findOrNew(langId, "black cat").copy(translation = "gato negro", status = TermStatus.NEW_2, statusExplicitlySet = true))

        val page = env.readingService.openPage(bookId, 1, trackOpen = false)
        val texts = page.rendered.items.map { it.renderText }
        assertEquals(listOf("The", " ", "black cat", " ", "sleeps", "."), texts)

        val listed = env.terms.list(TermListFilter(languageId = langId, minStatus = TermStatus.NEW_1), TermListSort(), 0, 50)
        assertEquals(listOf("black​ ​cat"), listed.items.map { it.text })
        assertEquals(1, listed.totalCount)

        val all = env.terms.list(TermListFilter(languageId = langId, search = "sle"), TermListSort(), 0, 50)
        assertEquals(listOf("sleeps"), all.items.map { it.text })

        env.readingService.markPageRead(bookId, 1, markRestAsKnown = true)
        val refs = env.termService.references(langId, "black cat")
        assertEquals(1, refs.term.size)
        assertEquals("The **black cat** sleeps.", refs.term[0].sentence)
        assertEquals(TermStatus.WELL_KNOWN, env.termService.find(langId, "sleeps")?.status)
        assertEquals(4, env.wordsRead.dailyCounts().sumOf { it.wordCount })

        val popup = env.popups.build(env.termService.find(langId, "black cat")!!.id)
        assertNotNull(popup)
        assertEquals("gato negro", popup.translation)
        assertNull(env.popups.build(env.termService.find(langId, "the")!!.id))
    }

    @Test
    fun pageEditing() = runTest {
        val env = Env()
        val langId = env.english()
        val bookId = env.bookService.create(BookDraft(languageId = langId, title = "Pages", text = "one.\n---\ntwo.\n---\nthree."))
        assertEquals(3, env.books.pageCount(bookId))
        env.bookService.addPage(bookId, com.tayra.languages.core.domain.service.PagePosition.AFTER, 1, "inserted.")
        assertEquals(listOf("one.", "inserted.", "two.", "three."), env.books.getPages(bookId).map { it.text })
        assertTrue(env.bookService.deletePage(bookId, 3))
        assertEquals(listOf(1, 2, 3), env.books.getPages(bookId).map { it.order })
        assertEquals(listOf("one.", "inserted.", "three."), env.books.getPages(bookId).map { it.text })
    }

    @Test
    fun demoDataLoadsAndWipes() = runTest {
        val env = Env()
        env.demo.loadIfEmpty()
        assertTrue(env.demo.isDemoData)
        val names = env.languages.getAll().map { it.name }
        assertTrue("English" in names && "Spanish" in names && "Arabic" in names, names.toString())
        assertTrue("Japanese" !in names, "mecab-based parser is unsupported")
        assertNotNull(env.demo.tutorialBookId())
        assertEquals(12, env.languages.observeSummaries().first().size)
        env.demo.wipeDatabase()
        assertTrue(env.languages.getAll().isEmpty())
        assertTrue(env.books.getBooks().isEmpty())
    }
}
