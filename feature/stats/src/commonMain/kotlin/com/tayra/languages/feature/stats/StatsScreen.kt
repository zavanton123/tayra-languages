package com.tayra.languages.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
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
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
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
        PageColumn(padding) { StatsContent(state) }
    }
}

private val BLUE = Color(0xFF3B6FE0)
private val GREEN = Color(0xFF2E9D57)
private val ORANGE = Color(0xFFEA7A1B)
private val PURPLE = Color(0xFF7C4DDB)
private val TEAL = Color(0xFF1F8A8A)
private val PINK = Color(0xFFD9488B)
private val seriesColors = listOf(BLUE, ORANGE, GREEN, Color(0xFFD62728), PURPLE, Color(0xFF8C564B), PINK, TEAL)

@Composable
internal fun StatsContent(state: StatsUiState) {
    val overview = state.overview
    val wide = LocalWindowWidth.current.isExpanded
    ScreenHeader(
        tr("Statistics"),
        overview?.let { tr("Your progress in {0}.", languageInSentence(it.languageName, LanguageCase.PREPOSITIONAL)) } ?: tr("Your progress in the languages you learn."),
    )
    if (overview != null) {
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
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
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
        Tile(tr("Words read"), formatCount(o.wordsRead), tr("{0} this week", formatCount(o.wordsReadThisWeek)), AppIcons.MenuBook, BLUE, "words-read"),
        Tile(tr("Known words"), formatCount(o.known), trPlural(o.savedThisWeek, "{1} word saved this week", "{1} words saved this week", formatCount(o.savedThisWeek)), AppIcons.DoneAll, GREEN, "known-words"),
        Tile(tr("Words being learned"), formatCount(o.learning), tr("Statuses 1 to 4"), AppIcons.Abc, ORANGE, "learning-words"),
        Tile(tr("Reading streak"), trPlural(o.streak, "{0} day", "{0} days"), trPlural(o.longestStreak, "Longest: {0} day", "Longest: {0} days"), AppIcons.Flame, PINK, "streak"),
        Tile(tr("Days with reading"), formatCount(o.daysRead), tr("{0} words a day on average", formatCount(o.wordsPerReadingDay)), AppIcons.History, PURPLE, "days-read"),
        Tile(tr("Pages read"), formatCount(o.pagesRead), trPlural(o.booksFinished, "{1} book finished", "{1} books finished", formatCount(o.booksFinished)), AppIcons.Book, TEAL, "pages-read"),
    )
    val compact = LocalWindowWidth.current.isCompact
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = if (compact) 2 else 3,
    ) {
        tiles.forEach { tile ->
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.weight(1f).clip(shape).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(if (compact) 14.dp else 20.dp).testTag("tile-${tile.tag}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!compact) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(tile.tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(tile.icon, contentDescription = null, tint = tile.tint, modifier = Modifier.size(26.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tile.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tile.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(tile.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

/** Each recent day as a square, darker the more words were read on it: a column per week from Monday, as many weeks as fit, up to a year. */
@Composable
private fun ActivityCard(o: LanguageOverview) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    // The weeks shown depend on the width, which the card's subtitle needs, so the whole card is measured first.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 4.dp
        // The card's padding takes 20 on each side.
        val inner = maxWidth - 40.dp
        val weeks = ((inner + gap) / (MIN_CELL + gap)).toInt().coerceIn(8, MAX_WEEKS)
        val cell: Dp = ((inner - gap * (weeks - 1)) / weeks).coerceAtMost(MAX_CELL)
        val thisMonday = o.today.minus(o.today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val start = thisMonday.minus((weeks - 1) * 7, DateTimeUnit.DAY)
        val days = o.readingDays.filterKeys { it >= start && it <= o.today }
        val maxWords = days.values.maxOrNull() ?: 0
        ContentCard(
            tr("Reading activity"),
            trPlural(days.size, "{1} day with reading since {2}", "{1} days with reading since {2}", formatCount(days.size), shortDate(start, o.today)),
            icon = AppIcons.BarChart,
        ) {
            Row(Modifier.testTag("heatmap"), horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { weekday ->
                            val date = start.plus(week * 7 + weekday, DateTimeUnit.DAY)
                            val words = days[date] ?: 0
                            val color = when {
                                date > o.today -> Color.Transparent
                                words == 0 -> empty
                                else -> GREEN.copy(alpha = shade(level(words, maxWords)))
                            }
                            Box(Modifier.size(cell).clip(RoundedCornerShape(3.dp)).background(color))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Fewer words"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(empty))
                (1..4).forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(GREEN.copy(alpha = shade(it)))) }
                Text(tr("More words"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val MAX_WEEKS = 53
private val MIN_CELL = 14.dp
private val MAX_CELL = 22.dp

/** 1 to 4: which quarter of the busiest day's words [words] reach. */
private fun level(words: Int, max: Int): Int = if (max <= 0) 0 else kotlin.math.ceil(4.0 * words / max).toInt().coerceIn(1, 4)

private fun shade(level: Int): Float = 0.25f + 0.75f * level / 4f

private val monthKeys = listOf("Jan {0}", "Feb {0}", "Mar {0}", "Apr {0}", "May {0}", "Jun {0}", "Jul {0}", "Aug {0}", "Sep {0}", "Oct {0}", "Nov {0}", "Dec {0}")

/** "Oct 7", or as the interface language writes a short date. */
private fun shortDate(date: LocalDate): String = tr(monthKeys[date.month.ordinal], date.day)

private val monthYearKeys = listOf("Jan {0}, {1}", "Feb {0}, {1}", "Mar {0}, {1}", "Apr {0}, {1}", "May {0}, {1}", "Jun {0}, {1}", "Jul {0}, {1}", "Aug {0}, {1}", "Sep {0}, {1}", "Oct {0}, {1}", "Nov {0}, {1}", "Dec {0}, {1}")

/** A short date, with the year when it is not [today]'s. */
private fun shortDate(date: LocalDate, today: LocalDate): String =
    if (date.year == today.year) shortDate(date) else tr(monthYearKeys[date.month.ordinal], date.day, date.year)

@Composable
private fun WordsPerDayCard(o: LanguageOverview, modifier: Modifier) {
    val first = o.today.minus(29, DateTimeUnit.DAY)
    val values = (0 until 30).map { o.readingDays[first.plus(it, DateTimeUnit.DAY)] ?: 0 }
    ContentCard(tr("Words read per day"), tr("The last 30 days: {0} words", formatCount(values.sum())), icon = AppIcons.MenuBook, modifier = modifier) {
        BarChart(values, BLUE, shortDate(first), shortDate(o.today), Modifier.testTag("words-per-day"))
    }
}

@Composable
private fun SavedPerWeekCard(o: LanguageOverview, modifier: Modifier) {
    val firstWeekEnd = o.today.minus((o.savedByWeek.size - 1) * 7, DateTimeUnit.DAY)
    ContentCard(tr("New words saved per week"), tr("The last 12 weeks: {0} words", formatCount(o.savedByWeek.sum())), icon = AppIcons.Abc, modifier = modifier) {
        BarChart(o.savedByWeek, ORANGE, shortDate(firstWeekEnd.minus(6, DateTimeUnit.DAY)), shortDate(o.today), Modifier.testTag("saved-per-week"))
    }
}

/** Columns for [values], oldest first, with the highest one's value above and the first and last dates under them. */
@Composable
private fun BarChart(values: List<Int>, color: Color, from: String, to: String, modifier: Modifier = Modifier) {
    val max = values.maxOrNull() ?: 0
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (max > 0) tr("Most: {0}", formatCount(max)) else tr("Nothing yet"), style = MaterialTheme.typography.bodySmall, color = muted)
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / values.size
            val bar = slot * 0.68f
            values.forEachIndexed { i, value ->
                val x = i * slot + (slot - bar) / 2
                drawRoundRect(track, Offset(x, 0f), Size(bar, size.height), CornerRadius(bar / 4))
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
        TermStatus.WELL_KNOWN -> GREEN
        TermStatus.IGNORED -> outline
        else -> statusColors.background(status)
    }
    val counts = vocabularyOrder.associateWith { o.termsByStatus[it] ?: 0 }
    val total = counts.values.sum()
    ContentCard(tr("Vocabulary by status"), trPlural(total, "{1} word saved", "{1} words saved", formatCount(total)), icon = AppIcons.Tune, modifier = modifier) {
        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(track).testTag("status-bar")) {
            if (total > 0) {
                vocabularyOrder.forEach { status ->
                    val n = counts.getValue(status)
                    if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(tint(status)))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        vocabularyOrder.forEach { status ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(tint(status)))
                Spacer(Modifier.width(10.dp))
                Text(tr(status.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(formatCount(counts.getValue(status)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
        BarChart(values, PURPLE, shortDate(first), shortDate(o.today), Modifier.testTag("reviews-per-day"))
    }
}

/** Words read in each language: today, this week, month, year and in all, and over time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagesCard(summary: ReadingStatsSummary) {
    ContentCard(tr("Words read by language"), tr("Every language you have read in."), icon = AppIcons.Globe) {
        val headers = listOf(tr("Language"), tr("Today"), tr("Week"), tr("Month"), tr("Year"), tr("Total"))
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            headers.forEachIndexed { i, h ->
                Text(h, Modifier.weight(if (i == 0) 2f else 1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        summary.table.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(tr(row.languageName), Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                listOf(row.counts.day, row.counts.week, row.counts.month, row.counts.year, row.counts.total).forEach {
                    Text(formatCount(it), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(tr("Cumulative words read"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Chart(summary.chart)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            summary.chart.keys.forEachIndexed { i, language ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(seriesColors[i % seriesColors.size]))
                    Spacer(Modifier.width(6.dp))
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
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
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
}
