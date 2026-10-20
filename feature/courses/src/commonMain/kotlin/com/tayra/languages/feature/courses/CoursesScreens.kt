package com.tayra.languages.feature.courses

import com.tayra.languages.core.ui.components.inLearnedScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.LessonProgress
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.CountTabs
import com.tayra.languages.core.ui.components.CoursesIllustration
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentStyle
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.Tab
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagFilterButton
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.largeText
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal val CourseLevel.tint: Color
    get() = when (this) {
        CourseLevel.A1 -> AppAccents.green
        CourseLevel.A2 -> AppAccents.blue
        CourseLevel.B1 -> AppAccents.purple
        CourseLevel.B2 -> AppAccents.orange
        CourseLevel.C1 -> AppAccents.red
        CourseLevel.C2 -> AppAccents.teal
    }

internal val LessonStatus.label: String
    get() = when (this) {
        LessonStatus.NOT_STARTED -> tr("Not started")
        LessonStatus.IN_PROGRESS -> tr("In progress")
        LessonStatus.COMPLETED -> tr("Completed")
    }

internal fun lessonCount(n: Int) = trPlural(n, "{0} lesson", "{0} lessons")

internal fun wordCount(n: Int) = trPlural(n, "{1} word", "{1} words", formatCount(n))

internal val CourseLevel.display: String get() = "$code · ${tr(label)}"

/** The level, and the topic when there is one. */
private val com.tayra.languages.core.domain.courses.Course.subtitle: String
    get() = if (topic.isBlank()) tr(level.label) else "${tr(level.label)} · $topic"

/** How many pills [CourseTags] shows. */
private val com.tayra.languages.core.domain.courses.Course.pillCount: Int
    get() = (if (rankUpTo != null) 1 else 0) + if (tags.isNotEmpty()) tags.size else if (builtIn) 1 else 0

/** The course's tags: the words of the frequency list it covers, its own tags, or else whether it is one of the samples. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseTags(course: com.tayra.languages.core.domain.courses.Course) {
    if (course.pillCount == 0) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        course.rankUpTo?.let { RankTag(it) }
        // A tag such as "tayra" says where a course comes from, so it stands in for the sample tag.
        if (course.tags.isNotEmpty()) course.tags.forEach { LabelTag(it) } else if (course.builtIn) SampleTag()
    }
}

/** The most common words a course built on the frequency list covers. */
@Composable
// Which of the language's most common words the course teaches, by their ranks on the frequency list.
internal fun RankTag(rankUpTo: Int) = Tag(tr("Common words {0}–{1}", formatCount(rankUpTo - 99), formatCount(rankUpTo)), AppAccents.blue, size = TagSize.Medium)

/** One of a course's or a lesson's tags. */
@Composable
internal fun LabelTag(label: String) = Tag(label, AppAccents.purple, size = TagSize.Medium)

/** Marks one of the app's sample courses. */
@Composable
internal fun SampleTag() = LabelTag(tr("Sample course"))

