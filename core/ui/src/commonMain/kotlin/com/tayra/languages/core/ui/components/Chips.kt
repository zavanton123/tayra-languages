package com.tayra.languages.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/*
 * The design system's small labels: tags, count badges, status pills, and the chips that choose
 * or remove a filter. Screens use these rather than tinted boxes of their own.
 */

/** How big a [Tag] or [StatusPill] is: [Small] inside rows and cards, [Medium] beside titles, [Large] on its own. */
enum class TagSize(internal val horizontal: Dp, internal val vertical: Dp) {
    Small(8.dp, 2.dp),
    Medium(10.dp, 3.dp),
    Large(12.dp, 6.dp),
}

/** How a [Tag] is coloured: [Soft] on a wash of its colour, [Solid] filled with it, [Neutral] grey whatever its colour. */
enum class TagStyle { Soft, Solid, Neutral }

@Composable
private fun TagSize.textStyle(): TextStyle = when (this) {
    TagSize.Small -> MaterialTheme.typography.labelMedium
    TagSize.Medium -> AppText.badge
    TagSize.Large -> MaterialTheme.typography.bodyMedium
}

/**
 * A short label on a tint, such as "Offline", "A2", "Sample" or a book's tag. [textColor] is for
 * a [TagStyle.Solid] tag on a colour that the primary's text colour does not suit.
 */
@Composable
fun Tag(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    size: TagSize = TagSize.Large,
    style: TagStyle = TagStyle.Soft,
    shape: Shape = AppShapes.pill,
    icon: ImageVector? = null,
    textColor: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (style) {
        TagStyle.Soft -> color.copy(alpha = AppAlpha.soft) to color
        TagStyle.Solid -> color to colors.onPrimary
        TagStyle.Neutral -> colors.onSurface.copy(alpha = AppAlpha.wash) to colors.onSurfaceVariant
    }
    Row(
        modifier.clip(shape).background(background).padding(horizontal = size.horizontal, vertical = size.vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = textColor ?: content, modifier = Modifier.size(if (size == TagSize.Large) AppSizes.iconSmall else AppSizes.iconTiny))
        Text(text, style = size.textStyle(), color = textColor ?: content, maxLines = 1, softWrap = false)
    }
}

/** How a [CountBadge] stands out: [Muted] beside an idle label, [Soft] beside a chosen one, [Solid] on its own, [OnColor] on a filled button or tab. */
enum class BadgeEmphasis { Muted, Soft, Solid, OnColor }

/** A number in a bubble beside a label: a tab's count, the filters that are on, cards due. Counts above [max] show as "[max]+". */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, emphasis: BadgeEmphasis = BadgeEmphasis.Muted, max: Int = 999) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (emphasis) {
        BadgeEmphasis.Muted -> colors.onSurface.copy(alpha = AppAlpha.wash) to colors.onSurfaceVariant
        BadgeEmphasis.Soft -> colors.primary.copy(alpha = AppAlpha.soft) to colors.primary
        BadgeEmphasis.Solid -> colors.primary to colors.onPrimary
        BadgeEmphasis.OnColor -> colors.onPrimary.copy(alpha = AppAlpha.line) to colors.onPrimary
    }
    Text(
        if (count > max) "${formatCount(max)}+" else formatCount(count),
        modifier.defaultMinSize(minWidth = BADGE_MIN_WIDTH).clip(AppShapes.pill).background(background).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs),
        style = AppText.badge,
        color = content,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** A coloured dot, for a status or a chart's legend. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = AppSizes.dotLarge) {
    Box(modifier.size(size).clip(AppShapes.circle).background(color))
}

