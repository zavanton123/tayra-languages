package com.tayra.languages.core.domain.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Writes down what is said in a video from the internet, with Whisper on this device, for a video
 * link that comes without subtitles. The video's sound is read from its address and kept nowhere.
 */
interface VideoTranscriber {
    /** What [install] is doing, for the screen that started it; null when idle. */
    val installProgress: StateFlow<String?>

    /** Whether Whisper writes down the language of [languageCode]. */
    fun supports(languageCode: String): Boolean = WhisperLanguages.code(languageCode) != null

    /** The models offered here, the best first among those downloaded. */
    suspend fun models(): List<TranscriptionModel>

    suspend fun install(model: TranscriptionModel)

    suspend fun remove(model: TranscriptionModel)

    /**
     * The lines said in the video at [url], in the language of [languageCode], heard by the best
     * downloaded model. [onProgress] follows the two stages. Throws [TranscriptionException] when
     * the video cannot be read or nothing is downloaded to hear it.
     */
    suspend fun transcribe(url: String, languageCode: String, onProgress: (TranscriptionStage, Float?) -> Unit): List<Cue>
}

/** A Whisper model: [id] is the platform's own name for it; [downloadSize] counts what [VideoTranscriber.install] fetches. */
data class TranscriptionModel(val id: String, val name: String, val downloadSize: Long, val installed: Boolean, val note: String = "")

enum class TranscriptionStage {
    /** The video's sound is read from the internet. */
    AUDIO,

    /** Whisper listens. */
    SPEECH,
}

/** A transcription that could not be made; [message] is shown, translated, as it is. */
class TranscriptionException(message: String) : Exception(message)

/** The languages Whisper writes down, by the app's codes. */
object WhisperLanguages {
    private val codes = setOf(
        "af", "am", "ar", "as", "az", "ba", "be", "bg", "bn", "bo", "br", "bs", "ca", "cs", "cy", "da", "de", "el", "en", "es", "et", "eu",
        "fa", "fi", "fo", "fr", "gl", "gu", "ha", "he", "hi", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "jw", "ka", "kk", "km", "kn",
        "ko", "la", "lb", "ln", "lo", "lt", "lv", "mg", "mi", "mk", "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "nn", "no", "oc", "pa",
        "pl", "ps", "pt", "ro", "ru", "sa", "sd", "si", "sk", "sl", "sn", "so", "sq", "sr", "su", "sv", "sw", "ta", "te", "tg", "th", "tk",
        "tl", "tr", "tt", "uk", "ur", "uz", "vi", "yi", "yo", "zh", "yue",
    )

    /** Whisper's code for the app's [code], or null when Whisper has none; Norwegian Bokmål is Whisper's "no". */
    fun code(code: String): String? = (if (code == "nb") "no" else code.substringBefore('-')).takeIf { it in codes }
}

/** A word Whisper heard, with its time. Its [text] keeps the space before it, as Whisper writes it. */
data class HeardWord(val beginMs: Long, val endMs: Long, val text: String)

/** A stretch Whisper heard at once; [words] is empty when the model gives no word times. */
data class HeardSegment(val beginMs: Long, val endMs: Long, val text: String, val words: List<HeardWord> = emptyList())

/**
 * Subtitle lines out of what Whisper heard. Its segments run from one to thirty seconds and often hold
 * several sentences: where the words carry times, a line ends with a sentence, at a pause, or once it is
 * long enough at a comma; a segment without word times is split at its sentences, its time shared by length.
 */
object TranscriptCues {
    const val SENTENCE_MIN_MS = 1_200L
    const val PAUSE_MS = 700L
    const val COMMA_AFTER_MS = 6_000L
    const val LONGEST_MS = 10_000L

    fun of(segments: List<HeardSegment>): List<Cue> =
        segments.flatMap { if (it.words.isNotEmpty()) byWords(it.words) else bySentences(it) }
            .filter { it.text.isNotBlank() }
            .sortedBy { it.beginMs }

    private fun byWords(words: List<HeardWord>): List<Cue> {
        val cues = mutableListOf<Cue>()
        var line = mutableListOf<HeardWord>()
        fun close() {
            if (line.isNotEmpty()) cues += Cue(line.first().beginMs, line.last().endMs, line.joinToString("") { it.text }.trim().replace(Regex("\\s+"), " "))
            line = mutableListOf()
        }
        words.forEachIndexed { i, word ->
            line += word
            val next = words.getOrNull(i + 1) ?: return@forEachIndexed
            val length = word.endMs - line.first().beginMs
            val end = word.text.trimEnd().trimEnd('"', '»', '”', '\'', ')')
            val breaks = when {
                next.beginMs - word.endMs >= PAUSE_MS -> true
                end.endsWithAny(SENTENCE_ENDS) && length >= SENTENCE_MIN_MS -> true
                end.endsWithAny(CLAUSE_ENDS) && length >= COMMA_AFTER_MS -> true
                else -> next.endMs - line.first().beginMs > LONGEST_MS
            }
            if (breaks) close()
        }
        close()
        return cues
    }

    private fun bySentences(segment: HeardSegment): List<Cue> {
        val sentences = Regex("""[^.!?…。！？]+[.!?…。！？]*["»”')\]]*\s*""").findAll(segment.text.trim()).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
        if (sentences.size <= 1) return listOf(Cue(segment.beginMs, segment.endMs, segment.text.trim()))
        val total = sentences.sumOf { it.length }.toDouble()
        val span = segment.endMs - segment.beginMs
        var at = segment.beginMs
        var done = 0
        return sentences.mapIndexed { i, sentence ->
            done += sentence.length
            val end = if (i == sentences.lastIndex) segment.endMs else segment.beginMs + (span * done / total).toLong()
            Cue(at, end, sentence).also { at = end }
        }
    }

    private fun String.endsWithAny(ends: String) = isNotEmpty() && last() in ends

    private const val SENTENCE_ENDS = ".!?…。！？"
    private const val CLAUSE_ENDS = ",;:，、；："
}
