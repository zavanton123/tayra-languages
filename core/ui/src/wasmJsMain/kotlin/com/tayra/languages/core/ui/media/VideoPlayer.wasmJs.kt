package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val videoSupported: Boolean = false

@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) =
    SilentVideo(source, onController, modifier)
