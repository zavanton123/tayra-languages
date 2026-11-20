package com.idiomiko.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.flashcards.FlashcardService
import com.idiomiko.core.domain.frequency.VocabularyProgress
import com.idiomiko.core.domain.frequency.WordFrequencyService
import com.idiomiko.core.domain.home.LevelSuggestions
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.playlists.PlaylistPlayer
import com.idiomiko.core.domain.playlists.PlaylistService
import com.idiomiko.core.domain.repository.BookRepository
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.service.StatsService
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.domain.stats.DailyGoal
import com.idiomiko.core.domain.stats.SavedWord
import com.idiomiko.core.domain.stats.StudyActivity
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    /** The course finished last, while no other is under way; null otherwise. */
    val finishedCourse: CourseProgress? = null,
    /** The book being read most recently; null when no book is being read. */
    val book: BookListItem? = null,
    val flashcardsDue: Int = 0,
    /** The playlist being listened to: started, not heard to the end, the one played last; null when there is none. */
    val playlist: Playlist? = null,
    /** Why a lesson could not be opened, if it could not. */
    val error: String? = null,
    /** Minutes of study a day the reader aims for. */
    val dailyGoal: Int = UserSettings.DEFAULT_DAILY_GOAL,
    /** The words known against the frequency list; null until counted, or where nothing counts them. */
    val vocabulary: VocabularyProgress? = null,
    /** The words saved last, the latest first. */
    val recentWords: List<SavedWord> = emptyList(),
    /** The course whose words go just past those the reader knows, not started yet. */
    val suggestedCourse: CourseProgress? = null,
    /** Books not opened yet that the reader can mostly read already. */
    val suggestedBooks: List<BookListItem> = emptyList(),
    /** Whether this platform can make a text from a web page's address. */
    val readsWebPages: Boolean = false,
) {
    /** The one step offered first. */
    val nextUp: NextUp get() = NextUp.choose(flashcardsDue, course, book?.takeIf { !it.isCompleted }, suggestedCourse, activity?.averageAnswer)

    val goal: DailyGoal get() = DailyGoal(dailyGoal, activity?.minutesToday ?: 0)

    /** Nothing studied yet and nothing under way: a first visit. */
    val firstVisit: Boolean
        get() = activity?.days.isNullOrEmpty() && course?.status != LessonStatus.IN_PROGRESS && finishedCourse == null && book == null && flashcardsDue == 0
}

sealed interface HomeEvent {
    /** The text with [bookId] is ready to read. */
    data class Read(val bookId: Long) : HomeEvent
}

/** Today's progress in the language being learned, the next step, and the ways back into its courses, books, cards and playlists. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val courses: CourseService,
    books: BookRepository,
    flashcards: FlashcardService,
    private val stats: StatsService,
    languages: LanguageRepository,
    private val settings: SettingsRepository,
    /** The playlists, for the one under way; null leaves it out, as in tests. */
    playlists: PlaylistService? = null,
    private val player: PlaylistPlayer? = null,
    /** Counts the words known against the frequency list; null leaves the vocabulary out. */
    frequency: WordFrequencyService? = null,
    readsWebPages: Boolean = false,
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
            val base = combine(courses.observeCourses(id), books.observeBooks(archived = false), flashcards.observeDueCount(), refreshes, error) { courseList, bookList, due, _, problem ->
                Loaded(
                    HomeUiState(
                        loading = false,
                        languageName = language?.name.orEmpty(),
                        course = courseToContinue(courseList),
                        finishedCourse = courseFinished(courseList),
                        book = bookList.filter { it.languageId == id && it.lastOpened != null && !it.isCompleted }.maxByOrNull { it.lastOpened ?: Instant.DISTANT_PAST },
                        flashcardsDue = due,
                        error = problem,
                        suggestedBooks = LevelSuggestions.books(bookList, id),
                        readsWebPages = readsWebPages,
                    ),
                    courseList,
                )
            }
            val vocabulary = frequency?.observeVocabulary(id)?.map<VocabularyProgress, VocabularyProgress?> { it } ?: flowOf(null)
            val goal = settings.settings.map { it.dailyGoalMinutes }.distinctUntilChanged()
            combine(base, playlists?.observePlaylists(id) ?: flowOf(emptyList()), vocabulary, goal) { loaded, list, words, minutes ->
                loaded.state.copy(
                    playlist = list.filter { it.position != null && !it.isComplete }.maxByOrNull { it.lastPlayedAt ?: Instant.DISTANT_PAST },
                    vocabulary = words,
                    suggestedCourse = words?.let { LevelSuggestions.course(loaded.courses, it.known) },
                    dailyGoal = minutes,
                )
            }.mapLatest { state ->
                state.copy(
                    activity = language?.let { stats.studyActivity(it.id, it.name) },
                    recentWords = language?.let { stats.recentlySaved(it.id, RECENT_WORDS) }.orEmpty(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** The page's state with every course, which the suggestion needs once the words known are counted. */
    private class Loaded(val state: HomeUiState, val courses: List<CourseProgress>)

    fun refresh() = refreshes.update { it + 1 }

    private val _savedToday = MutableStateFlow<List<SavedWord>?>(null)

    /** The words saved today, while they are shown; null otherwise. */
    val savedToday: StateFlow<List<SavedWord>?> = _savedToday

    fun showSavedToday() {
        viewModelScope.launch { _savedToday.value = stats.savedToday(settings.current.currentLanguageId) }
    }

    fun closeSavedToday() {
        _savedToday.value = null
    }

    /** Goes on with the playlist under way, from where listening stopped. */
    fun continuePlaylist() {
        val id = state.value.playlist?.id ?: return
        player?.play(id)
    }

    /** Opens [lesson] of [course] to read. */
    fun openLesson(course: CourseProgress, lesson: LessonProgress) {
        error.value = null
        viewModelScope.launch {
            val bookId = try {
                courses.openLesson(course.course.id, lesson.lesson.id)
            } catch (e: Exception) {
                error.value = e.message?.let { tr(it) } ?: tr("Could not open the lesson")
                return@launch
            }
            if (bookId == null) error.value = tr("This course's language is not set up in the app.") else events.send(HomeEvent.Read(bookId))
        }
    }

    private fun courseToContinue(list: List<CourseProgress>): CourseProgress? =
        list.filter { it.status == LessonStatus.IN_PROGRESS }.maxByOrNull { lastRead(it) }
            ?: list.firstOrNull { it.status == LessonStatus.NOT_STARTED && it.lessons.isNotEmpty() }

    private fun courseFinished(list: List<CourseProgress>): CourseProgress? =
        if (list.any { it.status == LessonStatus.IN_PROGRESS }) null
        else list.filter { it.status == LessonStatus.COMPLETED }.maxByOrNull { lastRead(it) }

    private fun lastRead(progress: CourseProgress): Instant = progress.lessons.mapNotNull { it.book?.lastOpened }.maxOrNull() ?: Instant.DISTANT_PAST

    private companion object {
        const val RECENT_WORDS = 8
    }
}
