package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStore
import com.tayra.languages.core.domain.books.PackedBook
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStore
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.InstalledCoursePacks
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.LanguageRemoval
import com.tayra.languages.core.domain.service.LanguageRemovalService
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Removing a language deletes its library and its downloads, and leaves the other languages' alone. */
class LanguageRemovalServiceTest {
    private class FakeCoursePacks : CoursePackStore {
        val installed = mutableSetOf<String>()
        override suspend fun installedSize(pack: CoursePack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: CoursePack) { installed -= pack.id }
        override suspend fun courseIds(pack: CoursePack): Set<String> = emptySet()
        override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
    }

    private class FakeBookPacks : BookPackStore {
        val installed = mutableSetOf<String>()
        override suspend fun installedSize(pack: BookPack): Long? = if (pack.id in installed) 2_000 else null
        override suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: BookPack) { installed -= pack.id }
        override suspend fun books(pack: BookPack): List<PackedBook> = emptyList()
    }

    private class FakeDictionaries : DictionaryPackStore, DictionaryRepository {
        val installed = mutableSetOf<DictionaryId>()
        override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id in installed) 4_000 else null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: DictionaryPack) { installed -= pack.id }
        override suspend fun isAvailable(dictionary: DictionaryId) = dictionary in installed
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) {}
    }

    private class FakePiper : LocalSpeechEngine {
        val voices = mutableSetOf("pt_BR-faber-medium", "de_DE-thorsten-medium", "kokoro")
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = ""
        override suspend fun isReady() = true
        override suspend fun setUp() = "ok"
        override suspend fun packages() = listOf(
            SpeechPackage("pt_BR-faber-medium", "Faber", "pt", "Portuguese", 8_000, "pt_BR-faber-medium" in voices),
            SpeechPackage("de_DE-thorsten-medium", "Thorsten", "de", "German", 8_000, "de_DE-thorsten-medium" in voices),
            // One model of many languages, which no language removes.
            SpeechPackage("kokoro", "Kokoro", null, "Many", 300_000, "kokoro" in voices),
        )
        override suspend fun installPackage(id: String) { voices += id }
        override suspend fun removePackage(id: String) { voices -= id }
        override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    private class FakeTranslator : LocalSentenceTranslator {
        val models = mutableSetOf("pt-en", "en-pt", "de-en", "en-ru", "ru-en")
        override val displayName = "Argos Translate"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = true
        override val lastError = MutableStateFlow<LocalTranslationProblem?>(null)
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = ""
        override suspend fun prepare(fromCode: String, toCode: String, fromName: String, toName: String) {}
        override suspend fun canTranslate(fromCode: String, toCode: String) = "$fromCode-$toCode" in models
        override suspend fun installModels(fromCode: String, toCode: String) {}
        override suspend fun setUp() = "ok"
        override suspend fun packages() = listOf("pt-en", "en-pt", "de-en", "en-ru", "ru-en").map {
            val (from, to) = it.split('-')
            LocalPackage(from, to, from, to, it in models, sizeBytes = 16_000)
        }
        override suspend fun installPackage(fromCode: String, toCode: String) { models += "$fromCode-$toCode" }
        override suspend fun removePackage(fromCode: String, toCode: String) { models -= "$fromCode-$toCode" }
        override suspend fun translate(text: String, language: Language): String? = null
    }

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-remove-language", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val bookService = BookService(books, languages)
    private val learning = LearningLanguageService(settings, languages, books, terms)
    private val coursePackStore = FakeCoursePacks()
    private val bookPackStore = FakeBookPacks()
    private val dictionaryStore = FakeDictionaries()
    private val piper = FakePiper()
    private val translator = FakeTranslator()
    private val service = LanguageRemovalService(
        settings, languages, LanguageService(languages, bookService, settings), learning, books, terms,
        CoursePackService(coursePackStore, CourseService(books, languages, bookService, CourseRepositoryImpl(provider), InstalledCoursePacks(coursePackStore))),
        BookPackService(bookPackStore, books, bookService, languages),
        DictionaryService(dictionaryStore, dictionaryStore),
        LocalSpeech(listOf(piper)),
        LocalTranslation(translator),
    )

    @Test
    fun aLanguageGoesWithItsLibraryAndDownloads() = runBlocking<Unit> {
        settings.update { it.copy(nativeLanguage = "ru") }
        val portuguese = languages.save(Language(name = "Portuguese"))
        val german = languages.save(Language(name = "German"))
        bookService.create(BookDraft(languageId = portuguese, title = "Lobo", text = "O lobo dorme."))
        bookService.create(BookDraft(languageId = german, title = "Wolf", text = "Der Wolf schläft."))
        TermService(terms, languages).save(TermDraft(languageId = portuguese, text = "lobo", status = TermStatus.NEW_1, statusExplicitlySet = true))
        TermService(terms, languages).save(TermDraft(languageId = portuguese, text = "dorme", status = TermStatus.WELL_KNOWN, statusExplicitlySet = true))
        coursePackStore.installed += setOf("courses-pt", "courses-en")
        bookPackStore.installed += "books-pt"
        val ptRu = DictionaryPacks.find("pt", "ru")!!.id
        val deRu = DictionaryPacks.find("de", "ru")!!.id
        dictionaryStore.installed += setOf(ptRu, deRu)
        settings.update { it.copy(speechVoices = mapOf("PIPER:pt" to "pt_BR-faber-medium", "PIPER:de" to "de_DE-thorsten-medium")) }
        learning.select(german)
        learning.select(portuguese)

        // A course pack of 1,000 bytes, a book pack of 2,000, a dictionary of 4,000, a voice of 8,000 and two translation models of 16,000.
        assertEquals(LanguageRemoval(texts = 1, words = 2, downloadBytes = 47_000), service.preview(portuguese))

        assertEquals(emptyList(), service.remove(portuguese))
        assertEquals(german, settings.current.currentLanguageId, "the next language being learned takes its place")
        assertEquals(listOf(german), learning.mine())
        assertEquals(listOf("Wolf"), books.getBooks().map { it.title })
        val fresh = languages.findByName("Portuguese")!!
        assertTrue(fresh.id != portuguese, "the language comes back empty")
        assertEquals(LanguageRemoval(0, 0, 0), service.preview(fresh.id))
        assertEquals(setOf("courses-en"), coursePackStore.installed)
        assertTrue(bookPackStore.installed.isEmpty())
        assertEquals(setOf(deRu), dictionaryStore.installed)
        assertEquals(setOf("de_DE-thorsten-medium", "kokoro"), piper.voices)
        assertEquals(mapOf("PIPER:de" to "de_DE-thorsten-medium"), settings.current.speechVoices)
        // German goes through English to Russian, so English and Russian's models stay.
        assertEquals(setOf("de-en", "en-ru", "ru-en"), translator.models)
    }

    @Test
    fun removingTheLastLanguageLeavesTheChoiceToTheUser() = runBlocking<Unit> {
        val portuguese = languages.save(Language(name = "Portuguese"))
        learning.select(portuguese)
        service.remove(portuguese)
        assertEquals(0, settings.current.currentLanguageId)
        assertEquals(emptyList(), learning.mine())
    }
}
