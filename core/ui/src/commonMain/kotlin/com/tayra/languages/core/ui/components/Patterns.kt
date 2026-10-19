package com.tayra.languages.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.math.roundToInt

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
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconHuge))
        Text(title, style = AppText.sectionTitle, textAlign = TextAlign.Center)
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
        modifier.fillMaxWidth().clip(AppShapes.panel).background(colors.primary.copy(alpha = AppAlpha.wash))
            .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.line)), AppShapes.panel)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, style = AppText.emphasis, modifier = Modifier.padding(end = AppSpacing.xs))
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
            modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(colors.surfaceVariant.copy(alpha = AppAlpha.half)).padding(padding),
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
        modifier.then(if (onClick != null) Modifier.clip(AppShapes.small).clickable(onClick = onClick) else Modifier).padding(vertical = AppSpacing.xs, horizontal = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(label, color = tint, style = AppText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.iconSmall), tint = tint)
    }
}

/** One choice of a [SegmentedToggle]: its icon, its label (or none, to show the icon alone), and its name for screen readers. */
class Segment<T>(val value: T, val icon: ImageVector? = null, val label: String? = null, val description: String? = label, val testTag: String? = null)

/** How a [SegmentedToggle] or [CountTabs] marks the chosen one: [Soft], on a wash of the primary colour, or [Solid], filled with it. */
enum class SegmentStyle { Soft, Solid }

/**
 * A choice of a few, side by side in an outline, the chosen one tinted: a list or grid view, a tab.
 * [fill] spreads it across the width in equal parts, as tabs on a phone.
 */
