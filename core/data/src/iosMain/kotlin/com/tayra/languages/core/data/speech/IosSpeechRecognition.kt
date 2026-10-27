package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.hintWords
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.Speech.SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
import platform.Speech.SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined

/**
 * The system's recogniser, on the device where it has the language. It hears from the microphone
 * until the learner pauses after speaking, and the audio session is put back as it was after.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSpeechRecognition : SpeechRecognition {
    private var stopping = false

    override suspend fun isAvailable(languageCode: String): Boolean = withContext(Dispatchers.Main) {
        val status = SFSpeechRecognizer.authorizationStatus()
        val allowed = status == SFSpeechRecognizerAuthorizationStatusAuthorized || status == SFSpeechRecognizerAuthorizationStatusNotDetermined
        allowed && SFSpeechRecognizer(NSLocale(languageCode)).isAvailable()
    }

    override suspend fun listen(languageCode: String, hint: String?, onRecorded: () -> Unit): Heard = withContext(Dispatchers.Main) {
        if (!allowed()) return@withContext Heard.Refused
        val recognizer = SFSpeechRecognizer(NSLocale(languageCode))
        // Unavailable for now (offline, or busy), or the audio held by something else: worth another try.
        if (!recognizer.isAvailable()) return@withContext Heard.Failed
        val session = AVAudioSession.sharedInstance()
        val category = session.category
        val mode = session.mode
        val options = session.categoryOptions
        if (!session.setCategory(AVAudioSessionCategoryPlayAndRecord, mode = AVAudioSessionModeMeasurement, options = AVAudioSessionCategoryOptionDefaultToSpeaker, error = null) ||
            !session.setActive(true, error = null)
        ) {
            return@withContext Heard.Failed
        }
        val engine = AVAudioEngine()
        val input = engine.inputNode
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = recognizer.supportsOnDeviceRecognition
        // The words expected, which the recogniser leans towards.
        request.contextualStrings = hintWords(hint)
        input.installTapOnBus(0u, bufferSize = 1024u, format = input.outputFormatForBus(0u)) { buffer, _ -> buffer?.let(request::appendAudioPCMBuffer) }
        val updates = Channel<Update>(Channel.UNLIMITED)
        val task = recognizer.recognitionTaskWithRequest(request) { result, error ->
            updates.trySend(Update(result?.bestTranscription?.formattedString.orEmpty(), done = result?.isFinal() == true || error != null))
        }
        stopping = false
        try {
            engine.prepare()
            if (!engine.startAndReturnError(null)) return@withContext Heard.Failed
            hear(updates) { engine.stop(); request.endAudio(); onRecorded() }
        } finally {
            engine.stop()
            input.removeTapOnBus(0u)
            task.cancel()
            category?.let { session.setCategory(it, mode = mode, options = options, error = null) }
        }
    }

    override fun stop() {
        stopping = true
    }

    private class Update(val text: String, val done: Boolean)

    /** What is heard until a pause after speaking, [stop], or [LIMIT]; then [endAudio] lets the last words through. */
    private suspend fun hear(updates: Channel<Update>, endAudio: () -> Unit): Heard {
        val started = TimeSource.Monotonic.markNow()
        var changed = started
        var ended: TimeSource.Monotonic.ValueTimeMark? = null
        var heard = ""
        while (true) {
            val update = withTimeoutOrNull(TICK) { updates.receive() }
            if (update != null) {
                if (update.text.isNotBlank() && update.text != heard) {
                    heard = update.text
                    changed = TimeSource.Monotonic.markNow()
                }
                if (update.done) break
            }
            if (ended == null && (stopping || started.elapsedNow() > LIMIT || (heard.isNotEmpty() && changed.elapsedNow() > PAUSE))) {
                endAudio()
                ended = TimeSource.Monotonic.markNow()
            }
            if (ended != null && ended.elapsedNow() > LAST_WORDS) break
        }
        return if (heard.isBlank()) Heard.Nothing else Heard.Words(heard.trim())
    }

    /** Recognition and the microphone, each asked for the first time it is needed. */
    private suspend fun allowed(): Boolean {
        val speech = when (SFSpeechRecognizer.authorizationStatus()) {
            SFSpeechRecognizerAuthorizationStatusAuthorized -> true
            SFSpeechRecognizerAuthorizationStatusNotDetermined -> suspendCancellableCoroutine { done ->
                SFSpeechRecognizer.requestAuthorization { status: SFSpeechRecognizerAuthorizationStatus -> done.resume(status == SFSpeechRecognizerAuthorizationStatusAuthorized) }
            }
            else -> false
        }
        return speech && suspendCancellableCoroutine { done -> AVAudioSession.sharedInstance().requestRecordPermission { granted -> done.resume(granted) } }
    }

    private companion object {
        val TICK = 200.milliseconds
        val PAUSE = 1.5.seconds
        val LIMIT = 20.seconds
        val LAST_WORDS = 2.seconds
    }
}
