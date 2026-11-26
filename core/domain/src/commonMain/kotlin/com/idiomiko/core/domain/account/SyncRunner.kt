package com.idiomiko.core.domain.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/** How syncing with the account stands on this device. */
data class SyncStatus(
    val running: Boolean = false,
    /** How far the sync under way has got; null when none is, or it has not said yet. */
    val progress: SyncProgress? = null,
    /** When this device last synced with the account signed in to, kept across restarts. */
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
    /** Where a combining the reader asked for runs, past the page that asked. */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _status = MutableStateFlow(SyncStatus(lastSynced = sync.lastSynced()))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Whether this device holds changes not sent yet, as [LibrarySync.unsent] says. */
    fun unsent(): Flow<Boolean> = sync.unsent()

    private val _combined = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /** Emits once a combining [combine] started has ended: true when the libraries are one, false when it failed. */
    val combined: SharedFlow<Boolean> = _combined.asSharedFlow()

    /**
     * Combines the two libraries, the reader's answer to the question, without them waiting on the page that asked:
     * the sync goes on when they leave it, and [combined] says how it ended.
     */
    fun combine() {
        _status.update { it.copy(waitingToCombine = false) }
        scope.launch {
            val done = try {
                syncNow(combine = true) is SyncResult.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The sync's status keeps why, for the sync card to say.
                false
            }
            _combined.emit(done)
        }
    }

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
