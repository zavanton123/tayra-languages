package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.DatabaseMaintenanceImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
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
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ClearDataService
import com.tayra.languages.core.domain.service.ClearStep
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.SpeechAudioCache
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Clearing removes the library and every download, and carries on past a step that fails. */
class ClearDataServiceTest {
    private class FakeCoursePacks : CoursePackStore {
        val installed = mutableSetOf<String>()
        override suspend fun installedSize(pack: CoursePack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: CoursePack) { installed -= pack.id }
        override suspend fun courseIds(pack: CoursePack): Set<String> = emptySet()
        override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
    }

    private class FakeDictionaries(var failing: Boolean = false) : DictionaryPackStore, DictionaryRepository {
        val installed = mutableSetOf<DictionaryId>()
        override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: DictionaryPack) {
            if (failing) error("disk is read-only")
            installed -= pack.id
        }
        override suspend fun isAvailable(dictionary: DictionaryId) = dictionary in installed
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) {}
    }

    private class FakePiper : LocalSpeechEngine {
        val voices = mutableSetOf("pt_BR-faber-medium")
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = ""
        override suspend fun isReady() = true
        override suspend fun setUp() = "ok"
        override suspend fun packages() = listOf(SpeechPackage("pt_BR-faber-medium", "Faber", "pt", "Portuguese", 1, "pt_BR-faber-medium" in voices))
        override suspend fun installPackage(id: String) { voices += id }
        override suspend fun removePackage(id: String) { voices -= id }
        override suspend fun voices(languageCode: String) = voices.map { SpeechVoice(it, it, "pt") }
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    private class FakeTranslator : LocalSentenceTranslator {
        val models = mutableSetOf("pt-en")
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
        override suspend fun packages() = listOf(LocalPackage("pt", "en", "Portuguese", "English", "pt-en" in models))
        override suspend fun installPackage(fromCode: String, toCode: String) { models += "$fromCode-$toCode" }
        override suspend fun removePackage(fromCode: String, toCode: String) { models -= "$fromCode-$toCode" }
        override suspend fun translate(text: String, language: Language): String? = null
    }

    private class FakeAudioCache : SpeechAudioCache {
        val entries = mutableMapOf("a" to byteArrayOf(1))
        override suspend fun read(key: String) = entries[key]
        override suspend fun write(key: String, audio: ByteArray) { entries[key] = audio }
        override suspend fun clear() = entries.clear()
    }

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-clear", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val bookService = BookService(books, languages)
    private val coursePackStore = FakeCoursePacks()
    private val dictionaryStore = FakeDictionaries()
    private val piper = FakePiper()
    private val translator = FakeTranslator()
    private val cache = FakeAudioCache()
    private val service = ClearDataService(
        DemoDataService(DatabaseMaintenanceImpl(provider), languages, books, LanguageService(languages, bookService, settings), settings),
        CoursePackService(coursePackStore, CourseService(books, languages, bookService, CourseRepositoryImpl(provider), InstalledCoursePacks(coursePackStore))),
        DictionaryService(dictionaryStore, dictionaryStore),
        LocalSpeech(listOf(piper)),
        LocalTranslation(translator),
        cache,
        settings,
    )

    private suspend fun fillTheDevice(): Long {
        val portuguese = languages.save(Language(name = "Portuguese"))
        bookService.create(BookDraft(languageId = portuguese, title = "Lobo", text = "O lobo dorme."))
        coursePackStore.installed += "courses-pt"
        dictionaryStore.installed += DictionaryPacks.find("pt", "en")!!.id
        settings.update { it.copy(speechVoices = mapOf("PIPER:pt" to "pt_BR-faber-medium"), currentLanguageId = portuguese) }
        return portuguese
    }

    @Test
    fun everythingGoesAndTheCatalogLanguagesComeBackEmpty() = runBlocking {
        val portuguese = fillTheDevice()
        assertEquals(emptyMap(), service.clearEverything())

        assertTrue(books.observeBooks(archived = false).first().isEmpty())
        assertTrue(languages.getAll().isNotEmpty() && languages.getAll().none { it.id == portuguese }, "the catalog languages are recreated afresh")
        assertTrue(coursePackStore.installed.isEmpty() && dictionaryStore.installed.isEmpty())
        assertTrue(piper.voices.isEmpty() && translator.models.isEmpty() && cache.entries.isEmpty())
        assertEquals(emptyMap(), settings.current.speechVoices)
        assertEquals(0, settings.current.currentLanguageId)
        assertTrue(service.running.value == null)
    }

    @Test
    fun aStepThatFailsIsReportedAndTheRestStillRuns() = runBlocking {
        fillTheDevice()
        dictionaryStore.failing = true
        val problems = service.clearEverything()
        assertEquals(setOf(ClearStep.DICTIONARIES), problems.keys)
        assertEquals("disk is read-only", problems[ClearStep.DICTIONARIES])
        assertTrue(dictionaryStore.installed.isNotEmpty(), "the failed step left its pack")
        assertTrue(books.observeBooks(archived = false).first().isEmpty() && piper.voices.isEmpty() && cache.entries.isEmpty(), "the other steps went on")
        assertTrue(service.running.value == null)
        assertTrue(PackState.NotInstalled == PackState.NotInstalled)
    }
}
