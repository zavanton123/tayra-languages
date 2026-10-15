package com.tayra.languages.core.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

@Composable
actual fun rememberCameraCapture(onPhoto: (fileName: String, bytes: ByteArray) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onPhoto)
    val camera = rememberCameraPickerLauncher { file ->
        if (file != null) scope.launch { deliver(file.name, file.readBytes()) }
    }
    return remember(camera) { { camera.launch() } }
}
