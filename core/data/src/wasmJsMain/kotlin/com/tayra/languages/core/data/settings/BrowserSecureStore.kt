package com.tayra.languages.core.data.settings

import kotlinx.browser.window

/**
 * Browsers offer no storage that page scripts cannot read, so the secret is kept in
 * sessionStorage: it survives reloads of this tab and is gone when the tab closes.
 */
class BrowserSecureStore : SecureStore {
    override val description: String = "kept only until this browser tab closes"

    override fun get(key: String): String? = window.sessionStorage.getItem(PREFIX + key)

    override fun put(key: String, value: String) = window.sessionStorage.setItem(PREFIX + key, value)

    override fun remove(key: String) = window.sessionStorage.removeItem(PREFIX + key)

    private companion object {
        const val PREFIX = "tayra.secure."
    }
}
