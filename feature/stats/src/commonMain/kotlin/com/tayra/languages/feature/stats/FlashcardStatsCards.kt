package com.tayra.languages.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.domain.flashcards.FlashcardStatistics
import com.tayra.languages.core.domain.flashcards.Pass
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.RetentionSpan
import com.tayra.languages.core.domain.flashcards.ReviewKind
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.label
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.RatingColors
import kotlin.math.roundToInt

/** The memory of the cards, how well the answers went, and how they were given. */
@Composable
internal fun FlashcardStatsDetails(s: FlashcardStatistics) {
    val wide = LocalWindowWidth.current.isExpanded
    SideBySide(wide, { IntervalsCard(s, it) }, { StabilityCard(s, it) })
    SideBySide(wide, { DifficultyCard(s, it) }, { RecallCard(s, it) })
    SideBySide(wide, { RetentionCard(s, it) }, { KnowledgeCard(s, it) })
    HoursCard(s)
    SideBySide(wide, { ButtonsCard(s, it) }, { AnsweredByCard(s, it) })
}

/** Day spans, as Anki's interval graphs group them. */
private val DAY_BUCKETS = listOf(1..1, 2..2, 3..6, 7..13, 14..20, 21..44, 45..89, 90..179, 180..364, 365..Int.MAX_VALUE)

private fun IntRange.dayLabel(): String = when {
    first == last -> "$first"
    last == Int.MAX_VALUE -> "$first+"
    else -> "$first–$last"
}

private fun <T : Comparable<T>> count(values: List<T>, buckets: List<ClosedRange<T>>): List<Int> = buckets.map { range -> values.count { it in range } }

private fun percent(share: Double): Int = (share * 100).roundToInt()

@Composable
private fun IntervalsCard(s: FlashcardStatistics, modifier: Modifier) {
    val average = if (s.intervals.isEmpty()) 0 else s.intervals.average().roundToInt()
    ContentCard(tr("Intervals"), tr("Days until each card in review comes back; {0} on average", formatCount(average)), icon = AppIcons.Schedule, modifier = modifier) {
        Histogram(count(s.intervals, DAY_BUCKETS), DAY_BUCKETS.map { it.dayLabel() }, AppAccents.teal, tr("No cards in review yet."), Modifier.testTag("intervals"))
    }
}

@Composable
private fun StabilityCard(s: FlashcardStatistics, modifier: Modifier) {
    val buckets = DAY_BUCKETS.map { it.first.toDouble()..(if (it.last == Int.MAX_VALUE) Double.MAX_VALUE else it.last + 0.999) }
    val values = s.stabilities.map { it.coerceAtLeast(1.0) }
    val average = if (values.isEmpty()) 0 else values.average().roundToInt()
    ContentCard(tr("Stability"), tr("Days until recall falls to 90%; {0} on average", formatCount(average)), icon = AppIcons.Memory, modifier = modifier) {
        Histogram(count(values, buckets), DAY_BUCKETS.map { it.dayLabel() }, AppAccents.green, tr("No answered cards yet."), Modifier.testTag("stability"))
    }
}

@Composable
private fun DifficultyCard(s: FlashcardStatistics, modifier: Modifier) {
    val buckets = (0 until 10).map { (it / 10.0)..(if (it == 9) 1.0 else it / 10.0 + 0.0999) }
    val average = if (s.difficulties.isEmpty()) 0 else percent(s.difficulties.average())
    ContentCard(tr("Difficulty"), tr("How hard each card is for you; {0}% on average", average), icon = AppIcons.Tune, modifier = modifier) {
        Histogram(count(s.difficulties, buckets), (1..10).map { "${it * 10}%" }, AppAccents.orange, tr("No answered cards yet."), Modifier.testTag("difficulty"))
    }
}

