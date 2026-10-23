package com.tayra.languages.feature.playlists

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Keeps the app running while a playlist is loaded, with the media notification, the lock screen's
 * controls and the headset's buttons, all through a media session over the playlist player.
 *
 * While the player sounds it holds the audio focus and keeps the processor awake between
 * sentences: a call or another app's sound pauses it, and it goes on when a short loss ends;
 * unplugging the headphones pauses it.
 *
 * The system sends the headset's buttons to the session of the app whose audio is playing, and the
 * system voice is played by the speech engine's own app; so while the player sounds, this plays
 * silence of its own.
 */
class PlaylistPlaybackService : MediaSessionService(), KoinComponent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player: PlaylistPlayer = get()
    private val nowPlaying = get<NowPlayingFeed>().nowPlaying
    private var session: MediaSession? = null

    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private val speech = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(speech)
            // Speech ducked under another sound cannot be followed, so it pauses instead.
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
            .build()
    }
    private var hasFocus = false

    /** Set while the player is held for a short loss of the focus, so it goes on when the focus returns. */
    private var pausedForFocus = false

    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tayra:playlist").apply { setReferenceCounted(false) }
    }

    private val silence by lazy {
        AudioTrack.Builder()
            .setAudioAttributes(speech)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SILENCE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(SILENCE_RATE * 2)
            .build()
            .apply {
                write(ShortArray(SILENCE_RATE), 0, SILENCE_RATE)
                setLoopPoints(0, SILENCE_RATE, -1)
            }
    }

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) player.pause()
        }
    }
    private var noisyRegistered = false

    override fun onCreate() {
        super.onCreate()
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val sessionPlayer = SessionPlayer(player, nowPlaying, scope)
        session = MediaSession.Builder(this, sessionPlayer)
            .apply { if (open != null) setSessionActivity(open) }
            .build()
        scope.launch { sessionPlayer.artwork = nowPlayingArtwork() }
        scope.launch {
            nowPlaying.map { it?.playing }.distinctUntilChanged().collect { playing ->
                when (playing) {
                    true -> startSounding()
                    false -> stopSounding()
                    // The playlist was unloaded: nothing is left to show or play.
                    null -> {
                        stopSounding()
                        stopSelf()
                    }
                }
            }
        }
        scope.launch {
            // Renewed with every sentence, so the lock outlasts no playing.
            nowPlaying.collect { if (it?.playing == true) wakeLock.acquire(WAKE_LOCK_MS) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // The app swiped away while nothing sounds ends listening, keeping its place; while it sounds, it plays on.
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (nowPlaying.value?.playing != true) player.stop()
    }

    private fun startSounding() {
        pausedForFocus = false
        if (!hasFocus) {
            if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // During a call, for one.
                player.pause()
                return
            }
            hasFocus = true
        }
        runCatching { silence.play() }
        if (!noisyRegistered) {
            ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyRegistered = true
        }
    }

    private fun stopSounding() {
        if (wakeLock.isHeld) wakeLock.release()
        runCatching { silence.pause() }
        if (noisyRegistered) {
            unregisterReceiver(noisy)
            noisyRegistered = false
        }
        if (hasFocus && !pausedForFocus) {
            audio.abandonAudioFocusRequest(focus)
            hasFocus = false
        }
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> if (pausedForFocus) {
                pausedForFocus = false
                player.resume()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (nowPlaying.value?.playing == true) {
                pausedForFocus = true
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedForFocus = false
                hasFocus = false
                audio.abandonAudioFocusRequest(focus)
                player.pause()
            }
        }
    }

    override fun onDestroy() {
        stopSounding()
        if (hasFocus) audio.abandonAudioFocusRequest(focus)
        runCatching { silence.release() }
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        /** Longer than any sentence takes to say. */
        const val WAKE_LOCK_MS = 10 * 60 * 1000L

        /** A second of silence a loop, at a low rate. */
        const val SILENCE_RATE = 8000
    }
}
