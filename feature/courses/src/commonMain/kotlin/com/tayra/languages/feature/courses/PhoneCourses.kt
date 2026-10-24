package com.tayra.languages.feature.courses

import com.tayra.languages.core.ui.components.inLearnedScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NavigationRow
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.TagFilterSheet
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

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
    AppIconButton(Icons.Default.Add, tr("New course"), onNewCourse, Modifier.testTag("new-course"), size = IconButtonSize.Large, style = IconButtonStyle.Filled)
}

/** Which sheet is open over the page. */
private enum class PhoneSheet { STATUS, FILTERS, TAGS, SORT }

@Composable
internal fun PhoneCoursesContent(state: CoursesUiState, actions: PhoneCoursesActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var sheet by remember { mutableStateOf<PhoneSheet?>(null) }
    val shown = state.shown
    ScrollColumn(modifier.fillMaxSize(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Text(
            if (state.languageName.isEmpty()) tr("Guided lessons, level by level.") else tr("Guided {0} lessons, level by level.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        if (state.courses.isEmpty()) {
            EmptyCourses(state, actions.onNewCourse, actions.onDownloadCourses)
            return@ScrollColumn
        }
        state.continueWith?.let { PhoneContinueCard(it, onContinue = { actions.onContinue(it) }, onOpen = { actions.onOpen(it.course.id) }) }
        state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
        SearchBox(state.search, actions.onSearch, Modifier.fillMaxWidth())
        val filterCount = (if (state.level != null) 1 else 0) + state.tags.size
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            SelectButton(
                state.status?.label ?: tr("All"),
                onClick = { sheet = PhoneSheet.STATUS },
                modifier = Modifier.weight(1f).testTag("status-filter"),
                active = state.status != null,
            )
            SecondaryButton(
                tr("Filters"),
                onClick = { sheet = PhoneSheet.FILTERS },
                modifier = Modifier.weight(1f).testTag("level-filter"),
                badge = filterCount.takeIf { it > 0 },
            )
            SecondaryButton(tr("Sort"), onClick = { sheet = PhoneSheet.SORT }, modifier = Modifier.weight(1f).testTag("course-sort"))
        }
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(state.status?.label ?: tr("All courses"), Modifier.weight(1f), style = AppText.sectionTitle)
            Text(state.sort.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (shown.isEmpty()) {
            EmptyState(AppIcons.MenuBook, tr("No courses match"), message = tr("Try another search, or clear the filters."))
            return@ScrollColumn
        }
        val cards: @Composable (List<CourseProgress>) -> Unit = { list ->
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { list.forEach { PhoneCourseCard(it) { actions.onOpen(it.course.id) } } }
        }
        if (state.sort == CoursesSort.RECOMMENDED) {
            // The recommended order goes level by level, so each level gets its heading.
            shown.groupBy { it.course.level }.forEach { (level, list) ->
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    PhoneLevelHeader(level, list.size)
                    cards(list)
                }
            }
        } else {
            cards(shown)
        }
    }
    when (sheet) {
        PhoneSheet.STATUS -> PickerSheet(
            title = tr("Status"),
            options = listOf<LessonStatus?>(null) + LessonStatus.entries,
            selected = state.status,
            label = { it?.label ?: tr("All courses") },
            onDismiss = { sheet = null },
            onPick = { actions.onStatus(it); sheet = null },
        )
        PhoneSheet.FILTERS -> PhoneFiltersSheet(
            state = state,
            onLevel = { actions.onLevel(it); sheet = null },
            onTags = { sheet = PhoneSheet.TAGS },
            onDismiss = { sheet = null },
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

/** Level and tag filters share one compact entry point on a phone. */
@Composable
private fun PhoneFiltersSheet(state: CoursesUiState, onLevel: (CourseLevel?) -> Unit, onTags: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(tr("Filters"), style = AppText.itemTitle)
            Text(tr("Level"), style = AppText.emphasis, color = colors.onSurfaceVariant)
            (listOf<CourseLevel?>(null) + state.levels).forEach { level ->
                val selected = level == state.level
                Row(
                    Modifier.fillMaxWidth().heightIn(min = AppSizes.control).clip(AppShapes.compact).clickable { onLevel(level) }
                        .padding(horizontal = AppSpacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(level?.display ?: tr("All levels"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
                }
            }
            if (state.availableTags.isNotEmpty()) {
                NavigationRow(
                    tr("Tags"),
                    onTags,
                    modifier = Modifier.testTag("tag-filter"),
                    value = if (state.tags.isEmpty()) null else state.tags.size.toString(),
                    icon = AppIcons.Tag,
                )
            }
        }
    }
}

/** The course read most recently: the course, how many lessons are read, and the way into the next one. */
@Composable
private fun PhoneContinueCard(progress: CourseProgress, onContinue: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val shape = AppShapes.large
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), shape)
            .padding(AppSpacing.l).testTag("continue-card"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Row(Modifier.fillMaxWidth().clip(AppShapes.control).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            LevelTile(course.level, 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Continue learning"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
                Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Row {
                Text(trPlural(progress.lessons.size, "{1} of {0} lesson", "{1} of {0} lessons", progress.completed), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(percent(progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            ProgressBar(progress, Modifier.fillMaxWidth())
        }
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("continue-course"), size = ButtonSize.Large)
    }
}

/** A concise phone card: its level, title, topic, size or frequency range, and one progress state. */
@Composable
private fun PhoneCourseCard(progress: CourseProgress, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val course = progress.course
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().pressScale(press).cardSurface(AppShapes.large)
            .clickable(press, LocalIndication.current, onClick = onClick).padding(start = AppSpacing.l, end = AppSpacing.m, top = AppSpacing.l, bottom = AppSpacing.l).testTag("course-${course.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            LevelTile(course.level, 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(course.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.cardSubtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (course.lessons.isNotEmpty()) {
                    Text(phoneCourseFacts(course), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.padding(top = AppSpacing.xxs))
        }
        if (course.lessons.isEmpty()) {
            Text(tr("No lessons yet"), Modifier.padding(top = AppSpacing.m), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        } else {
            Row(Modifier.padding(top = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                ProgressBar(progress, Modifier.weight(1f))
                PhoneProgressText(progress)
            }
        }
    }
}

@Composable
private fun PhoneProgressText(progress: CourseProgress) {
    val colors = MaterialTheme.colorScheme
    when (progress.status) {
        LessonStatus.NOT_STARTED -> Text(tr("Not started"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        LessonStatus.COMPLETED -> Text(tr("Completed"), style = AppText.label, color = AppAccents.green)
        LessonStatus.IN_PROGRESS -> Text(
            tr("{0} of {1} read", progress.completed, progress.lessons.size),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun phoneCourseFacts(course: Course): String = buildList {
    add(lessonCount(course.lessons.size))
    course.rankUpTo?.let { add(tr("Common words {0}–{1}", formatCount(it - 99), formatCount(it))) }
}.joinToString(" · ")

@Composable
private fun PhoneLevelHeader(level: CourseLevel, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.s).testTag("level-header-${level.name}"), verticalAlignment = Alignment.CenterVertically) {
        Text(level.code, style = AppText.itemTitle, color = level.tint)
        Text(trPlural(count, "{0} course", "{0} courses"), Modifier.padding(start = AppSpacing.m), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
