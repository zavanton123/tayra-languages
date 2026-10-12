package com.tayra.languages.core.ui.components

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
    ExerciseKind.GAP_CHOICE, ExerciseKind.NEW_CONTEXT -> tr("The sentence with a gap and four words to pick from")
    ExerciseKind.GAP_TYPED -> tr("The sentence with a gap to fill in")
    ExerciseKind.HEAR_CHOOSE -> tr("The word read aloud: pick what it means")
    ExerciseKind.DICTATION -> tr("The sentence read aloud: type the missing word, or a short sentence whole")
}
