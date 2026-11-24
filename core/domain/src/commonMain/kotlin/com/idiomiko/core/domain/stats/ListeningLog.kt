package com.idiomiko.core.domain.stats

import kotlin.time.Duration
import kotlin.time.Instant

/** Keeps the time spent listening to playlists in a language, which counts toward the daily goal. */
fun interface ListeningLog {
    suspend fun record(languageId: Long, at: Instant, duration: Duration)
}
