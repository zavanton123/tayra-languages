package com.idiomiko.core.domain.repository

import com.idiomiko.core.domain.model.WordsReadEntry
import com.idiomiko.core.domain.stats.DailyWordCount

interface WordsReadRepository {
    suspend fun add(entry: WordsReadEntry)
    /** Word counts per language and day (in the local time zone). */
    suspend fun dailyCounts(): List<DailyWordCount>
}
