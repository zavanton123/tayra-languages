package com.idiomiko.feature.playlists

import com.idiomiko.core.domain.playlists.SpeechOutput
import com.idiomiko.core.ui.audio.DeviceSpeechOutput
import com.idiomiko.core.ui.audio.SpeechSynthesizer
import com.idiomiko.core.ui.audio.WavPlayer
import org.koin.core.scope.Scope

internal actual fun Scope.deviceSpeechOutput(): SpeechOutput = DeviceSpeechOutput(SpeechSynthesizer(), WavPlayer())
