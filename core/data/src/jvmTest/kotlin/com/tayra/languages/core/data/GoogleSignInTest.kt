package com.tayra.languages.core.data

import com.sun.net.httpserver.HttpServer
import com.tayra.languages.core.data.account.DesktopIdentitySignIn
import com.tayra.languages.core.data.account.GoogleOAuth
import com.tayra.languages.core.data.account.sha256
import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.core.domain.account.SignInClients
import com.tayra.languages.core.domain.account.SignInProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Signing in with Google from the desktop: the browser's round trip, PKCE, and the code exchanged for the ID token. */
class GoogleSignInTest {

    private fun query(text: String) = text.split('&').filter { '=' in it }
        .associate { URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }

    /** Google's token endpoint: it gives the ID token for the code it handed out, to the verifier of the challenge it was shown. */
    private class FakeGoogle {
        var challenge: String? = null
        val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/token") { exchange ->
                val form = exchange.requestBody.readAllBytes().decodeToString().split('&')
                    .associate { URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
                val digest = MessageDigest.getInstance("SHA-256").digest(form.getValue("code_verifier").toByteArray())
                val ok = form["code"] == "code-1" && form["client_id"] == "desktop-client" && form["client_secret"] == "GOCSPX-x" &&
                    Base64.getUrlEncoder().withoutPadding().encodeToString(digest) == challenge
                val body = if (ok) """{"id_token":"id-token-1","access_token":"a","expires_in":3599}""" else """{"error":"invalid_grant","error_description":"Bad Request"}"""
                exchange.sendResponseHeaders(if (ok) 200 else 400, body.length.toLong())
                exchange.responseBody.use { it.write(body.toByteArray()) }
            }
            start()
        }
        val url get() = "http://127.0.0.1:${server.address.port}"
    }

    private val clients = SignInClients(googleDesktop = "desktop-client", googleDesktopSecret = "GOCSPX-x")

    /** A browser that goes straight back to the app with [answer], as Google would once the person chose. */
    private fun browser(google: FakeGoogle, answer: (Map<String, String>) -> String): (String) -> Unit = { url ->
        val asked = query(URI(url).rawQuery)
        google.challenge = asked["code_challenge"]
        assertEquals("S256", asked["code_challenge_method"])
        assertEquals("openid email", asked["scope"])
        val back = "${asked.getValue("redirect_uri")}/?${answer(asked)}"
        Thread { java.net.http.HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI(back)).build(), HttpResponse.BodyHandlers.discarding()) }.start()
    }

    @Test
    fun theDigestIsSha256() {
        for (size in listOf(0, 1, 55, 56, 63, 64, 65, 1000)) {
            val message = ByteArray(size) { (it * 31 + 7).toByte() }
            assertContentEquals(MessageDigest.getInstance("SHA-256").digest(message), sha256(message), "$size bytes")
        }
    }

    @Test
    fun theBrowsersCodeIsExchangedForTheIdToken() = runBlocking {
        val google = FakeGoogle()
        val signIn = DesktopIdentitySignIn(GoogleOAuth(HttpClient(), tokenUrl = "${google.url}/token"), browser(google) { "code=code-1&state=${it["state"]}" })
        assertEquals(listOf(SignInProvider.GOOGLE), signIn.offers(clients))
        assertEquals(emptyList(), signIn.offers(SignInClients()))
        assertEquals("id-token-1", signIn.idToken(SignInProvider.GOOGLE, clients))
        google.server.stop(0)
    }

    @Test
    fun cancellingOrAnotherSignInsAnswerSignsNoOneIn() = runBlocking {
        val google = FakeGoogle()
        val cancelled = DesktopIdentitySignIn(GoogleOAuth(HttpClient(), tokenUrl = "${google.url}/token"), browser(google) { "error=access_denied&state=${it["state"]}" })
        assertNull(cancelled.idToken(SignInProvider.GOOGLE, clients))
        val forged = DesktopIdentitySignIn(GoogleOAuth(HttpClient(), tokenUrl = "${google.url}/token"), browser(google) { "code=code-1&state=someone-else" })
        assertFailsWith<IdentityException> { forged.idToken(SignInProvider.GOOGLE, clients) }
        google.server.stop(0)
    }
}
