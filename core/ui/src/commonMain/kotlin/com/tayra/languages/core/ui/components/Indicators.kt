package com.tayra.languages.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing

/*
 * The design system's small controls and indicators: icon buttons, progress, spinners, dividers,
 * checkboxes and snackbars, each with one look across the app.
 */

/** How big an icon button is: its touch target and its icon. */
enum class IconButtonSize(internal val box: Dp, internal val icon: Dp) {
    Small(32.dp, 18.dp),
    Regular(40.dp, 22.dp),
    Large(44.dp, 24.dp),
}

/**
 * An icon button that draws its own [background] and [border] at its size, while on touch screens
 * its tap area reaches a finger's size around it (Material would grow the drawing with the area).
 */
@Composable
private fun DrawnIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: IconButtonSize,
    shape: Shape,
    background: Color,
    border: BorderStroke?,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .then(if (touchFirst) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clickable(interaction, ripple(bounded = false, radius = size.box / 2), enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size.box).clip(shape).background(background).then(if (border != null) Modifier.border(border, shape) else Modifier)
                .alpha(if (enabled) 1f else AppAlpha.disabled),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/**
 * How an icon button is drawn: [Plain]; [Filled] with the primary colour (a page's main action);
 * [Outlined] on the card surface; [Tonal] on a wash of its tint (a pane's close, a play button).
 */
enum class IconButtonStyle { Plain, Filled, Outlined, Tonal }

/**
 * A button that is only an icon; [contentDescription] says what it does to screen readers.
 * [selected] tints it while what it controls is on (a sound playing), [loading] shows a spinner in
 * place of the icon, and [shape] overrides the style's own.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: IconButtonSize = IconButtonSize.Regular,
    style: IconButtonStyle = IconButtonStyle.Plain,
    tint: Color? = null,
    shape: Shape? = null,
    selected: Boolean = false,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val accent = tint ?: colors.primary
    val content: @Composable (Color) -> Unit = { color ->
        if (loading) AppSpinner(size = SpinnerSize.Small, color = color)
        else Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size.icon), tint = color)
    }
    when (style) {
        IconButtonStyle.Filled -> FilledIconButton(onClick = onClick, modifier = modifier.size(size.box), enabled = enabled, shape = shape ?: AppShapes.pill) {
            content(tint ?: LocalContentColor.current)
        }
        IconButtonStyle.Outlined -> {
            val s = shape ?: AppShapes.control
            DrawnIconButton(onClick, modifier, enabled, size, s, if (selected) accent.copy(alpha = AppAlpha.soft) else colors.surface, AppBorders.hairline()) {
                content(if (selected) accent else tint ?: colors.onSurface)
            }
        }
        IconButtonStyle.Tonal -> DrawnIconButton(onClick, modifier, enabled, size, shape ?: AppShapes.circle, accent.copy(alpha = if (selected) AppAlpha.line else AppAlpha.tint), null) {
            content(accent)
        }
        IconButtonStyle.Plain -> IconButton(onClick = onClick, modifier = modifier.size(size.box), enabled = enabled, colors = IconButtonDefaults.iconButtonColors()) {
            content(if (selected) colors.primary else tint ?: colors.onSurfaceVariant)
        }
    }
}

/** An icon button whose [content] is more than one icon (an icon with a badge, say), in the plain style. */
@Composable
fun AppIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, size: IconButtonSize = IconButtonSize.Regular, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier.size(size.box), enabled = enabled, content = content)
}

/** A task's progress, from 0 to 1; null while it cannot tell, which animates. */
@Composable
fun AppProgressBar(progress: Float?, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    if (progress != null) {
        val shown by animateFloatAsState(progress.coerceIn(0f, 1f), AppMotion.spec())
        LinearProgressIndicator(progress = { shown }, modifier = modifier.fillMaxWidth(), color = color, trackColor = track, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
    } else {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth(), color = color, trackColor = track, strokeCap = StrokeCap.Round, gapSize = 0.dp)
    }
}

/** How much of something is done, as a bar of [color] over a rounded track: a book's reading, a course's lessons, the words known. */
@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp, trackColor: Color = MaterialTheme.colorScheme.surfaceVariant) {
    val done by animateFloatAsState(fraction.coerceIn(0f, 1f), AppMotion.spec())
    Box(modifier.fillMaxWidth().height(height).clip(AppShapes.tiny).background(trackColor)) {
        if (done > 0f) Box(Modifier.fillMaxWidth(done).fillMaxHeight().clip(AppShapes.tiny).background(color))
    }
}

