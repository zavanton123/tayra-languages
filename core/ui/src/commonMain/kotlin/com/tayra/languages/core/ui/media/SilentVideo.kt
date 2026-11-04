package com.tayra.languages.core.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.ui.components.AppIcons

/**
 * A player with no picture or sound whose clock still runs: what plays where no real player is
 * given, as in tests, so the lines go by as they would.
 */
@Composable
fun SilentVideo(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val clock = remember(source) { TickingClock(scope) }
    DisposableEffect(clock) {
        onController(clock)
        onDispose { clock.release() }
    }
    Box(modifier.background(MaterialTheme.colorScheme.inverseSurface).testTag("silent-video"), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
    }
}
