package com.tayra.languages.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.TagFilterSheet
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route

/** The courses on a phone: the page's own header with the language and a new course, the filters as sheets, and the courses as cards by level. */
@Composable
internal fun PhoneCoursesScreen(state: CoursesUiState, viewModel: CoursesViewModel, onNavigate: (Route) -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = tr("Courses"), onNavigate = onNavigate, section = NavSection.COURSES, actions = { NewCourseAction(state) { onNavigate(Route.NewCourse) } }) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PhoneCoursesContent(
            state,
            PhoneCoursesActions(
                onSearch = viewModel::setSearch,
                onLevel = viewModel::setLevel,
                onStatus = viewModel::setStatus,
                onSort = viewModel::setSort,
                onTags = viewModel::setTags,
                onOpen = { onNavigate(Route.Course(it)) },
                onContinue = viewModel::continueCourse,
                onNewCourse = { onNavigate(Route.NewCourse) },
                onDownloadCourses = viewModel::downloadCourses,
            ),
            Modifier.padding(padding),
        )
    }
}

internal class PhoneCoursesActions(
    val onSearch: (String) -> Unit = {},
    val onLevel: (CourseLevel?) -> Unit = {},
    val onStatus: (LessonStatus?) -> Unit = {},
    val onSort: (CoursesSort) -> Unit = {},
    val onTags: (tags: Set<String>, matchAll: Boolean) -> Unit = { _, _ -> },
    val onOpen: (courseId: String) -> Unit = {},
    val onContinue: (CourseProgress) -> Unit = {},
    val onNewCourse: () -> Unit = {},
    val onDownloadCourses: () -> Unit = {},
)

/** The phone bar's new course; without courses the empty state's own button creates the first one. */
@Composable
internal fun NewCourseAction(state: CoursesUiState, onNewCourse: () -> Unit) {
    if (state.languageName.isEmpty() || state.courses.isEmpty()) return
    FilledIconButton(onClick = onNewCourse, shape = CircleShape, modifier = Modifier.size(44.dp).testTag("new-course")) {
        Icon(Icons.Default.Add, contentDescription = tr("New course"))
    }
}

/** Which sheet is open over the page. */
private enum class PhoneSheet { LEVEL, TAGS, SORT }

@Composable
internal fun PhoneCoursesContent(state: CoursesUiState, actions: PhoneCoursesActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var sheet by remember { mutableStateOf<PhoneSheet?>(null) }
    val shown = state.shown
    ScrollColumn(modifier.fillMaxSize(), contentModifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            if (state.languageName.isEmpty()) tr("Guided lessons, level by level.") else tr("Guided {0} lessons, level by level.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        if (state.courses.isEmpty()) {
            EmptyCourses(state, actions.onNewCourse, actions.onDownloadCourses)
            return@ScrollColumn
        }
        StatusTabs(state.counts, state.status, actions.onStatus)
        SearchBox(state.search, actions.onSearch, Modifier.fillMaxWidth())
        // The level and tags take what their labels need; the sort, the longest, has the rest.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownButton(tr("Level: {0}", state.level?.code ?: tr("All")), Modifier.testTag("level-filter")) { sheet = PhoneSheet.LEVEL }
            if (state.availableTags.isNotEmpty()) {
                val tags = if (state.tags.isEmpty()) tr("Tags") else tr("Tags: {0}", state.tags.size)
                DropdownButton(tags, Modifier.testTag("tag-filter"), icon = AppIcons.Tag, active = state.tags.isNotEmpty()) { sheet = PhoneSheet.TAGS }
            }
            DropdownButton(state.sort.label, Modifier.weight(1f).testTag("course-sort"), fill = true) { sheet = PhoneSheet.SORT }
        }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        state.continueWith?.let { PhoneContinueCard(it, onContinue = { actions.onContinue(it) }, onOpen = { actions.onOpen(it.course.id) }) }
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.status?.label ?: tr("All courses"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(state.sort.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        if (shown.isEmpty()) {
            Notice(tr("No courses match"), tr("Try another search, or clear the filters."))
            return@ScrollColumn
        }
        val cards: @Composable (List<CourseProgress>) -> Unit = { list ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { list.forEach { PhoneCourseCard(it) { actions.onOpen(it.course.id) } } }
        }
        if (state.sort == CoursesSort.RECOMMENDED) {
            // The recommended order goes level by level, so each level gets its heading.
            shown.groupBy { it.course.level }.forEach { (level, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LevelHeader(level, list.size)
                    cards(list)
                }
            }
        } else {
            cards(shown)
        }
    }
    when (sheet) {
        PhoneSheet.LEVEL -> PickerSheet(
            title = tr("Level"),
            options = listOf<CourseLevel?>(null) + state.levels,
            selected = state.level,
            label = { it?.display ?: tr("All levels") },
            onDismiss = { sheet = null },
            onPick = { actions.onLevel(it); sheet = null },
        )
        PhoneSheet.SORT -> PickerSheet(
            title = tr("Sort"),
            options = CoursesSort.entries,
            selected = state.sort,
            label = { it.label },
            onDismiss = { sheet = null },
            onPick = { actions.onSort(it); sheet = null },
        )
        PhoneSheet.TAGS -> TagFilterSheet(
            state.availableTags,
            state.tags,
            state.matchAllTags,
            onApply = { tags, all -> actions.onTags(tags, all); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> {}
    }
}

/** A filter that opens its sheet, drawn as a dropdown. */
@Composable
private fun DropdownButton(label: String, modifier: Modifier, icon: ImageVector? = null, active: Boolean = false, fill: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).background(colors.surface)
            .border(if (active) 1.5.dp else 1.dp, if (active) colors.primary else colors.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp).size(18.dp))
        Text(
            label,
            if (fill) Modifier.weight(1f) else Modifier,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (active) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** The course read most recently: the course, how many lessons are read, and the way into the next one. */
@Composable
private fun PhoneContinueCard(progress: CourseProgress, onContinue: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = 0.05f)).border(1.dp, colors.primary.copy(alpha = 0.15f), shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
            Text(tr("Continue learning"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LevelTile(course.level, 52)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    Text(trPlural(progress.lessons.size, "{1} of {0} lesson", "{1} of {0} lessons", progress.completed), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                ProgressBar(progress, Modifier.fillMaxWidth())
            }
            Button(onClick = onContinue, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(52.dp).testTag("continue-course")) {
                Text(tr("Continue"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * A course as a card: its level, title and topic, the words of the frequency list it covers (or,
 * for a course without them, its own tags), its size, and how far it has got.
 */
@Composable
private fun PhoneCourseCard(progress: CourseProgress, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 18.dp).testTag("course-${course.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LevelTile(course.level, 52)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            course.rankUpTo?.let { Box(Modifier.padding(top = 2.dp)) { RankTag(it) } }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        if (course.rankUpTo == null) OwnTags(course)
        Text(courseFacts(course), Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Row(Modifier.padding(top = 10.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressBar(progress, Modifier.weight(1f))
            StatusText(progress, stacked = false)
        }
    }
}

/** A course of the reader's own carries its tags under the title, where a ready-made one has its words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnTags(course: Course) {
    if (course.tags.isEmpty() && !course.builtIn) return
    FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (course.tags.isNotEmpty()) course.tags.forEach { LabelTag(it) } else SampleTag()
    }
}
