package com.tayra.languages.feature.home

import com.tayra.languages.core.ui.components.inLearnedScript
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.tayra.languages.core.ui.components.QuietButton
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.stats.StudyActivity
import com.tayra.languages.core.ui.components.StatTile
import com.tayra.languages.core.ui.components.ActivityHeatmapCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.animatedCount
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.shortDate
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
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
    // A completed book is history, not something Home should offer to continue. The view-model
    // already filters it out; keeping the guard here also protects previews and direct callers.
    val shownState = if (state.book?.isCompleted == true) state.copy(book = null) else state
    ScreenHeader(
        tr("Home"),
        if (state.languageName.isEmpty()) tr("Pick up where you left off.")
        else tr("Pick up where you left off in {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
    )
    val activity = shownState.activity
    if (width.isCompact) {
        PhoneHomeContent(shownState, onNavigate, onContinueCourse)
        return
    }
    if (activity != null) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            WordsTodayTile(activity, Modifier.weight(1f).fillMaxHeight())
            StreakTile(activity, Modifier.weight(1f).fillMaxHeight())
        }
    }
    // What needs doing first: cards that are due, then the course and book under way, then the empty ones.
    val courseUnderWay = shownState.course?.nextLesson != null
    val cards: List<@Composable (Modifier, Boolean) -> Unit> = listOf(
        (if (courseUnderWay) 1 else 2) to @Composable { m: Modifier, fill: Boolean -> CourseCard(shownState, m, fill, onNavigate, onContinueCourse) },
        (if (shownState.book != null) 1 else 2) to @Composable { m: Modifier, fill: Boolean -> BookCard(shownState, m, fill, onNavigate) },
        (if (shownState.flashcardsDue > 0) 0 else 2) to @Composable { m: Modifier, fill: Boolean -> FlashcardsCard(shownState.flashcardsDue, m, fill, onNavigate) },
    ).sortedBy { it.first }.map { it.second }
    if (width.isExpanded) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            cards.forEach { it(Modifier.weight(1f).fillMaxHeight(), true) }
        }
    } else {
        cards.forEach { it(Modifier, false) }
    }
    if (activity != null) StudyActivityCard(activity, onStats = { onNavigate(Route.Stats) })
}

internal enum class PhonePrimaryActivity { COURSE, FLASHCARDS, BOOK, BROWSE_COURSES }

internal fun phonePrimaryActivity(state: HomeUiState): PhonePrimaryActivity = when {
    state.course?.status == LessonStatus.IN_PROGRESS && state.course.nextLesson != null -> PhonePrimaryActivity.COURSE
    state.flashcardsDue > 0 -> PhonePrimaryActivity.FLASHCARDS
    state.book?.isCompleted == false -> PhonePrimaryActivity.BOOK
    else -> PhonePrimaryActivity.BROWSE_COURSES
}

/** One prominent next step, followed by compact alternatives and secondary progress figures. */
@Composable
private fun PhoneHomeContent(state: HomeUiState, onNavigate: (Route) -> Unit, onContinueCourse: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { PhoneHomeItems(state, onNavigate, onContinueCourse) }
}

@Composable
private fun PhoneHomeItems(state: HomeUiState, onNavigate: (Route) -> Unit, onContinueCourse: () -> Unit) {
    val courseUnderWay = state.course?.status == LessonStatus.IN_PROGRESS && state.course.nextLesson != null
    val primary = phonePrimaryActivity(state)
    Box(Modifier.fillMaxWidth().testTag("primary-activity")) {
        when (primary) {
            PhonePrimaryActivity.COURSE -> CourseCard(state, Modifier, fill = false, onNavigate, onContinueCourse)
            PhonePrimaryActivity.FLASHCARDS -> FlashcardsCard(state.flashcardsDue, Modifier, fill = false, onNavigate)
            PhonePrimaryActivity.BOOK -> BookCard(state, Modifier, fill = false, onNavigate)
            PhonePrimaryActivity.BROWSE_COURSES -> BrowseCoursesCard(onNavigate)
        }
    }
    // Reviews are the second urgent action when a learner has a lesson under way.
    if (primary == PhonePrimaryActivity.COURSE && state.flashcardsDue > 0) {
        FlashcardsRow(state.flashcardsDue, onNavigate)
    }
    state.activity?.let { activity ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            WordsTodayTile(activity, Modifier.weight(1f).fillMaxHeight())
            StreakTile(activity, Modifier.weight(1f).fillMaxHeight())
        }
    }
    when (primary) {
        PhonePrimaryActivity.FLASHCARDS -> {
            // Keep real in-progress activities above empty discovery rows.
            if (courseUnderWay) CourseRow(state, onNavigate, onContinueCourse)
            if (state.book != null) BookRow(state.book, onNavigate)
            if (!courseUnderWay) CourseRow(state, onNavigate, onContinueCourse)
            if (state.book == null) BookRow(null, onNavigate)
        }
        PhonePrimaryActivity.COURSE -> {
            BookRow(state.book, onNavigate)
            if (state.flashcardsDue == 0) FlashcardsRow(0, onNavigate)
        }
        PhonePrimaryActivity.BOOK -> {
            CourseRow(state, onNavigate, onContinueCourse)
            FlashcardsRow(state.flashcardsDue, onNavigate)
        }
        PhonePrimaryActivity.BROWSE_COURSES -> {
            BookRow(state.book, onNavigate)
            FlashcardsRow(state.flashcardsDue, onNavigate)
        }
    }
    state.activity?.let { StudyActivityCard(it, onStats = { onNavigate(Route.Stats) }) }
}

