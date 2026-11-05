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
    if (JcefRuntime.available) return JcefVideo(source, onController, modifier)
    val url = when (source) {
        is VideoSource.Link -> source.url
        is VideoSource.YouTube -> YouTubeIds.watchUrl(source.id)
    }
    UnplayableVideo(source, VideoProblem.Elsewhere(url), onController, modifier)
}

/**
 * Whether this Java runtime carries Chromium (JetBrains Runtime's jcef module). Kept apart from
 * [Jcef], whose Chromium types do not exist on other runtimes and would fail to load there.
 */
internal object JcefRuntime {
    val available: Boolean by lazy { runCatching { Class.forName("com.jetbrains.cef.JCefAppConfig") }.isSuccess }
}
