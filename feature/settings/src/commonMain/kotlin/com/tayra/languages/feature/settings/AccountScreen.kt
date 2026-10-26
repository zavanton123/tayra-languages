package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.ui.components.APP_NAME
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/**
 * Settings: the Tayra Languages account this device is signed in to. Signed out, it signs in or
 * makes an account; signed in, it shows the subscription, keeps library backups in the cloud and
 * takes them back, lists the devices signed in, and signs out or deletes the account.
 */
@Composable
fun AccountScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, onRestored: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = if (compact) tr("Account") else APP_NAME, onNavigate = onNavigate, onBack = onBack.takeIf { compact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        val content: @Composable () -> Unit = { AccountContent(state, viewModel, onBack, onRestored) }
        if (compact) {
            ScrollColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) { content() }
        } else {
            PageColumn(padding) { Column(Modifier.widthIn(max = PAGE_WIDTH), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) { content() } }
        }
    }
}

@Composable
internal fun AccountContent(state: AccountUiState, actions: AccountViewModel?, onBack: () -> Unit, onRestored: () -> Unit) {
    ScreenHeader(tr("Account"), tr("Keep your library in the cloud and on all your devices."), onBackToSettings = onBack)
    state.problem?.let { ProblemNotice(it, state.problemText) }
    state.done?.let { DoneNotice(it) }
    when {
        !state.available -> NotSetUpCard()
        state.signedIn == null && state.resetSentTo != null -> ResetCard(state, actions)
        state.signedIn == null -> SignInCard(state, actions)
        else -> {
            AccountCard(state, actions)
            SyncCard(state, actions)
            CloudBackupsCard(state, actions, onRestored)
            DevicesCard(state, actions)
            DeleteAccountCard(state, actions)
        }
    }
    ServerRow(state, actions)
}

@Composable
private fun NotSetUpCard() {
    Notice(
        tr("Accounts are not available yet"),
        message = tr("This version of the app has no account service to sign in to."),
        icon = Icons.Default.AccountCircle,
    )
}

