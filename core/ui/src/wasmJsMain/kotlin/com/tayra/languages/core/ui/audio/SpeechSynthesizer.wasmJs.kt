package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private fun speakText(text: String, language: String?, withVoice: Boolean, rate: Double, onWord: (Int, Int) -> Unit, onDone: () -> Unit): Unit = js(
    """{
        if (!window.speechSynthesis) { onDone(); return; }
        window.speechSynthesis.cancel();
        var utterance = new SpeechSynthesisUtterance(text);
        if (language) utterance.lang = language;
        if (withVoice) utterance.voice = window.speechSynthesis.getVoices().find(function(v) { return v.lang === language; }) || null;
        utterance.rate = rate;
        utterance.onboundary = function(e) {
            if (e.name && e.name !== 'word') return;
            var length = e.charLength || ((text.slice(e.charIndex).match(/^\S+/) || [''])[0].length);
            onWord(e.charIndex, e.charIndex + length);
        };
        var finished = false;
        var finish = function() { if (!finished) { finished = true; onDone(); } };
        utterance.onend = finish;
        utterance.onerror = finish;
        window.speechSynthesis.speak(utterance);
    }""",
)

/** Chrome fills its list of voices after the first asking, and says so with voiceschanged; some browsers never do. */
private fun whenVoicesKnown(ready: () -> Unit): Unit = js(
    """{
        var speech = window.speechSynthesis;
        if (!speech || speech.getVoices().length) { ready(); return; }
        var called = false;
        var once = function() {
            if (called) return;
            called = true;
            speech.removeEventListener('voiceschanged', once);
            ready();
        };
        speech.addEventListener('voiceschanged', once);
        setTimeout(once, 1000);
    }""",
)

private fun voiceLanguages(): String =
    js("{ return window.speechSynthesis ? window.speechSynthesis.getVoices().map(function(v) { return v.lang; }).join('\\n') : ''; }")

private fun cancelSpeech(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.cancel(); }")
private fun pauseSpeech(): Boolean = js("{ if (!window.speechSynthesis || !window.speechSynthesis.speaking) return false; window.speechSynthesis.pause(); return true; }")
private fun resumeSpeech(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.resume(); }")

/** Uses the browser's Web Speech API. */
actual class SpeechSynthesizer {
    private var done: (() -> Unit)? = null

    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit, rate: Float, onWord: ((IntRange) -> Unit)?) {
        stop()
        done = onDone
        val say = { language: String?, withVoice: Boolean ->
            speakText(text, language, withVoice, rate.toDouble(), { start, end -> if (done === onDone) onWord?.invoke(start until end) }) { finish() }
        }
        if (!CantoneseVoices.isCantonese(languageCode)) {
            say(languageCode, false)
            return
        }
        whenVoicesKnown {
            if (done === onDone) {
                val voice = cantoneseVoice()
                if (voice != null) say(voice, true) else finish()
            }
        }
    }

    private fun cantoneseVoice(): String? = voiceLanguages().split('\n').firstOrNull(CantoneseVoices::matches)

    actual suspend fun hasVoice(languageCode: String?): Boolean {
        if (!CantoneseVoices.isCantonese(languageCode)) return true
        return suspendCoroutine { answer -> whenVoicesKnown { answer.resume(cantoneseVoice() != null) } }
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
