package com.tayra.languages.core.data.account

import com.sun.net.httpserver.HttpServer
import com.tayra.languages.core.domain.account.IdentityException
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.SignInClients
import com.tayra.languages.core.domain.account.SignInProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import kotlin.time.Duration.Companion.minutes

/**
 * Signing in with Google on the desktop: Google's page opens in the system browser, which Google
 * sends back to a server of the moment on 127.0.0.1, the loopback address Google allows desktop
 * apps; the code it brings is exchanged for the ID token.
 */
class DesktopIdentitySignIn(
    private val oauth: GoogleOAuth,
    private val openBrowser: (String) -> Unit = ::browse,
) : IdentitySignIn {

    override fun offers(clients: SignInClients) = listOfNotNull(SignInProvider.GOOGLE.takeIf { clients.googleDesktop != null })

    override suspend fun idToken(provider: SignInProvider, clients: SignInClients): String? {
        val clientId = clients.googleDesktop?.takeIf { provider == SignInProvider.GOOGLE } ?: throw IdentityException("This app cannot sign in with $provider")
        val answer = CompletableDeferred<Map<String, String>>()
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            val query = exchange.requestURI.rawQuery.orEmpty().split('&').filter { '=' in it }
                .associate { URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
            val page = PAGE.toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, page.size.toLong())
            exchange.responseBody.use { it.write(page) }
            answer.complete(query)
        }
        server.start()
        try {
            val request = oauth.request(clientId, "http://127.0.0.1:${server.address.port}")
            openBrowser(request.url)
            // Someone who closes the browser instead of signing in is not waited for long.
            val query = withTimeoutOrNull(5.minutes) { answer.await() } ?: return null
            query["error"]?.let { return if (it == "access_denied") null else throw IdentityException("Google refused: $it") }
            val code = query["code"] ?: throw IdentityException("Google sent no code")
            return oauth.idToken(request, code, query["state"], clientId, clients.googleDesktopSecret)
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        const val PAGE = "<!doctype html><meta charset=utf-8><title>Tayra Languages</title>" +
            "<body style=\"font-family:system-ui;margin:4rem\"><h1>Signed in</h1><p>You can close this tab and go back to Tayra Languages.</p>"

        fun browse(url: String) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) Desktop.getDesktop().browse(URI(url))
            else ProcessBuilder(if (System.getProperty("os.name").lowercase().contains("mac")) "open" else "xdg-open", url).start()
        }
    }
}
