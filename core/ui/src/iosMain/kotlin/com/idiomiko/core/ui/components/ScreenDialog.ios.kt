package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

actual fun screenDialogProperties(dismissOnBackPress: Boolean, dismissOnClickOutside: Boolean) = DialogProperties(
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    usePlatformDefaultWidth = false,
    usePlatformInsets = false,
)

@Composable
actual fun ScreenDialogBars() {}
