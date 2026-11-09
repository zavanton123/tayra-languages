package com.tayra.languages.feature.reading

import com.tayra.languages.core.ui.components.Confetti
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.courses.LessonSummary
import com.tayra.languages.core.domain.model.ReadingStage
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.math.roundToInt

/**
 * Shown once a lesson's last stage is checked, with confetti over the whole window: what the lesson came to, and the ways
 * on, to the course, back to the text, or to the next lesson.
 */
@Composable
internal fun LessonCompleteDialog(
    title: String,
    summary: LessonSummary,
    onClose: () -> Unit,
    onCourse: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppDialogSurface(onDismissRequest = onClose, modifier = Modifier.testTag("lesson-complete"), maxWidth = LESSON_COMPLETE_WIDTH) {
        Box {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                Box(Modifier.size(BADGE_SIZE).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconHuge))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(tr("Lesson complete"), style = AppText.pageTitle, textAlign = TextAlign.Center)
                    Text(title, style = AppText.headline, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text(tr("You completed all {0} stages.", ReadingStage.entries.size), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Stats(summary)
                if (summary.words > 0) {
                    TintedSurface(AppAccents.green, padding = PaddingValues(AppSpacing.l)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconXLarge))
                            Column {
                                Text(tr("{0} of {1} lesson words are now known", summary.known, summary.words), style = AppText.itemTitle)
                                Text(tr("Your vocabulary grew during this lesson."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                AppDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    LinkText(tr("Back to course"), onCourse, modifier = Modifier.testTag("lesson-complete-course"))
                    Spacer(Modifier.weight(1f))
                    SecondaryButton(tr("Review lesson"), onClick = onClose, modifier = Modifier.testTag("lesson-complete-review"))
                    if (summary.next != null) {
                        PrimaryButton(tr("Next lesson"), onClick = onNext, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.testTag("lesson-complete-next"))
                    }
                }
            }
            AppIconButton(Icons.Default.Close, tr("Close"), onClick = onClose, style = IconButtonStyle.Outlined, modifier = Modifier.align(Alignment.TopEnd).padding(AppSpacing.l))
        }
        Confetti()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stats(summary: LessonSummary) {
    val percent = if (summary.quizAsked > 0) (100f * summary.quizRight / summary.quizAsked).roundToInt() else null
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        maxItemsInEachRow = 3,
    ) {
        val cell = Modifier.weight(1f).widthIn(min = STAT_MIN_WIDTH)
        Stat(AppIcons.Book, summary.learning.toString(), tr("New words"), tr("Being learned now"), cell)
        Stat(AppIcons.Style, summary.flashcards.toString(), tr("Flashcards"), tr("Ready to review"), cell)
        Stat(
            AppIcons.School,
            if (percent != null) "${summary.quizRight} / ${summary.quizAsked}" else "—",
            tr("Quiz answers"),
            if (percent != null) tr("{0}% correct", percent) else tr("No answers yet"),
            cell,
        )
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String, note: String, modifier: Modifier) {
    Column(modifier.cardSurface().padding(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        IconTile(icon, size = 40)
        Spacer(Modifier.size(AppSpacing.s))
        Text(value, style = AppText.screenTitle)
        Text(label, style = AppText.itemTitle)
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val BADGE_SIZE = 88.dp
private val STAT_MIN_WIDTH = 160.dp
private val LESSON_COMPLETE_WIDTH = 900.dp
