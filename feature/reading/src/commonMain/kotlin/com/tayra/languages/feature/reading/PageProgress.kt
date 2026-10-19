package com.tayra.languages.feature.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppSlider
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.math.roundToInt

/**
 * The reader's position in the book: previous and next buttons around "42 of 248" and a slider
 * across the pages. The number follows the slider while it is dragged; the page opens on release.
 *
 * @param sliderWidth the slider's width, or null to fill the space left in the row (on phones).
 */
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        AppIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Previous page"),
            onClick = { onGoTo(pageNumber - 1) },
            enabled = pageNumber > 1,
            style = IconButtonStyle.Outlined,
            tint = if (pageNumber > 1) null else colors.outlineVariant,
        )
        Text(
            buildAnnotatedString {
                val text = tr("{0}  of {1}", shown, pageCount)
                val number = text.indexOf("$shown").coerceAtLeast(0)
                withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(text.substring(0, number)) }
                withStyle(AppText.sectionTitle.toSpanStyle().copy(color = colors.onSurface)) { append("$shown") }
                withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(text.substring(number + "$shown".length)) }
            },
            style = MaterialTheme.typography.bodyMedium,
            softWrap = false,
        )
        if (pageCount > 1) {
            AppSlider(
                value = dragged,
                onValueChange = { dragged = it },
                valueRange = 1f..pageCount.toFloat(),
                modifier = (if (sliderWidth == null) Modifier.weight(1f) else Modifier.width(sliderWidth)).height(SLIDER_HEIGHT),
                onValueChangeFinished = { if (shown != pageNumber) onGoTo(shown) },
            )
        } else if (sliderWidth == null) {
            Spacer(Modifier.weight(1f))
        }
        AppIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = tr("Next page"),
            onClick = { onGoTo(pageNumber + 1) },
            enabled = pageNumber < pageCount,
            style = IconButtonStyle.Outlined,
            tint = if (pageNumber < pageCount) null else colors.outlineVariant,
        )
    }
}

private val SLIDER_HEIGHT = 28.dp
