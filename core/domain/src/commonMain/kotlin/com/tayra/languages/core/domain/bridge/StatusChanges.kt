package com.tayra.languages.core.domain.bridge

import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the words of the language being learned whose status changed, in the app, on another
 * device or in another browser tab, so the extension can colour its subtitles again. Each change
 * gets a number; the extension asks for those after the last it saw.
 */
class StatusChanges(
    private val terms: TermRepository,
    private val settings: SettingsRepository,
    private val keep: Int = 5_000,
) {
    private val lock = Mutex()
    private var seq = 0L
    private var language = 0L
    private var known: Map<String, TermStatus>? = null
    private val log = ArrayDeque<Pair<Long, Pair<String, Int>>>()
    /** The changes before this number are gone, and so is everything when the language changes. */
    private var oldest = 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged()
                .flatMapLatest { id -> if (id == 0L) emptyFlow() else terms.observeWordStatuses(id).map { id to it } }
                .collect { (id, statuses) -> record(id, statuses) }
        }
    }

    internal suspend fun record(languageId: Long, statuses: Map<String, TermStatus>) = lock.withLock {
        val before = known
        if (languageId != language || before == null) {
            language = languageId
            known = statuses
            log.clear()
            seq++
            oldest = seq
            return@withLock
        }
        val changed = statuses.filter { (lc, status) -> before[lc] != status }.mapValues { it.value.value } +
            before.keys.filter { it !in statuses }.associateWith { TermStatus.UNKNOWN.value }
        known = statuses
        for ((lc, status) in changed) {
            log.addLast(++seq to (lc to status))
            if (log.size > keep) oldest = log.removeFirst().first
        }
    }

    suspend fun since(after: Long): ChangesResponse = lock.withLock {
        if (after < oldest || after > seq) return@withLock ChangesResponse(seq, reset = after != 0L && after != seq)
        ChangesResponse(seq, log.filter { it.first > after }.associate { it.second })
    }
}
