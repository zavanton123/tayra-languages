package com.tayra.languages.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes

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
        // Material grows a button to its touch size, and the outline and wash would grow with it.
        IconButtonStyle.Outlined -> CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            val s = shape ?: AppShapes.control
            IconButton(
                onClick = onClick,
                modifier = modifier.size(size.box).clip(s).background(if (selected) accent.copy(alpha = AppAlpha.soft) else colors.surface).border(AppBorders.hairline(), s),
                enabled = enabled,
                shape = s,
            ) { content(if (selected) accent else tint ?: colors.onSurface) }
        }
        IconButtonStyle.Tonal -> CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            val s = shape ?: AppShapes.circle
            IconButton(
                onClick = onClick,
                modifier = modifier.size(size.box).clip(s).background(accent.copy(alpha = if (selected) AppAlpha.line else AppAlpha.tint)),
                enabled = enabled,
                shape = s,
            ) { content(accent) }
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
