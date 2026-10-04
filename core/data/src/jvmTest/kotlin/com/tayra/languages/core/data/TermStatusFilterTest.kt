package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The vocabulary list can be narrowed to any set of statuses, ignored terms included. */
class TermStatusFilterTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-status", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val service = TermService(terms, languages)

    private suspend fun listed(filter: TermListFilter): List<String> =
        terms.list(filter, TermListSort(TermSortField.TEXT, ascending = true), 0, 100).items.map { it.text }

    private suspend fun seed(): Long {
        val id = languages.save(Language(name = "English"))
        mapOf("one" to TermStatus.NEW_1, "three" to TermStatus.LEARNING_3, "known" to TermStatus.WELL_KNOWN, "skip" to TermStatus.IGNORED)
            .forEach { (text, status) -> service.save(TermDraft(languageId = id, text = text, status = status, statusExplicitlySet = true)) }
        return id
    }

    @Test
    fun onlyTheChosenStatusesAreListed() = runTest {
        val id = seed()
        assertEquals(listOf("known", "three"), listed(TermListFilter(languageId = id, statuses = setOf(TermStatus.LEARNING_3, TermStatus.WELL_KNOWN))))
        assertEquals(listOf("one", "skip"), listed(TermListFilter(languageId = id, statuses = setOf(TermStatus.NEW_1, TermStatus.IGNORED))))
        assertEquals(3, terms.list(TermListFilter(languageId = id, statuses = setOf(TermStatus.NEW_1, TermStatus.LEARNING_3, TermStatus.WELL_KNOWN)), TermListSort(), 0, 1).totalCount)
    }

    @Test
    fun withoutChosenStatusesTheRangeStillApplies() = runTest {
        val id = seed()
        assertEquals(listOf("known", "one", "three"), listed(TermListFilter(languageId = id, minStatus = TermStatus.NEW_1)))
    }
}
