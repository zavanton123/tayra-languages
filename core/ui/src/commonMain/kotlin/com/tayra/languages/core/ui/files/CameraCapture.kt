package com.tayra.languages.core.ui.files

import androidx.compose.runtime.Composable

/**
 * A way to take a photo with the device's camera, which [onPhoto] receives as a JPEG; null where
 * there is no camera to open (the desktop, the browser).
 */
@Composable
expect fun rememberCameraCapture(onPhoto: (fileName: String, bytes: ByteArray) -> Unit): (() -> Unit)?
