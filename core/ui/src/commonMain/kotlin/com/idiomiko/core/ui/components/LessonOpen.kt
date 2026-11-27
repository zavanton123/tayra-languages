package com.idiomiko.core.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Set while a lesson is open, so the app's frame leaves the playlist player's bar off its screen. */
val LocalLessonOpen = staticCompositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }
