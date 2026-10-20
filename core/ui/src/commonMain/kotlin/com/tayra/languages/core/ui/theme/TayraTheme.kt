package com.tayra.languages.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.tayra.languages.core.ui.components.LocalLearningLevels

val LocalAppTheme = staticCompositionLocalOf { AppThemes.default }

/**
 * The app's look: [theme]'s colours, the design system's shapes, motion reduced when the reader or the
 * system asks ([reduceMotion]), and statuses shown with their four learning levels or as one Learning ([learningLevels]).
 */
@Composable
fun TayraTheme(theme: AppTheme = AppThemes.default, reduceMotion: Boolean = false, learningLevels: Boolean = true, content: @Composable () -> Unit) {
    SystemBarIcons(darkTheme = theme.isDark)
    CompositionLocalProvider(LocalAppTheme provides theme, LocalReduceMotion provides (reduceMotion || systemReducesMotion()), LocalLearningLevels provides learningLevels) {
        MaterialTheme(colorScheme = theme.colorScheme, shapes = AppShapes.material, content = content)
    }
}

/** Convenience accessor for the current app theme. */
object TayraTheme {
    val current: AppTheme
        @Composable get() = LocalAppTheme.current
}
