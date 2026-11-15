package com.idiomiko.core.domain.repository

import com.idiomiko.core.domain.model.Language
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun observeAll(): Flow<List<Language>>
    suspend fun getAll(): List<Language>
    suspend fun getById(id: Long): Language?
    suspend fun findByName(name: String): Language?
    /** Inserts or updates; returns the language id. */
    suspend fun save(language: Language): Long
    suspend fun delete(id: Long)
}
