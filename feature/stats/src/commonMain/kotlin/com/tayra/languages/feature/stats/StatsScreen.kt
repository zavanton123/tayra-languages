package com.tayra.languages.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.ReadingStatsSummary
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.stats.ChartPoint
import com.tayra.languages.core.domain.stats.LanguageOverview
import com.tayra.languages.core.domain.stats.heatLevel
import com.tayra.languages.core.ui.components.StatTile
import com.tayra.languages.core.ui.components.ActivityHeatmapCard
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.TableHeader
import com.tayra.languages.core.ui.components.TableHeaderCell
import com.tayra.languages.core.ui.components.animatedCount
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.shortDate
import com.tayra.languages.core.ui.components.shortDateWithYear
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
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

data class StatsUiState(
    val loading: Boolean = true,
    /** The language being learned; null before one is chosen. */
    val overview: LanguageOverview? = null,
    /** Words read in every language. */
    val summary: ReadingStatsSummary? = null,
)

/** The statistics of the language being learned, and the words read in all of them. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(statsService: StatsService, languages: LanguageRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<StatsUiState> = settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
        .mapLatest { id ->
            val language = languages.getById(id)
            StatsUiState(loading = false, overview = language?.let { statsService.overview(it.id, it.name) }, summary = statsService.summary())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

@Composable
fun StatsScreen(onNavigate: (Route) -> Unit, viewModel: StatsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = tr("Statistics"), onNavigate = onNavigate, section = NavSection.TERMS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) { StatsContent(state, onNavigate) }
    }
}

private val seriesColors = AppAccents.sequence

@Composable
internal fun StatsContent(state: StatsUiState, onNavigate: (Route) -> Unit = {}) {
    val overview = state.overview
    val wide = LocalWindowWidth.current.isExpanded
    ScreenHeader(
        tr("Statistics"),
        overview?.let { tr("Your progress in {0}.", languageInSentence(it.languageName, LanguageCase.PREPOSITIONAL)) } ?: tr("Your progress in the languages you learn."),
    )
    // With nothing read, saved or reviewed, one message and a way to start say more than a page of grey charts.
    if (overview != null && overview.wordsRead == 0 && overview.termsByStatus.values.sum() == 0 && overview.reviewsTotal == 0) {
        EmptyState(
            AppIcons.BarChart,
            tr("Nothing to count yet"),
            message = tr("Read a text or save a word, and your progress shows here."),
            action = { PrimaryButton(tr("Browse books"), onClick = { onNavigate(Route.Books) }, modifier = Modifier.testTag("stats-start")) },
        )
    } else if (overview != null) {
        OverviewTiles(overview)
        ActivityCard(overview)
        SideBySide(wide, { WordsPerDayCard(overview, it) }, { SavedPerWeekCard(overview, it) })
        SideBySide(wide, { VocabularyCard(overview, it) }, { FlashcardsCard(overview, it) })
    }
    state.summary?.takeIf { it.table.isNotEmpty() }?.let { LanguagesCard(it) }
}

/** Two cards side by side on wide windows, one above the other otherwise. */
@Composable
private fun SideBySide(wide: Boolean, first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    if (wide) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            first(Modifier.weight(1f).fillMaxHeight())
            second(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        first(Modifier)
        second(Modifier)
    }
}

private data class Tile(val label: String, val value: String, val detail: String, val icon: ImageVector, val tint: Color, val tag: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewTiles(o: LanguageOverview) {
    val tiles = listOf(
        Tile(tr("Words read"), formatCount(animatedCount(o.wordsRead)), tr("{0} this week", formatCount(o.wordsReadThisWeek)), AppIcons.MenuBook, AppAccents.blue, "words-read"),
        Tile(tr("Known words"), formatCount(animatedCount(o.known)), trPlural(o.learning, "{1} word being learned", "{1} words being learned", formatCount(o.learning)), AppIcons.DoneAll, AppAccents.green, "known-words"),
        Tile(tr("Words saved today"), formatCount(animatedCount(o.savedToday)), trPlural(o.savedThisWeek, "{1} word saved this week", "{1} words saved this week", formatCount(o.savedThisWeek)), AppIcons.Abc, AppAccents.orange, "words-today"),
        Tile(tr("Reading streak"), animatedCount(o.streak).let { trPlural(it, "{0} day", "{0} days") }, trPlural(o.longestStreak, "Longest: {0} day", "Longest: {0} days"), AppIcons.Flame, AppAccents.pink, "streak"),
        Tile(tr("Days with reading"), formatCount(animatedCount(o.daysRead)), tr("{0} words a day on average", formatCount(o.wordsPerReadingDay)), AppIcons.History, AppAccents.purple, "days-read"),
        Tile(tr("Pages read"), formatCount(animatedCount(o.pagesRead)), trPlural(o.booksFinished, "{1} book finished", "{1} books finished", formatCount(o.booksFinished)), AppIcons.Book, AppAccents.teal, "pages-read"),
    )
    val compact = LocalWindowWidth.current.isCompact
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        maxItemsInEachRow = if (compact) 2 else 3,
    ) {
        tiles.forEach { tile ->
            StatTile(tile.label, tile.value, tile.detail, tile.icon, tile.tint, Modifier.weight(1f).testTag("tile-${tile.tag}"))
        }
    }
}

