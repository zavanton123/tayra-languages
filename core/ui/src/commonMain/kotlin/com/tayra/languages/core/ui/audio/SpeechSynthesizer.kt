package com.tayra.languages.core.ui.audio

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tayra.languages.core.ui.components.AppIcons

/** Speaks text with the platform's text-to-speech engine. */
expect class SpeechSynthesizer {
    /** Speaks [text] in the language with the given ISO 639-1 [languageCode], interrupting any earlier speech. */
    fun speak(text: String, languageCode: String?)
    fun stop()
    /** Releases platform resources; the synthesizer must not be used afterwards. */
    fun release()
}

/** A synthesizer scoped to the composition: stopped and released when the composable leaves. */
@Composable
expect fun rememberSpeechSynthesizer(): SpeechSynthesizer

/** A speaker button that reads [text] aloud. */
@Composable
fun SpeakButton(text: String, languageCode: String?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val synthesizer = rememberSpeechSynthesizer()
    IconButton(onClick = { synthesizer.speak(text, languageCode) }, modifier = modifier, enabled = enabled && text.isNotBlank()) {
        Icon(AppIcons.VolumeUp, contentDescription = "Pronounce", tint = MaterialTheme.colorScheme.primary)
    }
}
