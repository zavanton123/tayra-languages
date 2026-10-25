package com.tayra.languages.core.domain.practice

import kotlinx.coroutines.flow.StateFlow

/**
 * Hears what the learner says, on the device: the system's recogniser on a phone, a local
 * model on a desktop. Practice offers answers by voice only where [isAvailable] says so.
 */
interface SpeechRecognition {
    /** Whether speech in the language with the ISO 639-1 [languageCode] can be recognised here now. */
    suspend fun isAvailable(languageCode: String): Boolean

    /**
     * Listens until the learner stops speaking, or [stop] is called. [hint] is what they are expected
     * to say, which recognisers that take one lean towards: a learner's accent is heard far better.
     * [onRecorded] is told when the speaking is over and only the recognising is left, which can take
     * a few seconds.
     */
    suspend fun listen(languageCode: String, hint: String? = null, onRecorded: () -> Unit = {}): Heard

    /** Stops listening early; [listen] then returns what was heard so far. */
    fun stop() {}
}

/** The words of a [SpeechRecognition.listen] hint, for recognisers that take words rather than a sentence. */
fun hintWords(hint: String?): List<String> =
    hint.orEmpty().split(Regex("""[^\p{L}\p{N}'’-]+""")).filter { it.isNotBlank() }.distinct()

/** What listening came to. */
sealed interface Heard {
    data class Words(val text: String) : Heard

    /** Nothing was made out: worth another try. */
    data object Nothing : Heard

    /** The microphone or recognition was refused or failed: speaking is given up for this practice. */
    data object Refused : Heard
}

/** A recogniser set up from the Speech settings before it can hear anything: a local model on a desktop. */
interface SpeechRecognitionSetup {
    /** What setting up is doing right now, or null. */
    val progress: StateFlow<String?>

    suspend fun isReady(): Boolean

    /** About how many bytes setting up downloads. */
    suspend fun downloadSize(): Long

    suspend fun setUp()
}
