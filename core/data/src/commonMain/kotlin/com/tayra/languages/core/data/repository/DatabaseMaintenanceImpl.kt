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
            // Emptying this device is not a change to send: the account keeps its records, and the
            // next sync takes them all afresh.
            database.syncQueries.setApplying(1)
            database.maintenanceQueries.deleteAllWordsRead()
            database.maintenanceQueries.deleteAllLanguages()
            database.maintenanceQueries.deleteAllBookTags()
            database.maintenanceQueries.deleteAllCourseTags()
            database.maintenanceQueries.deleteAllSentenceTranslations()
            database.syncQueries.clearTombstones()
            database.syncQueries.startOver()
            database.syncQueries.setApplying(0)
        }
    }

    override suspend fun hasAnyLanguage(): Boolean = withContext(databaseDispatcher) {
        provider.database().languagesQueries.countLanguages().awaitAsOne() > 0
    }
}
