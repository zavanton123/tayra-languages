package com.idiomiko.feature.terms.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.animatedCount
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.formatPercent
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** Below this width the card puts the total above the ring and the legend instead of beside them. */
private val ONE_LINE_WIDTH = 760.dp
private const val TOTAL_TILE = 56

private val knownColor = StatusTints.ok
private val learningColor = AppAccents.purple

/**
 * The known and learning words' shares, in tenths of a percent of the two together, so that the
 * ring and the legend add up; ignored words count in neither. A share that is there never reads
 * as none, nor one short of all as all.
 */
internal class VocabularyShares(val known: Int, val learning: Int) {
    private val counted = known + learning
    val empty: Boolean get() = counted == 0
    val knownFraction: Float = if (counted == 0) 0f else known.toFloat() / counted
    val knownTenths: Int = when {
        counted == 0 -> 0
        known == 0 || learning == 0 -> if (known == 0) 0 else 1000
        else -> ((known * 1000L + counted / 2) / counted).toInt().coerceIn(1, 999)
    }
    val learningTenths: Int = if (counted == 0) 0 else 1000 - knownTenths
}

/**
 * The vocabulary at a glance on a wide window: the total beside a book, a ring of the known
 * words' share against the learning ones', and a legend with both counts. A narrower card puts
 * the total above the other two.
 */
@Composable
internal fun VocabularyOverview(total: Int, known: Int, learning: Int, modifier: Modifier = Modifier) {
    val shares = VocabularyShares(known, learning)
    Column(
        modifier.fillMaxWidth().testTag("vocabulary-overview").cardSurface(AppShapes.card).padding(AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Text(tr("Overview"), style = AppText.sectionTitle)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= ONE_LINE_WIDTH) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TotalBlock(total, Modifier.weight(1f).padding(end = AppSpacing.xxl))
                    ShareRing(shares, AppSizes.ring, AppSizes.ringStroke, AppText.headline, Modifier.padding(horizontal = AppSpacing.huge))
                    Legend(shares, AppText.headline, Modifier.weight(1.2f).padding(start = AppSpacing.xxxl))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    TotalBlock(total)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
                        ShareRing(shares, AppSizes.ring, AppSizes.ringStroke, AppText.headline)
                        Legend(shares, AppText.headline, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The overview on a phone, in one narrow column: the total beside a small ring, then the legend's two rows. */
@Composable
internal fun PhoneVocabularyOverview(total: Int, known: Int, learning: Int, modifier: Modifier = Modifier) {
    val shares = VocabularyShares(known, learning)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().testTag("vocabulary-overview").cardSurface(AppShapes.card).padding(AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Text(tr("Overview"), style = AppText.itemTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = AppSpacing.m)) {
                Text(tr("Total terms"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(formatCount(animatedCount(total)), style = AppText.headline)
            }
            ShareRing(shares, AppSizes.ringCompact, AppSizes.ringStrokeCompact, AppText.labelBold)
        }
        Legend(shares, AppText.itemTitleBold, gap = AppSpacing.s)
    }
}

/** The book, and beside it the number of terms with its name above. */
@Composable
private fun TotalBlock(total: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        IconTile(AppIcons.Book, size = TOTAL_TILE)
        Column {
            Text(tr("Total terms"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(formatCount(animatedCount(total)), style = AppText.figure, maxLines = 1)
        }
    }
}

/**
 * The known share as a ring, green from the top round to its share and the learning colour the
 * rest of the way, with the percentage in its middle. With nothing counted the ring is all grey at 0%.
 */
@Composable
private fun ShareRing(shares: VocabularyShares, ring: Dp, stroke: Dp, percentStyle: TextStyle, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val track = colors.onSurface.copy(alpha = AppAlpha.tint)
    Box(modifier.size(ring).testTag("vocabulary-ring"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ring)) {
            val width = stroke.toPx()
            val corner = Offset(width / 2, width / 2)
            val arc = Size(size.width - width, size.height - width)
            if (shares.empty) {
                drawArc(track, 0f, 360f, useCenter = false, topLeft = corner, size = arc, style = Stroke(width))
            } else {
                val knownSweep = 360f * shares.knownFraction
                drawArc(learningColor, -90f + knownSweep, 360f - knownSweep, useCenter = false, topLeft = corner, size = arc, style = Stroke(width))
                drawArc(knownColor, -90f, knownSweep, useCenter = false, topLeft = corner, size = arc, style = Stroke(width))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatPercent(shares.knownTenths), style = percentStyle, maxLines = 1)
            Text(tr("known"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** The known and the learning words, each with its colour, count and share. */
@Composable
private fun Legend(shares: VocabularyShares, countStyle: TextStyle, modifier: Modifier = Modifier, gap: Dp = AppSpacing.m) {
    val known = tr("Known")
    val learning = tr("Learning")
    // Both labels take the longer one's width, so that the counts after them line up in any language.
    val measurer = rememberTextMeasurer()
    val labelStyle = AppText.label
    val density = LocalDensity.current
    val labelWidth = with(density) { listOf(known, learning).maxOf { measurer.measure(it, labelStyle, maxLines = 1).size.width }.toDp() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        LegendRow(knownColor, known, shares.known, shares.knownTenths, countStyle, labelWidth, "legend-known")
        LegendRow(learningColor, learning, shares.learning, shares.learningTenths, countStyle, labelWidth, "legend-learning")
    }
}

@Composable
private fun LegendRow(color: Color, label: String, count: Int, tenths: Int, countStyle: TextStyle, labelWidth: Dp, tag: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(AppSizes.dotLarge).clip(AppShapes.circle).background(color))
        Text(label, Modifier.padding(start = AppSpacing.m, end = AppSpacing.l).widthIn(min = labelWidth), style = AppText.label, color = colors.onSurfaceVariant, maxLines = 1)
        Text(formatCount(animatedCount(count)), Modifier.weight(1f).padding(end = AppSpacing.s), style = countStyle, maxLines = 1)
        Text(formatPercent(tenths), style = AppText.labelStrong, color = colors.onSurfaceVariant, maxLines = 1)
    }
}
