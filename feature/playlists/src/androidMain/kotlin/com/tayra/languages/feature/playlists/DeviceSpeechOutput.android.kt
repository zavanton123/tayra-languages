package com.tayra.languages.feature.playlists

import android.content.Context
import com.tayra.languages.core.domain.playlists.SpeechOutput
import com.tayra.languages.core.ui.audio.DeviceSpeechOutput
import com.tayra.languages.core.ui.audio.SpeechSynthesizer
import com.tayra.languages.core.ui.audio.WavPlayer
import org.koin.core.scope.Scope

// A file of its own, so the reader's clips never overwrite the one the playlist is playing.
internal actual fun Scope.deviceSpeechOutput(): SpeechOutput = DeviceSpeechOutput(SpeechSynthesizer(get<Context>()), WavPlayer(get(), fileName = "playlist.wav"))
