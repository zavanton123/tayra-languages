package com.tayra.languages.core.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.tayra.languages.core.data.db.Sync_tombstones
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Texts and reading. A text's key is the same on every device: the lesson it was made from
// ("lesson:<id>"), the address of a pack's book ("uri:<address>"), the language of a tutorial
// ("tutorial:<code>"), or its own sync id ("id:<id>"). The first three have their text on every
// device from the course, the pack or the app, so only how far they were read travels; the others
// travel whole, a page a record, and are made on a device once all their pages are there. A record
// for a text this device does not have waits until it does.

/** The key of a text: by the lesson it was made from, the pack book's address, its language for a tutorial, or its own sync id. */
internal fun bookKey(lessonId: String?, sourceUri: String?, syncId: String?, title: String, language: String): String? = when {
    lessonId != null -> "lesson:$lessonId"
    sourceUri != null && sourceUri.startsWith(PACK_SOURCE) -> "uri:$sourceUri"
    sourceUri == null && Tutorials.forLanguage(language)?.title == title -> LanguageCodes.codeFor(language)?.let { "tutorial:$it" }
    syncId != null -> "id:$syncId"
    else -> null
}

/** Whether the text with [key] travels whole, as it exists only where it was made. */
internal fun isWhole(key: String): Boolean = key.startsWith("id:")

/** A page's key: its text's and its place in it. */
internal fun pageKey(book: String, order: Long): String = "$book/$order"

internal fun pageParts(key: String): Pair<String, Long>? {
    val at = key.lastIndexOf('/')
    if (at <= 0) return null
    return key.substring(0, at) to (key.substring(at + 1).toLongOrNull() ?: return null)
}

/** A bookmark's key: its page's, and when it was made (its title for one from before that was kept). */
internal fun bookmarkKey(book: String, order: Long, made: String): String = "${pageKey(book, order)}\n$made"

internal const val PACK_SOURCE = "tayra-books:"

/** Texts larger than this stay on the device they were made on. */
internal const val MAX_BOOK_TEXT = 2_000_000L

internal data class BookRow(
    val id: Long, val language: String, val syncId: String?, val sourceUri: String?, val title: String, val archived: Boolean,
    val updatedAt: Long, val lessonId: String?, val news: String?, val currentPage: Long?, val tags: String, val pageCount: Long,
) {
    val key: String? get() = bookKey(lessonId, sourceUri, syncId, title, language)

    /**
     * Null for a course's or pack's text not read yet: it is no one's change, and making it on
     * another device later must not undo the reading done before.
     */
    fun record(code: String, device: String): SyncRecord? {
        val key = key ?: return null
        if (!isWhole(key) && currentPage == null && !archived) return null
        return SyncRecord(SyncKinds.BOOK, key, updatedAt, data = buildJsonObject {
            put("language", code)
            put("archived", archived)
            currentPage?.let { put("currentPage", it) }
            if (isWhole(key)) {
                put("title", title)
                sourceUri?.let { put("source", it) }
                news?.let { put("news", it) }
                put("tags", buildJsonArray { tags.split(TermRow.PARENT_SEPARATOR).filter { it.isNotEmpty() }.sorted().forEach { add(JsonPrimitive(it)) } })
                put("pageCount", pageCount)
            }
        }, deviceId = device)
    }
}

internal data class PageRow(
    val order: Long, val text: String, val wordCount: Long, val startDate: Long?, val readDate: Long?, val updatedAt: Long,
    val syncId: String?, val sourceUri: String?, val lessonId: String?, val bookTitle: String, val language: String,
) {
    /** Null for a page of a course's or pack's text not opened yet, as for its text. */
    fun record(device: String): SyncRecord? {
        val book = bookKey(lessonId, sourceUri, syncId, bookTitle, language) ?: return null
        if (!isWhole(book) && startDate == null && readDate == null) return null
        return SyncRecord(SyncKinds.PAGE, pageKey(book, order), updatedAt, data = buildJsonObject {
            startDate?.let { put("startDate", it) }
            readDate?.let { put("readDate", it) }
            put("wordCount", wordCount)
            if (isWhole(book)) put("text", text)
        }, deviceId = device)
    }
}

