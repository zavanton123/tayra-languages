package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppSpacing

/**
 * A popup menu styled like the rest of the app: white, rounded, bordered, with compact rows. A long
 * one scrolls within [MENU_MAX_HEIGHT], with the scrollbar on the desktop.
 */
@Composable
fun AppMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = MENU_MIN_WIDTH),
        shape = AppShapes.control,
        containerColor = MaterialTheme.colorScheme.surface,
        shadowElevation = AppElevation.raised,
        border = AppBorders.hairline(),
    ) {
        ScrollColumn(Modifier.heightIn(max = MENU_MAX_HEIGHT), content = content)
    }
}

@Composable
fun AppMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = AppSpacing.l, vertical = AppSpacing.s),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.onSurface else colors.outline,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) { text() }
    }
}

private val MENU_MIN_WIDTH = 160.dp
private val MENU_MAX_HEIGHT = 420.dp
