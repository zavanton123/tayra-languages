package com.idiomiko.feature.reading

import com.idiomiko.core.ui.theme.AppText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.model.ReadingStage
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.AppTooltip
import com.idiomiko.core.ui.components.BadgeEmphasis
import com.idiomiko.core.ui.components.CountBadge
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.LocalAppTheme
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppElevation
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing

internal val ReadingStage.icon: ImageVector
    get() = when (this) {
        ReadingStage.READ_ALOUD -> AppIcons.MenuBook
        ReadingStage.LISTEN -> AppIcons.StageListen
        ReadingStage.QUESTIONS -> AppIcons.Forum
        ReadingStage.WORDS -> AppIcons.StageWords
        ReadingStage.QUIZ -> AppIcons.ReadCourses
        ReadingStage.FLASHCARDS -> AppIcons.Flashcards
        ReadingStage.REVERSE -> AppIcons.StageReverse
        ReadingStage.RETELL -> AppIcons.StageRetell
    }

@Composable
internal fun ReadingStage.title(): String = when (this) {
    ReadingStage.READ_ALOUD -> tr("Read aloud")
    ReadingStage.LISTEN -> tr("Listen")
    ReadingStage.QUESTIONS -> tr("Questions")
    ReadingStage.WORDS -> tr("Check the words")
    ReadingStage.QUIZ -> tr("Quiz")
    ReadingStage.FLASHCARDS -> tr("Flashcards")
    ReadingStage.REVERSE -> tr("Reverse translation")
    ReadingStage.RETELL -> tr("Retell")
}

@Composable
private fun ReadingStage.hint(): String = when (this) {
    ReadingStage.READ_ALOUD -> tr("Read the text aloud and mark the words you do not know. Completing this stage marks the remaining words as known.")
    ReadingStage.LISTEN -> tr("Listen to the text with the play button. The original is blurred: point at a sentence to see it.")
    ReadingStage.QUESTIONS -> tr("Answer each question on the story, then point at the blurred answer to compare.")
    ReadingStage.WORDS -> tr("Go over the page's words and put their statuses right.")
    ReadingStage.QUIZ -> tr("Answer the questions on the words you are learning.")
    ReadingStage.FLASHCARDS -> tr("Review today's flashcards without leaving the text.")
    ReadingStage.REVERSE -> tr("Say each translated sentence in the original, then point at it to compare.")
    ReadingStage.RETELL -> tr("Tell the text again in the original, with only its translation in front of you.")
}

/** What opens a stage's own work, for the stages done in a dialog; null for those done on the page. */
@Composable
private fun ReadingStage.action(): String? = when (this) {
    ReadingStage.WORDS -> tr("Open the word list")
    ReadingStage.QUIZ -> tr("Start the quiz")
    ReadingStage.FLASHCARDS -> tr("Review the cards")
    else -> null
}

/** Whether the page has its player: when the read-aloud controls are on, and always while listening. */
internal val ReadingUiState.readsAloud: Boolean
    get() = settings.showSentencePlay || stage == ReadingStage.LISTEN

/** Whether working on the stage hides the original text behind a blur. */
internal val ReadingStage.blursText: Boolean
    get() = this == ReadingStage.LISTEN || this == ReadingStage.REVERSE

/**
 * The page's stages in order, joined by a line: a checked one is ringed green with a tick, the one
 * being worked on ringed in the accent. [badges] counts what a stage still has to do.
 */
@Composable
internal fun StageChain(
    stages: List<ReadingStage>,
    done: Set<ReadingStage>,
    current: ReadingStage,
    onSelect: (ReadingStage) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = STAGE_SIZE,
    badges: Map<ReadingStage, Int> = emptyMap(),
) {
    val colors = MaterialTheme.colorScheme
    val gap = if (size < STAGE_SIZE) AppSpacing.xs else AppSpacing.m
    Row(modifier.testTag("stage-chain"), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, stage ->
            if (index > 0) Box(Modifier.width(gap).height(CONNECTOR_THICKNESS).background(colors.outlineVariant))
            StageCircle(stage, stage in done, stage == current, badges[stage] ?: 0, size) { onSelect(stage) }
        }
    }
}

