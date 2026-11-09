package com.tayra.languages.feature.reading

import com.tayra.languages.core.ui.theme.AppText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.tayra.languages.core.domain.model.ReadingStage
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.BadgeEmphasis
import com.tayra.languages.core.ui.components.CountBadge
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing

internal val ReadingStage.icon: ImageVector
    get() = when (this) {
        ReadingStage.READ_ALOUD -> AppIcons.MenuBook
        ReadingStage.LISTEN -> AppIcons.Headphones
        ReadingStage.WORDS -> AppIcons.ViewList
        ReadingStage.QUIZ -> AppIcons.School
        ReadingStage.FLASHCARDS -> AppIcons.Style
        ReadingStage.REVERSE -> AppIcons.SwapHoriz
        ReadingStage.RETELL -> AppIcons.RecordVoiceOver
    }

@Composable
internal fun ReadingStage.title(): String = when (this) {
    ReadingStage.READ_ALOUD -> tr("Read aloud")
    ReadingStage.LISTEN -> tr("Listen")
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

/** Whether working on the stage hides the original text behind a blur. */
internal val ReadingStage.blursText: Boolean
    get() = this == ReadingStage.LISTEN || this == ReadingStage.REVERSE

/**
 * The page's stages in order, joined by a line: a checked one is ringed green with a tick, the one
 * being worked on ringed in the accent. [badges] counts what a stage still has to do.
 */
@Composable
internal fun StageChain(
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
        ReadingStage.entries.forEachIndexed { index, stage ->
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

/** Checks the stage being worked on, which moves on to the next: filled once the stage is [done], outlined until then. */
@Composable
internal fun CheckStageButton(stage: ReadingStage, done: Boolean, onCheck: () -> Unit, size: Dp = CHECK_SIZE) {
    val colors = MaterialTheme.colorScheme
    val label = tr("Check: {0}", stage.title())
    val doneLabel = tr("Done")
    AppTooltip(label) {
        Box(
            Modifier.size(size).shadow(AppElevation.raised, AppShapes.circle).clip(AppShapes.circle)
                .background(if (done) colors.primary else colors.surface)
                .then(if (done) Modifier else Modifier.border(BorderStroke(DONE_RING, colors.primary), AppShapes.circle))
                .clickable(onClick = onCheck)
                .testTag("check-stage")
                .semantics {
                    contentDescription = label
                    if (done) stateDescription = doneLabel
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Check, contentDescription = null, tint = if (done) colors.onPrimary else colors.primary, modifier = Modifier.size(if (size < CHECK_SIZE) AppSizes.iconMedium else AppSizes.iconLarge)) }
    }
}

/**
 * Above the text: the stage being worked on and what to do in it, beside the button that opens a
 * stage done in a dialog. Its place in the order is the chain's to show.
 */
@Composable
internal fun StageNote(stage: ReadingStage, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val number = tr("Stage {0} of {1}: {2}", stage.ordinal + 1, ReadingStage.entries.size, stage.title())
    Row(
        modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.faint))
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("stage-note"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Icon(stage.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(stage.title(), style = AppText.itemTitle, modifier = Modifier.semantics { contentDescription = number })
            Text(stage.hint(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        stage.action()?.let { label -> SecondaryButton(label, onClick = onAction, modifier = Modifier.testTag("stage-action")) }
    }
}

internal val STAGE_SIZE = 52.dp
internal val COMPACT_STAGE_SIZE = 36.dp
internal val CHECK_SIZE = 60.dp
internal val COMPACT_CHECK_SIZE = 44.dp
private val CONNECTOR_THICKNESS = 2.dp
private val CURRENT_RING = 3.dp
private val DONE_RING = 2.dp
