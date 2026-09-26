package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

private fun speakText(text: String, language: String?): Unit = js(
    """{
        if (!window.speechSynthesis) return;
        window.speechSynthesis.cancel();
        var utterance = new SpeechSynthesisUtterance(text);
        if (language) utterance.lang = language;
        window.speechSynthesis.speak(utterance);
    }""",
)

private fun cancelSpeech(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.cancel(); }")

/** Uses the browser's Web Speech API. */
actual class SpeechSynthesizer {
    actual fun speak(text: String, languageCode: String?) = speakText(text, languageCode)
    actual fun stop() = cancelSpeech()
    actual fun release() = stop()
}

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val synthesizer = remember { SpeechSynthesizer() }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
