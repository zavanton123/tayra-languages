package com.tayra.languages.core.domain.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CuePlaybackTest {

    private class FakeClock(override val latencyMs: Long = 30) : MediaClock {
        override val position = MutableStateFlow(0L)
        override val playing = MutableStateFlow(false)
        override val durationMs = MutableStateFlow(20_000L)
        override val rate = MutableStateFlow(1f)
        val seeks = mutableListOf<Long>()

        override fun play() { playing.value = true }
        override fun pause() { playing.value = false }
        override fun seekTo(ms: Long) { seeks += ms; position.value = ms }
        override fun setRate(rate: Float) { this.rate.value = rate }

        /** Plays on to [ms] in steps of [step], as a player reporting its place would. */
        fun runTo(ms: Long, step: Long = 16) {
            while (playing.value && position.value < ms) position.value = minOf(ms, position.value + step)
        }
    }

    private val cues = listOf(
        Cue(1000, 3000, "Olá."),
        Cue(3000, 5000, "Tudo bem?"),
        Cue(6000, 8000, "Sim."),
        Cue(8000, 9000, "Até logo."),
    )

    private fun TestScope.playback(clock: FakeClock = FakeClock()): Pair<FakeClock, CuePlayback> {
        val playback = CuePlayback(clock, TestScope(UnconfinedTestDispatcher(testScheduler)))
        playback.setCues(cues)
        return clock to playback
    }

    @Test
    fun theCurrentLineFollowsPlaybackAndStaysThroughTheGapAfterIt() = runTest {
        val (clock, playback) = playback()
        assertNull(playback.current.value)
        clock.play()
        clock.runTo(1500)
        assertEquals(0, playback.current.value)
        clock.runTo(5500)
        assertEquals(1, playback.current.value, "the line stays current in the silence after it")
        clock.runTo(6100)
        assertEquals(2, playback.current.value)
        assertTrue(clock.playing.value, "nothing pauses with auto-pause off")
    }

    @Test
    fun autoPauseStopsAtTheEndOfEachLineAndPlayGoesOn() = runTest {
        val (clock, playback) = playback()
        playback.setAutoPause(AutoPause.EVERY_LINE)
        clock.play()
        clock.runTo(10_000)
        assertFalse(clock.playing.value)
        assertEquals(0, playback.current.value)
        assertTrue(clock.position.value in 2970..3000, "paused at ${clock.position.value}")

        playback.play()
        clock.runTo(10_000)
        assertEquals(1, playback.current.value)
        assertTrue(clock.position.value in 4970..5000, "paused at ${clock.position.value}")

        playback.play()
        clock.runTo(10_000)
        assertEquals(2, playback.current.value, "the pause after a line before a gap comes at the line's end")
        assertTrue(clock.position.value in 7970..8000)
    }

    @Test
    fun aClockThatReportsSeldomStillPausesAfterTheLine() = runTest {
        val (clock, playback) = playback(FakeClock(latencyMs = 30))
        playback.setAutoPause(AutoPause.EVERY_LINE)
        clock.seekTo(2900)
        playback.setCues(cues)
        clock.play()
        clock.position.value = 3150
        assertFalse(clock.playing.value)
        assertEquals(0, playback.current.value, "the line just ended stays shown")
        playback.play()
        clock.runTo(3500)
        assertEquals(1, playback.current.value)
    }

    @Test
    fun onlyLinesWithUnknownWordsPauseInThatMode() = runTest {
        val (clock, playback) = playback()
        playback.hasUnknownWords = { it == 1 }
        playback.setAutoPause(AutoPause.UNKNOWN_LINES)
        clock.play()
        clock.runTo(10_000)
        assertEquals(1, playback.current.value)
        assertTrue(clock.position.value in 4970..5000)
    }

    @Test
    fun previousNextAndRepeatJumpJustBeforeTheLineAndPlay() = runTest {
        val (clock, playback) = playback()
        clock.play()
        clock.runTo(3500)
        assertEquals(1, playback.current.value)
        playback.setAutoPause(AutoPause.EVERY_LINE)

        playback.next()
        assertEquals(6000 - CuePlayback.LEAD_IN_MS, clock.seeks.last())
        assertEquals(2, playback.current.value)
        assertTrue(clock.playing.value)

        playback.previous()
        assertEquals(3000 - CuePlayback.LEAD_IN_MS, clock.seeks.last())
        assertEquals(1, playback.current.value, "the lead-in, in the line before, still counts as the line jumped to")
        clock.runTo(4000)
        assertTrue(clock.playing.value, "the end of the line before does not pause a jump into the next")
        clock.runTo(10_000)
        assertTrue(clock.position.value in 4970..5000)

        playback.repeat()
        assertEquals(3000 - CuePlayback.LEAD_IN_MS, clock.seeks.last())
        clock.runTo(10_000)
        assertTrue(clock.position.value in 4970..5000, "the repeated line pauses at its end again")
    }

    @Test
    fun nextBeforeTheFirstLineGoesToItAndNothingFollowsTheLast() = runTest {
        val (clock, playback) = playback()
        playback.next()
        assertEquals(0, playback.current.value)
        playback.jumpTo(3)
        val seeks = clock.seeks.size
        playback.next()
        assertEquals(seeks, clock.seeks.size)
        assertEquals(3, playback.current.value)
    }

    @Test
    fun aSeekOfTheUsersOwnDropsTheLineJumpedTo() = runTest {
        val (clock, playback) = playback()
        playback.jumpTo(2)
        clock.position.value = 1200
        assertEquals(0, playback.current.value)
        playback.seekBy(5000)
        assertEquals(6200, clock.position.value)
        assertEquals(2, playback.current.value)
    }

    @Test
    fun autoPauseModesCycle() = runTest {
        val (_, playback) = playback()
        assertEquals(AutoPause.OFF, playback.autoPause.value)
        playback.cycleAutoPause()
        assertEquals(AutoPause.EVERY_LINE, playback.autoPause.value)
        playback.cycleAutoPause()
        assertEquals(AutoPause.UNKNOWN_LINES, playback.autoPause.value)
        playback.cycleAutoPause()
        assertEquals(AutoPause.OFF, playback.autoPause.value)
    }
}
