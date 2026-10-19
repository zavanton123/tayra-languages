package com.tayra.languages.core.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppBorders
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

/** How an icon button is drawn: [Plain], [Filled] with the primary colour (a page's main action), or [Outlined] on the card surface. */
enum class IconButtonStyle { Plain, Filled, Outlined }

/** A button that is only an icon; [contentDescription] says what it does to screen readers. */
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
) {
    val colors = MaterialTheme.colorScheme
    when (style) {
        IconButtonStyle.Filled -> FilledIconButton(onClick = onClick, modifier = modifier.size(size.box), enabled = enabled, shape = AppShapes.pill) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size.icon), tint = tint ?: LocalContentColor.current)
        }
        IconButtonStyle.Outlined -> IconButton(
            onClick = onClick,
            modifier = modifier.size(size.box).clip(AppShapes.control).background(colors.surface).border(AppBorders.hairline(), AppShapes.control),
            enabled = enabled,
            shape = AppShapes.control,
        ) { Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size.icon), tint = tint ?: colors.onSurface) }
        IconButtonStyle.Plain -> IconButton(onClick = onClick, modifier = modifier.size(size.box), enabled = enabled, colors = IconButtonDefaults.iconButtonColors()) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size.icon), tint = tint ?: colors.onSurfaceVariant)
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
        LinearProgressIndicator(progress = { progress }, modifier = modifier.fillMaxWidth(), color = color, trackColor = track, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {})
    } else {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth(), color = color, trackColor = track, strokeCap = StrokeCap.Round, gapSize = 0.dp)
    }
}

/** How much of something is done, as a bar of [color] over a rounded track: a book's reading, a course's lessons, the words known. */
@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp, trackColor: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Box(modifier.fillMaxWidth().height(height).clip(AppShapes.tiny).background(trackColor)) {
        val done = fraction.coerceIn(0f, 1f)
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

/** A short message at the bottom of a screen, with an optional action such as OK or Undo. */
@Composable
fun AppSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Snackbar(
        modifier = modifier,
        shape = AppShapes.control,
        action = if (actionLabel != null && onAction != null) ({ QuietButton(actionLabel, onClick = onAction) }) else null,
    ) { Text(message) }
}
