package com.tayra.languages.feature.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoursesUiState(
    val loading: Boolean = true,
    val languageName: String = "",
    /** Whether ready-made courses for the language can be downloaded, offered while there are none. */
    val packAvailable: Boolean = false,
    val courses: List<CourseProgress> = emptyList(),
    val search: String = "",
    /** Null shows every level. */
    val level: CourseLevel? = null,
    /** Null shows courses whatever their progress. */
    val status: LessonStatus? = null,
) {
    /** The courses matching the search, in titles, descriptions, topics and lesson titles, and the filters. */
    val shown: List<CourseProgress>
        get() {
            val query = search.trim()
            return courses.filter { progress ->
                val course = progress.course
                (level == null || course.level == level) &&
                    (status == null || progress.status == status) &&
                    (query.isEmpty() || listOf(course.title, course.description, course.topic).any { it.contains(query, ignoreCase = true) } ||
                        course.lessons.any { it.title.contains(query, ignoreCase = true) })
            }
        }

    /** The levels that have a course, for the filter. */
    val levels: List<CourseLevel> get() = courses.map { it.course.level }.distinct().sorted()
}

/** The courses of the language being learned. */
class CoursesViewModel(service: CourseService, languages: LanguageRepository, settings: SettingsRepository) : ViewModel() {
    init {
        // A language added since the app started gets its sample courses here.
        viewModelScope.launch { service.seedSamples() }
    }

    private val search = MutableStateFlow("")
    private val level = MutableStateFlow<CourseLevel?>(null)
    private val status = MutableStateFlow<LessonStatus?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val courses = settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
        .flatMapLatest { id -> service.observeCourses(id).map { list -> (languages.getById(id)?.name.orEmpty()) to list } }

    val state: StateFlow<CoursesUiState> = combine(courses, search, level, status) { (language, list), query, lvl, st ->
        val packAvailable = LanguageCodes.codeFor(language)?.let { CoursePacks.forLanguage(it).isNotEmpty() } == true
        // A level chosen for another language's courses does not hide this one's.
        CoursesUiState(loading = false, languageName = language, packAvailable = packAvailable, courses = list, search = query, level = lvl?.takeIf { l -> list.any { it.course.level == l } }, status = st)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesUiState())

    fun setSearch(query: String) { search.value = query }
    fun setLevel(value: CourseLevel?) { level.value = value }
    fun setStatus(value: LessonStatus?) { status.value = value }
}

data class CourseUiState(val loading: Boolean = true, val progress: CourseProgress? = null, val error: String? = null)

sealed interface CourseEvent {
    /** The lesson's text is ready to read. */
    data class Read(val bookId: Long) : CourseEvent

    /** The course was deleted. */
    data object Deleted : CourseEvent
}

/** One course with its lessons. */
class CourseViewModel(private val courseId: String, private val service: CourseService) : ViewModel() {
    private val _state = MutableStateFlow(CourseUiState())
    val state: StateFlow<CourseUiState> = _state.asStateFlow()
    val events = UiEvents<CourseEvent>()

    init {
        viewModelScope.launch {
            service.observeCourse(courseId).collect { progress -> _state.update { it.copy(loading = false, progress = progress) } }
        }
    }

    fun openLesson(lessonId: String) {
        viewModelScope.launch {
            val bookId = try {
                service.openLesson(courseId, lessonId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not open the lesson") }
                return@launch
            }
            if (bookId == null) _state.update { it.copy(error = "This course's language is not set up in the app.") }
            else events.send(CourseEvent.Read(bookId))
        }
    }

    // Changing one of the reader's own courses.

    fun deleteCourse() = change {
        service.deleteCourse(courseId)
        events.send(CourseEvent.Deleted)
    }

    fun deleteLesson(lessonId: String) = change { service.deleteLesson(courseId, lessonId) }

    fun moveLesson(lessonId: String, by: Int) = change { service.moveLesson(courseId, lessonId, by) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not change the course") }
            }
        }
    }
}