@Composable
private fun BrowseCoursesCard(onNavigate: (Route) -> Unit) {
    ActionCard(
        tr("Continue course"), tr("No course in progress"), tr("Choose a course to start learning."),
        AppIcons.School, AppAccents.blue, "course", Modifier, fill = false,
        button = tr("Browse courses"), primary = true, onClick = { onNavigate(Route.Courses) },
    )
}

@Composable
private fun CourseRow(state: HomeUiState, onNavigate: (Route) -> Unit, onContinue: () -> Unit) {
    val progress = state.course?.takeIf { it.status == LessonStatus.IN_PROGRESS && it.nextLesson != null }
    val next = progress?.nextLesson
    SecondaryActivityRow(
        label = if (progress != null) tr("Continue course") else tr("Courses"),
        title = progress?.course?.title?.inLearnedScript() ?: tr("No course in progress"),
        detail = if (progress != null && next != null) {
            val number = progress.lessons.indexOf(next) + 1
            tr("Lesson {0}: {1}", number, next.lesson.title.inLearnedScript())
        } else tr("Choose a course to start learning."),
        icon = AppIcons.School,
        tint = AppAccents.blue,
        tag = "course",
        onClick = if (progress != null) onContinue else ({ onNavigate(Route.Courses) }),
    )
}

@Composable
private fun BookRow(book: BookListItem?, onNavigate: (Route) -> Unit) {
    val active = book
    val percent = active?.let(::bookPercent)
    SecondaryActivityRow(
        label = if (active != null) tr("Continue book") else tr("Books"),
        title = active?.title?.inLearnedScript() ?: tr("No book in progress"),
        detail = active?.let {
            if (it.pageCount > 1) tr("Page {0} of {1}", it.currentPage, it.pageCount)
            else trPlural(it.wordCount, "{1} word", "{1} words", formatCount(it.wordCount))
        } ?: tr("Open a book from your library to start reading."),
        trailing = percent?.let { "$it%" },
        icon = AppIcons.MenuBook,
        tint = AppAccents.purple,
        tag = "book",
        onClick = { if (active != null) onNavigate(Route.Read(active.id)) else onNavigate(Route.Books) },
    )
}

@Composable
private fun FlashcardsRow(due: Int, onNavigate: (Route) -> Unit) {
    SecondaryActivityRow(
        label = tr("Flashcards"),
        title = if (due > 0) trPlural(due, "{1} card due today", "{1} cards due today", formatCount(due)) else tr("All caught up"),
        detail = if (due > 0) tr("Review them to keep your words fresh.") else tr("No cards are due right now."),
        icon = AppIcons.Style,
        tint = AppAccents.teal,
        tag = "flashcards",
        onClick = { onNavigate(Route.Flashcards) },
    )
}

@Composable
private fun SecondaryActivityRow(
    label: String,
    title: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.card).testTag("card-$tag")) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("action-$tag"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            IconTile(icon, size = 40, tint = tint)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1)
                Text(title, style = AppText.emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) Text(trailing, style = AppText.label, color = colors.onSurfaceVariant)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
        }
    }
}

@Composable
private fun WordsTodayTile(activity: StudyActivity, modifier: Modifier) {
    val weekStart = activity.today.minus(6, DateTimeUnit.DAY)
    val week = activity.days.filterKeys { it in weekStart..activity.today }.values.sumOf { it.wordsSaved }
    Tile(tr("Words saved today"), formatCount(animatedCount(activity.savedToday)), tr("{0} this week", formatCount(week)), AppIcons.Abc, AppAccents.orange, "words-today", modifier)
}

@Composable
private fun StreakTile(activity: StudyActivity, modifier: Modifier) {
    val detail = when {
        activity.studiedToday -> trPlural(activity.longestStreak, "Longest: {0} day", "Longest: {0} days")
        activity.streak > 0 -> tr("Study today to keep it going")
        else -> tr("Study today to start a streak")
    }
    Tile(tr("Study streak"), animatedCount(activity.streak).let { trPlural(it, "{0} day", "{0} days") }, detail, AppIcons.Flame, AppAccents.pink, "streak", modifier)
}

