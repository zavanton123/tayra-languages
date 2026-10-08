package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
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
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.LanguageSetupService
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.SetupFileKind
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TranslationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A newly chosen language is offered what it lacks: its courses, dictionary, a voice and translation models. */
class LanguageSetupServiceTest {
    private class FakeCoursePacks : CoursePackStore {
        val installed = mutableSetOf<String>()
        override suspend fun installedSize(pack: CoursePack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: CoursePack) { installed -= pack.id }
        override suspend fun courseIds(pack: CoursePack): Set<String> = if (pack.id in installed) setOf("${pack.languageCode}-mini-0100") else emptySet()
        override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
    }

    private class FakeDictionaries : DictionaryPackStore, DictionaryRepository {
        val installed = mutableSetOf<DictionaryId>()
        override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) { installed += pack.id }
        override suspend fun remove(pack: DictionaryPack) { installed -= pack.id }
        override suspend fun isAvailable(dictionary: DictionaryId) = dictionary in installed
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) {}
    }

    /** Piper with one Portuguese voice and a runtime that may still need installing. */
    private class FakePiper(var ready: Boolean) : LocalSpeechEngine {
        val voices = mutableSetOf<String>()
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = ""
        override suspend fun isReady() = ready
        override suspend fun setUp(): String { ready = true; return "ok" }
        override suspend fun packages() = listOf(
            SpeechPackage("pt_BR-cadu-low", "Cadu (low)", "pt", "Portuguese", 20_000_000, "pt_BR-cadu-low" in voices),
            SpeechPackage("pt_BR-faber-medium", "Faber (medium)", "pt", "Portuguese", 60_000_000, "pt_BR-faber-medium" in voices),
        )
        override suspend fun installPackage(id: String) { voices += id }
        override suspend fun removePackage(id: String) { voices -= id }
        override suspend fun voices(languageCode: String) = if (ready && languageCode == "pt") voices.map { SpeechVoice(it, it, "pt") } else emptyList()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    private class FakeTranslator(var runtime: Boolean = true) : LocalSentenceTranslator {
        val models = mutableSetOf<String>()
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
        override suspend fun setUp(): String { runtime = true; return "ok" }
        override suspend fun runtimeDownloadSize(): Long? = if (runtime) null else 200_000_000
        override fun knownPackages() = listOf(LocalPackage("pt", "en", "Portuguese", "English", false, 50_000_000))
        override suspend fun packages(): List<LocalPackage> {
            check(runtime) { "not set up" }
            return listOf(LocalPackage("pt", "en", "Portuguese", "English", "pt-en" in models, 50_000_000))
        }
        override suspend fun installPackage(fromCode: String, toCode: String) { models += "$fromCode-$toCode" }
        override suspend fun removePackage(fromCode: String, toCode: String) { models -= "$fromCode-$toCode" }
        override suspend fun translate(text: String, language: Language): String? = null
    }

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-setup", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val coursePackStore = FakeCoursePacks()
    private val coursePacks = CoursePackService(
        coursePackStore,
        CourseService(books, languages, BookService(books, languages), CourseRepositoryImpl(provider), InstalledCoursePacks(coursePackStore)),
    )
    private val dictionaryStore = FakeDictionaries()
    private val piper = FakePiper(ready = true)
    private val translator = FakeTranslator()
    private val setup = LanguageSetupService(
        languages, settings, coursePacks, DictionaryService(dictionaryStore, dictionaryStore), LocalSpeech(listOf(piper)), LocalTranslation(translator),
    )

    @Test
    fun everythingMissingIsOfferedAndInstalledInOrder() = runBlocking {
        val id = languages.save(Language(name = "Portuguese"))
        val items = setup.missing(id)
        assertEquals(listOf(SetupKind.COURSES, SetupKind.DICTIONARY, SetupKind.VOICE, SetupKind.TRANSLATION), items.map { it.kind })
        assertTrue(items.all { it.recommended }, "nothing large, and every engine is in use or the system voice")
        val voice = items.single { it.kind == SetupKind.VOICE }
        assertTrue("Faber" in voice.name, "a medium voice is picked over a low one: ${voice.name}")
        assertTrue(voice.switchesEngine, "the system voices were in use, so the voice brings Piper in")
        assertEquals(listOf(SetupFileKind.VOICE), voice.files.map { it.kind })
        assertEquals(60_000_000, voice.sizeBytes)
        assertEquals(listOf("Portuguese \u2192 English" to 50_000_000L), items.single { it.kind == SetupKind.TRANSLATION }.files.map { it.name to it.sizeBytes })

        setup.install(items)
        withTimeout(5_000) { setup.states.first { s -> items.all { s[it.id] == SetupState.Done } } }
        assertTrue(coursePackStore.installed.contains("courses-pt"))
        assertTrue(dictionaryStore.installed.contains(DictionaryId("pt", "en")))
        assertEquals(setOf("pt_BR-faber-medium"), piper.voices)
        assertEquals(setOf("pt-en"), translator.models)
        assertEquals(SpeechEngine.PIPER, settings.current.speechEngine)
        assertEquals(TranslationEngine.ARGOS, settings.current.translationEngine)

        assertEquals(emptyList(), setup.missing(id), "nothing is offered once everything is there")
    }

    @Test
    fun aRuntimeStillToInstallIsOfferedUnticked() = runBlocking {
        piper.ready = false
        val id = languages.save(Language(name = "Portuguese"))
        val voice = setup.missing(id).single { it.kind == SetupKind.VOICE }
        assertTrue(voice.includesRuntime)
        assertEquals(listOf(SetupFileKind.ENGINE, SetupFileKind.VOICE), voice.files.map { it.kind })
        assertFalse(voice.recommended, "the speech runtime is a large download, left to the learner to tick")
        setup.install(listOf(voice))
        withTimeout(5_000) { setup.states.first { it[voice.id] == SetupState.Done } }
        assertTrue(piper.ready && piper.voices.isNotEmpty())
    }

    @Test
    fun theNativeLanguageNeedsNoDictionaryOrTranslation() = runBlocking {
        val id = languages.save(Language(name = "English"))
        val kinds = setup.missing(id).map { it.kind }
        assertTrue(SetupKind.DICTIONARY !in kinds && SetupKind.TRANSLATION !in kinds, "got $kinds")
        assertTrue(SetupKind.COURSES in kinds)
    }

    @Test
    fun beforeTheTranslatorIsSetUpItsKnownModelsDecideAndAreSized() = runBlocking {
        translator.runtime = false
        val icelandic = languages.save(Language(name = "Icelandic"))
        assertTrue(setup.missing(icelandic).none { it.kind == SetupKind.TRANSLATION }, "no model translates Icelandic, so nothing is offered")

        val portuguese = languages.save(Language(name = "Portuguese"))
        val item = setup.missing(portuguese).single { it.kind == SetupKind.TRANSLATION }
        assertEquals(listOf(SetupFileKind.ENGINE to 200_000_000L, SetupFileKind.MODEL to 50_000_000L), item.files.map { it.kind to it.sizeBytes })
        assertTrue(item.sizeEstimated, "the engine's size is an estimate")
        setup.install(listOf(item))
        withTimeout(5_000) { setup.states.first { it[item.id] == SetupState.Done } }
        assertEquals(setOf("pt-en"), translator.models)
    }
}
