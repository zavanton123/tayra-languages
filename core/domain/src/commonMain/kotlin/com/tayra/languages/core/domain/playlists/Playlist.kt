package com.tayra.languages.core.domain.playlists

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** What a text in a playlist is, for its icon and label: one of the reader's books, a course lesson, or a news article. */
enum class PlaylistTextKind { BOOK, LESSON, NEWS }

/** A text in a playlist. Every kind is a book of the library; a whole book is heard page after page. */
data class PlaylistItem(
    val id: Long,
    val bookId: Long,
    val title: String,
    val kind: PlaylistTextKind,
    val pageCount: Int,
    /** The length of its text, in characters, which its listening time is estimated from. */
    val length: Int,
    /** Whether it was listened to the end since the playlist last started over. */
    val finished: Boolean,
)

/** Where listening got to in a playlist: the item, its page (from 1) and the spoken sentence on the page (from 0). */
data class ListeningPosition(val itemId: Long, val page: Int, val sentence: Int)

/** Texts to listen to one after another, in a language. */
data class Playlist(
    val id: Long,
    val languageId: Long,
    val name: String,
    val description: String,
    /** The cover's colour, by its place among the app's cover colours. */
    val cover: Int,
    val createdAt: Instant,
    val lastPlayedAt: Instant?,
    val items: List<PlaylistItem>,
    /** Null before listening starts, and once it went through to the end. */
    val position: ListeningPosition?,
) {
    val finishedCount: Int get() = items.count { it.finished }

    /** The share of the texts listened to the end, from 0 to 1. */
    val progress: Float get() = if (items.isEmpty()) 0f else finishedCount.toFloat() / items.size

    /** The item listening goes on with, null when it has not started. */
    val currentItem: PlaylistItem? get() = position?.let { p -> items.firstOrNull { it.id == p.itemId } }

    /** Whether every text was listened to the end. */
    val isComplete: Boolean get() = items.isNotEmpty() && items.all { it.finished }
}

/** What the reader writes about a playlist. */
data class PlaylistDraft(val name: String, val description: String = "", val cover: Int = 0)

class PlaylistValidationException(message: String) : IllegalArgumentException(message)

/** A text of the library that can be added to a playlist: a book or a news article opened before. */
data class PlaylistText(val bookId: Long, val title: String, val kind: PlaylistTextKind, val length: Int, val lastOpened: Instant?)

interface PlaylistRepository {
    /** The language's playlists, oldest first, with their items in order. */
    fun observePlaylists(languageId: Long): Flow<List<Playlist>>
    fun observePlaylist(id: Long): Flow<Playlist?>
    suspend fun playlist(id: Long): Playlist?
    suspend fun create(languageId: Long, draft: PlaylistDraft, createdAt: Instant): Long
    suspend fun update(id: Long, draft: PlaylistDraft)
    suspend fun delete(id: Long)

    /** Adds the books after the playlist's last item, in order, leaving out those already in it. */
    suspend fun addBooks(playlistId: Long, bookIds: List<Long>)
    suspend fun removeItem(itemId: Long)
    suspend fun removeBook(playlistId: Long, bookId: Long)

    /** Puts the playlist's items in the order of [itemIds]; items not named keep their places after them. */
    suspend fun reorder(playlistId: Long, itemIds: List<Long>)

    /** Keeps where listening got to ([position] null when it went through to the end) and when. */
    suspend fun savePosition(playlistId: Long, position: ListeningPosition?, playedAt: Instant)
    suspend fun setFinished(itemId: Long, finishedAt: Instant?)

    /** Forgets which items were listened to the end, so the playlist starts over. */
    suspend fun clearFinished(playlistId: Long)

    /** The language's books and opened news articles, the most recently read first. */
    suspend fun texts(languageId: Long): List<PlaylistText>
}