@Composable
private fun StageCircle(stage: ReadingStage, done: Boolean, current: Boolean, badge: Int, size: Dp, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val title = stage.title()
    val doneLabel = tr("Done")
    val ring = when {
        current -> BorderStroke(CURRENT_RING, colors.primary)
        done -> BorderStroke(DONE_RING, AppAccents.green)
        else -> AppBorders.selected(colors.outlineVariant)
    }
    val tint = when {
        current -> colors.primary
        done -> AppAccents.green
        else -> colors.onSurfaceVariant
    }
    AppTooltip(title) {
        Box {
            Box(
                Modifier.size(size).clip(AppShapes.circle)
                    .background(if (done && !current) AppAccents.green.copy(alpha = AppAlpha.tint) else colors.surface)
                    .border(ring, AppShapes.circle)
                    .clickable(onClick = onClick)
                    .testTag("stage-${stage.name.lowercase()}")
                    .semantics {
                        contentDescription = title
                        selected = current
                        if (done) stateDescription = doneLabel
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(stage.icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (size < STAGE_SIZE) AppSizes.iconSmall else AppSizes.iconMedium)) }
            val corner = Modifier.align(Alignment.TopEnd).offset(x = AppSpacing.xs, y = -AppSpacing.xs)
            when {
                done -> Box(corner.size(if (size < STAGE_SIZE) 14.dp else 20.dp).clip(AppShapes.circle).background(AppAccents.green), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.surface, modifier = Modifier.size(if (size < STAGE_SIZE) 10.dp else 14.dp))
                }
                badge > 0 -> CountBadge(badge, corner.testTag("stage-badge"), emphasis = BadgeEmphasis.Solid)
            }
        }
    }
}

/** Checks the stage being worked on, which moves on to the next; always filled green, whether or not the stage is [done]. */
@Composable
internal fun CheckStageButton(
    stage: ReadingStage,
    done: Boolean,
    onCheck: () -> Unit,
    size: Dp = CHECK_SIZE,
    /** What the stage waits for before it can be checked, said on hover; null when it can be. */
    waitingFor: String? = null,
) {
    val label = tr("Check: {0}", stage.title())
    val doneLabel = tr("Done")
    AppTooltip(waitingFor ?: label) {
        Box(
            Modifier.size(size).shadow(if (waitingFor == null) AppElevation.raised else 0.dp, AppShapes.circle).clip(AppShapes.circle)
                .background(if (waitingFor == null) LocalAppTheme.current.checkAccent else MaterialTheme.colorScheme.outlineVariant)
                .clickable(enabled = waitingFor == null, onClick = onCheck)
                .testTag("check-stage")
                .semantics {
                    contentDescription = label
                    if (done) stateDescription = doneLabel
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Check, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(if (size < CHECK_SIZE) AppSizes.iconMedium else AppSizes.iconLarge)) }
    }
}

/**
 * Above the text: the stage being worked on and what to do in it, beside the button that opens a
 * stage done in a dialog, and a cross that closes the note for this stage. Its place in the order is the chain's to show.
 */
@Composable
internal fun StageNote(stage: ReadingStage, stages: List<ReadingStage>, onAction: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = AppAccents.onWash(AppAccents.blue)
    val number = tr("Stage {0} of {1}: {2}", stages.indexOf(stage) + 1, stages.size, stage.title())
    // On a phone the action goes under the words, which would otherwise be squeezed to a few letters a line beside it.
    val compact = LocalWindowWidth.current.isCompact
    val action = stage.action()
    Row(
        modifier.fillMaxWidth().clip(AppShapes.card).background(AppAccents.blue.copy(alpha = AppAlpha.soft))
            .border(AppBorders.hairline(AppAccents.blue.copy(alpha = AppAlpha.line)), AppShapes.card)
            .padding(start = AppSpacing.xl, top = AppSpacing.l, bottom = AppSpacing.l, end = AppSpacing.s).testTag("stage-note"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Icon(stage.icon, contentDescription = null, tint = accent, modifier = Modifier.size(AppSizes.iconHuge))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(stage.title(), style = AppText.sectionTitle, modifier = Modifier.semantics { contentDescription = number })
            Text(stage.hint(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            if (compact && action != null) {
                Spacer(Modifier.height(AppSpacing.s))
                SecondaryButton(action, onClick = onAction, modifier = Modifier.testTag("stage-action"))
            }
        }
        if (!compact && action != null) SecondaryButton(action, onClick = onAction, modifier = Modifier.testTag("stage-action"))
        AppIconButton(Icons.Default.Close, tr("Close"), onClick = onClose, modifier = Modifier.align(Alignment.Top).testTag("stage-note-close"), size = IconButtonSize.Small, tint = colors.onSurfaceVariant)
    }
}

internal val STAGE_SIZE = 52.dp
internal val COMPACT_STAGE_SIZE = 36.dp

/** The smallest a phone's stage gets, still a fair target for a finger, enough for a narrow phone's eight stages. */
private val SMALLEST_STAGE_SIZE = 26.dp

/** The size at which [count] stages, with their connectors, fit in [width], from [COMPACT_STAGE_SIZE] down to [SMALLEST_STAGE_SIZE]. */
internal fun fittedStageSize(count: Int, width: Dp): Dp {
    if (count == 0) return COMPACT_STAGE_SIZE
    val connectors = AppSpacing.xs * (count - 1)
    return ((width - connectors) / count).coerceIn(SMALLEST_STAGE_SIZE, COMPACT_STAGE_SIZE)
}
internal val CHECK_SIZE = 60.dp
internal val COMPACT_CHECK_SIZE = 44.dp
private val CONNECTOR_THICKNESS = 2.dp
private val CURRENT_RING = 3.dp
private val DONE_RING = 2.dp
