package com.tayra.languages.core.domain.account

import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** How syncing with the account stands on this device. */
data class SyncStatus(
    val running: Boolean = false,
    /** When this device last synced, since the app started. */
    val lastSynced: Instant? = null,
    /** Why the last sync failed; null when it did not. */
    val problem: AccountProblem? = null,
    /** The account holds another library: syncing waits for the reader to choose to combine them. */
    val waitingToCombine: Boolean = false,
)

/**
 * Syncs the library with the account without being asked: when the app starts, when it is brought
 * back, a little after the library changes, and every little while, as long as the device is signed
 * in and the reader has not turned it off. It waits rather than combine two libraries, which only
 * the reader can choose, and stops trying after the account says it may not sync, until the account
 * changes. Syncing from the account page goes through it too, so the two never overlap.
 */
class AutoSync(
    private val sync: LibrarySync,
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
    /** Emits whenever the library is written to. */
    private val changes: Flow<Unit>,
    private val clock: Clock = Clock.System,
    private val quiet: Duration = 30.seconds,
    private val every: Duration = 10.minutes,
) {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val running = Mutex()
    /** The account the service refused to sync for, so it is not asked again until another signs in. */
    private var refusedFor: String? = null
    /** When the last sync ended: the library's notices of what it wrote come in just after. */
    private var ended: Instant = Instant.DISTANT_PAST

    /** Asks for a sync soon, such as when the app is brought back. */
    fun request() {
        requests.trySend(Unit)
    }

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch { for (request in requests) automatically() }
        scope.launch {
            // Its own writes while it syncs are not changes to send.
            changes.filter { !_status.value.running && clock.now() - ended > OWN_WRITES }.debounce(quiet).collect { request() }
        }
        scope.launch { accounts.signedIn.filterNotNull().collect { request() } }
        scope.launch {
            while (true) {
                delay(every)
                request()
            }
        }
    }

    /** Syncs now, as asked from the account page; [combine] when the reader chose to combine two libraries. Throws as [LibrarySync.syncNow]. */
    suspend fun syncNow(combine: Boolean = false): SyncResult = running.withLock {
        if (!combine && sync.needsCombining()) {
            _status.update { it.copy(waitingToCombine = true) }
            return@withLock SyncResult.NeedsCombining
        }
        SyncResult.Done(run())
    }

    private suspend fun automatically() {
        if (!settings.current.autoSync) return
        val account = accounts.signedIn.value ?: return
        if (account == refusedFor) return
        running.withLock {
            try {
                if (sync.needsCombining()) {
                    _status.update { it.copy(waitingToCombine = true) }
                    return
                }
                run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountException) {
                if (e.problem == AccountProblem.SUBSCRIPTION_REQUIRED || e.problem == AccountProblem.SIGNED_OUT) refusedFor = account
            } catch (_: Exception) {
                // Said on the status; the next request tries again.
            }
        }
    }

    private suspend fun run(): SyncSummary {
        _status.update { it.copy(running = true, problem = null) }
        try {
            val done = sync.syncNow()
            refusedFor = null
            _status.update { SyncStatus(lastSynced = clock.now()) }
            return done
        } catch (e: CancellationException) {
            _status.update { it.copy(running = false) }
            throw e
        } catch (e: AccountException) {
            _status.update { it.copy(running = false, problem = e.problem) }
            throw e
        } catch (e: Exception) {
            _status.update { it.copy(running = false, problem = AccountProblem.SERVER) }
            throw e
        } finally {
            ended = clock.now()
        }
    }

    private companion object {
        val OWN_WRITES = 2.seconds
    }
}

sealed interface SyncResult {
    data class Done(val summary: SyncSummary) : SyncResult
    data object NeedsCombining : SyncResult
}
