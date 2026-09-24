package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import kotlinx.coroutines.withContext

class DatabaseMaintenanceImpl(private val provider: DatabaseProvider) : DatabaseMaintenance {

    override suspend fun wipeAllData() = withContext(databaseDispatcher) {
        val database = provider.database()
        database.transaction {
            database.maintenanceQueries.deleteAllWordsRead()
            database.maintenanceQueries.deleteAllLanguages()
            database.maintenanceQueries.deleteAllBookTags()
        }
    }

    override suspend fun hasAnyLanguage(): Boolean = withContext(databaseDispatcher) {
        provider.database().languagesQueries.countLanguages().awaitAsOne() > 0
    }
}
