package com.tayra.languages.feature.playlists

import androidx.compose.runtime.Composable
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import kotlin.time.DurationUnit

private fun hasMediaSession(): Boolean = js("typeof navigator !== 'undefined' && 'mediaSession' in navigator && typeof MediaMetadata !== 'undefined'")
private fun setMediaAction(action: String, handler: () -> Unit): Unit =
    js("{ try { navigator.mediaSession.setActionHandler(action, function () { handler(); }); } catch (e) {} }")
private fun setMediaMetadata(title: String, playlist: String): Unit =
    js("{ navigator.mediaSession.metadata = new MediaMetadata({ title: title, artist: playlist, album: playlist }); }")
private fun setMediaPlaybackState(state: String): Unit = js("{ navigator.mediaSession.playbackState = state; }")
private fun setMediaPosition(duration: Double, position: Double): Unit =
    js("{ try { navigator.mediaSession.setPositionState({ duration: duration, position: Math.min(position, duration), playbackRate: 1 }); } catch (e) {} }")
private fun clearMediaSession(): Unit =
    js("{ navigator.mediaSession.metadata = null; navigator.mediaSession.playbackState = 'none'; try { navigator.mediaSession.setPositionState(); } catch (e) {} }")

/** The browser's media keys and media controls, through its Media Session API; nothing where it has none. */
private class WebMediaControls(player: PlaylistPlayer, feed: NowPlayingFeed) : SystemMediaControls {
    init {
        if (hasMediaSession()) {
            setMediaAction("play") { player.resume() }
            setMediaAction("pause") { player.pause() }
            setMediaAction("previoustrack") { player.previous() }
            setMediaAction("nexttrack") { player.next() }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            scope.launch {
                feed.nowPlaying.map { now -> now?.let { it.title to it.playlistName } }.distinctUntilChanged().collect { names ->
                    if (names == null) clearMediaSession() else setMediaMetadata(names.first, names.second)
                }
            }
            scope.launch {
                feed.nowPlaying.collect { now ->
                    if (now == null) return@collect
                    setMediaPlaybackState(if (now.playing) "playing" else "paused")
                    if (now.duration.isPositive()) setMediaPosition(now.duration.toDouble(DurationUnit.SECONDS), now.elapsed.toDouble(DurationUnit.SECONDS))
                }
            }
        }
    }
}

internal actual fun Scope.systemMediaControls(): SystemMediaControls = WebMediaControls(get(), get())

@Composable
actual fun PlaybackNotificationsPermission() {}
