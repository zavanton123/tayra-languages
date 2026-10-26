package com.tayra.languages.core.data.account

import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.sync.SyncJson
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Google's OAuth for installed apps, where no web view may be used: the app opens Google's page in
 * the browser with a PKCE challenge, Google sends the browser back to [Request.redirectUri] with a
 * code, and the app exchanges the code, with the challenge's verifier, for the ID token it signs in with.
 */
class GoogleOAuth(
    private val http: HttpClient,
    private val authorizeUrl: String = "https://accounts.google.com/o/oauth2/v2/auth",
    private val tokenUrl: String = "https://oauth2.googleapis.com/token",
) {
    class Request(val url: String, val redirectUri: String, val state: String, internal val verifier: String)

    @OptIn(ExperimentalUuidApi::class)
    fun request(clientId: String, redirectUri: String): Request {
        // From the platform's secure random source; hex is within PKCE's characters.
        val verifier = Uuid.random().toHexString() + Uuid.random().toHexString()
        val state = Uuid.random().toHexString()
        val challenge = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(sha256(verifier.encodeToByteArray()))
        val url = URLBuilder(authorizeUrl).apply {
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", "openid email")
            parameters.append("code_challenge", challenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("state", state)
            parameters.append("prompt", "select_account")
        }.buildString()
        return Request(url, redirectUri, state, verifier)
    }

    /** The ID token for the [code] Google sent back to [request]'s redirect, with its [state]. */
    suspend fun idToken(request: Request, code: String, state: String?, clientId: String, clientSecret: String?): String {
        if (state != request.state) throw IdentityException("The answer from Google was not for this sign-in")
        val response = http.submitForm(tokenUrl, parameters {
            append("client_id", clientId)
            clientSecret?.let { append("client_secret", it) }
            append("code", code)
            append("code_verifier", request.verifier)
            append("grant_type", "authorization_code")
            append("redirect_uri", request.redirectUri)
        })
        val body = runCatching { SyncJson.parseToJsonElement(response.bodyAsText()) as JsonObject }.getOrNull()
        val token = (body?.get("id_token") as? JsonPrimitive)?.contentOrNull
        if (!response.status.isSuccess() || token == null) {
            throw IdentityException((body?.get("error_description") as? JsonPrimitive)?.contentOrNull ?: "Google did not sign you in")
        }
        return token
    }
}