@Composable
private fun SignInCard(state: AccountUiState, actions: AccountViewModel?) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val busy = state.busy != null
    ContentCard(
        if (creating) tr("Create an account") else tr("Sign in"),
        subtitle = tr("With an account, your library can be backed up to the cloud and taken to your other devices."),
        icon = Icons.Default.AccountCircle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            AppTextField(
                email, { email = it }, Modifier.fillMaxWidth().testTag("account-email"),
                label = { Text(tr("Email")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            AppTextField(
                password, { password = it }, Modifier.fillMaxWidth().testTag("account-password"),
                label = { Text(tr("Password")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = if (creating) ({ Text(tr("At least 8 characters")) }) else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                PrimaryButton(
                    if (creating) tr("Create account") else tr("Sign in"),
                    onClick = { if (creating) actions?.register(email, password) else actions?.signIn(email, password) },
                    enabled = email.isNotBlank() && password.isNotEmpty() && !busy,
                    loading = state.busy == AccountTask.SIGNING_IN,
                    modifier = Modifier.testTag("account-submit"),
                )
                QuietButton(
                    if (creating) tr("I have an account") else tr("Create an account"),
                    onClick = { creating = !creating },
                    enabled = !busy,
                    modifier = Modifier.testTag("account-switch"),
                )
            }
            if (!creating) {
                QuietButton(tr("Forgot your password?"), onClick = { actions?.forgotPassword(email) }, enabled = email.isNotBlank() && !busy, modifier = Modifier.testTag("account-forgot"))
            }
        }
    }
}

/** After a reset code was sent: the code and a new password. */
@Composable
private fun ResetCard(state: AccountUiState, actions: AccountViewModel?) {
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    ContentCard(tr("Choose a new password"), subtitle = tr("We sent a code to {0}. Enter it with your new password.", state.resetSentTo.orEmpty()), icon = Icons.Default.AccountCircle) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            AppTextField(code, { code = it }, Modifier.fillMaxWidth().testTag("reset-code"), label = { Text(tr("Code")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            AppTextField(
                password, { password = it }, Modifier.fillMaxWidth().testTag("reset-password"), label = { Text(tr("New password")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), supportingText = { Text(tr("At least 8 characters")) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                PrimaryButton(tr("Set password and sign in"), onClick = { actions?.resetPassword(code, password) }, enabled = code.isNotBlank() && password.length >= 8 && state.busy == null, loading = state.busy != null)
                QuietButton(tr("Cancel"), onClick = { actions?.cancelReset() })
            }
        }
    }
}

@Composable
private fun AccountCard(state: AccountUiState, actions: AccountViewModel?) {
    val account = state.account
    ContentCard(state.signedIn.orEmpty(), subtitle = planText(state), icon = Icons.Default.AccountCircle, modifier = Modifier.testTag("account-card")) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            if (account != null && !account.emailVerified) ConfirmEmail(state, actions)
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SecondaryButton(tr("Sign out"), onClick = { actions?.signOut() }, enabled = state.busy == null, modifier = Modifier.testTag("account-sign-out"))
            }
        }
    }
}

@Composable
private fun planText(state: AccountUiState): String {
    val plan = state.account?.plan ?: return tr("Loading…")
    return when {
        !plan.active -> tr("Cloud backups and sync are not active on this account.")
        plan.activeUntil != null -> tr("Cloud backups and sync are active until {0}.", day(plan.activeUntil!!))
        else -> tr("Cloud backups and sync are active.")
    }
}

@Composable
private fun ConfirmEmail(state: AccountUiState, actions: AccountViewModel?) {
    var code by rememberSaveable { mutableStateOf("") }
    Notice(tr("Confirm your email"), message = tr("Enter the code we sent to your email."), icon = Icons.Default.Warning, tint = MaterialTheme.colorScheme.tertiary) {
        // The code and its button on one line, asking again below: a phone has no room for all three.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            AppTextField(code, { code = it }, Modifier.widthIn(max = CODE_FIELD_WIDTH).testTag("confirm-code"), placeholder = { Text("123456") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            PrimaryButton(tr("Confirm"), onClick = { actions?.confirm(code) }, enabled = code.isNotBlank() && state.busy == null, loading = state.busy == AccountTask.CONFIRMING)
        }
        QuietButton(tr("Send a new code"), onClick = { actions?.sendCode() }, enabled = state.busy == null)
    }
}

@Composable
private fun SyncCard(state: AccountUiState, actions: AccountViewModel?) {
    val active = state.account?.plan?.active == true
    ContentCard(
        tr("Sync"),
        subtitle = if (active) tr("Your words, flashcards and practice, the same on all your devices.") else tr("Syncing needs an active subscription."),
        icon = Icons.Default.Refresh,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            PrimaryButton(
                tr("Sync now"), onClick = { actions?.syncNow() }, enabled = active && state.busy == null,
                loading = state.busy == AccountTask.SYNCING, modifier = Modifier.testTag("account-sync"),
            )
            state.synced?.let { done ->
                Text(
                    tr("Sent: {0} · Taken from other devices: {1}", done.sent, done.changed),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("account-synced"),
                )
            }
        }
    }
}

@Composable
private fun CloudBackupsCard(state: AccountUiState, actions: AccountViewModel?, onRestored: () -> Unit) {
    var restoring by remember { mutableStateOf<CloudBackupInfo?>(null) }
    var deleting by remember { mutableStateOf<CloudBackupInfo?>(null) }
    val active = state.account?.plan?.active == true
    ContentCard(
        tr("Cloud backups"),
        subtitle = if (active) tr("A copy of your library, vocabulary, progress and settings, kept in your account.") else tr("Backing up to the cloud needs an active subscription. Backups made before stay yours to restore."),
        icon = AppIcons.UploadFile,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            PrimaryButton(
                tr("Back up now"), onClick = { actions?.backUp() }, enabled = active && state.busy == null,
                loading = state.busy == AccountTask.BACKING_UP, modifier = Modifier.testTag("cloud-back-up"),
            )
            if (state.cloudBackups.isEmpty()) {
                Text(tr("No backups in the cloud yet."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.cloudBackups.forEachIndexed { index, backup ->
                if (index > 0) AppDivider()
                Row(Modifier.fillMaxWidth().testTag("cloud-backup-$index"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Column(Modifier.weight(1f)) {
                        Text(when_(backup.createdAt), style = MaterialTheme.typography.titleSmall)
                        Text(tr("From {0} · {1}", backup.deviceName.ifBlank { "?" }, formatSize(backup.sizeBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    QuietButton(tr("Restore"), onClick = { restoring = backup }, enabled = state.busy == null, modifier = Modifier.testTag("cloud-restore-$index"))
                    QuietButton(tr("Delete"), onClick = { deleting = backup }, enabled = state.busy == null, tone = ButtonTone.Danger)
                }
            }
        }
    }
    restoring?.let { backup ->
        ConfirmDialog(
            title = tr("Restore this backup?"),
            text = tr("Your library, vocabulary, progress and settings are replaced with the backup of {0}. What you have now is backed up on this device first, so you can go back.", when_(backup.createdAt)),
            confirmLabel = tr("Restore"),
            onConfirm = { restoring = null; actions?.restore(backup.id, onRestored) },
            onDismiss = { restoring = null },
        )
    }
    deleting?.let { backup ->
        ConfirmDialog(
            title = tr("Delete this backup?"),
            text = tr("The backup of {0} is removed from your account.", when_(backup.createdAt)),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { deleting = null; actions?.deleteCloudBackup(backup.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun DevicesCard(state: AccountUiState, actions: AccountViewModel?) {
    if (state.devices.isEmpty()) return
    ContentCard(tr("Devices"), subtitle = tr("The devices signed in to your account."), icon = Icons.Default.Phone) {
        state.devices.forEachIndexed { index, device ->
            if (index > 0) AppDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Column(Modifier.weight(1f)) {
                    Text(device.name.ifBlank { device.platform }, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (device.current) tr("This device") else tr("Last used {0}", when_(device.lastUsedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.current) StatusTints.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!device.current) QuietButton(tr("Sign out"), onClick = { actions?.signOutDevice(device.id) }, enabled = state.busy == null)
            }
        }
    }
}

@Composable
private fun DeleteAccountCard(state: AccountUiState, actions: AccountViewModel?) {
    var confirming by remember { mutableStateOf(false) }
    ContentCard(tr("Delete account"), subtitle = tr("Removes the account, its cloud backups and everything kept with it. The library on this device stays."), icon = Icons.Default.Warning) {
        SecondaryButton(tr("Delete account"), onClick = { confirming = true }, enabled = state.busy == null, tone = ButtonTone.Danger, modifier = Modifier.testTag("account-delete"))
    }
    if (confirming) {
        ConfirmDialog(
            title = tr("Delete your account?"),
            text = tr("Your account, its cloud backups and everything kept with it are deleted for good. This device is signed out; its library stays."),
            confirmLabel = tr("Delete account"),
            destructive = true,
            onConfirm = { confirming = false; actions?.deleteAccount() },
            onDismiss = { confirming = false },
        )
    }
}

/** The account service in use, changeable for one run locally while the apps ship without one. */
@Composable
private fun ServerRow(state: AccountUiState, actions: AccountViewModel?) {
    var editing by remember { mutableStateOf(false) }
    QuietButton(
        if (state.server.isBlank()) tr("Account service: the app's own") else tr("Account service: {0}", state.server),
        onClick = { editing = true },
        modifier = Modifier.testTag("account-server"),
    )
    if (editing) {
        // Not the shared text dialog: an empty address is a choice here, the app's own service.
        var value by remember { mutableStateOf(state.server) }
        AppDialog(
            onDismissRequest = { editing = false },
            title = { Text(tr("Account service")) },
            text = {
                AppTextField(value, { value = it }, Modifier.testTag("account-server-field"), label = { Text(tr("Address, such as http://localhost:8080; empty for the app's own")) }, singleLine = true)
            },
            confirmButton = { QuietButton(tr("Use"), onClick = { editing = false; actions?.setServer(value) }) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = { editing = false }) },
        )
    }
}

@Composable
private fun ProblemNotice(problem: AccountProblem, text: String?) {
    Notice(
        when (problem) {
            AccountProblem.NOT_SET_UP -> tr("Accounts are not available yet")
            AccountProblem.OFFLINE -> tr("The account service cannot be reached")
            AccountProblem.EMAIL_TAKEN -> tr("An account with this email exists already")
            AccountProblem.BAD_CREDENTIALS -> tr("Wrong email or password")
            AccountProblem.WRONG_CODE -> tr("That code is wrong or has expired")
            AccountProblem.SIGNED_OUT -> tr("This device was signed out")
            AccountProblem.SUBSCRIPTION_REQUIRED -> tr("This needs an active subscription")
            AccountProblem.TOO_LARGE -> tr("The library is too large to back up to the cloud")
            AccountProblem.NOT_FOUND -> tr("That is no longer in your account")
            AccountProblem.INVALID -> text ?: tr("That was not accepted")
            AccountProblem.SERVER -> tr("Something went wrong on the account service; try again later")
        },
        message = if (problem == AccountProblem.OFFLINE) listOfNotNull(tr("Check the internet connection and try again."), text).joinToString("\n") else null,
        icon = Icons.Default.Warning,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("account-problem"),
    )
}

@Composable
private fun DoneNotice(done: AccountDone) {
    Notice(
        when (done) {
            AccountDone.EMAIL_CONFIRMED -> tr("Your email is confirmed")
            AccountDone.CODE_SENT -> tr("A new code is on its way")
            AccountDone.PASSWORD_RESET -> tr("Your new password is set")
            AccountDone.SYNCED -> tr("This device is in sync")
            AccountDone.BACKED_UP -> tr("Your library is backed up to the cloud")
            AccountDone.RESTORED -> tr("The backup is restored")
            AccountDone.ACCOUNT_DELETED -> tr("Your account is deleted")
        },
        icon = Icons.Default.CheckCircle,
        tint = StatusTints.ok,
        modifier = Modifier.testTag("account-done"),
    )
}

/** A moment as a reader reads it: the day and time, in the device's time zone. */
private fun when_(at: Instant): String {
    val t = at.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.date} ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

private fun day(at: Instant): String = at.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private val PAGE_WIDTH = 880.dp

/** Wide enough for a six-digit code. */
private val CODE_FIELD_WIDTH = 160.dp
