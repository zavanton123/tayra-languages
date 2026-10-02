package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Saved settings hold every shortcut, so the page shortcuts added later are filled in once. */
class HotkeyDefaultsMigrationTest {

    @Test
    fun unassignedPageShortcutsGetTheirNewDefaultsOnce() = runTest {
        val store = MapSettings()
        // As saved by an older build: every shortcut stored, the page ones unassigned.
        HotkeyAction.entries.forEach { store.putString(it.settingKey, "") }

        val first = SettingsRepositoryImpl(store)
        assertEquals(Hotkey("Right", shift = true), first.current.hotkeys[HotkeyAction.NEXT_PAGE])
        assertEquals(Hotkey("Left", shift = true), first.current.hotkeys[HotkeyAction.PREVIOUS_PAGE])
        assertNull(first.current.hotkeys[HotkeyAction.LISTEN_PLAY_PAUSE], "a shortcut that was saved unassigned stays so")

        // Clearing one afterwards sticks.
        first.update { it.copy(hotkeys = it.hotkeys + (HotkeyAction.NEXT_PAGE to null)) }
        assertNull(SettingsRepositoryImpl(store).current.hotkeys[HotkeyAction.NEXT_PAGE])
    }

    @Test
    fun wordShortcutsStillOnTheOldArrowsMoveToCtrlOnce() = runTest {
        val store = MapSettings()
        HotkeyAction.entries.forEach { store.putString(it.settingKey, it.default?.serialized ?: "") }
        store.putString(HotkeyAction.PREV_WORD.settingKey, "Left")
        store.putString(HotkeyAction.NEXT_WORD.settingKey, "Right")
        store.putString(HotkeyAction.STATUS_UP.settingKey, "Up")
        // Changed by hand: kept.
        store.putString(HotkeyAction.STATUS_DOWN.settingKey, "J")

        val settings = SettingsRepositoryImpl(store)
        assertEquals(Hotkey("Left", ctrl = true), settings.current.hotkeys[HotkeyAction.PREV_WORD])
        assertEquals(Hotkey("Right", ctrl = true), settings.current.hotkeys[HotkeyAction.NEXT_WORD])
        assertEquals(Hotkey("Up", ctrl = true), settings.current.hotkeys[HotkeyAction.STATUS_UP])
        assertEquals(Hotkey("J"), settings.current.hotkeys[HotkeyAction.STATUS_DOWN])

        // Once saved, a plain arrow chosen again by hand stays.
        settings.update { it.copy(hotkeys = it.hotkeys + (HotkeyAction.PREV_WORD to Hotkey("Left"))) }
        assertEquals(Hotkey("Left"), SettingsRepositoryImpl(store).current.hotkeys[HotkeyAction.PREV_WORD])
    }

    /** A word shortcut that ended up on a key another shortcut uses (Escape, say) moves to its Ctrl arrow too. */
    @Test
    fun aWordShortcutClashingWithAnotherMovesToCtrl() {
        val store = MapSettings()
        HotkeyAction.entries.forEach { store.putString(it.settingKey, it.default?.serialized ?: "") }
        store.putString(HotkeyAction.STATUS_DOWN.settingKey, "Escape")

        val keys = SettingsRepositoryImpl(store).current.hotkeys
        assertEquals(Hotkey("Down", ctrl = true), keys[HotkeyAction.STATUS_DOWN])
        assertEquals(Hotkey("Escape"), keys[HotkeyAction.START_HOVER], "the other shortcut keeps its key")
    }

    /** Known moved from W to K: a saved W moves to K once, and W is left to pause and resume. */
    @Test
    fun theKnownShortcutMovesFromWToK() {
        val store = MapSettings()
        HotkeyAction.entries.forEach { store.putString(it.settingKey, it.default?.serialized ?: "") }
        store.putString(HotkeyAction.STATUS_WELL_KNOWN.settingKey, "W")

        val keys = SettingsRepositoryImpl(store).current.hotkeys
        assertEquals(Hotkey("K"), keys[HotkeyAction.STATUS_WELL_KNOWN])
        assertEquals(Hotkey("W"), keys[HotkeyAction.LISTEN_PAUSE])
    }

    @Test
    fun aNewInstallGetsEveryDefault() {
        val keys = SettingsRepositoryImpl(MapSettings()).current.hotkeys
        assertEquals(Hotkey("Space"), keys[HotkeyAction.LISTEN_PLAY_PAUSE])
        assertEquals(Hotkey("Q"), keys[HotkeyAction.LISTEN_AUTO_PAUSE])
    }
}
