package com.idiomiko.core.ui.audio

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMicrophoneAccess(): (onResult: (Boolean) -> Unit) -> Unit = { it(true) }
