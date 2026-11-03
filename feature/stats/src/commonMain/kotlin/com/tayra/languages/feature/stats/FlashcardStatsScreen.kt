package com.tayra.languages.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.flashcards.CardCounts
import com.tayra.languages.core.domain.flashcards.FlashcardStatistics
import com.tayra.languages.core.domain.flashcards.FlashcardStatsService
import com.tayra.languages.core.domain.flashcards.ReviewKind
import com.tayra.languages.core.domain.flashcards.StatsPeriod
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.stats.heatLevel
import com.tayra.languages.core.ui.components.ActivityHeatmapCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.StatTile
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.shortDate
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt

data class FlashcardStatsUiState(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.MONTH,
    val languageName: String = "",
    /** Null before a language is chosen. */
    val stats: FlashcardStatistics? = null,
)

/** The flashcards of the language being learned in figures, over the period chosen. */
@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardStatsViewModel(service: FlashcardStatsService, languages: LanguageRepository, settings: SettingsRepository) : ViewModel() {
    private val period = MutableStateFlow(StatsPeriod.MONTH)

    val state: StateFlow<FlashcardStatsUiState> = combine(settings.settings.map { it.currentLanguageId }.distinctUntilChanged(), period) { id, chosen -> id to chosen }
        .mapLatest { (id, chosen) ->
            val language = languages.getById(id)
            FlashcardStatsUiState(loading = false, period = chosen, languageName = language?.name.orEmpty(), stats = language?.let { service.statistics(it.id, chosen) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlashcardStatsUiState())

    fun choose(chosen: StatsPeriod) {
        period.value = chosen
    }
}

@Composable
fun FlashcardStatsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: FlashcardStatsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = tr("Flashcard statistics"), onNavigate = onNavigate, section = NavSection.TERMS, onBack = onBack.takeIf { compact }) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) { FlashcardStatsContent(state, viewModel::choose, onNavigate) }
    }
}

@Composable
internal fun FlashcardStatsContent(state: FlashcardStatsUiState, onPeriod: (StatsPeriod) -> Unit, onNavigate: (Route) -> Unit = {}) {
    val wide = LocalWindowWidth.current.isExpanded
    ScreenHeader(
        tr("Flashcard statistics"),
        state.languageName.takeIf { it.isNotEmpty() }?.let { tr("Your flashcards in {0}.", languageInSentence(it, LanguageCase.PREPOSITIONAL)) },
        crumbs = listOf(Crumb(tr("Statistics")) { onNavigate(Route.Stats) }, Crumb(tr("Flashcard statistics"))),
    )
    val stats = state.stats
    if (stats == null || stats.counts.total == 0) {
        EmptyState(AppIcons.Style, tr("No flashcards yet"), message = tr("Words you give a status of 1 to 4 while reading get a card, and their figures show here."))
        return
    }
    SegmentedToggle(
        StatsPeriod.entries.map { Segment(it, label = it.label(), testTag = "period-${it.name}") },
        state.period,
        onPeriod,
        fill = !wide,
    )
    TodayCard(stats)
    SideBySide(wide, { FutureDueCard(stats, state.period, it) }, { CardCountsCard(stats.counts, it) })
    CalendarCard(stats)
    ReviewsCard(stats)
    AddedCard(stats)
}

private fun StatsPeriod.label(): String = when (this) {
    StatsPeriod.MONTH -> tr("1 month")
    StatsPeriod.THREE_MONTHS -> tr("3 months")
    StatsPeriod.YEAR -> tr("1 year")
    StatsPeriod.ALL -> tr("All")
}

internal fun Long.date(): LocalDate = LocalDate.fromEpochDays(toInt())

/** The colours of the kinds of card an answer was given to, as Anki colours them. */
internal fun ReviewKind.tint(): Color = when (this) {
    ReviewKind.LEARNING -> AppAccents.blue
    ReviewKind.YOUNG -> AppAccents.teal
    ReviewKind.MATURE -> AppAccents.green
    ReviewKind.RELEARNING -> AppAccents.red
}

