package com.tayra.languages.core.domain.account

import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Syncing on its own: at start, after a quiet spell following changes, every while; never combining, never past a refusal. */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoSyncTest {

    private class Sync : LibrarySync {
        var syncs = 0
        var combining = false
        var failWith: AccountProblem? = null
        override suspend fun syncNow(): SyncSummary {
            failWith?.let { throw AccountException(it, "No") }
            syncs++
            return SyncSummary(0, 0, 0)
        }
        override suspend fun needsCombining() = combining
    }

    private class Accounts : AccountRepository {
        override val available = true
        override val signedIn = MutableStateFlow<String?>("reader@example.com")
        override suspend fun register(email: String, password: String) = error("")
        override suspend fun signIn(email: String, password: String) = error("")
        override suspend fun signOut() {}
        override suspend fun account() = error("")
        override suspend fun sendConfirmationCode() {}
        override suspend fun confirmEmail(code: String) {}
        override suspend fun forgotPassword(email: String) {}
        override suspend fun resetPassword(email: String, code: String, password: String) {}
        override suspend fun devices() = emptyList<SignedInDevice>()
        override suspend fun signOutDevice(id: String) {}
        override suspend fun deleteAccount() {}
        override suspend fun reportPurchase(receipt: StoreReceipt) {}
    }

    private class Settings : SettingsRepository {
        val state = MutableStateFlow(UserSettings())
        override val settings: StateFlow<UserSettings> = state
        override suspend fun update(transform: (UserSettings) -> UserSettings) { state.value = transform(state.value) }
    }

    private class Env(scope: TestScope) {
        val sync = Sync()
        val accounts = Accounts()
        val settings = Settings()
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        val clock = object : Clock { override fun now() = Instant.fromEpochMilliseconds(scope.testScheduler.currentTime) }
        val auto = AutoSync(sync, accounts, settings, changes, clock, quiet = 30.seconds, every = 10.minutes)
    }

    @Test
    fun itSyncsAtStartAfterChangesAndEveryWhile() = runTest {
        val env = Env(this)
        env.auto.start(backgroundScope)
        runCurrent()
        assertEquals(1, env.sync.syncs, "at start")
        assertTrue(env.auto.status.value.lastSynced != null)

        advanceTimeBy(5.seconds)
        repeat(3) {
            env.changes.emit(Unit)
            advanceTimeBy(10.seconds)
        }
        assertEquals(1, env.sync.syncs, "not while the reader is still changing things")
        advanceTimeBy(31.seconds)
        assertEquals(2, env.sync.syncs, "after a quiet spell")

        advanceTimeBy(10.minutes)
        assertEquals(3, env.sync.syncs, "and every while")

        env.auto.request()
        runCurrent()
        assertEquals(4, env.sync.syncs, "when brought back")
        coroutineContext.cancelChildren()
    }

    @Test
    fun itWaitsWhenTurnedOffSignedOutOrTwoLibrariesWouldCombine() = runTest {
        val env = Env(this)
        env.settings.state.value = UserSettings(autoSync = false)
        env.auto.start(backgroundScope)
        runCurrent()
        assertEquals(0, env.sync.syncs)

        env.settings.state.value = UserSettings(autoSync = true)
        env.accounts.signedIn.value = null
        env.auto.request()
        runCurrent()
        assertEquals(0, env.sync.syncs)

        env.sync.combining = true
        env.accounts.signedIn.value = "reader@example.com"
        runCurrent()
        assertEquals(0, env.sync.syncs, "combining two libraries is the reader's choice")
        assertTrue(env.auto.status.value.waitingToCombine)

        assertEquals(SyncResult.NeedsCombining, env.auto.syncNow())
        assertTrue(env.auto.syncNow(combine = true) is SyncResult.Done)
        assertEquals(1, env.sync.syncs)
    }

    @Test
    fun aRefusalStopsItUntilAnotherAccountSignsIn() = runTest {
        val env = Env(this)
        env.sync.failWith = AccountProblem.SUBSCRIPTION_REQUIRED
        env.auto.start(backgroundScope)
        runCurrent()
        assertEquals(AccountProblem.SUBSCRIPTION_REQUIRED, env.auto.status.value.problem)

        env.sync.failWith = null
        env.auto.request()
        advanceTimeBy(10.minutes + 1.seconds)
        assertEquals(0, env.sync.syncs, "not asked again for the same account")

        env.accounts.signedIn.value = "other@example.com"
        runCurrent()
        assertEquals(1, env.sync.syncs)
    }
}
