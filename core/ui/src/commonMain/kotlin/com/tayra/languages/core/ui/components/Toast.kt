package com.tayra.languages.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.delay

/**
 * A short message shown at the bottom of the screen for a few seconds, with an optional action such
 * as Undo. A new message replaces the one showing.
 */
class ToastState {
    var text by mutableStateOf("")
        private set
    internal var action by mutableStateOf<Pair<String, () -> Unit>?>(null)
        private set
    var visible by mutableStateOf(false)
        internal set
    internal var shown by mutableIntStateOf(0)
        private set

    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        text = message
        action = if (actionLabel != null && onAction != null) actionLabel to onAction else null
        visible = true
        shown++
    }
}

@Composable
fun rememberToastState() = remember { ToastState() }

@Composable
fun ToastHost(state: ToastState, durationMillis: Long = 3_000) {
    LaunchedEffect(state.shown) {
        if (state.visible) {
            // A message that can be undone stays a little longer, to give time to reach the button.
            delay(if (state.action != null) durationMillis * 2 else durationMillis)
            state.visible = false
        }
    }
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = state.visible
    // The popup stays until the fade-out has finished.
    if (!transition.currentState && !transition.targetState) return
    val lift = with(LocalDensity.current) { 32.dp.roundToPx() }
    Popup(alignment = Alignment.BottomCenter, offset = IntOffset(0, -lift)) {
        AnimatedVisibility(
            transition,
            enter = if (AppMotion.reduced) EnterTransition.None else fadeIn(AppMotion.spec(AppMotion.medium, AppMotion.enter)) + slideInVertically(AppMotion.spec(AppMotion.medium, AppMotion.enter)) { it / 2 },
            exit = if (AppMotion.reduced) ExitTransition.None else fadeOut(AppMotion.spec(AppMotion.short, AppMotion.exit)),
        ) {
            val colors = MaterialTheme.colorScheme
            Row(
                Modifier.padding(horizontal = AppSpacing.l).widthIn(max = TOAST_MAX_WIDTH)
                    .shadow(AppElevation.raised, AppShapes.panel).clip(AppShapes.panel).background(colors.inverseSurface)
                    .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.inversePrimary, modifier = Modifier.size(AppSizes.icon))
                Text(state.text, style = MaterialTheme.typography.bodyMedium, color = colors.inverseOnSurface)
                state.action?.let { (label, onAction) ->
                    Text(
                        label,
                        Modifier.clip(AppShapes.small).clickable(role = Role.Button) { state.visible = false; onAction() }.padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
                        style = AppText.labelStrong,
                        color = colors.inversePrimary,
                    )
                }
            }
        }
    }
}

private val TOAST_MAX_WIDTH = 480.dp
