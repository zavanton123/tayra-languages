package com.tayra.languages.core.ui.hotkeys

import com.tayra.languages.core.domain.settings.Hotkey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShiftTrackerTest {
    @Test
    fun aKeyArrivingWithoutShiftWhileShiftIsHeldCountsAsShifted() {
        val tracker = ShiftTracker()
        assertTrue(tracker.onKey(isShift = true, down = true))
        // As macOS sometimes reports ⌘⇧−: the Shift flag missing on the − press.
        assertEquals(Hotkey("Minus", shift = true, ctrl = true), tracker.adjust(Hotkey("Minus", ctrl = true)))

        tracker.onKey(isShift = true, down = false)
        assertEquals(Hotkey("Minus", ctrl = true), tracker.adjust(Hotkey("Minus", ctrl = true)))
    }

    @Test
    fun focusLossForgetsShift() {
        val tracker = ShiftTracker()
        tracker.onKey(isShift = true, down = true)
        tracker.reset()
        assertEquals(Hotkey("Minus", ctrl = true), tracker.adjust(Hotkey("Minus", ctrl = true)))
    }
}
