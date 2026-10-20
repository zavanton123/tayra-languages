package com.tayra.languages.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.frequency.FrequencyBand
import com.tayra.languages.core.domain.frequency.RankedWord
import com.tayra.languages.core.domain.frequency.WordFrequencyOverview
import com.tayra.languages.core.domain.frequency.WordKnowledge
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppRichTooltip
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ChipLeading
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.TagStyle
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlin.random.Random
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val WordKnowledge.label: String
    get() = when (this) {
        WordKnowledge.KNOWN -> tr("Known")
        WordKnowledge.LEARNING -> tr("Learning")
        WordKnowledge.IGNORED -> tr("Ignored")
        WordKnowledge.NEW -> tr("New")
    }

/** The colour a status is shown in, on the filter chips and the band bars. */
@Composable
private fun WordKnowledge.tint(): Color {
    val statuses = TayraTheme.current.statusColors
    return when (this) {
        WordKnowledge.KNOWN -> statuses.background(TermStatus.WELL_KNOWN)
        WordKnowledge.LEARNING -> statuses.background(TermStatus.NEW_1)
        WordKnowledge.IGNORED -> statuses.background(TermStatus.IGNORED)
        WordKnowledge.NEW -> statuses.background(TermStatus.UNKNOWN)
    }
}

