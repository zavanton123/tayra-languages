package com.tayra.languages.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.tayra.languages.core.domain.account.AccountInfo
import com.tayra.languages.core.domain.account.AccountRepository
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.domain.account.CloudBackupService
import com.tayra.languages.core.data.runtime.AppInstanceLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

// ---- Account: the Tayra Languages account this computer is signed in to, for cloud backups and sync.
// The service's address comes from TAYRA_API_URL until the apps ship with one.

fun accountCommand() = group(
    "account", "The Tayra Languages account this computer is signed in to: sign up, in and out, and what it holds",
    AccountRegister(), AccountLogin(), AccountLogout(), AccountStatus(), AccountSendCode(), AccountConfirm(), AccountDevices(), AccountDelete(),
)

fun cloudCommand() = group(
    "cloud", "Library backups kept in the account: back up, list, restore and delete them",
    CloudList(), CloudBackUp(), CloudRestore(), CloudDelete(),
)

@Serializable
data class AccountJson(val email: String, val emailVerified: Boolean, val providers: List<String>, val subscriptionActive: Boolean, val subscriptionUntil: String?, val subscriptionSource: String?)

private fun AccountInfo.toJson() = AccountJson(email, emailVerified, providers, plan.active, plan.activeUntil?.toString(), plan.source)

private fun AccountJson.text() = buildString {
    append("Signed in as $email")
    if (!emailVerified) append(" (email not confirmed)")
    append(". Cloud backups and sync: ")
    append(if (subscriptionActive) "active" + (subscriptionUntil?.let { " until $it" } ?: "") + (subscriptionSource?.let { " ($it)" } ?: "") else "not active")
    append('.')
}

private class AccountRegister : Command("register", "Make an account and sign this computer in to it; a code to confirm the email is sent") {
    private val email by option("--email", help = "The account's email").required()
    private val password by option("--password", envvar = "TAYRA_PASSWORD", help = "At least 8 characters; asked for when not given").prompt(hideInput = true, requireConfirmation = true)

    override fun run() = respond(cli, AccountJson.serializer()) {
        val account = cli.get<AccountRepository>().register(email, password).toJson()
        Output(account, account.text() + " Confirm the email with `tayra account confirm <code>`.")
    }
}

private class AccountLogin : Command("login", "Sign this computer in to an account") {
    private val email by option("--email", help = "The account's email").required()
    private val password by option("--password", envvar = "TAYRA_PASSWORD", help = "Asked for when not given").prompt(hideInput = true)

    override fun run() = respond(cli, AccountJson.serializer()) {
        val account = cli.get<AccountRepository>().signIn(email, password).toJson()
        Output(account, account.text())
    }
}

private class AccountLogout : Command("logout", "Sign this computer out; the account and its other devices are untouched") {
    override fun run() = respond(cli, DoneJson.serializer()) {
        cli.get<AccountRepository>().signOut()
        Output(DoneJson(message = "Signed out"), "Signed out.")
    }
}

private class AccountStatus : Command("status", "The account this computer is signed in to, and whether cloud backups and sync are active") {
    override fun run() = respond(cli, AccountJson.serializer()) {
        val accounts = cli.get<AccountRepository>()
        if (accounts.signedIn.value == null) notFound("Not signed in; `tayra account login --email <email>` signs in")
        val account = accounts.account().toJson()
        Output(account, account.text())
    }
}

private class AccountSendCode : Command("send-code", "Email a new code to confirm the account's address") {
    override fun run() = respond(cli, DoneJson.serializer()) {
        cli.get<AccountRepository>().sendConfirmationCode()
        Output(DoneJson(message = "Code sent"), "A code is on its way; `tayra account confirm <code>` confirms the email.")
    }
}

private class AccountConfirm : Command("confirm", "Confirm the account's email with the code sent to it") {
    private val code by argument(help = "The six-digit code from the email")

