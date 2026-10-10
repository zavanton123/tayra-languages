package com.tayra.languages.core.ui.files

import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

actual val acceptsDroppedFiles: Boolean = false

actual fun Modifier.acceptsDroppedFiles(enabled: Boolean, onHover: (Boolean) -> Unit, onFiles: (List<PlatformFile>) -> Unit): Modifier = this
