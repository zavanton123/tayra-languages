package com.tayra.languages.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.trPlural
import org.koin.compose.viewmodel.koinViewModel
import com.tayra.languages.core.ui.components.ScrollColumn

/**
 * Asks for the vocabulary level of the language with [languageId], just chosen to learn, when no
 * level was ever chosen for it. [onClosed] is called once a level is set or the reader puts it off;
 * put off, they are asked again the next time they choose the language.
 */
@Composable
fun VocabularyLevelPrompt(languageId: Long?, onClosed: () -> Unit, viewModel: VocabularySettingsViewModel = koinViewModel(key = "vocabulary-level-prompt")) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    if (languageId == null) return
    // The view model follows the language being learned, which has just become this one.
    val ready = !state.loading && state.languageId == languageId && state.list != null
    LaunchedEffect(languageId, state.chosen, state.saving, ready) {
        if (ready && state.chosen && !state.saving) onClosed()
    }
    // The slider always shows a level, so one is picked from the start: "starting out".
    LaunchedEffect(ready, state.picked) {
        if (ready && !state.chosen && state.picked == null) viewModel.pick(0)
    }
    // On a phone the prompt takes the whole screen, as a page of its own; elsewhere it floats.
    val compact = LocalWindowWidth.current.isCompact
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(
            if (compact) Modifier.fillMaxSize().testTag("vocabulary-level-prompt")
            else Modifier.widthIn(max = 1180.dp).fillMaxWidth(0.94f).padding(vertical = 24.dp).testTag("vocabulary-level-prompt"),
            shape = if (compact) RectangleShape else RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (!ready) {
                Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Surface
            }
            VocabularyLevelPromptContent(
                state,
                onPick = viewModel::pick,
                onSave = viewModel::save,
                onLater = { viewModel.forgetPick(); onClosed() },
            )
        }
    }
}

