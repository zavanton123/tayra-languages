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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.graphics.SolidColor
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
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private val WordKnowledge.label: String
    get() = when (this) {
        WordKnowledge.KNOWN -> "Known"
        WordKnowledge.LEARNING -> "Learning"
        WordKnowledge.IGNORED -> "Ignored"
        WordKnowledge.NEW -> "New"
    }

/** The colour a status is shown in, on the filter chips and the band bars. */
@Composable
private fun WordKnowledge.tint(): Color {
    val statuses = TayraTheme.current.statusColors
    return when (this) {
        WordKnowledge.KNOWN -> statuses.background(TermStatus.WELL_KNOWN)
        WordKnowledge.LEARNING -> statuses.background(TermStatus.NEW_1)
        WordKnowledge.IGNORED -> statuses.background(TermStatus.IGNORED)
        WordKnowledge.NEW -> MaterialTheme.colorScheme.primary
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
        topBar = { AppTopBar(title = "Word frequency", onNavigate = onNavigate, section = NavSection.TERMS) },
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
                VerticalDivider()
                TermPanel(state.languageId, selected, onSelect = viewModel::select, onNavigate = onNavigate, modifier = Modifier.width(440.dp).fillMaxHeight())
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
    val side = if (compact) 16.dp else 32.dp
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
            LazyColumn(
                Modifier.weight(1f).testTag("frequency-list"),
                state = listState,
                contentPadding = PaddingValues(start = side, end = if (index) 8.dp else side, top = if (compact) 16.dp else 24.dp, bottom = 32.dp),
            ) {
                item(key = "header") { Header(state, onToggle, onSearch, onSettings) }
                when {
                    overview == null -> item(key = "none") {
                        Notice(
                            "No frequency list yet",
                            if (state.languageName.isEmpty()) "Choose a language to learn to see its most common words."
                            else "There is no word frequency list for ${state.languageName}. Lists come with the languages the app offers to learn.",
                        )
                    }
                    bands.isEmpty() -> item(key = "empty") { Notice("No words match", "Try another search, or show more kinds of words.") }
                    else -> {
                        bands.forEachIndexed { i, band ->
                            item(key = "band-${band.index}") { BandHeader(
                                    overview.bands[band.index],
                                    isLevel = band.index == state.levelBand,
                                    // As in Language Reactor, a band's button puts the level on the line above it:
                                    // the reader knows every word before this band.
                                    levelAbove = overview.bands[band.index].firstRank - 1,
                                    currentLevel = state.level,
                                    saving = state.savingLevel,
                                    onSetLevel = { asking = overview.bands[band.index].firstRank - 1 },
                                )
                            }
                            items(rows[i], key = { "row-${it.first().word.rank}" }) { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { WordCell(it, selected = it.word.word == state.selected, onOpen, translate, Modifier.weight(1f)) }
                                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        item(key = "credit") {
                            Text(
                                "Word counts: ${overview.source}. Words are grouped with their forms using Wiktionary data (CC BY-SA 4.0) and the simplemma lemmatizer.",
                                Modifier.padding(top = 28.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: WordFrequencyUiState, onToggle: (WordKnowledge) -> Unit, onSearch: (String) -> Unit, onSettings: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val overview = state.overview
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Word frequency", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (overview == null) "The most common words of a language, from the most used down."
                else "The ${formatCount(overview.words.size)} most common ${overview.languageName} words, from the most used down. Click a word to save it or change its status.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        if (overview == null) return@Column
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (state.level > 0) "Your vocabulary level: ${formatCount(state.level)}" else "No vocabulary level set yet: use a band's button, or",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (onSettings != null) {
                Text(
                    if (state.level > 0) "Change" else "choose one",
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSettings).padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
            }
            if (state.savingLevel) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Saving the level…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            SearchBox(state.search, onSearch, if (compact) Modifier.fillMaxWidth() else Modifier.width(320.dp))
            WordKnowledge.entries.forEach { knowledge ->
                KnowledgeChip(knowledge, overview.count(knowledge), selected = knowledge in state.shown) { onToggle(knowledge) }
            }
        }
    }
}

@Composable
private fun KnowledgeChip(knowledge: WordKnowledge, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = knowledge.tint()
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(40.dp).clip(shape)
            .background(if (selected) tint.copy(alpha = 0.16f) else colors.surface)
            .border(1.dp, if (selected) tint else colors.outlineVariant, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp).testTag("chip-${knowledge.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (selected) tint else colors.outlineVariant))
        Text(knowledge.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (selected) colors.onSurface else colors.onSurfaceVariant)
        Text(formatCount(count), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (selected) colors.onSurface else colors.onSurfaceVariant)
    }
}

/** The band's ranks, how many of its words are known or being learned, and whether the reader's level is here. */
@Composable
private fun BandHeader(band: FrequencyBand, isLevel: Boolean, levelAbove: Int, currentLevel: Int, saving: Boolean, onSetLevel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val known = band.count(WordKnowledge.KNOWN)
    val learning = band.count(WordKnowledge.LEARNING)
    val ignored = band.count(WordKnowledge.IGNORED)
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ranks ${band.firstRank}–${band.lastRank}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (isLevel) {
                Text(
                    "Your level",
                    Modifier.clip(RoundedCornerShape(50)).background(colors.primary).padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary,
                )
            }
            if (levelAbove != currentLevel) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).border(1.dp, colors.outlineVariant, RoundedCornerShape(50))
                        .clickable(enabled = !saving, onClick = onSetLevel).padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("set-level-$levelAbove"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(AppIcons.DoneAll, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text(
                        if (compact) "Level ${formatCount(levelAbove)}" else "Set level to ${formatCount(levelAbove)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (saving) colors.onSurfaceVariant else colors.primary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (!compact) {
                Text(
                    listOfNotNull("$known known", "$learning learning".takeIf { learning > 0 }, "$ignored ignored".takeIf { ignored > 0 }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        // The band's words by status, known first.
        Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.onSurface.copy(alpha = 0.07f))) {
            for ((knowledge, n) in listOf(WordKnowledge.KNOWN to known, WordKnowledge.LEARNING to learning, WordKnowledge.IGNORED to ignored)) {
                if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(knowledge.tint()))
            }
            val rest = band.words.size - known - learning - ignored
            if (rest > 0) Spacer(Modifier.weight(rest.toFloat()))
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordCell(word: RankedWord, selected: Boolean, onOpen: (String) -> Unit, translate: suspend (String) -> String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val statuses = TayraTheme.current.statusColors
    val tooltip = rememberTooltipState(isPersistent = true)
    var translation by remember(word.word.word) { mutableStateOf<String?>(null) }
    var looked by remember(word.word.word) { mutableStateOf(false) }
    LaunchedEffect(tooltip.isVisible) {
        if (tooltip.isVisible && !looked) {
            translation = translate(word.word.word)
            looked = true
        }
    }
    val learning = word.knowledge == WordKnowledge.LEARNING
    Box(modifier.padding(vertical = 3.dp, horizontal = 2.dp)) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                RichTooltip(title = { Text("${word.word.word}  ·  #${word.word.rank}") }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (looked) translation ?: "No translation found" else "Looking up…", fontWeight = FontWeight.Medium)
                        val others = word.word.forms.filter { it != word.word.key }.take(8)
                        if (others.isNotEmpty()) Text("Also: ${others.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        Text(statusLabel(word.status), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            state = tooltip,
        ) {
            Text(
                word.word.word,
                Modifier.clip(RoundedCornerShape(6.dp))
                    .then(if (learning) Modifier.background(statuses.background(word.status)) else Modifier)
                    .then(if (selected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(6.dp)) else Modifier)
                    .clickable { onOpen(word.word.word) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .widthIn(min = 24.dp)
                    .testTag("word-${word.word.word}"),
                style = MaterialTheme.typography.bodyLarge,
                color = when (word.knowledge) {
                    WordKnowledge.KNOWN -> colors.onSurface
                    WordKnowledge.LEARNING -> if (statuses.onHighlight == Color.Unspecified) colors.onSurface else statuses.onHighlight
                    WordKnowledge.IGNORED -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                    WordKnowledge.NEW -> colors.primary
                },
                fontWeight = if (word.knowledge == WordKnowledge.NEW) FontWeight.Medium else FontWeight.Normal,
                textDecoration = if (word.knowledge == WordKnowledge.IGNORED) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun statusLabel(status: TermStatus): String = when (status) {
    TermStatus.UNKNOWN -> "Not saved yet"
    TermStatus.WELL_KNOWN -> "Known"
    TermStatus.IGNORED -> "Ignored"
    else -> "Learning, level ${status.value}"
}

/** Jumps to a rank: 1, every hundred up to 900, then every thousand. */
@Composable
private fun RankIndex(wordCount: Int, modifier: Modifier, onJump: (rank: Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val marks = (listOf(1) + (100..900 step 100) + (1000..10_000 step 1000)).filter { it <= wordCount }
    Column(modifier.padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        marks.forEach { mark ->
            Text(
                if (mark >= 1000) "${mark / 1000}k" else "$mark",
                Modifier.clip(RoundedCornerShape(6.dp)).clickable { onJump(if (mark == 1) 1 else mark + 1) }.padding(horizontal = 8.dp, vertical = 3.dp).testTag("rank-$mark"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag("frequency-search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Find a word", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun Notice(title: String, text: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp)).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.BarChart, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}
