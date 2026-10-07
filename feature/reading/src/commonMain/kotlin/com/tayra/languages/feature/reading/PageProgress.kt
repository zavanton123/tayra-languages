package com.tayra.languages.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import kotlin.math.roundToInt

/**
 * The reader's position in the book: previous and next buttons around "42 of 248" and a slider
 * across the pages. The number follows the slider while it is dragged; the page opens on release.
 *
 * @param sliderWidth the slider's width, or null to fill the space left in the row (on phones).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageProgress(
    pageNumber: Int,
    pageCount: Int,
    onGoTo: (Int) -> Unit,
    sliderWidth: Dp?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var dragged by remember(pageNumber, pageCount) { mutableFloatStateOf(pageNumber.toFloat()) }
    val shown = dragged.roundToInt()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PageStepButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Previous page"), enabled = pageNumber > 1) { onGoTo(pageNumber - 1) }
        Text(
            buildAnnotatedString {
                val text = tr("{0}  of {1}", shown, pageCount)
                val number = text.indexOf("$shown").coerceAtLeast(0)
                withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(text.substring(0, number)) }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.onSurface, fontSize = MaterialTheme.typography.titleLarge.fontSize)) { append("$shown") }
                withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(text.substring(number + "$shown".length)) }
            },
            style = MaterialTheme.typography.bodyMedium,
            softWrap = false,
        )
        if (pageCount > 1) {
            Slider(
                value = dragged,
                onValueChange = { dragged = it },
                onValueChangeFinished = { if (shown != pageNumber) onGoTo(shown) },
                valueRange = 1f..pageCount.toFloat(),
                modifier = (if (sliderWidth == null) Modifier.weight(1f) else Modifier.width(sliderWidth)).height(28.dp),
                thumb = {
                    Box(Modifier.size(18.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(colors.surface).padding(3.dp).clip(CircleShape).background(colors.primary))
                },
                track = { state ->
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(state.coercedValueAsFraction).fillMaxHeight().background(colors.primary))
                    }
                },
            )
        } else if (sliderWidth == null) {
            Spacer(Modifier.weight(1f))
        }
        PageStepButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Next page"), enabled = pageNumber < pageCount) { onGoTo(pageNumber + 1) }
    }
}

@Composable
private fun PageStepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick).padding(8.dp),
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) colors.onSurface else colors.outlineVariant, modifier = Modifier.size(22.dp))
    }
}
