package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.ui.components.APP_NAME
import com.idiomiko.core.ui.components.AppTextField
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ConfirmDialog
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/**
 * Settings: the Idiomiko account this device is signed in to. Signed out, it signs in or
 * makes an account; signed in, it shows the profile and subscription, how syncing stands, signs
 * out or deletes the account, and asks whether to combine two libraries when a sync finds another
 * one in the account.
 */
@Composable
fun AccountScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    // Back from the checkout in the browser, or from a store, the plan may have changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reload() }
    Scaffold(
        topBar = { AppTopBar(title = if (compact) tr("Account") else APP_NAME, onNavigate = onNavigate, onBack = onBack.takeIf { compact }, section = NavSection.ACCOUNT) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        // The account is reached from the account menu, not among the settings: no settings sidebar beside it.
        val content: @Composable () -> Unit = { AccountContent(state, viewModel, onBack) }
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
internal fun AccountContent(state: AccountUiState, actions: AccountViewModel?, onBack: () -> Unit) {
    ScreenHeader(tr("Account"), null)
    // Signed out, a problem is said in the form, beside the fields it is about.
    if (!state.signedOut) state.problem?.let { ProblemNotice(it, state.problemText) }
    state.done?.let { DoneNotice(it) }
    when {
        !state.available -> NotSetUpCard()
        state.signedOut -> SignedOutCard(state, actions)
        else -> {
            SignedInCards(state, actions)
            CombineQuestion(state, actions)
        }
    }
}

@Composable
private fun NotSetUpCard() {
    Notice(
        tr("Accounts are not available yet"),
        message = tr("This version of the app has no account service to sign in to."),
        icon = Icons.Default.AccountCircle,
    )
}

/** The store's plans, in the phone apps, for an account without a subscription; restoring is offered even without them. */
@Composable
internal fun StorePlans(state: AccountUiState, actions: AccountViewModel?) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        state.products.forEach { product ->
            PrimaryButton(
                "${product.title} · ${product.price}", onClick = { actions?.subscribe(product.id) }, enabled = state.busy == null,
                loading = state.busy == AccountTask.SUBSCRIBING, modifier = Modifier.testTag("account-buy-${product.id}"),
            )
        }
        if (state.products.isNotEmpty()) {
            Text(tr("Renews automatically until you cancel it in the store's subscriptions."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QuietButton(tr("Restore purchases"), onClick = { actions?.restorePurchases() }, enabled = state.busy == null, modifier = Modifier.testTag("account-restore-purchases"))
    }
}

@Composable
internal fun planText(state: AccountUiState): String {
    val plan = state.account?.plan ?: return tr("Loading…")
    return when {
        !plan.active -> tr("Sync is not active on this account.")
        plan.activeUntil != null -> tr("Sync is active until {0}.", day(plan.activeUntil!!))
        else -> tr("Sync is active.")
    }
}

@Composable
internal fun ConfirmEmail(state: AccountUiState, actions: AccountViewModel?) {
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

/** Asked when a sync found another library in the account: only the reader can choose to combine them. */
@Composable
private fun CombineQuestion(state: AccountUiState, actions: AccountViewModel?) {
    if (!state.confirmCombine) return
    ConfirmDialog(
        title = tr("Combine the libraries?"),
        text = tr("Your account already holds a library from another device. The words, texts and progress of both are kept; where the same thing was changed on both, the later change stays."),
        confirmLabel = tr("Combine"),
        onConfirm = { actions?.syncNow(combine = true) },
        onDismiss = { actions?.cancelCombine() },
    )
}

@Composable
internal fun ProblemNotice(problem: AccountProblem, text: String?) {
    Notice(
        problemTitle(problem, text),
        message = problemMessage(problem, text),
        icon = Icons.Default.Warning,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("account-problem"),
    )
}

/** What went wrong with the account, in a line. */
internal fun problemTitle(problem: AccountProblem, text: String?): String = when (problem) {
    AccountProblem.NOT_SET_UP -> tr("Accounts are not available yet")
    AccountProblem.OFFLINE -> tr("The account service cannot be reached")
    AccountProblem.EMAIL_TAKEN -> tr("An account with this email exists already")
    AccountProblem.BAD_CREDENTIALS -> tr("Wrong email or password")
    AccountProblem.WRONG_CODE -> tr("That code is wrong or has expired")
    AccountProblem.SIGNED_OUT -> tr("This device was signed out")
    AccountProblem.SUBSCRIPTION_REQUIRED -> tr("This needs an active subscription")
    AccountProblem.BUSY -> tr("Too many tries for now; wait a minute and try again")
    AccountProblem.TOO_MANY_CODES -> tr("Several codes have gone to this address already; check the inbox and spam folder, or try again in an hour")
    AccountProblem.NOT_FOUND -> tr("That is no longer in your account")
    AccountProblem.INVALID -> text ?: tr("That was not accepted")
    AccountProblem.SERVER -> tr("Something went wrong on the account service; try again later")
}

internal fun problemMessage(problem: AccountProblem, text: String?): String? =
    if (problem == AccountProblem.OFFLINE) listOfNotNull(tr("Check the internet connection and try again."), text).joinToString("\n") else null

@Composable
private fun DoneNotice(done: AccountDone) {
    Notice(
        when (done) {
            AccountDone.EMAIL_CONFIRMED -> tr("Your email is confirmed")
            AccountDone.CODE_SENT -> tr("A new code is on its way")
            AccountDone.PASSWORD_RESET -> tr("Your new password is set")
            AccountDone.SUBSCRIBED -> tr("Thank you: sync is on")
            AccountDone.PURCHASES_RESTORED -> tr("Your purchases are restored")
            AccountDone.SYNCED -> tr("This device is in sync")
            AccountDone.ACCOUNT_DELETED -> tr("Your account is deleted")
        },
        icon = Icons.Default.CheckCircle,
        tint = StatusTints.ok,
        modifier = Modifier.testTag("account-done"),
    )
}

private fun day(at: Instant): String = at.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

/** Wide enough for the sign-in form beside what an account gives, and for the profile beside the sync card. */
private val PAGE_WIDTH = AppSizes.contentWidth

/** The sign-in, the account card's place, shown while this device has an account service and is signed in to none. */
private val AccountUiState.signedOut: Boolean get() = available && signedIn == null

/** Wide enough for a six-digit code. */
private val CODE_FIELD_WIDTH = 160.dp
