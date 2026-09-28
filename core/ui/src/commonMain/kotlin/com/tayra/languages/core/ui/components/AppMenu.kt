package com.tayra.languages.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A popup menu styled like the rest of the app: white, rounded, bordered, with compact rows. */
@Composable
fun AppMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 160.dp),
        shape = RoundedCornerShape(10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
fun AppMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.onSurface else colors.outline,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
        ) { text() }
    }
}
