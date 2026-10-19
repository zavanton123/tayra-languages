package com.tayra.languages.core.ui.theme

import androidx.compose.runtime.Composable

/** The desktops give Java no way to read their Reduce Motion setting; the app's own setting covers them. */
@Composable
internal actual fun systemReducesMotion(): Boolean = false