internal data class BookmarkRow(
    val id: Long, val title: String, val createdAt: Long?, val updatedAt: Long, val order: Long,
    val syncId: String?, val sourceUri: String?, val lessonId: String?, val bookTitle: String, val language: String,
) {
    fun record(device: String): SyncRecord? {
        val book = bookKey(lessonId, sourceUri, syncId, bookTitle, language) ?: return null
        return SyncRecord(SyncKinds.BOOKMARK, bookmarkKey(book, order, createdAt?.toString() ?: title), updatedAt, data = buildJsonObject {
            put("title", title)
            createdAt?.let { put("createdAt", it) }
        }, deviceId = device)
    }
}

internal data class WordsReadRow(
    val readAt: Long, val wordCount: Long, val updatedAt: Long, val language: String, val order: Long?,
    val syncId: String?, val sourceUri: String?, val lessonId: String?, val bookTitle: String?,
) {
    fun record(code: String, device: String) = SyncRecord(SyncKinds.WORDS_READ, "$code/$readAt", updatedAt, data = buildJsonObject {
        put("wordCount", wordCount)
        val book = bookTitle?.let { bookKey(lessonId, sourceUri, syncId, it, language) }
        if (book != null && order != null) put("page", pageKey(book, order))
    }, deviceId = device)
}

internal val BOOK_KINDS = setOf(SyncKinds.BOOK, SyncKinds.PAGE, SyncKinds.BOOKMARK, SyncKinds.WORDS_READ)

internal val BOOK_TOMBSTONES = setOf("book", "page", "bookmark")

/** This device's texts and reading changed since a time, as records. */
internal class BookReader(private val db: TayraDatabase, private val device: String, private val codeOf: (String) -> String?) {
    private val q = db.syncBooksQueries

    suspend fun changedSince(since: Long): List<SyncRecord> {
        val oversized = q.oversizedBooks(MAX_BOOK_TEXT).awaitAsList().filterNotNull().map { "id:$it" }.toSet()
        fun fits(key: String) = key !in oversized && pageParts(key)?.first !in oversized
        val records = mutableListOf<SyncRecord>()
        q.changedBooks(since, ::BookRow).awaitAsList().forEach { row -> codeOf(row.language)?.let { row.record(it, device) }?.let { records += it } }
        q.changedPages(since, ::PageRow).awaitAsList().forEach { row -> row.record(device)?.let { records += it } }
        q.changedBookmarks(since, ::BookmarkRow).awaitAsList().forEach { row -> row.record(device)?.let { records += it } }
        q.changedWordsRead(since, ::WordsReadRow).awaitAsList().forEach { row -> codeOf(row.language)?.let { records += row.record(it, device) } }
        return records.filter { r -> if (r.kind == SyncKinds.BOOKMARK) fits(r.key.substringBefore('\n')) else fits(r.key) }
    }

    suspend fun deletion(t: Sync_tombstones): SyncRecord? {
        fun deleted(kind: String, key: String) = SyncRecord(kind, key, t.deleted_at, deleted = true, deviceId = device)
        // A deleted page's or bookmark's text, named by its sync id, may still be here, with another key.
        suspend fun keyOf(scope: String): String {
            val syncId = scope.removePrefix("id:")
            return q.bookIdOfSyncId(syncId).awaitAsOneOrNull()?.let { find(it) }?.key ?: "id:$syncId"
        }
        return when (t.kind) {
            "book" -> deleted(SyncKinds.BOOK, "id:${t.key}")
            "page" -> deleted(SyncKinds.PAGE, pageKey(keyOf(t.scope), t.key.toLongOrNull() ?: return null))
            "bookmark" -> {
                // Tombstones from before the key was kept with them name the sync id, and "order:made".
                val book = if (t.scope.startsWith("id:") || ':' !in t.scope) keyOf(t.scope) else t.scope
                val (order, made) = (if ('\n' in t.key) t.key.split('\n', limit = 2) else t.key.split(':', limit = 2)).takeIf { it.size == 2 } ?: return null
                deleted(SyncKinds.BOOKMARK, bookmarkKey(book, order.toLongOrNull() ?: return null, made))
            }
            else -> null
        }
    }

    private suspend fun find(id: Long): BookRow? = q.bookOfId(id, ::BookRow).awaitAsOneOrNull()
}

/**
 * Applies pulled text and reading records, each merged with this device's version. What cannot be
 * applied yet, a record of a text not here, is handed back to wait for a later sync.
 */
internal class BookWriter(private val db: TayraDatabase, private val device: String, private val languageId: (String) -> Long?) {
    private val q = db.syncBooksQueries

    class Result(val changed: Int, val deferred: List<SyncRecord>)

