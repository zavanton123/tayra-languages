package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.i18n.tr

/*
 * How statuses are shown. Words keep all seven statuses; by default only Unknown, Learning and Known
 * are shown and chosen: the four learning levels as one, Learning, which flashcards move a word
 * through as it is reviewed, and Ignored not offered at all. The setting to show the levels brings
 * back the numbered statuses 1 to 4 and Ignored.
 */

/** Whether all the statuses are shown: the four learning levels one by one, and Ignored; otherwise Unknown, Learning and Known. */
val LocalLearningLevels = staticCompositionLocalOf { true }

/** Whether the learning levels are shown here. */
val learningLevels: Boolean @Composable get() = LocalLearningLevels.current

/** [statuses] as they are offered: all of them, or the learning levels as one, standing for them all as level 1, and no Ignored. */
@Composable
fun shownStatuses(statuses: List<TermStatus>): List<TermStatus> =
    if (learningLevels) statuses else statuses.filter { (!it.isLearning || it == TermStatus.NEW_1) && it != TermStatus.IGNORED }

/** A status's name: "Learning 2" with the levels shown, "Learning" without. */
@Composable
fun TermStatus.shownName(): String = if (isLearning && !learningLevels) tr("Learning") else tr(label)

/** A status's mark on a badge: the level's number with the levels shown, "L" for Learning without. */
@Composable
fun TermStatus.shownAbbreviation(): String = if (isLearning && !learningLevels) "L" else abbreviation

/** Whether this status, as offered, is [current]'s: Learning without the levels is every level. */
@Composable
fun TermStatus.shownAs(current: TermStatus): Boolean = this == current || (!learningLevels && isLearning && current.isLearning)

/**
 * The status a word at [current] gets when this one is chosen. Without the levels, Learning keeps a
 * word that is already being learned at its level, so choosing it again does not set the word back.
 */
@Composable
fun TermStatus.chosenFor(current: TermStatus): TermStatus = if (!learningLevels && isLearning && current.isLearning) current else this
