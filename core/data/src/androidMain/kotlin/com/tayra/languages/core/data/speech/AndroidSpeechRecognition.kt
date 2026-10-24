package com.tayra.languages.core.data.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The system's recogniser, on the device where it has the language: it is asked to stay offline
 * first, and online only when it has no offline model for the language.
 */
class AndroidSpeechRecognition(private val context: Context) : SpeechRecognition {
    private var recognizer: SpeechRecognizer? = null

    override suspend fun isAvailable(languageCode: String): Boolean = withContext(Dispatchers.Main) { SpeechRecognizer.isRecognitionAvailable(context) }

    override suspend fun listen(languageCode: String): Heard = withContext(Dispatchers.Main) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return@withContext Heard.Refused
        val offline = hear(languageCode, offline = true)
        if (offline is Attempt.Unavailable) hear(languageCode, offline = false).heard else offline.heard
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    private sealed interface Attempt {
        val heard: Heard

        class Done(override val heard: Heard) : Attempt

        /** No offline model for the language, or no way to reach one online. */
        object Unavailable : Attempt {
            override val heard = Heard.Refused
        }
    }

    private suspend fun hear(languageCode: String, offline: Boolean): Attempt = suspendCancellableCoroutine { done ->
        val heard = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = heard
        var partial = ""
        fun finish(attempt: Attempt) {
            if (recognizer === heard) recognizer = null
            heard.destroy()
            if (done.isActive) done.resume(attempt)
        }
        heard.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty().ifEmpty { partial }
                finish(Attempt.Done(if (text.isBlank()) Heard.Nothing else Heard.Words(text)))
            }

            override fun onPartialResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { partial = it }
            }

            override fun onError(error: Int) = finish(
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Attempt.Done(Heard.Refused)
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER,
                    -> if (offline) Attempt.Unavailable else Attempt.Done(Heard.Refused)
                    else -> Attempt.Done(if (partial.isNotBlank()) Heard.Words(partial) else Heard.Nothing)
                },
            )

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        heard.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offline),
        )
        done.invokeOnCancellation {
            // The recogniser is the main thread's alone.
            Handler(Looper.getMainLooper()).post {
                if (recognizer === heard) recognizer = null
                heard.cancel()
                heard.destroy()
            }
        }
    }
}
