package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.domain.account.CloudBackupService
import com.tayra.languages.core.domain.account.CloudBackupStore
import com.tayra.languages.core.domain.account.Plan
import com.tayra.languages.core.domain.account.SignedInDevice
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The account page signed out and in: signing in, confirming the email, backing up and restoring, the devices. */
@OptIn(ExperimentalTestApi::class)
class AccountScreenTest {

    /** One account with this password, its email not confirmed until the code 123456 is given. */
    private class FakeAccounts(override val available: Boolean = true, val active: Boolean = true) : AccountRepository, CloudBackupStore {
        override val signedIn = MutableStateFlow<String?>(null)
        var confirmed = false
        val kept = mutableListOf<CloudBackupInfo>()

        private fun info() = AccountInfo("reader@example.com", confirmed, true, emptyList(), Plan(active, Instant.parse("2027-01-31T12:00:00Z").takeIf { active }, "paddle".takeIf { active }))
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

    private fun ComposeUiTest.show(accounts: FakeAccounts, local: FakeBackups = FakeBackups(), onRestored: () -> Unit = {}): AccountViewModel {
        val viewModel = AccountViewModel(accounts, CloudBackupService(local, accounts), SettingsRepositoryImpl(MapSettings()))
        setContent { TayraTheme { ProvideWindowWidth { AccountScreen(onNavigate = {}, onBack = {}, onRestored = onRestored, viewModel = viewModel) } } }
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
        save("ACCOUNT_SCREENSHOT", "-phone")
    }
}
