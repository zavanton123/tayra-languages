package com.tayra.languages.feature.playlists

import com.tayra.languages.core.domain.playlists.SpeechOutput
import com.tayra.languages.core.ui.audio.DeviceSpeechOutput
import com.tayra.languages.core.ui.audio.SpeechSynthesizer
import com.tayra.languages.core.ui.audio.WavPlayer
import org.koin.core.scope.Scope

internal actual fun Scope.deviceSpeechOutput(): SpeechOutput = DeviceSpeechOutput(SpeechSynthesizer(), WavPlayer())
