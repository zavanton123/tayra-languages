package com.tayra.languages.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
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
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Each opening of the dialog looks up its own language, whatever an earlier opening found, and says when nothing is left to download. */
@OptIn(ExperimentalTestApi::class)
class LanguageSetupDialogFlowTest {
    private class FakeCoursePacks(private val installed: Set<String>) : CoursePackStore {
        override suspend fun installedSize(pack: CoursePack): Long? = if (pack.id in installed) 1_000 else null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) {}
        override suspend fun remove(pack: CoursePack) {}
        override suspend fun courseIds(pack: CoursePack): Set<String> = emptySet()
        override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
    }

    private object NoDictionaries : DictionaryPackStore, DictionaryRepository {
        override suspend fun installedSize(pack: DictionaryPack): Long? = null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) {}
        override suspend fun remove(pack: DictionaryPack) {}
        override suspend fun isAvailable(dictionary: DictionaryId) = false
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) {}
    }

    @Test
    fun aLanguageWithNothingToDownloadDoesNotHideTheNextOnesDownloads() = runDesktopComposeUiTest(width = 900, height = 760) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-setup-flow", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val store = FakeCoursePacks(installed = setOf("courses-en"))
        val coursePacks = CoursePackService(store, CourseService(books, languages, BookService(books, languages), CourseRepositoryImpl(provider), InstalledCoursePacks(store)))
        val setup = LanguageSetupService(languages, settings, coursePacks, DictionaryService(NoDictionaries, NoDictionaries), LocalSpeech(emptyList()), LocalTranslation(null))
        val viewModel = LanguageSetupViewModel(setup)
        val unknown = runBlocking { languages.save(Language(name = "Klingon")) }
        val english = runBlocking { languages.save(Language(name = "English")) }
        val portuguese = runBlocking { languages.save(Language(name = "Portuguese")) }

        var open by mutableStateOf<Long?>(unknown)
        var closed = 0
        setContent {
            Hosted(settings) { LanguageSetupDialog(open, onClosed = { closed++; open = null }, viewModel = viewModel) }
        }
        waitUntil(timeoutMillis = 5_000) { closed == 1 }
        onNodeWithTag("language-setup").assertDoesNotExist()

        open = english
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithText("Everything available for English is already on this device.").assertExists() }.isSuccess }
        onNodeWithText("On this device").assertExists()
        onNodeWithText("Download selected").assertDoesNotExist()
        onNodeWithTag("setup-done").performClick()
        waitForIdle()
        assertEquals(2, closed)

        open = portuguese
        waitUntil(timeoutMillis = 5_000) { onNodeWithText("Get ready to learn Portuguese").let { runCatching { it.assertExists() }.isSuccess } }
        onNodeWithTag("setup-COURSES").assertExists()
        onNodeWithTag("setup-DICTIONARY").assertExists()
        assertEquals(2, closed)

        onNodeWithText("Set up later").performClick()
        waitForIdle()
        assertEquals(3, closed)
        open = portuguese
        waitUntil(timeoutMillis = 5_000) { onNodeWithText("Get ready to learn Portuguese").let { runCatching { it.assertExists() }.isSuccess } }
    }
}