/** How big a spinner is: inside a button or a row, in a card, or for a whole page. */
enum class SpinnerSize(internal val size: Dp, internal val stroke: Dp) {
    Small(16.dp, 2.dp),
    Medium(24.dp, 2.5.dp),
    Large(40.dp, 3.5.dp),
}

/** Something is under way and its progress is not known. */
@Composable
fun AppSpinner(modifier: Modifier = Modifier, size: SpinnerSize = SpinnerSize.Medium, color: Color = MaterialTheme.colorScheme.primary) {
    CircularProgressIndicator(modifier.size(size.size), color = color, strokeWidth = size.stroke)
}

/** The line between rows or sections, in the theme's outline colour. */
@Composable
fun AppDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outlineVariant) {
    HorizontalDivider(modifier, color = color)
}

/** The upright line between items side by side. */
@Composable
fun AppVerticalDivider(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outlineVariant) {
    VerticalDivider(modifier, color = color)
}

/** A tick box. */
@Composable
fun AppCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
}

/** A tick box for a group: ticked, unticked, or partly ticked when only some of the group are. */
@Composable
fun AppTriStateCheckbox(state: ToggleableState, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TriStateCheckbox(state = state, onClick = onClick, modifier = modifier, enabled = enabled)
}

/** A short message at the bottom of a screen, with an optional action such as OK or Undo. */
@Composable
fun AppSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Snackbar(
        modifier = modifier,
        shape = AppShapes.control,
        action = if (actionLabel != null && onAction != null) ({ QuietButton(actionLabel, onClick = onAction) }) else null,
    ) { Text(message) }
}

/**
 * Where content is still to come: a block in the placeholder grey, with a soft light crossing it
 * while it loads; still when motion is reduced.
 */
@Composable
fun Modifier.placeholder(shape: Shape = AppShapes.small): Modifier {
    val colors = MaterialTheme.colorScheme
    val base = colors.onSurface.copy(alpha = AppAlpha.wash)
    if (AppMotion.reduced) return clip(shape).background(base)
    val light = colors.surface.copy(alpha = AppAlpha.half)
    val crossing by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(AppMotion.shimmer, easing = LinearEasing)))
    return clip(shape).background(base).drawWithContent {
        drawContent()
        val band = size.width * SHIMMER_BAND
        val x = -band + (size.width + band * 2) * crossing
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, light, Color.Transparent), startX = x - band, endX = x + band))
    }
}

/** A line of text still to come, [fraction] of the width. */
@Composable
fun PlaceholderLine(fraction: Float = 1f, modifier: Modifier = Modifier, height: Dp = AppSizes.textLine) {
    Box(modifier.fillMaxWidth(fraction).height(height).placeholder())
}

/**
 * Cards still to come, while a list loads from the network: [count] cards, each a title and
 * [lines] of text, with a picture's square when [thumbnail]. Screen readers hear that it loads.
 */
@Composable
fun PlaceholderCards(count: Int = 3, modifier: Modifier = Modifier, lines: Int = 2, thumbnail: Boolean = false) {
    val loading = tr("Loading...")
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = loading }, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        repeat(count) { card ->
            Row(Modifier.fillMaxWidth().cardSurface().padding(AppSpacing.l), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                if (thumbnail) Box(Modifier.size(AppSizes.tileXLarge).placeholder(AppShapes.compact))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    // The lines end at different lengths, as text does.
                    PlaceholderLine(TITLE_LENGTHS[card % TITLE_LENGTHS.size], height = AppSizes.titleLine)
                    repeat(lines) { line -> PlaceholderLine(if (line == lines - 1) LAST_LINE else 1f) }
                }
            }
        }
    }
}

/** How wide the light crossing a placeholder is, as a part of its width. */
private const val SHIMMER_BAND = 0.35f
private val TITLE_LENGTHS = listOf(0.6f, 0.75f, 0.5f)
private const val LAST_LINE = 0.7f

/**
 * A one-line label in a space that cannot grow, such as a tab or a toolbar action: at its own size
 * when it fits, and only when it does not, a little smaller, then cut short.
 */
@Composable
fun FitText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified) {
    val size = style.fontSize.takeOrElse { MaterialTheme.typography.labelMedium.fontSize }
    BasicText(
        text,
        modifier,
        style = style.merge(color = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = size * FIT_TEXT_SMALLEST, maxFontSize = size),
    )
}

/** How far a [FitText] shrinks before it is cut short. */
private const val FIT_TEXT_SMALLEST = 0.8f
