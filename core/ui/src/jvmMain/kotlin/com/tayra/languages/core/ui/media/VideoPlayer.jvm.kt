package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val videoSupported: Boolean = true

@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    when {
        source is VideoSource.File && VlcEngine.available -> VlcVideo(source.uri, onController, modifier)
        else -> SilentVideo(source, onController, modifier)
    }
}