@Composable
private fun RecallCard(s: FlashcardStatistics, modifier: Modifier) {
    val edges = listOf(0.0, 0.5, 0.6, 0.7, 0.8, 0.85, 0.9, 0.95, 1.0001)
    val buckets = edges.zipWithNext { low, high -> low..(high - 0.000001) }
    val labels = listOf("<50%", "50%", "60%", "70%", "80%", "85%", "90%", "95%")
    val average = if (s.recall.isEmpty()) 0 else percent(s.recall.average())
    ContentCard(tr("Recall chance"), tr("The chance of recalling each card in review now; {0}% on average", average), icon = AppIcons.Lightbulb, modifier = modifier) {
        Histogram(count(s.recall, buckets), labels, AppAccents.purple, tr("No cards in review yet."), Modifier.testTag("recall"))
    }
}

/** A column for each of [labels], its count above the tallest; with nothing counted, [empty] in its place. */
@Composable
private fun Histogram(counts: List<Int>, labels: List<String>, color: Color, empty: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    if (counts.all { it == 0 }) {
        Text(empty, modifier, style = MaterialTheme.typography.bodyMedium, color = muted)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Bars(counts.map { listOf(it) }, listOf(color), "", "", empty)
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = muted, textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

private fun RetentionSpan.label(): String = when (this) {
    RetentionSpan.TODAY -> tr("Today")
    RetentionSpan.YESTERDAY -> tr("Yesterday")
    RetentionSpan.WEEK -> tr("Last week")
    RetentionSpan.MONTH -> tr("Last month")
    RetentionSpan.YEAR -> tr("Last year")
    RetentionSpan.ALL -> tr("All time")
}

@Composable
private fun RetentionCard(s: FlashcardStatistics, modifier: Modifier) {
    val target = s.desiredRetention
    ContentCard(tr("True retention"), tr("Reviews remembered, against your target of {0}%", percent(target)), icon = AppIcons.VerifiedUser, modifier = modifier) {
        Column(Modifier.fillMaxWidth().testTag("true-retention")) {
            RetentionRow(null, listOf(tr("Young"), tr("Mature"), tr("Total")))
            RetentionSpan.entries.forEach { span ->
                val kinds = s.retention[span].orEmpty()
                val young = kinds[ReviewKind.YOUNG] ?: Pass()
                val mature = kinds[ReviewKind.MATURE] ?: Pass()
                val total = Pass(young.passed + mature.passed, young.total + mature.total)
                RetentionRow(span.label(), listOf(young, mature, total).map { it.cell() }, listOf(young, mature, total).map { it.tint(target) })
            }
        }
    }
}

private fun Pass.cell(): String = rate?.let { "${(it * 1000).roundToInt() / 10.0}% ($passed/$total)" } ?: "—"

@Composable
private fun Pass.tint(target: Double): Color = when {
    rate == null -> MaterialTheme.colorScheme.onSurfaceVariant
    rate!! >= target -> AppAccents.green
    else -> AppAccents.amber
}

@Composable
private fun RetentionRow(name: String?, cells: List<String>, tints: List<Color>? = null) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(name.orEmpty(), Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
        cells.forEachIndexed { i, cell ->
            Text(
                cell,
                Modifier.weight(1f),
                style = if (name == null) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                color = tints?.get(i) ?: muted,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun KnowledgeCard(s: FlashcardStatistics, modifier: Modifier) {
    val known = s.knowledge.roundToInt()
    ContentCard(tr("Estimated knowledge"), tr("Words you would recall now, adding up each card's chance of recall"), icon = AppIcons.DoneAll, modifier = modifier) {
        Text(tr("About {0} of {1}", formatCount(known), formatCount(s.knowledgeOf)), style = AppText.headline, modifier = Modifier.testTag("knowledge"))
        if (s.knowledgeOf > 0) {
            Text(tr("{0}% of the answered cards", percent(s.knowledge / s.knowledgeOf)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HoursCard(s: FlashcardStatistics) {
    val answers = s.hours.map { it.answers.total }
    val total = answers.sum()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val busiest = s.hours.maxByOrNull { it.answers.total }?.takeIf { it.answers.total > 0 }
    // An hour with only a few answers says little of how well that hour goes.
    val best = s.hours.filter { it.answers.total >= maxOf(3, total / 50) }.maxByOrNull { it.answers.rate ?: 0.0 }
    val subtitle = when {
        busiest == null -> tr("When in the day you answer, and how well")
        best == null -> tr("Most answers at {0}:00", busiest.hour)
        else -> tr("Most answers at {0}:00; best at {1}:00, {2}% correct", busiest.hour, best.hour, percent(best.answers.rate ?: 0.0))
    }
    ContentCard(tr("Hourly breakdown"), subtitle, icon = AppIcons.History) {
        if (total == 0) {
            Text(tr("No answers in the period."), style = MaterialTheme.typography.bodyMedium, color = muted)
            return@ContentCard
        }
        val bar = AppAccents.blue
        val line = AppAccents.green
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
        val max = answers.max()
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT).testTag("hours")) {
            val slot = size.width / 24
            val width = slot * 0.68f
            drawRect(track, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
            answers.forEachIndexed { hour, n ->
                if (n > 0) {
                    val h = (size.height * n / max).coerceAtLeast(2f)
                    drawRoundRect(bar.copy(alpha = AppAlpha.half), Offset(hour * slot + (slot - width) / 2, size.height - h), Size(width, h), CornerRadius(width / 6))
                }
            }
            // The share answered right, as a line over the hours with answers, from none at the foot to all at the top.
            val path = Path()
            var started = false
            s.hours.forEach { hour ->
                val rate = hour.answers.rate ?: return@forEach
                val x = hour.hour * slot + slot / 2
                val y = size.height * (1 - rate.toFloat())
                if (started) path.lineTo(x, y) else path.moveTo(x, y).also { started = true }
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("0:00", "6:00", "12:00", "18:00", "23:00").forEachIndexed { i, label ->
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = muted, textAlign = if (i == 4) TextAlign.End else TextAlign.Start)
            }
        }
        Legend(listOf(tr("Answers") to bar.copy(alpha = AppAlpha.half), tr("Correct") to line))
    }
}

private val Rating.tint: Color
    get() = when (this) {
        Rating.AGAIN -> RatingColors.again
        Rating.HARD -> RatingColors.hard
        Rating.GOOD -> RatingColors.good
        Rating.EASY -> RatingColors.easy
    }

@Composable
private fun ButtonsCard(s: FlashcardStatistics, modifier: Modifier) {
    ContentCard(tr("Answer buttons"), tr("How often each answer was given, by kind of card"), icon = AppIcons.BarChart, modifier = modifier) {
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
        Column(Modifier.fillMaxWidth().testTag("answer-buttons"), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            listOf(ReviewKind.LEARNING, ReviewKind.YOUNG, ReviewKind.MATURE).forEach { kind ->
                val counts = s.buttons[kind] ?: List(4) { 0 }
                val total = counts.sum()
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(kind.label(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (total == 0) tr("No answers") else tr("{0}% correct of {1}", percent((total - counts[0]).toDouble() / total), formatCount(total)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(Modifier.fillMaxWidth().height(AppSizes.trackBold).clip(AppShapes.pill).background(track)) {
                        Rating.entries.forEachIndexed { i, rating -> if (counts[i] > 0) Box(Modifier.weight(counts[i].toFloat()).fillMaxHeight().background(rating.tint)) }
                    }
                }
            }
            Legend(Rating.entries.map { tr(it.label) to it.tint })
        }
    }
}

@Composable
private fun AnsweredByCard(s: FlashcardStatistics, modifier: Modifier) {
    val total = s.answeredBy.values.sumOf { it.total }
    ContentCard(tr("How you answered"), tr("Answers shown, typed or said, and how many were right"), icon = AppIcons.Keyboard, modifier = modifier) {
        Column(Modifier.fillMaxWidth().testTag("answered-by")) {
            AnswerMode.entries.forEach { mode ->
                val pass = s.answeredBy[mode] ?: Pass()
                Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        pass.rate?.let { tr("{0}% correct of {1}", percent(it), formatCount(pass.total)) } ?: tr("No answers"),
                        Modifier.padding(end = AppSpacing.l),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(if (total > 0) "${percent(pass.total.toDouble() / total)}%" else "—", style = AppText.labelStrong)
                }
            }
        }
    }
}
