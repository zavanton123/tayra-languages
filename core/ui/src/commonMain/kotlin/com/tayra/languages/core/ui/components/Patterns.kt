package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing

/*
 * The design system's larger patterns, which screens had each drawn their own way: the card's
 * surface, empty and notice states, the bar of a selection, a table's header, a segmented toggle,
 * a search field and a row's menu of actions.
 */

/** The surface of a card or panel: the theme's surface colour inside a hairline, in [shape]. */
@Composable
fun Modifier.cardSurface(shape: Shape = AppShapes.card, color: Color = MaterialTheme.colorScheme.surface): Modifier =
    clip(shape).background(color).border(AppBorders.hairline(), shape)

/** A plain card: its [content] on the card surface, with the cards' padding. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(AppSpacing.l),
    shape: Shape = AppShapes.card,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().cardSurface(shape).padding(padding), verticalArrangement = verticalArrangement, content = content)
}

/**
 * What a list shows when it has nothing to show, or a notice in place of content: an icon, a
 * title, an optional [message] and an optional [action], centred on a card.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (message != null) Text(message, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/**
 * The bar over a list while items are ticked: an optional [leading] control (a tick for all),
 * how many are chosen, and the [actions] that act on them, which wrap onto a second line when
 * they do not fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectionBar(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier.fillMaxWidth().clip(AppShapes.panel).background(colors.primary.copy(alpha = 0.06f))
            .border(AppBorders.hairline(colors.primary.copy(alpha = 0.25f)), AppShapes.panel)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = AppSpacing.xs))
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** A table's header: its [cells] on a tinted band across the top of the table, in the header's text style. */
@Composable
fun TableHeader(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(horizontal = AppSpacing.s, vertical = AppSpacing.xs), cells: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
        Row(
            modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)).padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            content = cells,
        )
    }
}

/** A column's name in a [TableHeader]; one that sorts the table has [onClick], and [active] while the table is sorted by it. */
@Composable
fun TableHeaderCell(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
    Row(
        modifier.then(if (onClick != null) Modifier.clip(AppShapes.small).clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp, horizontal = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(label, color = tint, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.iconSmall), tint = tint)
    }
}

/** One choice of a [SegmentedToggle]: its icon, its label (or none, to show the icon alone), and its name for screen readers. */
class Segment<T>(val value: T, val icon: ImageVector? = null, val label: String? = null, val description: String? = label, val testTag: String? = null)

/**
 * A choice of a few, side by side in an outline, the chosen one tinted: a list or grid view, a tab.
 * [fill] spreads it across the width in equal parts, as tabs on a phone.
 */
@Composable
fun <T> SegmentedToggle(segments: List<Segment<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, height: Dp = AppSizes.control, fill: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier).height(height).cardSurface(AppShapes.control).padding(AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        segments.forEach { segment ->
            val active = segment.value == selected
            val tint = if (active) colors.primary else colors.onSurfaceVariant
            Row(
                Modifier.then(if (fill) Modifier.weight(1f) else Modifier).fillMaxHeight().clip(AppShapes.compact)
                    .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(segment.value) }
                    .padding(horizontal = if (segment.label == null) AppSpacing.m else AppSpacing.l)
                    .then(if (segment.testTag != null) Modifier.testTag(segment.testTag) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.CenterHorizontally),
            ) {
                if (segment.icon != null) {
                    Icon(segment.icon, contentDescription = if (segment.label == null) segment.description else null, tint = tint, modifier = Modifier.size(AppSizes.icon))
                }
                if (segment.label != null) {
                    Text(segment.label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

/** A search field: a magnifier, the query, a way to clear it, at the controls' height. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(AppSizes.control).cardSurface(AppShapes.control).padding(start = AppSpacing.m, end = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
        Spacer(Modifier.width(AppSpacing.s))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = fieldModifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            AppIconButton(Icons.Default.Close, contentDescription = tr("Clear search"), onClick = { onValueChange("") }, size = IconButtonSize.Small)
        } else {
            Spacer(Modifier.width(AppSpacing.s))
        }
    }
}

/** A row's actions behind its ⋮ button: [items] are [AppMenuItem]s, given a way to close the menu. [buttonModifier] is the button's, for its test tag. */
@Composable
fun OverflowMenu(contentDescription: String, modifier: Modifier = Modifier, buttonModifier: Modifier = Modifier, items: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AppIconButton(Icons.Default.MoreVert, contentDescription = contentDescription, onClick = { open = true }, modifier = buttonModifier)
        AppMenu(expanded = open, onDismissRequest = { open = false }) { items { open = false } }
    }
}
