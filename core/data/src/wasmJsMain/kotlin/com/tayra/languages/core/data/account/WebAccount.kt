package com.tayra.languages.core.data.account

import com.tayra.languages.core.data.settings.SecureStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import kotlinx.browser.window

/** The web app's account requests: they carry the sign-in cookie, and say they come from the web app. */
fun webAccountHttpClient(): HttpClient = HttpClient(Js) {
    engine { configureRequest { credentials = "include".toJsString() } }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
    }
    defaultRequest { header("X-Tayra-Client", "web") }
}

/** The sign-in's email and account, kept across tabs and restarts; the refresh token is the cookie's. */
class WebAccountStore : SecureStore {
    override val description: String = "kept in this browser"

    override fun get(key: String): String? = window.localStorage.getItem(PREFIX + key)

    override fun put(key: String, value: String) = window.localStorage.setItem(PREFIX + key, value)

    override fun remove(key: String) = window.localStorage.removeItem(PREFIX + key)

    private companion object {
        const val PREFIX = "tayra.account."
    }
}
