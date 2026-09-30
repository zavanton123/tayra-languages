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

/** A saved translation and the language it is written in, null when that was never recorded. */
data class SavedTranslation(val termId: Long, val languageId: Long, val text: String, val translation: String, val language: String?)

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

    /** Saved translations not known to be written in [language]. */
    suspend fun translationsNotIn(language: String): List<SavedTranslation>

    /** Replaces a translation, unless it no longer reads [expected]. Returns whether it was replaced. */
    suspend fun replaceTranslation(termId: Long, expected: String, translation: String, language: String): Boolean

    /** Records [language] for translations whose language was never recorded. */
    suspend fun markTranslationLanguage(termIds: Collection<Long>, language: String)

    /** Sentences of read pages containing the term. */
    suspend fun references(languageId: Long, termTextLc: String, limit: Int = 20, includeUnread: Boolean = false): List<TermReference>

}
