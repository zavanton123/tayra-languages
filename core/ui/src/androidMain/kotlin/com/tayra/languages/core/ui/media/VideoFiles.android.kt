package com.tayra.languages.core.ui.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path

actual suspend fun PlatformFile.videoAddress(): String {
    // Keeps the permission to read the picked document after the app restarts.
    runCatching { bookmarkData() }
    return path
}

actual fun videoFileExists(address: String): Boolean = runCatching { PlatformFile(address).exists() }.getOrDefault(false)

actual fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile> = emptyList()
