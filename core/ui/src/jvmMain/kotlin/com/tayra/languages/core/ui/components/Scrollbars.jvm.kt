package com.tayra.languages.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing

@Composable
actual fun BoxScope.EdgeScrollbar(state: ScrollState, modifier: Modifier) = Scrollbar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun BoxScope.EdgeScrollbar(state: LazyListState, modifier: Modifier) = Scrollbar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun BoxScope.BottomEdgeScrollbar(state: ScrollState, modifier: Modifier) {
    Layout(
        content = { HorizontalScrollbar(rememberScrollbarAdapter(state), modifier.fillMaxWidth(), style = scrollbarStyle()) },
        modifier = Modifier.matchParentSize(),
        measurePolicy = Overlay(Alignment.BottomCenter),
    )
}

/** A thin rounded thumb with no track, darker under the pointer; drawn only while the content is longer than its viewport. */
@Composable
private fun BoxScope.Scrollbar(adapter: ScrollbarAdapter, modifier: Modifier) {
    // Sized by the box rather than sizing it, so a box that wraps short content stays short.
    Layout(
        content = { VerticalScrollbar(adapter, modifier.fillMaxHeight().padding(top = AppSpacing.xs, bottom = AppSpacing.xs, end = AppSpacing.xs), style = scrollbarStyle()) },
        modifier = Modifier.matchParentSize(),
        measurePolicy = Overlay(Alignment.CenterEnd),
    )
}

/**
 * Lays the bar over the box at [alignment], and has no size of its own when asked beforehand: a
 * menu or a row that sizes itself by its content's intrinsic size (a dropdown menu does) would
 * otherwise ask the bar, which fills the height, how tall it is with no bound.
 */
private class Overlay(private val alignment: Alignment) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val bar = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        return layout(constraints.maxWidth, constraints.maxHeight) {
            bar.place(alignment.align(IntSize(bar.width, bar.height), IntSize(constraints.maxWidth, constraints.maxHeight), layoutDirection))
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = 0
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
}

@Composable
private fun scrollbarStyle(): ScrollbarStyle {
    val ink = MaterialTheme.colorScheme.onSurface
    return ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 8.dp,
        shape = AppShapes.tiny,
        hoverDurationMillis = 250,
        unhoverColor = ink.copy(alpha = AppAlpha.line),
        hoverColor = ink.copy(alpha = AppAlpha.half),
    )
}
