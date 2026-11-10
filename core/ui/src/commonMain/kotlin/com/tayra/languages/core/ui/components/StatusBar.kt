package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppBorders
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.core.ui.theme.AppShapes
import kotlin.math.roundToInt
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

const val STATUS_BAR_TAG = "status-distribution-bar"

/** The blue part of the bar, the words still unknown. */
const val UNKNOWN_SHARE_TAG = "status-bar-unknown"

private val LEARNING = listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4)

/**
 * The share of the different words in a book or on a page still unknown, in solid blue on a white,
 * bordered track; the words known and being learned are the white, and with none unknown the track is
 * white throughout. Hovering it (or a long press on touch screens) shows the counts by status.
 *
 * @param scope how the tooltip names what is counted, in English: "on this page" or "in this book"
 *   (shown through `tr`, so a new scope needs its own translation).
 */
@Composable
fun StatusDistributionBar(stats: BookStats?, modifier: Modifier = Modifier, scope: String = "in this book") {
    val track = Modifier.height(AppSizes.trackBold).clip(AppShapes.tiny).background(MaterialTheme.colorScheme.surface)
        .border(AppBorders.hairline(MaterialTheme.colorScheme.outline), AppShapes.tiny)
    if (stats == null || stats.distinctTerms == 0) {
        Box(modifier.then(track))
        return
    }
    val unknown = stats.distinctUnknowns
    val rest = stats.distinctTerms - unknown
    // The caller's modifier stays on this box: a row weight set on the tooltip box is not applied.
    Box(modifier) {
        AppRichTooltip(title = tr("Vocabulary {0}", tr(scope)), modifier = Modifier.fillMaxWidth(), persistent = true, body = { StatusBreakdown(stats) }) {
            Row(Modifier.testTag(STATUS_BAR_TAG).fillMaxWidth().then(track)) {
                if (unknown > 0) Box(Modifier.fillMaxHeight().weight(unknown.toFloat()).background(AppAccents.blue).testTag(UNKNOWN_SHARE_TAG))
                if (rest > 0) Spacer(Modifier.fillMaxHeight().weight(rest.toFloat()))
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
        Triple(tr("New"), unknown to unknownPercent, colors.background(TermStatus.UNKNOWN)),
        Triple(tr("Learning"), learning to learningPercent, colors.background(TermStatus.NEW_1)),
        Triple(tr("Known"), known to (100 - unknownPercent - learningPercent), colors.background(TermStatus.WELL_KNOWN)),
    )
    Column(Modifier.padding(top = AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        for ((label, numbers, color) in rows) {
            val (n, percent) = numbers
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color)
                Text(label, Modifier.padding(start = AppSpacing.s).width(BREAKDOWN_LABEL_WIDTH), style = AppText.label)
                Text("${words(n)} ($percent%)")
            }
        }
        Text(
            trPlural(total, "{0} word in total", "{0} words in total"),
            Modifier.padding(top = AppSpacing.xxs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun words(n: Int) = trPlural(n, "{0} word", "{0} words")

private val BREAKDOWN_LABEL_WIDTH = 72.dp
