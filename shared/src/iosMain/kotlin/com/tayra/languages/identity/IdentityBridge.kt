package com.tayra.languages.identity

import com.tayra.languages.core.data.account.GoogleOAuth
import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.SignInClients
import com.tayra.languages.core.domain.account.SignInProvider
import io.ktor.http.Url
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * What the Swift side implements with AuthenticationServices. Plain callbacks rather than suspend
 * functions, because Kotlin cannot call a suspend function that Swift implements; each callback
 * gets neither a result nor an error when the person cancelled.
 */
interface IdentityBridge {
    /** Opens [url] in a web authentication session and gives the address it comes back to, with [callbackScheme]. */
    fun authenticate(url: String, callbackScheme: String, callback: (callbackUrl: String?, error: String?) -> Unit)

    /** Signs in with Apple and gives the identity token. */
    fun appleIdToken(callback: (idToken: String?, error: String?) -> Unit)
}

/**
 * Signing in with Google or Apple on iOS: Google's page in a web authentication session, which
 * returns to the iOS client's own scheme with a code exchanged here, and Apple's sheet.
 */
class IosIdentitySignIn(private val bridge: IdentityBridge, private val oauth: GoogleOAuth) : IdentitySignIn {

    override fun offers(clients: SignInClients) = listOfNotNull(
        SignInProvider.APPLE.takeIf { clients.apple },
        SignInProvider.GOOGLE.takeIf { clients.googleIos != null },
    )

    override suspend fun idToken(provider: SignInProvider, clients: SignInClients): String? = when (provider) {
        SignInProvider.APPLE -> callback { done -> bridge.appleIdToken(done) }
        SignInProvider.GOOGLE -> {
            val clientId = clients.googleIos ?: throw IdentityException("This app cannot sign in with Google")
            // Google's iOS clients answer at their id turned round as a scheme.
            val scheme = "com.googleusercontent.apps." + clientId.removeSuffix(".apps.googleusercontent.com")
            val request = oauth.request(clientId, "$scheme:/oauth2redirect")
            val back = callback { done -> bridge.authenticate(request.url, scheme, done) }?.let(::Url)
            when {
                back == null -> null
                back.parameters["error"] == "access_denied" -> null
                back.parameters["error"] != null -> throw IdentityException("Google refused: ${back.parameters["error"]}")
                else -> oauth.idToken(request, back.parameters["code"] ?: throw IdentityException("Google sent no code"), back.parameters["state"], clientId, null)
            }
        }
    }

    private suspend fun callback(start: ((String?, String?) -> Unit) -> Unit): String? = suspendCancellableCoroutine { continuation ->
        start { result, error ->
            if (error != null) continuation.resumeWithException(IdentityException(error)) else continuation.resume(result)
        }
    }
}
