package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.Playlists
import com.tayra.languages.core.data.db.SelectItems
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.playlists.ListeningPosition
import com.tayra.languages.core.domain.playlists.Playlist
import com.tayra.languages.core.domain.playlists.PlaylistDraft
import com.tayra.languages.core.domain.playlists.PlaylistItem
import com.tayra.languages.core.domain.playlists.PlaylistRepository
import com.tayra.languages.core.domain.playlists.PlaylistText
import com.tayra.languages.core.domain.playlists.PlaylistTextKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class PlaylistRepositoryImpl(private val provider: DatabaseProvider) : PlaylistRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    // Items change apart from playlists, and so do the texts they show (titles, pages); both are watched.
    override fun observePlaylists(languageId: Long): Flow<List<Playlist>> = flow {
        val database = db()
        val playlists = database.playlistsQueries.selectPlaylists(languageId).asFlow().mapToList(databaseDispatcher)
        emitAll(combine(playlists, database.itemChanges()) { rows, _ -> withContext(databaseDispatcher) { hydrate(database, rows) } })
    }

    override fun observePlaylist(id: Long): Flow<Playlist?> = flow {
        val database = db()
        val playlists = database.playlistsQueries.selectPlaylist(id).asFlow().mapToList(databaseDispatcher)
        emitAll(combine(playlists, database.itemChanges()) { rows, _ -> withContext(databaseDispatcher) { hydrate(database, rows).firstOrNull() } })
    }

    override suspend fun playlist(id: Long): Playlist? = withContext(databaseDispatcher) {
        val database = db()
        hydrate(database, database.playlistsQueries.selectPlaylist(id).awaitAsList()).firstOrNull()
    }

    /** Emits whenever an item or a text it shows changes, in any playlist: a query is told of every change to its tables. */
    private fun TayraDatabase.itemChanges(): Flow<Unit> = playlistsQueries.selectItems(listOf(-1L)).asFlow().map { }

    private suspend fun hydrate(database: TayraDatabase, rows: List<Playlists>): List<Playlist> {
        if (rows.isEmpty()) return emptyList()
        val items = database.playlistsQueries.selectItems(rows.map { it.id }).awaitAsList().groupBy { it.playlist_id }
        return rows.map { row ->
            val own = items[row.id].orEmpty().map { it.toItem() }
            Playlist(
                id = row.id,
                languageId = row.language_id,
                name = row.name,
                description = row.description,
                cover = row.cover.toInt(),
                createdAt = row.created_at.toInstant(),
                lastPlayedAt = row.last_played_at?.toInstant(),
                items = own,
                // A place in an item since removed is no place.
                position = row.current_item_id?.takeIf { id -> own.any { it.id == id } }
                    ?.let { ListeningPosition(it, row.current_page.toInt(), row.current_sentence.toInt()) },
            )
        }
    }

    private fun SelectItems.toItem() = PlaylistItem(
        id = id,
        bookId = book_id,
        title = title,
        kind = when {
            is_lesson -> PlaylistTextKind.LESSON
            is_news -> PlaylistTextKind.NEWS
            else -> PlaylistTextKind.BOOK
        },
        pageCount = page_count.toInt(),
        length = char_count.toInt(),
        finished = finished_at != null,
    )

    override suspend fun create(languageId: Long, draft: PlaylistDraft, createdAt: Instant): Long = withContext(databaseDispatcher) {
        val q = db().playlistsQueries
        q.transactionWithResult {
            q.insertPlaylist(languageId, draft.name, draft.description, draft.cover.toLong(), createdAt.toEpochMillis())
            q.lastInsertId().awaitAsOne()
        }
    }

    override suspend fun update(id: Long, draft: PlaylistDraft) {
        withContext(databaseDispatcher) { db().playlistsQueries.updatePlaylist(draft.name, draft.description, draft.cover.toLong(), id) }
    }

    override suspend fun delete(id: Long) {
        withContext(databaseDispatcher) { db().playlistsQueries.deletePlaylist(id) }
    }

    override suspend fun addBooks(playlistId: Long, bookIds: List<Long>) {
        if (bookIds.isEmpty()) return
        withContext(databaseDispatcher) {
            val q = db().playlistsQueries
            q.transaction {
                var position = q.nextPosition(playlistId).awaitAsOne() ?: 0L
                for (bookId in bookIds.distinct()) {
                    q.insertItem(playlistId, bookId, position)
                    position++
                }
            }
        }
    }

    override suspend fun removeItem(itemId: Long) {
        withContext(databaseDispatcher) { db().playlistsQueries.deleteItem(itemId) }
    }

    override suspend fun removeBook(playlistId: Long, bookId: Long) {
        withContext(databaseDispatcher) { db().playlistsQueries.deleteBookFromPlaylist(playlistId, bookId) }
    }

    override suspend fun reorder(playlistId: Long, itemIds: List<Long>) {
        withContext(databaseDispatcher) {
            val database = db()
            val q = database.playlistsQueries
            q.transaction {
                val current = q.selectItems(listOf(playlistId)).awaitAsList().map { it.id }
                val named = itemIds.filter { it in current }.distinct()
                (named + current.filter { it !in named }).forEachIndexed { i, id -> q.setItemPosition(i.toLong(), id) }
            }
        }
    }

    override suspend fun savePosition(playlistId: Long, position: ListeningPosition?, playedAt: Instant) {
        withContext(databaseDispatcher) {
            db().playlistsQueries.savePosition(
                itemId = position?.itemId,
                page = (position?.page ?: 1).toLong(),
                sentence = (position?.sentence ?: 0).toLong(),
                playedAt = playedAt.toEpochMillis(),
                id = playlistId,
            )
        }
    }

    override suspend fun setFinished(itemId: Long, finishedAt: Instant?) {
        withContext(databaseDispatcher) { db().playlistsQueries.setItemFinished(finishedAt?.toEpochMillis(), itemId) }
    }

    override suspend fun clearFinished(playlistId: Long) {
        withContext(databaseDispatcher) { db().playlistsQueries.clearFinished(playlistId) }
    }

    override suspend fun texts(languageId: Long): List<PlaylistText> = withContext(databaseDispatcher) {
        db().playlistsQueries.selectTexts(languageId).awaitAsList().map {
            PlaylistText(it.id, it.title, if (it.is_news) PlaylistTextKind.NEWS else PlaylistTextKind.BOOK, it.char_count.toInt(), it.last_opened?.toInstant())
        }
    }
}
