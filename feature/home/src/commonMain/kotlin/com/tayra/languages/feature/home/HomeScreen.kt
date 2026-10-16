package com.tayra.languages.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.stats.StudyActivity
import com.tayra.languages.core.ui.components.ActivityHeatmapCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.shortDate
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(onNavigate: (Route) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    // Back from reading or reviewing, the day's figures have changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    Scaffold(
        topBar = { AppTopBar(title = tr("Home"), onNavigate = onNavigate, section = NavSection.HOME) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) { HomeContent(state, onNavigate, onContinueCourse = viewModel::continueCourse) }
    }
}

@Composable
internal fun HomeContent(state: HomeUiState, onNavigate: (Route) -> Unit, onContinueCourse: () -> Unit) {
    val width = LocalWindowWidth.current
    ScreenHeader(
        tr("Home"),
        if (state.languageName.isEmpty()) tr("Pick up where you left off.")
        else tr("Pick up where you left off in {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
    )
    val activity = state.activity
    if (activity != null) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(if (width.isCompact) 12.dp else 20.dp)) {
            WordsTodayTile(activity, Modifier.weight(1f).fillMaxHeight())
            StreakTile(activity, Modifier.weight(1f).fillMaxHeight())
        }
    }
    val cards: List<@Composable (Modifier, Boolean) -> Unit> = listOf(
        { m, fill -> CourseCard(state, m, fill, onNavigate, onContinueCourse) },
        { m, fill -> BookCard(state, m, fill, onNavigate) },
        { m, fill -> FlashcardsCard(state.flashcardsDue, m, fill, onNavigate) },
    )
    if (width.isExpanded) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            cards.forEach { it(Modifier.weight(1f).fillMaxHeight(), true) }
        }
    } else {
        cards.forEach { it(Modifier, false) }
    }
    if (activity != null) StudyActivityCard(activity)
}

private val ORANGE = Color(0xFFEA7A1B)
private val PINK = Color(0xFFD9488B)
private val BLUE = Color(0xFF3B6FE0)
private val PURPLE = Color(0xFF7C4DDB)
private val TEAL = Color(0xFF1F8A8A)

@Composable
private fun WordsTodayTile(activity: StudyActivity, modifier: Modifier) {
    val weekStart = activity.today.minus(6, DateTimeUnit.DAY)
    val week = activity.days.filterKeys { it in weekStart..activity.today }.values.sumOf { it.wordsSaved }
    Tile(tr("Words added today"), formatCount(activity.savedToday), tr("{0} this week", formatCount(week)), AppIcons.Abc, ORANGE, "words-today", modifier)
}

@Composable
private fun StreakTile(activity: StudyActivity, modifier: Modifier) {
    val detail = when {
        activity.studiedToday -> trPlural(activity.longestStreak, "Longest: {0} day", "Longest: {0} days")
        activity.streak > 0 -> tr("Study today to keep it going")
        else -> tr("Study today to start a streak")
    }
    Tile(tr("Day streak"), trPlural(activity.streak, "{0} day", "{0} days"), detail, AppIcons.Flame, PINK, "streak", modifier)
}

