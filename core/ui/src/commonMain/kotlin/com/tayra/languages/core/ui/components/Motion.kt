package com.tayra.languages.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * [content] fading in the first time it is shown, as a notice or a result that arrives; [grow]
 * also has it grow a little into place, as the next card does.
 */
@Composable
fun Appear(modifier: Modifier = Modifier, grow: Boolean = false, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val enter = when {
        AppMotion.reduced -> EnterTransition.None
        grow -> fadeIn(AppMotion.spec(AppMotion.medium, AppMotion.enter)) + scaleIn(AppMotion.spec(AppMotion.medium, AppMotion.enter), initialScale = GROW_FROM)
        else -> fadeIn(AppMotion.spec(AppMotion.medium))
    }
    AnimatedVisibility(state, modifier, enter = enter, exit = ExitTransition.None, content = content)
}

/** [content] popping into place the first time it is shown: a tick for a right answer, a mark just earned. */
@Composable
fun PopIn(modifier: Modifier = Modifier, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val enter = if (AppMotion.reduced) EnterTransition.None
    else scaleIn(AppMotion.spec(AppMotion.long, AppMotion.pop), initialScale = POP_FROM) + fadeIn(AppMotion.spec(AppMotion.short))
    AnimatedVisibility(state, modifier, enter = enter, exit = ExitTransition.None, content = content)
}

/** A count shown as it changes: it runs to its new value rather than jumping, and starts where it is. */
@Composable
fun animatedCount(value: Int): Int {
    val shown by animateIntAsState(value, AppMotion.spec(AppMotion.long))
    return shown
}

/** How large the next card starts, and how small a popped mark. */
private const val GROW_FROM = 0.97f
private const val POP_FROM = 0.4f

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

/**
 * A card that gives a little under the finger: it shrinks a touch while pressed. Put it before the
 * card's own shape and surface, and give its clickable the same [source].
 */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !AppMotion.reduced) PRESSED_SCALE else 1f, AppMotion.spec(AppMotion.short))
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** How small a pressed card gets. */
private const val PRESSED_SCALE = 0.98f
