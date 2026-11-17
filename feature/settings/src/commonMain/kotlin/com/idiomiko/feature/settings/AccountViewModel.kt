package com.idiomiko.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.AutoSync
import com.idiomiko.core.domain.account.SyncResult
import com.idiomiko.core.domain.account.IdentityException
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.SignInClients
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.domain.account.StoreException
import com.idiomiko.core.domain.account.StoreProduct
import com.idiomiko.core.domain.account.Subscriptions
import com.idiomiko.core.domain.account.SyncProgress
import com.idiomiko.core.domain.account.SyncSummary
import com.idiomiko.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** What the account page shows. */
data class AccountUiState(
    /** Whether this build can reach an account service. */
    val available: Boolean = false,
    /** The email of the account this device is signed in to; null when signed out. */
    val signedIn: String? = null,
    val account: AccountInfo? = null,
    /** What the last sync from this page did. */
    val synced: SyncSummary? = null,
    /** The first sync would combine this device's library with the account's: the page asks first. */
    val confirmCombine: Boolean = false,
    /** When this device last synced, on its own or asked. */
    val lastSynced: Instant? = null,
    /** Why the last sync, on its own or asked, did not finish. */
    val syncProblem: AccountProblem? = null,
    /** A sync is under way, on its own or asked. */
    val syncing: Boolean = false,
    /** How far it has got; null until it says. */
    val syncProgress: SyncProgress? = null,
    /** What the store sells here, while the account has no subscription; empty without a store. */
    val products: List<StoreProduct> = emptyList(),
    /** Whether the store can restore purchases here. */
    val storeAvailable: Boolean = false,
    /** The providers this app can sign in with, as the service takes them. */
    val signInProviders: List<SignInProvider> = emptyList(),
    val autoSync: Boolean = true,
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

enum class AccountTask { SIGNING_IN, LOADING, SENDING_CODE, CONFIRMING, SUBSCRIBING, SYNCING, DELETING }

enum class AccountDone { EMAIL_CONFIRMED, CODE_SENT, PASSWORD_RESET, SUBSCRIBED, PURCHASES_RESTORED, SYNCED, ACCOUNT_DELETED }

/** The account this device is signed in to and syncing with it. */
class AccountViewModel(
    private val accounts: AccountRepository,
    private val sync: AutoSync,
    private val settings: SettingsRepository,
    /** Buying in the store, in the phone apps; null elsewhere. */
    private val subscriptions: Subscriptions? = null,
    /** Signing in with Google or Apple, where the platform can; null elsewhere. */
    private val identity: IdentitySignIn? = null,
) : ViewModel() {
    private var clients = SignInClients()

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        reload()
        viewModelScope.launch { sync.status.collect { status ->
                _state.update { it.copy(lastSynced = status.lastSynced, syncProblem = status.problem, syncing = status.running, syncProgress = status.progress) }
            } }
        viewModelScope.launch { settings.settings.collect { s -> _state.update { it.copy(autoSync = s.autoSync) } } }
        // A sync asked for elsewhere found two libraries: the question is asked here.
        viewModelScope.launch { sync.status.map { it.waitingToCombine }.distinctUntilChanged().collect { waiting -> if (waiting) _state.update { it.copy(confirmCombine = true) } } }
    }

    /** Reads what the account holds again, when signed in. */
    fun reload() {
        _state.update { it.copy(available = accounts.available, signedIn = accounts.signedIn.value) }
        if (accounts.signedIn.value != null) work(AccountTask.LOADING) { load() } else offerProviders()
    }

    /** Asks the service which providers it takes, for the sign-in to offer the ones this app can use. */
    private fun offerProviders() {
        val identity = identity ?: return
        if (!accounts.available) return
        viewModelScope.launch {
            clients = accounts.signInClients()
            _state.update { it.copy(signInProviders = identity.offers(clients)) }
        }
    }

    /** Signs in with [provider]: the platform asks the person there, and the service checks the token it gives. */
    fun signInWith(provider: SignInProvider) = work(AccountTask.SIGNING_IN) {
        val token = identity?.idToken(provider, clients) ?: return@work
        accounts.signInWith(provider, token)
        load()
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
        _state.update { AccountUiState(available = it.available, autoSync = it.autoSync) }
        offerProviders()
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

    /** Syncs, after asking when the first sync would combine two libraries; [combine] is the answer. */
    fun syncNow(combine: Boolean = false) = work(AccountTask.SYNCING) {
        when (val result = sync.syncNow(combine)) {
            SyncResult.NeedsCombining -> _state.update { it.copy(confirmCombine = true) }
            is SyncResult.Done -> _state.update { it.copy(confirmCombine = false, synced = result.summary, done = AccountDone.SYNCED) }
        }
    }

    fun setAutoSync(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(autoSync = on) } }
        if (on) sync.request()
    }

    fun cancelCombine() = _state.update { it.copy(confirmCombine = false) }

    fun deleteAccount() = work(AccountTask.DELETING) {
        accounts.deleteAccount()
        _state.update { AccountUiState(available = it.available, autoSync = it.autoSync, done = AccountDone.ACCOUNT_DELETED) }
        offerProviders()
    }

    fun doneSeen() = _state.update { it.copy(done = null) }

    private suspend fun load() {
        val account = accounts.account()
        _state.update {
            it.copy(signedIn = accounts.signedIn.value, account = account, storeAvailable = subscriptions != null)
        }
        // The store answers in its own time, and one that cannot be reached leaves the page without plans to buy.
        val store = subscriptions ?: return
        if (account.plan.active) return _state.update { it.copy(products = emptyList()) }
        viewModelScope.launch {
            val products = try {
                store.products()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            _state.update { it.copy(products = products) }
        }
    }

    /** Buys the store's [productId] for this account; the plan is read again once the service has the purchase. */
    fun subscribe(productId: String) = work(AccountTask.SUBSCRIBING) {
        val store = subscriptions ?: return@work
        if (!store.subscribe(productId)) return@work
        load()
        _state.update { it.copy(done = AccountDone.SUBSCRIBED) }
        sync.request()
    }

    /** Hands the service the subscriptions this store account already holds. */
    fun restorePurchases() = work(AccountTask.SUBSCRIBING) {
        val store = subscriptions ?: return@work
        store.restore()
        load()
        _state.update { it.copy(done = AccountDone.PURCHASES_RESTORED) }
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
                    (if (signedOut) AccountUiState(available = it.available, autoSync = it.autoSync) else it).copy(problem = e.problem, problemText = e.message)
                }
                if (e.problem == AccountProblem.SIGNED_OUT) offerProviders()
            } catch (e: IdentityException) {
                _state.update { it.copy(problem = AccountProblem.INVALID, problemText = e.message) }
            } catch (e: StoreException) {
                _state.update { it.copy(problem = AccountProblem.INVALID, problemText = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(problem = AccountProblem.SERVER, problemText = e.message) }
            } finally {
                _state.update { it.copy(busy = null) }
            }
        }
    }
}
