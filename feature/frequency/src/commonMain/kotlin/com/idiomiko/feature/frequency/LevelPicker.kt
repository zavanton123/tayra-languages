package com.idiomiko.feature.frequency

import com.idiomiko.core.ui.components.StatusTints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.frequency.LevelEdge
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.tint

/**
 * The levels to choose from, as cards with the [selected] one ringed: one row of them where they fit at a
 * readable width, else two rows; [compact], for a phone, one level to a row with its badge on the left.
 * The [current] level, the one set, says so on its card.
 */
@Composable
internal fun LevelPicker(edges: List<LevelEdge>, selected: LevelEdge?, onPick: (Int) -> Unit, compact: Boolean, modifier: Modifier = Modifier, current: LevelEdge? = null) {
    if (compact) {
        Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            edges.forEach { edge -> LevelCard(edge, edge == selected, edge == current, { onPick(edge.words) }, Modifier.fillMaxWidth(), compact = true) }
        }
        return
    }
    BoxWithConstraints(modifier) {
        val gap = AppSpacing.m
        val perRow = if (fitsOneRow(maxWidth, edges.size, gap)) edges.size else (edges.size + 1) / 2
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            edges.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { edge -> LevelCard(edge, edge == selected, edge == current, { onPick(edge.words) }, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun fitsOneRow(width: Dp, cards: Int, gap: Dp) = width >= CARD_MIN_WIDTH * cards + gap * (cards - 1)

/** The level's short name: its code, or Starter. */
@Composable
internal fun LevelEdge.code(): String = level?.code ?: tr("Starter")

@Composable
private fun LevelEdge.name(): String = level?.let { tr(it.label) } ?: tr("I'm just beginning")

/** The level's colour, as the courses show it; Starter has the theme's. */
@Composable
private fun LevelEdge.color(): Color = level?.tint ?: MaterialTheme.colorScheme.primary

/**
 * A level to choose: its badge, its code and its name, stacked, or in a row when [compact]. The chosen
 * one has a ring, a wash and a check, in its corner or at the row's end; the [current] one is labelled so.
 */
@Composable
private fun LevelCard(edge: LevelEdge, selected: Boolean, current: Boolean, onClick: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    Box(
        modifier.clip(shape)
            .border(if (selected) AppBorders.chosen() else AppBorders.hairline(), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("level-card-${edge.level?.code ?: "Starter"}"),
    ) {
        if (compact) {
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.m, vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                LevelBadge(edge, AppSizes.tileLarge)
                Column(Modifier.weight(1f)) {
                    Text(edge.code(), style = AppText.itemTitleBold, maxLines = 1)
                    Text(edge.name(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (current) CurrentLabel()
                if (selected) ChosenCheck()
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.s, vertical = AppSpacing.l), horizontalAlignment = Alignment.CenterHorizontally) {
                LevelBadge(edge, AppSizes.tileXLarge)
                Spacer(Modifier.height(AppSpacing.m))
                Text(edge.code(), style = AppText.itemTitleBold, maxLines = 1)
                Text(edge.name(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, minLines = 2)
                if (current) {
                    Spacer(Modifier.height(AppSpacing.s))
                    CurrentLabel()
                }
            }
            if (selected) ChosenCheck(Modifier.align(Alignment.TopEnd).padding(AppSpacing.s))
        }
    }
}

/** "Current" on the card of the level set. */
@Composable
private fun CurrentLabel() {
    val colors = MaterialTheme.colorScheme
    Text(
        tr("Current"),
        Modifier.clip(AppShapes.pill).background(colors.primary.copy(alpha = AppAlpha.tint)).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs),
        style = AppText.badgeBold,
        color = colors.primary,
        maxLines = 1,
    )
}

/** The small filled check on the chosen card, green as the app's other ticks. */
@Composable
private fun ChosenCheck(modifier: Modifier = Modifier) {
    Box(modifier.size(AppSizes.icon).clip(AppShapes.circle).background(StatusTints.ok), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconTiny))
    }
}

/** The level's code on a rounded square of its colour; Starter's reads Starter, smaller on a smaller badge. */
@Composable
private fun LevelBadge(edge: LevelEdge, size: Dp) {
    val starter = edge.level == null
    val starterStyle = if (size < AppSizes.tileXLarge) AppText.badgeBold.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize) else AppText.badgeBold
    Box(Modifier.size(size).clip(AppShapes.card).background(edge.color()), contentAlignment = Alignment.Center) {
        Text(
            edge.code(),
            style = if (starter) starterStyle else AppText.sectionTitle,
            color = if (starter) MaterialTheme.colorScheme.onPrimary else AppAccents.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** What choosing [edge] does: which of the most common words start as known. */
@Composable
internal fun ChosenLevelStrip(edge: LevelEdge) {
    val what = if (edge.words == 0) tr("We'll begin with the basics and mark no words as already known.")
    else trPlural(edge.words, "We'll mark the {1} most common word as known.", "We'll mark the {1} most common words as known.", formatCount(edge.words))
    LevelStrip(tr("{0} level selected", edge.code()), what)
}

/** A line under the level cards: [title] in bold, then [text]. */
@Composable
internal fun LevelStrip(title: String, text: String, icon: ImageVector = Icons.Default.CheckCircle) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("chosen-level"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
        Text(
            buildAnnotatedString {
                withStyle(AppText.labelBold.toSpanStyle()) { append(title) }
                append("  ")
                append(text)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
    }
}

/** The narrowest a level card gets in one row before the cards take two rows: "Intermediate" on one line. */
private val CARD_MIN_WIDTH = 120.dp
