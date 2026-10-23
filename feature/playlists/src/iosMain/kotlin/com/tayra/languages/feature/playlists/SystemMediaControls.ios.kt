package com.tayra.languages.feature.playlists

import androidx.compose.runtime.Composable
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeSpokenAudio
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSNotification
import platform.Foundation.dataWithBytes
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueCount
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueIndex
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.time.DurationUnit

/**
 * The Now Playing controls of the lock screen and Control Center, the headphones' buttons, and the
 * audio session that lets the player go on with the screen locked or another app open: the session
 * plays in the playback category from when a playlist starts. A call pauses the player, which goes
 * on after it when the system says it may.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosMediaControls(private val player: PlaylistPlayer, private val feed: NowPlayingFeed) : SystemMediaControls {
    private val session = AVAudioSession.sharedInstance()
    private val commands = MPRemoteCommandCenter.sharedCommandCenter()
    private var active = false
    private var pausedForInterruption = false
    private var backgroundTask: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
    private var artwork: MPMediaItemArtwork? = null

    init {
        commands.playCommand.handle { player.resume() }
        commands.pauseCommand.handle { player.pause() }
        commands.togglePlayPauseCommand.handle { player.toggle() }
        commands.nextTrackCommand.handle { player.next() }
        commands.previousTrackCommand.handle { player.previous() }
        commands.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val seconds = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime
            val duration = feed.nowPlaying.value?.duration?.toDouble(DurationUnit.SECONDS)
            if (seconds == null || duration == null || duration <= 0.0) return@addTargetWithHandler MPRemoteCommandHandlerStatusCommandFailed
            player.seek((seconds / duration).toFloat())
            MPRemoteCommandHandlerStatusSuccess
        }
        NSNotificationCenter.defaultCenter.addObserverForName(AVAudioSessionInterruptionNotification, `object` = session, queue = NSOperationQueue.mainQueue) { note -> interrupted(note) }
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            artwork = nowPlayingArtwork()?.let(::coverOf)
            feed.nowPlaying.collect(::show)
        }
    }

    private fun MPRemoteCommand.handle(action: () -> Unit) {
        addTargetWithHandler { _: MPRemoteCommandEvent? ->
            action()
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun show(now: NowPlaying?) {
        val center = MPNowPlayingInfoCenter.defaultCenter()
        if (now == null) {
            center.nowPlayingInfo = null
            endBackgroundTask()
            if (active) session.setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
            active = false
            return
        }
        if (now.playing && !active) {
            session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeSpokenAudio, options = 0u, error = null)
            active = session.setActive(true, error = null)
        }
        // Nothing sounds while a sentence is got ready, and in the background the system would suspend the app then.
        if (now.status == NowPlayingStatus.LOADING) beginBackgroundTask() else endBackgroundTask()
        center.nowPlayingInfo = listOfNotNull(
            MPMediaItemPropertyTitle to now.title,
            MPMediaItemPropertyArtist to now.playlistName,
            MPMediaItemPropertyAlbumTitle to now.playlistName,
            MPMediaItemPropertyPlaybackDuration to now.duration.toDouble(DurationUnit.SECONDS),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to now.elapsed.toDouble(DurationUnit.SECONDS),
            MPNowPlayingInfoPropertyPlaybackRate to if (now.status == NowPlayingStatus.PLAYING) 1.0 else 0.0,
            MPNowPlayingInfoPropertyPlaybackQueueIndex to now.index,
            MPNowPlayingInfoPropertyPlaybackQueueCount to now.queue.size,
            artwork?.let { MPMediaItemPropertyArtwork to it },
        ).toMap<Any?, Any?>()
        commands.nextTrackCommand.enabled = now.hasNext
    }

    /** The cover from its PNG [bytes], handed to the system at whatever size it asks for. */
    private fun coverOf(bytes: ByteArray): MPMediaItemArtwork? {
        val image = bytes.usePinned { UIImage(data = NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong())) }
        return MPMediaItemArtwork(boundsSize = image.size) { _ -> image }
    }

    private fun interrupted(note: NSNotification?) {
        val info = note?.userInfo ?: return
        when ((info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedIntegerValue) {
            AVAudioSessionInterruptionTypeBegan -> {
                // The system has deactivated the session; it is activated again when the player next sounds.
                active = false
                if (feed.nowPlaying.value?.playing == true) {
                    pausedForInterruption = true
                    player.pause()
                }
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedIntegerValue ?: 0u
                if (pausedForInterruption && options and AVAudioSessionInterruptionOptionShouldResume != 0uL) player.resume()
                pausedForInterruption = false
            }
        }
    }

    private fun beginBackgroundTask() {
        if (backgroundTask != UIBackgroundTaskInvalid) return
        backgroundTask = UIApplication.sharedApplication.beginBackgroundTaskWithExpirationHandler { endBackgroundTask() }
    }

    private fun endBackgroundTask() {
        if (backgroundTask == UIBackgroundTaskInvalid) return
        UIApplication.sharedApplication.endBackgroundTask(backgroundTask)
        backgroundTask = UIBackgroundTaskInvalid
    }
}

internal actual fun Scope.systemMediaControls(): SystemMediaControls = IosMediaControls(get(), get())

@Composable
actual fun PlaybackNotificationsPermission() {}
