package com.tayra.languages.core.ui.hotkeys

import com.tayra.languages.core.domain.settings.Hotkey

/**
 * Remembers whether Shift is held from its own key presses. macOS now and then delivers a key
 * pressed with ⌘ and Shift (⌘⇧− in particular) without the Shift flag; a key that arrives while
 * Shift is known to be down counts as shifted.
 */
class ShiftTracker {
    var held: Boolean = false
        private set

    /** Notes a Shift key going down or up; returns true when [isShift] was one, so it is not a shortcut. */
    fun onKey(isShift: Boolean, down: Boolean): Boolean {
        if (isShift) held = down
        return isShift
    }

    /** Forgets Shift, as when focus leaves and its release may never be seen. */
    fun reset() {
        held = false
    }

    fun adjust(hotkey: Hotkey): Hotkey = if (held && !hotkey.shift) hotkey.copy(shift = true) else hotkey
}
