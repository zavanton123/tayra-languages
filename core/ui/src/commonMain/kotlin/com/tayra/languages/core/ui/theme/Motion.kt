package com.tayra.languages.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the app keeps still: no sliding, fading or flipping, as the reader or the system asks. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Whether the system asks apps for less motion (Android's animations off, iOS's and the browser's Reduce Motion). */
@Composable
internal expect fun systemReducesMotion(): Boolean

/**
 * The design system's motion: how long things take to change and how they ease. Changes are quick,
 * so nothing keeps a reader waiting, and none at all when motion is reduced.
 */
object AppMotion {
    /** A colour, a check or a highlight changing. */
    const val short = 150

    /** Something moving or growing: a tab's indicator, a panel opening, a list making room. */
    const val medium = 250

    /** A screen or a card turning over. */
    const val long = 400

    /** Most changes: quick to start, gentle to settle. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** What arrives: it slows as it settles. */
    val enter: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /** What leaves: it speeds up as it goes. */
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** Whether motion is reduced here. */
    val reduced: Boolean @Composable get() = LocalReduceMotion.current

    /** How a change of [duration] runs, or at once when motion is reduced. */
    @Composable
    fun <T> spec(duration: Int = medium, easing: Easing = standard, delay: Int = 0): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else tween(duration, delay, easing)
}
