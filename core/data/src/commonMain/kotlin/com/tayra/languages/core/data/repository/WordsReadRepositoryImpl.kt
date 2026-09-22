package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.model.WordsReadEntry
import com.tayra.languages.core.domain.repository.WordsReadRepository
import com.tayra.languages.core.domain.stats.DailyWordCount
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class WordsReadRepositoryImpl(
    private val provider: DatabaseProvider,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : WordsReadRepository {

    override suspend fun add(entry: WordsReadEntry) {
        withContext(databaseDispatcher) {
            provider.database().wordsReadQueries.insert(
                languageId = entry.languageId,
                pageId = entry.pageId,
                readAt = entry.readAt.toEpochMillis(),
                wordCount = entry.wordCount.toLong(),
            )
        }
    }

    override suspend fun dailyCounts(): List<DailyWordCount> = withContext(databaseDispatcher) {
        provider.database().wordsReadQueries.selectAllWithLanguage().awaitAsList()
            .groupBy { it.language_name to it.read_at.toInstant().toLocalDateTime(timeZone).date }
            .map { (key, rows) -> DailyWordCount(key.first, key.second, rows.sumOf { it.word_count }.toInt()) }
    }
}
