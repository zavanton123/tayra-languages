package com.tayra.languages.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BoxScope.EdgeScrollbar(state: ScrollState, modifier: Modifier) = Scrollbar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun BoxScope.EdgeScrollbar(state: LazyListState, modifier: Modifier) = Scrollbar(rememberScrollbarAdapter(state), modifier)

/** A thin rounded thumb with no track, darker under the pointer; drawn only while the content is longer than its viewport. */
@Composable
private fun BoxScope.Scrollbar(adapter: ScrollbarAdapter, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val style = ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 250,
        unhoverColor = ink.copy(alpha = 0.28f),
        hoverColor = ink.copy(alpha = 0.5f),
    )
    // Sized by the box rather than sizing it, so a box that wraps short content stays short.
    Box(Modifier.matchParentSize()) {
        VerticalScrollbar(adapter, modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = 4.dp, bottom = 4.dp, end = 3.dp), style = style)
    }
}
