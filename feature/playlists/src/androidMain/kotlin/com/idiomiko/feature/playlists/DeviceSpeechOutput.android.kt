package com.idiomiko.feature.playlists

import android.content.Context
import com.idiomiko.core.domain.playlists.SpeechOutput
import com.idiomiko.core.ui.audio.DeviceSpeechOutput
import com.idiomiko.core.ui.audio.SpeechSynthesizer
import com.idiomiko.core.ui.audio.WavPlayer
import org.koin.core.scope.Scope

// A file of its own, so the reader's clips never overwrite the one the playlist is playing.
internal actual fun Scope.deviceSpeechOutput(): SpeechOutput = DeviceSpeechOutput(SpeechSynthesizer(get<Context>()), WavPlayer(get(), fileName = "playlist.wav"))
