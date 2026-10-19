package com.tayra.languages.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppTheme = staticCompositionLocalOf { AppThemes.default }

/** The app's look: [theme]'s colours, the design system's shapes, and motion reduced when the reader or the system asks ([reduceMotion]). */
@Composable
fun TayraTheme(theme: AppTheme = AppThemes.default, reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppTheme provides theme, LocalReduceMotion provides (reduceMotion || systemReducesMotion())) {
        MaterialTheme(colorScheme = theme.colorScheme, shapes = AppShapes.material, content = content)
    }
}

/** Convenience accessor for the current app theme. */
object TayraTheme {
    val current: AppTheme
        @Composable get() = LocalAppTheme.current
}
