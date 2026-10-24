package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable

/**
 * Asks for the microphone where the app must ask before using it (Android), then tells [onResult]
 * whether it may be used; elsewhere the system asks on first use, so the answer is yes at once.
 */
@Composable
expect fun rememberMicrophoneAccess(): (onResult: (Boolean) -> Unit) -> Unit
