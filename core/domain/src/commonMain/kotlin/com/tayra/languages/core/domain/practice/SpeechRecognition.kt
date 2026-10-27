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

    /**
     * The microphone gave no sound at all, as one muted, switching (a headset changing modes) or held
     * by another app does: worth another try, though twice running it is as good as refused.
     */
    data object Muted : Heard

    /** Recognition failed this time, the recogniser erring or not ready: worth another try. */
    data object Failed : Heard

    /** The microphone may not be used, or nothing here hears the language: speaking is given up for this practice. */
    data object Refused : Heard
}

/** How well a downloaded recogniser hears, against how long it takes. */
enum class RecognitionQuality { MEDIUM, HIGH }

/**
 * A recogniser's model of one [quality]: about how much [install] downloads, and whether it is there.
 * [runtimeSize] is the part of [downloadSize] that what the models run on takes, fetched once for both;
 * [name] is the model's own, as "small".
 */
data class RecognitionModel(val quality: RecognitionQuality, val downloadSize: Long, val installed: Boolean, val runtimeSize: Long = 0, val name: String = "")

/**
 * A recogniser set up from the Speech settings before it can hear anything: a local model on a
 * desktop, of either quality; the one chosen in the settings hears, or else the other if only it
 * is downloaded.
 */
interface SpeechRecognitionSetup {
    /** What setting up is doing right now, or null. */
    val progress: StateFlow<String?>

    suspend fun models(): List<RecognitionModel>

    /** Whether speech in the language with the ISO 639-1 [languageCode] can be heard once a model is downloaded. */
    fun supports(languageCode: String): Boolean

    /** Downloads the model of [quality], and what it runs on the first time. */
    suspend fun install(quality: RecognitionQuality)

    suspend fun remove(quality: RecognitionQuality)
}
