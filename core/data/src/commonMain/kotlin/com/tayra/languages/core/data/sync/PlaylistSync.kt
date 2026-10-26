package com.tayra.languages.core.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Playlists travel whole, keyed by their sync id, with their texts named by the texts' keys. A text
// another device has and this one does not stays in the playlist's record: it is kept aside here and
// sent back with the playlist, so changing the playlist here does not drop it everywhere.

internal data class PlaylistRow(
    val id: Long, val language: String, val syncId: String?, val name: String, val description: String, val cover: Long,
    val createdAt: Long, val lastPlayedAt: Long?, val currentItemId: Long?, val currentPage: Long, val currentSentence: Long, val updatedAt: Long,
)

internal data class PlaylistItemRow(
    val id: Long, val position: Long, val finishedAt: Long?, val syncId: String?, val sourceUri: String?, val lessonId: String?,
    val title: String, val language: String, val bookId: Long,
) {
    val key: String? get() = bookKey(lessonId, sourceUri, syncId, title, language)
}

/** The playlist's record, with the texts it holds that only other devices have, as last taken from the account. */
internal suspend fun playlistRecord(db: TayraDatabase, row: PlaylistRow, code: String, device: String): SyncRecord {
    val q = db.syncBooksQueries
    val syncId = row.syncId.orEmpty()
    val items = q.playlistItems(row.id, ::PlaylistItemRow).awaitAsList()
    val here = items.mapNotNull { item -> item.key?.let { key -> Triple(key, item.position, item.finishedAt) } }
    val elsewhere = q.pendingRecord(SyncKinds.PLAYLIST, syncId).awaitAsOneOrNull()
        ?.let { runCatching { SyncJson.decodeFromString(SyncRecord.serializer(), it) }.getOrNull() }
        ?.data?.objects("items").orEmpty()
        .mapNotNull { item -> item.string("book")?.takeIf { key -> here.none { it.first == key } }?.let { Triple(it, item.long("position") ?: 0, item.long("finishedAt")) } }
    return SyncRecord(SyncKinds.PLAYLIST, syncId, row.updatedAt, data = buildJsonObject {
        put("language", code)
        put("name", row.name)
        put("description", row.description)
        put("cover", row.cover)
        put("createdAt", row.createdAt)
        row.lastPlayedAt?.let { put("lastPlayedAt", it) }
        items.firstOrNull { it.id == row.currentItemId }?.key?.let { put("currentItem", it) }
        put("currentPage", row.currentPage)
        put("currentSentence", row.currentSentence)
        put("items", buildJsonArray {
            (here + elsewhere).sortedWith(compareBy({ it.second }, { it.first })).forEach { (key, position, finishedAt) ->
                add(buildJsonObject {
                    put("book", key)
                    put("position", position)
                    finishedAt?.let { put("finishedAt", it) }
                })
            }
        })
    }, deviceId = device)
}

/** Applies pulled playlist records, merged with this device's version. */
internal class PlaylistWriter(private val db: TayraDatabase, private val device: String, private val languageId: (String) -> Long?) {
    private val q = db.syncBooksQueries

    /** Whether the playlist here changed, and the record to keep aside while it holds texts this device lacks. */
    class Outcome(val changed: Boolean, val waiting: SyncRecord?)

    suspend fun apply(remote: SyncRecord): Outcome {
        val outcome = write(remote)
        val kept = outcome.second ?: return Outcome(outcome.first, null)
        val lacking = kept.data?.objects("items").orEmpty().any { item -> item.string("book")?.let { findBook(q, it, languageId) } == null }
        return Outcome(outcome.first, kept.takeIf { lacking })
    }

    /** Whether the playlist holds texts that have come to this device since it was applied. */
    private suspend fun gainedTexts(row: PlaylistRow?, merged: SyncRecord): Boolean {
        if (row == null) return false
        val here = q.playlistItems(row.id, ::PlaylistItemRow).awaitAsList().mapNotNull { it.key }.toSet()
        return merged.data?.objects("items").orEmpty().any { item -> item.string("book")?.let { it !in here && findBook(q, it, languageId) != null } == true }
    }

    /** Writes the merge of [remote] and this device's playlist; whether it changed it, and the merged record unless deleted. */
    private suspend fun write(remote: SyncRecord): Pair<Boolean, SyncRecord?> {
        val row = q.playlistOfSyncId(remote.key, ::PlaylistRow).awaitAsOneOrNull()
        val code = remote.data?.string("language")
        val local = if (row != null) playlistRecord(db, row, code ?: "", device)
        else db.syncRecordsQueries.tombstone("playlist", "", remote.key).awaitAsOneOrNull()?.let { SyncRecord(remote.kind, remote.key, it, deleted = true, deviceId = device) }
        val merged = if (local == null) remote else SyncMerge.merge(local, remote)
        if (merged.deleted) {
            if (row == null || local?.deleted == true) return false to null
            q.deletePlaylist(row.id)
            return true to null
        }
        if (local != null && local.updatedAt == merged.updatedAt && local.data == merged.data && !gainedTexts(row, merged)) return false to merged
        val d = merged.data ?: return false to null
        val id = row?.id ?: run {
            val language = languageId(d.string("language") ?: return false to null) ?: return false to null
            q.insertPlaylist(language, d.string("name") ?: return false to null, d.string("description").orEmpty(), d.long("cover") ?: 0,
                d.long("createdAt") ?: merged.updatedAt, d.long("lastPlayedAt"), d.long("currentPage") ?: 1, d.long("currentSentence") ?: 0, remote.key, merged.updatedAt)
            q.playlistOfSyncId(remote.key, ::PlaylistRow).awaitAsOneOrNull()?.id ?: return false to null
        }
        q.updatePlaylist(d.string("name") ?: "", d.string("description").orEmpty(), d.long("cover") ?: 0, d.long("lastPlayedAt"),
            d.long("currentPage") ?: 1, d.long("currentSentence") ?: 0, merged.updatedAt, id)
        q.clearItems(id)
        val books = mutableMapOf<String, Long>()
        for (item in d.objects("items")) {
            val key = item.string("book") ?: continue
            val book = findBook(q, key, languageId) ?: continue
            books[key] = book.id
            q.insertItem(id, book.id, item.long("position") ?: 0, item.long("finishedAt"))
        }
        val current = d.string("currentItem")?.let { books[it] }
        if (current != null) q.setCurrentItem(id, current) else q.clearCurrentItem(id)
        return true to merged
    }
}
