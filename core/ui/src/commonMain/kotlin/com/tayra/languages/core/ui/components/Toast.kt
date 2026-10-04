package com.tayra.languages.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

/** A short message shown at the bottom of the screen for a few seconds. A new message replaces the one showing. */
class ToastState {
    var text by mutableStateOf("")
        private set
    var visible by mutableStateOf(false)
        internal set
    internal var shown by mutableIntStateOf(0)
        private set

    fun show(message: String) {
        text = message
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
            delay(durationMillis)
            state.visible = false
        }
    }
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = state.visible
    // The popup stays until the fade-out has finished.
    if (!transition.currentState && !transition.targetState) return
    val lift = with(LocalDensity.current) { 32.dp.roundToPx() }
    Popup(alignment = Alignment.BottomCenter, offset = IntOffset(0, -lift)) {
        AnimatedVisibility(transition, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
            val colors = MaterialTheme.colorScheme
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.inverseSurface,
                contentColor = colors.inverseOnSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 480.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.inversePrimary, modifier = Modifier.size(20.dp))
                    Text(state.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
