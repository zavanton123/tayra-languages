package com.idiomiko.feature.settings

import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.dictionary.DictionaryEntry
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryPack
import com.idiomiko.core.domain.dictionary.DictionaryPackStore
import com.idiomiko.core.domain.dictionary.DictionarySense
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.service.DictionaryService
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Dictionaries page shows the reader's pair, the installed packs and the ones to download, and tests a lookup. */
@OptIn(ExperimentalTestApi::class)
class DictionariesScreenTest {

    private class FakeStore(installed: Set<String>) : DictionaryPackStore {
        val installed = installed.toMutableSet()
        override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id.name in installed) 2_000_000L + pack.id.name.hashCode() % 900_000 else null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) { installed += pack.id.name }
        override suspend fun remove(pack: DictionaryPack) { installed -= pack.id.name }
    }

    private object FakeEntries : DictionaryRepository {
        override suspend fun isAvailable(dictionary: DictionaryId) = true
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) =
            if (wordLc == "lobo") listOf(DictionaryEntry("lobo", "noun", "ˈlobu", listOf(DictionarySense(listOf("волк")), DictionarySense(listOf("хищник"))))) else emptyList()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) = Unit
    }

    @Test
    fun thePairIsReadyAndALookupCanBeTested() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-dictionaries", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val store = FakeStore(setOf("en-ru", "pt-ru"))
        val service = DictionaryService(store, FakeEntries)
        runBlocking {
            service.refresh()
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru") }
        }
        val viewModel = DictionariesViewModel(settings, service, languages)
        setContent { Hosted(settings) { DictionariesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Offline lookup ready")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it) }

        // The app keeps the pair's dictionary on its own, so there is nothing to download here.
        onNodeWithText("Downloaded dictionaries").assertDoesNotExist()
        onAllNodes(hasText("Download") and hasClickAction()).assertCountEquals(0)

        // A lookup shows what the reader will show.
        onNodeWithText("Test lookup").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A Portuguese word")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("A Portuguese word").performTextInput("lobo")
        onNodeWithText("Look up").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("1. волк")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it.replace(".png", "-lookup.png"), root = 1) }
    }

    /** A phone shows the pair's dictionary is missing and downloads it in place, as when the app's own download failed. */
    @Test
    fun aPhoneDownloadsAMissingDictionaryInPlace() = runDesktopComposeUiTest(width = 400, height = 1200) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-dictionaries-phone", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val store = FakeStore(setOf("en-ru"))
        val service = DictionaryService(store, FakeEntries)
        runBlocking {
            service.refresh()
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru") }
        }
        val viewModel = DictionariesViewModel(settings, service, languages)
        setContent { Hosted(settings) { DictionariesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Portuguese → Russian is not downloaded")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onNodeWithTag("phone-download-current").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Offline lookup ready")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("pt-ru" in store.installed)
        onNodeWithText("Test lookup").assertExists()
    }

    /** On a wide window the missing dictionary is downloaded from the lookup card, with its progress shown meanwhile. */
    @Test
    fun theMissingDictionaryIsDownloadedFromTheLookupCard() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-dictionaries-download", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val finish = CompletableDeferred<Unit>()
        val store = object : DictionaryPackStore {
            val installed = mutableSetOf<String>()
            override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id.name in installed) 2_000_000L else null
            override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) { onProgress(0.4f); finish.await(); installed += pack.id.name }
            override suspend fun remove(pack: DictionaryPack) { installed -= pack.id.name }
        }
        val service = DictionaryService(store, FakeEntries)
        runBlocking {
            service.refresh()
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru") }
        }
        val viewModel = DictionariesViewModel(settings, service, languages)
        setContent { Hosted(settings) { DictionariesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("download-current").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("download-current").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Downloading the dictionary…")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("download-current").assertDoesNotExist()
        finish.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Offline lookup ready")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("pt-ru" in store.installed)
    }

    private fun ComposeUiTest.save(path: String, root: Int = 0) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[root].captureToImage().toAwtImage(), "png", File(path))
    }
}
