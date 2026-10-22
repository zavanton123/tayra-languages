package com.tayra.languages.feature.playlists

import androidx.compose.runtime.Composable
import org.koin.core.scope.Scope

// Until the platform's controls are in place.
private object NoSystemMediaControls : SystemMediaControls

internal actual fun Scope.systemMediaControls(): SystemMediaControls = NoSystemMediaControls

@Composable
actual fun PlaybackNotificationsPermission() {}