internal fun ReviewKind.label(): String = when (this) {
    ReviewKind.LEARNING -> tr("Learning")
    ReviewKind.YOUNG -> tr("Young")
    ReviewKind.MATURE -> tr("Mature")
    ReviewKind.RELEARNING -> tr("Relearning")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayCard(s: FlashcardStatistics) {
    val today = s.todayStats
    val minutes = today.time.inWholeSeconds / 60.0
    val perCard = if (today.studied > 0) (today.time.inWholeMilliseconds / 1000.0 / today.studied).roundToInt() else 0
    val compact = LocalWindowWidth.current.isCompact
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l), maxItemsInEachRow = if (compact) 2 else 4) {
        StatTile(tr("Studied today"), formatCount(today.studied), tr("{0} min, {1} s a card", (minutes * 10).roundToInt() / 10.0, perCard), AppIcons.Style, AppAccents.blue, Modifier.weight(1f).testTag("today-studied"))
        StatTile(tr("Forgotten today"), formatCount(today.again), today.correct.rate?.let { tr("{0}% correct", (it * 100).roundToInt()) } ?: tr("Nothing answered yet"), AppIcons.Replay, AppAccents.red, Modifier.weight(1f).testTag("today-again"))
        StatTile(
            tr("Mature cards today"),
            today.mature.rate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
            tr("{0} of {1} remembered", today.mature.passed, today.mature.total),
            AppIcons.DoneAll,
            AppAccents.green,
            Modifier.weight(1f).testTag("today-mature"),
        )
        StatTile(
            tr("Due tomorrow"),
            formatCount(s.dueByDay[1] ?: 0),
            if (s.overdue > 0) trPlural(s.overdue, "{1} card overdue", "{1} cards overdue", formatCount(s.overdue)) else tr("Nothing overdue"),
            AppIcons.Schedule,
            AppAccents.purple,
            Modifier.weight(1f).testTag("today-due"),
        )
    }
    if (today.studied > 0) {
        Text(
            ReviewKind.entries.joinToString(" · ") { "${it.label()} ${today.kinds[it] ?: 0}" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("today-kinds"),
        )
    }
}

/** How many [days] go into each column, for at most [MAX_COLUMNS] of them. */
private fun columnWidth(days: Int): Int = ceil(days.toDouble() / MAX_COLUMNS).toInt().coerceAtLeast(1)

@Composable
private fun FutureDueCard(s: FlashcardStatistics, period: StatsPeriod, modifier: Modifier) {
    val horizon = period.days ?: 365
    val width = columnWidth(horizon)
    val columns = (0 until horizon step width).map { start -> listOf((start until start + width).sumOf { s.dueByDay[it] ?: 0 }) }
    val total = s.dueByDay.values.sum()
    ContentCard(
        tr("Future due"),
        tr("{0} due over the period, {1} a day on average", formatCount(total), formatCount((total.toDouble() / horizon).roundToInt())),
        icon = AppIcons.Schedule,
        modifier = modifier,
    ) {
        Bars(columns, listOf(AppAccents.purple), tr("Today"), (s.today + horizon - 1).date().shortDate(), tr("Nothing due in the period."), Modifier.testTag("future-due"))
    }
}

@Composable
private fun CardCountsCard(counts: CardCounts, modifier: Modifier) {
    val parts = listOf(
        Triple(tr("New"), counts.new, AppAccents.blue),
        Triple(tr("Learning"), counts.learning, AppAccents.orange),
        Triple(tr("Relearning"), counts.relearning, AppAccents.red),
        Triple(tr("Young"), counts.young, AppAccents.teal),
        Triple(tr("Mature"), counts.mature, AppAccents.green),
        Triple(tr("Suspended"), counts.suspended, MaterialTheme.colorScheme.outline),
    )
    ContentCard(tr("Card counts"), trPlural(counts.total, "{1} card", "{1} cards", formatCount(counts.total)), icon = AppIcons.PieChart, modifier = modifier) {
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
        Row(Modifier.fillMaxWidth().height(AppSizes.trackBold).clip(AppShapes.pill).background(track).testTag("card-counts-bar")) {
            parts.forEach { (_, n, tint) -> if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(tint)) }
        }
        Spacer(Modifier.height(AppSpacing.l))
        parts.forEach { (name, n, tint) -> LegendRow(name, n, counts.total, tint) }
    }
}

/** A share of a whole, with its colour, as a row of a card's list. */
@Composable
internal fun LegendRow(name: String, n: Int, total: Int, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(tint)
        Spacer(Modifier.width(AppSpacing.m))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (total > 0) Text("${(n * 100.0 / total).roundToInt()}%", Modifier.padding(end = AppSpacing.l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatCount(n), style = AppText.labelStrong)
    }
}

