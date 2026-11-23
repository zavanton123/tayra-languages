package com.idiomiko.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.SyncRunner
import com.idiomiko.core.domain.account.LibrarySync
import com.idiomiko.core.domain.account.Plan
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.SignInClients
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.domain.account.SignedInDevice
import com.idiomiko.core.domain.account.StoreProduct
import com.idiomiko.core.domain.account.StoreProducts
import com.idiomiko.core.domain.account.StorePurchases
import com.idiomiko.core.domain.account.StoreReceipt
import com.idiomiko.core.domain.account.Subscriptions
import com.idiomiko.core.domain.account.SyncProgress
import com.idiomiko.core.domain.account.SyncSummary
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The account page signed out and in: signing in, confirming the email, the plan, combining two libraries. */
@OptIn(ExperimentalTestApi::class)
class AccountScreenTest {

    /** One account with this password, its email not confirmed until the code 123456 is given. */
    private class FakeAccounts(override val available: Boolean = true, val active: Boolean = true, val checkoutUrl: String? = null, val manageUrl: String? = null) : AccountRepository {
        override val signedIn = MutableStateFlow<String?>(null)
        var confirmed = false
        val reported = mutableListOf<StoreReceipt>()

        private fun info() = AccountInfo("reader@example.com", confirmed, true, emptyList(), Plan(active, Instant.parse("2027-01-31T12:00:00Z").takeIf { active }, "paddle".takeIf { active }, checkoutUrl, manageUrl), id = "u1")
        override suspend fun register(email: String, password: String) = signIn(email, password)
        override suspend fun signIn(email: String, password: String): AccountInfo {
            if (password != "correct horse") throw AccountException(AccountProblem.BAD_CREDENTIALS, "Wrong email or password")
            signedIn.value = email
            return info()
        }
        override suspend fun signOut() { signedIn.value = null }
        override suspend fun account() = info()
        override suspend fun sendConfirmationCode() {}
        override suspend fun confirmEmail(code: String) {
            if (code != "123456") throw AccountException(AccountProblem.WRONG_CODE, "Wrong code")
            confirmed = true
        }
        override suspend fun forgotPassword(email: String) {}
        override suspend fun resetPassword(email: String, code: String, password: String) {}
        override suspend fun devices() = listOf(
            SignedInDevice("desk", "Studio Mac", "desktop", Instant.parse("2026-10-26T09:00:00Z"), current = true),
            SignedInDevice("phone", "Pixel 9", "android", Instant.parse("2026-10-25T21:30:00Z"), current = false),
        )
        override suspend fun signOutDevice(id: String) {}
        override suspend fun deleteAccount() { signedIn.value = null }
        override suspend fun reportPurchase(receipt: StoreReceipt) { reported += receipt }
        override suspend fun signInClients() = SignInClients(googleDesktop = "desktop-client")
        override suspend fun signInWith(provider: SignInProvider, idToken: String): AccountInfo {
            if (idToken != "id-token-1") throw AccountException(AccountProblem.INVALID, "Not a token")
            signedIn.value = "google-user@example.com"
            return info()
        }
    }

    /** The addresses the page opened in the browser. */
    private val opened = mutableListOf<String>()

    /** What syncs the page shown last, as asked from elsewhere in the app. */
    private lateinit var runner: SyncRunner

    /** A library that holds changes not sent yet while [unsent], until it syncs. */
    private class FakeSync(var combining: Boolean = false, unsent: Boolean = false, private val synced: Instant? = null) : LibrarySync {
        var syncs = 0
        val unsentNow = MutableStateFlow(unsent)
        override suspend fun syncNow(progress: (SyncProgress) -> Unit): SyncSummary {
            syncs++; combining = false; unsentNow.value = false; return SyncSummary(sent = 3, received = 5, changed = 2)
        }
        override suspend fun needsCombining() = combining
        override fun unsent() = unsentNow
        override fun lastSynced() = synced
    }

    private class FakeStore : StorePurchases {
        val bought = mutableListOf<Pair<String, String>>()
        override suspend fun products(ids: List<String>) = listOf(StoreProduct(StoreProducts.MONTHLY, "Monthly", "€3.99"), StoreProduct(StoreProducts.YEARLY, "Yearly", "€29.99"))
        override suspend fun buy(productId: String, accountId: String): StoreReceipt { bought += productId to accountId; return StoreReceipt.GooglePlay(productId, "token-1") }
        override suspend fun owned() = listOf<StoreReceipt>(StoreReceipt.GooglePlay(StoreProducts.YEARLY, "token-0"))
    }

