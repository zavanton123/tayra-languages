package com.idiomiko.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.SyncRunner
import com.idiomiko.core.domain.account.SyncResult
import com.idiomiko.core.domain.account.IdentityException
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.SignInClients
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.domain.account.StoreException
import com.idiomiko.core.domain.account.StoreProduct
import com.idiomiko.core.domain.account.Subscriptions
import kotlinx.coroutines.CancellationException
import com.idiomiko.core.domain.account.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the account page shows. */
data class AccountUiState(
    /** Whether this build can reach an account service. */
    val available: Boolean = false,
    /** The email of the account this device is signed in to; null when signed out. */
    val signedIn: String? = null,
    val account: AccountInfo? = null,
    /** The first sync would combine this device's library with the account's: the page asks first. */
    val confirmCombine: Boolean = false,
    /** What the store sells here, while the account has no subscription; empty without a store. */
    val products: List<StoreProduct> = emptyList(),
    /** Whether the store can restore purchases here. */
    val storeAvailable: Boolean = false,
    /** The providers this app can sign in with, as the service takes them. */
    val signInProviders: List<SignInProvider> = emptyList(),
    /** What is under way, shown with a spinner; null when nothing is. */
    val busy: AccountTask? = null,
    /** What went wrong last, until the next action. */
    val problem: AccountProblem? = null,
    val problemText: String? = null,
    /** A reset code was sent to this address; the page asks for it and a new password. */
    val resetSentTo: String? = null,
    /** The last thing done, said once. */
    val done: AccountDone? = null,
    /** How syncing stands on this device: under way, when it last synced, why it last failed. */
    val sync: SyncStatus = SyncStatus(),
    /** This device holds changes not sent to the account yet; taken as so until it is known. */
    val unsent: Boolean = true,
    /** The words known across the languages being learned, as last counted; null where not counted. */
    val knownWords: Int? = null,
) {
    /** Signed out again: what is about the account goes, what is about this device stays. */
    fun signedOut(): AccountUiState = AccountUiState(available = available, sync = sync, unsent = unsent, knownWords = knownWords)
}

enum class AccountTask { SIGNING_IN, LOADING, SENDING_CODE, CONFIRMING, SUBSCRIBING, SYNCING, DELETING }

enum class AccountDone { EMAIL_CONFIRMED, CODE_SENT, PASSWORD_RESET, SUBSCRIBED, PURCHASES_RESTORED, SYNCED, ACCOUNT_DELETED }

/** The account this device is signed in to, and combining its library with the account's when the reader agrees. */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModel(
    private val accounts: AccountRepository,
    private val sync: SyncRunner,
    /** Buying in the store, in the phone apps; null elsewhere. */
    private val subscriptions: Subscriptions? = null,
    /** Signing in with Google or Apple, where the platform can; null elsewhere. */
    private val identity: IdentitySignIn? = null,
    /** The words known across the languages being learned, for the sync card's vocabulary row. */
    knownWords: Flow<Int> = emptyFlow(),
) : ViewModel() {
    private var clients = SignInClients()

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        reload()
        // A sync asked for elsewhere found two libraries: the question is asked here.
        viewModelScope.launch { sync.status.map { it.waitingToCombine }.distinctUntilChanged().collect { waiting -> if (waiting) _state.update { it.copy(confirmCombine = true) } } }
        viewModelScope.launch { sync.status.collect { status -> _state.update { it.copy(sync = status) } } }
        // Signing in to another account makes everything here unsent to it.
        viewModelScope.launch { accounts.signedIn.flatMapLatest { sync.unsent() }.collect { unsent -> _state.update { it.copy(unsent = unsent) } } }
        viewModelScope.launch { knownWords.collect { words -> _state.update { it.copy(knownWords = words) } } }
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
        _state.update { it.signedOut() }
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

    /**
     * Syncs, combining the two libraries when [combine], the reader's answer to the page's question. A failure
     * is said by the sync card, from the sync's own status, rather than again above it.
     */
    fun syncNow(combine: Boolean = false) = work(AccountTask.SYNCING, sayProblem = false) {
        when (sync.syncNow(combine)) {
            SyncResult.NeedsCombining -> _state.update { it.copy(confirmCombine = true) }
            is SyncResult.Done -> _state.update { it.copy(confirmCombine = false, done = AccountDone.SYNCED) }
        }
    }

    fun cancelCombine() = _state.update { it.copy(confirmCombine = false) }

    fun deleteAccount() = work(AccountTask.DELETING) {
        accounts.deleteAccount()
        _state.update { it.signedOut().copy(done = AccountDone.ACCOUNT_DELETED) }
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
    }

    /** Hands the service the subscriptions this store account already holds. */
    fun restorePurchases() = work(AccountTask.SUBSCRIBING) {
        val store = subscriptions ?: return@work
        store.restore()
        load()
        _state.update { it.copy(done = AccountDone.PURCHASES_RESTORED) }
    }

    /** Runs [action] as [task]; a failure is kept for the page to say, and signing out elsewhere shows the sign-in again. */
    private fun work(task: AccountTask, sayProblem: Boolean = true, action: suspend () -> Unit) {
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
                    if (signedOut) it.signedOut().copy(problem = e.problem, problemText = e.message)
                    else if (sayProblem) it.copy(problem = e.problem, problemText = e.message) else it
                }
                if (e.problem == AccountProblem.SIGNED_OUT) offerProviders()
            } catch (e: IdentityException) {
                _state.update { it.copy(problem = AccountProblem.INVALID, problemText = e.message) }
            } catch (e: StoreException) {
                _state.update { it.copy(problem = AccountProblem.INVALID, problemText = e.message) }
            } catch (e: Exception) {
                if (sayProblem) _state.update { it.copy(problem = AccountProblem.SERVER, problemText = e.message) }
            } finally {
                _state.update { it.copy(busy = null) }
            }
        }
    }
}
