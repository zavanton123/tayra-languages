package com.tayra.languages.core.ui.theme

import androidx.compose.runtime.Composable

/** Draws the system's status and navigation bar icons dark on a light theme and light on a dark one, where the app draws behind them. */
@Composable
internal expect fun SystemBarIcons(darkTheme: Boolean)
