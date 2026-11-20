package com.idiomiko.core.domain.account

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Syncing only when asked: one sync at a time, never combining unasked, the status said as it goes. */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRunnerTest {

    private class Sync : LibrarySync {
        var started = 0
        var syncs = 0
        var combining = false
        var failWith: AccountProblem? = null
        /** Holds each sync halfway until completed, as a long one over the network. */
        var hold: CompletableDeferred<Unit>? = null
        override suspend fun syncNow(progress: (SyncProgress) -> Unit): SyncSummary {
            started++
            progress(SyncProgress(SyncPhase.SENDING, 12_000, 47_480))
            hold?.await()
            failWith?.let { throw AccountException(it, "No") }
            syncs++
            return SyncSummary(0, 0, 0)
        }
        override suspend fun needsCombining() = combining
    }

    private class Env(scope: TestScope) {
        val sync = Sync()
        val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(scope.testScheduler.currentTime) }
        val runner = SyncRunner(sync, clock)
    }

    @Test
    fun aSyncRunsWhenAskedAndSaysWhenItEnded() = runTest {
        val env = Env(this)
        assertNull(env.runner.status.value.lastSynced)
        assertTrue(env.runner.syncNow() is SyncResult.Done)
        assertEquals(1, env.sync.syncs)
        assertEquals(Instant.fromEpochMilliseconds(testScheduler.currentTime), env.runner.status.value.lastSynced)
        advanceTimeBy(5.hours)
        assertEquals(1, env.sync.started, "nothing more after it")
    }

    @Test
    fun itWaitsWhenTwoLibrariesWouldCombine() = runTest {
        val env = Env(this)
        env.sync.combining = true
        assertEquals(SyncResult.NeedsCombining, env.runner.syncNow())
        assertEquals(0, env.sync.syncs, "combining two libraries is the reader's choice")
        assertTrue(env.runner.status.value.waitingToCombine)

        assertTrue(env.runner.syncNow(combine = true) is SyncResult.Done)
        assertEquals(1, env.sync.syncs)
        assertFalse(env.runner.status.value.waitingToCombine)
    }

    @Test
    fun syncingAgainDuringASyncWaitsForItInsteadOfSyncingAgain() = runTest {
        val env = Env(this)
        val hold = CompletableDeferred<Unit>().also { env.sync.hold = it }
        val first = async { env.runner.syncNow() }
        runCurrent()
        assertTrue(env.runner.status.value.running)
        assertEquals(SyncProgress(SyncPhase.SENDING, 12_000, 47_480), env.runner.status.value.progress)

        val asked = async { env.runner.syncNow() }
        runCurrent()
        assertFalse(asked.isCompleted, "it waits for the sync under way")
        hold.complete(Unit)
        runCurrent()
        assertEquals(SyncResult.Done(SyncSummary(0, 0, 0)), asked.await())
        assertEquals(first.await(), asked.await())
        assertEquals(1, env.sync.started, "no second sync after it")
        assertFalse(env.runner.status.value.running)
        assertNull(env.runner.status.value.progress)
    }

    @Test
    fun aSyncThatFailsEndsAsNotRunningForWhoeverWaitedForIt() = runTest {
        val env = Env(this)
        val hold = CompletableDeferred<Unit>().also { env.sync.hold = it }
        val first = async { runCatching { env.runner.syncNow() } }
        runCurrent()
        val asked = async { runCatching { env.runner.syncNow() } }
        runCurrent()
        env.sync.failWith = AccountProblem.OFFLINE
        hold.complete(Unit)
        runCurrent()
        assertEquals(AccountProblem.OFFLINE, (first.await().exceptionOrNull() as AccountException).problem)
        assertEquals(AccountProblem.OFFLINE, (asked.await().exceptionOrNull() as AccountException).problem)
        assertFalse(env.runner.status.value.running)
        assertNull(env.runner.status.value.progress)
        assertEquals(AccountProblem.OFFLINE, env.runner.status.value.problem)

        env.sync.hold = null
        env.sync.failWith = null
        assertTrue(env.runner.syncNow() is SyncResult.Done, "the next sync runs afresh")
        assertEquals(2, env.sync.started)
        assertNull(env.runner.status.value.problem)
    }
}
