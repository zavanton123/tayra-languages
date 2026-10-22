package com.tayra.languages.core.ui.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

actual class SpeechSynthesizer(context: Context) {
    private var ready = false
    private var pending: Request? = null
    private val ids = AtomicLong()

    /** The completion of each utterance still speaking, by utterance id. */
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    /** The word reports of each utterance still speaking that asked for them, by utterance id. */
    private val words = ConcurrentHashMap<String, (IntRange) -> Unit>()

    private class Request(val text: String, val languageCode: String?, val onDone: () -> Unit, val rate: Float, val onWord: ((IntRange) -> Unit)?)

    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (!ready) Logger.w { "Text-to-speech engine unavailable ($status)" }
        pending?.let { if (ready) speak(it.text, it.languageCode, it.onDone, it.rate, it.onWord) else it.onDone() }
        pending = null
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId)
            override fun onError(utteranceId: String, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                words[utteranceId]?.invoke(start until end)
            }
        })
    }

    private fun finish(utteranceId: String) {
        words.remove(utteranceId)
        callbacks.remove(utteranceId)?.invoke()
    }

    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit, rate: Float, onWord: ((IntRange) -> Unit)?) {
        if (!ready) {
            pending?.onDone?.invoke()
            pending = Request(text, languageCode, onDone, rate, onWord)
            return
        }
        languageCode?.let { tts.setLanguage(Locale.forLanguageTag(it)) }
        tts.setSpeechRate(rate)
        val id = "tayra-${ids.incrementAndGet()}"
        callbacks[id] = onDone
        if (onWord != null) words[id] = onWord
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id)
    }

    /** Android's text-to-speech can only stop, so a pause starts the sentence over. */
    actual fun pause(): Boolean = false

    actual fun resume() {}

    actual fun stop() {
        pending?.onDone?.invoke()
        pending = null
        tts.stop()
        // An engine that does not report onStop still releases its waiters.
        callbacks.keys.toList().forEach(::finish)
    }

    actual fun release() {
        stop()
        tts.shutdown()
    }
}

@Composable
actual fun rememberSpeechSynthesizer(): SpeechSynthesizer {
    val context = LocalContext.current
    val synthesizer = remember(context) { SpeechSynthesizer(context) }
    DisposableEffect(synthesizer) { onDispose { synthesizer.release() } }
    return synthesizer
}
