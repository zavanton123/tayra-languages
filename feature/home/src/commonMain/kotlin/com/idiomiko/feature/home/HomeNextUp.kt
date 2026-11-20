package com.idiomiko.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.stats.StudyTime
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.FlashcardsIllustration
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

/** What a [NextUp] card shows: its kind's icon, colour and picture, a title, what it holds, how far it has got, and the way in. */
private class NextUpView(
    val icon: ImageVector,
    val tint: Color,
    val face: ArtFace,
    val title: String,
    val detail: String?,
    val facts: List<String>,
    val button: String,
    val onClick: () -> Unit,
    val progress: Float? = null,
    val progressText: String? = null,
    val quick: (() -> Unit)? = null,
    /** The detail where room is short, as on a phone. */
    val shortDetail: String? = detail,
)

/**
 * The one step to take first, as [NextUp] chooses it: the cards when many are due, the lesson or the book
 * under way, the cards still due, a course to start, or the courses. It holds the page's only filled button.
 * A wide window shows it beside a picture of its kind, with how far it takes the day's goal; a phone beside its icon.
 */
@Composable
internal fun NextUpCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val next = state.nextUp
    val view = view(next, actions)
    val compact = LocalWindowWidth.current.isCompact
    // What led here: a first visit starts here, and a finished course is said before the next one.
    val finished = state.finishedCourse?.takeIf { next is NextUp.Lesson && !next.started }
    val label = when {
        state.firstVisit -> tr("Start here")
        finished != null -> tr("You finished {0}. Next:", finished.course.title.inLearnedScript())
        // A wide window's card needs no label: its picture and size say it comes first.
        compact -> tr("Next up")
        else -> null
    }
    val labelText = @Composable {
        label?.let {
            Text(
                if (compact) it.uppercase() else it,
                style = if (compact) AppText.overline else AppText.labelStrong,
                color = if (finished != null) AppAccents.green else colors.primary,
                modifier = Modifier.testTag("next-up-label"),
            )
        }
    }
    val buttons = @Composable {
        val quick = view.quick
        ButtonPair(
            // On a phone the play icon is left out, so both buttons fit side by side.
            primary = { m -> PrimaryButton(view.button, onClick = view.onClick, modifier = m.heightIn(min = BUTTON_HEIGHT).testTag("next-up-action"), size = ButtonSize.Large, icon = if (compact) null else AppIcons.PlayArrow) },
            secondary = quick?.let { onQuick ->
                { m -> SecondaryButton(tr("Quick {0}", NextUp.QUICK_SESSION), onClick = onQuick, modifier = m.heightIn(min = BUTTON_HEIGHT).testTag("next-up-quick"), size = ButtonSize.Large, icon = AppIcons.Speed) }
            },
        )
    }
    val progress = @Composable {
        if (view.progress != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                ProgressTrack(view.progress, view.tint, Modifier.weight(1f), height = AppSizes.track)
                view.progressText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
        }
        state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
    }
    if (compact) {
        // The buttons run nearer the card's edges than its words, so both fit side by side on most phones.
        Column(
            modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(horizontal = AppSpacing.m, vertical = AppSpacing.l).testTag("next-up"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Box(Modifier.padding(horizontal = AppSpacing.xs)) { labelText() }
            Row(Modifier.padding(horizontal = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                IconTile(view.icon, size = 52, tint = view.tint)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Text(view.title, style = AppText.headline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    view.shortDetail?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    val facts = listOfNotNull(next.minutes?.let { Fact(null, tr("~{0} min", it)) }) + view.facts.map { Fact(null, it) }
                    if (facts.isNotEmpty()) Facts(facts)
                }
            }
            Column(Modifier.padding(horizontal = AppSpacing.xs)) { progress() }
            buttons()
        }
        return
    }
    Row(
        modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(AppSpacing.xxl).testTag("next-up"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl),
    ) {
        // Where the card is narrow, its words need the room more than the picture.
        if (LocalWindowWidth.current.isExpanded) {
            if (view.face == ArtFace.CARDS) FlashcardsIllustration(Modifier.size(ART_SIZE).testTag("next-up-art"))
            else NextUpArt(view.face, view.tint, view.icon, Modifier.size(ART_SIZE))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            labelText()
            Text(view.title, style = AppText.pageTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            view.detail?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            val goal = state.goal
            val hint = if (goal.goal > 0 && !goal.reached) {
                Fact(
                    AppIcons.Target,
                    if (goal.fraction >= 0.5f) trPlural(goal.left, "Just {0} minute to reach today's goal.", "Just {0} minutes to reach today's goal.")
                    else trPlural(goal.left, "{0} minute to go for today's goal.", "{0} minutes to go for today's goal."),
                    AppAccents.green,
                )
            } else null
            val facts = listOfNotNull(next.minutes?.let { Fact(AppIcons.Schedule, tr("~{0} min", it)) }) + view.facts.map { Fact(null, it) } + listOfNotNull(hint)
            if (facts.isNotEmpty()) Facts(facts)
            progress()
            Spacer(Modifier.height(AppSpacing.s))
            buttons()
        }
    }
}

@Composable
private fun view(next: NextUp, actions: HomeActions): NextUpView = when (next) {
    is NextUp.Review -> NextUpView(
        AppIcons.Style, AppAccents.teal, ArtFace.CARDS,
        title = trPlural(next.due, "{1} card due today", "{1} cards due today", formatCount(next.due)),
        detail = tr("Review them to keep your words fresh."),
        shortDetail = tr("Keep your words fresh"),
        facts = emptyList(),
        button = tr("Review flashcards"),
        onClick = { actions.onNavigate(Route.Flashcards()) },
        quick = if (next.due > NextUp.QUICK_SESSION) ({ actions.onNavigate(Route.Flashcards(limit = NextUp.QUICK_SESSION)) }) else null,
    )
    is NextUp.Lesson -> {
        val total = next.course.lessons.size
        NextUpView(
            AppIcons.School, AppAccents.blue, ArtFace.PAGE,
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
        AppIcons.MenuBook, AppAccents.purple, ArtFace.PAGE,
        title = next.book.title.inLearnedScript(),
        detail = pageLine(next.book),
        facts = emptyList(),
        button = tr("Continue book"),
        onClick = { actions.onNavigate(Route.Read(next.book.id)) },
        progress = bookPercent(next.book) / 100f,
        progressText = "${bookPercent(next.book)}%",
    )
    NextUp.StartHere -> NextUpView(
        AppIcons.School, AppAccents.blue, ArtFace.SUMMIT,
        title = tr("Start with a course"),
        detail = tr("Short stories at your level. The words you save become flashcards."),
        facts = emptyList(),
        button = tr("Browse courses"),
        onClick = { actions.onNavigate(Route.Courses) },
    )
}

/** A short fact about the next step, with its icon and colour where it has them. */
private class Fact(val icon: ImageVector?, val text: String, val color: Color? = null)

/** Short facts in a row, between thin bars: the time a step takes, what it brings, and how near it takes the day's goal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Facts(facts: List<Fact>) {
    val style = if (LocalWindowWidth.current.isCompact) AppText.label else AppText.emphasis
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = AppSpacing.xxs).testTag("next-up-facts"),
    ) {
        facts.forEachIndexed { i, fact ->
            if (i > 0) AppVerticalDivider(Modifier.height(AppSizes.iconSmall))
            val color = fact.color ?: colors.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                fact.icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(AppSizes.icon)) }
                Text(fact.text, style = style, color = color)
            }
        }
    }
}

/**
 * The main button with the further one after it, side by side where both fit at their size, the main one taking
 * the room left; one under the other where they do not, as on a narrow phone or in a longer language.
 */
@Composable
private fun ButtonPair(primary: @Composable (Modifier) -> Unit, secondary: (@Composable (Modifier) -> Unit)?) {
    val spacing = if (LocalWindowWidth.current.isCompact) AppSpacing.s else AppSpacing.m
    Layout({ primary(Modifier); secondary?.invoke(Modifier) }) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val main = measurables[0]
        val other = measurables.getOrNull(1)
        if (other == null) {
            val p = main.measure(Constraints.fixedWidth(width))
            return@Layout layout(width, p.height) { p.place(0, 0) }
        }
        val otherWidth = other.maxIntrinsicWidth(Constraints.Infinity)
        if (main.maxIntrinsicWidth(Constraints.Infinity) + gap + otherWidth <= width) {
            val o = other.measure(Constraints.fixedWidth(otherWidth))
            val p = main.measure(Constraints(minWidth = width - gap - otherWidth, maxWidth = width - gap - otherWidth, minHeight = o.height))
            val height = maxOf(p.height, o.height)
            layout(width, height) {
                p.placeRelative(0, (height - p.height) / 2)
                o.placeRelative(width - otherWidth, (height - o.height) / 2)
            }
        } else {
            val p = main.measure(Constraints.fixedWidth(width))
            val o = other.measure(Constraints.fixedWidth(width))
            layout(width, p.height + gap + o.height) {
                p.place(0, 0)
                o.place(0, p.height + gap)
            }
        }
    }
}

/**
 * Everything else under way, as rows: the course, the book, the playlist and the flashcards, leaving out
 * whatever the next step already offers. A finished course says so; with no cards due, they are all caught up.
 */
@Composable
internal fun ContinueCard(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier) {
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
                progress = if (course.lessons.isEmpty()) null else course.completed.toFloat() / course.lessons.size,
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
            ContinueRow(
                tr("Continue book"), book.title.inLearnedScript(), pageLine(book), AppIcons.MenuBook, AppAccents.purple, "book",
                trailing = "${bookPercent(book)}%", progress = bookPercent(book) / 100f,
            ) { actions.onNavigate(Route.Read(book.id)) }
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
    val edge = if (compact) AppSpacing.l else AppSpacing.xxl
    Column(modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(vertical = AppSpacing.s).testTag("continue")) {
        // A phone's card says what it is with its icon; a wide one's title stands alone, as its neighbours' pictures say enough.
        CardHeader(
            tr("Continue learning"),
            icon = if (compact) AppIcons.PlayArrow else null,
            tint = AppAccents.purple,
            modifier = Modifier.padding(start = edge, end = edge, top = if (compact) AppSpacing.s else AppSpacing.l, bottom = AppSpacing.xs),
            size = CardHeaderSize.Dense,
        )
        rows.forEachIndexed { i, row ->
            if (i > 0) AppDivider(Modifier.padding(horizontal = edge))
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
        progress = playlist.progress.takeIf { percent > 0 },
        onClick = onContinue,
    )
}

/**
 * A row of something under way: its kind, its title and where it stands, with a bar of how far it has got on a
 * wide window; the whole row goes on with it.
 */
@Composable
private fun ContinueRow(
    label: String,
    title: String,
    detail: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    trailing: String? = null,
    progress: Float? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxl, vertical = AppSpacing.m).testTag("row-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.l),
    ) {
        IconTile(icon, size = if (compact) 44 else 60, tint = tint)
        Column(Modifier.weight(1f)) {
            val small = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
            Text(label, style = small, color = colors.onSurfaceVariant, maxLines = 1)
            Text(title, style = if (compact) AppText.itemTitle else AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = small, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) Text(trailing, style = if (compact) AppText.label else AppText.emphasis, color = colors.onSurfaceVariant)
        if (!compact && progress != null) ProgressTrack(progress, tint, Modifier.width(ROW_BAR_WIDTH), height = AppSizes.trackThick)
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

private val ART_SIZE = 200.dp
private val BUTTON_HEIGHT = 56.dp
private val ROW_BAR_WIDTH = 160.dp