@Composable
private fun Tile(label: String, value: String, detail: String, icon: ImageVector, tint: Color, tag: String, modifier: Modifier) =
    StatTile(label, value, detail, icon, tint, modifier.testTag("tile-$tag"))

@Composable
private fun CourseCard(state: HomeUiState, modifier: Modifier, fill: Boolean, onNavigate: (Route) -> Unit, onContinue: () -> Unit) {
    val progress = state.course
    val next = progress?.nextLesson
    if (progress == null || next == null) {
        ActionCard(
            tr("Continue course"), tr("No course in progress"), tr("Choose a course to start learning."), AppIcons.School, AppAccents.blue, "course", modifier, fill,
            // With nothing under way, a course is the next step: its button leads.
            button = tr("Browse courses"), primary = state.book == null, onClick = { onNavigate(Route.Courses) },
        )
        return
    }
    val started = progress.status != LessonStatus.NOT_STARTED
    val total = progress.lessons.size
    val number = progress.lessons.indexOf(next) + 1
    ActionCard(
        tr("Continue course"), progress.course.title.inLearnedScript(), tr("Lesson {0}: {1}", number, next.lesson.title.inLearnedScript()), AppIcons.School, AppAccents.blue, "course", modifier, fill,
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
            tr("Continue book"), tr("No book in progress"), tr("Open a book from your library to start reading."), AppIcons.MenuBook, AppAccents.purple, "book", modifier, fill,
            button = tr("Browse books"), primary = false, onClick = { onNavigate(Route.Books) },
        )
        return
    }
    // As on Books: the pages finished, and never all of them until the book is marked finished.
    val percent = bookPercent(book)
    ActionCard(
        // A text of one page has no pages to count, so its length is given instead.
        tr("Continue book"), book.title.inLearnedScript(),
        if (book.pageCount > 1) tr("Page {0} of {1}", book.currentPage, book.pageCount) else trPlural(book.wordCount, "{1} word", "{1} words", formatCount(book.wordCount)), AppIcons.MenuBook, AppAccents.purple, "book", modifier, fill,
        button = tr("Continue book"), onClick = { onNavigate(Route.Read(book.id)) },
        progress = percent / 100f, progressText = "$percent%",
    )
}

private fun bookPercent(book: BookListItem): Int =
    if (book.pageCount > 0) (100 * (book.currentPage - 1) / book.pageCount).coerceIn(0, 99) else 0

@Composable
private fun FlashcardsCard(due: Int, modifier: Modifier, fill: Boolean, onNavigate: (Route) -> Unit) {
    ActionCard(
        tr("Flashcards"),
        if (due > 0) animatedCount(due).let { shown -> trPlural(shown, "{1} card due today", "{1} cards due today", formatCount(shown)) } else tr("All caught up"),
        if (due > 0) tr("Review them to keep your words fresh.") else tr("No cards are due right now."),
        AppIcons.Style, AppAccents.teal, "flashcards", modifier, fill,
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
    Column(
        modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xl).testTag("card-$tag"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(icon, size = 52, tint = tint)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(title, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                ProgressTrack(progress, tint, Modifier.weight(1f), height = AppSizes.trackThick)
                if (progressText != null) Text(progressText, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodyMedium, color = colors.error)
        if (fill) Spacer(Modifier.weight(1f))
        val buttonModifier = Modifier.fillMaxWidth().testTag("action-$tag")
        if (primary) {
            PrimaryButton(button, onClick = onClick, modifier = buttonModifier, size = ButtonSize.Large)
        } else {
            SecondaryButton(button, onClick = onClick, modifier = buttonModifier, size = ButtonSize.Large)
        }
    }
}

/** Each recent day, darker the more was studied: words read, flashcards answered or words saved. */
@Composable
private fun StudyActivityCard(activity: StudyActivity, onStats: () -> Unit) {
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
        // The days in more detail, and the rest of the figures, are on Statistics.
        action = { QuietButton(tr("Statistics"), onClick = onStats, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.testTag("home-stats")) },
        describe = { date ->
            val day = activity.days[date]
            listOfNotNull(
                day?.wordsRead?.takeIf { it > 0 }?.let { trPlural(it, "{1} word read", "{1} words read", formatCount(it)) },
                day?.reviews?.takeIf { it > 0 }?.let { trPlural(it, "{1} review", "{1} reviews", formatCount(it)) },
                day?.wordsSaved?.takeIf { it > 0 }?.let { trPlural(it, "{1} word saved", "{1} words saved", formatCount(it)) },
            ).joinToString(" · ").ifEmpty { tr("Nothing studied") }
        },
    )
}
