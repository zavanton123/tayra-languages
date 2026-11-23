package com.idiomiko.feature.courses

import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.CoursesLoadingIllustration
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import com.idiomiko.core.ui.components.rememberToastState
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.feature.playlists.AddToPlaylistMenuButton
import com.idiomiko.feature.playlists.AddToPlaylistPopup
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.ui.components.inLearnedScript
import androidx.compose.foundation.background
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
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.CoursesIllustration
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.TagSize
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.largeText
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.tint
import com.idiomiko.core.ui.theme.IdiomikoTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
private val com.idiomiko.core.domain.courses.Course.subtitle: String
    get() = if (topic.isBlank()) tr(level.label) else "${tr(level.label)} · $topic"

/** How many pills [CourseTags] shows. */
private val com.idiomiko.core.domain.courses.Course.pillCount: Int
    get() = (if (rankUpTo != null) 1 else 0) + if (tags.isNotEmpty()) tags.size else if (builtIn) 1 else 0

/** The course's tags: the words of the frequency list it covers, its own tags, or else whether it is one of the samples. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseTags(course: com.idiomiko.core.domain.courses.Course) {
    if (course.pillCount == 0) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        course.rankUpTo?.let { RankTag(it) }
        // A tag such as "idiomiko" says where a course comes from, so it stands in for the sample tag.
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

/**
 * The button that puts a whole course or one lesson into a playlist: an icon, or a full-width
 * button with the label given. The screens provide it; their content alone shows none.
 */
internal val LocalPlaylistButton = staticCompositionLocalOf<(@Composable (source: PlaylistSource, label: String?) -> Unit)?> { null }

/** Gives [content] the playlist button and the course tiles' playlist menu, which confirm each adding with a toast. */
@Composable
internal fun ProvidePlaylistButton(content: @Composable () -> Unit) {
    val toast = rememberToastState()
    val button: @Composable (PlaylistSource, String?) -> Unit = { source, label ->
        AddToPlaylistMenuButton(source, onAdded = { toast.show(tr("Added to {0}", it)) }, label = label)
    }
    val popup: @Composable (PlaylistSource, Boolean, () -> Unit) -> Unit = { source, open, onDismiss ->
        AddToPlaylistPopup(source, open, onDismiss, onAdded = { toast.show(tr("Added to {0}", it)) })
    }
    CompositionLocalProvider(LocalPlaylistButton provides button, LocalPlaylistPopup provides popup, content = content)
    ToastHost(toast)
}

/** Under a course's title: its topic, else its description; the level is shown beside it already. */
internal val com.idiomiko.core.domain.courses.Course.cardSubtitle: String
    get() = topic.ifBlank { description }

internal fun courseFacts(course: com.idiomiko.core.domain.courses.Course) = "${lessonCount(course.lessons.size)} · ${wordCount(course.wordCount)}"

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
    // The ready-made courses on their way: the page says so, with how far the download has got.
    val downloading = state.packAvailable && state.packState is PackState.Downloading
    Box(Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else AppSpacing.xxl), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = AppSizes.readableWidth).fillMaxWidth()
                .cardSurface(AppShapes.large)
                .padding(horizontal = if (compact) AppSpacing.xl else AppSpacing.giant, vertical = if (compact) AppSpacing.xxxl else AppSpacing.huge)
                .testTag("empty-courses"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // On a phone, or with large text, the picture gives way so the buttons stay on screen; a dark theme dims it.
            val full = if (downloading) LOADING_ILLUSTRATION_SIZE else ILLUSTRATION_SIZE
            val art = Modifier.size(if (compact || largeText) full * SMALL_ILLUSTRATION else full)
                .alpha(if (IdiomikoTheme.current.isDark) AppAlpha.strong else 1f)
            if (downloading) CoursesLoadingIllustration(art) else CoursesIllustration(art)
            Spacer(Modifier.height(AppSpacing.xxl))
            Text(
                when {
                    language.isEmpty() -> tr("Choose a language to learn")
                    downloading -> tr("Preparing your courses")
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
                    downloading -> tr("We're downloading the courses and finding the best place to start.")
                    state.packAvailable -> tr("Download the ready-made {0} courses, or build your own from books and texts you choose.", languageInSentence(language, LanguageCase.PREPOSITIONAL))
                    else -> tr("Build a guided reading course from books and texts you choose.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (language.isEmpty()) return@Column
            if (downloading) {
                Spacer(Modifier.height(AppSpacing.xl))
                Text(tr("Downloading courses…"), style = AppText.labelStrong, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(AppSpacing.s))
                AppProgressBar((state.packState as PackState.Downloading).progress, Modifier.widthIn(max = PROGRESS_WIDTH).fillMaxWidth().testTag("courses-progress"))
            }
            Spacer(Modifier.height(AppSpacing.xxxl))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (state.packAvailable) {
                    // The courses are downloaded from here; they take the page over as soon as they are written.
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
    ProvidePlaylistButton {
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
        // The app's courses are read as they come: only deleting one is left of the editing.
        val own = editing.takeIf { !progress.course.builtIn }
        val deletable = editing
        if (width.isExpanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Top) {
                CourseSummary(progress, onOpenLesson, own, Modifier.weight(1f), deletable)
                LessonList(progress, compact = false, onOpenLesson, own, Modifier.weight(2.05f))
            }
        } else {
            CourseSummary(progress, onOpenLesson, own, Modifier.fillMaxWidth(), deletable)
            LessonList(progress, compact, onOpenLesson, own, Modifier.fillMaxWidth())
        }
    }
}

/** What the course is, how far the reader has got, and the way into the next lesson. */
@Composable
private fun CourseSummary(progress: CourseProgress, onOpenLesson: (String) -> Unit, editing: CourseEditing?, modifier: Modifier, deleting: CourseEditing? = editing) {
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
                if (deleting != null) {
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
            if (course.lessons.isNotEmpty()) LocalPlaylistButton.current?.invoke(PlaylistSource.WholeCourse(course.id), tr("Add course to playlist"))
            if (editing != null) {
                SecondaryButton(tr("Edit course"), onClick = editing.onEdit, modifier = Modifier.fillMaxWidth().testTag("edit-course"), icon = Icons.Default.Edit)
            }
            if (deleting != null && confirm) {
                ConfirmDelete(
                    tr("Delete this course?"),
                    trPlural(course.lessons.size, "{1} and its {0} lesson are deleted, with your reading progress in them. Words you saved stay in your vocabulary.", "{1} and its {0} lessons are deleted, with your reading progress in them. Words you saved stay in your vocabulary.", course.title),
                    onConfirm = { confirm = false; deleting.onDelete() },
                    onDismiss = { confirm = false },
                )
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
                courseId = course.id,
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

/** The mascot stacking the courses is a scene of its own, drawn larger than the book. */
private val LOADING_ILLUSTRATION_SIZE = 320.dp

/** How wide the courses' download progress runs under the text. */
private val PROGRESS_WIDTH = 480.dp
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
    courseId: String,
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
        LocalPlaylistButton.current?.invoke(PlaylistSource.Lesson(courseId, lesson.id), null)
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
