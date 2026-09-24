package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermMatch
import com.tayra.languages.core.domain.model.TermReference
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.flow.Flow

/** Filters for the term listing. */
data class TermListFilter(
    val languageId: Long? = null,
    val search: String = "",
    val parentsOnly: Boolean = false,
    val minStatus: TermStatus = TermStatus.UNKNOWN,
    val maxStatus: TermStatus = TermStatus.WELL_KNOWN,
    val includeIgnored: Boolean = false,
    val minAgeDays: Int? = null,
    val maxAgeDays: Int? = null,
    val termIds: List<Long>? = null,
)

data class TermListPage(val items: List<Term>, val totalCount: Int)

data class TermListSort(val field: TermSortField = TermSortField.CREATED, val ascending: Boolean = false)

enum class TermSortField { TEXT, STATUS, CREATED, LANGUAGE }

/** A multi-word term stub used when scanning texts. */
data class MultiwordTerm(val id: Long, val textLc: String, val tokenCount: Int)

interface TermRepository {
    suspend fun getById(id: Long): Term?
    suspend fun getByIds(ids: Collection<Long>): List<Term>
    suspend fun findByTextLc(languageId: Long, textLc: String): Term?
    suspend fun findByTextLcs(languageId: Long, textLcs: Collection<String>): List<Term>
    suspend fun multiwordTerms(languageId: Long): List<MultiwordTerm>

    /** Inserts or updates the term and its flash message. Returns the id. */
    suspend fun save(term: Term): Long
    suspend fun insertAll(terms: List<Term>): List<Long>
    suspend fun setParents(termId: Long, parentIds: List<Long>)
    suspend fun updateStatus(termIds: Collection<Long>, status: TermStatus)
    suspend fun updateSyncStatus(termId: Long, syncStatus: Boolean)
    suspend fun clearFlashMessage(termId: Long)
    suspend fun delete(termId: Long)
    suspend fun deleteAll(termIds: Collection<Long>)

    suspend fun parents(termId: Long): List<Term>
    suspend fun children(termId: Long): List<Term>

    suspend fun search(languageId: Long, textLc: String, limit: Int = 50): List<TermMatch>
    fun observeList(filter: TermListFilter, sort: TermListSort, offset: Int, limit: Int): Flow<TermListPage>
    suspend fun list(filter: TermListFilter, sort: TermListSort, offset: Int, limit: Int): TermListPage

    /** Sentences of read pages containing the term. */
    suspend fun references(languageId: Long, termTextLc: String, limit: Int = 20, includeUnread: Boolean = false): List<TermReference>

}