@Composable
private fun Tile(label: String, value: String, detail: String, icon: ImageVector, tint: Color, tag: String, modifier: Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(if (compact) 14.dp else 20.dp).testTag("tile-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!compact) IconTile(icon, tint)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun CourseCard(state: HomeUiState, modifier: Modifier, fill: Boolean, onNavigate: (Route) -> Unit, onContinue: () -> Unit) {
    val progress = state.course
    val next = progress?.nextLesson
    if (progress == null || next == null) {
        ActionCard(
            tr("Continue course"), tr("No course in progress"), tr("Choose a course to start learning."), AppIcons.School, BLUE, "course", modifier, fill,
            button = tr("Browse courses"), primary = false, onClick = { onNavigate(Route.Courses) },
        )
        return
    }
    val started = progress.status != LessonStatus.NOT_STARTED
    val total = progress.lessons.size
    val number = progress.lessons.indexOf(next) + 1
    ActionCard(
        tr("Continue course"), progress.course.title, tr("Lesson {0}: {1}", number, next.lesson.title), AppIcons.School, BLUE, "course", modifier, fill,
        button = if (started) tr("Continue course") else tr("Start course"), onClick = onContinue,
        progress = if (total == 0) 0f else progress.completed.toFloat() / total,
        progressText = trPlural(total, "{1} of {0} lesson", "{1} of {0} lessons", progress.completed),
        error = state.error,
    )
}

@Composable
private fun BookCard(state: HomeUiState, modifier: Modifier, fill: Boolean, onNavigate: (Route) -> Unit) {
    val book = state.book
    if (book == null) {
        ActionCard(
            tr("Continue book"), tr("No book in progress"), tr("Open a book from your library to start reading."), AppIcons.MenuBook, PURPLE, "book", modifier, fill,
            button = tr("Browse books"), primary = false, onClick = { onNavigate(Route.Books) },
        )
        return
    }
    val share = if (book.pageCount > 0) book.currentPage.toFloat() / book.pageCount else 0f
    ActionCard(
        tr("Continue book"), book.title, tr("Page {0} of {1}", book.currentPage, book.pageCount), AppIcons.MenuBook, PURPLE, "book", modifier, fill,
        button = tr("Continue book"), onClick = { onNavigate(Route.Read(book.id)) },
        progress = share, progressText = "${(share * 100).toInt().coerceIn(0, 100)}%",
    )
}

@Composable
private fun FlashcardsCard(due: Int, modifier: Modifier, fill: Boolean, onNavigate: (Route) -> Unit) {
    ActionCard(
        tr("Flashcards"),
        if (due > 0) trPlural(due, "{1} card due today", "{1} cards due today", formatCount(due)) else tr("All caught up"),
        if (due > 0) tr("Review them to keep your words fresh.") else tr("No cards are due right now."),
        AppIcons.Style, TEAL, "flashcards", modifier, fill,
        button = tr("Review flashcards"), primary = due > 0, onClick = { onNavigate(Route.Flashcards) },
    )
}

/**
 * A way back into some study: what it is, how far it has got, and one button. With [fill] the
 * card takes the height it is given, its button at the bottom, so cards side by side line up.
 */
@Composable
private fun ActionCard(
    label: String,
    title: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    modifier: Modifier,
    fill: Boolean,
    button: String,
    onClick: () -> Unit,
    primary: Boolean = true,
    progress: Float? = null,
    progressText: String? = null,
    error: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape).padding(20.dp).testTag("card-$tag"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon, tint)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.onSurface.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(tint))
                }
                if (progressText != null) Text(progressText, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
        if (fill) Spacer(Modifier.weight(1f))
        val buttonModifier = Modifier.fillMaxWidth().height(48.dp).testTag("action-$tag")
        val buttonShape = RoundedCornerShape(12.dp)
        if (primary) {
            Button(onClick = onClick, shape = buttonShape, modifier = buttonModifier) { Text(button, fontWeight = FontWeight.SemiBold) }
        } else {
            OutlinedButton(onClick = onClick, shape = buttonShape, modifier = buttonModifier) { Text(button, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Each recent day, darker the more was studied: words read, flashcards answered or words saved. */
@Composable
private fun StudyActivityCard(activity: StudyActivity) {
    ActivityHeatmapCard(
        title = tr("Study activity"),
        subtitle = { start ->
            val days = activity.days.keys.count { it in start..activity.today }
            trPlural(days, "{1} day studied since {2}", "{1} days studied since {2}", formatCount(days), start.shortDate(activity.today))
        },
        today = activity.today,
        levels = activity::levels,
        fewer = tr("Less study"),
        more = tr("More study"),
        icon = AppIcons.History,
    )
}
