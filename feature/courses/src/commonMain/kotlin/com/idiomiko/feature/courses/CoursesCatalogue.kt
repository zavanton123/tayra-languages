package com.idiomiko.feature.courses

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.courses.CourseJourney
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.Segment
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel

/** What the courses page can do, for its content to call. */
internal class CoursesActions(
    val onSearch: (String) -> Unit = {},
    val onStatus: (LessonStatus?) -> Unit = {},
    val onSort: (CoursesSort) -> Unit = {},
    val onView: (CoursesView) -> Unit = {},
    val onTags: (tags: Set<String>, matchAll: Boolean) -> Unit = { _, _ -> },
    val onToggleTag: (String) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onOpen: (courseId: String) -> Unit = {},
    val onEdit: ((courseId: String) -> Unit)? = null,
    val onOpenLesson: (CourseProgress, lessonId: String) -> Unit = { _, _ -> },
    val onNewCourse: () -> Unit = {},
    val onDownloadCourses: () -> Unit = {},
) {
    val tiles: TileActions get() = TileActions(onOpen, onEdit)

    companion object {
        fun of(viewModel: CoursesViewModel, onNavigate: (Route) -> Unit) = CoursesActions(
            onSearch = viewModel::setSearch,
            onStatus = viewModel::setStatus,
            onSort = viewModel::setSort,
            onView = viewModel::setView,
            onTags = viewModel::setTags,
            onToggleTag = viewModel::toggleTag,
            onClearFilters = viewModel::clearFilters,
            onOpen = { onNavigate(Route.Course(it)) },
            onEdit = { onNavigate(Route.EditCourse(it)) },
            onOpenLesson = viewModel::openLesson,
            onNewCourse = { onNavigate(Route.NewCourse) },
            onDownloadCourses = viewModel::downloadCourses,
        )
    }
}

/** The courses of the language being learned: the reader's level and journey, the next lesson, and the courses level by level. */
@Composable
fun CoursesScreen(onNavigate: (Route) -> Unit, viewModel: CoursesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is CoursesEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    val actions = remember(viewModel) { CoursesActions.of(viewModel, onNavigate) }
    val compact = LocalWindowWidth.current.isCompact
    ProvidePlaylistButton {
        Scaffold(
            topBar = { AppTopBar(title = tr("Courses"), onNavigate = onNavigate, section = NavSection.COURSES) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) { padding ->
            when {
                state.loading -> LoadingIndicator(Modifier.padding(padding))
                compact -> PhoneCoursesContent(state, actions, Modifier.padding(padding))
                else -> CoursesContent(state, actions, Modifier.padding(padding))
            }
        }
    }
}

internal val CoursesSort.label: String
    get() = when (this) {
        CoursesSort.RECOMMENDED -> tr("Recommended")
        CoursesSort.RECENTLY_READ -> tr("Recently read")
        CoursesSort.TITLE -> tr("Title")
        CoursesSort.PROGRESS -> tr("Progress")
    }

internal val CoursesSort.description: String
    get() = when (this) {
        CoursesSort.RECOMMENDED -> tr("Sorted by recommended")
        CoursesSort.RECENTLY_READ -> tr("Sorted by recently read")
        CoursesSort.TITLE -> tr("Sorted by title")
        CoursesSort.PROGRESS -> tr("Sorted by progress")
    }

/** Grid or list, as icons. */
internal fun viewSegments(): List<Segment<CoursesView>> =
    listOf(Triple(CoursesView.GRID, AppIcons.GridView, tr("Grid")), Triple(CoursesView.LIST, AppIcons.ViewList, tr("List"))).map { (view, icon, label) ->
        Segment(view, icon, label = null, description = label, testTag = "view-${view.name}")
    }

/**
 * Where each level's courses are on the page, which of them are open, and the way to scroll to one: a level
 * chosen on the journey opens and comes into view.
 */
@Stable
internal class JourneyNav(val scroll: ScrollState, private val scope: CoroutineScope) {
    /** The levels folded or unfolded by hand; the reader's level starts open, the others folded. */
    val expanded = mutableStateMapOf<CourseLevel, Boolean>()

    /** Whether the finished levels are unfolded. */
    var showFinished by mutableStateOf(false)

    /** The scrolled content, which the levels' places are measured in. */
    var content: LayoutCoordinates? = null

    private val tops = HashMap<CourseLevel, Float>()

    /** Keeps the level's place, for [goTo]. */
    fun mark(level: CourseLevel): Modifier = Modifier.onPlaced { coordinates ->
        content?.takeIf { it.isAttached }?.let { tops[level] = it.localPositionOf(coordinates, Offset.Zero).y }
    }

    /** Opens the [level] (unfolding the finished levels for one of them) and scrolls it into view once it is laid out. */
    fun goTo(level: CourseLevel, journey: CourseJourney) {
        if (journey.finished.any { it.level == level }) showFinished = true
        else expanded[level] = true
        scope.launch {
            withFrameNanos { }
            withFrameNanos { }
            tops[level]?.let { scroll.animateScrollTo(it.roundToInt()) }
        }
    }
}

@Composable
internal fun rememberJourneyNav(): JourneyNav {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    return remember { JourneyNav(scroll, scope) }
}

/**
 * A wide window's courses: under the title, a pane with the journey through the levels, the search and the filters,
 * then the next lesson and the courses of the reader's level, the finished levels folded and the others to open.
 * Where the window is not wide enough for the pane, the page is one column, as on a phone.
 */
@Composable
internal fun CoursesContent(state: CoursesUiState, actions: CoursesActions, modifier: Modifier = Modifier) {
    if (state.courses.isEmpty()) {
        ScrollColumn(modifier.fillMaxSize(), contentModifier = Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            ScreenHeader(tr("Courses"), subtitle = null)
            EmptyCourses(state, actions.onNewCourse, actions.onDownloadCourses)
        }
        return
    }
    if (!LocalWindowWidth.current.isExpanded) {
        CoursesColumn(state, actions, phone = false, modifier = modifier)
        return
    }
    val nav = rememberJourneyNav()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ScrollColumn(
            Modifier.widthIn(max = AppSizes.pageWidth + AppSpacing.xxxl * 2 + AppSizes.scrollbarRoom).fillMaxSize(),
            state = nav.scroll,
            // The same gap on either side of the page, measured to the scrollbar on the right.
            contentModifier = Modifier.padding(start = AppSpacing.xxxl, end = AppSpacing.xxxl + AppSizes.scrollbarRoom, top = AppSpacing.xxl, bottom = AppSpacing.xxl).onPlaced { nav.content = it },
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            ScreenHeader(tr("Courses"), subtitle = null)
            CoursesTopPane(state, actions, onLevel = { nav.goTo(it, state.journey) })
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= FIVE_COLUMNS -> 5
                    maxWidth >= FOUR_COLUMNS -> 4
                    maxWidth >= THREE_COLUMNS -> 3
                    else -> 2
                }
                Catalogue(state, actions, nav, columns, compact = false, picture = maxWidth >= PICTURE_WIDTH)
            }
        }
    }
}

