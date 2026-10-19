package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.coroutines.delay

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

val ContentPadding = PaddingValues(horizontal = AppSpacing.l, vertical = AppSpacing.m)

/** Whether the reader has asked the system for large text, so tight rows give their words more room. */
val largeText: Boolean @Composable get() = LocalDensity.current.fontScale >= LARGE_TEXT_SCALE

private const val LARGE_TEXT_SCALE = 1.15f

/** A spinner while a screen loads; it shows only once the load has taken a moment, so a quick one flashes none. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(AppMotion.loadingDelay.toLong())
        waited = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (waited) Appear { AppSpinner(size = SpinnerSize.Large) }
    }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(AppSpacing.xxxl), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorMessage(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank()) return
    Column(modifier.padding(vertical = AppSpacing.xs)) {
        Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
