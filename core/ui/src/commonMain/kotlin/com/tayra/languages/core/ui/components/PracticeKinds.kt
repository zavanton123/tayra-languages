package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.PracticeNeed
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSkill
import com.tayra.languages.core.ui.i18n.tr

/** The type's name, as offered to tick. */
fun ExerciseType.label(): String = when (this) {
    ExerciseType.GAP -> tr("Missing word")
    ExerciseType.MEANING -> tr("Meaning")
    ExerciseType.MATCHING -> tr("Matching pairs")
    ExerciseType.HEAR_WORD -> tr("Hear the word")
    ExerciseType.HEAR_SENTENCE -> tr("Hear the sentence")
    ExerciseType.DICTATION -> tr("Dictation")
    ExerciseType.SHADOW -> tr("Shadowing")
    ExerciseType.BUILD -> tr("Build the sentence")
}

/** What the type asks, in a few words. */
fun ExerciseType.description(): String = when (this) {
    ExerciseType.GAP -> tr("Choose or type the word missing from the sentence")
    ExerciseType.MEANING -> tr("Pick what the marked word means")
    ExerciseType.MATCHING -> tr("Match up to five words with their meanings")
    ExerciseType.HEAR_WORD -> tr("Hear a word and choose its meaning, or pick it among words spelled like it")
    ExerciseType.HEAR_SENTENCE -> tr("Hear a sentence and find the word that means what is shown")
    ExerciseType.DICTATION -> tr("Hear the sentence and type the missing word, or all of a short one")
    ExerciseType.SHADOW -> tr("Hear a phrase and say it back")
    ExerciseType.BUILD -> tr("Put the sentence's words in order from its translation")
}

fun ExerciseType.icon(): ImageVector = when (this) {
    ExerciseType.GAP -> AppIcons.ViewList
    ExerciseType.MEANING -> AppIcons.Lightbulb
    ExerciseType.MATCHING -> AppIcons.SwapHoriz
    ExerciseType.HEAR_WORD -> AppIcons.VolumeUp
    ExerciseType.HEAR_SENTENCE -> AppIcons.Headphones
    ExerciseType.DICTATION -> AppIcons.RecordVoiceOver
    ExerciseType.SHADOW -> AppIcons.Mic
    ExerciseType.BUILD -> AppIcons.SwapVert
}

fun PracticeSkill.label(): String = when (this) {
    PracticeSkill.READING -> tr("Reading")
    PracticeSkill.LISTENING -> tr("Listening")
    PracticeSkill.TRANSLATION -> tr("Translation")
}

fun PracticeResponse.label(): String = when (this) {
    PracticeResponse.CHOOSE -> tr("Choose")
    PracticeResponse.TYPE -> tr("Type")
    PracticeResponse.SPEAK -> tr("Speak")
}

fun PracticeResponse.icon(): ImageVector = when (this) {
    PracticeResponse.CHOOSE -> AppIcons.ViewList
    PracticeResponse.TYPE -> Icons.Default.Edit
    PracticeResponse.SPEAK -> AppIcons.Mic
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
    PracticeNeed.MORE_WORDS -> tr("Needs four words with meanings")
    PracticeNeed.SENTENCE_LENGTH -> tr("Needs a sentence of 3 to 8 words")
}

