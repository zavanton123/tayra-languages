package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable

/**
 * Keeps a desktop window's Escape on the page while this is shown: left alone, an Escape nothing
 * else used would take the window back to the page before. A phone's back button and gesture
 * still go back.
 */
@Composable
expect fun EscapeStaysOnPage()