@Composable
internal fun VocabularyLevelPromptContent(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val list = state.list ?: return
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val level = state.picked ?: state.level
    if (width.isCompact) {
        PhonePromptContent(state, list, level, onPick, onSave, onLater)
        return
    }
    Column {
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(start = 40.dp, end = 28.dp, top = 28.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        tr("Choose an estimate. Words below this level will start as known. You can change it later in Settings → Vocabulary."),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onLater, enabled = !state.saving) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
            }
            Spacer(Modifier.height(24.dp))
            if (width.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    EstimateCard(list, level, onPick, Modifier.weight(1f).fillMaxHeight())
                    PreviewCard(state, level, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                EstimateCard(list, level, onPick, Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                PreviewCard(state, level, Modifier.fillMaxWidth())
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
        PromptButtons(state, level, onSave, onLater, Modifier.padding(horizontal = 28.dp, vertical = 20.dp))
    }
}

/**
 * The prompt as one phone screen, with nothing to scroll: the title, the estimate with its slider
 * and the words at that level, spread over the height, and the buttons at the bottom. The reading
 * preview is left to wider screens.
 */
@Composable
private fun PhonePromptContent(state: VocabularySettingsUiState, list: FrequencyList, level: Int, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val choices = remember(list) { VocabularyLevelService.choices(list.words.size) }
    val index = choices.indexOf(level).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onLater, enabled = !state.saving) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
        }
        Text(
            tr("Choose an estimate. Words below this level will start as known. You can change it later in Settings → Vocabulary."),
            Modifier.padding(end = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        // The rest keeps clear of the close button's column and shares out the height left.
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            Column {
                Text(tr("Estimated vocabulary"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatCount(level), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(trPlural(level, "word", "words"), Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                }
                Text(levelName(level), style = MaterialTheme.typography.titleMedium)
            }
            LevelSlider(choices, index, onPick, compact = true)
            val words = remember(list, level, choices) {
                val previous = if (index == 0) 0 else choices[index - 1]
                if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.06f)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                words.take(6).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { word ->
                            Text(
                                word,
                                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(colors.primary.copy(alpha = 0.10f)).padding(vertical = 6.dp, horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        PromptButtons(state, level, onSave, onLater, Modifier.padding(end = 12.dp, top = 8.dp, bottom = 12.dp), compact = true)
    }
}

/** Skip for now on the left, the saving state and the Start button on the right. */
@Composable
private fun PromptButtons(state: VocabularySettingsUiState, level: Int, onSave: () -> Unit, onLater: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onLater, enabled = !state.saving, contentPadding = if (compact) PaddingValues(horizontal = 4.dp) else androidx.compose.material3.ButtonDefaults.TextButtonContentPadding) {
            Text(tr("Skip for now"), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        if (state.saving) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            if (!compact) Text(tr("Saving…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Button(
            onClick = onSave,
            enabled = state.picked != null && !state.saving,
            contentPadding = if (compact) PaddingValues(horizontal = 20.dp, vertical = 12.dp) else PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            modifier = Modifier.testTag("prompt-set-level"),
        ) {
            Text(
                if (level == 0) tr("Start from scratch") else trPlural(level, "Start with {1} word", "Start with {1} words", formatCount(level)),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** What a level means to a learner, roughly. */
private fun levelName(level: Int): String = when {
    level == 0 -> tr("Starting out")
    level <= 300 -> tr("Early beginner")
    level <= 700 -> tr("Beginner")
    level <= 1500 -> tr("Elementary")
    level <= 3000 -> tr("Intermediate")
    level <= 6000 -> tr("Upper intermediate")
    else -> tr("Advanced")
}

/** The levels named under the slider; the others are ticks only. */
private val NAMED_STOPS = setOf(0, 100, 200, 300, 500, 700, 1000, 2000, 3000, 5000, 7000, 10_000)

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
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).border(1.dp, colors.outlineVariant, shape).padding(if (compact) 16.dp else 24.dp)) {
        Text(tr("Estimated vocabulary"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatCount(level), style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = colors.primary)
            Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
            Text(trPlural(level, "word", "words"), Modifier.padding(bottom = if (compact) 8.dp else 10.dp), style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, color = colors.onSurface)
        }
        Text(levelName(level), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
        LevelSlider(choices, index, onPick, compact)
        Spacer(Modifier.height(if (compact) 16.dp else 22.dp))
        // The words a learner at this level has just reached; at 0, the first ones to learn.
        val words = remember(list, level, choices) {
            val previous = if (index == 0) 0 else choices[index - 1]
            if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.06f)).padding(if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        ) {
            Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            words.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
                    row.forEach { word ->
                        Text(
                            word,
                            Modifier.weight(1f).clip(RoundedCornerShape(50)).background(colors.primary.copy(alpha = 0.10f)).padding(vertical = if (compact) 8.dp else 10.dp, horizontal = 8.dp),
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

/** A slider that stops at each level, with ticks under it and the main levels named; a [compact] one ticks only the named levels. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelSlider(choices: List<Int>, index: Int, onPick: (Int) -> Unit, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val last = (choices.size - 1).coerceAtLeast(1)
    Column {
        Slider(
            value = index.toFloat(),
            onValueChange = { onPick(choices[it.roundToInt().coerceIn(0, choices.size - 1)]) },
            valueRange = 0f..last.toFloat(),
            steps = (choices.size - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("level-slider"),
            thumb = {
                Box(Modifier.size(THUMB).shadow(3.dp, CircleShape).clip(CircleShape).background(colors.surface).padding(3.dp).clip(CircleShape).background(colors.primary))
            },
            track = { sliderState ->
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(colors.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(sliderState.coercedValueAsFraction).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(colors.primary))
                }
            },
        )
        // Ticks and names placed where the slider stops, the track running between the thumb's centres
        // at either end. Names that would run into one another are left out, the chosen level's first.
        Layout(
            content = {
                choices.forEach { stop -> Box(Modifier.width(1.5.dp).height(10.dp).background(if (compact && stop !in NAMED_STOPS) Color.Transparent else colors.outline)) }
                choices.forEachIndexed { i, stop ->
                    Text(
                        shortCount(stop),
                        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
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
            val order = listOf(index, 0, choices.size - 1) + choices.indices.filter { choices[it] in NAMED_STOPS } + choices.indices
            for (i in order) if (i !in shown && fits(i)) shown += i
            val tickHeight = ticks.maxOf { it.height }
            val top = tickHeight + 6.dp.roundToPx()
            layout(constraints.maxWidth, top + names.maxOf { it.height }) {
                ticks.forEachIndexed { i, tick -> tick.place(inset + span * i / last - tick.width / 2, 0) }
                shown.forEach { i -> names[i].place(left(i), top) }
            }
        }
        Text(tr("Starting out"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

private val THUMB = 28.dp

/** The language's sample story coloured by the estimate: the words it knows, the new ones, and the ones outside the list. */
@Composable
internal fun PreviewCard(state: VocabularySettingsUiState, level: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val outside = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).border(1.dp, colors.outlineVariant, shape).padding(if (compact) 16.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("Reading preview"), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(tr("A sample text at this estimate"), style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        val inner = RoundedCornerShape(14.dp)
        // Beside the estimate card the text takes the height left; stacked in a scrolling page, where
        // the height is unbounded, a weight would leave it no height at all.
        val textHeight = if (width.isExpanded) Modifier.weight(1f, fill = false) else Modifier
        Box(Modifier.fillMaxWidth().then(textHeight).clip(inner).border(1.dp, colors.outlineVariant, inner).padding(if (compact) 14.dp else 20.dp)) {
            if (state.example.isEmpty()) {
                Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
            } else {
                Text(
                    buildAnnotatedString {
                        for (token in state.example) {
                            val color = when {
                                !token.isWord -> colors.onSurface
                                token.rank == null -> outside
                                token.rank <= level -> KNOWN_GREEN
                                else -> colors.primary
                            }
                            withStyle(SpanStyle(color = color)) { append(token.text) }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.45f),
                    maxLines = 9,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Three legends do not always fit one phone-wide row.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PreviewLegend(KNOWN_GREEN, tr("Known"))
            PreviewLegend(colors.primary, tr("New"))
            PreviewLegend(outside, tr("Outside frequency list"))
        }
    }
}

@Composable
private fun PreviewLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
