package com.tayra.languages.core.domain.playlists

import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How long texts take to hear: their characters at a voice's pace, at the speed chosen in the
 * settings. The pace starts as a guess for the script and turns into what the voices were heard to
 * take once sentences have been synthesized in the language.
 */
class ListeningTime(private val settings: SettingsRepository) {
    private class Heard(val characters: Long, val seconds: Double)

    private val heard = MutableStateFlow<Map<String, Heard>>(emptyMap())

    private val _changes = MutableStateFlow(0)

    /** Changes whenever a measurement changes the pace of a language, so estimates shown can be made again. */
    val changes: StateFlow<Int> = _changes.asStateFlow()

    /** Characters spoken per second at normal speed in the language with [languageCode]. */
    fun pace(languageCode: String?): Double {
        val measured = languageCode?.let { heard.value[it] }?.takeIf { it.seconds >= MIN_SECONDS_HEARD }
        return measured?.let { it.characters / it.seconds } ?: guessedPace(languageCode)
    }

    /** How long [characters] of the language take at the speed chosen. */
    fun duration(characters: Int, languageCode: String?): Duration {
        val speed = settings.current.speechSpeed.takeIf { it > 0f } ?: 1f
        return (characters / (pace(languageCode) * speed) * 1000).roundToLong().milliseconds
    }

    /**
     * Notes that [characters] of the language took [seconds] to say at [speed], from a synthesized
     * sentence's audio; the pace is the average over everything heard so far.
     */
    fun record(languageCode: String, characters: Int, seconds: Double, speed: Float) {
        if (characters <= 0 || seconds <= 0.0) return
        // A clip at another speed counts as the time it would take at normal speed.
        val normal = seconds * speed
        heard.update { all ->
            val before = all[languageCode]
            all + (languageCode to Heard((before?.characters ?: 0) + characters, (before?.seconds ?: 0.0) + normal))
        }
        _changes.update { it + 1 }
    }

    private fun guessedPace(languageCode: String?): Double = if (languageCode in DENSE_SCRIPTS) DENSE_PACE else ALPHABETIC_PACE

    private companion object {
        /** About 150 words a minute in a language written with an alphabet. */
        const val ALPHABETIC_PACE = 14.0

        /** Languages whose characters are whole syllables or words, said a few a second. */
        val DENSE_SCRIPTS = setOf("zh", "ja", "ko", "yue", "lzh")
        const val DENSE_PACE = 5.0

        /** Until this much has been heard the guess is the better estimate. */
        const val MIN_SECONDS_HEARD = 20.0
    }
}

/** How long the WAV audio [wav] plays, read from its header; null when it is no WAV file. */
fun wavSeconds(wav: ByteArray): Double? {
    fun int(at: Int, bytes: Int): Long = (0 until bytes).fold(0L) { acc, i -> acc or ((wav[at + i].toLong() and 0xFF) shl (8 * i)) }
    fun tag(at: Int) = wav.copyOfRange(at, at + 4).decodeToString()
    if (wav.size < 12 || tag(0) != "RIFF" || tag(8) != "WAVE") return null
    var byteRate = 0L
    var at = 12
    while (at + 8 <= wav.size) {
        val size = int(at + 4, 4)
        when (tag(at)) {
            "fmt " -> if (at + 20 <= wav.size) byteRate = int(at + 16, 4)
            // Streamed WAVs leave the data size unset; what follows the header is the data then.
            "data" -> return if (byteRate > 0) (if (size in 1..(wav.size - at - 8).toLong()) size else (wav.size - at - 8).toLong()).toDouble() / byteRate else null
        }
        at += 8 + size.toInt() + (size.toInt() and 1)
    }
    return null
}
