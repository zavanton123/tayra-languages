package com.tayra.languages.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tayra.languages.core.ui.components.LocalWindowWidth

/**
 * Shows a practice, [Route.Practice] or [Route.PracticeWords], in a dialog over the page that starts
 * it, calling back when it is closed. Null where the app does not provide it, and the practice
 * opens as a page of its own.
 */
val LocalPracticeDialog = staticCompositionLocalOf<(@Composable (route: Route, onClose: () -> Unit) -> Unit)?> { null }

/**
 * The way to start a practice from this page: on a window wider than a phone, in the
 * [LocalPracticeDialog] shown here over the page; on a phone, or without one, by [onNavigate].
 */
@Composable
fun rememberPracticeOpener(onNavigate: (Route) -> Unit): (Route) -> Unit {
    val dialog = LocalPracticeDialog.current?.takeIf { !LocalWindowWidth.current.isCompact }
    var open by remember { mutableStateOf<Route?>(null) }
    open?.let { route -> dialog?.invoke(route) { open = null } }
    return { route -> if (dialog != null) open = route else onNavigate(route) }
}
