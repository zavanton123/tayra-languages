package com.tayra.languages.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.stats.StudyActivity
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class HomeUiState(
    val loading: Boolean = true,
    /** The language being learned; empty before one is chosen. */
    val languageName: String = "",
    val activity: StudyActivity? = null,
    /** The course to go on with: the one read most recently, or else the first not started; null when there is none. */
    val course: CourseProgress? = null,
    /** The book being read most recently; null when no book is being read. */
    val book: BookListItem? = null,
    val flashcardsDue: Int = 0,
    /** Why the course's next lesson could not be opened, if it could not. */
    val error: String? = null,
)

sealed interface HomeEvent {
    /** The text with [bookId] is ready to read. */
    data class Read(val bookId: Long) : HomeEvent
}

/** Today's progress in the language being learned, and the ways back into its courses, books and flashcards. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val courses: CourseService,
    books: BookRepository,
    flashcards: FlashcardService,
    private val stats: StatsService,
    languages: LanguageRepository,
    settings: SettingsRepository,
) : ViewModel() {
    init {
        // A language added since the app started gets its sample courses here.
        viewModelScope.launch { courses.seedSamples() }
    }

    val events = UiEvents<HomeEvent>()

    // The study records have no flow of their own, so they are read again whenever the page comes back.
    private val refreshes = MutableStateFlow(0)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> = settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
        .flatMapLatest { id ->
            val language = languages.getById(id)
            combine(courses.observeCourses(id), books.observeBooks(archived = false), flashcards.observeDueCount(), refreshes, error) { courseList, bookList, due, _, problem ->
                HomeUiState(
                    loading = false,
                    languageName = language?.name.orEmpty(),
                    course = courseToContinue(courseList),
                    book = bookList.filter { it.languageId == id && it.lastOpened != null && !it.isCompleted }.maxByOrNull { it.lastOpened ?: Instant.DISTANT_PAST },
                    flashcardsDue = due,
                    error = problem,
                )
            }.mapLatest { state -> state.copy(activity = language?.let { stats.studyActivity(it.id, it.name) }) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() = refreshes.update { it + 1 }

    /** Opens the next lesson of the course to go on with. */
    fun continueCourse() {
        val progress = state.value.course ?: return
        val lesson = progress.nextLesson ?: return
        error.value = null
        viewModelScope.launch {
            val bookId = try {
                courses.openLesson(progress.course.id, lesson.lesson.id)
            } catch (e: Exception) {
                error.value = e.message?.let { tr(it) } ?: tr("Could not open the lesson")
                return@launch
            }
            if (bookId == null) error.value = tr("This course's language is not set up in the app.") else events.send(HomeEvent.Read(bookId))
        }
    }

    private fun courseToContinue(list: List<CourseProgress>): CourseProgress? =
        list.filter { it.status == LessonStatus.IN_PROGRESS }.maxByOrNull { progress -> progress.lessons.mapNotNull { it.book?.lastOpened }.maxOrNull() ?: Instant.DISTANT_PAST }
            ?: list.firstOrNull { it.status == LessonStatus.NOT_STARTED && it.lessons.isNotEmpty() }
}
