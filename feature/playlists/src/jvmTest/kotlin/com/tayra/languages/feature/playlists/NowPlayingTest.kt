package com.tayra.languages.feature.playlists

import com.tayra.languages.core.domain.playlists.PlayerState
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.playlists.Playlist
import com.tayra.languages.core.domain.playlists.PlaylistItem
import com.tayra.languages.core.domain.playlists.PlaylistTextKind
import com.tayra.languages.core.domain.playlists.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The system's media controls are told the player's state: the text, the queue in playing order, and the time estimates. */
class NowPlayingTest {
    private fun item(id: Long, title: String, length: Int) = PlaylistItem(id, bookId = id * 10, title, PlaylistTextKind.LESSON, pageCount = 1, length, finished = false)

    private val playlist = Playlist(
        id = 1, languageId = 2, name = "Manhã", description = "", cover = 0, createdAt = Instant.fromEpochSeconds(0), lastPlayedAt = null,
        items = listOf(item(11, "Primeiro", 100), item(12, "Segundo", 300), item(13, "Terceiro", 50)),
        position = null,
    )

    /** Ten characters a second. */
    private fun nowPlaying(state: PlayerState) = nowPlayingOf(state) { (it / 10).seconds }

    private fun playing(status: PlayerStatus = PlayerStatus.PLAYING) = PlayerState(
        status = status, playlist = playlist, order = listOf(12, 11, 13), itemId = 11, heard = 40, textLength = 100, shuffle = true,
    )

    @Test
    fun nothingLoadedShowsNothing() {
        assertNull(nowPlaying(PlayerState()))
        assertNull(nowPlaying(PlayerState(status = PlayerStatus.IDLE, playlist = playlist)))
    }

    @Test
    fun theTextAndTheQueueAreShownInTheOrderTheyPlay() {
        val now = nowPlaying(playing())!!
        assertEquals("Primeiro", now.title)
        assertEquals("Manhã", now.playlistName)
        assertEquals(listOf(12L, 11L, 13L), now.queue.map { it.itemId })
        assertEquals(listOf(30.seconds, 10.seconds, 5.seconds), now.queue.map { it.duration })
        assertEquals(1, now.index, "the current text's place in the shuffled queue")
        assertEquals(4.seconds, now.elapsed)
        assertEquals(10.seconds, now.duration)
        assertTrue(now.hasNext)
        assertTrue(now.shuffle)
    }

    @Test
    fun theStatusSaysWhetherItSounds() {
        assertEquals(NowPlayingStatus.PLAYING, nowPlaying(playing())!!.status)
        assertTrue(nowPlaying(playing(PlayerStatus.LOADING))!!.playing, "getting a sentence ready counts as playing")
        assertEquals(NowPlayingStatus.PAUSED, nowPlaying(playing(PlayerStatus.PAUSED))!!.status)
        assertFalse(nowPlaying(playing(PlayerStatus.PAUSED))!!.playing)
        assertEquals(NowPlayingStatus.PAUSED, nowPlaying(playing(PlayerStatus.BLOCKED))!!.status, "a text that cannot be played holds the player")
        assertEquals(NowPlayingStatus.ENDED, nowPlaying(playing(PlayerStatus.COMPLETE))!!.status)
    }

    @Test
    fun theLastTextHasANextOnlyWhenThePlaylistRepeats() {
        val last = playing().copy(itemId = 13)
        assertFalse(nowPlaying(last)!!.hasNext)
        assertTrue(nowPlaying(last.copy(repeat = RepeatMode.ALL))!!.hasNext)
        assertEquals(RepeatMode.ALL, nowPlaying(last.copy(repeat = RepeatMode.ALL))!!.repeat)
    }

    @Test
    fun theWordBeingSpokenChangesNothingShown() {
        assertEquals(nowPlaying(playing()), nowPlaying(playing().copy(word = 3..7)))
    }
}