    /** Google, as the desktop reaches it: the person signs in, or [cancels]. */
    private class FakeIdentity(var cancels: Boolean = false) : IdentitySignIn {
        override fun offers(clients: SignInClients) = listOfNotNull(SignInProvider.GOOGLE.takeIf { clients.googleDesktop != null })
        override suspend fun idToken(provider: SignInProvider, clients: SignInClients) = if (cancels) null else "id-token-1"
    }

    private fun ComposeUiTest.show(
        accounts: FakeAccounts, sync: LibrarySync = FakeSync(), store: StorePurchases? = null, identity: IdentitySignIn? = null, knownWords: Int? = null,
    ): AccountViewModel {
        runner = SyncRunner(sync)
        val viewModel = AccountViewModel(accounts, runner, store?.let { Subscriptions(it, accounts) }, identity, knownWords?.let { flowOf(it) } ?: emptyFlow())
        setContent {
            CompositionLocalProvider(LocalUriHandler provides object : UriHandler { override fun openUri(uri: String) { opened += uri } }) {
                IdiomikoTheme { ProvideWindowWidth { AccountScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }
            }
        }
        waitForIdle()
        return viewModel
    }

    private fun ComposeUiTest.save(env: String, suffix: String = "") {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "$suffix.png"))) }
    }

    @Test
    fun signingInWithGoogle() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts()
        val identity = FakeIdentity(cancels = true)
        show(accounts, identity = identity)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-google").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("account-apple").fetchSemanticsNodes().size, "the service takes no Apple sign-in here")
        save("ACCOUNT_SCREENSHOT", "-providers")
        onNodeWithText("Continue with Google").performClick()
        waitForIdle()
        assertEquals(null, accounts.signedIn.value, "given up in the browser")

        identity.cancels = false
        onNodeWithText("Continue with Google").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("google-user@example.com", accounts.signedIn.value)
    }

    @Test
    fun signedOutAWideWindowShowsWhatAnAccountGivesBesideTheForm() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(FakeAccounts(), identity = FakeIdentity())
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-google").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Your learning, everywhere").assertExists()
        onNodeWithText("Pick up exactly where you left off").assertExists()
        onNodeWithTag("account-mascot").assertExists()
        onNodeWithText("Welcome back").assertExists()
        onNodeWithText("or").assertExists()
        val pitch = onNodeWithTag("account-pitch").fetchSemanticsNode().boundsInRoot
        val email = onNodeWithTag("account-email").fetchSemanticsNode().boundsInRoot
        assertTrue(pitch.right <= email.left, "the form stands beside the panel")
        save("ACCOUNT_SCREENSHOT", "-signed-out-wide")

        onNodeWithTag("account-switch").performClick()
        onNodeWithText("Create your account").assertExists()
        onNodeWithText("Already have an account?").assertExists()
        onNodeWithText("At least 8 characters").assertExists()
        assertEquals(0, onAllNodesWithTag("account-forgot").fetchSemanticsNodes().size, "no reset while making an account")
        save("ACCOUNT_SCREENSHOT", "-create-account")
        onNodeWithTag("account-switch").performClick()
        onNodeWithText("Welcome back").assertExists()
    }

    @Test
    fun signedOutAMediumWindowKeepsThePanelBesideTheForm() = runDesktopComposeUiTest(width = 1000, height = 1000) {
        show(FakeAccounts(), identity = FakeIdentity())
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-google").fetchSemanticsNodes().isNotEmpty() }
        val pitch = onNodeWithTag("account-pitch").fetchSemanticsNode().boundsInRoot
        val email = onNodeWithTag("account-email").fetchSemanticsNode().boundsInRoot
        assertTrue(pitch.right <= email.left, "the form stands beside the panel")
        save("ACCOUNT_SCREENSHOT", "-signed-out-medium")
    }

    @Test
    fun signedOutAPhoneHasTheHeadlineAboveTheForm() = runDesktopComposeUiTest(width = 400, height = 1100) {
        show(FakeAccounts(), identity = FakeIdentity())
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-google").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Your learning, everywhere").assertExists()
        assertEquals(0, onAllNodesWithText("Continue on any device").fetchSemanticsNodes().size, "the phone keeps only the headline")
        val pitch = onNodeWithTag("account-pitch").fetchSemanticsNode().boundsInRoot
        val email = onNodeWithTag("account-email").fetchSemanticsNode().boundsInRoot
        assertTrue(pitch.bottom <= email.top, "the headline is above the form")
        save("ACCOUNT_SCREENSHOT", "-signed-out-phone")
    }

    @Test
    fun creatingAnAccountSignsIn() = runDesktopComposeUiTest(width = 1400, height = 1100) {
        val accounts = FakeAccounts()
        show(accounts)
        onNodeWithTag("account-switch").performClick()
        onNodeWithTag("account-email").performTextInput("new@example.com")
        onNodeWithTag("account-submit").assertIsNotEnabled()
        onNodeWithTag("account-password").performTextInput("correct horse")
        onNodeWithText("Create account").assertExists()
        onNodeWithTag("account-submit").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("new@example.com", accounts.signedIn.value)
    }

    @Test
    fun aForgottenPasswordIsResetWithTheCodeInTheForm() = runDesktopComposeUiTest(width = 1400, height = 1100) {
        val accounts = FakeAccounts()
        show(accounts)
        onNodeWithTag("account-forgot").assertIsNotEnabled()
        onNodeWithTag("account-email").performTextInput("reader@example.com")
        onNodeWithTag("account-forgot").assertIsEnabled().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Choose a new password").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("We sent a code to reader@example.com. Enter it with your new password.").assertExists()
        onNodeWithTag("account-pitch").assertExists()
        save("ACCOUNT_SCREENSHOT", "-reset")

        onNodeWithTag("reset-cancel").performClick()
        onNodeWithText("Welcome back").assertExists()
        onNodeWithTag("account-forgot").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("reset-code").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("reset-code").performTextInput("123456")
        onNodeWithTag("reset-password").performTextInput("correct horse")
        onNodeWithTag("reset-submit").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Your new password is set").assertExists()
        assertEquals("reader@example.com", accounts.signedIn.value)
    }

    @Test
    fun thePasswordCanBeShownAndHiddenAgain() = runDesktopComposeUiTest(width = 1400, height = 1100) {
        show(FakeAccounts())
        onNodeWithTag("account-password").performTextInput("correct horse")
        assertEquals("•".repeat(13), drawnPassword())
        onNodeWithContentDescription("Show password").performClick()
        assertEquals("correct horse", drawnPassword())
        onNodeWithContentDescription("Hide password").performClick()
        assertEquals("•".repeat(13), drawnPassword())
    }

    /** The password as the field draws it. */
    private fun ComposeUiTest.drawnPassword(): String {
        waitForIdle()
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithTag("account-password").fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.first().layoutInput.text.text
    }

    @Test
    fun withoutAServiceItSaysSo() = runDesktopComposeUiTest(width = 1400, height = 900) {
        show(FakeAccounts(available = false))
        onNodeWithText("Accounts are not available yet").assertExists()
        assertEquals(0, onAllNodesWithTag("account-email").fetchSemanticsNodes().size)
    }

    @Test
    fun signingInShowsThePlan() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts()
        show(accounts)
        save("ACCOUNT_SCREENSHOT", "-signed-out")
        onNodeWithTag("account-email").performTextInput("reader@example.com")
        onNodeWithTag("account-password").performTextInput("wrong one")
        onNodeWithTag("account-submit").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Wrong email or password").fetchSemanticsNodes().isNotEmpty() }

        onNodeWithTag("account-password").performTextClearance()
        onNodeWithTag("account-password").performTextInput("correct horse")
        onNodeWithTag("account-submit").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Sync is active until 2027-01-31.").assertExists()
        onNodeWithText("Pixel 9").assertDoesNotExist()

        // The email is not confirmed yet: a wrong code says so, the right one confirms it.
        onNodeWithTag("confirm-code").performTextInput("000000")
        onNodeWithText("Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("That code is wrong or has expired").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("confirm-code").performTextClearance()
        onNodeWithTag("confirm-code").performTextInput("123456")
        onNodeWithText("Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your email is confirmed").fetchSemanticsNodes().isNotEmpty() }

        assertEquals(0, onAllNodesWithText("Cloud backups").fetchSemanticsNodes().size, "the account keeps no backups")
        save("ACCOUNT_SCREENSHOT")
    }

    @Test
    fun withoutASubscriptionTheCheckoutOpensInTheBrowser() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts(active = false, checkoutUrl = "https://api.example.com/checkout?account=u1").apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-subscribe").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-subscribe").performClick()
        assertEquals(listOf("https://api.example.com/checkout?account=u1"), opened)
        onNodeWithText("The checkout opens in your browser. Come back here when it is done.").assertExists()
        assertEquals(0, onAllNodesWithTag("account-manage").fetchSemanticsNodes().size)
        save("ACCOUNT_SCREENSHOT", "-subscribe")
    }

    @Test
    fun inThePhoneAppsTheStoresPlansAreBoughtThere() = runDesktopComposeUiTest(width = 400, height = 1600) {
        val accounts = FakeAccounts(active = false).apply { signedIn.value = "reader@example.com" }
        val store = FakeStore()
        show(accounts, store = store)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-buy-${StoreProducts.YEARLY}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Yearly · €29.99").assertExists()
        onNodeWithText("Renews automatically until you cancel it in the store's subscriptions.").assertExists()
        save("ACCOUNT_SCREENSHOT", "-store")
        onNodeWithTag("account-buy-${StoreProducts.YEARLY}").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Thank you: sync is on").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf(StoreProducts.YEARLY to "u1"), store.bought)
        assertEquals(listOf<StoreReceipt>(StoreReceipt.GooglePlay(StoreProducts.YEARLY, "token-1")), accounts.reported)

        onNodeWithTag("account-restore-purchases").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your purchases are restored").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, accounts.reported.size)
    }

    @Test
    fun aStoresSubscriptionIsManagedThere() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts(manageUrl = "https://apps.apple.com/account/subscriptions").apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-manage").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-manage").performClick()
        assertEquals(listOf("https://apps.apple.com/account/subscriptions"), opened)
        assertEquals(0, onAllNodesWithTag("account-subscribe").fetchSemanticsNodes().size)
    }

    @Test
    fun aSyncAskedElsewhereThatWouldCombineAsksHere() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        val sync = FakeSync(combining = true)
        show(accounts, sync = sync)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { runner.syncNow() }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Combine the libraries?").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Combine").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("This device is in sync").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, sync.syncs)
    }

    @Test
    fun withoutASubscriptionThePlanSaysSyncIsNotActive() = runDesktopComposeUiTest(width = 400, height = 1600) {
        val accounts = FakeAccounts(active = false).apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Sync is not active on this account.").assertExists()
        save("ACCOUNT_SCREENSHOT", "-signed-in-phone")
    }

    @Test
    fun signedInThePageShowsTheProfileBesideHowSyncingStands() = runDesktopComposeUiTest(width = 1586, height = 1150) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com"; confirmed = true }
        val sync = FakeSync(synced = kotlin.time.Clock.System.now() - 12.minutes)
        show(accounts, sync = sync, knownWords = 1540)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-sign-in-methods").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-signed-in").assertExists()
        onNodeWithText("Email").assertExists()
        onNodeWithText("Everything is synced").assertExists()
        onNodeWithText("1,540 words").assertExists()
        assertEquals(2, onAllNodesWithText("Up to date").fetchSemanticsNodes().size)
        onNodeWithText("Last synced 12 minutes ago").assertExists()
        val profile = onNodeWithTag("account-card").fetchSemanticsNode().boundsInRoot
        val syncCard = onNodeWithTag("account-sync-card").fetchSemanticsNode().boundsInRoot
        assertTrue(profile.right <= syncCard.left, "the cards stand side by side")
        assertEquals(profile.height, syncCard.height, "of equal height")
        assertEquals(0, sync.syncs, "opening the page syncs nothing")
        save("ACCOUNT_SCREENSHOT", "-signed-in")
    }

    @Test
    fun syncNowSendsWhatIsNotSentYet() = runDesktopComposeUiTest(width = 1586, height = 1150) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com"; confirmed = true }
        val sync = FakeSync(unsent = true)
        show(accounts, sync = sync)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Changes not sent yet").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(3, onAllNodesWithText("Not sent yet").fetchSemanticsNodes().size)
        onNodeWithTag("account-last-synced").assertDoesNotExist()
        save("ACCOUNT_SCREENSHOT", "-signed-in-unsent")

        onNodeWithTag("account-sync-now").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Everything is synced").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, sync.syncs)
        onNodeWithText("Last synced just now").assertExists()
        onNodeWithText("This device is in sync").assertExists()
    }

    @Test
    fun signingOutShowsTheSignInAgain() = runDesktopComposeUiTest(width = 1400, height = 1150) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-sign-out").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Welcome back").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(null, accounts.signedIn.value)
    }

    @Test
    fun deletingTheAccountAsksFirst() = runDesktopComposeUiTest(width = 1400, height = 1150) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("This action cannot be undone.").assertExists()
        onNodeWithTag("account-delete").performClick()
        onNodeWithText("Delete your account?").assertExists()
        onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        assertEquals("reader@example.com", accounts.signedIn.value, "kept when not confirmed")

        onNodeWithTag("account-delete").performClick()
        onNode(hasText("Delete account") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your account is deleted").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(null, accounts.signedIn.value)
        onNodeWithText("Welcome back").assertExists()
    }
}
