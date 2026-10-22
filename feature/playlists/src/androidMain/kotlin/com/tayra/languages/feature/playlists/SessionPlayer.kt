package com.tayra.languages.feature.playlists

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The playlist player as the media session sees it: a Media3 player whose state is what
 * [nowPlaying] says, each text of the queue a media item, and whose commands (play, pause, next,
 * previous, seeking) go to [player]. It runs on the main thread, as the player does.
 */
@OptIn(UnstableApi::class)
internal class SessionPlayer(
    private val player: PlaylistPlayer,
    private val nowPlaying: StateFlow<NowPlaying?>,
    scope: CoroutineScope,
) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val watch = scope.launch { nowPlaying.collect { invalidateState() } }

    override fun getState(): State {
        val now = nowPlaying.value?.takeIf { it.queue.isNotEmpty() } ?: return State.Builder().setAvailableCommands(COMMANDS).build()
        val items = now.queue.map { text ->
            val metadata = MediaMetadata.Builder()
                .setTitle(text.title)
                .setArtist(now.playlistName)
                .setAlbumTitle(now.playlistName)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                .build()
            MediaItemData.Builder(text.itemId)
                .setMediaItem(MediaItem.Builder().setMediaId(text.itemId.toString()).setMediaMetadata(metadata).build())
                .setDurationUs(text.duration.inWholeMicroseconds)
                .setIsSeekable(true)
                .build()
        }
        return State.Builder()
            .setAvailableCommands(COMMANDS)
            .setAudioAttributes(SPEECH)
            .setPlaylist(items)
            .setCurrentMediaItemIndex(now.index)
            .setContentPositionMs(now.elapsed.inWholeMilliseconds)
            .setPlayWhenReady(now.playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(
                when (now.status) {
                    NowPlayingStatus.LOADING -> Player.STATE_BUFFERING
                    NowPlayingStatus.PLAYING, NowPlayingStatus.PAUSED -> Player.STATE_READY
                    NowPlayingStatus.ENDED -> Player.STATE_ENDED
                },
            )
            // The queue is already in the order it plays, shuffled or not; repeat tells whether next goes round.
            .setRepeatMode(
                when (now.repeat) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                },
            )
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) player.resume() else player.pause()
        return Futures.immediateVoidFuture()
    }

    // A headset's stop button holds the playlist rather than unloading it.
    override fun handleStop(): ListenableFuture<*> {
        player.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val now = nowPlaying.value ?: return Futures.immediateVoidFuture()
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> player.next()
            // The player decides between the text's start and the text before it.
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> player.previous()
            else -> {
                val target = now.queue.getOrNull(mediaItemIndex)
                when {
                    target != null && mediaItemIndex != now.index -> player.jumpTo(target.itemId)
                    positionMs != C.TIME_UNSET && now.duration.isPositive() -> player.seek(positionMs.toFloat() / now.duration.inWholeMilliseconds)
                }
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        watch.cancel()
        return Futures.immediateVoidFuture()
    }

    private companion object {
        val COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_RELEASE,
            )
            .build()

        val SPEECH: AudioAttributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()
    }
}
