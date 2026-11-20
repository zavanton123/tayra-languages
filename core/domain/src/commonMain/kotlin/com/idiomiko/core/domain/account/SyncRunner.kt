package com.idiomiko.core.domain.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/** How syncing with the account stands on this device. */
data class SyncStatus(
    val running: Boolean = false,
    /** How far the sync under way has got; null when none is, or it has not said yet. */
    val progress: SyncProgress? = null,
    /** When this device last synced, since the app started. */
    val lastSynced: Instant? = null,
    /** Why the last sync failed; null when it did not. */
    val problem: AccountProblem? = null,
    /** The account holds another library: syncing waits for the reader to choose to combine them. */
    val waitingToCombine: Boolean = false,
)

/**
 * Runs the syncs the reader asks for (from the sync prompt, a phone page pulled down, or the account
 * page's question about combining two libraries) one at a time: asked while a sync is under way, it
 * waits for that one and gives its result. It never syncs on its own. It waits rather than combine two
 * libraries, which only the reader can choose.
 */
class SyncRunner(
    private val sync: LibrarySync,
    private val clock: Clock = Clock.System,
) {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val running = Mutex()
    /** The sync under way, whose result a sync asked for meanwhile takes instead of running another after it. */
    private val current = MutableStateFlow<Deferred<SyncResult>?>(null)

    /**
     * The result of the sync under way, or of a new one, which waits rather than combine two libraries
     * unless [combine], the reader's choice. Throws as [LibrarySync.syncNow].
     */
    suspend fun syncNow(combine: Boolean = false): SyncResult {
        if (!combine) current.value?.let { return it.await() }
        val pass = CompletableDeferred<SyncResult>()
        running.withLock {
            current.value = pass
            try {
                pass.complete(
                    if (!combine && sync.needsCombining()) {
                        _status.update { it.copy(waitingToCombine = true) }
                        SyncResult.NeedsCombining
                    } else SyncResult.Done(run()),
                )
            } catch (e: Throwable) {
                pass.completeExceptionally(e)
            } finally {
                current.value = null
            }
        }
        return pass.await()
    }

    private suspend fun run(): SyncSummary {
        _status.update { it.copy(running = true, progress = null, problem = null) }
        try {
            val done = sync.syncNow { progress -> _status.update { it.copy(progress = progress) } }
            _status.update { SyncStatus(lastSynced = clock.now()) }
            return done
        } catch (e: CancellationException) {
            _status.update { it.copy(running = false, progress = null) }
            throw e
        } catch (e: AccountException) {
            _status.update { it.copy(running = false, progress = null, problem = e.problem) }
            throw e
        } catch (e: Exception) {
            _status.update { it.copy(running = false, progress = null, problem = AccountProblem.SERVER) }
            throw e
        }
    }
}

sealed interface SyncResult {
    data class Done(val summary: SyncSummary) : SyncResult
    data object NeedsCombining : SyncResult
}
