package com.tayra.languages.core.ui.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import java.util.Locale

actual class SpeechSynthesizer(context: Context) {
    private var ready = false
    private var pending: Pair<String, String?>? = null
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (!ready) Logger.w { "Text-to-speech engine unavailable ($status)" }
        pending?.let { (text, language) -> speak(text, language) }
        pending = null
    }

    actual fun speak(text: String, languageCode: String?) {
        if (!ready) {
            pending = text to languageCode
            return
        }
        languageCode?.let { tts.setLanguage(Locale.forLanguageTag(it)) }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "term")
    }

    actual fun stop() {
        pending = null
        tts.stop()
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
