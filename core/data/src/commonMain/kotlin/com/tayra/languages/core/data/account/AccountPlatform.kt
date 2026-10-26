package com.tayra.languages.core.data.account

import com.tayra.languages.core.data.settings.SecureStore
import io.ktor.client.HttpClient

/**
 * What a platform's account requests need beyond the usual: in the web app, an HTTP client that sends
 * the sign-in cookie, and a store that outlives the tab for what is kept of the sign-in, none of it
 * secret there, as the refresh token stays in the cookie.
 */
class AccountPlatform(val http: HttpClient, val store: SecureStore)
