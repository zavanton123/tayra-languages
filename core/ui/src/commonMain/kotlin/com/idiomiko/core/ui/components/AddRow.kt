package com.idiomiko.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** A dashed row under a list that makes another of its kind: a + and [label], across the list's width. */
@Composable
fun AddRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant
    Row(
        modifier.fillMaxWidth().clip(AppShapes.large)
            .drawBehind {
                val stroke = DASH_WIDTH.toPx()
                drawRoundRect(
                    line,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(DASH_CORNER.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx()))),
                )
            }
            .clickable(onClick = onClick).padding(vertical = AppSpacing.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
        Spacer(Modifier.width(AppSpacing.s))
        Text(label, style = AppText.labelBold, color = colors.primary)
    }
}

private val DASH = 6.dp
private val DASH_WIDTH = 2.dp
private val DASH_CORNER = 18.dp
