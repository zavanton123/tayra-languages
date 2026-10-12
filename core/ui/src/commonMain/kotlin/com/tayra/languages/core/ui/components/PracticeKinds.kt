package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.ui.i18n.tr

/** The kind's name, as offered to choose from; a sentence from elsewhere comes with the gap choice. */
fun ExerciseKind.label(): String = when (this) {
    ExerciseKind.GAP_CHOICE, ExerciseKind.NEW_CONTEXT -> tr("Choose the missing word")
    ExerciseKind.GAP_TYPED -> tr("Type the missing word")
    ExerciseKind.HEAR_CHOOSE -> tr("Listen and choose")
    ExerciseKind.DICTATION -> tr("Listen and type")
}

/** What the kind asks, in a few words. */
fun ExerciseKind.description(): String = when (this) {
    ExerciseKind.GAP_CHOICE, ExerciseKind.NEW_CONTEXT -> tr("Pick from four options")
    ExerciseKind.GAP_TYPED -> tr("Complete the sentence")
    ExerciseKind.HEAR_CHOOSE -> tr("Hear a word and choose its meaning")
    ExerciseKind.DICTATION -> tr("Hear the prompt and type the answer")
}

/** The kind's icon, as in the dialog that offers it. */
fun ExerciseKind.icon(): ImageVector = when (this) {
    ExerciseKind.GAP_CHOICE, ExerciseKind.NEW_CONTEXT -> AppIcons.ViewList
    ExerciseKind.GAP_TYPED -> Icons.Default.Edit
    ExerciseKind.HEAR_CHOOSE -> AppIcons.VolumeUp
    ExerciseKind.DICTATION -> AppIcons.RecordVoiceOver
}
