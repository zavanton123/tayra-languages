package com.tayra.languages.core.ui.files

import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

/** Whether files can be dragged into the app from outside on this platform; see [acceptsDroppedFiles]. */
expect val acceptsDroppedFiles: Boolean

/**
 * Takes files dragged onto the composable from outside the app, where the platform allows it:
 * [onFiles] gets them, and [onHover] follows a drag passing over it, for a highlight. Nothing
 * happens while [enabled] is false, or on a platform without drops.
 */
expect fun Modifier.acceptsDroppedFiles(enabled: Boolean = true, onHover: (Boolean) -> Unit = {}, onFiles: (List<PlatformFile>) -> Unit): Modifier
