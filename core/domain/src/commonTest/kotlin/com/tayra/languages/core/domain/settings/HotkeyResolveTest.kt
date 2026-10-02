package com.tayra.languages.core.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** Keys shared by word and listening shortcuts act on a selected word, and on reading aloud otherwise. */
class HotkeyResolveTest {
    private val keys = HotkeyAction.defaults

    private fun resolve(key: Hotkey, wordSelected: Boolean, listening: Boolean = true) = HotkeyAction.resolve(keys, key, wordSelected, listening)

    @Test
    fun arrowsReadAloudUnlessAWordIsSelected() {
        assertEquals(HotkeyAction.LISTEN_PREVIOUS_ARROW, resolve(Hotkey("Left"), wordSelected = false))
        assertEquals(HotkeyAction.PREV_WORD, resolve(Hotkey("Left"), wordSelected = true))
        assertEquals(HotkeyAction.LISTEN_PAUSE_ARROW, resolve(Hotkey("Up"), wordSelected = false))
        assertEquals(HotkeyAction.STATUS_UP, resolve(Hotkey("Up"), wordSelected = true))
        assertEquals(HotkeyAction.LISTEN_PAUSE, resolve(Hotkey("W"), wordSelected = false))
        assertEquals(HotkeyAction.STATUS_WELL_KNOWN, resolve(Hotkey("W"), wordSelected = true))
    }

    @Test
    fun keysOnlyForListeningWorkEitherWayAndNotWithoutAudio() {
        assertEquals(HotkeyAction.LISTEN_PLAY_PAUSE, resolve(Hotkey("Space"), wordSelected = true))
        assertEquals(HotkeyAction.LISTEN_AUTO_PAUSE, resolve(Hotkey("Q"), wordSelected = false))
        assertEquals(HotkeyAction.NEXT_PAGE, resolve(Hotkey("Right", shift = true), wordSelected = false))
        assertEquals(null, resolve(Hotkey("Space"), wordSelected = false, listening = false))
        assertEquals(HotkeyAction.NEXT_WORD, resolve(Hotkey("Right"), wordSelected = false, listening = false))
    }
}
