package com.tayra.languages.feature.playlists

import com.tayra.languages.core.domain.playlists.ListeningTime
import com.tayra.languages.core.domain.playlists.PlayerState
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.RepeatMode
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration

/** How far the player has got, as the system's media controls tell it. */
enum class NowPlayingStatus {
    /** A sentence is being got ready to sound. */
    LOADING,
    PLAYING,

    /** Held, or stopped at a text that cannot be played. */
    PAUSED,

    /** The playlist was heard to the end. */
    ENDED,
}

/** A text of the queue, as the system's media controls list it. */
data class QueuedText(val itemId: Long, val title: String, val duration: Duration)

/**
 * What the system's media controls show of the playlist player (the notification and lock screen,
 * the Now Playing centre, the browser's media keys): the text and the playlist, the queue, and how
 * far into the text listening has got, estimated from the text's length.
 */
data class NowPlaying(
    val playlistName: String,
    val title: String,
    val status: NowPlayingStatus,
    /** The texts in the order they are played; the current one is at [index]. */
    val queue: List<QueuedText>,
    val index: Int,
    val elapsed: Duration,
    val duration: Duration,
    val hasNext: Boolean,
    val shuffle: Boolean,
    val repeat: RepeatMode,
) {
    /** Whether the player means to sound: playing, or getting the sentence ready. */
    val playing: Boolean get() = status == NowPlayingStatus.PLAYING || status == NowPlayingStatus.LOADING
}

/** What the controls show of [state], with [duration] the time its characters take; null when nothing is loaded. */
fun nowPlayingOf(state: PlayerState, duration: (characters: Int) -> Duration): NowPlaying? {
    val playlist = state.playlist ?: return null
    if (!state.isLoaded) return null
    val items = playlist.items.associateBy { it.id }
    val queue = state.order.mapNotNull { id -> items[id]?.let { QueuedText(it.id, it.title, duration(it.length)) } }
    return NowPlaying(
        playlistName = playlist.name,
        title = state.item?.title.orEmpty(),
        status = when (state.status) {
            PlayerStatus.LOADING -> NowPlayingStatus.LOADING
            PlayerStatus.PLAYING -> NowPlayingStatus.PLAYING
            PlayerStatus.COMPLETE -> NowPlayingStatus.ENDED
            PlayerStatus.IDLE, PlayerStatus.PAUSED, PlayerStatus.BLOCKED -> NowPlayingStatus.PAUSED
        },
        queue = queue,
        index = queue.indexOfFirst { it.itemId == state.itemId }.coerceAtLeast(0),
        elapsed = duration(state.heard),
        duration = duration(state.textLength),
        hasNext = state.hasNext,
        shuffle = state.shuffle,
        repeat = state.repeat,
    )
}

/** What [player] has loaded, for the system's media controls, made again as the time estimates change. */
class NowPlayingFeed(player: PlaylistPlayer, service: PlaylistService, time: ListeningTime, settings: SettingsRepository, scope: CoroutineScope) {
    val nowPlaying: StateFlow<NowPlaying?> =
        combine(player.state, time.changes, settings.settings.map { it.playlistSpeed }.distinctUntilChanged()) { state, _, _ ->
            val code = state.languageCode
            nowPlayingOf(state) { service.duration(it, code) }
        }.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, null)
}
