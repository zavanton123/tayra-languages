package com.tayra.languages.feature.playlists

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex

/**
 * A column whose rows are dragged by their handle to another place. While a row is dragged the
 * others make room for it; [onMove] gets its place before and after once it is let go. [row] draws
 * an item, given the modifier its drag handle takes and whether it is the one being dragged.
 */
@Composable
internal fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, index: Int, handle: Modifier, dragging: Boolean) -> Unit,
) {
    var order by remember { mutableStateOf(items) }
    var dragged by remember { mutableStateOf<Any?>(null) }
    var start by remember { mutableStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    // The list as it was when a row was let go, until the move is stored and the list comes back changed.
    var stale by remember { mutableStateOf<List<T>?>(null) }
    // The list as stored comes back between drags; during one, the rows keep the order dragged into.
    if (dragged == null && items != stale && order != items) {
        order = items
        stale = null
    }
    val move by rememberUpdatedState(onMove)
    val latest by rememberUpdatedState(items)
    Column(modifier) {
        order.forEachIndexed { index, item ->
            val id = key(item)
            key(id) {
                val handle = Modifier.pointerInput(id) {
                    detectDragGestures(
                        onDragStart = {
                            dragged = id
                            start = order.indexOfFirst { key(it) == id }
                            offset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val at = order.indexOfFirst { key(it) == id }
                            // Past half of the neighbour's height, the row takes the neighbour's place.
                            val below = order.getOrNull(at + 1)?.let { heights[key(it)] }
                            val above = order.getOrNull(at - 1)?.let { heights[key(it)] }
                            if (below != null && offset > below / 2f) {
                                order = order.toMutableList().apply { add(at + 1, removeAt(at)) }
                                offset -= below
                            } else if (above != null && offset < -above / 2f) {
                                order = order.toMutableList().apply { add(at - 1, removeAt(at)) }
                                offset += above
                            }
                        },
                        onDragEnd = {
                            val to = order.indexOfFirst { key(it) == id }
                            val from = start
                            if (from >= 0 && to != from) {
                                stale = latest
                                move(from, to)
                            }
                            dragged = null
                            offset = 0f
                        },
                        onDragCancel = {
                            dragged = null
                            offset = 0f
                            order = latest
                        },
                    )
                }
                val isDragged = dragged == id
                Column(
                    Modifier.onSizeChanged { heights[id] = it.height }
                        .then(if (isDragged) Modifier.zIndex(1f).graphicsLayer { translationY = offset } else Modifier),
                ) { row(item, index, handle, isDragged) }
            }
        }
    }
}
