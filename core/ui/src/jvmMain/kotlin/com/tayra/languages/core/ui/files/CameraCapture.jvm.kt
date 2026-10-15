package com.tayra.languages.core.ui.files

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraCapture(onPhoto: (fileName: String, bytes: ByteArray) -> Unit): (() -> Unit)? = null
