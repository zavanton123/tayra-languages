package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** One language is being learned at a time, never the native one; when none is chosen, or the chosen one is gone, one is picked. */
class LearningLanguageTest {

    @Test
    fun aLanguageIsPickedWhenNoneIsChosen() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-learning-language", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = LearningLanguageService(settings, languages, books)
        assertEquals(0, service.ensure(), "nothing to pick without languages")

        val czech = languages.save(Language(name = "Czech"))
        val polish = languages.save(Language(name = "Polish"))
        val portuguese = languages.save(Language(name = "Portuguese"))
        assertEquals(czech, service.ensure(), "without books, the first language by name")

        // A language with a book wins over one without, and the book read last over the others.
        settings.update { it.copy(currentLanguageId = 0) }
        val bookService = BookService(books, languages)
        bookService.create(BookDraft(languageId = polish, title = "Kot", text = "Kot śpi."))
        assertEquals(polish, service.ensure())
        settings.update { it.copy(currentLanguageId = 0) }
        val read = bookService.create(BookDraft(languageId = portuguese, title = "Lobo", text = "O lobo dorme."))
        ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages)).openPage(read, 1, trackOpen = true)
        assertEquals(portuguese, service.ensure())
        assertEquals(portuguese, settings.current.currentLanguageId)

        // A choice that still exists is kept; one that is gone is replaced.
        service.select(czech)
        assertEquals(czech, service.ensure())
        settings.update { it.copy(currentLanguageId = 987_654) }
        assertEquals(portuguese, service.ensure())
    }

    /** The app leaves the first language to the user; one that is gone is still replaced. */
    @Test
    fun anUnchosenLanguageIsLeftForTheUser() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-learning-unchosen", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = LearningLanguageService(settings, languages, BookRepositoryImpl(provider))
        val czech = languages.save(Language(name = "Czech"))
        assertEquals(0, service.ensure(pickUnchosen = false))
        assertEquals(0, settings.current.currentLanguageId)

        settings.update { it.copy(currentLanguageId = 987_654) }
        assertEquals(czech, service.ensure(pickUnchosen = false))
    }

    @Test
    fun theNativeLanguageIsNeverTheOneLearned() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-learning-native", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = LearningLanguageService(settings, languages, BookRepositoryImpl(provider))
        settings.update { it.copy(nativeLanguage = "bg") }
        val bulgarian = languages.save(Language(name = "Bulgarian"))
        val czech = languages.save(Language(name = "Czech"))
        assertEquals(czech, service.ensure(), "the first language by name is passed over, being the native one")

        assertFalse(service.select(bulgarian))
        assertEquals(czech, settings.current.currentLanguageId)
        assertTrue(service.select(czech))
    }

    /** The languages being learned are first those with texts or saved words, and the current one; each one chosen joins them. */
    @Test
    fun theLanguagesBeingLearnedAreTheUsedOnesAndEachChosen() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-learning-mine", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val service = LearningLanguageService(settings, languages, books, terms)
        settings.update { it.copy(nativeLanguage = "bg") }
        languages.save(Language(name = "Bulgarian"))
        val czech = languages.save(Language(name = "Czech"))
        val german = languages.save(Language(name = "German"))
        val polish = languages.save(Language(name = "Polish"))
        val spanish = languages.save(Language(name = "Spanish"))
        languages.save(Language(name = "Swedish"))
        BookService(books, languages).create(BookDraft(languageId = polish, title = "Kot", text = "Kot śpi."))
        TermService(terms, languages).save(TermDraft(languageId = german, text = "Hund", status = TermStatus.NEW_1, statusExplicitlySet = true))
        // An ignored word is no sign of learning.
        TermService(terms, languages).save(TermDraft(languageId = spanish, text = "y", status = TermStatus.IGNORED, statusExplicitlySet = true))
        settings.update { it.copy(currentLanguageId = czech) }

        service.fillMine()
        assertEquals(listOf(czech, german, polish), service.observeMine().first())

        service.select(spanish)
        assertEquals(listOf(czech, german, polish, spanish), service.observeMine().first())
        service.select(czech)
        assertEquals(listOf(czech, german, polish, spanish), service.observeMine().first(), "a language is listed once")
        service.fillMine()
        assertEquals(listOf(czech, german, polish, spanish), service.observeMine().first(), "filled only the first time")
    }
}
