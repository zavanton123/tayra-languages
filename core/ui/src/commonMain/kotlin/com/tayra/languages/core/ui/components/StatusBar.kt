package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.math.roundToInt

const val STATUS_BAR_TAG = "status-distribution-bar"

private val LEARNING = listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4)

/** Ignored words are counted as known: the reader no longer needs to look them up. */
private val KNOWN = listOf(TermStatus.WELL_KNOWN, TermStatus.IGNORED)

/**
 * Stacked bar of the word statuses in a book or on a page, from unknown through the learning
 * levels to known. Hovering it (or a long press on touch screens) shows the counts behind it.
 *
 * @param scope how the tooltip names what is counted, e.g. "on this page" or "in this book".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDistributionBar(stats: BookStats?, modifier: Modifier = Modifier, scope: String = "in this book") {
    if (stats == null || stats.distinctTerms == 0) {
        Box(modifier.height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }
    val colors = TayraTheme.current.statusColors
    val count = { statuses: List<TermStatus> -> statuses.sumOf { stats.statusDistribution[it] ?: 0 } }
    // The caller's modifier stays on this box: a row weight set on the tooltip box is not applied.
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { RichTooltip(title = { Text("Vocabulary $scope") }) { StatusBreakdown(stats) } },
            // Persistent: stays while the pointer is on the bar (or, on touch, until a tap elsewhere) instead of hiding after 1.5 s.
            state = rememberTooltipState(isPersistent = true),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.testTag(STATUS_BAR_TAG).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val segments = listOf(TermStatus.UNKNOWN to count(listOf(TermStatus.UNKNOWN))) +
                    LEARNING.map { it to count(listOf(it)) } +
                    (TermStatus.WELL_KNOWN to count(KNOWN))
                for ((status, n) in segments) {
                    if (n > 0) Box(Modifier.fillMaxHeight().weight(n.toFloat()).background(colors.background(status)))
                }
            }
        }
    }
}

@Composable
private fun StatusBreakdown(stats: BookStats) {
    val colors = TayraTheme.current.statusColors
    val total = stats.distinctTerms
    val unknown = stats.distinctUnknowns
    val learning = LEARNING.sumOf { stats.statusDistribution[it] ?: 0 }
    val known = total - unknown - learning
    // Unknown keeps the rounding shown beside the bar; known takes the remainder so the rows add up to 100%.
    val unknownPercent = stats.unknownPercent
    val learningPercent = (100.0 * learning / total).roundToInt().coerceAtMost(100 - unknownPercent)
    val rows = listOf(
        Triple("Unknown", unknown to unknownPercent, colors.background(TermStatus.UNKNOWN)),
        Triple("Learning", learning to learningPercent, colors.background(TermStatus.NEW_1)),
        Triple("Known", known to (100 - unknownPercent - learningPercent), colors.background(TermStatus.WELL_KNOWN)),
    )
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((label, numbers, color) in rows) {
            val (n, percent) = numbers
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text(label, Modifier.padding(start = 8.dp).width(72.dp), fontWeight = FontWeight.Medium)
                Text("${words(n)} ($percent%)")
            }
        }
        Text(
            "${words(total)} in total",
            Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun words(n: Int) = if (n == 1) "1 word" else "$n words"
