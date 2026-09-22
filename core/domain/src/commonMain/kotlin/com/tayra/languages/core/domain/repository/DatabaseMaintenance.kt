package com.tayra.languages.core.domain.repository

interface DatabaseMaintenance {
    /** Deletes all languages, books, terms and tags. */
    suspend fun wipeAllData()
    suspend fun hasAnyLanguage(): Boolean
}
