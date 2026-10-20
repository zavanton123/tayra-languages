package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/*
 * The design system's notices: a one-line banner, a notice with a title and an action, and the
 * tinted surface they are drawn on, for anything else that a colour should set apart. Banners and
 * notices fade in as they appear.
 */

/** A panel washed with [tint] inside an outline of it: the ground of notices, and of a tinted toolbar or summary. */
@Composable
fun TintedSurface(
    tint: Color,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.panel,
    padding: PaddingValues = PaddingValues(AppSpacing.l),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppSpacing.m),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(shape).background(tint.copy(alpha = AppAlpha.wash)).border(AppBorders.hairline(tint.copy(alpha = AppAlpha.line)), shape).padding(padding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * The colour of a banner that only informs: the theme's primary, unless that is a warm orange or
 * red, which would read as a warning; then the app's blue, lifted on dark themes.
 */
@Composable
private fun infoTint(): Color {
    val primary = MaterialTheme.colorScheme.primary
    return if (primary.isWarm()) AppAccents.onWash(AppAccents.blue) else primary
}

/** Whether a colour is a bright red, orange or yellow, the hues warnings use. */
private fun Color.isWarm(): Boolean {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    // A dark brown, as Sepia's, is not a warning colour: only bright warm hues are.
    if (max - min < WARM_MIN_CHROMA || max < WARM_MIN_VALUE) return false
    val hue = when (max) {
        red -> 60f * (((green - blue) / (max - min)).mod(6f))
        green -> 60f * ((blue - red) / (max - min) + 2f)
        else -> 60f * ((red - green) / (max - min) + 4f)
    }
    return hue < WARM_HUE_END || hue > WARM_HUE_START
}

private const val WARM_MIN_CHROMA = 0.15f
private const val WARM_MIN_VALUE = 0.7f
private const val WARM_HUE_END = 65f
private const val WARM_HUE_START = 330f

/**
 * A tinted note with an icon, such as where downloads go or what a list holds. [dense] is the
 * lighter one inside a card: smaller, without an outline.
 */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = infoTint(),
    icon: ImageVector = Icons.Default.Info,
    dense: Boolean = false,
) {
    val shape = if (dense) AppShapes.control else AppShapes.panel
    Appear(modifier) { Row(
        Modifier.fillMaxWidth().clip(shape).background(tint.copy(alpha = if (dense) AppAlpha.tint else AppAlpha.wash))
            .then(if (dense) Modifier else Modifier.border(AppBorders.hairline(tint.copy(alpha = AppAlpha.line)), shape))
            .padding(horizontal = if (dense) AppSpacing.m else AppSpacing.l, vertical = if (dense) AppSpacing.m else AppSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (dense) AppSizes.icon else AppSizes.iconMedium))
        Spacer(Modifier.width(AppSpacing.m))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (dense) MaterialTheme.colorScheme.onSurface else tint)
    } }
}

/**
 * A notice with a title: an engine's state, a result, a warning before an action. Its [icon] (popping in when [popIcon]), or a
 * spinner while [busy], the [title] and an optional [message], more [content] below them, and an
 * [action] (a button) at the end; on phones the action goes below the text.
 */
@Composable
fun Notice(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = Icons.Default.Info,
    busy: Boolean = false,
    /** Whether the icon pops into place, for a result just earned. */
    popIcon: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val compact = LocalWindowWidth.current.isCompact
    Appear(modifier) { TintedSurface(tint, padding = PaddingValues(if (compact) AppSpacing.m else AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            when {
                busy -> AppSpinner(size = SpinnerSize.Medium, color = tint)
                icon != null && popIcon -> PopIn { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconXLarge)) }
                icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconXLarge))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(title, style = AppText.itemTitle)
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (action != null && !compact) action()
        }
        content?.invoke(this)
        if (action != null && compact) action()
    } }
}
