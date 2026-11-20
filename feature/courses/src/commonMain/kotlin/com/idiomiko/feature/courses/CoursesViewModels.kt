package com.idiomiko.feature.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.courses.CourseJourney
import com.idiomiko.core.domain.courses.CourseKnowledge
import com.idiomiko.core.domain.courses.CoursePackService
import com.idiomiko.core.domain.courses.CoursePacks
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.frequency.VocabularyProgress
import com.idiomiko.core.domain.frequency.WordFrequencyService
import com.idiomiko.core.domain.home.LevelSuggestions
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    /** The words known against the frequency list; null until counted, or where nothing counts them. */
    val vocabulary: VocabularyProgress? = null,
    /** The share of each course's words the reader knows, by course id; a course not counted yet is missing. */
    val knownShares: Map<String, Int> = emptyMap(),
    val search: String = "",
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
    /** The courses matching the search (in titles, descriptions, topics, tags and lesson titles) and the tags, whatever their progress. */
    private val searched: List<CourseProgress> by lazy {
        val query = search.trim()
        val wanted = tags.map { it.lowercase() }
        courses.filter { progress ->
            val course = progress.course
            val own = course.tags.map { it.lowercase() }
            (wanted.isEmpty() || if (matchAllTags) own.containsAll(wanted) else wanted.any { it in own }) &&
                (query.isEmpty() || (listOf(course.title, course.description, course.topic) + course.tags).any { it.contains(query, ignoreCase = true) } ||
                    course.lessons.any { it.title.contains(query, ignoreCase = true) })
        }
    }

    /** Every tag the courses have, the most used first. */
    val availableTags: List<String> by lazy {
        courses.flatMap { it.course.tags }.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() }).map { it.key }
    }

    /** How many of the searched courses each progress filter holds, null being all of them. */
    val counts: Map<LessonStatus?, Int> by lazy {
        searched.let { list -> mapOf<LessonStatus?, Int>(null to list.size) + LessonStatus.entries.associateWith { st -> list.count { it.status == st } } }
    }

    /** The courses to show, in the chosen order. */
    val shown: List<CourseProgress> by lazy {
        val list = searched.filter { status == null || it.status == status }
        when (sort) {
            CoursesSort.RECOMMENDED -> list
            CoursesSort.RECENTLY_READ -> list.sortedByDescending { it.lastRead }
            CoursesSort.TITLE -> list.sortedBy { it.course.title.lowercase() }
            CoursesSort.PROGRESS -> list.sortedByDescending { it.fraction }
        }
    }

    /** Whether a search, a filter or another order narrows or reorders the courses, so they show as results rather than as the journey. */
    val browsing: Boolean get() = search.isNotBlank() || status != null || tags.isNotEmpty() || sort != CoursesSort.RECOMMENDED

    /** The filters on, for the badge of the button that opens them: the status, the tags, and an order other than the recommended one. */
    val filterCount: Int get() = (if (status != null) 1 else 0) + tags.size + (if (sort != CoursesSort.RECOMMENDED) 1 else 0)

    /** The levels of the courses, how far the reader has got through each, and the one the words known place them at. */
    val journey: CourseJourney by lazy { CourseJourney.of(courses, vocabulary?.known) }

    /** The course being read most recently, to go on with. */
    val continueWith: CourseProgress? by lazy {
        courses.filter { it.status == LessonStatus.IN_PROGRESS }.maxByOrNull { it.lastRead ?: Instant.DISTANT_PAST }
    }

    /** The course not started yet whose words go just past those known, picked out as the one to read next. */
    val suggested: CourseProgress? by lazy { vocabulary?.let { LevelSuggestions.course(courses, it.known) } }

    /**
     * The lesson to read next, as Home chooses it: the next of the course under way, else the first of the suggested
     * course, else of the first course not started; null when every course is read.
     */
    val nextLesson: NextUp.Lesson? by lazy {
        val toStart = suggested ?: courses.firstOrNull { it.status == LessonStatus.NOT_STARTED && it.lessons.isNotEmpty() }
        NextUp.choose(due = 0, course = continueWith, book = null, toStart = toStart) as? NextUp.Lesson
    }
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
class CoursesViewModel(
    private val service: CourseService,
    languages: LanguageRepository,
    settings: SettingsRepository,
    private val packs: CoursePackService,
    /** Counts the words known against the frequency list, which place the reader on the levels; null leaves them out. */
    frequency: WordFrequencyService? = null,
    /** Works out how much of each course the reader knows; null leaves the shares out. */
    knowledge: CourseKnowledge? = null,
) : ViewModel() {
    init {
        // A language added since the app started gets its sample courses here.
        viewModelScope.launch { service.seedSamples() }
    }

    val events = UiEvents<CoursesEvent>()

    private data class Choices(
        val search: String = "",
        val status: LessonStatus? = null,
        val sort: CoursesSort = CoursesSort.RECOMMENDED,
        val view: CoursesView = CoursesView.GRID,
        val tags: Set<String> = emptySet(),
        val matchAllTags: Boolean = false,
        val error: String? = null,
    )

    private data class Loaded(val language: String, val courses: List<CourseProgress>, val vocabulary: VocabularyProgress?, val knownShares: Map<String, Int>)

    private val choices = MutableStateFlow(Choices())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loaded = settings.settings.map { it.currentLanguageId }.distinctUntilChanged().flatMapLatest { id ->
        val language = languages.getById(id)?.name.orEmpty()
        val courses = service.observeCourses(id)
        val vocabulary = frequency?.observeVocabulary(id)?.map<VocabularyProgress, VocabularyProgress?> { it } ?: flowOf(null)
        // The shares take a moment the first time; the courses show without them meanwhile.
        val shares = knowledge?.observe(id, courses.map { list -> list.map { it.course } })?.onStart { emit(emptyMap()) } ?: flowOf(emptyMap())
        combine(courses, vocabulary, shares) { list, words, known -> Loaded(language, list, words, known) }
    }

    val state: StateFlow<CoursesUiState> = combine(loaded, choices, packs.packs) { data, c, packStates ->
        val list = data.courses
        val pack = LanguageCodes.codeFor(data.language)?.let { code -> packStates.firstOrNull { it.pack.languageCode == code } }
        CoursesUiState(
            loading = false,
            languageName = data.language,
            packAvailable = pack != null,
            packState = pack?.state,
            courses = list,
            vocabulary = data.vocabulary,
            knownShares = data.knownShares,
            search = c.search,
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
    fun setStatus(value: LessonStatus?) = choices.update { it.copy(status = value) }
    fun setSort(value: CoursesSort) = choices.update { it.copy(sort = value) }
    fun setView(value: CoursesView) = choices.update { it.copy(view = value) }
    fun setTags(tags: Set<String>, matchAll: Boolean) = choices.update { it.copy(tags = tags, matchAllTags = matchAll) }

    /** Back to every course in the recommended order: no search, status or tags. */
    fun clearFilters() = choices.update { it.copy(search = "", status = null, sort = CoursesSort.RECOMMENDED, tags = emptySet(), matchAllTags = false) }

    /** Turns one tag on or off, keeping the others and whether a course needs them all. */
    fun toggleTag(tag: String) = choices.update { it.copy(tags = if (tag in it.tags) it.tags - tag else it.tags + tag) }

    /** Downloads the language's ready-made courses; they appear here as soon as they are written. */
    fun downloadCourses() {
        val code = LanguageCodes.codeFor(state.value.languageName) ?: return
        val pack = CoursePacks.forLanguage(code).firstOrNull() ?: return
        viewModelScope.launch { packs.download(pack) }
    }

    /** Opens the next lesson of the course to read it. */
    fun continueCourse(progress: CourseProgress) {
        val lesson = progress.nextLesson ?: return
        openLesson(progress, lesson.lesson.id)
    }

    /** Opens the lesson with [lessonId] of the course to read it. */
    fun openLesson(progress: CourseProgress, lessonId: String) {
        viewModelScope.launch {
            val bookId = try {
                service.openLesson(progress.course.id, lessonId)
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
