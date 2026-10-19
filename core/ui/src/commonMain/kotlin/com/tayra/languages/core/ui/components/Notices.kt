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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing

/*
 * The design system's notices: a one-line banner, a notice with a title and an action, and the
 * tinted surface they are drawn on, for anything else that a colour should set apart.
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
 * A tinted note with an icon, such as where downloads go or what a list holds. [dense] is the
 * lighter one inside a card: smaller, without an outline.
 */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector = Icons.Default.Info,
    dense: Boolean = false,
) {
    val shape = if (dense) AppShapes.control else AppShapes.panel
    Row(
        modifier.fillMaxWidth().clip(shape).background(tint.copy(alpha = if (dense) AppAlpha.tint else AppAlpha.wash))
            .then(if (dense) Modifier else Modifier.border(AppBorders.hairline(tint.copy(alpha = AppAlpha.line)), shape))
            .padding(horizontal = if (dense) AppSpacing.m else AppSpacing.l, vertical = if (dense) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (dense) AppSizes.icon else AppSizes.iconMedium))
        Spacer(Modifier.width(AppSpacing.m))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (dense) MaterialTheme.colorScheme.onSurface else tint)
    }
}

/**
 * A notice with a title: an engine's state, a result, a warning before an action. Its [icon], or a
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
    action: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val compact = LocalWindowWidth.current.isCompact
    TintedSurface(tint, modifier, padding = PaddingValues(if (compact) 14.dp else AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                busy -> AppSpinner(size = SpinnerSize.Medium, color = tint)
                icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconXLarge))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (action != null && !compact) action()
        }
        content?.invoke(this)
        if (action != null && compact) action()
    }
}
