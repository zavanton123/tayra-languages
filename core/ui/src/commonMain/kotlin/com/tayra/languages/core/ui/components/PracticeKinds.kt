package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.PracticeNeed
import com.tayra.languages.core.domain.practice.PracticePreset
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSkill
import com.tayra.languages.core.ui.i18n.tr

/** The type's name, as offered to tick. */
fun ExerciseType.label(): String = when (this) {
    ExerciseType.GAP -> tr("Missing word")
    ExerciseType.MEANING -> tr("Meaning")
    ExerciseType.HEAR_WORD -> tr("Hear the word")
    ExerciseType.HEAR_SENTENCE -> tr("Hear the sentence")
    ExerciseType.DICTATION -> tr("Dictation")
    ExerciseType.TRANSLATION -> tr("From the translation")
}

/** What the type asks, in a few words. */
fun ExerciseType.description(): String = when (this) {
    ExerciseType.GAP -> tr("Choose or type the word missing from the sentence")
    ExerciseType.MEANING -> tr("Pick what the marked word means")
    ExerciseType.HEAR_WORD -> tr("Hear a word and choose its meaning")
    ExerciseType.HEAR_SENTENCE -> tr("Hear a sentence and find the word that means what is shown")
    ExerciseType.DICTATION -> tr("Hear the sentence and type the missing word, or all of a short one")
    ExerciseType.TRANSLATION -> tr("Read what the sentence means and find the missing word")
}

fun ExerciseType.icon(): ImageVector = when (this) {
    ExerciseType.GAP -> AppIcons.ViewList
    ExerciseType.MEANING -> AppIcons.Lightbulb
    ExerciseType.HEAR_WORD -> AppIcons.VolumeUp
    ExerciseType.HEAR_SENTENCE -> AppIcons.Headphones
    ExerciseType.DICTATION -> AppIcons.RecordVoiceOver
    ExerciseType.TRANSLATION -> AppIcons.Translate
}

fun PracticeSkill.label(): String = when (this) {
    PracticeSkill.READING -> tr("Reading")
    PracticeSkill.LISTENING -> tr("Listening")
    PracticeSkill.TRANSLATION -> tr("Translation")
}

fun PracticeResponse.label(): String = when (this) {
    PracticeResponse.CHOOSE -> tr("Choose")
    PracticeResponse.TILES -> tr("Letter tiles")
    PracticeResponse.TYPE -> tr("Type")
}

fun PracticeResponse.icon(): ImageVector = when (this) {
    PracticeResponse.CHOOSE -> AppIcons.ViewList
    PracticeResponse.TILES -> AppIcons.GridView
    PracticeResponse.TYPE -> Icons.Default.Edit
}

fun PracticeContext.label(): String = when (this) {
    PracticeContext.ORIGINAL -> tr("From the text")
    PracticeContext.EXAMPLE -> tr("Other examples")
}

/** Why a type cannot be asked of some words. */
fun PracticeNeed.reason(): String = when (this) {
    PracticeNeed.WORD_MEANING -> tr("Needs the word's translation")
    PracticeNeed.OTHER_MEANINGS -> tr("Needs more saved words with meanings")
    PracticeNeed.SENTENCE_TRANSLATION -> tr("Needs a translation of the sentence")
}

fun PracticePreset.label(): String = when (this) {
    PracticePreset.QUICK_REVIEW -> tr("Quick review")
    PracticePreset.BALANCED -> tr("Balanced")
    PracticePreset.LISTENING -> tr("Listening")
    PracticePreset.WRITING -> tr("Writing")
}
