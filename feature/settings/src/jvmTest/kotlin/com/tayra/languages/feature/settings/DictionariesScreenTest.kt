package com.tayra.languages.feature.settings

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
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
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
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-dictionaries", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val store = FakeStore(setOf("be-ru", "bg-ru", "ca-ru", "hr-ru", "cs-ru", "en-ru", "fi-ru", "de-ru", "pt-ru", "pt-en"))
        val service = DictionaryService(store, FakeEntries)
        runBlocking {
            service.refresh()
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru") }
        }
        val viewModel = DictionariesViewModel(settings, service, languages)
        setContent { Hosted(settings) { DictionariesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Offline lookup ready")).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Portuguese → French · Wiktionary")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it) }

        // Nine Russian packs are installed; eight show until "View all", the language being learned first.
        onAllNodes(hasText("Portuguese → Russian")).onFirst().assertExists()
        onNodeWithText("View all 9 installed dictionaries").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Show fewer")).fetchSemanticsNodes().isNotEmpty() }

        // Downloading a pack for the language being learned moves it to the installed ones.
        onAllNodes(hasText("Download") and hasClickAction())[0].performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { "pt-ru" in store.installed && store.installed.size == 11 }

        // A lookup shows what the reader will show.
        onNodeWithText("Test lookup").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A Portuguese word")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("A Portuguese word").performTextInput("lobo")
        onNodeWithText("Look up").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("1. волк")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it.replace(".png", "-lookup.png"), root = 1) }
        onNodeWithText("Close").performClick()

        // A pack is removed from its menu.
        onNodeWithContentDescription("More for Belarusian → Russian").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Remove")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Remove").performClick()
        waitUntil(timeoutMillis = 5_000) { "be-ru" !in store.installed }
        assertFalse("be-ru" in store.installed)
    }

    /** A phone downloads the pair's dictionary with one button and switches between Installed and Available with a search. */
    @Test
    fun aPhoneDownloadsThePairWithOneButtonAndSearchesTheRest() = runDesktopComposeUiTest(width = 400, height = 1200) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-dictionaries-phone", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val store = FakeStore(setOf("en-ru", "de-ru"))
        val service = DictionaryService(store, FakeEntries)
        runBlocking {
            service.refresh()
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru") }
        }
        val viewModel = DictionariesViewModel(settings, service, languages)
        setContent { Hosted(settings) { DictionariesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Portuguese → Russian is not downloaded")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Manage downloaded dictionaries for offline lookup.").assertDoesNotExist()
        onNodeWithText("Installed 2").assertExists()
        System.getenv("DICTIONARIES_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onNodeWithTag("phone-download-current").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { "pt-ru" in store.installed }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Dictionary ready")).fetchSemanticsNodes().isNotEmpty() }

        // Available leads with the learning language; a search looks through everything.
        onNodeWithTag("phone-tab-AVAILABLE").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Available for Portuguese")).fetchSemanticsNodes().isNotEmpty() }
        // The pair's FreeDict pack has the same title, its source on the line below.
        onAllNodesWithText("Portuguese → French").assertCountEquals(2)
        onAllNodesWithText("Portuguese → French")[1].performScrollTo()
        onAllNodesWithText("FreeDict").onFirst().assertExists()
        onNodeWithText("Search dictionaries").performTextInput("Czech")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("All dictionaries")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Czech → Russian").onFirst().performScrollTo().assertExists()

        // Installed lists what is on the device, with removal from the row's menu.
        onNodeWithTag("phone-tab-INSTALLED").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Installed dictionaries")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("More for German → Russian · Wiktionary").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Remove")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Remove").performClick()
        waitUntil(timeoutMillis = 5_000) { "de-ru" !in store.installed }
    }

    private fun ComposeUiTest.save(path: String, root: Int = 0) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[root].captureToImage().toAwtImage(), "png", File(path))
    }
}
