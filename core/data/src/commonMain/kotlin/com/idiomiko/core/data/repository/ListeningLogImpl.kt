package com.idiomiko.core.data.repository

import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.db.databaseDispatcher
import com.idiomiko.core.domain.stats.ListeningLog
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Instant

class ListeningLogImpl(private val provider: DatabaseProvider) : ListeningLog {
    override suspend fun record(languageId: Long, at: Instant, duration: Duration) {
        if (duration <= Duration.ZERO) return
        withContext(databaseDispatcher) {
            provider.database().listeningQueries.insert(languageId, at.toEpochMilliseconds(), duration.inWholeMilliseconds)
        }
    }
}
