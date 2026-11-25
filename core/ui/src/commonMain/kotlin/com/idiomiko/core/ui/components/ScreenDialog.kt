package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * A dialog window that fills the screen and draws under the system bars, so its page's colour reaches the screen's
 * edges; the content keeps clear of the bars itself.
 */
expect fun screenDialogProperties(dismissOnBackPress: Boolean = true, dismissOnClickOutside: Boolean = true): DialogProperties

/** In a dialog made by [screenDialogProperties]: the system bars' icons dark over a light page, light over a dark one. */
@Composable
expect fun ScreenDialogBars()
