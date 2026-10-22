package com.tayra.languages.feature.playlists

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

/**
 * Starts the playback service while a playlist is loaded, by connecting to its session, and lets it
 * go when the playlist is unloaded; the service then shows the controls and keeps the app running.
 */
private class AndroidMediaControls(private val context: Context, feed: NowPlayingFeed) : SystemMediaControls {
    private var controller: ListenableFuture<MediaController>? = null

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            feed.nowPlaying.map { it != null }.distinctUntilChanged().collect { loaded -> if (loaded) connect() else disconnect() }
        }
    }

    private fun connect() {
        if (controller != null) return
        controller = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaylistPlaybackService::class.java))).buildAsync()
    }

    private fun disconnect() {
        controller?.let(MediaController::releaseFuture)
        controller = null
    }
}

internal actual fun Scope.systemMediaControls(): SystemMediaControls = AndroidMediaControls(get(), get())

@Composable
actual fun PlaybackNotificationsPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val player = koinInject<PlaylistPlayer>()
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(player) {
        // Asked when a playlist first plays, once while the app is open.
        player.state.map { it.isPlaying }.distinctUntilChanged().first { it }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
