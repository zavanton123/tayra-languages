package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding

enum class WindowWidth { COMPACT, MEDIUM, EXPANDED;
    val isCompact: Boolean get() = this == COMPACT
    val isExpanded: Boolean get() = this == EXPANDED

    companion object {
        fun of(width: Dp): WindowWidth = when {
            width < 600.dp -> COMPACT
            width < 900.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

val LocalWindowWidth = compositionLocalOf { WindowWidth.EXPANDED }

/** Provides [LocalWindowWidth] based on the available width. */
@Composable
fun ProvideWindowWidth(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalWindowWidth provides WindowWidth.of(maxWidth), content = content)
    }
}

val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorMessage(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Column(modifier.padding(vertical = 4.dp)) {
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
