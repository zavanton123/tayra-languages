package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.media.YouTubeIds

actual val videoSupported: Boolean = true

/**
 * Videos play in the Chromium of JetBrains Runtime, which the desktop app runs on. Run on another
 * Java, it has none: a video is studied from its lines here and watched in the browser.
 */
@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    if (Jcef.available) return JcefVideo(source, onController, modifier)
    val url = when (source) {
        is VideoSource.Link -> source.url
        is VideoSource.YouTube -> YouTubeIds.watchUrl(source.id)
    }
    UnplayableVideo(source, VideoProblem.Elsewhere(url), onController, modifier)
}
