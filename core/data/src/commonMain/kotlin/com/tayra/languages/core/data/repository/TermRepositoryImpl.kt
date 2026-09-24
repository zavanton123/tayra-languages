package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.Terms
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermMatch
import com.tayra.languages.core.domain.model.TermRef
import com.tayra.languages.core.domain.model.TermReference
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.repository.MultiwordTerm
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListPage
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.repository.TermSortField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class TermRepositoryImpl(
    private val provider: DatabaseProvider,
    private val clock: Clock = Clock.System,
) : TermRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    override suspend fun getById(id: Long): Term? = withContext(databaseDispatcher) {
        val database = db()
        database.termsQueries.selectById(id).awaitAsOneOrNull()?.let { hydrate(database, listOf(it)).single() }
    }

    override suspend fun getByIds(ids: Collection<Long>): List<Term> = withContext(databaseDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val database = db()
        hydrate(database, database.termsQueries.selectByIds(ids.toList()).awaitAsList())
    }

    override suspend fun findByTextLc(languageId: Long, textLc: String): Term? = withContext(databaseDispatcher) {
        val database = db()
        database.termsQueries.selectByTextLc(languageId, textLc).awaitAsOneOrNull()?.let { hydrate(database, listOf(it)).single() }
    }

    override suspend fun findByTextLcs(languageId: Long, textLcs: Collection<String>): List<Term> = withContext(databaseDispatcher) {
        if (textLcs.isEmpty()) return@withContext emptyList()
        val database = db()
        val rows = textLcs.chunked(CHUNK).flatMap { chunk -> database.termsQueries.selectByTextLcs(languageId, chunk).awaitAsList() }
        hydrate(database, rows)
    }

    override suspend fun multiwordTerms(languageId: Long): List<MultiwordTerm> = withContext(databaseDispatcher) {
        db().termsQueries.selectMultiword(languageId).awaitAsList().map { MultiwordTerm(it.id, it.text_lc, it.token_count.toInt()) }
    }

    override suspend fun save(term: Term): Long = withContext(databaseDispatcher) {
        val database = db()
        database.transactionWithResult { saveInTransaction(database, term) }
    }

    private suspend fun saveInTransaction(database: TayraDatabase, term: Term): Long {
        val q = database.termsQueries
        val now = clock.now().toEpochMilliseconds()
        val id = if (term.id == 0L) {
            q.insert(
                languageId = term.languageId,
                text = term.text,
                textLc = term.textLc,
                status = term.status.value.toLong(),
                translation = term.translation,
                romanization = term.romanization,
                tokenCount = term.tokenCount.toLong(),
                syncStatus = term.syncStatus,
                flashMessage = term.flashMessage,
                createdAt = now,
                statusChangedAt = now,
            )
            q.lastInsertId().awaitAsOne()
        } else {
            val previous = q.selectById(term.id).awaitAsOneOrNull()
            q.update(
                id = term.id,
                text = term.text,
                textLc = term.textLc,
                status = term.status.value.toLong(),
                translation = term.translation,
                romanization = term.romanization,
                tokenCount = term.tokenCount.toLong(),
                syncStatus = term.syncStatus,
                flashMessage = term.flashMessage,
            )
            if (previous != null && previous.status != term.status.value.toLong()) {
                q.updateStatus(status = term.status.value.toLong(), changedAt = now, ids = listOf(term.id))
            }
            term.id
        }
        return id
    }

    override suspend fun insertAll(terms: List<Term>): List<Long> = withContext(databaseDispatcher) {
        if (terms.isEmpty()) return@withContext emptyList()
        val database = db()
        database.transactionWithResult { terms.map { saveInTransaction(database, it) } }
    }

    override suspend fun setParents(termId: Long, parentIds: List<Long>) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.termsQueries.deleteParents(termId)
            for (parentId in parentIds.distinct()) {
                if (parentId != termId) database.termsQueries.insertParent(termId, parentId)
            }
            database.termsQueries.clearSyncStatusForOrphans()
        }
    }

    override suspend fun updateStatus(termIds: Collection<Long>, status: TermStatus) = withContext(databaseDispatcher) {
        if (termIds.isEmpty()) return@withContext
        val database = db()
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            termIds.chunked(CHUNK).forEach { chunk ->
                database.termsQueries.updateStatus(status = status.value.toLong(), changedAt = now, ids = chunk)
            }
        }
    }

    override suspend fun updateSyncStatus(termId: Long, syncStatus: Boolean) {
        withContext(databaseDispatcher) {
            db().termsQueries.updateSyncStatus(syncStatus = syncStatus, id = termId)
        }
    }

    override suspend fun clearFlashMessage(termId: Long) {
        withContext(databaseDispatcher) {
            db().termsQueries.clearFlashMessage(termId)
        }
    }

    override suspend fun delete(termId: Long) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.termsQueries.delete(termId)
            database.termsQueries.clearSyncStatusForOrphans()
        }
    }

    override suspend fun deleteAll(termIds: Collection<Long>) = withContext(databaseDispatcher) {
        if (termIds.isEmpty()) return@withContext
        val database = db()
        database.transaction {
            termIds.chunked(CHUNK).forEach { database.termsQueries.deleteByIds(it) }
            database.termsQueries.clearSyncStatusForOrphans()
        }
    }

    override suspend fun parents(termId: Long): List<Term> = withContext(databaseDispatcher) {
        val database = db()
        hydrate(database, database.termsQueries.selectParents(termId).awaitAsList())
    }

    override suspend fun children(termId: Long): List<Term> = withContext(databaseDispatcher) {
        val database = db()
        hydrate(database, database.termsQueries.selectChildren(termId).awaitAsList())
    }

    override suspend fun search(languageId: Long, textLc: String, limit: Int): List<TermMatch> = withContext(databaseDispatcher) {
        val escaped = likeEscape(textLc)
        db().termsQueries.search(
            textLc = textLc,
            startsWith = "$escaped%",
            languageId = languageId,
            wildcard = "%$escaped%",
            limit = limit.toLong(),
        ).awaitAsList().map {
            TermMatch(
                id = it.id,
                text = it.text,
                translation = it.translation,
                status = TermStatus.fromValueOrNull(it.status.toInt()) ?: TermStatus.UNKNOWN,
                hasChildren = it.has_children == 1L,
            )
        }
    }

    override fun observeList(filter: TermListFilter, sort: TermListSort, offset: Int, limit: Int): Flow<TermListPage> = flow {
        val database = db()
        listQuery(database, filter, sort, offset, limit).asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(TermListPage(hydrate(database, rows.map { it.toTermsRow() }), count(database, filter)))
        }
    }

    override suspend fun list(filter: TermListFilter, sort: TermListSort, offset: Int, limit: Int): TermListPage = withContext(databaseDispatcher) {
        val database = db()
        val rows = listQuery(database, filter, sort, offset, limit).awaitAsList()
        TermListPage(hydrate(database, rows.map { it.toTermsRow() }), count(database, filter))
    }

    private fun listQuery(database: TayraDatabase, filter: TermListFilter, sort: TermListSort, offset: Int, limit: Int) =
        database.termsQueries.listTerms(
            languageId = filter.languageId?.takeIf { it != 0L },
            search = searchPattern(filter),
            parentsOnly = if (filter.parentsOnly) 1L else 0L,
            minStatus = filter.minStatus.value.toLong(),
            maxStatus = filter.maxStatus.value.toLong(),
            includeIgnored = if (filter.includeIgnored) 1L else 0L,
            minCreated = filter.minAgeDays?.let { ageThreshold(it) },
            maxCreated = filter.maxAgeDays?.let { ageThreshold(it) },
            filterIds = if (filter.termIds != null) 1L else 0L,
            ids = filter.termIds ?: listOf(-1L),
            sort = sortKey(sort),
            limit = limit.toLong(),
            offset = offset.toLong(),
        )

    private suspend fun count(database: TayraDatabase, filter: TermListFilter): Int =
        database.termsQueries.countTerms(
            languageId = filter.languageId?.takeIf { it != 0L },
            search = searchPattern(filter),
            parentsOnly = if (filter.parentsOnly) 1L else 0L,
            minStatus = filter.minStatus.value.toLong(),
            maxStatus = filter.maxStatus.value.toLong(),
            includeIgnored = if (filter.includeIgnored) 1L else 0L,
            minCreated = filter.minAgeDays?.let { ageThreshold(it) },
            maxCreated = filter.maxAgeDays?.let { ageThreshold(it) },
            filterIds = if (filter.termIds != null) 1L else 0L,
            ids = filter.termIds ?: listOf(-1L),
        ).awaitAsOne().toInt()

    private fun searchPattern(filter: TermListFilter): String =
        filter.search.trim().lowercase().let { if (it.isEmpty()) "" else "%${likeEscape(it)}%" }

    private fun ageThreshold(days: Int): Long = clock.now().toEpochMilliseconds() - days.toLong() * MILLIS_PER_DAY

    private fun sortKey(sort: TermListSort): String {
        val field = when (sort.field) {
            TermSortField.TEXT -> "text"
            TermSortField.STATUS -> "status"
            TermSortField.CREATED -> "created"
            TermSortField.LANGUAGE -> "language"
        }
        return "${field}_${if (sort.ascending) "asc" else "desc"}"
    }

    override suspend fun references(languageId: Long, termTextLc: String, limit: Int, includeUnread: Boolean): List<TermReference> =
        withContext(databaseDispatcher) {
            val pattern = "%$ZWS_STRING${likeEscape(termTextLc)}$ZWS_STRING%"
            db().termsQueries.references(
                onlyRead = if (includeUnread) 0L else 1L,
                languageId = languageId,
                pattern = pattern,
                limit = limit.toLong(),
            ).awaitAsList().map {
                TermReference(
                    bookId = it.book_id,
                    pageId = it.page_id,
                    pageNumber = it.page_order.toInt(),
                    bookTitle = it.title,
                    pageCount = it.page_count.toInt(),
                    sentence = highlight(it.text, termTextLc),
                )
            }
        }

    /** Wraps occurrences of the term in the sentence with `**` markers for the UI, removing separators. */
    private fun highlight(sentence: String, termLc: String): String {
        val marked = Regex(Regex.escape("$ZWS_STRING$termLc$ZWS_STRING"), RegexOption.IGNORE_CASE)
            .replace(sentence.trim()) { m -> "$ZWS_STRING**${m.value.trim(com.tayra.languages.core.domain.model.ZWS)}**$ZWS_STRING" }
        return marked.replace(ZWS_STRING, "").replace("¶", "")
    }

    /** Attaches parents to the raw rows. */
    private suspend fun hydrate(database: TayraDatabase, rows: List<Terms>): List<Term> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.id }
        val parents = HashMap<Long, MutableList<TermRef>>()
        for (chunk in ids.chunked(CHUNK)) {
            for (row in database.termsQueries.selectParentsForTerms(chunk).awaitAsList()) {
                parents.getOrPut(row.term_id) { mutableListOf() }.add(
                    TermRef(row.id, row.text, TermStatus.fromValueOrNull(row.status.toInt()) ?: TermStatus.UNKNOWN, row.translation),
                )
            }
        }
        return rows.map { it.toDomain(parents[it.id] ?: emptyList()) }
    }

    private fun com.tayra.languages.core.data.db.ListTerms.toTermsRow() = Terms(
        id = id, language_id = language_id, text = text, text_lc = text_lc, status = status, translation = translation,
        romanization = romanization, token_count = token_count, sync_status = sync_status,
        flash_message = flash_message, created_at = created_at, status_changed_at = status_changed_at,
    )

    private companion object {
        const val CHUNK = 500
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