@Composable
fun <T> SegmentedToggle(
    segments: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppSizes.control,
    fill: Boolean = false,
    style: SegmentStyle = SegmentStyle.Soft,
) {
    val colors = MaterialTheme.colorScheme
    val solid = style == SegmentStyle.Solid
    // Where each segment sits, so the chosen one's highlight can glide to it from the one before.
    val places = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val chosen = segments.indexOfFirst { it.value == selected }
    Box(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier).height(height).cardSurface(AppShapes.control).padding(AppSpacing.xs)) {
        places[chosen]?.let { (left, width) ->
            val x by animateIntAsState(left, AppMotion.spec())
            val w by animateIntAsState(width, AppMotion.spec())
            Box(
                Modifier.offset { IntOffset(x, 0) }.width(with(LocalDensity.current) { w.toDp() }).fillMaxHeight().clip(AppShapes.compact)
                    .background(if (solid) colors.primary else colors.primary.copy(alpha = AppAlpha.soft)),
            )
        }
        Row(Modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            segments.forEachIndexed { index, segment ->
                val active = index == chosen
                val tint by animateColorAsState(
                    if (active) (if (solid) colors.onPrimary else colors.primary) else colors.onSurfaceVariant,
                    AppMotion.spec(AppMotion.short),
                )
                Row(
                    Modifier.then(if (fill) Modifier.weight(1f) else Modifier).fillMaxHeight()
                        .onPlaced { places[index] = it.positionInParent().x.roundToInt() to it.size.width }
                        .clip(AppShapes.compact)
                        // Until the segments are placed, the chosen one draws its own highlight.
                        .then(if (active && places[chosen] == null) Modifier.background(if (solid) colors.primary else colors.primary.copy(alpha = AppAlpha.soft)) else Modifier)
                        .clickable { onSelect(segment.value) }
                        .padding(horizontal = if (segment.label == null) AppSpacing.m else if (fill) AppSpacing.s else AppSpacing.l)
                        .then(if (segment.testTag != null) Modifier.testTag(segment.testTag) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.CenterHorizontally),
                ) {
                    if (segment.icon != null) {
                        Icon(segment.icon, contentDescription = if (segment.label == null) segment.description else null, tint = tint, modifier = Modifier.size(AppSizes.icon))
                    }
                    if (segment.label != null) {
                        Text(
                            segment.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tint,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** One of [CountTabs] or [UnderlineTabs]: its label, an optional count and icon, and a test tag. */
class Tab<T>(val value: T, val label: String, val count: Int? = null, val icon: ImageVector? = null, val testTag: String? = null)

/**
 * Tabs that each say how many they hold, such as All 6 · In progress 1 · Finished 4, side by side
 * and scrolling when they do not fit. [compact] is the phone's smaller row.
 */
@Composable
fun <T> CountTabs(
    tabs: List<Tab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentStyle = SegmentStyle.Soft,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val solid = style == SegmentStyle.Solid
    val scroll = rememberScrollState()
    Row(modifier.fadedEdges(scroll).horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(if (solid) AppSpacing.m else AppSpacing.s)) {
        tabs.forEach { tab ->
            val active = tab.value == selected
            val shape = if (solid) AppShapes.panel else AppShapes.control
            val fillColor by animateColorAsState(
                when {
                    active && solid -> colors.primary
                    active -> colors.primary.copy(alpha = AppAlpha.tint)
                    solid -> colors.surface
                    compact -> colors.onSurface.copy(alpha = AppAlpha.faint)
                    else -> Color.Transparent
                },
                AppMotion.spec(AppMotion.short),
            )
            Row(
                Modifier.height(if (solid) AppSizes.control else SEGMENT_HEIGHT).clip(shape)
                    .background(fillColor)
                    .then(if (solid) Modifier.border(AppBorders.hairline(if (active) colors.primary else colors.outlineVariant), shape) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(tab.value) }
                    .then(if (tab.testTag != null) Modifier.testTag(tab.testTag) else Modifier)
                    .padding(horizontal = if (solid) AppSpacing.xl else AppSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                val tint by animateColorAsState(if (active) (if (solid) colors.onPrimary else colors.primary) else colors.onSurface, AppMotion.spec(AppMotion.short))
                if (tab.icon != null) Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconCompact))
                Text(
                    tab.label,
                    style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1,
                )
                if (tab.count != null) {
                    CountBadge(tab.count, emphasis = if (!active) BadgeEmphasis.Muted else if (solid) BadgeEmphasis.OnColor else BadgeEmphasis.Soft)
                }
            }
        }
    }
}

/**
 * Tabs underlined while chosen: the top bar's sections, a pane's pages. [fill] spreads them across
 * the width; [tinted] also washes the chosen one.
 */
@Composable
fun <T> UnderlineTabs(
    tabs: List<Tab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    tinted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    // Where each tab sits, so the underline can glide to the chosen one.
    val places = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val chosen = tabs.indexOfFirst { it.value == selected }
    val underline = if (fill) TAB_UNDERLINE_FILL else TAB_UNDERLINE
    Box(modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)) {
        Row(Modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            tabs.forEachIndexed { index, tab ->
                val active = index == chosen
                val tint by animateColorAsState(if (active) colors.primary else colors.onSurfaceVariant, AppMotion.spec(AppMotion.short))
                val wash by animateColorAsState(if (active && tinted) colors.primary.copy(alpha = AppAlpha.tint) else Color.Transparent, AppMotion.spec(AppMotion.short))
                Column(
                    Modifier.then(if (fill) Modifier.weight(1f) else Modifier.width(IntrinsicSize.Max))
                        .onPlaced { places[index] = it.positionInParent().x.roundToInt() to it.size.width }
                        .clip(AppShapes.control)
                        .background(wash)
                        .clickable(role = Role.Tab) { onSelect(tab.value) }
                        .then(if (tab.testTag != null) Modifier.testTag(tab.testTag) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier.padding(horizontal = AppSpacing.m)).padding(vertical = AppSpacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.CenterHorizontally),
                    ) {
                        if (tab.icon != null) Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.icon))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active) FontWeight.SemiBold else if (fill) FontWeight.Normal else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (tab.count != null && tab.count > 0) CountBadge(tab.count, emphasis = BadgeEmphasis.Solid, max = 99)
                    }
                    Spacer(Modifier.height(underline))
                }
            }
        }
        places[chosen]?.let { (left, width) ->
            val x by animateIntAsState(left, AppMotion.spec())
            val w by animateIntAsState(width, AppMotion.spec())
            Box(Modifier.align(Alignment.BottomStart).offset { IntOffset(x, 0) }.width(with(LocalDensity.current) { w.toDp() }).height(underline).background(colors.primary))
        }
    }
}

/**
 * A search field: a magnifier, the query, a way to clear it, at the controls' height.
 * [onSearch], when given, runs from the keyboard's search key, for a search that is not live.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
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
            keyboardOptions = if (onSearch != null) KeyboardOptions(imeAction = ImeAction.Search) else KeyboardOptions.Default,
            keyboardActions = if (onSearch != null) KeyboardActions(onSearch = { onSearch() }) else KeyboardActions.Default,
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

private val SEGMENT_HEIGHT = 40.dp
private val TAB_UNDERLINE_FILL = 3.dp
private val TAB_UNDERLINE = 2.dp

/** Fades the content out towards an edge that more of it lies beyond, so a row that scrolls looks like it does. */
private fun Modifier.fadedEdges(state: ScrollState): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = FADED_EDGE.toPx().coerceAtMost(size.width / 2)
        // Only the mask's alpha counts, so any opaque colour does.
        val opaque = AppAccents.ink
        if (state.canScrollForward) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, opaque), startX = size.width - fade, endX = size.width), Offset(size.width - fade, 0f), Size(fade, size.height), blendMode = BlendMode.DstOut)
        }
        if (state.canScrollBackward) {
            drawRect(Brush.horizontalGradient(listOf(opaque, Color.Transparent), startX = 0f, endX = fade), Offset.Zero, Size(fade, size.height), blendMode = BlendMode.DstOut)
        }
    }

private val FADED_EDGE = 32.dp