@Composable
private fun CalendarCard(s: FlashcardStatistics) {
    val today = s.today.date()
    val days = s.calendar.mapKeys { it.key.date() }
    fun shown(start: LocalDate) = days.filterKeys { it in start..today }
    ActivityHeatmapCard(
        title = tr("Calendar"),
        subtitle = { start ->
            val count = shown(start).size
            trPlural(count, "{1} day with reviews since {2}", "{1} days with reviews since {2}", formatCount(count), start.shortDate(today))
        },
        today = today,
        levels = { start ->
            val inView = shown(start)
            val max = inView.values.maxOrNull() ?: 0
            inView.mapValues { heatLevel(it.value, max) }
        },
        fewer = tr("Fewer reviews"),
        more = tr("More reviews"),
        color = AppAccents.purple,
        describe = { date -> days[date]?.let { trPlural(it, "{1} answer", "{1} answers", formatCount(it)) } ?: tr("No reviews") },
    )
}

/** The days of the period in columns of a day or more each, oldest first. */
private fun FlashcardStatistics.columns(): List<LongRange> {
    val days = (today - firstDay + 1).toInt().coerceAtLeast(1)
    val width = columnWidth(days)
    return (firstDay..today step width.toLong()).map { it..minOf(it + width - 1, today) }
}

@Composable
private fun ReviewsCard(s: FlashcardStatistics) {
    val columns = s.columns()
    val kinds = ReviewKind.entries
    val values = columns.map { range -> kinds.map { kind -> range.sumOf { s.reviewsByDay[it]?.kinds?.get(kind) ?: 0 } } }
    val total = s.reviewsByDay.values.sumOf { it.total }
    val minutes = s.reviewsByDay.values.sumOf { it.time.inWholeSeconds } / 60
    ContentCard(tr("Reviews"), tr("{0} answers in {1} minutes", formatCount(total), formatCount(minutes.toInt())), icon = AppIcons.BarChart) {
        Bars(values, kinds.map { it.tint() }, s.firstDay.date().shortDate(s.today.date()), s.today.date().shortDate(), tr("No answers in the period."), Modifier.testTag("reviews-by-day"))
        Legend(kinds.map { it.label() to it.tint() })
    }
}

@Composable
private fun AddedCard(s: FlashcardStatistics) {
    val columns = s.columns()
    val values = columns.map { range -> listOf(range.sumOf { s.added[it] ?: 0 }) }
    val total = s.added.values.sum()
    ContentCard(tr("Added"), trPlural(total, "{1} card added in the period", "{1} cards added in the period", formatCount(total)), icon = AppIcons.PageAdd) {
        Bars(values, listOf(AppAccents.blue), s.firstDay.date().shortDate(s.today.date()), s.today.date().shortDate(), tr("No cards added in the period."), Modifier.testTag("added-by-day"))
    }
}

/** What the colours of a chart stand for, in a row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Legend(parts: List<Pair<String, Color>>) {
    FlowRow(Modifier.fillMaxWidth().padding(top = AppSpacing.s), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        parts.forEach { (name, tint) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                StatusDot(tint)
                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Columns of parts stacked from the bottom, one value for each of [colors], oldest first, with the
 * tallest column's total above them and [from] and [to] under them; with nothing to draw, [empty]
 * says so in place of an empty chart.
 */
@Composable
internal fun Bars(columns: List<List<Int>>, colors: List<Color>, from: String, to: String, empty: String, modifier: Modifier = Modifier) {
    val max = columns.maxOfOrNull { it.sum() } ?: 0
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
    if (max == 0) {
        Text(empty, modifier, style = MaterialTheme.typography.bodyMedium, color = muted)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Most: {0}", formatCount(max)), style = MaterialTheme.typography.bodySmall, color = muted)
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            val slot = size.width / columns.size
            val bar = slot * 0.68f
            val base = 2.dp.toPx()
            drawRect(track, Offset(0f, size.height - base), Size(size.width, base))
            columns.forEachIndexed { i, parts ->
                val x = i * slot + (slot - bar) / 2
                var top = size.height
                parts.forEachIndexed { p, value ->
                    if (value > 0) {
                        val h = (size.height * value / max).coerceAtLeast(2f)
                        top -= h
                        drawRoundRect(colors[p % colors.size], Offset(x, top), Size(bar, h), CornerRadius(bar / 6))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(from, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = muted)
            Text(to, style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

/** The most columns a chart over time is drawn with; longer periods put several days in one. */
private const val MAX_COLUMNS = 90
internal val CHART_HEIGHT = 140.dp
