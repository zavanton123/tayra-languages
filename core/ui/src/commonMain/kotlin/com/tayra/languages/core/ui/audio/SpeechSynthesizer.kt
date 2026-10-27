package com.tayra.languages.core.ui.audio

import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppTooltip

/** Speaks text with the platform's text-to-speech engine. */
expect class SpeechSynthesizer {
    /**
     * Speaks [text] in the language with the given ISO 639-1 [languageCode], interrupting any earlier
     * speech. [onDone] runs once when the speech ends, is stopped or fails, possibly on another thread.
     * [rate] is the speed, 1 for the voice's own; where the platform's voice tells, [onWord] gets the
     * characters of each word as it is reached, also possibly on another thread.
     */
    fun speak(text: String, languageCode: String?, onDone: () -> Unit = {}, rate: Float = 1f, onWord: ((IntRange) -> Unit)? = null)

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

/**
 * A speaker button that reads [text] aloud with the engine chosen in settings, and stops it while it plays;
 * [style], [size] and [shape] are its icon button's. With a [tooltip] it says so on hover, and Stop while it plays.
 */
@Composable
fun SpeakButton(
    text: String,
    languageCode: String?,
    speaker: Speaker,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Plain,
    size: IconButtonSize = IconButtonSize.Regular,
    shape: Shape? = null,
    tooltip: String? = null,
) {
    val playing by speaker.playing.collectAsState()
    val active = playing != null && playing == text
    val description = if (active) tr("Stop") else tooltip ?: tr("Pronounce")
    val button = @Composable {
        AppIconButton(
            if (active) AppIcons.Stop else AppIcons.VolumeUp,
            contentDescription = description,
            onClick = { speaker.toggle(text, languageCode) },
            modifier = modifier,
            enabled = enabled && text.isNotBlank(),
            size = size,
            style = style,
            tint = MaterialTheme.colorScheme.primary,
            shape = shape,
            selected = active,
        )
    }
    if (tooltip != null) AppTooltip(description) { button() } else button()
}
