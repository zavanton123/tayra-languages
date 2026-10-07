package com.tayra.languages.feature.courses

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.i18n.formatCount
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.LessonProgress
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val BLUE = Color(0xFF3B6FE0)
private val GREEN = Color(0xFF2E9D57)
private val ORANGE = Color(0xFFD9822B)
private val PURPLE = Color(0xFF7C4DDB)
private val RED = Color(0xFFC94A4A)
private val TEAL = Color(0xFF1F8A8A)

private val CourseLevel.tint: Color
    get() = when (this) {
        CourseLevel.A1 -> GREEN
        CourseLevel.A2 -> BLUE
        CourseLevel.B1 -> PURPLE
        CourseLevel.B2 -> ORANGE
        CourseLevel.C1 -> RED
        CourseLevel.C2 -> TEAL
    }

private val LessonStatus.label: String
    get() = when (this) {
        LessonStatus.NOT_STARTED -> tr("Not started")
        LessonStatus.IN_PROGRESS -> tr("In progress")
        LessonStatus.COMPLETED -> tr("Completed")
    }

private fun lessonCount(n: Int) = trPlural(n, "{0} lesson", "{0} lessons")

private fun wordCount(n: Int) = trPlural(n, "{1} word", "{1} words", formatCount(n))

internal val CourseLevel.display: String get() = "$code · ${tr(label)}"

/** The level, and the topic when there is one. */
private val com.tayra.languages.core.domain.courses.Course.subtitle: String
    get() = if (topic.isBlank()) tr(level.label) else "${tr(level.label)} · $topic"

/** The course's tags: the words of the frequency list it covers, and whether it is one of the samples. */
@Composable
private fun CourseTags(course: com.tayra.languages.core.domain.courses.Course) {
    if (!course.builtIn && course.rankUpTo == null && course.tags.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        course.rankUpTo?.let { RankTag(it) }
        // A tag such as "tayra" says where a course comes from, so it stands in for the sample tag.
        if (course.tags.isNotEmpty()) course.tags.forEach { LabelTag(it) } else if (course.builtIn) SampleTag()
    }
}

/** The most common words a course built on the frequency list covers. */
@Composable
private fun RankTag(rankUpTo: Int) {
    Text(
        tr("Words {0}–{1}", rankUpTo - 99, rankUpTo),
        Modifier.clip(RoundedCornerShape(50)).background(BLUE.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = BLUE,
        fontWeight = FontWeight.SemiBold,
    )
}

/** One of a course's or a lesson's tags. */
@Composable
private fun LabelTag(label: String) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(50)).background(PURPLE.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = PURPLE,
        fontWeight = FontWeight.SemiBold,
        softWrap = false,
    )
}

/** Marks one of the app's sample courses. */
@Composable
private fun SampleTag() {
    Text(
        tr("Sample course"),
        Modifier.clip(RoundedCornerShape(50)).background(PURPLE.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = PURPLE,
        fontWeight = FontWeight.SemiBold,
    )
}

/** The courses of the language being learned, to search, filter and sort. */
@Composable
fun CoursesScreen(onNavigate: (Route) -> Unit, viewModel: CoursesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is CoursesEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
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
            onDownloadCourses = { onNavigate(Route.CoursePacks) },
            onSort = viewModel::setSort,
            onView = viewModel::setView,
            onContinue = viewModel::continueCourse,
            modifier = Modifier.padding(padding),
        )
    }
}

private val CoursesSort.label: String
    get() = when (this) {
        CoursesSort.RECOMMENDED -> tr("Recommended")
        CoursesSort.RECENTLY_READ -> tr("Recently read")
        CoursesSort.TITLE -> tr("Title")
        CoursesSort.PROGRESS -> tr("Progress")
    }

