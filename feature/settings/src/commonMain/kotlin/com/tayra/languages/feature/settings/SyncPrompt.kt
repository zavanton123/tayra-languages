package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.AccountRepository
import com.tayra.languages.core.domain.account.AutoSync
import com.tayra.languages.core.domain.account.SyncProgress
import com.tayra.languages.core.domain.account.SyncResult
import com.tayra.languages.core.domain.account.SyncSummary
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SyncStep { ASK, SYNCING, DONE, FAILED }

/** The question whether to sync now, asked from the app bar or by pulling a phone page down, and how that sync went. */
data class SyncPromptState(
    val open: Boolean = false,
    /** Whether this build can reach an account service. */
    val available: Boolean = false,
    val signedIn: Boolean = false,
    val step: SyncStep = SyncStep.ASK,
    /** A sync is under way, on its own or asked. */
    val running: Boolean = false,
    val progress: SyncProgress? = null,
    val summary: SyncSummary? = null,
    val problem: AccountProblem? = null,
    val problemText: String? = null,
    /** The account holds another library: the account page asks whether to combine them. */
    val combine: Boolean = false,
    /** The sync finished after the dialog was closed, to be said once. */
    val doneUnseen: Boolean = false,
)

class SyncPromptViewModel(private val sync: AutoSync, private val accounts: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(SyncPromptState(available = accounts.available, signedIn = accounts.signedIn.value != null))
    val state: StateFlow<SyncPromptState> = _state.asStateFlow()

    init {
        viewModelScope.launch { sync.status.collect { s -> _state.update { it.copy(running = s.running, progress = s.progress) } } }
        viewModelScope.launch { accounts.signedIn.collect { email -> _state.update { it.copy(signedIn = email != null) } } }
    }

    /** Asks again, or shows the sync this dialog started while it still runs. */
    fun open() = _state.update {
        if (it.step == SyncStep.SYNCING) it.copy(open = true)
        else it.copy(open = true, step = SyncStep.ASK, summary = null, problem = null, problemText = null, doneUnseen = false)
    }

    /** Closes the dialog; a sync under way goes on. */
    fun close() = _state.update { it.copy(open = false) }

    fun syncNow() {
        if (_state.value.step == SyncStep.SYNCING) return
        _state.update { it.copy(step = SyncStep.SYNCING, summary = null, problem = null, problemText = null) }
        viewModelScope.launch {
            try {
                when (val result = sync.syncNow()) {
                    // Only the reader can choose to combine two libraries, which the account page asks.
                    SyncResult.NeedsCombining -> _state.update { it.copy(step = SyncStep.ASK, combine = it.open, open = false) }
                    is SyncResult.Done -> _state.update { it.copy(step = SyncStep.DONE, summary = result.summary, doneUnseen = !it.open) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountException) {
                _state.update { it.copy(step = SyncStep.FAILED, problem = e.problem, problemText = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(step = SyncStep.FAILED, problem = AccountProblem.SERVER, problemText = e.message) }
            }
        }
    }

    fun combineSeen() = _state.update { it.copy(combine = false) }

    fun doneSeen() = _state.update { it.copy(doneUnseen = false) }
}

/** The sync dialog, hosted once for the whole app; [onOpenAccount] leads to the account page. */
@Composable
fun SyncPrompt(viewModel: SyncPromptViewModel, onOpenAccount: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    LaunchedEffect(state.combine) {
        if (!state.combine) return@LaunchedEffect
        viewModel.combineSeen()
        onOpenAccount()
    }
    LaunchedEffect(state.doneUnseen) {
        if (!state.doneUnseen) return@LaunchedEffect
        toast.show(tr("This device is in sync"))
        viewModel.doneSeen()
    }
    ToastHost(toast)
    if (state.open) SyncDialog(state, viewModel, onOpenAccount = { viewModel.close(); onOpenAccount() })
}

@Composable
internal fun SyncDialog(state: SyncPromptState, actions: SyncPromptViewModel?, onOpenAccount: () -> Unit) {
    val ready = state.available && state.signedIn
    val close = { actions?.close(); Unit }
    AppDialog(
        onDismissRequest = close,
        modifier = Modifier.testTag("sync-dialog"),
        title = { Text(if (!ready || state.step == SyncStep.ASK) tr("Sync your library?") else tr("Sync")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                when {
                    !state.available -> Text(tr("This version of the app has no account service to sign in to."))
                    !state.signedIn -> Text(tr("Sign in to your account to sync your library with your other devices."))
                    state.step == SyncStep.ASK -> Text(tr("Your words, texts, reading and learning setup are brought up to date with your account and your other devices."))
                    state.step == SyncStep.SYNCING -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                            AppSpinner(size = SpinnerSize.Small)
                            Text(tr("Syncing…"))
                        }
                        state.progress?.let(::progressLine)?.let { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("sync-progress"))
                        }
                    }
                    state.step == SyncStep.DONE -> {
                        Text(tr("This device is in sync"))
                        state.summary?.let { done ->
                            Text(
                                tr("Sent: {0} · Taken from other devices: {1}", done.sent, done.changed),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("sync-summary"),
                            )
                        }
                    }
                    else -> {
                        val problem = state.problem ?: AccountProblem.SERVER
                        Text(problemTitle(problem, state.problemText), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("sync-problem"))
                        problemMessage(problem, state.problemText)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        },
        confirmButton = {
            when {
                !ready || state.step == SyncStep.FAILED && state.problem in ACCOUNT_PROBLEMS -> QuietButton(tr("Open Account"), onClick = onOpenAccount)
                state.step == SyncStep.ASK -> QuietButton(tr("Sync now"), onClick = { actions?.syncNow() }, modifier = Modifier.testTag("sync-confirm"))
                state.step == SyncStep.FAILED -> QuietButton(tr("Try again"), onClick = { actions?.syncNow() })
                state.step == SyncStep.DONE -> QuietButton(tr("OK"), onClick = close)
                else -> QuietButton(tr("Close"), onClick = close)
            }
        },
        dismissButton = when {
            !ready || state.step == SyncStep.ASK -> ({ QuietButton(tr("Cancel"), onClick = close) })
            state.step == SyncStep.FAILED -> ({ QuietButton(tr("Close"), onClick = close) })
            else -> null
        },
    )
}

/** Problems the account page explains and solves, rather than trying again. */
private val ACCOUNT_PROBLEMS = setOf(AccountProblem.SIGNED_OUT, AccountProblem.SUBSCRIPTION_REQUIRED, AccountProblem.NOT_SET_UP)