/**
 * The page's main column below its header: the next lesson, then the reader's level with its courses, the finished
 * levels in one line, and the other levels to open. A search, a filter or another order shows the matching courses instead.
 */
@Composable
internal fun Catalogue(state: CoursesUiState, actions: CoursesActions, nav: JourneyNav, columns: Int, compact: Boolean, picture: Boolean) {
    val colors = MaterialTheme.colorScheme
    val upNext = state.suggested ?: state.nextLesson?.course?.takeIf { it.status == LessonStatus.NOT_STARTED }
    val pinOf: (CourseProgress) -> TilePin? = { progress ->
        when {
            progress.status == LessonStatus.IN_PROGRESS -> TilePin.IN_PROGRESS
            progress.course.id == upNext?.course?.id -> TilePin.UP_NEXT
            else -> null
        }
    }
    val items: @Composable (List<CourseProgress>) -> Unit = { list ->
        if (state.view == CoursesView.LIST && !compact) CourseList(list, state.knownShares, pinOf, actions.tiles)
        else CourseGrid(list, columns, state.knownShares, pinOf, actions.tiles)
    }
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xl)) {
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        if (state.browsing) {
            Results(state, actions, items)
            return@Column
        }
        state.nextLesson?.let { next ->
            NextLessonHero(
                next,
                onStart = { actions.onOpenLesson(next.course, next.lesson.lesson.id) },
                onOpenLesson = { actions.onOpenLesson(next.course, it) },
                onAllLessons = { actions.onOpen(next.course.course.id) },
                compact = compact,
                picture = picture,
            )
        }
        val journey = state.journey
        val byLevel = state.courses.groupBy { it.course.level }
        journey.currentProgress?.let { level ->
            val open = nav.expanded[level.level] != false
            Column(nav.mark(level.level), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val known = journey.known
                FoldedLevelHeading(
                    level,
                    open,
                    onToggle = { nav.expanded[level.level] = !open },
                    detail = if (known != null && known > 0) trPlural(known, "Picked for your {1} word", "Picked for your {1} words", formatCount(known)) else null,
                )
                if (open) items(byLevel[level.level].orEmpty())
            }
        }
        val finished = journey.finished
        if (finished.isNotEmpty()) {
            FinishedLevelsBar(finished, nav.showFinished, onToggle = { nav.showFinished = !nav.showFinished })
            if (nav.showFinished) finished.forEach { level ->
                Column(nav.mark(level.level), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    LevelHeading(level.level, trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed))
                    items(byLevel[level.level].orEmpty())
                }
            }
        }
        journey.levels.filter { it.level != journey.current && it !in finished }.forEach { level ->
            val open = nav.expanded[level.level] == true
            Column(nav.mark(level.level), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                FoldedLevelHeading(level, open, onToggle = { nav.expanded[level.level] = !open })
                if (open) items(byLevel[level.level].orEmpty())
            }
        }
    }
}

/** The courses a search, a filter or an order picks: how many, in what order, and a way back to the journey. */
@Composable
private fun Results(state: CoursesUiState, actions: CoursesActions, items: @Composable (List<CourseProgress>) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shown = state.shown
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Column(Modifier.weight(1f)) {
                Text(trPlural(shown.size, "{0} course", "{0} courses"), style = AppText.headline, modifier = Modifier.testTag("result-count"))
                Text(state.sort.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            LinkText(tr("Clear filters"), actions.onClearFilters, Modifier.testTag("clear-filters"), style = MaterialTheme.typography.bodyMedium)
        }
        if (shown.isEmpty()) {
            EmptyState(tr("No courses match"), message = tr("Try another search, or clear the filters."))
        } else if (state.sort == CoursesSort.RECOMMENDED) {
            // The recommended order goes level by level, so each level keeps its heading.
            shown.groupBy { it.course.level }.forEach { (level, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    LevelHeading(level, trPlural(list.size, "{0} course", "{0} courses"))
                    items(list)
                }
            }
        } else {
            items(shown)
        }
    }
}

private val FIVE_COLUMNS = 1400.dp
private val FOUR_COLUMNS = 1000.dp
private val THREE_COLUMNS = 720.dp

/** The main column's width from which the next lesson's card has room for its picture beside the ten stops of its path. */
private val PICTURE_WIDTH = 960.dp
