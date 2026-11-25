package com.idiomiko.feature.playlists

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.playlists.PlaylistItem
import com.idiomiko.core.domain.playlists.PlaylistDraft
import com.idiomiko.core.domain.playlists.PlaylistLibrary
import com.idiomiko.core.domain.playlists.PlaylistPlayer
import com.idiomiko.core.domain.playlists.PlaylistService
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.domain.playlists.PlaylistTextKind
import com.idiomiko.core.domain.playlists.PlayerState
import com.idiomiko.core.domain.playlists.ListeningTime
import com.idiomiko.core.domain.service.BookStatsService
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Instant

enum class PlaylistsSort { RECENTLY_PLAYED, NAME, CREATED }

/** A playlist with the listening times shown for it. */
data class PlaylistSummary(val playlist: Playlist, val duration: Duration, val remaining: Duration)

/** The playlist chosen on the wide Playlists page, kept while the app runs so the page opens on it again. */
class PlaylistSelection {
    val id = MutableStateFlow<Long?>(null)
}

data class PlaylistsUiState(
    val loading: Boolean = true,
    val languageName: String = "",
    val playlists: List<PlaylistSummary> = emptyList(),
    val search: String = "",
    val sort: PlaylistsSort = PlaylistsSort.RECENTLY_PLAYED,
    /** The playlist the player has loaded, if it is one of these. */
    val playingId: Long? = null,
    val playing: Boolean = false,
    /** The playlist chosen on the wide page, when it is still there. */
    val chosenId: Long? = null,
) {
    /** The playlists matching the search, in the chosen order. */
    val shown: List<PlaylistSummary>
        get() {
            val query = search.trim()
            val list = playlists.filter { query.isEmpty() || it.playlist.name.contains(query, ignoreCase = true) || it.playlist.description.contains(query, ignoreCase = true) }
            return when (sort) {
                PlaylistsSort.RECENTLY_PLAYED -> list.sortedWith(compareByDescending<PlaylistSummary> { it.playlist.lastPlayedAt ?: Instant.DISTANT_PAST }.thenByDescending { it.playlist.createdAt })
                PlaylistsSort.NAME -> list.sortedBy { it.playlist.name.lowercase() }
                PlaylistsSort.CREATED -> list.sortedByDescending { it.playlist.createdAt }
            }
        }

    /** The playlist the wide page shows: the one chosen, else the one played last, else the first listed. */
    val selected: PlaylistSummary?
        get() = playlists.firstOrNull { it.playlist.id == chosenId }
            ?: playlists.filter { it.playlist.lastPlayedAt != null }.maxByOrNull { it.playlist.lastPlayedAt!! }
            ?: shown.firstOrNull()
            ?: playlists.firstOrNull()

    /** Whether the player has the playlist with [id] loaded and is playing it. */
    fun isPlaying(id: Long): Boolean = playing && playingId == id
}

