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
    fun aNewInstallGetsEveryDefault() {
        val keys = SettingsRepositoryImpl(MapSettings()).current.hotkeys
        assertEquals(Hotkey("Space"), keys[HotkeyAction.LISTEN_PLAY_PAUSE])
        assertEquals(Hotkey("Q"), keys[HotkeyAction.LISTEN_AUTO_PAUSE])
    }
}