/** The courses of the language being learned, to search, filter and sort. */
@Composable
fun CoursesScreen(onNavigate: (Route) -> Unit, viewModel: CoursesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is CoursesEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    if (LocalWindowWidth.current.isCompact) {
        PhoneCoursesScreen(state, viewModel, onNavigate)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Courses"), onNavigate = onNavigate, section = NavSection.COURSES) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        CoursesContent(
            state,
            onSearch = viewModel::setSearch,
            onLevel = viewModel::setLevel,
            onStatus = viewModel::setStatus,
            onOpen = { onNavigate(Route.Course(it)) },
            onNewCourse = { onNavigate(Route.NewCourse) },
            onDownloadCourses = viewModel::downloadCourses,
            onSort = viewModel::setSort,
            onView = viewModel::setView,
            onContinue = viewModel::continueCourse,
            onTags = viewModel::setTags,
            modifier = Modifier.padding(padding),
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CoursesContent(
    state: CoursesUiState,
    onSearch: (String) -> Unit,
    onLevel: (CourseLevel?) -> Unit,
    onStatus: (LessonStatus?) -> Unit,
    onOpen: (courseId: String) -> Unit,
    onNewCourse: () -> Unit = {},
    onDownloadCourses: () -> Unit = {},
    onSort: (CoursesSort) -> Unit = {},
    onView: (CoursesView) -> Unit = {},
    onContinue: (CourseProgress) -> Unit = {},
    onTags: (tags: Set<String>, matchAll: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val shown = state.shown
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(
            tr("Courses"),
            if (state.languageName.isEmpty()) tr("Guided lessons to read, level by level.") else tr("Guided {0} lessons to read, level by level.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            actions = {
                // Without courses the empty state's own button creates the first one.
                if (state.languageName.isNotEmpty() && state.courses.isNotEmpty()) {
                    PrimaryButton(if (compact) tr("Create") else tr("New course"), onClick = onNewCourse, modifier = Modifier.testTag("new-course"), icon = Icons.Default.Add)
                }
            },
        )
        if (state.courses.isEmpty()) {
            EmptyCourses(state, onNewCourse, onDownloadCourses)
            return@ScrollColumn
        }
        // The next step comes first, before the filters, as on Books.
        state.continueWith?.let { ContinueCard(it, compact, onContinue = { onContinue(it) }, onOpen = { onOpen(it.course.id) }) }
        StatusTabs(state.counts, state.status, onStatus)
        val levelLabel = tr("Level: {0}", state.level?.display ?: tr("All"))
        val count = trPlural(shown.size, "{0} course", "{0} courses")
        if (compact) {
            SearchBox(state.search, onSearch, Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m), itemVerticalAlignment = Alignment.CenterVertically) {
                LevelMenu(state, levelLabel, onLevel)
                if (state.availableTags.isNotEmpty()) TagFilterButton(state.availableTags, state.tags, state.matchAllTags, onTags)
                SelectMenu(CoursesSort.entries, state.sort, onSort, { it.label }, buttonModifier = Modifier.testTag("course-sort"))
                SegmentedToggle(viewSegments(iconsOnly = true), state.view, onView)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SearchBox(state.search, onSearch, Modifier.weight(1f))
                LevelMenu(state, levelLabel, onLevel)
                if (state.availableTags.isNotEmpty()) TagFilterButton(state.availableTags, state.tags, state.matchAllTags, onTags)
                SelectMenu(CoursesSort.entries, state.sort, onSort, { it.label }, buttonModifier = Modifier.testTag("course-sort"))
                SegmentedToggle(viewSegments(iconsOnly = !width.isExpanded), state.view, onView)
                Text(count, Modifier.padding(start = AppSpacing.s), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
            }
        }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(state.status?.label ?: tr("All courses"), style = AppText.headline)
                Text(state.sort.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            if (compact) Text(count, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        if (shown.isEmpty()) {
            EmptyState(AppIcons.MenuBook, tr("No courses match"), message = tr("Try another search, or clear the filters."))
            return@ScrollColumn
        }
        val columns = when {
            compact -> 1
            width.isExpanded -> 3
            else -> 2
        }
        val items: @Composable (List<CourseProgress>) -> Unit = { list ->
            if (state.view == CoursesView.LIST) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { list.forEach { CourseRow(it, compact) { onOpen(it.course.id) } } }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    list.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                            row.forEach { CourseCard(it, Modifier.weight(1f).fillMaxHeight(), tagsBelow = compact || it.course.pillCount > 2) { onOpen(it.course.id) } }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (state.sort == CoursesSort.RECOMMENDED) {
            // The recommended order goes level by level, so each level gets its heading.
            shown.groupBy { it.course.level }.forEach { (level, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    LevelHeader(level, list.size)
                    items(list)
                }
            }
        } else {
            items(shown)
        }
    }
}

/** All courses, then those not started, in progress and completed, each with how many it holds. */
@Composable
internal fun StatusTabs(counts: Map<LessonStatus?, Int>, selected: LessonStatus?, onSelect: (LessonStatus?) -> Unit) {
    val tabs = listOf(null, LessonStatus.IN_PROGRESS, LessonStatus.NOT_STARTED, LessonStatus.COMPLETED).map { status ->
        Tab(status, status?.label ?: tr("All"), count = counts[status] ?: 0, testTag = "status-${status?.name ?: "ALL"}")
    }
    CountTabs(tabs, selected, onSelect, style = SegmentStyle.Solid)
}

/** Grid or list, as icons with their names, or [iconsOnly]. */
private fun viewSegments(iconsOnly: Boolean): List<Segment<CoursesView>> =
    listOf(Triple(CoursesView.GRID, AppIcons.GridView, tr("Grid")), Triple(CoursesView.LIST, AppIcons.ViewList, tr("List"))).map { (view, icon, label) ->
        Segment(view, icon, label = if (iconsOnly) null else label, description = label, testTag = "view-${view.name}")
    }

/** The course read most recently, with how far it has got and the way into its next lesson. */
@Composable
private fun ContinueCard(progress: CourseProgress, compact: Boolean, onContinue: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val total = progress.lessons.size
    val shape = AppShapes.large
    val heading: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(AppIcons.MenuBook, size = 52)
            Text(tr("Continue learning"), style = AppText.sectionTitle, softWrap = false)
        }
    }
    val courseInfo: @Composable (Modifier) -> Unit = { m ->
        Row(m.clip(AppShapes.control).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            LevelTile(course.level, 52)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(trPlural(total, "{1} of {0} lesson", "{1} of {0} lessons", progress.completed), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                ProgressBar(progress, Modifier.weight(1f))
                Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        PrimaryButton(onClick = onContinue, modifier = m.height(AppSizes.control).testTag("continue-course")) {
            Text(tr("Continue"), style = AppText.smallTitle)
        }
    }
    val card = Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), shape)
    if (compact) {
        Column(card.padding(AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            heading()
            courseInfo(Modifier)
            bar(Modifier.fillMaxWidth())
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(card.height(IntrinsicSize.Min).padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxxl)) {
            heading()
            AppVerticalDivider()
            courseInfo(Modifier.weight(1f))
            bar(Modifier.weight(1f))
            button(Modifier)
        }
    }
}

/** A level's heading over its courses, in the level's colour. */
@Composable
internal fun LevelHeader(level: CourseLevel, count: Int) {
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).background(level.tint.copy(alpha = AppAlpha.tint)).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(level.code, style = AppText.sectionTitle, color = level.tint)
        Text(" · ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr(level.label), Modifier.weight(1f), style = AppText.itemTitle)
        Text(trPlural(count, "{0} course", "{0} courses"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Under a course's title: its topic, else its description; the level is shown beside it already. */
internal val com.tayra.languages.core.domain.courses.Course.cardSubtitle: String
    get() = topic.ifBlank { description }

internal fun percent(progress: CourseProgress) = "${(progress.fraction * 100).roundToInt()}%"

/** How far the course has got: not started, completed, or lessons read with the share. */
@Composable
internal fun StatusText(progress: CourseProgress, stacked: Boolean) {
    val colors = MaterialTheme.colorScheme
    when (progress.status) {
        LessonStatus.NOT_STARTED -> Text(tr("Not started"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
        LessonStatus.COMPLETED -> Text(tr("Completed"), style = AppText.label, color = AppAccents.green, softWrap = false)
        LessonStatus.IN_PROGRESS -> {
            val read = tr("{0} of {1} read", progress.completed, progress.lessons.size)
            if (stacked) {
                Column {
                    Text(read, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                    Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    Text(read, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                    Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                }
            }
        }
    }
}

@Composable
internal fun ProgressBar(progress: CourseProgress, modifier: Modifier) {
    val color = if (progress.status == LessonStatus.COMPLETED) AppAccents.green else MaterialTheme.colorScheme.primary
    ProgressTrack(if (progress.completed > 0) progress.fraction else 0f, color, modifier, height = AppSizes.trackThick)
}

/** A course as a card of the grid; on narrow screens, or with several, its tags go [tagsBelow] the title, which then keeps its width. */
@Composable
private fun CourseCard(progress: CourseProgress, modifier: Modifier, tagsBelow: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.pressScale(interaction).clip(AppShapes.large).background(colors.surface)
            .border(AppBorders.hairline(if (hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.large)
            .hoverable(interaction).clickable(interaction, LocalIndication.current, onClick = onClick).padding(AppSpacing.xl).testTag("course-${course.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            LevelTile(course.level, 52)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!tagsBelow) CourseTags(course)
        }
        if (tagsBelow) {
            Spacer(Modifier.height(AppSpacing.m))
            CourseTags(course)
        }
        // Cards in a row share the tallest one's height; the facts and progress keep to the bottom.
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(AppSpacing.l))
        // A course without lessons has no progress to show, only that it is still empty.
        if (course.lessons.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("No lessons yet"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        } else {
            Text(courseFacts(course), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(AppSpacing.m))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                ProgressBar(progress, Modifier.weight(1f))
                StatusText(progress, stacked = false)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        }
    }
}

internal fun courseFacts(course: com.tayra.languages.core.domain.courses.Course) = "${lessonCount(course.lessons.size)} · ${wordCount(course.wordCount)}"

/** A course as one row of the list. */
@Composable
private fun CourseRow(progress: CourseProgress, compact: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().pressScale(interaction).clip(AppShapes.card).background(colors.surface)
            .border(AppBorders.hairline(if (hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.card)
            .hoverable(interaction).clickable(interaction, LocalIndication.current, onClick = onClick).padding(horizontal = AppSpacing.l, vertical = AppSpacing.l).testTag("course-${course.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xxl),
    ) {
        LevelTile(course.level, 52)
        if (compact) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(courseFacts(course), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    ProgressBar(progress, Modifier.weight(1f))
                    StatusText(progress, stacked = false)
                }
            }
        } else {
            Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f)) { CourseTags(course) }
            Text(courseFacts(course), Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
            ProgressBar(progress, Modifier.weight(0.9f))
            Box(Modifier.width(ROW_STATUS_WIDTH)) { StatusText(progress, stacked = true) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Composable
internal fun LevelTile(level: CourseLevel, size: Int) {
    IconTile(null, level.code, size = size, tint = level.tint)
}

/** A bar of the lessons read, with the count beside it. */
@Composable
private fun ProgressLine(progress: CourseProgress) {
    val colors = MaterialTheme.colorScheme
    val total = progress.lessons.size
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        ProgressTrack(if (progress.completed > 0) progress.completed.toFloat() / total else 0f, AppAccents.green, Modifier.weight(1f), height = AppSizes.trackThick)
        Text(
            when (progress.status) {
                LessonStatus.COMPLETED -> tr("Completed")
                LessonStatus.NOT_STARTED -> tr("Not started")
                LessonStatus.IN_PROGRESS -> tr("{0} of {1} read", progress.completed, total)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (progress.status == LessonStatus.COMPLETED) AppAccents.green else colors.onSurfaceVariant,
            softWrap = false,
        )
    }
}

/** The courses page of a language without courses: how to make the first one, or download the ready-made ones. */
@Composable
internal fun EmptyCourses(state: CoursesUiState, onNewCourse: () -> Unit, onDownloadCourses: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val language = state.languageName
    Box(Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else AppSpacing.xxl), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = AppSizes.readableWidth).fillMaxWidth()
                .cardSurface(AppShapes.large)
                .padding(horizontal = if (compact) AppSpacing.xl else AppSpacing.giant, vertical = if (compact) AppSpacing.xxxl else AppSpacing.huge)
                .testTag("empty-courses"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // On a phone, or with large text, the picture gives way so the buttons stay on screen; a dark theme dims it.
            CoursesIllustration(
                Modifier.size(if (compact || largeText) ILLUSTRATION_SIZE * SMALL_ILLUSTRATION else ILLUSTRATION_SIZE)
                    .alpha(if (TayraTheme.current.isDark) AppAlpha.strong else 1f),
            )
            Spacer(Modifier.height(AppSpacing.xxl))
            Text(
                when {
                    language.isEmpty() -> tr("Choose a language to learn")
                    state.packAvailable -> tr("Get started with courses")
                    else -> tr("Create your first course")
                },
                style = if (compact) AppText.headline else AppText.screenTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.m))
            Text(
                when {
                    language.isEmpty() -> tr("Choose a language to learn to see its courses.")
                    state.packAvailable -> tr("Download the ready-made {0} courses, or build your own from books and texts you choose.", languageInSentence(language, LanguageCase.PREPOSITIONAL))
                    else -> tr("Build a guided reading course from books and texts you choose.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (language.isEmpty()) return@Column
            Spacer(Modifier.height(AppSpacing.xxxl))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (state.packAvailable) {
                    // The courses are downloaded from here; they take the page over as soon as they are written.
                    val downloading = state.packState is PackState.Downloading
                    PrimaryButton(if (downloading) tr("Downloading…") else tr("Download courses"), onClick = onDownloadCourses, icon = AppIcons.Download, loading = downloading, modifier = Modifier.testTag("download-courses"))
                    SecondaryButton(tr("Create course"), onClick = onNewCourse, modifier = Modifier.testTag("new-course"), icon = Icons.Default.Add)
                } else {
                    PrimaryButton(tr("Create course"), onClick = onNewCourse, modifier = Modifier.testTag("new-course"), icon = Icons.Default.Add)
                }
            }
        }
    }
}

@Composable
internal fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) =
    SearchField(value, onChange, tr("Search courses, lessons and tags"), modifier, fieldModifier = Modifier.testTag("course-search"))

@Composable
private fun LevelMenu(state: CoursesUiState, label: String, onLevel: (CourseLevel?) -> Unit) =
    SelectMenu(listOf<CourseLevel?>(null) + state.levels, state.level, onLevel, { it?.display ?: tr("All levels") }, value = label, buttonModifier = Modifier.testTag("level-filter"))

/** A course: what it is, how far the reader has got, and its lessons to read in order. */
@Composable
fun CourseScreen(
    courseId: String,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseViewModel = koinViewModel(key = "course-$courseId") { parametersOf(courseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is CourseEvent.Read -> onNavigate(Route.Read(event.bookId))
            CourseEvent.Deleted -> onBack()
        }
    }
    Scaffold(
        topBar = { AppTopBar(title = state.progress?.course?.title ?: tr("Course"), onNavigate = onNavigate, section = NavSection.COURSES, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        CourseContent(
            state,
            onOpenLesson = viewModel::openLesson,
            onCourses = onBack,
            editing = CourseEditing(
                onEdit = { onNavigate(Route.EditCourse(courseId)) },
                onDelete = viewModel::deleteCourse,
                onAddLesson = { onNavigate(Route.NewLesson(courseId)) },
                onEditLesson = { onNavigate(Route.EditLesson(courseId, it)) },
                onMoveLesson = viewModel::moveLesson,
                onDeleteLesson = viewModel::deleteLesson,
            ),
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
internal fun CourseContent(
    state: CourseUiState,
    onOpenLesson: (lessonId: String) -> Unit,
    onCourses: () -> Unit,
    editing: CourseEditing = CourseEditing(),
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val progress = state.progress
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        // On a phone the bar has the way back and the course's title.
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically) {
            LinkText(tr("Courses"), onCourses, color = colors.onSurfaceVariant, icon = Icons.AutoMirrored.Filled.ArrowBack)
            if (progress != null) {
                Text("/", Modifier.padding(horizontal = AppSpacing.m), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                Text(progress.course.title.inLearnedScript(), style = AppText.emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (progress == null) {
            EmptyState(AppIcons.MenuBook, tr("Course not found"), message = tr("This course is no longer available."))
            return@ScrollColumn
        }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        val own = editing
        if (width.isExpanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Top) {
                CourseSummary(progress, onOpenLesson, own, Modifier.weight(1f))
                LessonList(progress, compact = false, onOpenLesson, own, Modifier.weight(2.05f))
            }
        } else {
            CourseSummary(progress, onOpenLesson, own, Modifier.fillMaxWidth())
            LessonList(progress, compact, onOpenLesson, own, Modifier.fillMaxWidth())
        }
    }
}

/** What the course is, how far the reader has got, and the way into the next lesson. */
@Composable
private fun CourseSummary(progress: CourseProgress, onOpenLesson: (String) -> Unit, editing: CourseEditing?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    var confirm by remember { mutableStateOf(false) }
    Column(modifier.cardSurface(AppShapes.large)) {
        // A stripe in the level's colour.
        Box(Modifier.fillMaxWidth().height(LEVEL_STRIPE).background(course.level.tint.copy(alpha = AppAlpha.half)))
        Column(Modifier.padding(AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                LevelTile(course.level, 60)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(course.title.inLearnedScript(), style = AppText.headline)
                    Text(course.subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                // Deleting is kept in a menu by the title, away from the buttons that start and edit the course.
                if (editing != null) {
                    OverflowMenu(tr("Course actions"), buttonModifier = Modifier.testTag("course-menu")) { close ->
                        AppMenuItem(text = { Text(tr("Delete course"), color = colors.error) }, onClick = { close(); confirm = true })
                    }
                }
            }
            CourseTags(course)
            if (course.description.isNotBlank()) Text(course.description.inLearnedScript(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Fact(AppIcons.MenuBook, lessonCount(course.lessons.size))
                AppVerticalDivider(Modifier.height(FACT_DIVIDER_HEIGHT))
                Fact(AppIcons.FileOutline, wordCount(course.wordCount))
            }
            AppDivider()
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("Course progress"), style = AppText.itemTitle)
                ProgressLine(progress)
            }
            progress.nextLesson?.let { next ->
                PrimaryButton(onClick = { onOpenLesson(next.lesson.id) }, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Large) {
                    Text(
                        when (progress.status) {
                            LessonStatus.NOT_STARTED -> tr("Start course")
                            LessonStatus.COMPLETED -> tr("Read again from the start")
                            LessonStatus.IN_PROGRESS -> tr("Continue: {0}", next.lesson.title)
                        },
                        Modifier.weight(1f, fill = false),
                        style = AppText.itemTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(AppSpacing.m))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(AppSizes.icon))
                }
            }
            if (editing != null) {
                SecondaryButton(tr("Edit course"), onClick = editing.onEdit, modifier = Modifier.fillMaxWidth().testTag("edit-course"), icon = Icons.Default.Edit)
                if (confirm) {
                    ConfirmDelete(
                        tr("Delete this course?"),
                        trPlural(course.lessons.size, "{1} and its {0} lesson are deleted, with your reading progress in them. Words you saved stay in your vocabulary.", "{1} and its {0} lessons are deleted, with your reading progress in them. Words you saved stay in your vocabulary.", course.title),
                        onConfirm = { confirm = false; editing.onDelete() },
                        onDismiss = { confirm = false },
                    )
                }
            }
        }
    }
}

/** What can be done to a course from its page; null where nothing can. */
internal data class CourseEditing(
    val onEdit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onAddLesson: () -> Unit = {},
    val onEditLesson: (lessonId: String) -> Unit = {},
    val onMoveLesson: (lessonId: String, by: Int) -> Unit = { _, _ -> },
    val onDeleteLesson: (lessonId: String) -> Unit = {},
)

@Composable
private fun ConfirmDelete(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            PrimaryButton(onClick = onConfirm, tone = ButtonTone.Danger, modifier = Modifier.testTag("confirm-delete")) { Text(tr("Delete")) }
        },
        dismissButton = { QuietButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

/** The lessons in order, joined by a line, with the one to read next picked out. */
@Composable
private fun LessonList(progress: CourseProgress, compact: Boolean, onOpenLesson: (String) -> Unit, editing: CourseEditing?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val next = progress.nextLesson?.takeIf { progress.status != LessonStatus.COMPLETED }
    Column(modifier.cardSurface(AppShapes.large)) {
        Row(Modifier.padding(start = if (compact) AppSpacing.xl else AppSpacing.xxxl, end = AppSpacing.xl, top = AppSpacing.xxl, bottom = AppSpacing.xl), verticalAlignment = Alignment.CenterVertically) {
            // The summary beside the list gives the lessons' and words' counts.
            Text(tr("Lessons"), Modifier.weight(1f), style = AppText.headline)
            // The page's main button starts or continues the course; adding a lesson leads only while there is none.
            if (editing != null && progress.lessons.isEmpty()) {
                PrimaryButton(tr("Add lesson"), onClick = editing.onAddLesson, modifier = Modifier.testTag("add-lesson"), icon = Icons.Default.Add)
            } else if (editing != null) {
                SecondaryButton(tr("Add lesson"), onClick = editing.onAddLesson, modifier = Modifier.testTag("add-lesson"), icon = Icons.Default.Add)
            }
        }
        if (progress.lessons.isEmpty()) {
            AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
            Text(
                if (editing != null) tr("No lessons yet. Add the first one: a text you want to read.") else tr("This course has no lessons."),
                Modifier.padding(AppSpacing.xxxl),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        progress.lessons.forEachIndexed { index, lesson ->
            AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
            LessonRow(
                number = index + 1,
                progress = lesson,
                compact = compact,
                first = index == 0,
                last = index == progress.lessons.lastIndex,
                hint = if (lesson === next) (if (progress.status == LessonStatus.NOT_STARTED) tr("Start here") else tr("Continue here")) else null,
                courseTags = progress.course.tags,
                onClick = { onOpenLesson(lesson.lesson.id) },
                editing = editing,
            )
        }
        Spacer(Modifier.height(AppSpacing.s))
    }
}

private val LESSON_CIRCLE = 48.dp
private val LESSON_STATUS_WIDTH = 128.dp
private val ROW_STATUS_WIDTH = 120.dp
private val ILLUSTRATION_SIZE = 220.dp
private const val SMALL_ILLUSTRATION = 0.6f
private val LEVEL_STRIPE = 5.dp
private val FACT_DIVIDER_HEIGHT = 22.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonRow(
    number: Int,
    progress: LessonProgress,
    compact: Boolean,
    first: Boolean,
    last: Boolean,
    hint: String?,
    courseTags: List<String>,
    onClick: () -> Unit,
    editing: CourseEditing?,
) {
    val colors = MaterialTheme.colorScheme
    val lesson = progress.lesson
    val status = progress.status
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val inset = if (compact) AppSpacing.xl else AppSpacing.xxxl
    val line = colors.outlineVariant
    // The row's tint runs edge to edge, square, as the card's dividers do.
    Row(
        Modifier.fillMaxWidth()
            .background(
                when {
                    hint != null -> colors.primary.copy(alpha = AppAlpha.wash)
                    hovered -> colors.primary.copy(alpha = AppAlpha.faint)
                    else -> Color.Transparent
                },
            )
            // The line through the numbers, from the first lesson's to the last one's.
            .drawBehind {
                val x = (inset + LESSON_CIRCLE / 2).toPx()
                val middle = size.height / 2
                drawLine(line, Offset(x, if (first) middle else 0f), Offset(x, if (last) middle else size.height), 1.5.dp.toPx())
            }
            .hoverable(interaction).clickable(onClick = onClick)
            .padding(horizontal = inset, vertical = AppSpacing.xl).testTag("lesson-${lesson.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xxl),
    ) {
        val done = status == LessonStatus.COMPLETED
        Box(
            Modifier.size(LESSON_CIRCLE).clip(AppShapes.circle).background(colors.surface).background(if (done) AppAccents.green else colors.primary.copy(alpha = AppAlpha.tint)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, contentDescription = tr("Completed"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconLarge))
            else Text("$number", style = AppText.sectionTitle, color = colors.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            // The title keeps its width; the tags follow it, or go under it when the line is full.
            FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(lesson.title.inLearnedScript(), Modifier.padding(end = AppSpacing.xs), style = AppText.itemTitle)
                // Only what sets the lesson apart: a tag every lesson of the course has says nothing here.
                lesson.tags.filterNot { it in courseTags }.forEach { LabelTag(it) }
                if (hint != null) Tag(hint, AppAccents.green, size = TagSize.Medium)
            }
            if (lesson.summary.isNotBlank()) Text(lesson.summary.inLearnedScript(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            if (lesson.newWords.isNotEmpty()) {
                Text(
                    tr("New words: {0}", lesson.newWords.joinToString(", ").inLearnedScript()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // A status shows once the lesson is begun; "Not started" on every row would say nothing.
            val started = status != LessonStatus.NOT_STARTED
            if (compact) Text(if (started) "${wordCount(lesson.wordCount)} · ${status.label}" else wordCount(lesson.wordCount), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (!compact) {
            Text(wordCount(lesson.wordCount), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
            val tint = when (status) {
                LessonStatus.COMPLETED -> AppAccents.green
                LessonStatus.IN_PROGRESS -> AppAccents.blue
                LessonStatus.NOT_STARTED -> colors.outline
            }
            if (status != LessonStatus.NOT_STARTED) Tag(status.label, tint, Modifier.width(LESSON_STATUS_WIDTH)) else Spacer(Modifier.width(LESSON_STATUS_WIDTH))
        }
        if (editing != null) LessonMenu(lesson.id, lesson.title, first, last, editing)
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** Editing, moving and deleting a lesson of the reader's own course. */
@Composable
private fun LessonMenu(lessonId: String, title: String, first: Boolean, last: Boolean, editing: CourseEditing) {
    var confirm by remember { mutableStateOf(false) }
    OverflowMenu(tr("Lesson actions"), buttonModifier = Modifier.testTag("lesson-menu-$lessonId")) { close ->
        AppMenuItem(text = { Text(tr("Edit lesson")) }, onClick = { close(); editing.onEditLesson(lessonId) })
        AppMenuItem(text = { Text(tr("Move up")) }, onClick = { close(); editing.onMoveLesson(lessonId, -1) }, enabled = !first)
        AppMenuItem(text = { Text(tr("Move down")) }, onClick = { close(); editing.onMoveLesson(lessonId, 1) }, enabled = !last)
        AppMenuItem(text = { Text(tr("Delete lesson"), color = MaterialTheme.colorScheme.error) }, onClick = { close(); confirm = true })
    }
    if (confirm) {
        ConfirmDelete(
            tr("Delete this lesson?"),
            tr("{0} is deleted, with your reading progress in it. Words you saved stay in your vocabulary.", title),
            onConfirm = { confirm = false; editing.onDeleteLesson(lessonId) },
            onDismiss = { confirm = false },
        )
    }
}
