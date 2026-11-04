package com.tayra.languages.core.ui.media

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlin.random.Random

actual suspend fun PlatformFile.videoAddress(): String {
    val folder = FileKit.filesDir / "videos"
    folder.createDirectories()
    val copy = folder / "${Random.nextLong().toULong().toString(16)}-$name"
    copyTo(copy)
    return copy.path
}

actual fun videoFileExists(address: String): Boolean = runCatching { PlatformFile(address).exists() }.getOrDefault(false)

actual fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile> = emptyList()
