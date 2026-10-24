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
    ExerciseType.MATCHING -> tr("Matching pairs")
    ExerciseType.TRUE_FALSE -> tr("True or false")
    ExerciseType.HEAR_WORD -> tr("Hear the word")
    ExerciseType.HEAR_SENTENCE -> tr("Hear the sentence")
    ExerciseType.DICTATION -> tr("Dictation")
    ExerciseType.SOUND_ALIKE -> tr("Sound-alike")
    ExerciseType.TRANSLATION -> tr("From the translation")
    ExerciseType.BUILD -> tr("Build the sentence")
    ExerciseType.FORM -> tr("Right form")
}

/** What the type asks, in a few words. */
fun ExerciseType.description(): String = when (this) {
    ExerciseType.GAP -> tr("Choose or type the word missing from the sentence")
    ExerciseType.MEANING -> tr("Pick what the marked word means")
    ExerciseType.MATCHING -> tr("Match up to five words with their meanings")
    ExerciseType.TRUE_FALSE -> tr("Say whether a meaning is the word's")
    ExerciseType.HEAR_WORD -> tr("Hear a word and choose its meaning")
    ExerciseType.HEAR_SENTENCE -> tr("Hear a sentence and find the word that means what is shown")
    ExerciseType.DICTATION -> tr("Hear the sentence and type the missing word, or all of a short one")
    ExerciseType.SOUND_ALIKE -> tr("Hear a word and pick it among words spelled nearly like it")
    ExerciseType.TRANSLATION -> tr("Read what the sentence means and find the missing word")
    ExerciseType.BUILD -> tr("Put the sentence's words in order from its translation")
    ExerciseType.FORM -> tr("Find the form of the word that fits the sentence")
}

fun ExerciseType.icon(): ImageVector = when (this) {
    ExerciseType.GAP -> AppIcons.ViewList
    ExerciseType.MEANING -> AppIcons.Lightbulb
    ExerciseType.MATCHING -> AppIcons.SwapHoriz
    ExerciseType.TRUE_FALSE -> AppIcons.DoneAll
    ExerciseType.HEAR_WORD -> AppIcons.VolumeUp
    ExerciseType.HEAR_SENTENCE -> AppIcons.Headphones
    ExerciseType.DICTATION -> AppIcons.RecordVoiceOver
    ExerciseType.SOUND_ALIKE -> AppIcons.Speed
    ExerciseType.TRANSLATION -> AppIcons.Translate
    ExerciseType.BUILD -> AppIcons.SwapVert
    ExerciseType.FORM -> AppIcons.Abc
}

fun PracticeSkill.label(): String = when (this) {
    PracticeSkill.READING -> tr("Reading")
    PracticeSkill.LISTENING -> tr("Listening")
    PracticeSkill.TRANSLATION -> tr("Translation")
    PracticeSkill.WORD_FORMS -> tr("Word forms")
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
    PracticeNeed.WORD_FORMS -> tr("Needs other forms of the word")
    PracticeNeed.SENTENCE_LENGTH -> tr("Needs a sentence of 3 to 8 words")
}

fun PracticePreset.label(): String = when (this) {
    PracticePreset.QUICK_REVIEW -> tr("Quick review")
    PracticePreset.BALANCED -> tr("Balanced")
    PracticePreset.LISTENING -> tr("Listening")
    PracticePreset.WRITING -> tr("Writing")
}
