package com.tayra.languages.core.ui.audio

import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tayra.languages.core.ui.components.AppIcons

/** Speaks text with the platform's text-to-speech engine. */
expect class SpeechSynthesizer {
    /**
     * Speaks [text] in the language with the given ISO 639-1 [languageCode], interrupting any earlier
     * speech. [onDone] runs once when the speech ends, is stopped or fails, possibly on another thread.
     */
    fun speak(text: String, languageCode: String?, onDone: () -> Unit = {})

    /** Holds speech where it is, without calling onDone; false when this platform's voice cannot. */
    fun pause(): Boolean

    /** Goes on from where [pause] held it. */
    fun resume()
    fun stop()
    /** Releases platform resources; the synthesizer must not be used afterwards. */
    fun release()
}

/** A synthesizer scoped to the composition: stopped and released when the composable leaves. */
@Composable
expect fun rememberSpeechSynthesizer(): SpeechSynthesizer

/** A speaker button that reads [text] aloud with the engine chosen in settings, and stops it while it plays. */
@Composable
fun SpeakButton(text: String, languageCode: String?, speaker: Speaker, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val playing by speaker.playing.collectAsState()
    val active = playing != null && playing == text
    IconButton(onClick = { speaker.toggle(text, languageCode) }, modifier = modifier, enabled = enabled && text.isNotBlank()) {
        Icon(if (active) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = if (active) tr("Stop") else tr("Pronounce"), tint = MaterialTheme.colorScheme.primary)
    }
}
