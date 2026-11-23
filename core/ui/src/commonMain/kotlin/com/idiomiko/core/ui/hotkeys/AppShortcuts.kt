package com.idiomiko.core.ui.hotkeys

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Whether a press opens the settings, as in other desktop apps: ⌘, on macOS, Ctrl+, elsewhere, with no other
 * modifier. None of the default hotkeys (see HotkeyAction) is on the comma.
 */
fun opensSettings(key: Key, ctrl: Boolean, meta: Boolean, alt: Boolean, shift: Boolean, macOs: Boolean): Boolean =
    key == Key.Comma && !alt && !shift && if (macOs) meta && !ctrl else ctrl && !meta

fun KeyEvent.opensSettings(macOs: Boolean): Boolean =
    type == KeyEventType.KeyDown && opensSettings(key, isCtrlPressed, isMetaPressed, isAltPressed, isShiftPressed, macOs)
