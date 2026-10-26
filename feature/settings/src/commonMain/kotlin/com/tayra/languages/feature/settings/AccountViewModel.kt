package com.tayra.languages.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountInfo
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.AccountRepository
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.domain.account.CloudBackupService
import com.tayra.languages.core.domain.account.LibrarySync
import com.tayra.languages.core.domain.account.SignedInDevice
import com.tayra.languages.core.domain.account.SyncSummary
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the account page shows. */
data class AccountUiState(
    /** Whether this build can reach an account service. */
    val available: Boolean = false,
    val server: String = "",
    /** The email of the account this device is signed in to; null when signed out. */
    val signedIn: String? = null,
    val account: AccountInfo? = null,
    val devices: List<SignedInDevice> = emptyList(),
    val cloudBackups: List<CloudBackupInfo> = emptyList(),
    /** What the last sync from this page did. */
    val synced: SyncSummary? = null,
    /** What is under way, shown with a spinner; null when nothing is. */
    val busy: AccountTask? = null,
    /** What went wrong last, until the next action. */
    val problem: AccountProblem? = null,
    val problemText: String? = null,
    /** A reset code was sent to this address; the page asks for it and a new password. */
    val resetSentTo: String? = null,
    /** The last thing done, said once. */
    val done: AccountDone? = null,
)

enum class AccountTask { SIGNING_IN, LOADING, SENDING_CODE, CONFIRMING, SYNCING, BACKING_UP, RESTORING, DELETING }

enum class AccountDone { EMAIL_CONFIRMED, CODE_SENT, PASSWORD_RESET, SYNCED, BACKED_UP, RESTORED, ACCOUNT_DELETED }

/** The account this device is signed in to, its devices, syncing with it and its cloud backups. */
class AccountViewModel(
    private val accounts: AccountRepository,
    private val cloud: CloudBackupService,
    private val sync: LibrarySync,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        reload()
    }

    /** Reads what the account holds again, when signed in. */
    fun reload() {
        _state.update { it.copy(available = accounts.available, server = settings.current.accountServer, signedIn = accounts.signedIn.value) }
        if (accounts.signedIn.value != null) work(AccountTask.LOADING) { load() }
    }

    fun signIn(email: String, password: String) = work(AccountTask.SIGNING_IN) {
        accounts.signIn(email.trim(), password)
        load()
    }

    fun register(email: String, password: String) = work(AccountTask.SIGNING_IN) {
        accounts.register(email.trim(), password)
        load()
    }

    fun signOut() = work(AccountTask.LOADING) {
        accounts.signOut()
        _state.update { AccountUiState(available = it.available, server = it.server) }
    }

    fun sendCode() = work(AccountTask.SENDING_CODE) {
        accounts.sendConfirmationCode()
        _state.update { it.copy(done = AccountDone.CODE_SENT) }
    }

    fun confirm(code: String) = work(AccountTask.CONFIRMING) {
        accounts.confirmEmail(code.trim())
        load()
        _state.update { it.copy(done = AccountDone.EMAIL_CONFIRMED) }
    }

    fun forgotPassword(email: String) = work(AccountTask.SENDING_CODE) {
        accounts.forgotPassword(email.trim())
        _state.update { it.copy(resetSentTo = email.trim()) }
    }

    fun resetPassword(code: String, password: String) = work(AccountTask.SIGNING_IN) {
        val email = _state.value.resetSentTo ?: return@work
        accounts.resetPassword(email, code.trim(), password)
        accounts.signIn(email, password)
        _state.update { it.copy(resetSentTo = null, done = AccountDone.PASSWORD_RESET) }
        load()
    }

    fun cancelReset() = _state.update { it.copy(resetSentTo = null) }

    fun signOutDevice(id: String) = work(AccountTask.LOADING) {
        accounts.signOutDevice(id)
        _state.update { it.copy(devices = accounts.devices()) }
    }

    fun syncNow() = work(AccountTask.SYNCING) {
        val done = sync.syncNow()
        _state.update { it.copy(synced = done, done = AccountDone.SYNCED) }
    }

    fun backUp() = work(AccountTask.BACKING_UP) {
        cloud.backUp()
        _state.update { it.copy(cloudBackups = cloud.list(), done = AccountDone.BACKED_UP) }
    }

    /** Replaces the library with the cloud backup; [onRestored] starts the app over on the restored library. */
    fun restore(id: String, onRestored: () -> Unit) = work(AccountTask.RESTORING) {
        cloud.restore(id)
        _state.update { it.copy(done = AccountDone.RESTORED) }
        onRestored()
    }

    fun deleteCloudBackup(id: String) = work(AccountTask.LOADING) {
        cloud.delete(id)
        _state.update { it.copy(cloudBackups = cloud.list()) }
    }

    fun deleteAccount() = work(AccountTask.DELETING) {
        accounts.deleteAccount()
        _state.update { AccountUiState(available = it.available, server = it.server, done = AccountDone.ACCOUNT_DELETED) }
    }

    /** The account service to use instead of the build's own, for one run locally; blank goes back to the build's. */
    fun setServer(url: String) {
        viewModelScope.launch {
            if (accounts.signedIn.value != null) runCatching { accounts.signOut() }
            settings.update { it.copy(accountServer = url.trim()) }
            _state.update { AccountUiState() }
            reload()
        }
    }

    fun doneSeen() = _state.update { it.copy(done = null) }

    private suspend fun load() {
        val account = accounts.account()
        val devices = accounts.devices()
        val backups = cloud.list()
        _state.update { it.copy(signedIn = accounts.signedIn.value, account = account, devices = devices, cloudBackups = backups) }
    }

    /** Runs [action] as [task]; a failure is kept for the page to say, and signing out elsewhere shows the sign-in again. */
    private fun work(task: AccountTask, action: suspend () -> Unit) {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = task, problem = null, problemText = null, done = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountException) {
                _state.update {
                    val signedOut = e.problem == AccountProblem.SIGNED_OUT
                    (if (signedOut) AccountUiState(available = it.available, server = it.server) else it).copy(problem = e.problem, problemText = e.message)
                }
            } catch (e: BackupException) {
                _state.update { it.copy(problem = AccountProblem.INVALID, problemText = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(problem = AccountProblem.SERVER, problemText = e.message) }
            } finally {
                _state.update { it.copy(busy = null) }
            }
        }
    }
}
