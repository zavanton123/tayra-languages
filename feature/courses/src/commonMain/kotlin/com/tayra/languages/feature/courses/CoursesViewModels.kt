package com.tayra.languages.feature.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.i18n.tr
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
import kotlin.time.Instant

enum class CoursesSort { RECOMMENDED, RECENTLY_READ, TITLE, PROGRESS }

enum class CoursesView { GRID, LIST }

data class CoursesUiState(
    val loading: Boolean = true,
    val languageName: String = "",
    /** Whether ready-made courses for the language can be downloaded, offered while there are none. */
    val packAvailable: Boolean = false,
    /** How far the language's ready-made courses are: downloading, failed, or neither; null without a pack. */
    val packState: PackState? = null,
    val courses: List<CourseProgress> = emptyList(),
    val search: String = "",
    /** Null shows every level. */
    val level: CourseLevel? = null,
    /** Null shows courses whatever their progress. */
    val status: LessonStatus? = null,
    val sort: CoursesSort = CoursesSort.RECOMMENDED,
    val view: CoursesView = CoursesView.GRID,
    /** The tags a course must have; empty shows courses whatever their tags. */
    val tags: Set<String> = emptySet(),
    /** Whether a course needs every one of [tags], or any of them. */
    val matchAllTags: Boolean = false,
    /** Why the last lesson could not be opened, if it could not. */
    val error: String? = null,
) {
    /** The courses matching the search (in titles, descriptions, topics, tags and lesson titles), the level and the tags, whatever their progress. */
    private val searched: List<CourseProgress>
        get() {
            val query = search.trim()
            val wanted = tags.map { it.lowercase() }
            return courses.filter { progress ->
                val course = progress.course
                val own = course.tags.map { it.lowercase() }
                (level == null || course.level == level) &&
                    (wanted.isEmpty() || if (matchAllTags) own.containsAll(wanted) else wanted.any { it in own }) &&
                    (query.isEmpty() || (listOf(course.title, course.description, course.topic) + course.tags).any { it.contains(query, ignoreCase = true) } ||
                        course.lessons.any { it.title.contains(query, ignoreCase = true) })
            }
        }

    /** Every tag the courses have, the most used first. */
    val availableTags: List<String>
        get() = courses.flatMap { it.course.tags }.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() }).map { it.key }

    /** How many of the searched courses each progress tab holds, null being all of them. */
    val counts: Map<LessonStatus?, Int>
        get() = searched.let { list -> mapOf<LessonStatus?, Int>(null to list.size) + LessonStatus.entries.associateWith { st -> list.count { it.status == st } } }

    /** The courses to show, in the chosen order. */
    val shown: List<CourseProgress>
        get() {
            val list = searched.filter { status == null || it.status == status }
            return when (sort) {
                CoursesSort.RECOMMENDED -> list
                CoursesSort.RECENTLY_READ -> list.sortedByDescending { it.lastRead }
                CoursesSort.TITLE -> list.sortedBy { it.course.title.lowercase() }
                CoursesSort.PROGRESS -> list.sortedByDescending { it.fraction }
            }
        }

    /** The levels that have a course, for the filter. */
    val levels: List<CourseLevel> get() = courses.map { it.course.level }.distinct().sorted()

    /** The course being read most recently, to go on with. */
    val continueWith: CourseProgress?
        get() = courses.filter { it.status == LessonStatus.IN_PROGRESS }.maxByOrNull { it.lastRead ?: Instant.DISTANT_PAST }
}

/** When a lesson of the course was last opened; null for a course never read. */
internal val CourseProgress.lastRead: Instant? get() = lessons.mapNotNull { it.book?.lastOpened }.maxOrNull()

/** The share of the course's lessons completed, from 0 to 1. */
internal val CourseProgress.fraction: Float get() = if (lessons.isEmpty()) 0f else completed.toFloat() / lessons.size

sealed interface CoursesEvent {
    /** The lesson's text is ready to read. */
    data class Read(val bookId: Long) : CoursesEvent
}

/** The courses of the language being learned. */
class CoursesViewModel(private val service: CourseService, languages: LanguageRepository, settings: SettingsRepository, private val packs: CoursePackService) : ViewModel() {
    init {
        // A language added since the app started gets its sample courses here.
        viewModelScope.launch { service.seedSamples() }
    }

    val events = UiEvents<CoursesEvent>()

    private data class Choices(
        val search: String = "",
        val level: CourseLevel? = null,
        val status: LessonStatus? = null,
        val sort: CoursesSort = CoursesSort.RECOMMENDED,
        val view: CoursesView = CoursesView.GRID,
        val tags: Set<String> = emptySet(),
        val matchAllTags: Boolean = false,
        val error: String? = null,
    )

    private val choices = MutableStateFlow(Choices())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val courses = settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
        .flatMapLatest { id -> service.observeCourses(id).map { list -> (languages.getById(id)?.name.orEmpty()) to list } }

    val state: StateFlow<CoursesUiState> = combine(courses, choices, packs.packs) { (language, list), c, packStates ->
        val pack = LanguageCodes.codeFor(language)?.let { code -> packStates.firstOrNull { it.pack.languageCode == code } }
        CoursesUiState(
            loading = false,
            languageName = language,
            packAvailable = pack != null,
            packState = pack?.state,
            courses = list,
            search = c.search,
            // A level chosen for another language's courses does not hide this one's.
            level = c.level?.takeIf { l -> list.any { it.course.level == l } },
            status = c.status,
            sort = c.sort,
            view = c.view,
            // Tags only another language's courses have do not hide this one's.
            tags = c.tags.filterTo(linkedSetOf()) { tag -> list.any { tag in it.course.tags } },
            matchAllTags = c.matchAllTags,
            error = c.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesUiState())

    fun setSearch(query: String) = choices.update { it.copy(search = query) }
    fun setLevel(value: CourseLevel?) = choices.update { it.copy(level = value) }
    fun setStatus(value: LessonStatus?) = choices.update { it.copy(status = value) }
    fun setSort(value: CoursesSort) = choices.update { it.copy(sort = value) }
    fun setView(value: CoursesView) = choices.update { it.copy(view = value) }
    fun setTags(tags: Set<String>, matchAll: Boolean) = choices.update { it.copy(tags = tags, matchAllTags = matchAll) }

    /** Downloads the language's ready-made courses; they appear here as soon as they are written. */
    fun downloadCourses() {
        val code = LanguageCodes.codeFor(state.value.languageName) ?: return
        val pack = CoursePacks.forLanguage(code).firstOrNull() ?: return
        viewModelScope.launch { packs.download(pack) }
    }

    /** Opens the next lesson of the course to read it. */
    fun continueCourse(progress: CourseProgress) {
        val lesson = progress.nextLesson ?: return
        viewModelScope.launch {
            val bookId = try {
                service.openLesson(progress.course.id, lesson.lesson.id)
            } catch (e: Exception) {
                choices.update { it.copy(error = e.message?.let { message -> tr(message) } ?: tr("Could not open the lesson")) }
                return@launch
            }
            if (bookId == null) choices.update { it.copy(error = tr("This course's language is not set up in the app.")) }
            else events.send(CoursesEvent.Read(bookId))
        }
    }
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
                _state.update { it.copy(error = e.message?.let { message -> tr(message) } ?: tr("Could not open the lesson")) }
                return@launch
            }
            if (bookId == null) _state.update { it.copy(error = tr("This course's language is not set up in the app.")) }
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
                _state.update { it.copy(error = e.message?.let { message -> tr(message) } ?: tr("Could not change the course")) }
            }
        }
    }
}
