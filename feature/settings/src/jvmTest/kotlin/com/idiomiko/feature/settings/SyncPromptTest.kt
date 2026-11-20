package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.SyncRunner
import com.idiomiko.core.domain.account.LibrarySync
import com.idiomiko.core.domain.account.SignInClients
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.domain.account.StoreReceipt
import com.idiomiko.core.domain.account.SyncPhase
import com.idiomiko.core.domain.account.SyncProgress
import com.idiomiko.core.domain.account.SyncSummary
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LocalSyncEntry
import com.idiomiko.core.ui.components.PullToSync
import com.idiomiko.core.ui.components.SyncEntry
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

/** Syncing from the wide bar's button and a phone page's pull: asked first, then the progress and what it did. */
@OptIn(ExperimentalTestApi::class)
class SyncPromptTest {

    private class Accounts(email: String?, override val available: Boolean = true) : AccountRepository {
        override val signedIn = MutableStateFlow(email)
        override suspend fun register(email: String, password: String): AccountInfo = error("not here")
        override suspend fun signIn(email: String, password: String): AccountInfo = error("not here")
        override suspend fun signOut() { signedIn.value = null }
        override suspend fun account(): AccountInfo = error("not here")
        override suspend fun sendConfirmationCode() {}
        override suspend fun confirmEmail(code: String) {}
        override suspend fun forgotPassword(email: String) {}
        override suspend fun resetPassword(email: String, code: String, password: String) {}
        override suspend fun devices() = emptyList<com.idiomiko.core.domain.account.SignedInDevice>()
        override suspend fun signOutDevice(id: String) {}
        override suspend fun deleteAccount() {}
        override suspend fun reportPurchase(receipt: StoreReceipt) {}
        override suspend fun signInClients() = SignInClients()
        override suspend fun signInWith(provider: SignInProvider, idToken: String): AccountInfo = error("not here")
    }

    private class Sync(var combining: Boolean = false, var failing: Boolean = false, val hold: CompletableDeferred<Unit>? = null) : LibrarySync {
        var syncs = 0
        override suspend fun syncNow(progress: (SyncProgress) -> Unit): SyncSummary {
            progress(SyncProgress(SyncPhase.SENDING, 12_000, 47_480))
            hold?.await()
            if (failing) throw AccountException(AccountProblem.OFFLINE, "No connection")
            syncs++
            return SyncSummary(sent = 3, received = 5, changed = 2)
        }
        override suspend fun needsCombining() = combining
    }

    /** The account pages opened. */
    private var accountOpened = 0

    /** The wide bar over a page, with the dialog hosted as the app hosts it. */
    private fun ComposeUiTest.showBar(accounts: AccountRepository, sync: LibrarySync): SyncPromptViewModel {
        val viewModel = SyncPromptViewModel(SyncRunner(sync), accounts)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            IdiomikoTheme {
                CompositionLocalProvider(LocalSyncEntry provides SyncEntry(state.running, state.lastSynced, state.lastFailed, viewModel::open)) {
                    AppTopBar(title = "Home", onNavigate = {})
                }
                SyncPrompt(viewModel, onOpenAccount = { accountOpened++ })
            }
        }
        return viewModel
    }

    @Test
    fun theBarsButtonAsksThenSyncsAndSaysWhatItDid() = runDesktopComposeUiTest(width = 1600, height = 900) {
        val sync = Sync()
        showBar(Accounts("reader@example.com"), sync)
        onNodeWithTag("sync-dialog").assertDoesNotExist()
        onNodeWithTag("sync-button").performClick()
        onNodeWithText("Sync your library?").assertExists()
        assertEquals(0, sync.syncs, "nothing syncs before the answer")
        onNodeWithTag("sync-confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sync-summary").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, sync.syncs)
        onNodeWithText("This device is in sync").assertExists()
        onNodeWithText("Sent: 3 · Taken from other devices: 2").assertExists()
        onNodeWithText("OK").performClick()
        onNodeWithTag("sync-dialog").assertDoesNotExist()
        // The bar says when the device last synced.
        onNodeWithContentDescription("Synced just now").assertExists()
    }

    @Test
    fun theProgressShowsWhileItSyncs() = runDesktopComposeUiTest(width = 1600, height = 900) {
        val hold = CompletableDeferred<Unit>()
        showBar(Accounts("reader@example.com"), Sync(hold = hold))
        onNodeWithTag("sync-button").performClick()
        onNodeWithTag("sync-confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sync-progress").fetchSemanticsNodes().isNotEmpty() }
        // In the dialog, and as the bar's status.
        assertEquals(1, onAllNodesWithText("Syncing…").fetchSemanticsNodes().size)
        onNodeWithContentDescription("Syncing…").assertExists()
        onNodeWithText("Sending 12,000 of 47,480").assertExists()
        hold.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sync-summary").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun signedOutItLeadsToTheAccountPage() = runDesktopComposeUiTest(width = 1600, height = 900) {
        val sync = Sync()
        showBar(Accounts(null), sync)
        onNodeWithTag("sync-button").performClick()
        onNodeWithText("Sign in to your account to sync your library with your other devices.").assertExists()
        onNodeWithTag("sync-confirm").assertDoesNotExist()
        onNodeWithText("Open Account").performClick()
        waitForIdle()
        assertEquals(1, accountOpened)
        assertEquals(0, sync.syncs)
        onNodeWithTag("sync-dialog").assertDoesNotExist()
    }

    @Test
    fun twoLibrariesAreCombinedOnlyFromTheAccountPage() = runDesktopComposeUiTest(width = 1600, height = 900) {
        val sync = Sync(combining = true)
        showBar(Accounts("reader@example.com"), sync)
        onNodeWithTag("sync-button").performClick()
        onNodeWithTag("sync-confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { accountOpened == 1 }
        assertEquals(0, sync.syncs, "nothing is combined before the answer")
        onNodeWithTag("sync-dialog").assertDoesNotExist()
    }

    @Test
    fun aSyncThatFailsSaysWhyAndTriesAgain() = runDesktopComposeUiTest(width = 1600, height = 900) {
        val sync = Sync(failing = true)
        showBar(Accounts("reader@example.com"), sync)
        onNodeWithTag("sync-button").performClick()
        onNodeWithTag("sync-confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sync-problem").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("The account service cannot be reached").assertExists()
        sync.failing = false
        onNodeWithText("Try again").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sync-summary").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, sync.syncs)
    }

    @Test
    fun withoutAServiceTheBarHasNoButton() = runDesktopComposeUiTest(width = 1600, height = 900) {
        setContent { IdiomikoTheme { AppTopBar(title = "Home", onNavigate = {}) } }
        onNodeWithTag("sync-button").assertDoesNotExist()
    }

    @Test
    fun pullingAPhonePageDownAsksToSync() = runDesktopComposeUiTest(width = 400, height = 800) {
        val sync = Sync()
        val accounts = Accounts("reader@example.com")
        val viewModel = SyncPromptViewModel(SyncRunner(sync), accounts)
        setContent {
            IdiomikoTheme {
                PullToSync(enabled = true, onPull = viewModel::open, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("page")) {
                        repeat(40) { Text("Row $it", Modifier.height(40.dp)) }
                    }
                }
                SyncPrompt(viewModel, onOpenAccount = {})
            }
        }
        onNodeWithTag("page").performTouchInput { swipeDown(startY = top + 10f, endY = bottom, durationMillis = 400) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Sync your library?").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, sync.syncs, "a pull only asks")
    }
}
