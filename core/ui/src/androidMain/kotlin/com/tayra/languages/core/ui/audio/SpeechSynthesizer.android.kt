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
    private var pending: Triple<String, String?, () -> Unit>? = null
    private val ids = AtomicLong()

    /** The completion of each utterance still speaking, by utterance id. */
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (!ready) Logger.w { "Text-to-speech engine unavailable ($status)" }
        pending?.let { (text, language, onDone) -> if (ready) speak(text, language, onDone) else onDone() }
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
        })
    }

    private fun finish(utteranceId: String) {
        callbacks.remove(utteranceId)?.invoke()
    }

    actual fun speak(text: String, languageCode: String?, onDone: () -> Unit) {
        if (!ready) {
            pending?.third?.invoke()
            pending = Triple(text, languageCode, onDone)
            return
        }
        languageCode?.let { tts.setLanguage(Locale.forLanguageTag(it)) }
        val id = "tayra-${ids.incrementAndGet()}"
        callbacks[id] = onDone
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id)
    }

    actual fun stop() {
        pending?.third?.invoke()
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
