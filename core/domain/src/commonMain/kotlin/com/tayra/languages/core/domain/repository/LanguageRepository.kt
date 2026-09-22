package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageSummary
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeAll(): Flow<List<Language>>
    fun observeSummaries(): Flow<List<LanguageSummary>>
    suspend fun getAll(): List<Language>
    suspend fun getById(id: Long): Language?
    suspend fun findByName(name: String): Language?
    /** Inserts or updates; returns the language id. */
    suspend fun save(language: Language): Long
    suspend fun delete(id: Long)
}