    suspend fun apply(records: List<SyncRecord>): Result {
        var changed = 0
        val deferred = mutableListOf<SyncRecord>()
        val pages = records.filter { it.kind == SyncKinds.PAGE }.groupBy { pageParts(it.key)?.first }
        val currentPages = mutableMapOf<Long, Long?>()
        val pageCounts = mutableMapOf<Long, Long>()
        val waiting = mutableSetOf<String>()

        for (remote in records.filter { it.kind == SyncKinds.BOOK }) {
            var row = find(remote.key)
            if (row == null && !remote.deleted && isWhole(remote.key)) row = adopt(remote, pages[remote.key].orEmpty())
            if (row == null) {
                if (remote.deleted) continue
                if (isWhole(remote.key) && create(remote, pages[remote.key].orEmpty())) {
                    row = find(remote.key)
                    changed++
                } else {
                    waiting += remote.key
                    deferred += remote
                    continue
                }
            }
            val book = row ?: continue
            val code = remote.data?.string("language")
            val local = book.record(code ?: "", device)
            val merged = if (local == null) remote else SyncMerge.merge(local, remote)
            if (local != null && same(local, merged)) continue
            if (merged.deleted) {
                if (isWhole(remote.key)) q.deleteBook(book.id)
                changed++
                continue
            }
            val d = merged.data ?: continue
            if (isWhole(remote.key)) {
                q.updateBook(d.string("title") ?: book.title, d.string("source"), d.bool("archived") ?: false, merged.updatedAt, book.id)
                q.clearBookTags(book.id)
                for (tag in d.strings("tags")) {
                    q.addTag(tag)
                    q.tagId(tag).awaitAsOneOrNull()?.let { q.tagBook(book.id, it) }
                }
                d.string("news")?.let { q.linkNews(it, book.id) }
                d.long("pageCount")?.let { pageCounts[book.id] = it }
            } else {
                q.setBookProgress(d.bool("archived") ?: false, merged.updatedAt, book.id)
            }
            currentPages[book.id] = d.long("currentPage")
            changed++
        }

        for (remote in records.filter { it.kind == SyncKinds.PAGE }) {
            val (key, order) = pageParts(remote.key) ?: continue
            val book = if (key in waiting) null else find(key)
            if (book == null) {
                if (!remote.deleted) deferred += remote
                continue
            }
            val row = q.pageOf(book.id, order, ::PageRow).awaitAsOneOrNull()
            val local = row?.record(device) ?: tombstoned(remote, "page", book.syncId.orEmpty(), order.toString())
            if (!write(local, remote) { m ->
                    val d = m.data
                    when {
                        m.deleted -> q.deletePage(book.id, order)
                        d == null -> Unit
                        row == null -> d.string("text")?.let { q.insertPage(book.id, order, it, d.long("wordCount") ?: 0, d.long("startDate"), d.long("readDate"), m.updatedAt) }
                        isWhole(key) -> q.updatePage(d.string("text") ?: row.text, d.long("wordCount") ?: row.wordCount, d.long("startDate"), d.long("readDate"), m.updatedAt, book.id, order)
                        else -> q.setPageDates(d.long("startDate"), d.long("readDate"), m.updatedAt, book.id, order)
                    }
                    q.clearStats(book.id)
                }
            ) continue
            changed++
        }
        for ((id, count) in pageCounts) q.deletePagesAfter(id, count)
        for ((id, order) in currentPages) if (order != null) q.setCurrentPage(id, order)

        for (remote in records.filter { it.kind == SyncKinds.BOOKMARK }) {
            val (page, made) = remote.key.split('\n', limit = 2).takeIf { it.size == 2 } ?: continue
            val (key, order) = pageParts(page) ?: continue
            val book = if (key in waiting) null else find(key)
            val pageId = book?.let { q.pageId(it.id, order).awaitAsOneOrNull() }
            if (pageId == null) {
                if (!remote.deleted) deferred += remote
                continue
            }
            val row = made.toLongOrNull()?.let { q.bookmarkMadeAt(pageId, it, ::BookmarkRow).awaitAsOneOrNull() } ?: q.bookmarkTitled(pageId, made, ::BookmarkRow).awaitAsOneOrNull()
            val local = row?.record(device) ?: tombstoned(remote, "bookmark", key, "$order\n$made")
            if (write(local, remote) { m ->
                    val title = m.data?.string("title")
                    when {
                        m.deleted -> row?.let { q.deleteBookmark(it.id) }
                        title == null -> Unit
                        row == null -> q.insertBookmark(pageId, title, m.data?.long("createdAt"), m.updatedAt)
                        else -> q.renameBookmark(title, m.updatedAt, row.id)
                    }
                }
            ) changed++
        }

        for (remote in records.filter { it.kind == SyncKinds.WORDS_READ }) {
            if (remote.deleted) continue
            val language = languageId(remote.key.substringBefore('/')) ?: continue
            val readAt = remote.key.substringAfter('/').toLongOrNull() ?: continue
            if (q.wordsReadAt(language, readAt).awaitAsOne() > 0) continue
            val pageId = remote.data?.string("page")?.let(::pageParts)?.let { (key, order) -> find(key)?.let { q.pageId(it.id, order).awaitAsOneOrNull() } }
            q.insertWordsRead(language, pageId, readAt, remote.data?.long("wordCount") ?: 0, remote.updatedAt)
            changed++
        }
        return Result(changed, deferred)
    }