/** The playlists of the language being learned. */
class PlaylistsViewModel(
    private val service: PlaylistService,
    private val player: PlaylistPlayer,
    settings: SettingsRepository,
    languages: com.idiomiko.core.domain.repository.LanguageRepository,
    time: ListeningTime,
    private val selection: PlaylistSelection,
) : ViewModel() {
    private data class Choices(val search: String = "", val sort: PlaylistsSort = PlaylistsSort.RECENTLY_PLAYED)

    private val choices = MutableStateFlow(Choices())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val playlists = combine(settings.settings.map { it.currentLanguageId to it.playlistSpeed }.distinctUntilChanged(), time.changes) { language, _ -> language.first }
        .flatMapLatest { id ->
            val name = languages.getById(id)?.name.orEmpty()
            val code = service.languageCode(id)
            service.playlistsWithMain(id).map { list -> name to list.map { PlaylistSummary(it, service.duration(it, code), service.remaining(it, code)) } }
        }

    val state: StateFlow<PlaylistsUiState> = combine(playlists, choices, player.state, selection.id) { (name, list), c, playing, chosen ->
        PlaylistsUiState(
            loading = false,
            languageName = name,
            playlists = list,
            search = c.search,
            sort = c.sort,
            playingId = playing.playlist?.id?.takeIf { playing.isLoaded },
            playing = playing.isPlaying,
            chosenId = chosen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    fun setSearch(query: String) = choices.update { it.copy(search = query) }
    fun setSort(sort: PlaylistsSort) = choices.update { it.copy(sort = sort) }

    /** Shows the playlist on the wide page, now and when the page opens again. */
    fun select(playlistId: Long) {
        selection.id.value = playlistId
    }

    /** Plays the playlist, or pauses it when it is the one playing. */
    fun play(playlistId: Long) {
        val s = player.state.value
        if (s.playlist?.id == playlistId && s.isPlaying) player.pause() else player.play(playlistId)
    }

    fun delete(playlistId: Long) {
        viewModelScope.launch { service.delete(playlistId) }
    }
}

data class PlaylistDetailUiState(
    val loading: Boolean = true,
    val playlist: Playlist? = null,
    val duration: Duration = Duration.ZERO,
    /** What is left to hear of it, the current text's heard pages left out. */
    val remaining: Duration = Duration.ZERO,
    val durations: Map<Long, Duration> = emptyMap(),
    /** The share of each text's words known, by item id, where it has been counted. */
    val known: Map<Long, Int> = emptyMap(),
    /** The title of the course each lesson comes from, by item id. */
    val courses: Map<Long, String> = emptyMap(),
    val player: PlayerState = PlayerState(),
    val error: String? = null,
) {
    /** Whether the player has this playlist loaded. */
    val isLoaded: Boolean get() = player.isLoaded && player.playlist?.id == playlist?.id

    /** Whether the player is playing this playlist now. */
    val isPlaying: Boolean get() = isLoaded && player.isPlaying

    /** The text listening is at: the player's while it has the playlist, else the place kept. */
    val currentId: Long? get() = if (isLoaded) player.itemId else playlist?.position?.itemId

    /** The text that comes after the current one, or the first not heard before listening starts. */
    val nextId: Long?
        get() {
            val items = playlist?.items ?: return null
            if (isLoaded) return player.order.getOrNull(player.queueIndex + 1)
            playlist.position?.let { return it.itemId }
            return items.firstOrNull { !it.finished }?.id
        }

    /** Where [item] stands: heard, being played, or next. */
    fun statusOf(item: PlaylistItem): ItemStatus = when {
        item.id == currentId && isLoaded ->
            if (player.isPlaying) ItemStatus.Playing(player.sentence + 1, player.sentences.size) else ItemStatus.Paused(player.sentence + 1, player.sentences.size)
        item.id == nextId && !item.finished -> ItemStatus.UpNext
        item.finished -> ItemStatus.Played
        else -> ItemStatus.None
    }
}

/** A text's place in the listening, as its row tells it. */
sealed interface ItemStatus {
    data object Played : ItemStatus
    data class Playing(val sentence: Int, val sentences: Int) : ItemStatus
    data class Paused(val sentence: Int, val sentences: Int) : ItemStatus
    data object UpNext : ItemStatus
    data object None : ItemStatus
}

sealed interface PlaylistDetailEvent {
    data object Deleted : PlaylistDetailEvent
}

/** One playlist with its texts, to play, reorder and change. */
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val service: PlaylistService,
    private val player: PlaylistPlayer,
    time: ListeningTime,
    settings: SettingsRepository,
    private val stats: BookStatsService,
) : ViewModel() {
    val events = UiEvents<PlaylistDetailEvent>()
    private val error = MutableStateFlow<String?>(null)
    private val playlist = service.observePlaylist(playlistId)

    /** The known share of each of the playlist's texts, counted once for each text it gets. */
    private val known: Flow<Map<Long, Int>> = playlist.map { it?.items.orEmpty().map { item -> item.bookId }.toSet() }.distinctUntilChanged()
        .map { books -> books.mapNotNull { book -> knownShare(book)?.let { book to it } }.toMap() }
        .onStart { emit(emptyMap()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val courses: Flow<Map<Long, String>> = playlist.map { it?.languageId }.distinctUntilChanged()
        .flatMapLatest { language -> if (language == null) flowOf(emptyMap()) else service.observeLessonCourses(language) }
        .onStart { emit(emptyMap()) }

    val state: StateFlow<PlaylistDetailUiState> = combine(
        playlist,
        player.state,
        combine(time.changes, settings.settings.map { it.playlistSpeed }.distinctUntilChanged()) { _, _ -> },
        combine(known, courses) { k, c -> k to c },
        error,
    ) { playlist, playing, _, (known, courses), error ->
        val code = playlist?.let { service.languageCode(it.languageId) }
        val items = playlist?.items.orEmpty()
        PlaylistDetailUiState(
            loading = false,
            playlist = playlist,
            duration = playlist?.let { service.duration(it, code) } ?: Duration.ZERO,
            remaining = playlist?.let { service.remaining(it, code) } ?: Duration.ZERO,
            durations = items.associate { it.id to service.duration(it.length, code) },
            known = items.mapNotNull { item -> known[item.bookId]?.let { item.id to it } }.toMap(),
            courses = items.mapNotNull { item -> courses[item.bookId]?.let { item.id to it } }.toMap(),
            player = playing,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailUiState())

    /** Plays from [itemId]'s start, or from where listening got to; [shuffle] given sets shuffling. */
    fun play(itemId: Long? = null, shuffle: Boolean? = null) = player.play(playlistId, itemId, shuffle)

    fun pause() = player.pause()

    fun move(itemId: Long, toIndex: Int) = change { service.move(playlistId, itemId, toIndex) }

    fun reorder(itemIds: List<Long>) = change { service.reorder(playlistId, itemIds) }

    fun remove(itemId: Long) = change { service.remove(itemId) }

    fun delete() = change {
        service.delete(playlistId)
        events.send(PlaylistDetailEvent.Deleted)
    }

    private suspend fun knownShare(bookId: Long): Int? = try {
        withContext(Dispatchers.Default) { stats.stats(bookId) }?.takeIf { it.distinctTerms > 0 }?.let { 100 - it.unknownPercent }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error.value = e.message?.let { tr(it) } ?: tr("Could not change the playlist")
            }
        }
    }
}

/** What a playlist form does: make one, change its details, or add texts to it. */
sealed interface PlaylistFormMode {
    /** A new playlist, starting with [source] when given, or else with the text [bookId] when given. */
    data class Create(val bookId: Long?, val source: PlaylistSource? = null) : PlaylistFormMode
    data class Edit(val playlistId: Long) : PlaylistFormMode
    data class AddTexts(val playlistId: Long) : PlaylistFormMode
}

/** Which texts the form's list shows. */
enum class TextFilter { ALL, BOOKS, COURSES, NEWS }

data class PlaylistFormUiState(
    val loading: Boolean = true,
    val mode: PlaylistFormMode = PlaylistFormMode.Create(null),
    val name: String = "",
    val description: String = "",
    val cover: Int = 0,
    val library: PlaylistLibrary = PlaylistLibrary(emptyList(), emptyList()),
    /** The texts already in the playlist, which adding leaves out. */
    val present: Set<Long> = emptySet(),
    val search: String = "",
    val filter: TextFilter = TextFilter.ALL,
    val selected: Set<PlaylistSource> = emptySet(),
    /** The courses opened to show their lessons. */
    val expanded: Set<String> = emptySet(),
    val languageCode: String? = null,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val showsDetails: Boolean get() = mode !is PlaylistFormMode.AddTexts
    /** A new playlist is made from its details alone; texts are chosen only when adding to one. */
    val showsTexts: Boolean get() = mode is PlaylistFormMode.AddTexts

    /** The books and news articles to list, by the search and the filter. */
    val texts: List<com.idiomiko.core.domain.playlists.PlaylistText>
        get() = if (filter == TextFilter.COURSES) emptyList() else library.texts.filter { text ->
            (filter == TextFilter.ALL || (filter == TextFilter.BOOKS) == (text.kind == PlaylistTextKind.BOOK)) &&
                text.bookId !in present && matches(text.title)
        }

    /** The courses to list: those whose title or a lesson's matches the search. */
    val courses: List<Course>
        get() = if (filter != TextFilter.ALL && filter != TextFilter.COURSES) emptyList() else library.courses.filter { course ->
            matches(course.title) || course.lessons.any { matches(it.title) }
        }

    /** The lessons of [course] to list: all of them, or those matching the search when the course's title does not. */
    fun lessonsOf(course: Course) = if (search.isBlank() || matches(course.title)) course.lessons else course.lessons.filter { matches(it.title) }

    /** Whether [course]'s lessons show: opened by hand, or found by the search. */
    fun showsLessons(course: Course) = course.id in expanded || (search.isNotBlank() && !matches(course.title))

    fun isSelected(course: Course) = PlaylistSource.WholeCourse(course.id) in selected

    private fun matches(title: String) = search.isBlank() || title.contains(search.trim(), ignoreCase = true)

    /** How long the chosen texts are, in characters. */
    val selectedLength: Int
        get() = selected.sumOf { source ->
            when (source) {
                is PlaylistSource.Text -> library.texts.firstOrNull { it.bookId == source.bookId }?.length ?: 0
                is PlaylistSource.WholeCourse -> library.courses.firstOrNull { it.id == source.courseId }?.lessons?.sumOf { it.text.length } ?: 0
                is PlaylistSource.Lesson -> library.courses.firstOrNull { it.id == source.courseId }?.lessons?.firstOrNull { it.id == source.lessonId }?.text?.length ?: 0
            }
        }

    /** How many texts the chosen ones make: a whole course counts its lessons. */
    val selectedCount: Int
        get() = selected.sumOf { source ->
            if (source is PlaylistSource.WholeCourse) library.courses.firstOrNull { it.id == source.courseId }?.lessons?.size ?: 0 else 1
        }

    val canSave: Boolean get() = !saving && (!showsDetails || name.isNotBlank()) && (mode !is PlaylistFormMode.AddTexts || selected.isNotEmpty())
}

sealed interface PlaylistFormEvent {
    data class Saved(val playlistId: Long) : PlaylistFormEvent
}

/** Making a playlist, changing its details, or choosing texts to add to it. */
class PlaylistFormViewModel(
    private val mode: PlaylistFormMode,
    private val service: PlaylistService,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistFormUiState(mode = mode))
    val state: StateFlow<PlaylistFormUiState> = _state.asStateFlow()
    val events = UiEvents<PlaylistFormEvent>()

    init {
        viewModelScope.launch {
            val existing = when (mode) {
                is PlaylistFormMode.Edit -> service.playlist(mode.playlistId)
                is PlaylistFormMode.AddTexts -> service.playlist(mode.playlistId)
                is PlaylistFormMode.Create -> null
            }
            val languageId = existing?.languageId ?: settings.current.currentLanguageId
            val library = if (mode is PlaylistFormMode.AddTexts) service.library(languageId) else PlaylistLibrary(emptyList(), emptyList())
            val start = (mode as? PlaylistFormMode.Create)?.let { create -> create.source ?: create.bookId?.let { service.sourceFor(it) } }
            _state.update {
                it.copy(
                    loading = false,
                    name = existing?.name.orEmpty(),
                    description = existing?.description.orEmpty(),
                    cover = existing?.cover ?: 0,
                    library = library,
                    present = existing?.items.orEmpty().map { item -> item.bookId }.toSet(),
                    selected = setOfNotNull(start),
                    // A lesson is chosen within its course, which opens to show it.
                    expanded = setOfNotNull((start as? PlaylistSource.Lesson)?.courseId),
                    languageCode = service.languageCode(languageId),
                )
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name.take(PlaylistService.NAME_MAX_LENGTH), error = null) }
    fun setDescription(text: String) = _state.update { it.copy(description = text) }
    fun setCover(cover: Int) = _state.update { it.copy(cover = cover) }
    fun setSearch(query: String) = _state.update { it.copy(search = query) }
    fun setFilter(filter: TextFilter) = _state.update { it.copy(filter = filter) }

    fun toggle(source: PlaylistSource) = _state.update { s ->
        val selected = if (source in s.selected) s.selected - source else when (source) {
            // A whole course stands for its lessons, which are not chosen besides.
            is PlaylistSource.WholeCourse -> s.selected.filterNot { it is PlaylistSource.Lesson && it.courseId == source.courseId }.toSet() + source
            is PlaylistSource.Lesson -> (s.selected - PlaylistSource.WholeCourse(source.courseId)) + source
            is PlaylistSource.Text -> s.selected + source
        }
        s.copy(selected = selected)
    }

    fun toggleExpanded(courseId: String) = _state.update { s -> s.copy(expanded = if (courseId in s.expanded) s.expanded - courseId else s.expanded + courseId) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                // The chosen texts go in in the order the list shows them: books and articles, then the courses.
                val sources = s.selected.sortedBy { source -> if (source is PlaylistSource.Text) 0 else 1 }
                val id = when (mode) {
                    is PlaylistFormMode.Create -> service.create(settings.current.currentLanguageId, PlaylistDraft(s.name, s.description, s.cover), sources)
                    is PlaylistFormMode.Edit -> mode.playlistId.also { service.update(it, PlaylistDraft(s.name, s.description, s.cover)) }
                    is PlaylistFormMode.AddTexts -> mode.playlistId.also { service.add(it, sources) }
                }
                events.send(PlaylistFormEvent.Saved(id))
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message?.let { m -> tr(m) } ?: tr("Could not save the playlist")) }
            }
        }
    }

    /** About how long [characters] of the playlist's language take to hear. */
    fun duration(characters: Int): Duration = service.duration(characters, _state.value.languageCode)
}

