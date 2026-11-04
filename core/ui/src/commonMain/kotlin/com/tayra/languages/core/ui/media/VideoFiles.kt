package com.tayra.languages.core.ui.media

import io.github.vinceglb.filekit.PlatformFile

/**
 * Where a picked video file is kept, so it can be played again after the app restarts: its path on
 * the desktop, its content address (with lasting permission to read it) on Android, a copy in the
 * app's own folder on iOS, where a picked file can't be reached later.
 */
expect suspend fun PlatformFile.videoAddress(): String

/** Whether the video file at [address] can still be read. */
expect fun videoFileExists(address: String): Boolean

/**
 * Subtitle files lying beside the video at [address] with its name ("film.srt", "film.pt.vtt"), on
 * platforms that can look in its folder.
 */
expect fun sidecarSubtitleFiles(address: String, extensions: Collection<String>): List<PlatformFile>
