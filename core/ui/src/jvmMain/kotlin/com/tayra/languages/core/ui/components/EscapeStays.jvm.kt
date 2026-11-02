package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/** The window sends an unused Escape as back; taking it here leaves it with nowhere to go. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun EscapeStaysOnPage() = BackHandler {}
