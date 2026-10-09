package com.tayra.languages.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun BoxScope.EdgeScrollbar(state: ScrollState, modifier: Modifier) = Unit

@Composable
actual fun BoxScope.EdgeScrollbar(state: LazyListState, modifier: Modifier) = Unit
