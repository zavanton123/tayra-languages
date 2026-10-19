package com.tayra.languages.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tayra.languages.core.ui.theme.AppMotion

/*
 * The design system's motion for content that comes and goes: a panel that opens, a notice that
 * appears, a list's items making room. All of it keeps still when motion is reduced.
 */

@Composable
private fun revealEnter(): EnterTransition =
    if (AppMotion.reduced) EnterTransition.None else fadeIn(AppMotion.spec(AppMotion.medium)) + expandVertically(AppMotion.spec(AppMotion.medium))

@Composable
private fun revealExit(): ExitTransition =
    if (AppMotion.reduced) ExitTransition.None else fadeOut(AppMotion.spec(AppMotion.short, AppMotion.exit)) + shrinkVertically(AppMotion.spec(AppMotion.medium, AppMotion.exit))

/** [content] opening and closing in place while [visible], as a panel of filters or of advanced settings does. */
@Composable
fun Reveal(visible: Boolean, modifier: Modifier = Modifier, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    AnimatedVisibility(visible, modifier, enter = revealEnter(), exit = revealExit(), content = content)
}

/**
 * [content] for [value] while there is one, opening when it comes and closing when it goes; the
 * last value is kept on screen while it closes, as an error that is dismissed.
 */
@Composable
fun <T : Any> RevealValue(value: T?, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val last = remember { LastValue<T>() }
    if (value != null) last.value = value
    AnimatedVisibility(value != null, modifier, enter = revealEnter(), exit = revealExit()) {
        last.value?.let { content(it) }
    }
}

private class LastValue<T> { var value: T? = null }

/** [content] fading in the first time it is shown, as a notice or a result that arrives. */
@Composable
fun Appear(modifier: Modifier = Modifier, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(state, modifier, enter = if (AppMotion.reduced) EnterTransition.None else fadeIn(AppMotion.spec(AppMotion.medium)), exit = ExitTransition.None, content = content)
}

/** The motion of a list's item: fading in and out, and gliding to its new place when the list is filtered or sorted. */
@Composable
fun LazyItemScope.itemMotion(modifier: Modifier = Modifier): Modifier =
    if (AppMotion.reduced) modifier
    else modifier.animateItem(fadeInSpec = AppMotion.spec(AppMotion.short), placementSpec = AppMotion.spec(AppMotion.medium), fadeOutSpec = AppMotion.spec(AppMotion.short))

/** [itemMotion] for an item of a grid. */
@Composable
fun LazyGridItemScope.itemMotion(modifier: Modifier = Modifier): Modifier =
    if (AppMotion.reduced) modifier
    else modifier.animateItem(fadeInSpec = AppMotion.spec(AppMotion.short), placementSpec = AppMotion.spec(AppMotion.medium), fadeOutSpec = AppMotion.spec(AppMotion.short))