/** A playlist menu: the language's playlists, and which hold what the menu is for. */
data class AddToPlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    /** The texts the menu's text, lesson or course stands for; null for a lesson not made a text yet. */
    val texts: List<Long?> = emptyList(),
) {
    /** Whether [playlist] holds all of it: the text, the lesson, or every lesson of the course. */
    fun contains(playlist: Playlist) = texts.isNotEmpty() && texts.all { text -> text != null && playlist.items.any { it.bookId == text } }
}

/** The playlist menus of the reader and the course pages: [source] goes into the language's playlists, or comes out. */
class AddToPlaylistViewModel(private val source: PlaylistSource, private val service: PlaylistService, settings: SettingsRepository) : ViewModel() {
    /** The name of each playlist [source] has just been put in. */
    val added = UiEvents<String>()
    private val texts = MutableStateFlow<List<Long?>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AddToPlaylistUiState> =
        settings.settings.map { it.currentLanguageId }.distinctUntilChanged().flatMapLatest { service.playlistsWithMain(it) }
            .combine(texts) { playlists, texts -> AddToPlaylistUiState(playlists, texts) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddToPlaylistUiState())

    init {
        viewModelScope.launch { texts.value = service.textsOf(source) }
    }

    /** Puts [source] into the playlist, or takes it out when the playlist holds all of it. */
    fun toggle(playlistId: Long) {
        viewModelScope.launch {
            val playlist = service.observePlaylist(playlistId).first() ?: return@launch
            val adding = !AddToPlaylistUiState(texts = service.textsOf(source)).contains(playlist)
            service.setContains(playlistId, source, adding)
            // Adding a lesson makes its text, which the menu then knows it by.
            texts.value = service.textsOf(source)
            if (adding) added.send(playlist.name)
        }
    }
}

/** The language's playlists, its main one made first where it has none yet, so there is always one to add to. */
internal fun PlaylistService.playlistsWithMain(languageId: Long): Flow<List<Playlist>> = flow {
    if (languageId > 0) ensureMain(languageId, tr("Main"))
    emitAll(observePlaylists(languageId))
}