/** The most common words of the language being learned, a hundred to a band, coloured by how far the reader has got with each. */
@Composable
fun WordFrequencyScreen(onNavigate: (Route) -> Unit, viewModel: WordFrequencyViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expanded = LocalWindowWidth.current.isExpanded
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    Scaffold(
        topBar = { AppTopBar(title = tr("Word frequency"), onNavigate = onNavigate, section = NavSection.TERMS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Row(Modifier.padding(padding).fillMaxSize()) {
            WordFrequencyContent(
                state,
                onToggle = viewModel::toggle,
                onSearch = viewModel::setSearch,
                // Wide windows edit the word beside the list; narrow ones open it on its own screen.
                onOpen = { word -> if (expanded) viewModel.select(word) else onNavigate(Route.EditTermByText(state.languageId, word)) },
                translate = viewModel::translate,
                onSetLevel = viewModel::setLevel,
                onSettings = { onNavigate(Route.VocabularySettings) },
                modifier = Modifier.weight(1f),
            )
            val selected = state.selected
            if (expanded && selected != null) {
                AppVerticalDivider()
                TermPanel(state.languageId, selected, onSelect = viewModel::select, onNavigate = onNavigate, modifier = Modifier.width(TERM_PANEL_WIDTH).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun TermPanel(languageId: Long, word: String, onSelect: (String?) -> Unit, onNavigate: (Route) -> Unit, modifier: Modifier) {
    // Each opening gets a new form, since a form may have moved on to a looked-up word.
    val opening = remember(word) { Random.nextLong() }
    val form = koinViewModel<TermFormViewModel>(key = "frequency-term-$word-$opening") { parametersOf(TermFormKey.ByText(languageId, word)) }
    CollectEvents(form.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> if (!event.keepOpen) onSelect(null)
            TermFormEvent.Deleted -> onSelect(null)
            is TermFormEvent.OpenParent -> onSelect(event.text)
            is TermFormEvent.OpenTerm -> onSelect(event.text)
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surface)) {
        TermFormPanel(
            viewModel = form,
            modifier = Modifier.fillMaxSize(),
            embedded = true,
            onClose = { form.flush(); onSelect(null) },
            onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
            onOpenExamples = { id, text -> onNavigate(Route.Examples(id, text)) },
            onManageDictionaries = { id -> onNavigate(Route.ManageDictionaries(id)) },
        )
    }
}

@Composable
internal fun WordFrequencyContent(
    state: WordFrequencyUiState,
    onToggle: (WordKnowledge) -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (word: String) -> Unit,
    translate: suspend (String) -> String?,
    onSetLevel: (Int) -> Unit = {},
    onSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var asking by remember { mutableStateOf<Int?>(null) }
    asking?.let { to -> LevelConfirmDialog(from = state.level, to = to, onConfirm = { asking = null; onSetLevel(to) }, onDismiss = { asking = null }) }
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val overview = state.overview
    val bands = remember(state.overview, state.shown, state.search) { state.visibleBands }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val side = if (compact) AppSpacing.l else AppSpacing.xxxl
    BoxWithConstraints(modifier.fillMaxSize()) {
        val index = overview != null && width.isExpanded
        val usable = maxWidth - side * 2 - (if (index) INDEX_WIDTH else 0.dp)
        val columns = (usable / CELL_WIDTH).toInt().coerceIn(2, 8)
        val rows = remember(bands, columns) { bands.map { columnRows(it.words, columns) } }
        // Where each band starts in the list: the header, then per band its title and its rows.
        val bandItems = remember(rows) {
            var at = 1
            bands.indices.associate { i -> (bands[i].index to at).also { at += 1 + rows[i].size } }
        }
        Row(Modifier.fillMaxSize()) {
            ScrollList(
                Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(start = side, end = if (index) AppSpacing.s else side, top = if (compact) AppSpacing.l else AppSpacing.xxl, bottom = AppSpacing.xxxl),
                listModifier = Modifier.testTag("frequency-list"),
            ) {
                item(key = "header") { Header(state, onToggle, onSearch, onSettings) }
                when {
                    overview == null -> item(key = "none") {
                        EmptyState(
                            AppIcons.BarChart,
                            tr("No frequency list yet"),
                            Modifier.padding(top = AppSpacing.s),
                            message = if (state.languageName.isEmpty()) tr("Choose a language to learn to see its most common words.")
                            else tr("There is no word frequency list for {0}. Lists come with the languages the app offers to learn.", languageInSentence(state.languageName, LanguageCase.GENITIVE)),
                        )
                    }
                    bands.isEmpty() -> item(key = "empty") { EmptyState(AppIcons.BarChart, tr("No words match"), Modifier.padding(top = AppSpacing.s), message = tr("Try another search, or show more kinds of words.")) }
                    else -> {
                        bands.forEachIndexed { i, band ->
                            item(key = "band-${band.index}") { BandHeader(
                                    overview.bands[band.index],
                                    isLevel = band.index == state.levelBand,
                    levelSet = state.level > 0,
                                    // As in Language Reactor, a band's button puts the level on the line above it:
                                    // the reader knows every word before this band.
                                    levelAbove = overview.bands[band.index].firstRank - 1,
                                    currentLevel = state.level,
                                    saving = state.savingLevel,
                                    onSetLevel = { asking = overview.bands[band.index].firstRank - 1 },
                                )
                            }
                            items(rows[i], key = { "row-${it.first().word.rank}" }) { row -> Column(itemMotion()) {
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { WordCell(it, selected = it.word.word == state.selected, onOpen, translate, Modifier.weight(1f)) }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }}
                        }
                        item(key = "credit") {
                            Text(
                                tr("Word counts: {0}. Words are grouped with their forms using Wiktionary data (CC BY-SA 4.0) and the simplemma lemmatizer.", overview.source),
                                Modifier.padding(top = AppSpacing.xxxl),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (index && bands.isNotEmpty()) {
                RankIndex(overview!!.words.size, Modifier.width(INDEX_WIDTH).fillMaxHeight()) { rank ->
                    // The band holding the rank, or the next one the filters left.
                    val band = (rank - 1) / WordFrequencyOverview.BAND_SIZE
                    val target = bandItems.entries.firstOrNull { it.key >= band } ?: bandItems.entries.last()
                    scope.launch { listState.animateScrollToItem(target.value) }
                }
            }
        }
        // Without the rail, a button opens the bands to jump to, so rank 5,000 is not fifty bands of scrolling.
        if (!index && overview != null && bands.size > 1) {
            var jumping by remember { mutableStateOf(false) }
            SecondaryButton(
                tr("Jump to rank"),
                onClick = { jumping = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(AppSpacing.l).testTag("jump-to-rank"),
                icon = AppIcons.SwapVert,
            )
            if (jumping) {
                PickerSheet(
                    tr("Jump to rank"),
                    bands.map { overview.bands[it.index] },
                    selected = null,
                    label = { tr("Ranks {0}–{1}", it.firstRank, it.lastRank) },
                    onDismiss = { jumping = false },
                    onPick = { band ->
                        jumping = false
                        bandItems[band.index]?.let { scope.launch { listState.animateScrollToItem(it) } }
                    },
                )
            }
        }
    }
}

/**
 * A band's words in alphabetical order down each column, as rows of [columns] cells: the rank
 * already says how common a word is, and a band read like an index is easier to scan.
 */
private fun columnRows(words: List<RankedWord>, columns: Int): List<List<RankedWord>> {
    val sorted = words.sortedWith(compareBy({ sortKey(it.word.key) }, { it.word.word }))
    val height = (sorted.size + columns - 1) / columns
    return List(height) { row -> (0 until columns).mapNotNull { column -> sorted.getOrNull(column * height + row) } }
}

/** The word with accents taken off, so "água" sorts with the a's and "ônibus" with the o's. */
private fun sortKey(word: String): String = word.lowercase().map { BASE_LETTERS[it] ?: it }.joinToString("")

private val BASE_LETTERS: Map<Char, Char> = buildMap {
    for ((base, accented) in listOf('a' to "áàâãäå", 'e' to "éèêë", 'i' to "íìîï", 'o' to "óòôõö", 'u' to "úùûü", 'c' to "ç", 'n' to "ñ", 'y' to "ýÿ")) {
        accented.forEach { put(it, base) }
    }
}

private val CELL_WIDTH = 168.dp
private val INDEX_WIDTH = 56.dp
private val WORD_MIN_WIDTH = 24.dp
private val SEARCH_WIDTH = 320.dp
private val TERM_PANEL_WIDTH = 440.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: WordFrequencyUiState, onToggle: (WordKnowledge) -> Unit, onSearch: (String) -> Unit, onSettings: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val overview = state.overview
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        ScreenHeader(
            tr("Word frequency"),
            if (overview == null) tr("The most common words of a language, from the most used down.")
            // Bands run from the most used down; inside a band the words are in alphabetical order.
            else if (compact) tr("The {0} most common {1} words in bands of 100, A–Z within each band. Tap a word to save it or change its status.", formatCount(overview.words.size), tr(overview.languageName))
            else tr("The {0} most common {1} words in bands of 100, A–Z within each band. Click a word to save it or change its status.", formatCount(overview.words.size), tr(overview.languageName)),
        )
        if (overview == null) return@Column
        // The link keeps its words together and goes under the sentence when the line is full.
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(
                if (state.level > 0) tr("Your vocabulary level: {0}", formatCount(state.level)) else tr("No vocabulary level yet. The one shown is estimated from your words; mark the band you know up to, or"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (onSettings != null) {
                LinkText(if (state.level > 0) tr("Change") else tr("choose one"), onSettings, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.savingLevel) {
                AppSpinner(size = SpinnerSize.Small)
                Text(tr("Saving the level…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m), itemVerticalAlignment = Alignment.CenterVertically) {
            SearchField(state.search, onSearch, tr("Find a word"), if (compact) Modifier.fillMaxWidth() else Modifier.width(SEARCH_WIDTH), fieldModifier = Modifier.testTag("frequency-search"))
            WordKnowledge.entries.forEach { knowledge ->
                KnowledgeChip(knowledge, overview.count(knowledge), selected = knowledge in state.shown) { onToggle(knowledge) }
            }
        }
    }
}

@Composable
private fun KnowledgeChip(knowledge: WordKnowledge, count: Int, selected: Boolean, onClick: () -> Unit) {
    SelectableChip(knowledge.label, selected, onClick, Modifier.testTag("chip-${knowledge.name}"), tint = knowledge.tint(), count = count, leading = ChipLeading.Toggle)
}

/** The band's ranks, how many of its words are known or being learned, and whether the reader's level is here. */
@Composable
private fun BandHeader(band: FrequencyBand, isLevel: Boolean, levelSet: Boolean, levelAbove: Int, currentLevel: Int, saving: Boolean, onSetLevel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val known = band.count(WordKnowledge.KNOWN)
    val learning = band.count(WordKnowledge.LEARNING)
    val ignored = band.count(WordKnowledge.IGNORED)
    Column(Modifier.fillMaxWidth().padding(top = AppSpacing.xxxl, bottom = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(tr("Ranks {0}–{1}", band.firstRank, band.lastRank), style = AppText.itemTitle)
            if (isLevel) {
                // Until a level is saved, the band shown is only a guess from the words already known.
                Tag(if (levelSet) tr("Your level") else tr("Estimated level"), size = TagSize.Medium, style = if (levelSet) TagStyle.Solid else TagStyle.Soft)
            }
            // The first band's button would only clear the level, so it has none.
            if (levelAbove > 0 && levelAbove != currentLevel) {
                SecondaryButton(
                    if (compact) tr("Known up to here") else tr("Set level to {0}", formatCount(levelAbove)),
                    onClick = onSetLevel,
                    modifier = Modifier.testTag("set-level-$levelAbove"),
                    icon = AppIcons.DoneAll,
                    enabled = !saving,
                    size = ButtonSize.Compact,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!compact) {
                Text(
                    listOfNotNull(tr("{0} known", known), tr("{0} learning", learning).takeIf { learning > 0 }, tr("{0} ignored", ignored).takeIf { ignored > 0 }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        // The band's words by status, known first.
        Row(Modifier.fillMaxWidth().height(AppSizes.track).clip(AppShapes.tiny).background(colors.onSurface.copy(alpha = AppAlpha.wash))) {
            for ((knowledge, n) in listOf(WordKnowledge.KNOWN to known, WordKnowledge.LEARNING to learning, WordKnowledge.IGNORED to ignored)) {
                if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(knowledge.tint()))
            }
            val rest = band.words.size - known - learning - ignored
            if (rest > 0) Spacer(Modifier.weight(rest.toFloat()))
        }
        AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.half))
    }
}

@Composable
private fun WordCell(word: RankedWord, selected: Boolean, onOpen: (String) -> Unit, translate: suspend (String) -> String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val statuses = TayraTheme.current.statusColors
    var translation by remember(word.word.word) { mutableStateOf<String?>(null) }
    var looked by remember(word.word.word) { mutableStateOf(false) }
    Box(modifier.padding(vertical = AppSpacing.xs, horizontal = AppSpacing.xxs)) {
        AppRichTooltip(
            "${word.word.word}  ·  #${word.word.rank}",
            persistent = true,
            body = {
                // The body is composed only while the tooltip shows, so the word is looked up on its first hover.
                LaunchedEffect(Unit) {
                    if (!looked) {
                        translation = translate(word.word.word)
                        looked = true
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(if (looked) translation ?: tr("No translation found") else tr("Looking up…"), style = AppText.label)
                    val others = word.word.forms.filter { it != word.word.key }.take(8)
                    if (others.isNotEmpty()) Text(tr("Also: {0}", others.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
                    Text(statusLabel(word.status), style = MaterialTheme.typography.bodySmall)
                }
            },
        ) {
            Text(
                word.word.word,
                Modifier.clip(AppShapes.small)
                    // Each word on its status's colour, as the legend and the reader show it.
                    .background(
                        when (word.knowledge) {
                            WordKnowledge.LEARNING -> statuses.background(word.status)
                            WordKnowledge.KNOWN -> statuses.background(TermStatus.WELL_KNOWN).copy(alpha = AppAlpha.line)
                            WordKnowledge.NEW -> statuses.background(TermStatus.UNKNOWN)
                            WordKnowledge.IGNORED -> Color.Transparent
                        },
                    )
                    .then(if (selected) Modifier.border(AppBorders.selected(), AppShapes.small) else Modifier)
                    .clickable { onOpen(word.word.word) }
                    .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs)
                    .widthIn(min = WORD_MIN_WIDTH)
                    .testTag("word-${word.word.word}"),
                style = MaterialTheme.typography.bodyLarge,
                color = when (word.knowledge) {
                    WordKnowledge.IGNORED -> colors.onSurfaceVariant.copy(alpha = AppAlpha.muted)
                    WordKnowledge.KNOWN -> colors.onSurface
                    else -> if (statuses.onHighlight == Color.Unspecified) colors.onSurface else statuses.onHighlight
                },
                textDecoration = if (word.knowledge == WordKnowledge.IGNORED) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun statusLabel(status: TermStatus): String = when (status) {
    TermStatus.UNKNOWN -> tr("Not saved yet")
    TermStatus.WELL_KNOWN -> tr("Known")
    TermStatus.IGNORED -> tr("Ignored")
    else -> tr("Learning, level {0}", status.value)
}

/** Jumps to a rank: 1, every hundred up to 900, then every thousand. */
@Composable
private fun RankIndex(wordCount: Int, modifier: Modifier, onJump: (rank: Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val marks = (listOf(1) + (100..900 step 100) + (1000..10_000 step 1000)).filter { it <= wordCount }
    // The marks share the rail's height, so each is as big a target as the window allows.
    Column(modifier.padding(vertical = AppSpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
        marks.forEach { mark ->
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(AppShapes.small).clickable { onJump(if (mark == 1) 1 else mark + 1) }.testTag("rank-$mark"),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (mark >= 1000) "${mark / 1000}k" else "$mark", style = AppText.label, color = colors.onSurfaceVariant)
            }
        }
    }
}
