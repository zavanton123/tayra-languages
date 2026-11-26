package com.idiomiko.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.coroutines.delay

/**
 * Tells which of [count] equal columns across the width the pointer is over, or null once it leaves; on a touch
 * screen a tap picks the column. Found from where the pointer is, rather than a listener on every column.
 */
fun Modifier.hoverColumn(count: Int, onColumn: (Int?) -> Unit): Modifier = pointerInput(count) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val at = event.changes.firstOrNull()?.position ?: continue
            val outside = event.type == PointerEventType.Exit || at.x < 0 || at.y < 0 || at.x >= size.width || at.y > size.height
            onColumn(if (outside || count == 0) null else (at.x / (size.width.toFloat() / count)).toInt().coerceIn(0, count - 1))
        }
    }
}

/**
 * A chart of [count] equal columns whose column under the pointer is named in a tooltip by [describe], over the
 * column at [level] of the chart's height (a fraction, its bar's top). [content] draws the chart, told which column
 * is pointed at, to mark it.
 */
@Composable
fun ColumnTooltips(
    count: Int,
    describe: ((Int) -> String)?,
    modifier: Modifier = Modifier,
    level: (Int) -> Float = { 1f },
    content: @Composable BoxScope.(hovered: Int?) -> Unit,
) {
    var hovered by remember(count) { mutableStateOf<Int?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    TouchTooltipTimeout(hovered) { hovered = null }
    Box(modifier.onSizeChanged { size = it }.then(if (describe == null) Modifier else Modifier.hoverColumn(count) { hovered = it })) {
        content(hovered)
        val column = hovered
        if (describe != null && column != null && column < count && size.width > 0) {
            val slot = size.width.toFloat() / count
            val top = (size.height * (1 - level(column).coerceIn(0f, 1f))).toInt()
            ChartTooltip(describe(column), IntRect(IntOffset((column * slot).toInt(), top), IntSize(slot.toInt().coerceAtLeast(1), (size.height - top).coerceAtLeast(1))))
        }
    }
}

/**
 * On a phone, where a tap leaves a chart's tooltip up with no pointer to move away, [clear] once the tooltip for
 * [shown] has been up for [TOUCH_TOOLTIP_MILLIS]; a new tap starts the wait again.
 */
@Composable
internal fun TouchTooltipTimeout(shown: Any?, clear: () -> Unit) {
    if (!LocalWindowWidth.current.isCompact) return
    LaunchedEffect(shown) {
        if (shown == null) return@LaunchedEffect
        delay(TOUCH_TOOLTIP_MILLIS)
        clear()
    }
}

/** How long a tapped chart's tooltip stays on a phone. */
internal const val TOUCH_TOOLTIP_MILLIS = 3_000L

/** [text] about the part of a chart at [anchor] (within the box it is placed in), over it, kept inside the window. */
@Composable
fun ChartTooltip(text: String, anchor: IntRect) {
    val margin = with(LocalDensity.current) { AppSpacing.xs.roundToPx() }
    val position = remember(anchor, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val part = anchor.translate(anchorBounds.topLeft)
                val x = (part.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val above = part.top - popupContentSize.height - margin
                return IntOffset(x, if (above >= 0) above else part.bottom + margin)
            }
        }
    }
    Popup(popupPositionProvider = position) {
        Text(
            text,
            Modifier.clip(AppShapes.small).background(MaterialTheme.colorScheme.inverseSurface).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs).testTag("chart-tooltip"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}
