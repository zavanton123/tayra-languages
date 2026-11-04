package com.tayra.languages.core.ui.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import java.io.File

actual suspend fun PlatformFile.videoAddress(): String = absolutePath()

actual fun videoFileExists(address: String): Boolean = File(address).isFile

actual fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile> {
    val video = File(address)
    val stem = video.nameWithoutExtension
    return video.parentFile?.listFiles().orEmpty()
        .filter { it.isFile && it.extension.lowercase() in extensions && (it.nameWithoutExtension == stem || it.nameWithoutExtension.startsWith("$stem.")) }
        .sortedBy { it.name }
        .map { PlatformFile(it) }
}
