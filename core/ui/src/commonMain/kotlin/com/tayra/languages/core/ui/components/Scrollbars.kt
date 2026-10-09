package com.tayra.languages.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A scrollbar for [state] along the right edge of the Box, on the desktop, where a long page
 * gives no other sign of how far it goes; nothing on the other platforms. It lies over the
 * content's edge, so content keeps its usual padding there. [modifier] insets it.
 */
@Composable
expect fun BoxScope.EdgeScrollbar(state: ScrollState, modifier: Modifier = Modifier)

@Composable
expect fun BoxScope.EdgeScrollbar(state: LazyListState, modifier: Modifier = Modifier)

/**
 * A column that scrolls, with [EdgeScrollbar] beside it. [modifier] frames the whole;
 * [contentModifier] goes on the column inside the scrolling, where padding belongs;
 * [scrollbarModifier] insets the bar, as inside a rounded border.
 */
@Composable
fun ScrollColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentModifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollbarModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(Modifier.fillMaxWidth().verticalScroll(state).then(contentModifier), verticalArrangement, horizontalAlignment, content)
        EdgeScrollbar(state, scrollbarModifier)
    }
}

/** A [LazyColumn] with [EdgeScrollbar] beside it. [modifier] frames the whole; [listModifier] goes on the list. */
@Composable
fun ScrollList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    listModifier: Modifier = Modifier,
    scrollbarModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize().then(listModifier), state, contentPadding, verticalArrangement = verticalArrangement, content = content)
        EdgeScrollbar(state, scrollbarModifier)
    }
}
