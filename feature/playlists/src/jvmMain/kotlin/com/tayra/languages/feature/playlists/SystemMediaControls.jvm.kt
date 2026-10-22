package com.tayra.languages.feature.playlists

import androidx.compose.runtime.Composable
import org.koin.core.scope.Scope

// The desktop app plays while it is open, with its own window's controls.
private object NoSystemMediaControls : SystemMediaControls

internal actual fun Scope.systemMediaControls(): SystemMediaControls = NoSystemMediaControls

@Composable
actual fun PlaybackNotificationsPermission() {}