/** The recent days, darker the more words were read on them. */
@Composable
private fun ActivityCard(o: LanguageOverview) {
    // Only the days shown are compared, so a busy day long ago does not make the recent ones look quiet.
    fun shown(start: LocalDate) = o.readingDays.filterKeys { it in start..o.today }
    ActivityHeatmapCard(
        title = tr("Reading activity"),
        subtitle = { start ->
            val days = shown(start).size
            trPlural(days, "{1} day with reading since {2}", "{1} days with reading since {2}", formatCount(days), start.shortDate(o.today))
        },
        today = o.today,
        levels = { start ->
            val days = shown(start)
            val max = days.values.maxOrNull() ?: 0
            days.mapValues { heatLevel(it.value, max) }
        },
        fewer = tr("Fewer words"),
        more = tr("More words"),
        describe = { date -> o.readingDays[date]?.takeIf { it > 0 }?.let { trPlural(it, "{1} word read", "{1} words read", formatCount(it)) } ?: tr("Nothing read") },
    )
}

@Composable
private fun WordsPerDayCard(o: LanguageOverview, modifier: Modifier) {
    val first = o.today.minus(29, DateTimeUnit.DAY)
    val values = (0 until 30).map { o.readingDays[first.plus(it, DateTimeUnit.DAY)] ?: 0 }
    ContentCard(tr("Words read per day"), tr("The last 30 days: {0} words", formatCount(values.sum())), icon = AppIcons.MenuBook, modifier = modifier) {
        BarChart(values, AppAccents.blue, first.shortDate(), o.today.shortDate(), tr("No reading in the last 30 days. Words you read in books and lessons count here."), Modifier.testTag("words-per-day"))
    }
}

@Composable
private fun SavedPerWeekCard(o: LanguageOverview, modifier: Modifier) {
    val firstWeekEnd = o.today.minus((o.savedByWeek.size - 1) * 7, DateTimeUnit.DAY)
    ContentCard(tr("New words saved per week"), o.savedByWeek.sum().let { trPlural(it, "The last 12 weeks: {1} word", "The last 12 weeks: {1} words", formatCount(it)) }, icon = AppIcons.Abc, modifier = modifier) {
        BarChart(o.savedByWeek, AppAccents.orange, firstWeekEnd.minus(6, DateTimeUnit.DAY).shortDate(), o.today.shortDate(), tr("No words saved in the last 12 weeks. Words get saved when you give them a status while reading."), Modifier.testTag("saved-per-week"))
    }
}

/**
 * Columns for [values], oldest first, with the highest one's value above and the first and last
 * dates under them; with nothing to draw, [empty] says so in place of an empty chart.
 */
