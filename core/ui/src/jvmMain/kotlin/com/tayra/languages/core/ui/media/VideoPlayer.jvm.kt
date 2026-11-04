package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.media.YouTubeIds

actual val videoSupported: Boolean = true

/**
 * VLC plays files on the desktop when it is installed. YouTube's player needs a browser engine the
 * desktop app doesn't carry, so a YouTube video is studied from its transcript here and watched on YouTube.
 */
@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    when (source) {
        is VideoSource.File -> if (VlcEngine.available) VlcVideo(source.uri, onController, modifier) else UnplayableVideo(source, VideoProblem.NeedsEngine("VLC"), onController, modifier)
        is VideoSource.YouTube -> UnplayableVideo(source, VideoProblem.Elsewhere(YouTubeIds.watchUrl(source.id)), onController, modifier)
    }
}
