package com.tayra.languages.core.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.tayra.languages.core.data.db.ChangedVideos
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.Video_cues
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

// Videos: a video goes with its transcript's text (a BOOK and its PAGEs) as one VIDEO record of the
// same key, with its address and its timed lines; being on the internet, it plays the same anywhere.

/** This device's video as a record, or null for one of a text no other device can know. */
internal suspend fun videoRecord(db: TayraDatabase, row: ChangedVideos, device: String): SyncRecord? {
    val key = bookKey(null, row.source_uri, row.sync_id, row.title, row.language) ?: return null
    val cues = db.videosQueries.cues(row.book_id).awaitAsList()
    return SyncRecord(SyncKinds.VIDEO, key, row.updated_at, data = buildJsonObject {
        put("kind", row.kind)
        put("media", row.media)
        row.channel?.let { put("channel", it) }
        row.thumbnail?.let { put("thumbnail", it) }
        put("duration", row.duration_ms)
        put("position", row.position_ms)
        put("subtitles", row.subtitle_kind)
        put("createdAt", row.created_at)
        put("cues", buildJsonArray { cues.forEach { add(cueJson(it)) } })
    }, deviceId = device)
}

private fun cueJson(cue: Video_cues) = buildJsonArray {
    add(JsonPrimitive(cue.begin_ms))
    add(JsonPrimitive(cue.end_ms))
    add(JsonPrimitive(cue.text))
    add(cue.translation?.let(::JsonPrimitive) ?: JsonNull)
    add(cue.translation_source?.let(::JsonPrimitive) ?: JsonNull)
    add(JsonPrimitive(cue.page_order))
    add(JsonPrimitive(cue.line_index))
}

internal suspend fun changedVideos(db: TayraDatabase, since: Long, device: String): List<SyncRecord> =
    db.videosQueries.changedVideos(since).awaitAsList().mapNotNull { videoRecord(db, it, device) }

/**
 * Applies a pulled VIDEO record to the text it belongs to, merged with this device's version; false
 * when that text is not here yet (the record waits), true otherwise.
 */
internal suspend fun applyVideo(db: TayraDatabase, remote: SyncRecord, bookId: Long, device: String): Boolean {
    val q = db.videosQueries
    val row = q.videoRowOfBook(bookId).awaitAsOneOrNull()?.let {
        ChangedVideos(it.book_id, it.kind, it.media, it.channel, it.thumbnail, it.duration_ms, it.position_ms, it.subtitle_kind, it.created_at, it.updated_at, it.sync_id, it.source_uri, it.title, it.language)
    }
    val local = row?.let { videoRecord(db, it, device) }
    val merged = if (local == null) remote else SyncMerge.merge(local, remote)
    if (local != null && local.updatedAt == merged.updatedAt && local.data == merged.data) return true
    if (merged.deleted) return true
    val d = merged.data ?: return true
    val kind = d.string("kind") ?: return true
    val media = d.string("media") ?: return true
    q.putVideo(
        bookId = bookId,
        kind = kind,
        media = media,
        channel = d.string("channel"),
        thumbnail = d.string("thumbnail"),
        durationMs = d.long("duration") ?: 0,
        positionMs = d.long("position") ?: 0,
        subtitleKind = d.string("subtitles") ?: "FILE",
        createdAt = d.long("createdAt") ?: merged.updatedAt,
        updatedAt = merged.updatedAt,
    )
    q.deleteCues(bookId)
    (d["cues"] as? JsonArray)?.forEachIndexed { order, element ->
        val c = element as? JsonArray ?: return@forEachIndexed
        q.insertCue(
            bookId = bookId,
            cueOrder = order.toLong(),
            pageOrder = c.getOrNull(5)?.jsonPrimitive?.longOrNull ?: 1,
            lineIndex = c.getOrNull(6)?.jsonPrimitive?.longOrNull ?: order.toLong(),
            beginMs = c.getOrNull(0)?.jsonPrimitive?.longOrNull ?: 0,
            endMs = c.getOrNull(1)?.jsonPrimitive?.longOrNull ?: 0,
            text = c.getOrNull(2)?.jsonPrimitive?.contentOrNull.orEmpty(),
            translation = (c.getOrNull(3) as? JsonPrimitive)?.contentOrNull,
            translationSource = (c.getOrNull(4) as? JsonPrimitive)?.contentOrNull,
        )
    }
    return true
}
