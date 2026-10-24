package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.ui.i18n.tr

/** A kind of question as offered to tick: a type of question answered one way, named as earlier versions named it. */
class PracticeQuestion(val name: String, val type: ExerciseType, val response: PracticeResponse) {
    val label: String
        get() = when (type) {
            ExerciseType.GAP -> if (response == PracticeResponse.CHOOSE) tr("Choose the missing word") else tr("Type the missing word")
            ExerciseType.MEANING -> tr("Choose the meaning")
            ExerciseType.HEAR_WORD -> tr("Listen and choose")
            ExerciseType.DICTATION -> tr("Listen and type")
        }

    /** What it asks, in a few words. */
    val description: String
        get() = when (type) {
            ExerciseType.GAP -> if (response == PracticeResponse.CHOOSE) tr("Pick from four options") else tr("Complete the sentence")
            ExerciseType.MEANING -> tr("Pick what the marked word means")
            ExerciseType.HEAR_WORD -> tr("Hear a word and choose its meaning")
            ExerciseType.DICTATION -> tr("Hear the prompt and type the answer")
        }

    val icon: ImageVector
        get() = when (type) {
            ExerciseType.GAP -> if (response == PracticeResponse.CHOOSE) AppIcons.ViewList else Icons.Default.Edit
            ExerciseType.MEANING -> AppIcons.Translate
            ExerciseType.HEAR_WORD -> AppIcons.VolumeUp
            ExerciseType.DICTATION -> AppIcons.RecordVoiceOver
        }

    companion object {
        /** The kinds offered, in the order they come round in a practice. */
        val all = listOf(
            PracticeQuestion("GAP_CHOICE", ExerciseType.GAP, PracticeResponse.CHOOSE),
            PracticeQuestion("MEANING", ExerciseType.MEANING, PracticeResponse.CHOOSE),
            PracticeQuestion("HEAR_CHOOSE", ExerciseType.HEAR_WORD, PracticeResponse.CHOOSE),
            PracticeQuestion("GAP_TYPED", ExerciseType.GAP, PracticeResponse.TYPE),
            PracticeQuestion("DICTATION", ExerciseType.DICTATION, PracticeResponse.TYPE),
        )

        fun of(type: ExerciseType, response: PracticeResponse): PracticeQuestion? = all.firstOrNull { it.type == type && it.response == response }
    }
}
