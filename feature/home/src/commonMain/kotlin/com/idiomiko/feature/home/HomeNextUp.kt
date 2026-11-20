package com.idiomiko.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.stats.StudyTime
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** What a [NextUp] card shows: its kind's icon and colour, a title, what it holds, how far it has got, and the way in. */
private class NextUpView(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val detail: String?,
    val facts: List<String>,
    val button: String,
    val onClick: () -> Unit,
    val progress: Float? = null,
    val progressText: String? = null,
    val quick: (() -> Unit)? = null,
)

/**
 * The one step to take first, as [NextUp] chooses it: the cards when many are due, the lesson or the book
 * under way, the cards still due, a course to start, or the courses. It holds the page's only filled button.
 */
@Composable
internal fun NextUpCard(state: HomeUiState, actions: HomeActions) {
    val colors = MaterialTheme.colorScheme
    val next = state.nextUp
    val view = view(next, actions)
    val minutes = next.minutes?.let { tr("~{0} min", it) }
    val compact = LocalWindowWidth.current.isCompact
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(if (compact) AppSpacing.l else AppSpacing.xl).testTag("next-up"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        // What led here: a first visit starts here, and a finished course is said before the next one.
        val finished = state.finishedCourse?.takeIf { next is NextUp.Lesson && !next.started }
        Text(
            when {
                state.firstVisit -> tr("Start here")
                finished != null -> tr("You finished {0}. Next:", finished.course.title.inLearnedScript())
                else -> tr("Next up")
            },
            style = AppText.labelStrong,
            color = if (finished != null) AppAccents.green else colors.primary,
            modifier = Modifier.testTag("next-up-label"),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(view.icon, size = if (compact) 44 else 52, tint = view.tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(view.title, style = AppText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                view.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                val facts = listOfNotNull(minutes) + view.facts
                if (facts.isNotEmpty()) Facts(facts)
            }
        }
        if (view.progress != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                ProgressTrack(view.progress, view.tint, Modifier.weight(1f), height = AppSizes.track)
                view.progressText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
        }
        state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        val quick = view.quick?.let { onQuick ->
            @Composable { modifier: Modifier ->
                SecondaryButton(tr("Quick {0}", NextUp.QUICK_SESSION), onClick = onQuick, modifier = modifier.testTag("next-up-quick"), size = ButtonSize.Large, icon = AppIcons.Speed)
            }
        }
        // A phone has no room for both side by side: the quick session goes under the main button.
        if (compact) {
            PrimaryButton(view.button, onClick = view.onClick, modifier = Modifier.fillMaxWidth().testTag("next-up-action"), size = ButtonSize.Large)
            quick?.invoke(Modifier.fillMaxWidth())
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(view.button, onClick = view.onClick, modifier = Modifier.weight(1f).testTag("next-up-action"), size = ButtonSize.Large)
                quick?.invoke(Modifier)
            }
        }
    }
}

@Composable
private fun view(next: NextUp, actions: HomeActions): NextUpView = when (next) {
    is NextUp.Review -> NextUpView(
        AppIcons.Style, AppAccents.teal,
        title = trPlural(next.due, "{1} card due today", "{1} cards due today", formatCount(next.due)),
        detail = tr("Review them to keep your words fresh."),
        facts = emptyList(),
        button = tr("Review flashcards"),
        onClick = { actions.onNavigate(Route.Flashcards()) },
        quick = if (next.due > NextUp.QUICK_SESSION) ({ actions.onNavigate(Route.Flashcards(limit = NextUp.QUICK_SESSION)) }) else null,
    )
    is NextUp.Lesson -> {
        val total = next.course.lessons.size
        NextUpView(
            AppIcons.School, AppAccents.blue,
            title = next.course.course.title.inLearnedScript(),
            detail = tr("Lesson {0}: {1}", next.number, next.lesson.lesson.title.inLearnedScript()),
            facts = listOfNotNull(next.newWords.takeIf { it > 0 }?.let { trPlural(it, "{1} new word", "{1} new words", formatCount(it)) }),
            button = if (next.started) tr("Continue course") else tr("Start course"),
            onClick = { actions.onOpenLesson(next.course, next.lesson) },
            progress = if (next.started && total > 0) next.course.completed.toFloat() / total else null,
            progressText = if (next.started) trPlural(total, "{1} of {0} lesson", "{1} of {0} lessons", next.course.completed) else null,
        )
    }
    is NextUp.Book -> NextUpView(
        AppIcons.MenuBook, AppAccents.purple,
        title = next.book.title.inLearnedScript(),
        detail = pageLine(next.book),
        facts = emptyList(),
        button = tr("Continue book"),
        onClick = { actions.onNavigate(Route.Read(next.book.id)) },
        progress = bookPercent(next.book) / 100f,
        progressText = "${bookPercent(next.book)}%",
    )
    NextUp.StartHere -> NextUpView(
        AppIcons.School, AppAccents.blue,
        title = tr("Start with a course"),
        detail = tr("Short stories at your level. The words you save become flashcards."),
        facts = emptyList(),
        button = tr("Browse courses"),
        onClick = { actions.onNavigate(Route.Courses) },
    )
}

