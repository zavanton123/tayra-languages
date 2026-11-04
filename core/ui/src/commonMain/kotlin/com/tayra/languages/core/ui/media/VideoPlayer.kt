package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.media.MediaClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** What a video player plays. */
sealed interface VideoSource {
    /** A file on this device, by its path or (on Android) its content address. */
    data class File(val uri: String) : VideoSource

    data class YouTube(val id: String) : VideoSource
}

/** A player as the screen drives it: its clock, and letting go of it when the screen goes. */
interface VideoController : MediaClock {
    /** Why the video can't be played, when it can't; null while it can. */
    val problem: StateFlow<VideoProblem?>

    fun release() {}
}

/** Why a video does not play. */
sealed interface VideoProblem {
    /** The file is not where it was; it can be chosen again. */
    data object FileMissing : VideoProblem

    /** The player can't open this kind of file; on the desktop installing VLC helps. */
    data object Unsupported : VideoProblem

    /** The player needs a download before it can play anything. */
    data class NeedsEngine(val name: String) : VideoProblem

    data class Failed(val message: String) : VideoProblem
}

/** Whether this platform plays videos at all; the web version has no video feature. */
expect val videoSupported: Boolean

/**
 * The platform's player for [source], filling [modifier]. [onController] is given the player once it
 * exists, to read its clock and drive it; the player is released when this leaves the screen.
 */
@Composable
expect fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier = Modifier)

/**
 * A clock that only counts time, for a player that is not there (tests, previews) or one whose own
 * place is read seldom: it runs on from the last place set at the speed it is given.
 */
class TickingClock(
    private val scope: CoroutineScope,
    durationMs: Long = 0,
    private val stepMs: Long = 16,
) : VideoController {
    override val position = MutableStateFlow(0L)
    override val playing = MutableStateFlow(false)
    override val durationMs = MutableStateFlow(durationMs)
    override val rate = MutableStateFlow(1f)
    override val problem = MutableStateFlow<VideoProblem?>(null)

    private var ticker: Job? = null

    override fun play() {
        if (playing.value) return
        playing.value = true
        ticker = scope.launch {
            var mark = TimeSource.Monotonic.markNow()
            while (isActive && playing.value) {
                delay(stepMs)
                val elapsed = mark.elapsedNow().inWholeMilliseconds
                mark = TimeSource.Monotonic.markNow()
                val next = position.value + (elapsed * rate.value).toLong()
                val end = durationMs.value
                if (end in 1..next) {
                    position.value = end
                    playing.value = false
                } else {
                    position.value = next
                }
            }
        }
    }

    override fun pause() {
        playing.value = false
        ticker?.cancel()
        ticker = null
    }

    override fun seekTo(ms: Long) {
        position.value = ms.coerceAtLeast(0)
    }

    override fun setRate(rate: Float) {
        this.rate.value = rate
    }

    override fun release() = pause()
}
