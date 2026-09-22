package com.tayra.languages.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppTheme = staticCompositionLocalOf { AppThemes.default }

@Composable
fun TayraTheme(theme: AppTheme = AppThemes.default, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = theme.colorScheme, content = content)
    }
}

/** Convenience accessor for the current app theme. */
object TayraTheme {
    val current: AppTheme
        @Composable get() = LocalAppTheme.current
}
