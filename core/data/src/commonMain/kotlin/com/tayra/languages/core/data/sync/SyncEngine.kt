package com.tayra.languages.core.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.Flashcards
import com.tayra.languages.core.data.db.Practice_progress
import com.tayra.languages.core.data.db.Sync_tombstones
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.LibrarySync
import com.tayra.languages.core.domain.account.SyncSummary
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncKeys
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

/** The account's side of sync: sending records, and taking the ones changed after a change number. */
interface SyncTransport {
    /** The account signed in to, with the service it is on; null when none is. */
    fun syncAccount(): String?

    suspend fun push(request: PushRequest): PushResponse

    suspend fun pull(since: Long, limit: Int): PullResponse
}

/**
 * Sync of the vocabulary: words with their parents and vocabulary-level mark, flashcards and their
 * reviews, practice progress and examples, vocabulary levels, the languages' own settings, and the
 * removal of a language.
 *
 * A sync sends the records changed since the last one (all of them after a restore or a wipe) and the
 * deletions, then takes every record changed in the account since its last change number and applies
 * them in one transaction, each merged with this device's version by the shared rules, the triggers
 * told the writes are not this device's own. A failure leaves the library and the change number as
 * they were, so the next sync takes it all again.
 */
class SyncEngine(
    private val provider: DatabaseProvider,
    private val transport: SyncTransport,
    private val settings: Settings,
    /** Makes the catalog's language with the code, which a device that never set it up lacks. */
    private val addLanguage: suspend (code: String) -> Unit = {},
    private val clock: Clock = Clock.System,
) : LibrarySync {
    private val running = Mutex()

    override suspend fun syncNow(): SyncSummary = running.withLock {
        val account = transport.syncAccount() ?: throw AccountException(AccountProblem.SIGNED_OUT, "Not signed in")
        val db = provider.database()
        // What this device sent and took went to another account: the one now gets all of it.
        if (settings.getStringOrNull(ACCOUNT) != account) {
            withContext(databaseDispatcher) { db.syncQueries.startOver() }
            settings.putString(ACCOUNT, account)
        }
        val state = withContext(databaseDispatcher) { db.syncQueries.state().awaitAsOne() }
        val device = state.device_id
        val startedAt = clock.now().toEpochMilliseconds()
        val since = if (state.full_push != 0L) -1L else state.last_push_at ?: -1L

        val outgoing = withContext(databaseDispatcher) { Reader(db, device).changedSince(since) }
        for (batch in outgoing.records.chunked(BATCH)) transport.push(PushRequest(device, batch))
        withContext(databaseDispatcher) {
            db.transaction {
                // A change made in the millisecond the sync started may have come after the reading: it goes again.
                db.syncRecordsQueries.pushed(startedAt - 1)
                for (t in outgoing.tombstones) db.syncRecordsQueries.forgetTombstones(t.kind, t.scope, t.key, startedAt)
            }
        }

        val incoming = mutableListOf<SyncRecord>()
        var seq = state.last_seq
        do {
            val page = transport.pull(seq, BATCH)
            incoming += page.records
            seq = page.seq
        } while (page.more)
        val have = withContext(databaseDispatcher) { db.syncRecordsQueries.languageNames().awaitAsList().mapNotNull { LanguageCodes.codeFor(it.name) }.toSet() }
        for (code in incoming.filter { it.kind != LANGUAGE_REMOVED }.mapNotNull(::languageCodeOf).toSet() - have) addLanguage(code)
        val changed = withContext(databaseDispatcher) {
            db.transactionWithResult {
                db.syncQueries.setApplying(1)
                val n = Writer(db, device).apply(incoming)
                db.syncQueries.setApplying(0)
                db.syncRecordsQueries.pulled(seq)
                n
            }
        }
        SyncSummary(sent = outgoing.records.size, received = incoming.size, changed = changed)
    }

    private class Outgoing(val records: List<SyncRecord>, val tombstones: List<Sync_tombstones>)

    /** The languages by the code they travel as, and back. */
    private class Languages(rows: List<Pair<Long, String>>) {
        val codeOf: Map<String, String> = rows.mapNotNull { (_, name) -> LanguageCodes.codeFor(name)?.let { name to it } }.toMap()
        val idOf: Map<String, Long> = rows.mapNotNull { (id, name) -> LanguageCodes.codeFor(name)?.let { it to id } }.toMap()
        val nameOf: Map<String, String> = rows.mapNotNull { (_, name) -> LanguageCodes.codeFor(name)?.let { it to name } }.toMap()
    }

    private class Reader(private val db: TayraDatabase, private val device: String) {
        suspend fun changedSince(since: Long): Outgoing {
            val q = db.syncRecordsQueries
            val langs = Languages(q.languageNames().awaitAsList().map { it.id to it.name })
            val records = mutableListOf<SyncRecord>()
            fun code(language: String) = langs.codeOf[language]
            q.changedLanguages(since, ::LanguageRow).awaitAsList().forEach { row ->
                val c = code(row.name) ?: return@forEach
                records += languageRecord(row, q.dictionariesOf(row.id, ::DictionaryRow).awaitAsList(), c, device)
            }
            q.changedTerms(since, ::TermRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }
            q.changedFlashcards(since, ::FlashcardRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }
            q.changedReviews(since, ::ReviewRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }
            q.changedProgress(since, ::ProgressRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }
            q.changedExamples(since, ::ExampleRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }
            q.changedLevels(since, ::LevelRow).awaitAsList().forEach { r -> code(r.language)?.let { records += r.record(it, device) } }

            val tombstones = q.tombstonesSince(since).awaitAsList().filter { it.kind in VOCABULARY_TOMBSTONES }
            for (t in tombstones) {
                val record = deletion(t, langs) ?: continue
                records += record
            }
            return Outgoing(records, tombstones)
        }

        private fun deletion(t: Sync_tombstones, langs: Languages): SyncRecord? {
            val code = langs.codeOf[t.scope] ?: LanguageCodes.codeFor(t.scope) ?: return null
            fun deleted(kind: String, key: String) = SyncRecord(kind, key, t.deleted_at, deleted = true, deviceId = device)
            return when (t.kind) {
                "term" -> deleted(SyncKinds.TERM, SyncKeys.term(code, t.key))
                "flashcard" -> deleted(SyncKinds.FLASHCARD, SyncKeys.ofTerm(code, t.key))
                "practice_progress" -> deleted(SyncKinds.PRACTICE_PROGRESS, SyncKeys.ofTerm(code, t.key))
                "review" -> {
                    val (at, textLc) = t.key.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
                    deleted(SyncKinds.REVIEW, SyncKeys.review(code, textLc, at.toLongOrNull() ?: return null))
                }
                "practice_example" -> {
                    val (textLc, sentence) = t.key.split('\n', limit = 2).takeIf { it.size == 2 } ?: return null
                    deleted(SyncKinds.PRACTICE_EXAMPLE, SyncKeys.practiceExample(code, textLc, sentence))
                }
                "vocabulary_level" -> deleted(SyncKinds.VOCABULARY_LEVEL, SyncKeys.language(code))
                // A removal is a time, not a deletion: records of the language older than it lose to it.
                "language" -> SyncRecord(LANGUAGE_REMOVED, SyncKeys.language(code), t.deleted_at, data = JsonObject(emptyMap()), deviceId = device)
                else -> null
            }
        }
    }

    /** Applies pulled records: languages first, then words, then what belongs to words. */
    private class Writer(private val db: TayraDatabase, private val device: String) {
        private val q = db.syncRecordsQueries
        private lateinit var langs: Languages

        suspend fun apply(incoming: List<SyncRecord>): Int {
            langs = Languages(q.languageNames().awaitAsList().map { it.id to it.name })
            // The latest removal of each language: its records from before it are stale.
            val removedAt = incoming.filter { it.kind == LANGUAGE_REMOVED && !it.deleted }.groupBy { it.key }.mapValues { (_, v) -> v.maxOf { it.updatedAt } }
            var changed = 0
            for ((code, at) in removedAt) {
                val id = langs.idOf[code] ?: continue
                q.removeTermsBefore(id, at)
                q.removeLevelBefore(id, at)
                changed++
            }
            val live = incoming.filter { record ->
                val code = languageCodeOf(record) ?: return@filter false
                val removal = removedAt[code]
                removal == null || record.updatedAt >= removal
            }
            val pendingParents = mutableListOf<Pair<Long, Pair<Long, List<String>>>>()
            for (record in live.sortedBy { ORDER.indexOf(it.kind).let { i -> if (i < 0) ORDER.size else i } }) {
                if (applyOne(record, pendingParents)) changed++
            }
            // A word's parents may come after it in the same pull.
            for ((termId, rest) in pendingParents) {
                val (languageId, parents) = rest
                q.clearParents(termId)
                for (parent in parents) q.termId(languageId, parent).awaitAsOneOrNull()?.let { q.addParent(termId, it) }
            }
            return changed
        }

        /** Merges [remote] with this device's version and writes the result if it differs; whether it did. */
        private suspend fun applyOne(remote: SyncRecord, pendingParents: MutableList<Pair<Long, Pair<Long, List<String>>>>): Boolean {
            if (remote.kind == LANGUAGE_REMOVED) return false
            val code = languageCodeOf(remote) ?: return false
            val languageId = langs.idOf[code] ?: return false
            val languageName = langs.nameOf.getValue(code)
            return when (remote.kind) {
                SyncKinds.LANGUAGE -> {
                    val row = q.languageSettings(languageId, ::LanguageRow).awaitAsOne()
                    val local = languageRecord(row, q.dictionariesOf(languageId, ::DictionaryRow).awaitAsList(), code, device).takeIf { row.updatedAt > 0 }
                    write(local, remote) { m ->
                        val data = m.data ?: return@write
                        q.setLanguageSettings(data.bool("showRomanization") ?: false, m.updatedAt, languageId)
                        q.clearDictionaries(languageId)
                        for (d in data.objects("dictionaries")) {
                            q.addDictionary(languageId, d.string("useFor") ?: continue, d.string("dictType") ?: continue, d.string("url") ?: continue, d.bool("active") ?: true, d.long("order") ?: 0)
                        }
                        val script = data.string("script")
                        if (script != null) q.setScript(languageId, script) else q.clearScript(languageId)
                    }
                }
                SyncKinds.VOCABULARY_LEVEL -> {
                    val local = q.levelOf(languageId, ::LevelRow).awaitAsOneOrNull()?.record(code, device) ?: tombstoned(remote, "vocabulary_level", languageName, "")
                    write(local, remote) { m ->
                        if (m.deleted) q.deleteLevel(languageId) else q.upsertLevel(languageId, m.data?.long("level") ?: return@write, m.updatedAt)
                    }
                }
                SyncKinds.TERM -> {
                    val textLc = SyncKeys.termParts(remote.key)!!.second
                    val row = q.termByKey(languageId, textLc, ::TermRow).awaitAsOneOrNull()
                    val local = row?.record(code, device) ?: tombstoned(remote, "term", languageName, textLc)
                    write(local, remote) { m ->
                        if (m.deleted) {
                            row?.let { q.deleteTerm(it.id) }
                            return@write
                        }
                        val d = m.data ?: return@write
                        val text = d.string("text") ?: return@write
                        val status = d.long("status") ?: return@write
                        val statusChangedAt = d.long("statusChangedAt") ?: m.updatedAt
                        val id = if (row != null) {
                            q.updateTerm(text, status, d.string("translation"), d.string("romanization"), d.long("tokenCount") ?: 1, d.string("flashMessage"),
                                d.long("createdAt") ?: m.updatedAt, statusChangedAt, d.string("translationLanguage"), d.string("sentence"), m.updatedAt, row.id)
                            row.id
                        } else {
                            q.insertTerm(languageId, text, textLc, status, d.string("translation"), d.string("romanization"), d.long("tokenCount") ?: 1, d.string("flashMessage"),
                                d.long("createdAt") ?: m.updatedAt, statusChangedAt, d.string("translationLanguage"), d.string("sentence"), m.updatedAt)
                            q.termId(languageId, textLc).awaitAsOne()
                        }
                        pendingParents += id to (languageId to d.strings("parents"))
                        if (d.bool("level") == true) q.addLevelTerm(id) else q.removeLevelTerm(id)
                    }
                }
                SyncKinds.FLASHCARD, SyncKinds.PRACTICE_PROGRESS -> {
                    val textLc = SyncKeys.termParts(remote.key)!!.second
                    val termId = q.termId(languageId, textLc).awaitAsOneOrNull() ?: return false
                    val tombKind = if (remote.kind == SyncKinds.FLASHCARD) "flashcard" else "practice_progress"
                    val local = (if (remote.kind == SyncKinds.FLASHCARD) q.flashcardOf(termId, ::FlashcardRow).awaitAsOneOrNull()?.record(code, device)
                        else q.progressOf(termId, ::ProgressRow).awaitAsOneOrNull()?.record(code, device)) ?: tombstoned(remote, tombKind, languageName, textLc)
                    write(local, remote) { m ->
                        val d = m.data
                        if (remote.kind == SyncKinds.FLASHCARD) {
                            if (m.deleted || d == null) q.deleteFlashcard(termId)
                            else q.upsertFlashcard(Flashcards(termId, d.long("state") ?: 0, d.long("due") ?: 0, d.double("stability"), d.double("difficulty"), d.long("intervalDays") ?: 0,
                                d.long("remainingSteps") ?: 0, d.long("reps") ?: 0, d.long("lapses") ?: 0, d.long("lastReview"), d.bool("suspended") ?: false, d.long("seenStatus") ?: 0, m.updatedAt))
                        } else {
                            if (m.deleted || d == null) q.deleteProgress(termId)
                            else q.upsertProgress(Practice_progress(termId, d.long("level") ?: 0, d.long("streak") ?: 0, d.long("asked") ?: 0, d.long("rightAnswers") ?: 0, d.long("lastPractised"), m.updatedAt))
                        }
                    }
                }
                SyncKinds.REVIEW -> {
                    val reviewedAt = remote.key.substringBefore('/').toLongOrNull() ?: return false
                    val textLc = SyncKeys.termParts(remote.key.substringAfter('/'))?.second ?: return false
                    val termId = q.termId(languageId, textLc).awaitAsOneOrNull() ?: return false
                    val local = q.reviewOf(termId, reviewedAt, ::ReviewRow).awaitAsOneOrNull()?.record(code, device) ?: tombstoned(remote, "review", languageName, "$reviewedAt:$textLc")
                    write(local, remote) { m ->
                        val d = m.data
                        if (m.deleted || d == null) q.deleteReview(termId, reviewedAt)
                        else if (local == null || local.deleted) q.insertReview(termId, reviewedAt, d.long("rating") ?: 0, d.long("stateBefore") ?: 0, d.long("intervalDays") ?: 0, d.long("elapsedDays") ?: 0, m.updatedAt)
                    }
                }
                SyncKinds.PRACTICE_EXAMPLE -> {
                    val termKey = remote.key.substringBefore('\n')
                    val sentence = remote.key.substringAfter('\n')
                    val textLc = SyncKeys.termParts(termKey)?.second ?: return false
                    val termId = q.termId(languageId, textLc).awaitAsOneOrNull() ?: return false
                    val local = q.exampleOf(termId, sentence, ::ExampleRow).awaitAsOneOrNull()?.record(code, device) ?: tombstoned(remote, "practice_example", languageName, "$textLc\n$sentence")
                    write(local, remote) { m ->
                        val d = m.data
                        if (m.deleted || d == null) q.deleteExample(termId, sentence)
                        else q.upsertExample(termId, sentence, d.string("translation"), d.string("form") ?: return@write, d.long("formStart") ?: 0, d.string("source") ?: return@write, d.string("sourceId"), d.long("fetchedAt") ?: m.updatedAt, m.updatedAt)
                    }
                }
                else -> false
            }
        }

        /** This device's deletion of the record, when it has one; null when it never had the record. */
        private suspend fun tombstoned(remote: SyncRecord, kind: String, scope: String, key: String): SyncRecord? =
            q.tombstone(kind, scope, key).awaitAsOneOrNull()?.let { SyncRecord(remote.kind, remote.key, it, deleted = true, deviceId = device) }

        /** Writes the merge of [local] and [remote] when it is not what is here already. */
        private suspend fun write(local: SyncRecord?, remote: SyncRecord, put: suspend (SyncRecord) -> Unit): Boolean {
            val merged = if (local == null) remote else SyncMerge.merge(local, remote)
            if (local != null && merged.updatedAt == local.updatedAt && merged.deleted == local.deleted && merged.data == local.data) return false
            if (local == null && merged.deleted) return false
            put(merged)
            return true
        }
    }

    private companion object {
        const val ACCOUNT = "sync.account"
        const val BATCH = 1_000
        /** The order pulled records are applied in: what others refer to first. */
        val ORDER = listOf(SyncKinds.LANGUAGE, SyncKinds.VOCABULARY_LEVEL, SyncKinds.TERM, SyncKinds.FLASHCARD, SyncKinds.PRACTICE_PROGRESS, SyncKinds.PRACTICE_EXAMPLE, SyncKinds.REVIEW)
        val VOCABULARY_TOMBSTONES = setOf("term", "flashcard", "review", "practice_progress", "practice_example", "vocabulary_level", "language")
    }
}

/** The code of the language a record belongs to. */
private fun languageCodeOf(record: SyncRecord): String? = when (record.kind) {
    SyncKinds.LANGUAGE, SyncKinds.VOCABULARY_LEVEL, LANGUAGE_REMOVED -> record.key
    SyncKinds.REVIEW -> SyncKeys.termParts(record.key.substringAfter('/'))?.first
    SyncKinds.PRACTICE_EXAMPLE -> SyncKeys.termParts(record.key.substringBefore('\n'))?.first
    SyncKinds.TERM, SyncKinds.FLASHCARD, SyncKinds.PRACTICE_PROGRESS -> SyncKeys.termParts(record.key)?.first
    else -> null
}
