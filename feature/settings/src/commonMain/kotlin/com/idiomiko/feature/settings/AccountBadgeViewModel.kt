package com.idiomiko.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountProfile
import com.idiomiko.core.domain.account.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Who the app's bar shows as signed in, with the image of their picture once it is fetched. */
data class AccountBadgeState(val profile: AccountProfile? = null, val picture: ByteArray? = null)

/**
 * The account behind the bar's avatar. Each time the device is signed in, at launch too, the service
 * is asked once for the account's latest name and picture; the picture is fetched when it changes.
 */
class AccountBadgeViewModel(private val accounts: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(AccountBadgeState(accounts.profile.value))
    val state: StateFlow<AccountBadgeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            accounts.signedIn.map { it != null }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) try {
                    accounts.account()
                } catch (_: AccountException) {
                    // Offline, or signed out meanwhile: the bar keeps what the device has.
                }
            }
        }
        viewModelScope.launch {
            accounts.profile.collectLatest { profile ->
                // The same picture under a new name or email is not fetched again.
                val kept = _state.value.picture.takeIf { profile?.picture != null && profile.picture == _state.value.profile?.picture }
                _state.value = AccountBadgeState(profile, kept)
                if (profile?.picture != null && kept == null) _state.value = AccountBadgeState(profile, accounts.picture())
            }
        }
    }
}
