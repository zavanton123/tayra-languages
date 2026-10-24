package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** Colours of status pills, tags and banners, readable on light and dark themes alike. */
object StatusTints {
    val ok = AppAccents.green
    val warning = AppAccents.amber
}

/** The screen's name as a heading, on wide windows only: phones show it in the top bar instead. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    if (LocalWindowWidth.current.isCompact) return
    Text(title, modifier, style = AppText.screenTitle)
}

/** A step of a [ScreenHeader]'s trail back, such as "Vocabulary" above a term; [onClick] goes there. */
class Crumb(val label: String, val onClick: (() -> Unit)? = null)

/**
 * The page heading: an optional way back (to Settings, or along [crumbs], whose back button takes
 * [onBack] or else the last crumb that acts), the title and subtitle, and actions on the right. On a phone the bar names the screen, so only the subtitle and actions
 * show, unless [titleOnCompact] keeps a title the bar does not give.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String?,
    onBackToSettings: (() -> Unit)? = null,
    crumbs: List<Crumb> = emptyList(),
    titleOnCompact: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val compact = LocalWindowWidth.current.isCompact
    val colors = MaterialTheme.colorScheme
    // A phone's bar holds the title; a subtitle alone moves up under it, so it reads as the title's and not the content's.
    val underBar = compact && !titleOnCompact && crumbs.isEmpty() && subtitle != null
    Column(Modifier.fillMaxWidth().then(if (underBar) Modifier.pullUp(AppSpacing.l) else Modifier)) {
        if (onBackToSettings != null && !compact) {
            Row(
                Modifier.clip(AppShapes.compact).clickable(onClick = onBackToSettings).padding(vertical = AppSpacing.xs, horizontal = AppSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(AppSizes.iconCompact), tint = colors.onSurfaceVariant)
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("Settings"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(AppSpacing.s))
        }
        if (crumbs.isNotEmpty()) {
            val back = onBack ?: crumbs.lastOrNull { it.onClick != null }?.onClick
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                if (back != null) AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = back, size = IconButtonSize.Small, style = IconButtonStyle.Outlined)
                crumbs.forEachIndexed { i, crumb ->
                    if (i > 0) Text("/", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                    val onClick = crumb.onClick
                    if (onClick != null) LinkText(crumb.label, onClick = onClick, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    else Text(crumb.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(AppSpacing.s))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Column(Modifier.weight(1f)) {
                if (!compact) Text(title, style = AppText.pageTitle)
                else if (titleOnCompact) Text(title, style = AppText.headline)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            actions()
        }
    }
}

/** How big a [CardHeader] is: [Dense] on phone cards, [Card] on cards, [Large] heading a column or a page's summary. */
enum class CardHeaderSize(internal val tile: Int, internal val gap: Int) { Dense(44, 14), Card(48, 16), Large(56, 16) }

/**
 * The head of a card or a section: an icon tile (or a short text on it) washed with [tint], the
 * title with [titleExtra] beside it, an optional subtitle, and [actions] at the end.
 */
@Composable
fun CardHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconText: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: CardHeaderSize = CardHeaderSize.Card,
    titleExtra: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null || iconText != null) {
            IconTile(icon, iconText, size = size.tile, tint = tint)
            Spacer(Modifier.width(size.gap.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(title, style = if (size == CardHeaderSize.Large) AppText.headline else AppText.sectionTitle)
                titleExtra()
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

/**
 * The head of a side pane or panel: a way back or an [icon], the title and subtitle, and a close
 * button. The caller gives its padding.
 */
@Composable
fun PaneHeader(
    title: String,
    subtitle: String?,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    icon: ImageVector? = null,
    closeModifier: Modifier = Modifier,
    closeDescription: String = tr("Close"),
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        when {
            onBack != null -> AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack, style = IconButtonStyle.Tonal, tint = colors.onSurface, shape = AppShapes.control)
            icon != null -> Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onClose != null) AppIconButton(Icons.Default.Close, contentDescription = closeDescription, onClick = onClose, modifier = closeModifier, style = IconButtonStyle.Tonal, tint = colors.onSurface)
    }
}

/**
 * A section's title within a page, with [actions] at the end; [tint] draws it as a band of that
 * colour, as a level's heading in a list.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth()
            .then(if (tint != null) Modifier.clip(AppShapes.control).background(tint.copy(alpha = AppAlpha.tint)).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.sectionTitle, color = tint ?: MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

/**
 * A card with an icon tile, a title and an optional subtitle, and its content below. [dense] is
 * the phone's card: less padding, a smaller tile and less room under the header.
 */
@Composable
fun ContentCard(
    title: String,
    subtitle: String?,
    icon: ImageVector? = null,
    iconText: String? = null,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    headerExtra: @Composable RowScope.() -> Unit = {},
    titleExtra: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().cardSurface().padding(if (dense) AppSpacing.l else AppSpacing.xl)) {
        CardHeader(title, subtitle, icon = icon, iconText = iconText, size = if (dense) CardHeaderSize.Dense else CardHeaderSize.Card, titleExtra = titleExtra, actions = headerExtra)
        Spacer(Modifier.height(if (dense) AppSpacing.m else AppSpacing.l))
        content()
    }
}


