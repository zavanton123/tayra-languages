package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

private fun speakText(text: String, language: String?, onDone: () -> Unit): Unit = js(
    """{
        if (!window.speechSynthesis) { onDone(); return; }
        window.speechSynthesis.cancel();
        var utterance = new SpeechSynthesisUtterance(text);
        if (language) utterance.lang = language;
        var finished = false;
        var finish = function() { if (!finished) { finished = true; onDone(); } };
        utterance.onend = finish;
        utterance.onerror = finish;
        window.speechSynthesis.speak(utterance);
    }""",
)

private fun cancelSpeech(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.cancel(); }")
private fun pauseSpeech(): Boolean = js("{ if (!window.speechSynthesis || !window.speechSynthesis.speaking) return false; window.speechSynthesis.pause(); return true; }")
private fun resumeSpeech(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.resume(); }")

/** Uses the browser's Web Speech API. */
actual class SpeechSynthesizer {
    private var done: (() -> Unit)? = null

    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit) {
        stop()
        done = onDone
        speakText(text, languageCode) { finish() }
    }

    private fun finish() {
        done?.also { done = null }?.invoke()
    }

    actual fun pause(): Boolean = done != null && pauseSpeech()

    actual fun resume() = resumeSpeech()

    actual fun stop() {
        cancelSpeech()
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
