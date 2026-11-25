package com.idiomiko.core.data

import com.idiomiko.core.data.settings.SETTINGS_FILE
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.data.settings.desktopSettings
import com.idiomiko.core.domain.settings.Hotkey
import com.idiomiko.core.domain.settings.HotkeyAction
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.prefs.AbstractPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The desktop app's settings file: started once from the old preferences, then the app's alone, and always written whole. */
class DesktopSettingsTest {
    private val folder: File = Files.createTempDirectory("idiomiko-settings").toFile()

    @Test
    fun theFirstStartCarriesThePreferencesOver() {
        val former = MemoryPreferences(mapOf("current_theme" to "nord", "hotkey_NEXT_WORD" to "Ctrl+K", "daily_goal_minutes" to "30"))
        val settings = desktopSettings(folder) { former }
        assertEquals("nord", settings.getString("current_theme", ""))
        assertEquals("Ctrl+K", settings.getString("hotkey_NEXT_WORD", ""))
        assertEquals(30, settings.getInt("daily_goal_minutes", 0))
        assertTrue(File(folder, SETTINGS_FILE).exists())
    }

    @Test
    fun laterStartsKeepToTheFile() {
        desktopSettings(folder) { MemoryPreferences(mapOf("current_theme" to "nord")) }.putString("current_theme", "sepia")
        val again = desktopSettings(folder) { MemoryPreferences(mapOf("current_theme" to "nord")) }
        assertEquals("sepia", again.getString("current_theme", ""), "the preferences are read once only")
    }

    @Test
    fun withoutFormerPreferencesTheFileStartsEmpty() {
        val settings = desktopSettings(folder) { null }
        assertNull(settings.getStringOrNull("current_theme"))
        assertFalse(File(folder, SETTINGS_FILE).exists())
    }

    @Test
    fun hotkeysSurviveARestartAndNothingIsLeftHalfWritten() = runBlocking {
        val repository = SettingsRepositoryImpl(desktopSettings(folder) { null })
        val custom = Hotkey("J", ctrl = true)
        repository.update { it.copy(hotkeys = it.hotkeys + (HotkeyAction.NEXT_WORD to custom) + (HotkeyAction.PREV_WORD to null)) }

        val restarted = SettingsRepositoryImpl(desktopSettings(folder) { MemoryPreferences(emptyMap()) })
        assertEquals(custom, restarted.current.hotkeys[HotkeyAction.NEXT_WORD])
        assertNull(restarted.current.hotkeys[HotkeyAction.PREV_WORD])
        assertEquals(listOf(SETTINGS_FILE), folder.list()!!.toList(), "no file is left half-written beside it")
    }

    /** Preferences held in memory, so the tests leave the computer's own alone. */
    private class MemoryPreferences(initial: Map<String, String>) : AbstractPreferences(null, "") {
        private val values = initial.toMutableMap()
        override fun putSpi(key: String, value: String) { values[key] = value }
        override fun getSpi(key: String): String? = values[key]
        override fun removeSpi(key: String) { values.remove(key) }
        override fun removeNodeSpi() {}
        override fun keysSpi(): Array<String> = values.keys.toTypedArray()
        override fun childrenNamesSpi(): Array<String> = emptyArray()
        override fun childSpi(name: String): AbstractPreferences = throw UnsupportedOperationException()
        override fun syncSpi() {}
        override fun flushSpi() {}
    }
}
