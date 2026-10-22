package com.tayra.languages.core.domain.playlists

import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration

/** Something to add to a playlist: a text of the library, one lesson of a course, or a whole course. */
sealed interface PlaylistSource {
    data class Text(val bookId: Long) : PlaylistSource
    data class Lesson(val courseId: String, val lessonId: String) : PlaylistSource

    /** Each of the course's lessons, in the course's order, as an item of its own. */
    data class WholeCourse(val courseId: String) : PlaylistSource
}

/** What the language's library offers a playlist: the books and news articles read, and the courses. */
data class PlaylistLibrary(val texts: List<PlaylistText>, val courses: List<Course>)

/**
 * Playlists: texts of a language to listen to one after another. A playlist holds books of the
 * library, so a course lesson added is made a text the way opening it does (without opening it), and
 * an item goes when its book is deleted. Changing a playlist never touches the texts' reading progress.
 */
class PlaylistService(
    private val repository: PlaylistRepository,
    private val courses: CourseService,
    private val languages: LanguageRepository,
    private val time: ListeningTime,
    private val clock: Clock = Clock.System,
) {
    fun observePlaylists(languageId: Long): Flow<List<Playlist>> = repository.observePlaylists(languageId)

    fun observePlaylist(id: Long): Flow<Playlist?> = repository.observePlaylist(id)

    suspend fun playlist(id: Long): Playlist? = repository.playlist(id)

    /** Makes a playlist for the language with [languageId], holding [sources] in order; returns its id. */
    suspend fun create(languageId: Long, draft: PlaylistDraft, sources: List<PlaylistSource> = emptyList()): Long {
        val id = repository.create(languageId, validated(draft), clock.now())
        add(id, sources)
        return id
    }

    suspend fun update(id: Long, draft: PlaylistDraft) = repository.update(stored(id).id, validated(draft))

    /** Deletes the playlist; its texts stay in the library. */
    suspend fun delete(id: Long) = repository.delete(id)

    /** Adds [sources] after the playlist's last item; a text already in it stays where it is. */
    suspend fun add(playlistId: Long, sources: List<PlaylistSource>) {
        if (sources.isEmpty()) return
        stored(playlistId)
        val bookIds = sources.flatMap { source ->
            when (source) {
                is PlaylistSource.Text -> listOf(source.bookId)
                is PlaylistSource.Lesson -> listOfNotNull(courses.openLesson(source.courseId, source.lessonId))
                is PlaylistSource.WholeCourse -> courses.observeCourse(source.courseId).first()?.course?.lessons.orEmpty()
                    .mapNotNull { courses.openLesson(source.courseId, it.id) }
            }
        }
        repository.addBooks(playlistId, bookIds)
    }

    /** Puts the text with [bookId] into the playlist, or takes it out, as the reader's playlist menu does. */
    suspend fun setContains(playlistId: Long, bookId: Long, contains: Boolean) {
        if (contains) add(playlistId, listOf(PlaylistSource.Text(bookId))) else repository.removeBook(playlistId, bookId)
    }

    suspend fun remove(itemId: Long) = repository.removeItem(itemId)

    /** Moves the item to [toIndex] among the playlist's items. */
    suspend fun move(playlistId: Long, itemId: Long, toIndex: Int) {
        val ids = stored(playlistId).items.map { it.id }.toMutableList()
        if (!ids.remove(itemId)) return
        ids.add(toIndex.coerceIn(0, ids.size), itemId)
        repository.reorder(playlistId, ids)
    }

    suspend fun reorder(playlistId: Long, itemIds: List<Long>) = repository.reorder(playlistId, itemIds)

    /** What the library of the language with [languageId] has to add. */
    suspend fun library(languageId: Long): PlaylistLibrary =
        PlaylistLibrary(repository.texts(languageId), courses.observeCourses(languageId).first().map { it.course }.filter { it.lessons.isNotEmpty() })

    /** The ISO 639-1 code of the language with [languageId], which listening times are estimated in. */
    suspend fun languageCode(languageId: Long): String? = languages.getById(languageId)?.name?.let { LanguageCodes.codeFor(it) }

    /** About how long [characters] of the language take to hear. */
    fun duration(characters: Int, languageCode: String?): Duration = time.duration(characters, languageCode)

    /** About how long the whole playlist takes to hear. */
    fun duration(playlist: Playlist, languageCode: String?): Duration = time.duration(playlist.items.sumOf { it.length }, languageCode)

    /**
     * About how long is left of the playlist: the texts not heard to the end, less the pages of the
     * current one already heard.
     */
    fun remaining(playlist: Playlist, languageCode: String?): Duration {
        val position = playlist.position
        val left = playlist.items.filter { !it.finished }.sumOf { item ->
            if (position != null && item.id == position.itemId && item.pageCount > 0) item.length * (item.pageCount - position.page + 1) / item.pageCount
            else item.length
        }
        return time.duration(left, languageCode)
    }

    private suspend fun stored(id: Long): Playlist = repository.playlist(id) ?: throw PlaylistValidationException("This playlist no longer exists")

    private fun validated(draft: PlaylistDraft): PlaylistDraft {
        val name = draft.name.trim()
        if (name.isEmpty()) throw PlaylistValidationException("A playlist needs a name")
        if (name.length > NAME_MAX_LENGTH) throw PlaylistValidationException("A playlist's name is at most $NAME_MAX_LENGTH characters")
        return draft.copy(name = name, description = draft.description.trim())
    }

    companion object {
        const val NAME_MAX_LENGTH = 60
    }
}
