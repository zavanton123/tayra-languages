package com.tayra.languages.core.domain.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A player's sense of time: where it is, whether it plays, and the few things it is told to do. */
interface MediaClock {
    /** Where playback is, in milliseconds. */
    val position: StateFlow<Long>
    val playing: StateFlow<Boolean>
    val durationMs: StateFlow<Long>
    val rate: StateFlow<Float>

    /** How late [position] can be behind the sound; a pause at a line's end comes this much early. */
    val latencyMs: Long get() = 30

    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun setRate(rate: Float)
}

/** What of a video's lines stays hidden until revealed, for listening first. */
enum class VideoBlur {
    OFF,

    /** The translations: the line is read, its meaning worked out first. */
    TRANSLATION,

    /** The lines themselves, with their translations, until they have been heard. */
    LINE,
}

enum class AutoPause {
    OFF,

    /** After every line. */
    EVERY_LINE,

    /** After lines with words not known yet. */
    UNKNOWN_LINES,
}

/**
 * Plays a video line by line, as reading aloud does a page: knows the line being spoken, pauses at
 * the end of a line when [autoPause] says so, and goes to the line before, the next or the same one
 * again. A line is jumped to a little before it starts, so its first sound is not cut off.
 */
class CuePlayback(private val clock: MediaClock, scope: CoroutineScope) {

    private var cues: List<Cue> = emptyList()

    private val _current = MutableStateFlow<Int?>(null)

    /** The line being spoken, or the last one spoken in a pause between lines; null before the first. */
    val current: StateFlow<Int?> = _current.asStateFlow()

    private val _autoPause = MutableStateFlow(AutoPause.OFF)
    val autoPause: StateFlow<AutoPause> = _autoPause.asStateFlow()

    /** Whether the player plays, as its clock says. */
    val playing: StateFlow<Boolean> get() = clock.playing

    val rate: StateFlow<Float> get() = clock.rate

    fun setRate(rate: Float) = clock.setRate(rate)

    /** For [AutoPause.UNKNOWN_LINES]: whether the line at an index has words not known yet. */
    var hasUnknownWords: (Int) -> Boolean = { true }

    /** The line jumped to, held as current until playback reaches it, since the jump lands just before it. */
    private var target: Int? = null

    /** The line the last automatic pause came after, so playing on does not stop at it again. */
    private var pausedAfter: Int? = null

    init {
        scope.launch { clock.position.collect { onPosition(it) } }
    }

    fun setCues(cues: List<Cue>) {
        this.cues = cues
        target = null
        pausedAfter = null
        _current.value = indexAt(clock.position.value)
    }

    fun setAutoPause(mode: AutoPause) {
        _autoPause.value = mode
        pausedAfter = null
    }

    fun cycleAutoPause() = setAutoPause(AutoPause.entries[(autoPause.value.ordinal + 1) % AutoPause.entries.size])

    fun toggle() = if (clock.playing.value) pause() else play()

    fun play() = clock.play()

    fun pause() = clock.pause()

    fun previous() {
        val at = _current.value
        jumpTo(if (at == null) 0 else (at - 1).coerceAtLeast(0))
    }

    fun next() {
        val at = _current.value
        val next = if (at == null) cues.indexOfFirst { it.beginMs >= clock.position.value }.takeIf { it >= 0 } ?: 0 else at + 1
        if (next <= cues.lastIndex) jumpTo(next)
    }

    /** The current line again, from its start. */
    fun repeat() = jumpTo(_current.value ?: 0)

    /** Plays the line at [index] from just before its start. */
    fun jumpTo(index: Int) {
        val cue = cues.getOrNull(index) ?: return
        target = index
        pausedAfter = null
        _current.value = index
        clock.seekTo((cue.beginMs - LEAD_IN_MS).coerceAtLeast(0))
        clock.play()
    }

    /** Moves playback by [deltaMs], as the ±5 seconds keys do. */
    fun seekBy(deltaMs: Long) {
        target = null
        pausedAfter = null
        val to = (clock.position.value + deltaMs).coerceIn(0, clock.durationMs.value.takeIf { it > 0 } ?: Long.MAX_VALUE)
        clock.seekTo(to)
        _current.value = indexAt(to)
    }

    internal fun onPosition(ms: Long) {
        val held = target
        val found = indexAt(ms)
        val index = if (held != null && (found == null || found < held)) {
            // Still in the lead-in before the line jumped to; anything far off is a seek of the user's own.
            if (ms < cues[held].beginMs - LEAD_IN_MS - SEEK_SLACK_MS || ms > cues[held].endMs) {
                target = null
                found
            } else {
                held
            }
        } else {
            if (held != null) target = null
            found
        }
        val before = _current.value
        if (index != before) {
            // A clock that reports seldom can step from one line straight into the next; the pause owed
            // to the line before still comes, a moment late.
            if (before != null && index == before + 1 && held == null && clock.playing.value && pausedAfter != before &&
                ms < cues[before].endMs + LATE_PAUSE_MS && shouldPauseAfter(before)
            ) {
                pausedAfter = before
                clock.pause()
                return
            }
            if (index != pausedAfter) pausedAfter = null
            _current.value = index
        }
        if (index == null || !clock.playing.value || pausedAfter == index) return
        val cue = cues[index]
        if (ms >= cue.endMs - clock.latencyMs && ms < cue.endMs + LATE_PAUSE_MS && shouldPauseAfter(index)) {
            pausedAfter = index
            clock.pause()
        }
    }

    private fun shouldPauseAfter(index: Int): Boolean = when (autoPause.value) {
        AutoPause.OFF -> false
        AutoPause.EVERY_LINE -> true
        AutoPause.UNKNOWN_LINES -> hasUnknownWords(index)
    }

    /** The last line begun by [ms], or null before the first. */
    private fun indexAt(ms: Long): Int? {
        var low = 0
        var high = cues.lastIndex
        var found = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (cues[mid].beginMs <= ms) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found.takeIf { it >= 0 }
    }

    companion object {
        /** How long before a line a jump to it lands. */
        const val LEAD_IN_MS = 125L

        /** How far from a line jumped to playback may be found (a slow seek) before it counts as a seek elsewhere. */
        private const val SEEK_SLACK_MS = 400L

        /** A line's end seen this late (a slow clock, a seek past it) no longer pauses. */
        private const val LATE_PAUSE_MS = 600L
    }
}
