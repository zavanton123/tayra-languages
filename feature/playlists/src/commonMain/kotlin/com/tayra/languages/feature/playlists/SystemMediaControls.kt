package com.tayra.languages.feature.playlists

import androidx.compose.runtime.Composable
import org.koin.core.scope.Scope

/**
 * The system's own controls of the playlist player: the notification, the lock screen and the
 * headset's buttons on Android, the Now Playing controls on iOS, the browser's media keys on the
 * web. Made as the app starts, they follow [NowPlayingFeed] and pass their presses to the player.
 */
internal interface SystemMediaControls

internal expect fun Scope.systemMediaControls(): SystemMediaControls

/** Asks to show the player's notification the first time a playlist plays, where the system asks for that (Android 13 on). */
@Composable
expect fun PlaybackNotificationsPermission()
