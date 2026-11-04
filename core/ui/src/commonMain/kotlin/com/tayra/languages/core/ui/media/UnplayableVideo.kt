package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A video this device can't play: the screen shows [problem], and the lines can still be read and gone through. */
@Composable
fun UnplayableVideo(source: VideoSource, problem: VideoProblem, onController: (VideoController) -> Unit, modifier: Modifier = Modifier) {
    SilentVideo(source, { clock -> (clock as? TickingClock)?.problem?.value = problem; onController(clock) }, modifier)
}