/** Short facts in a row: the time a step takes and what it brings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Facts(facts: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), modifier = Modifier.padding(top = AppSpacing.xs).testTag("next-up-facts")) {
        facts.forEachIndexed { i, fact ->
            if (i > 0) Text("·", style = AppText.label, color = MaterialTheme.colorScheme.outline)
            Text(fact, style = AppText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Everything else under way, as rows: the course, the book, the playlist and the flashcards, leaving out
 * whatever the next step already offers. A finished course says so; with no cards due, they are all caught up.
 */
@Composable
internal fun ContinueCard(state: HomeUiState, actions: HomeActions) {
    val next = state.nextUp
    val rows = buildList<@Composable () -> Unit> {
        val course = state.course?.takeIf { it.status == LessonStatus.IN_PROGRESS }
        val lesson = course?.nextLesson
        if (course != null && lesson != null && !(next is NextUp.Lesson && next.course.course.id == course.course.id)) add {
            ContinueRow(
                tr("Continue course"), course.course.title.inLearnedScript(),
                tr("Lesson {0}: {1}", course.lessons.indexOf(lesson) + 1, lesson.lesson.title.inLearnedScript()),
                AppIcons.School, AppAccents.blue, "course",
                trailing = "${course.completed}/${course.lessons.size}",
            ) { actions.onOpenLesson(course, lesson) }
        }
        state.finishedCourse?.let { done ->
            add {
                ContinueRow(
                    tr("Course finished"), done.course.title.inLearnedScript(), tr("Choose your next course"),
                    Icons.Default.Check, AppAccents.green, "finished-course",
                ) { actions.onNavigate(Route.Courses) }
            }
        }
        val book = state.book?.takeIf { !it.isCompleted }
        if (book != null && !(next is NextUp.Book && next.book.id == book.id)) add {
            ContinueRow(tr("Continue book"), book.title.inLearnedScript(), pageLine(book), AppIcons.MenuBook, AppAccents.purple, "book", trailing = "${bookPercent(book)}%") {
                actions.onNavigate(Route.Read(book.id))
            }
        }
        state.playlist?.let { playlist -> if (resumable(playlist)) add { PlaylistRow(playlist, actions.onContinuePlaylist) } }
        if (next !is NextUp.Review) add {
            val due = state.flashcardsDue
            if (due > 0) {
                ContinueRow(
                    tr("Flashcards"), trPlural(due, "{1} card due today", "{1} cards due today", formatCount(due)),
                    tr("~{0} min", StudyTime.reviewMinutes(due, state.activity?.averageAnswer)), AppIcons.Style, AppAccents.teal, "flashcards",
                ) { actions.onNavigate(Route.Flashcards()) }
            } else {
                ContinueRow(tr("Flashcards"), tr("All caught up"), tr("No cards are due right now."), Icons.Default.Check, AppAccents.teal, "flashcards") {
                    actions.onNavigate(Route.Flashcards())
                }
            }
        }
    }
    if (rows.isEmpty()) return
    val compact = LocalWindowWidth.current.isCompact
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(vertical = AppSpacing.s).testTag("continue")) {
        CardHeader(tr("Continue"), icon = AppIcons.PlayArrow, tint = AppAccents.purple, modifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xl, vertical = AppSpacing.s), size = CardHeaderSize.Dense)
        rows.forEachIndexed { i, row ->
            if (i > 0) AppDivider(Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xl))
            row()
        }
    }
}

/**
 * The playlist under way can be gone on with once it is at a text: the row names that text, which says more
 * than the playlist's name. A playlist at no text, with nothing heard yet, has nothing to go on with.
 */
internal fun resumable(playlist: Playlist): Boolean = playlist.currentItem != null || playlist.progress > 0f

@Composable
private fun PlaylistRow(playlist: Playlist, onContinue: () -> Unit) {
    val item = playlist.currentItem
    val percent = (playlist.progress * 100).toInt()
    ContinueRow(
        label = tr("Continue listening"),
        title = item?.title?.inLearnedScript() ?: playlist.name,
        detail = if (item != null) playlist.name else trPlural(playlist.items.size, "{0} text", "{0} texts"),
        icon = AppIcons.Headphones,
        tint = AppAccents.orange,
        tag = "playlist",
        trailing = percent.takeIf { it > 0 }?.let { "$it%" },
        onClick = onContinue,
    )
}

/** A row of something under way: its kind, its title and where it stands; the whole row goes on with it. */
@Composable
private fun ContinueRow(label: String, title: String, detail: String, icon: ImageVector, tint: Color, tag: String, trailing: String? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xl, vertical = AppSpacing.m).testTag("row-$tag"),
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

/** Where a book stands: its page, or for a text of one page, its length. */
@Composable
internal fun pageLine(book: BookListItem): String =
    if (book.pageCount > 1) tr("Page {0} of {1}", book.currentPage, book.pageCount) else trPlural(book.wordCount, "{1} word", "{1} words", formatCount(book.wordCount))

/** As on Books: the pages finished, and never all of them until the book is marked finished. */
internal fun bookPercent(book: BookListItem): Int =
    if (book.pageCount > 0) (100 * (book.currentPage - 1) / book.pageCount).coerceIn(0, 99) else 0
