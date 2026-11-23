package com.idiomiko.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.VocabularyLevelService
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.ui.components.MarkedText
import com.idiomiko.core.ui.components.pushFill
import com.idiomiko.core.ui.components.AppSlider
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.StatusDot
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlin.math.roundToInt

/** The sample text with each word in the colour of its standing at [level]: known, new, or outside the list. */
@Composable
internal fun colouredExample(state: VocabularySettingsUiState, level: Int) = buildAnnotatedString {
    // As the reader draws a page: known words plain, new ones on the unknown words' highlight.
    val colors = MaterialTheme.colorScheme
    val statuses = IdiomikoTheme.current.statusColors
    val outside = colors.onSurfaceVariant
    val onNew = if (statuses.onHighlight == Color.Unspecified) colors.onSurface else statuses.onHighlight
    for (token in state.example) {
        val style = when {
            !token.isWord -> SpanStyle(color = colors.onSurface)
            token.rank == null -> SpanStyle(color = outside)
            token.rank <= level -> SpanStyle(color = colors.onSurface)
            else -> null
        }
        if (style != null) withStyle(style) { append(token.text) }
        else {
            pushFill(statuses.background(TermStatus.UNKNOWN))
            withStyle(SpanStyle(color = onNew)) { append(token.text) }
            pop()
        }
    }
}

/** What a level means to a learner, roughly. */
internal fun levelName(level: Int): String = when {
    level == 0 -> tr("Starting out")
    level <= 300 -> tr("Early beginner")
    level <= 700 -> tr("Beginner")
    level <= 1500 -> tr("Elementary")
    level <= 3000 -> tr("Intermediate")
    level <= 6000 -> tr("Upper intermediate")
    else -> tr("Advanced")
}

/** The levels named under the slider; the others are ticks only. */
private val NAMED_STOPS = setOf(0, 1000, 5000, 10_000)

/** The levels named under a slider over [choices]: the start, 1k, 5k and the list's end. */
internal fun landmarks(choices: List<Int>): Set<Int> = setOf(0, 1000, 5000, choices.last())

private fun shortCount(n: Int): String = when {
    n < 1000 -> "$n"
    n % 1000 == 0 -> "${n / 1000}k"
    else -> "${n / 1000}.${n % 1000 / 100}k"
}