/**
 * A rounded label in an outline of its colour, such as "Offline translation ready": a [dot] of
 * the colour, or a bold [value] ("12 due"), before the text.
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dot: Boolean = true,
    value: String? = null,
    size: TagSize = TagSize.Large,
    shape: Shape = AppShapes.pill,
) {
    val padding = when (size) {
        TagSize.Small -> PaddingValues(horizontal = AppSpacing.s, vertical = AppSpacing.xs)
        TagSize.Medium -> PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.xs)
        TagSize.Large -> PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.s)
    }
    val valueStyle = if (size == TagSize.Small) AppText.badge else AppText.labelStrong
    val textStyle = if (size == TagSize.Small) AppText.badge else AppText.label
    Row(
        modifier.clip(shape).background(color.copy(alpha = AppAlpha.tint)).border(AppBorders.hairline(color.copy(alpha = AppAlpha.line)), shape).padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (size == TagSize.Small) AppSpacing.xs else AppSpacing.s),
    ) {
        if (dot && value == null) StatusDot(color, size = if (size == TagSize.Small) AppSizes.dot else AppSizes.dotLarge)
        if (value != null) Text(value, style = valueStyle, color = color)
        Text(text, style = textStyle, maxLines = 1)
    }
}

/** How tall a [SelectableChip] is. */
enum class ChipSize(internal val height: Dp) { Small(36.dp), Regular(40.dp), Large(46.dp) }

/**
 * What goes before a [SelectableChip]'s label: nothing, a dot of its tint, a tick while it is chosen, or
 * [Toggle], for a chip that shows or hides something: a tick while on, a faded dot while off.
 */
enum class ChipLeading { None, Dot, Check, Toggle }

/**
 * A chip that is chosen or not, as a filter or a view: its label and an optional [count], washed
 * and outlined in its [tint] while [selected]; the text stays in the text colour, as some tints
 * are pale. [onRemove] adds a × that removes it.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    count: Int? = null,
    leading: ChipLeading = ChipLeading.None,
    size: ChipSize = ChipSize.Regular,
    enabled: Boolean = true,
    onRemove: (() -> Unit)? = null,
    removeDescription: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.pill
    val fill by animateColorAsState(if (selected) tint.copy(alpha = AppAlpha.soft) else colors.surface, AppMotion.spec(AppMotion.short))
    val edge by animateColorAsState(if (selected) tint else colors.outlineVariant, AppMotion.spec(AppMotion.short))
    Row(
        modifier.height(size.height).clip(shape)
            .background(fill)
            .border(AppBorders.hairline(edge), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = AppSpacing.m, end = if (onRemove != null) AppSpacing.xs else AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        when {
            leading == ChipLeading.Dot -> StatusDot(tint)
            (leading == ChipLeading.Check || leading == ChipLeading.Toggle) && selected -> Icon(Icons.Default.Check, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconSmall))
            leading == ChipLeading.Toggle -> StatusDot(tint.copy(alpha = AppAlpha.disabled))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
        )
        if (count != null) Text(formatCount(count), style = AppText.labelStrong, color = if (selected) colors.onSurface else colors.onSurfaceVariant)
        if (onRemove != null) AppIconButton(Icons.Default.Close, contentDescription = removeDescription, onClick = onRemove, size = IconButtonSize.Small, tint = if (selected) tint else null)
    }
}

/** A chosen filter or value with a × that removes it; [tint] colours it, or it is neutral. */
@Composable
fun RemovableChip(
    onRemove: () -> Unit,
    removeDescription: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    background: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.compact
    Row(
        modifier.height(REMOVABLE_CHIP_HEIGHT).clip(shape)
            .background(background ?: tint?.copy(alpha = AppAlpha.tint) ?: colors.surfaceContainerLow)
            .border(AppBorders.hairline(tint?.copy(alpha = AppAlpha.line) ?: colors.outlineVariant), shape)
            .padding(start = AppSpacing.m, end = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        content()
        AppIconButton(Icons.Default.Close, contentDescription = removeDescription, onClick = onRemove, size = IconButtonSize.Small)
    }
}

private val BADGE_MIN_WIDTH = 20.dp
private val REMOVABLE_CHIP_HEIGHT = 34.dp
