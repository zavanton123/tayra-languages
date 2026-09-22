package com.tayra.languages.core.ui.hotkeys

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.tayra.languages.core.domain.settings.Hotkey

/** Maps Compose key events to the platform-neutral [Hotkey] representation. */
object HotkeyMatcher {
    private val namedKeys: Map<Key, String> = buildMap {
        ('A'..'Z').forEach { c -> put(keyForLetter(c), c.toString()) }
        put(Key.Zero, "0"); put(Key.One, "1"); put(Key.Two, "2"); put(Key.Three, "3"); put(Key.Four, "4")
        put(Key.Five, "5"); put(Key.Six, "6"); put(Key.Seven, "7"); put(Key.Eight, "8"); put(Key.Nine, "9")
        put(Key.NumPad0, "0"); put(Key.NumPad1, "1"); put(Key.NumPad2, "2"); put(Key.NumPad3, "3"); put(Key.NumPad4, "4")
        put(Key.NumPad5, "5"); put(Key.NumPad6, "6"); put(Key.NumPad7, "7"); put(Key.NumPad8, "8"); put(Key.NumPad9, "9")
        put(Key.DirectionUp, "Up"); put(Key.DirectionDown, "Down"); put(Key.DirectionLeft, "Left"); put(Key.DirectionRight, "Right")
        put(Key.Enter, "Enter"); put(Key.NumPadEnter, "Enter"); put(Key.Escape, "Escape"); put(Key.Spacebar, "Space"); put(Key.Tab, "Tab")
        put(Key.Backspace, "Backspace"); put(Key.Delete, "Delete"); put(Key.MoveHome, "Home"); put(Key.MoveEnd, "End")
        put(Key.PageUp, "PageUp"); put(Key.PageDown, "PageDown")
        put(Key.F1, "F1"); put(Key.F2, "F2"); put(Key.F3, "F3"); put(Key.F4, "F4"); put(Key.F5, "F5"); put(Key.F6, "F6")
        put(Key.F7, "F7"); put(Key.F8, "F8"); put(Key.F9, "F9"); put(Key.F10, "F10"); put(Key.F11, "F11"); put(Key.F12, "F12")
        put(Key.Comma, ","); put(Key.Period, "."); put(Key.Minus, "-"); put(Key.Equals, "="); put(Key.Slash, "/")
        put(Key.Semicolon, ";"); put(Key.Apostrophe, "'"); put(Key.LeftBracket, "["); put(Key.RightBracket, "]"); put(Key.Backslash, "\\")
        put(Key.Grave, "`")
    }

    private fun keyForLetter(c: Char): Key = when (c) {
        'A' -> Key.A; 'B' -> Key.B; 'C' -> Key.C; 'D' -> Key.D; 'E' -> Key.E; 'F' -> Key.F; 'G' -> Key.G
        'H' -> Key.H; 'I' -> Key.I; 'J' -> Key.J; 'K' -> Key.K; 'L' -> Key.L; 'M' -> Key.M; 'N' -> Key.N
        'O' -> Key.O; 'P' -> Key.P; 'Q' -> Key.Q; 'R' -> Key.R; 'S' -> Key.S; 'T' -> Key.T; 'U' -> Key.U
        'V' -> Key.V; 'W' -> Key.W; 'X' -> Key.X; 'Y' -> Key.Y; else -> Key.Z
    }

    /** The hotkey for a key event, or null for modifier-only presses and unknown keys. */
    fun fromEvent(event: KeyEvent): Hotkey? {
        val name = namedKeys[event.key] ?: return null
        return Hotkey(
            key = name,
            shift = event.isShiftPressed,
            ctrl = event.isCtrlPressed || event.isMetaPressed,
            alt = event.isAltPressed,
        )
    }
}