    override fun run() = respond(cli, DoneJson.serializer()) {
        cli.get<AccountRepository>().confirmEmail(code)
        Output(DoneJson(message = "Email confirmed"), "Email confirmed.")
    }
}

@Serializable
data class DeviceJson(val id: String, val name: String, val platform: String, val lastUsed: String, val current: Boolean)

private class AccountDevices : Command("devices", "The devices signed in to the account") {
    override fun run() = respond(cli, ListSerializer(DeviceJson.serializer())) {
        val devices = cli.get<AccountRepository>().devices().map { DeviceJson(it.id, it.name, it.platform, it.lastUsedAt.toString(), it.current) }
        Output(devices, table(listOf("NAME", "PLATFORM", "LAST USED", ""), devices.map { listOf(it.name, it.platform, it.lastUsed.take(16).replace('T', ' '), if (it.current) "this one" else "") }))
    }
}

private class AccountDelete : Command("delete", "Delete the account and everything the service keeps for it; the library here stays") {
    private val yes by option("--yes", help = "Confirm: the account, its cloud backups and its synced data are deleted").flag()

    override fun run() = respond(cli, DoneJson.serializer()) {
        if (!yes) invalid("Deleting the account removes its cloud backups and synced data for good; repeat with --yes")
        cli.get<AccountRepository>().deleteAccount()
        Output(DoneJson(message = "Account deleted"), "Account deleted; this computer is signed out.")
    }
}

@Serializable
data class CloudBackupJson(val id: String, val name: String, val device: String, val created: String, val sizeBytes: Long)

private fun CloudBackupInfo.toJson() = CloudBackupJson(id, name, deviceName, createdAt.toString(), sizeBytes)

private class CloudList : Command("list", "The library backups kept in the account, newest first") {
    override fun run() = respond(cli, ListSerializer(CloudBackupJson.serializer())) {
        val list = cli.get<CloudBackupService>().list().map { it.toJson() }
        Output(list, table(listOf("ID", "NAME", "FROM", "CREATED", "SIZE"), list.map { listOf(it.id, it.name, it.device, it.created.take(16).replace('T', ' '), "${it.sizeBytes / 1024} KB") }).ifEmpty { "No cloud backups." })
    }
}

private class CloudBackUp : Command("backup", "Back up the library and keep the backup in the account") {
    override fun run() = respond(cli, CloudBackupJson.serializer()) {
        val backup = cli.get<CloudBackupService>().backUp().toJson()
        Output(backup, "Kept ${backup.name} (${backup.sizeBytes / 1024} KB) in the account")
    }
}

private class CloudRestore : Command("restore", "Replace the library and settings with a cloud backup's; the library as it was is backed up here first") {
    private val id by argument(help = "Cloud backup id, from `tayra cloud list`")
    private val yes by option("--yes", help = "Confirm: the library is replaced").flag()
    private val force by option("--force", help = "Restore although the app is open; close it right after without changing anything").flag()

    override fun run() = respond(cli, BackupJson.serializer(), changes = true) {
        if (!yes) invalid("Restoring replaces the whole library and settings with the cloud backup; repeat with --yes")
        if (AppInstanceLock.isAppOpen() && !force) {
            throw CliFailure("Tayra Languages is open, and would write its own state over the restored one. Close it first, or add --force.", ExitCode.CONFLICT)
        }
        val undo = cli.get<CloudBackupService>().restore(id)
        Output(BackupJson(undo.name, undo.createdAt?.toString(), undo.sizeBytes), "Restored. The library as it was is kept as ${undo.name}.")
    }
}

private class CloudDelete : Command("delete", "Delete a backup kept in the account") {
    private val id by argument(help = "Cloud backup id, from `tayra cloud list`")

    override fun run() = respond(cli, DoneJson.serializer()) {
        cli.get<CloudBackupService>().delete(id)
        Output(DoneJson(message = "Deleted"), "Deleted.")
    }
}
