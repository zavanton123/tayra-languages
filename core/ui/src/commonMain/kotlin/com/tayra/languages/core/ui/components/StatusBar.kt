package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.theme.TayraTheme

/** Stacked bar showing the distribution of term statuses in a book. */
@Composable
fun StatusDistributionBar(stats: BookStats?, modifier: Modifier = Modifier) {
    if (stats == null || stats.distinctTerms == 0) {
        Box(modifier.height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }
    val colors = TayraTheme.current.statusColors
    Row(modifier.height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        for (status in listOf(TermStatus.UNKNOWN, TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4, TermStatus.LEARNED, TermStatus.WELL_KNOWN, TermStatus.IGNORED)) {
            val count = stats.statusDistribution[status] ?: 0
            if (count > 0) {
                Box(Modifier.fillMaxHeight().weight(count.toFloat()).background(colors.background(status)))
            }
        }
    }
}

@Composable
fun StatusLegend(stats: BookStats) {
    Text(
        "${stats.distinctTerms} terms, ${stats.distinctUnknowns} unknown (${stats.unknownPercent}%)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
