package com.tayra.languages.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
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
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountInfo
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.AccountRepository
import com.tayra.languages.core.domain.account.AutoSync
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.domain.account.CloudBackupService
import com.tayra.languages.core.domain.account.CloudBackupStore
import com.tayra.languages.core.domain.account.LibrarySync
import com.tayra.languages.core.domain.account.Plan
import com.tayra.languages.core.domain.account.SignedInDevice
import com.tayra.languages.core.domain.account.StoreProduct
import com.tayra.languages.core.domain.account.StoreProducts
import com.tayra.languages.core.domain.account.StorePurchases
import com.tayra.languages.core.domain.account.StoreReceipt
import com.tayra.languages.core.domain.account.Subscriptions
import com.tayra.languages.core.domain.account.SyncSummary
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The account page signed out and in: signing in, confirming the email, backing up and restoring, the devices. */
@OptIn(ExperimentalTestApi::class)
class AccountScreenTest {

    /** One account with this password, its email not confirmed until the code 123456 is given. */
    private class FakeAccounts(override val available: Boolean = true, val active: Boolean = true, val checkoutUrl: String? = null, val manageUrl: String? = null) : AccountRepository, CloudBackupStore {
        override val signedIn = MutableStateFlow<String?>(null)
        var confirmed = false
        val kept = mutableListOf<CloudBackupInfo>()
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
        override suspend fun list() = kept.reversed()
        override suspend fun upload(name: String, content: ByteArray) =
            CloudBackupInfo("c${kept.size}", name, "Studio Mac", Instant.parse("2026-10-26T10:15:00Z"), content.size.toLong()).also { kept += it }
        override suspend fun download(id: String) = byteArrayOf(1, 2, 3)
        override suspend fun delete(id: String) { kept.removeAll { it.id == id } }
    }

    private class FakeBackups : BackupRepository {
        val restored = mutableListOf<String>()
        override suspend fun list() = emptyList<Backup>()
        override suspend fun create() = Backup("tayra-backup.sqlite", null, 2_400_000)
        override suspend fun delete(name: String) {}
        override suspend fun read(name: String) = ByteArray(2_400_000)
        override suspend fun import(file: ByteArray) = Backup("taken.sqlite", null, file.size.toLong())
        override suspend fun restore(name: String): Backup { restored += name; return Backup("before.sqlite", null, 1) }
    }

    /** The addresses the page opened in the browser. */
    private val opened = mutableListOf<String>()

    private class FakeSync(var combining: Boolean = false, var failing: Boolean = false) : LibrarySync {
        var syncs = 0
        override suspend fun syncNow(): SyncSummary {
            if (failing) throw AccountException(AccountProblem.OFFLINE, "No connection")
            syncs++; combining = false; return SyncSummary(sent = 3, received = 5, changed = 2)
        }
        override suspend fun needsCombining() = combining
    }

    private class FakeStore : StorePurchases {
        val bought = mutableListOf<Pair<String, String>>()
        override suspend fun products(ids: List<String>) = listOf(StoreProduct(StoreProducts.MONTHLY, "Monthly", "€3.99"), StoreProduct(StoreProducts.YEARLY, "Yearly", "€29.99"))
        override suspend fun buy(productId: String, accountId: String): StoreReceipt { bought += productId to accountId; return StoreReceipt.GooglePlay(productId, "token-1") }
        override suspend fun owned() = listOf<StoreReceipt>(StoreReceipt.GooglePlay(StoreProducts.YEARLY, "token-0"))
    }

    private fun ComposeUiTest.show(accounts: FakeAccounts, local: FakeBackups = FakeBackups(), sync: LibrarySync = FakeSync(), store: StorePurchases? = null, onRestored: () -> Unit = {}): AccountViewModel {
        val settings = SettingsRepositoryImpl(MapSettings())
        val viewModel = AccountViewModel(accounts, CloudBackupService(local, accounts), AutoSync(sync, accounts, settings, emptyFlow()), settings, store?.let { Subscriptions(it, accounts) })
        setContent {
            CompositionLocalProvider(LocalUriHandler provides object : UriHandler { override fun openUri(uri: String) { opened += uri } }) {
                TayraTheme { ProvideWindowWidth { AccountScreen(onNavigate = {}, onBack = {}, onRestored = onRestored, viewModel = viewModel) } }
            }
        }
        waitForIdle()
        return viewModel
    }

