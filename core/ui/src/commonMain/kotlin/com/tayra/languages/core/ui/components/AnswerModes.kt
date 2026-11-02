package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.ui.i18n.tr

/** The name of a way of answering flashcards. */
val AnswerMode.label: String
    @Composable get() = when (this) {
        AnswerMode.SHOW -> tr("Show")
        AnswerMode.TYPE -> tr("Type")
        AnswerMode.SPEAK -> tr("Speak")
    }

/**
 * The ways of answering flashcards side by side, [selected] tinted: the answer shown, or the word
 * typed or said first. [modes] are those this device offers; [iconsOnly] names them by tooltip
 * alone, for a phone's header. Each is tagged "answer-mode-SHOW" and so on.
 */
@Composable
fun AnswerModeToggle(
    selected: AnswerMode,
    onSelect: (AnswerMode) -> Unit,
    modifier: Modifier = Modifier,
    modes: List<AnswerMode> = AnswerMode.entries,
    iconsOnly: Boolean = false,
    fill: Boolean = false,
) {
    val segments = modes.map { mode ->
        val icon = when (mode) {
            AnswerMode.SHOW -> AppIcons.Style
            AnswerMode.TYPE -> AppIcons.Keyboard
            AnswerMode.SPEAK -> AppIcons.Mic
        }
        Segment(mode, icon = icon, label = if (iconsOnly) null else mode.label, description = mode.label, testTag = "answer-mode-${mode.name}")
    }
    SegmentedToggle(segments, selected, onSelect, modifier, fill = fill)
}