private val CoursesSort.description: String
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
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    val shown = state.shown
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Courses"), style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (state.languageName.isEmpty()) tr("Guided lessons to read, level by level.") else tr("Guided {0} lessons to read, level by level.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            if (state.languageName.isNotEmpty()) {
                Button(onClick = onNewCourse, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("new-course")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (compact) tr("Create") else tr("New course"))
                }
            }
        }
        if (state.courses.isEmpty()) {
            Notice(
                tr("No courses yet"),
                when {
                    state.languageName.isEmpty() -> tr("Choose a language to learn to see its courses.")
                    state.packAvailable -> tr("Download the ready-made {0} courses, or make one of your own with New course, from texts you choose.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))
                    else -> tr("There are no {0} courses yet. Make one of your own with New course, from texts you choose.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))
                },
            ) {
                if (state.packAvailable) {
                    Button(onClick = onDownloadCourses, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("download-courses")) {
                        Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Download courses"))
                    }
                }
            }
            return@Column
        }
        StatusTabs(state.counts, state.status, onStatus)
        val levelLabel = tr("Level: {0}", state.level?.display ?: tr("All"))
        val count = trPlural(shown.size, "{0} course", "{0} courses")
        if (compact) {
            SearchBox(state.search, onSearch, Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                FilterMenu(levelLabel, listOf<CourseLevel?>(null) + state.levels, { it?.display ?: tr("All levels") }, onLevel, Modifier.testTag("level-filter"))
                FilterMenu(state.sort.label, CoursesSort.entries, { it.label }, onSort, Modifier.testTag("course-sort"))
                ViewToggle(state.view, onView, iconsOnly = true)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SearchBox(state.search, onSearch, Modifier.weight(1f))
                FilterMenu(levelLabel, listOf<CourseLevel?>(null) + state.levels, { it?.display ?: tr("All levels") }, onLevel, Modifier.testTag("level-filter"))
                FilterMenu(state.sort.label, CoursesSort.entries, { it.label }, onSort, Modifier.testTag("course-sort"))
                ViewToggle(state.view, onView, iconsOnly = !width.isExpanded)
                Text(count, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
            }
        }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        state.continueWith?.let { ContinueCard(it, compact, onContinue = { onContinue(it) }, onOpen = { onOpen(it.course.id) }) }
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.status?.label ?: tr("All courses"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.sort.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            if (compact) Text(count, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        if (shown.isEmpty()) {
            Notice(tr("No courses match"), tr("Try another search, or clear the filters."))
            return@Column
        }
        val columns = when {
            compact -> 1
            width.isExpanded -> 3
            else -> 2
        }
        val items: @Composable (List<CourseProgress>) -> Unit = { list ->
            if (state.view == CoursesView.LIST) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { list.forEach { CourseRow(it, compact) { onOpen(it.course.id) } } }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    list.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { CourseCard(it, Modifier.weight(1f).fillMaxHeight(), tagsBelow = compact) { onOpen(it.course.id) } }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (state.sort == CoursesSort.RECOMMENDED) {
            // The recommended order goes level by level, so each level gets its heading.
            shown.groupBy { it.course.level }.forEach { (level, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun StatusTabs(counts: Map<LessonStatus?, Int>, selected: LessonStatus?, onSelect: (LessonStatus?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(null, LessonStatus.IN_PROGRESS, LessonStatus.NOT_STARTED, LessonStatus.COMPLETED).forEach { status ->
            val active = status == selected
            Row(
                Modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.primary else colors.surface)
                    .border(1.dp, if (active) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable { onSelect(status) }
                    .padding(horizontal = 20.dp)
                    .testTag("status-${status?.name ?: "ALL"}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(status?.label ?: tr("All"), style = MaterialTheme.typography.titleMedium, color = if (active) colors.onPrimary else colors.onSurface, softWrap = false)
                Text(
                    formatCount(counts[status] ?: 0),
                    Modifier.clip(RoundedCornerShape(50)).background(if (active) colors.onPrimary.copy(alpha = 0.22f) else colors.onSurface.copy(alpha = 0.07f)).padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Grid or list, as icons with their names, or [iconsOnly]. */
@Composable
private fun ViewToggle(selected: CoursesView, onSelect: (CoursesView) -> Unit, iconsOnly: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))) {
        listOf(Triple(CoursesView.GRID, AppIcons.GridView, tr("Grid")), Triple(CoursesView.LIST, AppIcons.ViewList, tr("List"))).forEach { (view, icon, label) ->
            val active = view == selected
            Row(
                Modifier.fillMaxHeight().clip(RoundedCornerShape(10.dp))
                    .then(if (active) Modifier.background(colors.primary.copy(alpha = 0.08f)).border(1.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)) else Modifier)
                    .clickable { onSelect(view) }
                    .padding(horizontal = if (iconsOnly) 12.dp else 18.dp)
                    .testTag("view-${view.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(icon, contentDescription = if (iconsOnly) label else null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                if (!iconsOnly) Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (active) colors.primary else colors.onSurface)
            }
        }
    }
}

/** The course read most recently, with how far it has got and the way into its next lesson. */
@Composable
private fun ContinueCard(progress: CourseProgress, compact: Boolean, onContinue: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val total = progress.lessons.size
    val shape = RoundedCornerShape(16.dp)
    val heading: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
            }
            Text(tr("Continue learning"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, softWrap = false)
        }
    }
    val courseInfo: @Composable (Modifier) -> Unit = { m ->
        Row(m.clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LevelTile(course.level, 52)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(trPlural(total, "{1} of {0} lesson", "{1} of {0} lessons", progress.completed), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ProgressBar(progress, Modifier.weight(1f))
                Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        Button(onClick = onContinue, shape = RoundedCornerShape(12.dp), modifier = m.height(CONTROL_HEIGHT).testTag("continue-course")) {
            Text(tr("Continue"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
    val card = Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = 0.05f)).border(1.dp, colors.primary.copy(alpha = 0.15f), shape)
    if (compact) {
        Column(card.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            heading()
            courseInfo(Modifier)
            bar(Modifier.fillMaxWidth())
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(card.height(IntrinsicSize.Min).padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            heading()
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant))
            courseInfo(Modifier.weight(1f))
            bar(Modifier.weight(1f))
            button(Modifier)
        }
    }
}

/** A level's heading over its courses, in the level's colour. */
@Composable
private fun LevelHeader(level: CourseLevel, count: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(level.tint.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(level.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = level.tint)
        Text(" · ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr(level.label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(trPlural(count, "{0} course", "{0} courses"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Under a course's title: its topic, else its description; the level is shown beside it already. */
private val com.tayra.languages.core.domain.courses.Course.cardSubtitle: String
    get() = topic.ifBlank { description }

private fun percent(progress: CourseProgress) = "${(progress.fraction * 100).roundToInt()}%"

/** How far the course has got: not started, completed, or lessons read with the share. */
@Composable
private fun StatusText(progress: CourseProgress, stacked: Boolean) {
    val colors = MaterialTheme.colorScheme
    when (progress.status) {
        LessonStatus.NOT_STARTED -> Text(tr("Not started"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
        LessonStatus.COMPLETED -> Text(tr("Completed"), style = MaterialTheme.typography.bodyMedium, color = GREEN, fontWeight = FontWeight.Medium, softWrap = false)
        LessonStatus.IN_PROGRESS -> {
            val read = tr("{0} of {1} read", progress.completed, progress.lessons.size)
            if (stacked) {
                Column {
                    Text(read, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                    Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(read, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                    Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: CourseProgress, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.onSurface.copy(alpha = 0.07f))) {
        if (progress.completed > 0) {
            Box(
                Modifier.fillMaxWidth(progress.fraction).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                    .background(if (progress.status == LessonStatus.COMPLETED) GREEN else colors.primary),
            )
        }
    }
}

/** A course as a card of the grid; on narrow screens its tags go [tagsBelow] the title, which then keeps its width. */
@Composable
private fun CourseCard(progress: CourseProgress, modifier: Modifier, tagsBelow: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .border(1.dp, if (hovered) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, RoundedCornerShape(16.dp))
            .hoverable(interaction).clickable(onClick = onClick).padding(20.dp).testTag("course-${course.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LevelTile(course.level, 52)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!tagsBelow) CourseTags(course)
        }
        if (tagsBelow) {
            Spacer(Modifier.height(12.dp))
            CourseTags(course)
        }
        // Cards in a row share the tallest one's height; the facts and progress keep to the bottom.
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        Text(courseFacts(course), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressBar(progress, Modifier.weight(1f))
            StatusText(progress, stacked = false)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

private fun courseFacts(course: com.tayra.languages.core.domain.courses.Course) = "${lessonCount(course.lessons.size)} · ${wordCount(course.wordCount)}"

/** A course as one row of the list. */
@Composable
private fun CourseRow(progress: CourseProgress, compact: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface)
            .border(1.dp, if (hovered) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, RoundedCornerShape(14.dp))
            .hoverable(interaction).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp).testTag("course-${course.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 24.dp),
    ) {
        LevelTile(course.level, 52)
        if (compact) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(courseFacts(course), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProgressBar(progress, Modifier.weight(1f))
                    StatusText(progress, stacked = false)
                }
            }
        } else {
            Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f)) { CourseTags(course) }
            Text(courseFacts(course), Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
            ProgressBar(progress, Modifier.weight(0.9f))
            Box(Modifier.width(120.dp)) { StatusText(progress, stacked = true) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun LevelTile(level: CourseLevel, size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(level.tint.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Text(level.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = level.tint)
    }
}

/** A bar of the lessons read, with the count beside it. */
@Composable
private fun ProgressLine(progress: CourseProgress) {
    val colors = MaterialTheme.colorScheme
    val total = progress.lessons.size
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.onSurface.copy(alpha = 0.07f))) {
            if (progress.completed > 0) Box(Modifier.fillMaxWidth(progress.completed.toFloat() / total).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(GREEN))
        }
        Text(
            when (progress.status) {
                LessonStatus.COMPLETED -> tr("Completed")
                LessonStatus.NOT_STARTED -> tr("Not started")
                LessonStatus.IN_PROGRESS -> tr("{0} of {1} read", progress.completed, total)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (progress.status == LessonStatus.COMPLETED) GREEN else colors.onSurfaceVariant,
            softWrap = false,
        )
    }
}

@Composable
private fun Notice(title: String, text: String, action: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp)).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.MenuBook, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        action()
    }
}

private val CONTROL_HEIGHT = 48.dp

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag("course-search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(tr("Search courses and lessons"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun <T> FilterMenu(label: String, options: List<T>, optionLabel: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).background(colors.surface)
                .clickable { open = true }.padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option -> AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) }) }
        }
    }
}

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
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCourses).padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(tr("Courses"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            if (progress != null) {
                Text("/", Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                Text(progress.course.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (progress == null) {
            Notice(tr("Course not found"), tr("This course is no longer available."))
            return@Column
        }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        val own = editing
        if (width.isExpanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
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
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))) {
        // A stripe in the level's colour.
        Box(Modifier.fillMaxWidth().height(5.dp).background(course.level.tint.copy(alpha = 0.55f)))
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LevelTile(course.level, 60)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(course.subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
            }
            CourseTags(course)
            if (course.description.isNotBlank()) Text(course.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Fact(AppIcons.MenuBook, lessonCount(course.lessons.size))
                Box(Modifier.width(1.dp).height(22.dp).background(colors.outlineVariant))
                Fact(AppIcons.FileOutline, wordCount(course.wordCount))
            }
            HorizontalDivider(color = colors.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Course progress"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ProgressLine(progress)
            }
            progress.nextLesson?.let { next ->
                Button(onClick = { onOpenLesson(next.lesson.id) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(
                        when (progress.status) {
                            LessonStatus.NOT_STARTED -> tr("Start course")
                            LessonStatus.COMPLETED -> tr("Read again from the start")
                            LessonStatus.IN_PROGRESS -> tr("Continue: {0}", next.lesson.title)
                        },
                        Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            if (editing != null) {
                var confirm by remember { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = editing.onEdit, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).testTag("edit-course")) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Edit course"))
                    }
                    OutlinedButton(onClick = { confirm = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).testTag("delete-course")) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Delete"), color = colors.error)
                    }
                }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.testTag("confirm-delete")) { Text(tr("Delete")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

/** The lessons in order, joined by a line, with the one to read next picked out. */
@Composable
private fun LessonList(progress: CourseProgress, compact: Boolean, onOpenLesson: (String) -> Unit, editing: CourseEditing?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val next = progress.nextLesson?.takeIf { progress.status != LessonStatus.COMPLETED }
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))) {
        Row(Modifier.padding(start = if (compact) 18.dp else 28.dp, end = 18.dp, top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Lessons"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${lessonCount(course.lessons.size)} · ${wordCount(course.wordCount)}", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            if (editing != null) {
                Button(onClick = editing.onAddLesson, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("add-lesson")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Add lesson"))
                }
            }
        }
        if (progress.lessons.isEmpty()) {
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
            Text(
                if (editing != null) tr("No lessons yet. Add the first one: a text you want to read.") else tr("This course has no lessons."),
                Modifier.padding(28.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        progress.lessons.forEachIndexed { index, lesson ->
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
            LessonRow(
                number = index + 1,
                progress = lesson,
                compact = compact,
                first = index == 0,
                last = index == progress.lessons.lastIndex,
                hint = if (lesson === next) (if (progress.status == LessonStatus.NOT_STARTED) tr("Start here") else tr("Continue here")) else null,
                onClick = { onOpenLesson(lesson.lesson.id) },
                editing = editing,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

private val LESSON_CIRCLE = 48.dp

@Composable
private fun LessonRow(
    number: Int,
    progress: LessonProgress,
    compact: Boolean,
    first: Boolean,
    last: Boolean,
    hint: String?,
    onClick: () -> Unit,
    editing: CourseEditing?,
) {
    val colors = MaterialTheme.colorScheme
    val lesson = progress.lesson
    val status = progress.status
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val inset = if (compact) 10.dp else 22.dp
    val line = colors.outlineVariant
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = if (hint != null) 4.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    hint != null -> colors.primary.copy(alpha = 0.07f)
                    hovered -> colors.primary.copy(alpha = 0.04f)
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
            .padding(horizontal = inset, vertical = 18.dp).testTag("lesson-${lesson.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 26.dp),
    ) {
        val done = status == LessonStatus.COMPLETED
        Box(
            Modifier.size(LESSON_CIRCLE).clip(CircleShape).background(colors.surface).background(if (done) GREEN else colors.primary.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, contentDescription = tr("Completed"), tint = Color.White, modifier = Modifier.size(24.dp))
            else Text("$number", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = colors.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(lesson.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                lesson.tags.forEach { LabelTag(it) }
                if (hint != null) {
                    Text(
                        hint,
                        Modifier.clip(RoundedCornerShape(50)).background(GREEN.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = GREEN,
                        softWrap = false,
                    )
                }
            }
            if (lesson.summary.isNotBlank()) Text(lesson.summary, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            if (lesson.newWords.isNotEmpty()) {
                Text(
                    tr("New words: {0}", lesson.newWords.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (compact) Text("${wordCount(lesson.wordCount)} · ${status.label}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (!compact) {
            Text(wordCount(lesson.wordCount), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
            val tint = when (status) {
                LessonStatus.COMPLETED -> GREEN
                LessonStatus.IN_PROGRESS -> BLUE
                LessonStatus.NOT_STARTED -> colors.outline
            }
            Text(
                status.label,
                Modifier.width(128.dp).clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                softWrap = false,
            )
        }
        if (editing != null) LessonMenu(lesson.id, lesson.title, first, last, editing)
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** Editing, moving and deleting a lesson of the reader's own course. */
@Composable
private fun LessonMenu(lessonId: String, title: String, first: Boolean, last: Boolean, editing: CourseEditing) {
    var open by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("lesson-menu-$lessonId")) {
            Icon(Icons.Default.MoreVert, contentDescription = tr("Lesson actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Edit lesson")) }, onClick = { open = false; editing.onEditLesson(lessonId) })
            AppMenuItem(text = { Text(tr("Move up")) }, onClick = { open = false; editing.onMoveLesson(lessonId, -1) }, enabled = !first)
            AppMenuItem(text = { Text(tr("Move down")) }, onClick = { open = false; editing.onMoveLesson(lessonId, 1) }, enabled = !last)
            AppMenuItem(text = { Text(tr("Delete lesson"), color = MaterialTheme.colorScheme.error) }, onClick = { open = false; confirm = true })
        }
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
