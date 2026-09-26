package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

actual class SpeechSynthesizer {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, languageCode: String?) {
        stop()
        val utterance = AVSpeechUtterance(string = text)
        languageCode?.let { code ->
            // Voices are keyed by full tags such as de-DE, so pick the first voice for the language.
            val voice = AVSpeechSynthesisVoice.speechVoices()
                .filterIsInstance<AVSpeechSynthesisVoice>()
                .firstOrNull { it.language.startsWith(code, ignoreCase = true) }
            if (voice != null) utterance.voice = voice
        }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val synthesizer = remember { SpeechSynthesizer() }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