@Composable
private fun BarChart(values: List<Int>, color: Color, from: String, to: String, empty: String, modifier: Modifier = Modifier) {
    val max = values.maxOrNull() ?: 0
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
    if (max == 0) {
        Text(empty, modifier, style = MaterialTheme.typography.bodyMedium, color = muted)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Most: {0}", formatCount(max)), style = MaterialTheme.typography.bodySmall, color = muted)
        Canvas(Modifier.fillMaxWidth().height(BAR_CHART_HEIGHT)) {
            val slot = size.width / values.size
            val bar = slot * 0.68f
            // A baseline rather than a grey bar for each day, so an empty day reads as nothing.
            val base = BAR_BASELINE.toPx()
            drawRect(track, Offset(0f, size.height - base), Size(size.width, base))
            values.forEachIndexed { i, value ->
                val x = i * slot + (slot - bar) / 2
                if (value > 0) {
                    val h = (size.height * value / max).coerceAtLeast(3f)
                    drawRoundRect(color, Offset(x, size.height - h), Size(bar, h), CornerRadius(bar / 4))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(from, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = muted)
            Text(to, style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

private val vocabularyOrder = listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4, TermStatus.WELL_KNOWN, TermStatus.IGNORED)

/** The saved words by status, as one bar and a list. */
@Composable
private fun VocabularyCard(o: LanguageOverview, modifier: Modifier) {
    val statusColors = TayraTheme.current.statusColors
    val outline = MaterialTheme.colorScheme.outline
    // Known words have no colour in the reader, so here they get the green of "known".
    fun tint(status: TermStatus): Color = when (status) {
        TermStatus.WELL_KNOWN -> AppAccents.green
        TermStatus.IGNORED -> outline
        else -> statusColors.background(status)
    }
    val counts = vocabularyOrder.associateWith { o.termsByStatus[it] ?: 0 }
    val total = counts.values.sum()
    ContentCard(tr("Vocabulary by status"), trPlural(total, "{1} word saved", "{1} words saved", formatCount(total)), icon = AppIcons.Tune, modifier = modifier) {
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
        Row(Modifier.fillMaxWidth().height(AppSizes.trackBold).clip(AppShapes.pill).background(track).testTag("status-bar")) {
            if (total > 0) {
                vocabularyOrder.forEach { status ->
                    val n = counts.getValue(status)
                    if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(tint(status)))
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.l))
        vocabularyOrder.forEach { status ->
            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(tint(status))
                Spacer(Modifier.width(AppSpacing.m))
                Text(tr(status.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(formatCount(counts.getValue(status)), style = AppText.labelStrong)
            }
        }
    }
}

@Composable
private fun FlashcardsCard(o: LanguageOverview, modifier: Modifier) {
    val first = o.today.minus(29, DateTimeUnit.DAY)
    val values = (0 until 30).map { o.reviewsByDay[first.plus(it, DateTimeUnit.DAY)] ?: 0 }
    ContentCard(
        tr("Flashcard reviews"),
        when (val remembered = o.rememberedPercent) {
            null -> tr("No reviews yet.")
            else -> trPlural(o.reviewsTotal, "{1} review, remembered {2}% of the time", "{1} reviews, remembered {2}% of the time", formatCount(o.reviewsTotal), remembered)
        },
        icon = AppIcons.DoneAll,
        modifier = modifier,
    ) {
        BarChart(values, AppAccents.purple, first.shortDate(), o.today.shortDate(), tr("No flashcard reviews in the last 30 days."), Modifier.testTag("reviews-per-day"))
    }
}

/** Words read in each language: today, this week, month, year and in all, and over time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesCard(summary: ReadingStatsSummary) {
    ContentCard(tr("Words read by language"), tr("Every language you have read in."), icon = AppIcons.Globe) {
        val headers = listOf(tr("Language"), tr("Today"), tr("Week"), tr("Month"), tr("Year"), tr("Total"))
        TableHeader(padding = PaddingValues(vertical = AppSpacing.xxs)) {
            // Counts line up on the right, so their digits do.
            headers.forEachIndexed { i, h ->
                if (i == 0) TableHeaderCell(h, Modifier.weight(2f)) else Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { TableHeaderCell(h) }
            }
        }
        summary.table.forEach { row ->
            AppDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s)) {
                Text(tr(row.languageName), Modifier.weight(2f), style = AppText.label)
                listOf(row.counts.day, row.counts.week, row.counts.month, row.counts.year, row.counts.total).forEach {
                    Text(formatCount(it), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.xl))
        Text(tr("Cumulative words read"), style = AppText.itemTitle)
        Spacer(Modifier.height(AppSpacing.m))
        Chart(summary.chart)
        Spacer(Modifier.height(AppSpacing.m))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            summary.chart.keys.forEachIndexed { i, language ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(seriesColors[i % seriesColors.size])
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text(tr(language), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Chart(series: Map<String, List<ChartPoint>>) {
    val points = series.values.flatten()
    if (points.isEmpty()) return
    val minDate = points.minOf { it.date.toEpochDays() }
    val maxDate = points.maxOf { it.date.toEpochDays() }.coerceAtLeast(minDate + 1)
    val maxTotal = points.maxOf { it.runningTotal }.coerceAtLeast(1)
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // The top and the two ends are labelled, so the line says how much and when.
    Text(formatCount(maxTotal), style = MaterialTheme.typography.bodySmall, color = muted)
    Canvas(Modifier.fillMaxWidth().height(LINE_CHART_HEIGHT)) {
        val bottom = size.height
        drawLine(axisColor, Offset(0f, bottom), Offset(size.width, bottom))
        series.values.forEachIndexed { i, list ->
            val path = Path()
            list.forEachIndexed { index, point ->
                val x = (point.date.toEpochDays() - minDate).toFloat() / (maxDate - minDate) * size.width
                val y = bottom - point.runningTotal.toFloat() / maxTotal * (bottom - 4f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, seriesColors[i % seriesColors.size], style = Stroke(width = 3f))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs)) {
        Text(LocalDate.fromEpochDays(minDate).shortDateWithYear(), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = muted)
        Text(LocalDate.fromEpochDays(maxDate).shortDateWithYear(), style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

private val BAR_BASELINE = 2.dp
private val BAR_CHART_HEIGHT = 140.dp
private val LINE_CHART_HEIGHT = 200.dp
