package com.tayra.languages.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.MarkedText
import com.tayra.languages.core.ui.components.pushFill
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppSlider
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

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
    val later = { viewModel.forgetPick(); onClosed() }
    // On a phone the prompt is a bottom sheet with the essentials; elsewhere a floating dialog with the cards.
    if (LocalWindowWidth.current.isCompact) {
        PhonePromptSheet(ready, state, onPick = viewModel::pick, onSave = viewModel::save, onLater = later)
        return
    }
    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.testTag("vocabulary-level-prompt"), maxWidth = 1180.dp, dismissible = false) {
        if (!ready) {
            Box(Modifier.fillMaxWidth().heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
            return@AppDialogSurface
        }
        VocabularyLevelPromptContent(state, onPick = viewModel::pick, onSave = viewModel::save, onLater = later)
    }
}

/**
 * The prompt on a phone: a bottom sheet with the question, the estimate and its slider, and the
 * sample text coloured by it; swiping it away puts the question off, like Skip for now.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhonePromptSheet(ready: Boolean, state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onLater, modifier = Modifier.testTag("vocabulary-level-prompt")) {
        val list = state.list
        if (!ready || list == null) {
            Box(Modifier.fillMaxWidth().heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
            return@AppBottomSheet
        }
        val level = state.picked ?: state.level
        val choices = remember(list) { VocabularyLevelService.choices(list.words.size) }
        val index = choices.indexOf(level).coerceAtLeast(0)
        Column(Modifier.padding(horizontal = AppSpacing.xl)) {
            // The close button shares the title's row, so the sheet starts with the question.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            }
            Spacer(Modifier.height(AppSpacing.s))
            Text(tr("Choose an estimate. Words below this level will start as known. You can change it later in Settings → Vocabulary."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(AppSpacing.xl))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatCount(level), style = AppText.figure, color = colors.primary)
                Spacer(Modifier.width(AppSpacing.m))
                Text(trPlural(level, "word", "words"), Modifier.padding(bottom = AppSpacing.s), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
            }
            Text(levelName(level), style = AppText.sectionTitle)
            Spacer(Modifier.height(AppSpacing.s))
            // Four landmarks are all a phone's width can name.
            LevelSlider(choices, index, onPick, named = landmarks(choices), sparse = true, caption = false)
            Spacer(Modifier.height(AppSpacing.m))
            // Words at the level, to judge it by, as on the wider screens.
            val words = remember(list, level, choices) {
                val previous = if (index == 0) 0 else choices[index - 1]
                if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
            }
            if (words.isNotEmpty()) {
                Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = AppText.smallTitle)
                Spacer(Modifier.height(AppSpacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    words.forEach { Tag(it, colors.primary, size = TagSize.Medium) }
                }
                Spacer(Modifier.height(AppSpacing.l))
            }
            Column(
                Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                Text(tr("Example text"), style = AppText.smallTitle)
                if (state.example.isEmpty()) {
                    Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
                } else {
                    Text(
                        colouredExample(state, level),
                        style = AppText.emphasis.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(AppSpacing.l))
        }
        AppDivider()
        PromptButtons(state, level, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m), compact = true)
    }
}

/** The sample text with each word in the colour of its standing at [level]: known, new, or outside the list. */
@Composable
internal fun colouredExample(state: VocabularySettingsUiState, level: Int) = buildAnnotatedString {
    // As the reader draws a page: known words plain, new ones on the unknown words' highlight.
    val colors = MaterialTheme.colorScheme
    val statuses = TayraTheme.current.statusColors
    val outside = colors.onSurfaceVariant.copy(alpha = AppAlpha.half)
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

@Composable
internal fun VocabularyLevelPromptContent(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val list = state.list ?: return
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val level = state.picked ?: state.level
    Column {
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(start = AppSpacing.huge, end = AppSpacing.xxxl, top = AppSpacing.xxxl, bottom = AppSpacing.xxl)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), style = AppText.screenTitle)
                    Text(
                        tr("Choose an estimate. Words below this level will start as known. You can change it later in Settings → Vocabulary."),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving)
            }
            Spacer(Modifier.height(AppSpacing.xxl))
            if (width.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
                    EstimateCard(list, level, onPick, Modifier.weight(1f).fillMaxHeight())
                    PreviewCard(state, level, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                EstimateCard(list, level, onPick, Modifier.fillMaxWidth())
                Spacer(Modifier.height(AppSpacing.xl))
                PreviewCard(state, level, Modifier.fillMaxWidth())
            }
        }
        AppDivider()
        PromptButtons(state, level, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xl))
    }
}

/** Skip for now on the left, the saving state and the Start button on the right. */
@Composable
private fun PromptButtons(state: VocabularySettingsUiState, level: Int, onSave: () -> Unit, onLater: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        QuietButton(onClick = onLater, enabled = !state.saving, size = if (compact) ButtonSize.Compact else ButtonSize.Regular) {
            Text(tr("Skip for now"), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        if (state.saving) {
            AppSpinner(size = SpinnerSize.Small)
            if (!compact) Text(tr("Saving…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        PrimaryButton(
            onClick = onSave,
            enabled = state.picked != null && !state.saving,
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
private val LOADING_HEIGHT = 240.dp

/** The language's sample story coloured by the estimate: the words it knows, the new ones, and the ones outside the list. */
@Composable
internal fun PreviewCard(state: VocabularySettingsUiState, level: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val outside = colors.onSurfaceVariant.copy(alpha = AppAlpha.half)
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
            PreviewLegend(TayraTheme.current.statusColors.background(TermStatus.UNKNOWN), tr("Unknown"))
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