    private fun ComposeUiTest.save(env: String, suffix: String = "") {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "$suffix.png"))) }
    }

    @Test
    fun withoutAServiceItSaysSo() = runDesktopComposeUiTest(width = 1400, height = 900) {
        show(FakeAccounts(available = false))
        onNodeWithText("Accounts are not available yet").assertExists()
        assertEquals(0, onAllNodesWithTag("account-email").fetchSemanticsNodes().size)
    }

    @Test
    fun signingInShowsThePlanTheBackupsAndTheDevices() = runDesktopComposeUiTest(width = 1400, height = 1600) {
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
        onNodeWithText("Cloud backups and sync are active until 2027-01-31.").assertExists()
        onNodeWithText("This device").assertExists()
        onNodeWithText("Pixel 9").assertExists()

        // The email is not confirmed yet: a wrong code says so, the right one confirms it.
        onNodeWithTag("confirm-code").performTextInput("000000")
        onNodeWithText("Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("That code is wrong or has expired").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("confirm-code").performTextClearance()
        onNodeWithTag("confirm-code").performTextInput("123456")
        onNodeWithText("Confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your email is confirmed").fetchSemanticsNodes().isNotEmpty() }

        onNodeWithTag("cloud-back-up").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your library is backed up to the cloud").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("cloud-backup-0").assertExists()
        assertEquals(1, accounts.kept.size)
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
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Thank you: sync and cloud backups are on").fetchSemanticsNodes().isNotEmpty() }
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
    fun syncingSaysWhatItDid() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        val sync = FakeSync()
        show(accounts, sync = sync)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-sync").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("This device is in sync").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Sent: 3 · Taken from other devices: 2").assertExists()
        onNodeWithTag("account-last-synced").assertExists()
        assertEquals(1, sync.syncs)
        save("ACCOUNT_SCREENSHOT", "-synced")
    }

    @Test
    fun aSyncThatDidNotFinishSaysSo() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        show(accounts, sync = FakeSync(failing = true))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-sync").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-sync-problem").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theFirstSyncAsksBeforeCombiningTwoLibraries() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        val sync = FakeSync(combining = true)
        show(accounts, sync = sync)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("account-sync").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Combine the libraries?").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, sync.syncs, "nothing is combined before the answer")
        save("ACCOUNT_SCREENSHOT", "-combine")
        onNodeWithText("Combine").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("This device is in sync").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, sync.syncs)
    }

    @Test
    fun aCloudBackupIsRestoredAfterAsking() = runDesktopComposeUiTest(width = 1400, height = 1600) {
        val accounts = FakeAccounts().apply { signedIn.value = "reader@example.com" }
        val local = FakeBackups()
        var restarted = 0
        kotlinx.coroutines.runBlocking { accounts.upload("tayra-backup.sqlite", ByteArray(10)) }
        show(accounts, local, onRestored = { restarted++ })
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("cloud-restore-0").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("cloud-restore-0").performScrollTo().performClick()
        onNodeWithText("Restore this backup?").assertExists()
        onAllNodesWithText("Restore").fetchSemanticsNodes().size.let { assertEquals(true, it >= 2) }
        onAllNodesWithText("Restore")[onAllNodesWithText("Restore").fetchSemanticsNodes().size - 1].performClick()
        waitUntil(timeoutMillis = 5_000) { restarted == 1 }
        assertEquals(listOf("taken.sqlite"), local.restored, "the cloud backup is taken back and restored from")
    }

    @Test
    fun withoutASubscriptionBackingUpWaits() = runDesktopComposeUiTest(width = 400, height = 1600) {
        val accounts = FakeAccounts(active = false).apply { signedIn.value = "reader@example.com" }
        show(accounts)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Cloud backups and sync are not active on this account.").assertExists()
        onNodeWithTag("cloud-back-up").performScrollTo().assertExists()
        onNodeWithText("Syncing needs an active subscription.").assertExists()
        onNodeWithTag("account-sync").assertIsNotEnabled()
        save("ACCOUNT_SCREENSHOT", "-phone")
    }
}
