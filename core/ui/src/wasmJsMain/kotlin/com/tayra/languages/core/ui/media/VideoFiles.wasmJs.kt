package com.tayra.languages.core.ui.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

actual suspend fun PlatformFile.videoAddress(): String = name

actual fun videoFileExists(address: String): Boolean = false

actual fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile> = emptyList()