/** The estimate in words, a slider over the levels, and words at the chosen one to judge it by; the vocabulary settings show it too. */
@Composable
internal fun EstimateCard(list: FrequencyList, level: Int, onPick: (Int) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val choices = remember(list) { VocabularyLevelService.choices(list.words.size) }
    val index = choices.indexOf(level).coerceAtLeast(0)
    val shape = AppShapes.large
    Column(modifier.clip(shape).border(AppBorders.hairline(), shape).padding(if (compact) AppSpacing.l else AppSpacing.xxl)) {
        Text(tr("Estimated vocabulary"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatCount(level), style = if (compact) AppText.figure else AppText.figure.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize, lineHeight = MaterialTheme.typography.displayLarge.lineHeight), color = colors.primary)
            Spacer(Modifier.width(if (compact) AppSpacing.m else AppSpacing.l))
            Text(trPlural(level, "word", "words"), Modifier.padding(bottom = if (compact) AppSpacing.s else AppSpacing.m), style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, color = colors.onSurface)
        }
        Text(levelName(level), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(if (compact) AppSpacing.m else AppSpacing.xl))
        // The same four landmarks everywhere: more crowd each other and differ from screen to screen.
        LevelSlider(choices, index, onPick, named = landmarks(choices), sparse = true)
        Spacer(Modifier.height(if (compact) AppSpacing.l else AppSpacing.xxl))
        // The words a learner at this level has just reached; at 0, the first ones to learn.
        val words = remember(list, level, choices) {
            val previous = if (index == 0) 0 else choices[index - 1]
            if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
        }
        Column(
            Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(if (compact) AppSpacing.l else AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = AppText.itemTitle)
            words.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.m)) {
                    row.forEach { word ->
                        Text(
                            word,
                            Modifier.weight(1f).clip(AppShapes.pill).background(colors.primary.copy(alpha = AppAlpha.tint)).padding(vertical = AppSpacing.s, horizontal = AppSpacing.s),
                            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                if (level == 0) tr("Not familiar yet? Start from scratch, or move the slider.") else tr("Move the slider until these words feel familiar."),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * A slider that stops at each level, with ticks under it and the [named] levels labelled, plus any
 * other with room unless [sparse], which also ticks only the named levels; [caption] names the start.
 */
@Composable
internal fun LevelSlider(choices: List<Int>, index: Int, onPick: (Int) -> Unit, named: Set<Int> = NAMED_STOPS, sparse: Boolean = false, caption: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val last = (choices.size - 1).coerceAtLeast(1)
    Column {
        AppSlider(
            value = index.toFloat(),
            onValueChange = { onPick(choices[it.roundToInt().coerceIn(0, choices.size - 1)]) },
            valueRange = 0f..last.toFloat(),
            modifier = Modifier.fillMaxWidth().height(SLIDER_HEIGHT).testTag("level-slider"),
            steps = (choices.size - 2).coerceAtLeast(0),
        )
        // Ticks and names placed where the slider stops, the track running between the thumb's centres
        // at either end. Names that would run into one another are left out, the chosen level's first.
        Layout(
            content = {
                choices.forEachIndexed { i, stop -> Box(Modifier.width(TICK_WIDTH).height(TICK_HEIGHT).background(if (sparse && stop !in named && i != index) Color.Transparent else colors.outline)) }
                choices.forEachIndexed { i, stop ->
                    Text(
                        shortCount(stop),
                        style = if (sparse) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == index) colors.primary else colors.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0)
            val ticks = measurables.take(choices.size).map { it.measure(loose) }
            val names = measurables.drop(choices.size).map { it.measure(loose) }
            val inset = (THUMB / 2).roundToPx()
            val span = (constraints.maxWidth - 2 * inset).coerceAtLeast(1)
            val gap = 10.dp.roundToPx()
            fun left(i: Int) = (inset + span * i / last - names[i].width / 2).coerceIn(0, constraints.maxWidth - names[i].width)
            val shown = mutableSetOf<Int>()
            fun fits(i: Int) = shown.none { j -> left(i) < left(j) + names[j].width + gap && left(j) < left(i) + names[i].width + gap }
            // The chosen level, the two ends, the named levels, then any other stop with room.
            val order = listOf(index, 0, choices.size - 1) + choices.indices.filter { choices[it] in named } + if (sparse) emptyList() else choices.indices
            for (i in order) if (i !in shown && fits(i)) shown += i
            val tickHeight = ticks.maxOf { it.height }
            val top = tickHeight + AppSpacing.s.roundToPx()
            layout(constraints.maxWidth, top + names.maxOf { it.height }) {
                ticks.forEachIndexed { i, tick -> tick.place(inset + span * i / last - tick.width / 2, 0) }
                shown.forEach { i -> names[i].place(left(i), top) }
            }
        }
        if (caption) Text(tr("Starting out"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** AppSlider's thumb, whose centres at either end bound the track. */
private val THUMB = 22.dp
private val SLIDER_HEIGHT = 32.dp
private val TICK_WIDTH = 1.5.dp
private val TICK_HEIGHT = 10.dp
/** The language's sample story coloured by the estimate: the words it knows, the new ones, and the ones outside the list. */
@Composable
internal fun PreviewCard(state: VocabularySettingsUiState, level: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val outside = colors.onSurfaceVariant
    val shape = AppShapes.large
    Column(modifier.clip(shape).border(AppBorders.hairline(), shape).padding(if (compact) AppSpacing.l else AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.l)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr("Reading preview"), style = if (compact) AppText.itemTitle else AppText.sectionTitle)
            Text(tr("A sample text at this estimate"), style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        val inner = AppShapes.card
        // Beside the estimate card the text takes the height left; stacked in a scrolling page, where
        // the height is unbounded, a weight would leave it no height at all.
        val textHeight = if (width.isExpanded) Modifier.weight(1f, fill = false) else Modifier
        Box(Modifier.fillMaxWidth().then(textHeight).clip(inner).border(AppBorders.hairline(), inner).padding(if (compact) AppSpacing.l else AppSpacing.xl)) {
            if (state.example.isEmpty()) {
                Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
            } else {
                MarkedText(
                    colouredExample(state, level),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.45f),
                    maxLines = 9,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Three legends do not always fit one phone-wide row.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            PreviewLegend(null, tr("Known"))
            PreviewLegend(IdiomikoTheme.current.statusColors.background(TermStatus.UNKNOWN), tr("New"))
            PreviewLegend(outside, tr("Outside frequency list"))
        }
    }
}

@Composable
internal fun PreviewLegend(color: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        // No colour is plain text, shown as an empty ring, as the reader's legend shows known words.
        if (color != null) StatusDot(color) else Box(Modifier.size(AppSizes.dotLarge).border(AppBorders.hairline(MaterialTheme.colorScheme.onSurfaceVariant), AppShapes.circle))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