/**
 * A figure on a card: its [label], the [value] large, and a [detail] under it, with an [icon] beside
 * them where there is room. The text starts at the top, so tiles side by side line up, and a label
 * takes two lines in large text rather than being cut.
 */
@Composable
fun StatTile(label: String, value: String, detail: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    Row(
        modifier.cardSurface(AppShapes.card).padding(if (compact) AppSpacing.l else AppSpacing.xl),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        if (!compact) IconTile(icon, size = 52, tint = tint)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(value, style = AppText.headline, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** An icon (or a short text) on a rounded square washed with its [tint], the primary colour unless an accent is given. */
@Composable
fun IconTile(icon: ImageVector?, text: String? = null, size: Int = 48, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier.size(size.dp).clip(AppShapes.panel).background(tint.copy(alpha = AppAlpha.tint)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
        else if (text != null) Text(text, style = AppText.sectionTitle, color = tint)
    }
}

/** A whole number with minus and plus buttons on either side. */
@Composable
fun NumberStepper(value: Int, range: IntRange, name: String, step: Int = 1, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", tr("Decrease {0}", name), enabled = value > range.first, bordered = false) { onChange((value - step).coerceAtLeast(range.first)) }
        AppVerticalDivider(Modifier.height(AppSizes.control))
        Text(value.toString(), Modifier.widthIn(min = STEPPER_VALUE_MIN_WIDTH).padding(horizontal = AppSpacing.m), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        AppVerticalDivider(Modifier.height(AppSizes.control))
        StepButton("+", tr("Increase {0}", name), enabled = value < range.last, bordered = false) { onChange((value + step).coerceAtMost(range.last)) }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, bordered: Boolean = true, onClick: () -> Unit) {
    val shape = AppShapes.compact
    Box(
        Modifier.size(AppSizes.control).clip(shape)
            .then(if (bordered) Modifier.border(AppBorders.hairline(), shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (symbol == "+") {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(AppSizes.iconCompact), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else AppAlpha.disabled))
        } else {
            Text(
                symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else AppAlpha.disabled),
            )
        }
    }
}

/** The outlined button with an icon used for page actions. */
@Composable
fun HeaderButton(text: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    SecondaryButton(text, onClick = onClick, icon = icon, enabled = enabled)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> tr("{0} GB", (bytes / 10_000_000) / 100.0)
    bytes >= 1_000_000 -> tr("{0} MB", (bytes / 100_000) / 10.0)
    bytes >= 1_000 -> tr("{0} kB", bytes / 1_000)
    else -> tr("{0} B", bytes)
}

/** The scrolling page body, centred and kept to a readable width on large windows. */
@Composable
fun PageColumn(padding: PaddingValues, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    val scroll = rememberScrollState()
    Box(modifier.padding(padding).fillMaxSize()) {
        Box(Modifier.fillMaxSize().verticalScroll(scroll), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = AppSizes.pageWidth).fillMaxWidth()
                    .padding(horizontal = if (compact) AppSpacing.l else AppSpacing.giant, vertical = if (compact) AppSpacing.l else AppSpacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
                content = content,
            )
        }
        EdgeScrollbar(scroll)
    }
}

/** [label] is English, shown through `tr`. */
enum class PackageTab(val label: String) { INSTALLED("Downloaded"), AVAILABLE("Available") }

/** The Downloaded / Available switch above a list of downloads; [fill] spreads it across a phone's width. */
@Composable
fun TabToggle(selected: PackageTab, fill: Boolean = false, onSelect: (PackageTab) -> Unit) {
    SegmentedToggle(PackageTab.entries.map { Segment(it, label = tr(it.label)) }, selected, onSelect, fill = fill)
}

private val codeTints = AppAccents.sequence.take(7)

/** A language code on a square tinted per code, as on download tiles. */
@Composable
fun CodeTile(languageCode: String, size: Int = 44) {
    val code = languageCode.uppercase().take(3)
    val tint = codeTints[(code.hashCode() and Int.MAX_VALUE) % codeTints.size]
    Box(Modifier.size(size.dp).clip(AppShapes.compact).background(tint.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
        Text(code, color = AppAccents.onWash(tint), style = AppText.badge)
    }
}

private val STEPPER_VALUE_MIN_WIDTH = 64.dp

/** Draws the content [by] higher and takes that much less room, closing a gap above it that its parent leaves. */
private fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val pull = by.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - pull) { placeable.place(0, -pull) }
}

