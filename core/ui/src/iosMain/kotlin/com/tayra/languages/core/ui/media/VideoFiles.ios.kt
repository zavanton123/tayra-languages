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

/** The folder, in the app's own files, where picked videos are copied. */
private const val VIDEOS = "videos"

/**
 * A copy in the app's own files, kept by its place there ("videos/…"): iOS may move the app's
 * folder when the app is updated, so a whole path would stop leading to it.
 */
actual suspend fun PlatformFile.videoAddress(): String {
    val folder = FileKit.filesDir / VIDEOS
    folder.createDirectories()
    val stored = "${Random.nextLong().toULong().toString(16)}-$name"
    copyTo(folder / stored)
    return "$VIDEOS/$stored"
}

/** The file a stored video address names now. */
internal fun videoPath(address: String): String = if (address.startsWith("/")) address else (FileKit.filesDir / address).path

actual fun videoFileExists(address: String): Boolean = runCatching { PlatformFile(videoPath(address)).exists() }.getOrDefault(false)

actual fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile> = emptyList()