    private suspend fun find(key: String): BookRow? = when {
        key.startsWith("lesson:") -> q.bookOfLesson(key.removePrefix("lesson:"), ::BookRow).awaitAsOneOrNull()
        key.startsWith("uri:") -> q.bookOfSource(key.removePrefix("uri:"), ::BookRow).awaitAsOneOrNull()
        key.startsWith("id:") -> q.bookOfSyncId(key.removePrefix("id:"), ::BookRow).awaitAsOneOrNull()
        key.startsWith("tutorial:") -> {
            val code = key.removePrefix("tutorial:")
            val language = languageId(code)
            val title = LanguageCodes.option(code)?.let { Tutorials.forLanguage(it.name)?.title }
            if (language == null || title == null) null
            else q.booksTitled(language, title).awaitAsList().firstOrNull()?.let { q.bookOfId(it, ::BookRow).awaitAsOneOrNull() }
        }
        else -> null
    }

    /**
     * A text made here that is the one [remote] stands for, which takes its key: the same download,
     * or one with the same title and first page, such as a tutorial every device makes for itself.
     */
    private suspend fun adopt(remote: SyncRecord, pages: List<SyncRecord>): BookRow? {
        val d = remote.data ?: return null
        val language = languageId(d.string("language") ?: return null) ?: return null
        val source = d.string("source")
        val candidate = if (source != null) {
            q.bookOfSource(source, ::BookRow).awaitAsOneOrNull()?.id
        } else {
            val first = pages.firstOrNull { pageParts(it.key)?.second == 1L }?.data?.string("text") ?: return null
            q.booksTitled(language, d.string("title") ?: return null).awaitAsList().firstOrNull { id -> q.firstPageText(id).awaitAsOneOrNull() == first }
        } ?: return null
        q.adoptBook(remote.key.removePrefix("id:"), candidate)
        return q.bookOfSyncId(remote.key.removePrefix("id:"), ::BookRow).awaitAsOneOrNull()
    }

    /** Makes the text [remote] stands for, when all its pages came with it; whether it did. */
    private suspend fun create(remote: SyncRecord, pages: List<SyncRecord>): Boolean {
        val d = remote.data ?: return false
        val language = languageId(d.string("language") ?: return false) ?: return false
        val count = d.long("pageCount") ?: return false
        val texts = pages.filter { !it.deleted }.associateBy { pageParts(it.key)?.second }
        if (count < 1 || (1..count).any { texts[it]?.data?.string("text") == null }) return false
        q.insertBook(language, d.string("title") ?: return false, d.string("source"), d.bool("archived") ?: false, remote.key.removePrefix("id:"), 0)
        // Its pages are added as their records are applied, and the book gets the record's time then.
        return true
    }

    private suspend fun tombstoned(remote: SyncRecord, kind: String, scope: String, key: String): SyncRecord? =
        db.syncRecordsQueries.tombstone(kind, scope, key).awaitAsOneOrNull()?.let { SyncRecord(remote.kind, remote.key, it, deleted = true, deviceId = device) }

    private suspend fun write(local: SyncRecord?, remote: SyncRecord, put: suspend (SyncRecord) -> Unit): Boolean {
        val merged = if (local == null) remote else SyncMerge.merge(local, remote)
        if (local != null && same(local, merged)) return false
        if (local == null && merged.deleted) return false
        put(merged)
        return true
    }

    private fun same(a: SyncRecord, b: SyncRecord) = a.updatedAt == b.updatedAt && a.deleted == b.deleted && a.data == b.data
}
