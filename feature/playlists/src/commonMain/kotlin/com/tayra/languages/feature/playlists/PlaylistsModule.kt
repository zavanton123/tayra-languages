package com.tayra.languages.feature.playlists

import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.SpeechOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.scope.Scope
import org.koin.dsl.module

/** The device's voice and audio player for the playlist player, made apart from any screen. */
internal expect fun Scope.deviceSpeechOutput(): SpeechOutput

val playlistsModule = module {
    single<SpeechOutput> { deviceSpeechOutput() }
    // The app's player: it outlives every screen, so it has a scope of its own on the main thread.
    single { PlaylistPlayer(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Main)) }
}
