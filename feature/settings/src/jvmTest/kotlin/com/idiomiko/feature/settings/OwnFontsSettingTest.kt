package com.idiomiko.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.fonts.FontFiles
import com.idiomiko.core.domain.fonts.OwnFont
import com.idiomiko.core.domain.fonts.OwnFontRepository
import com.idiomiko.core.ui.theme.LocalOwnFonts
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The reader's own fonts on the General page: added as the reading font, listed in the menu, and removed. */
@OptIn(ExperimentalTestApi::class)
class OwnFontsSettingTest {
    private val lora = File("../../core/ui/src/commonMain/composeResources/font/lora.ttf").readBytes()

    @Test
    fun anAddedFontBecomesTheReadingFontAndIsListedUntilRemoved() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val viewModel = SettingsViewModel(settings)
        val fonts = MemoryOwnFonts()
        setContent { Hosted(settings) { CompositionLocalProvider(LocalOwnFonts provides fonts) { SettingsScreen(onNavigate = {}, viewModel = viewModel) } } }
        onNodeWithTag("add-font").performScrollTo()

        val actions = OwnFontActions(fonts, viewModel)
        assertFalse(runBlocking { actions.add("notes.ttf", "not a font".encodeToByteArray()) })
        assertEquals("serif", settings.current.readingFont)
        assertTrue(runBlocking { actions.add("Mine-Regular.ttf", lora) })
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFont == "own:1" }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Mine").fetchSemanticsNodes().isNotEmpty() }

        // The menu lists it under its own heading, before the app's fonts.
        onAllNodesWithText("Mine").fetchSemanticsNodes().size.let { assertTrue(it >= 2, "the chosen font and the chip") }
        onNodeWithTag("own-font-1").performScrollTo()
        onNodeWithContentDescription("Remove Mine").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFont == "serif" }
        assertEquals(emptyList(), fonts.fonts.value)
    }

    @Test
    fun theFontMenuListsOwnFontsUnderTheirHeading() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val fonts = MemoryOwnFonts().also { runBlocking { it.add("Mine.ttf", lora) } }
        setContent { Hosted(settings) { CompositionLocalProvider(LocalOwnFonts provides fonts) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } } }
        onNodeWithText("System serif").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("JetBrains Mono").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, onAllNodesWithText("Your fonts").fetchSemanticsNodes().size, "the menu's heading and the setting's name")
        onAllNodesWithText("Mine").fetchSemanticsNodes().let { assertEquals(2, it.size, "in the menu and as a chip") }
    }

    /** Own fonts held in memory, numbered from 1. */
    private class MemoryOwnFonts : OwnFontRepository {
        private val files = mutableMapOf<Long, ByteArray>()
        override val fonts = MutableStateFlow<List<OwnFont>>(emptyList())
        override suspend fun add(fileName: String, bytes: ByteArray): OwnFont? {
            if (!FontFiles.isFont(bytes)) return null
            val font = OwnFont((files.keys.maxOrNull() ?: 0) + 1, FontFiles.nameOf(fileName))
            files[font.id] = bytes
            fonts.update { it + font }
            return font
        }
        override suspend fun remove(id: Long) {
            files.remove(id)
            fonts.update { list -> list.filter { it.id != id } }
        }
        override suspend fun data(id: Long): ByteArray? = files[id]
    }
}
