package com.tayra.languages.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.CourseValidationException
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.BarSurface
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.TagInput
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LabeledField
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.state.UiEvents
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ---- A course ----

data class CourseFormUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val languageId: Long = 0,
    val languageName: String = "",
    val draft: CourseDraft = CourseDraft(""),
    /** The tags the language's courses have, offered while typing one. */
    val tagSuggestions: List<String> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
)

/** Makes a course, or changes one ([courseId]). */
class CourseFormViewModel(
    private val courseId: String?,
    private val service: CourseService,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseFormUiState())
    val state: StateFlow<CourseFormUiState> = _state.asStateFlow()
    /** The course saved, by id. */
    val events = UiEvents<String>()

    init {
        viewModelScope.launch {
            val course = courseId?.let { service.observeCourse(it).first()?.course }
            val languageId = course?.languageId ?: settings.current.currentLanguageId
            _state.value = CourseFormUiState(
                loading = false,
                isNew = course == null,
                languageId = languageId,
                languageName = languages.getById(languageId)?.name.orEmpty(),
                draft = course?.let { CourseDraft(it.title, it.description, it.level, it.topic, it.tags) } ?: CourseDraft(""),
                tagSuggestions = service.observeCourses(languageId).first().flatMap { it.course.tags }.distinct().sorted(),
                error = if (courseId != null && course == null) tr("This course no longer exists.") else null,
            )
        }
    }

    fun update(change: (CourseDraft) -> CourseDraft) = _state.update { it.copy(draft = change(it.draft), error = null) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = if (courseId == null) service.createCourse(current.languageId, current.draft) else courseId.also { service.updateCourse(it, current.draft) }
                events.send(id)
            } catch (e: CourseValidationException) {
                _state.update { it.copy(error = e.message?.let { message -> tr(message) }) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}

/** The form for a course: its title, what it is about, its level, topic and tags. */
@Composable
fun CourseFormScreen(
    courseId: String?,
    onNavigate: (Route) -> Unit,
    onSaved: (courseId: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: CourseFormViewModel = koinViewModel(key = "course-form-${courseId ?: "new"}") { parametersOf(courseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { onSaved(it) }
    Scaffold(
        topBar = { AppTopBar(title = if (courseId == null) tr("New course") else tr("Edit course"), onNavigate = onNavigate, section = NavSection.COURSES) },
        bottomBar = { if (!state.loading) FormActions(if (state.isNew) tr("Create course") else tr("Save changes"), state.saving, onCancel, viewModel::save) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) { CourseFormContent(state, viewModel::update) }
    }
}

@Composable
internal fun CourseFormContent(state: CourseFormUiState, onChange: ((CourseDraft) -> CourseDraft) -> Unit) {
    ScreenHeader(
        if (state.isNew) tr("New course") else tr("Edit course"),
        if (state.isNew) tr("A course of your own {0} texts, read lesson by lesson.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)) else tr("Change what the course is called and what it is about."),
    )
    state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
    ContentCard(tr("About the course"), tr("Shown on the course's card and page."), icon = AppIcons.MenuBook) {
        LabeledField(tr("Title"), Modifier.padding(bottom = AppSpacing.l)) {
            AppTextField(
                value = state.draft.title,
                onValueChange = { title -> onChange { it.copy(title = title) } },
                placeholder = { Text(tr("For example: News I want to read")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("course-title"),
            )
        }
        LabeledField(tr("Description"), Modifier.padding(bottom = AppSpacing.l)) {
            AppTextField(
                value = state.draft.description,
                onValueChange = { text -> onChange { it.copy(description = text) } },
                placeholder = { Text(tr("What the lessons are about, and who they are for")) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("course-description"),
            )
        }
        val compact = LocalWindowWidth.current.isCompact
        val pair: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                LabeledField(tr("Level"), Modifier.padding(bottom = AppSpacing.l)) {
                    Dropdown(
                        options = CourseLevel.entries,
                        selected = state.draft.level,
                        onSelect = { level -> onChange { it.copy(level = level) } },
                        label = null,
                        optionLabel = { it.display },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        val topic: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                LabeledField(tr("Topic"), Modifier.padding(bottom = AppSpacing.l)) {
                    AppTextField(
                        value = state.draft.topic,
                        onValueChange = { text -> onChange { it.copy(topic = text) } },
                        placeholder = { Text(tr("For example: Travel")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("course-topic"),
                    )
                }
            }
        }
        if (compact) {
            pair(Modifier.fillMaxWidth())
            topic(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                pair(Modifier.weight(1f))
                topic(Modifier.weight(1f))
            }
        }
        LabeledField(tr("Tags"), Modifier.padding(bottom = AppSpacing.l)) {
            TagInput(
                values = state.draft.tags,
                onValuesChange = { tags -> onChange { it.copy(tags = tags) } },
                label = tr("Add tags"),
                suggestions = state.tagSuggestions,
                modifier = Modifier.fillMaxWidth().testTag("course-tags"),
            )
            Text(
                tr("Press Enter or a comma after each tag. Courses can be filtered by their tags."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- A lesson ----

data class LessonFormUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val courseTitle: String = "",
    val languageName: String = "",
    val draft: LessonDraft = LessonDraft("", text = ""),
    val saving: Boolean = false,
    val error: String? = null,
)

/** Adds a lesson to a course, or changes one ([lessonId]). */
class LessonFormViewModel(
    private val courseId: String,
    private val lessonId: String?,
    private val service: CourseService,
    private val languages: LanguageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LessonFormUiState())
    val state: StateFlow<LessonFormUiState> = _state.asStateFlow()
    val events = UiEvents<Unit>()

    init {
        viewModelScope.launch {
            val course = service.observeCourse(courseId).first()?.course
            val lesson = lessonId?.let { id -> course?.lessons?.firstOrNull { it.id == id } }
            _state.value = LessonFormUiState(
                loading = false,
                isNew = lesson == null,
                courseTitle = course?.title.orEmpty(),
                languageName = course?.languageId?.let { languages.getById(it)?.name }.orEmpty(),
                draft = lesson?.let { LessonDraft(it.title, it.summary, it.text) } ?: LessonDraft("", text = ""),
                error = if (course == null) tr("This course no longer exists.") else null,
            )
        }
    }

    fun update(change: (LessonDraft) -> LessonDraft) = _state.update { it.copy(draft = change(it.draft), error = null) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (lessonId == null) service.addLesson(courseId, current.draft) else service.updateLesson(courseId, lessonId, current.draft)
                events.send(Unit)
            } catch (e: CourseValidationException) {
                _state.update { it.copy(error = e.message?.let { message -> tr(message) }) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}

/** The form for a lesson: its title, a line saying what it is about, and the text to read. */
@Composable
fun LessonFormScreen(
    courseId: String,
    lessonId: String?,
    onNavigate: (Route) -> Unit,
    onDone: () -> Unit,
    viewModel: LessonFormViewModel = koinViewModel(key = "lesson-form-$courseId-${lessonId ?: "new"}") { parametersOf(courseId, lessonId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { onDone() }
    Scaffold(
        topBar = { AppTopBar(title = if (lessonId == null) tr("New lesson") else tr("Edit lesson"), onNavigate = onNavigate, section = NavSection.COURSES) },
        bottomBar = { if (!state.loading) FormActions(if (state.isNew) tr("Add lesson") else tr("Save changes"), state.saving, onDone, viewModel::save) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) { LessonFormContent(state, viewModel::update) }
    }
}

@Composable
internal fun LessonFormContent(state: LessonFormUiState, onChange: ((LessonDraft) -> LessonDraft) -> Unit) {
    ScreenHeader(
        if (state.isNew) tr("New lesson") else tr("Edit lesson"),
        when {
            !state.isNew -> tr("Changes to the text keep your place in the lesson as far as they can.")
            state.courseTitle.isEmpty() -> tr("Added at the end of the course; you can move it later.")
            else -> tr("Added at the end of {0}; you can move it later.", state.courseTitle)
        },
    )
    state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
    ContentCard(tr("Lesson"), tr("The title and summary are shown in the course's list of lessons."), icon = AppIcons.Page) {
        LabeledField(tr("Title"), Modifier.padding(bottom = AppSpacing.l)) {
            AppTextField(
                value = state.draft.title,
                onValueChange = { title -> onChange { it.copy(title = title) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lesson-title"),
            )
        }
        LabeledField(tr("Summary"), Modifier.padding(bottom = AppSpacing.l)) {
            AppTextField(
                value = state.draft.summary,
                onValueChange = { text -> onChange { it.copy(summary = text) } },
                placeholder = { Text(tr("One line on what happens in the text")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lesson-summary"),
            )
        }
        LabeledField(tr("Text"), Modifier.padding(bottom = AppSpacing.l)) {
            AppTextField(
                value = state.draft.text,
                onValueChange = { text -> onChange { it.copy(text = text) } },
                placeholder = { Text(state.languageName.ifEmpty { null }?.let { tr("Paste or write the {0} text to read…", languageInSentence(it, LanguageCase.PREPOSITIONAL)) } ?: tr("Paste or write the text to read…")) },
                modifier = Modifier.fillMaxWidth().height(LESSON_TEXT_HEIGHT).testTag("lesson-text"),
            )
            val words = state.draft.text.split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }
            Text(
                trPlural(words, "{0} word", "{0} words"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Shared ----

/** Cancel and save, kept in view at the bottom of the window. */
@Composable
private fun FormActions(saveLabel: String, saving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    BarSurface {
        Column {
            AppDivider()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.widthIn(max = AppSizes.pageWidth).fillMaxWidth().padding(horizontal = if (LocalWindowWidth.current.isCompact) AppSpacing.l else AppSpacing.giant, vertical = AppSpacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(onClick = onCancel) { Text(tr("Cancel"), Modifier.padding(horizontal = AppSpacing.m)) }
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(onClick = onSave, enabled = !saving, modifier = Modifier.testTag("form-save")) {
                        Text(saveLabel, Modifier.padding(horizontal = AppSpacing.m))
                    }
                }
            }
        }
    }
}

private val LESSON_TEXT_HEIGHT = 380.dp
