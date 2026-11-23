package com.idiomiko.core.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Pages asked for from outside the screens, such as the settings by the desktop's ⌘, or Ctrl+,; the navigation opens each. */
class NavigationRequests {
    private val requests = MutableSharedFlow<Route>(extraBufferCapacity = 8)

    val routes: SharedFlow<Route> = requests.asSharedFlow()

    fun open(route: Route) {
        requests.tryEmit(route)
    }
}
