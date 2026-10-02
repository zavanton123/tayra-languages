package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

actual class SpeechSynthesizer {
    private val synthesizer = AVSpeechSynthesizer()
    private var current: AVSpeechUtterance? = null
    private var done: (() -> Unit)? = null

    /** Reports the end of the current utterance; kept here because the synthesizer holds it weakly. */
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) = ended(didFinishSpeechUtterance)

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) = ended(didCancelSpeechUtterance)
    }

    init {
        synthesizer.delegate = delegate
    }

    private fun ended(utterance: AVSpeechUtterance) {
        if (utterance !== current) return
        current = null
        finish()
    }

    private fun finish() {
        done?.also { done = null }?.invoke()
    }

    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit) {
        stop()
        val utterance = AVSpeechUtterance(string = text)
        languageCode?.let { code ->
            // Voices are keyed by full tags such as de-DE, so pick the first voice for the language.
            val voice = AVSpeechSynthesisVoice.speechVoices()
                .filterIsInstance<AVSpeechSynthesisVoice>()
                .firstOrNull { it.language.startsWith(code, ignoreCase = true) }
            if (voice != null) utterance.voice = voice
        }
        current = utterance
        done = onDone
        synthesizer.speakUtterance(utterance)
    }

    actual fun pause(): Boolean = current != null && synthesizer.pauseSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)

    actual fun resume() {
        synthesizer.continueSpeaking()
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        current = null
        finish()
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val synthesizer = remember { SpeechSynthesizer() }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
