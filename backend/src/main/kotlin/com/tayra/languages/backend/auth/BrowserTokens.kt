package com.tayra.languages.backend.auth

import com.tayra.languages.sync.RefreshRequest
import com.tayra.languages.sync.Tokens
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * The web app's refresh token lives in a cookie its scripts cannot read, so a script injected into
 * the page cannot take the sign-in with it; the access token stays in the page's memory. The other
 * apps keep the refresh token in the platform's protected storage and get it in the body.
 */
@Component
class BrowserTokens(@Value("\${tayra.auth.refresh-token-days}") private val days: Long) {

    /** Whether the request comes from the web app, which says so in a header of its own. */
    fun isBrowser(request: HttpServletRequest): Boolean = request.getHeader(CLIENT_HEADER) == WEB

    /** [tokens] as the client should get them: for the web app the refresh token goes into the cookie instead. */
    fun hand(tokens: Tokens, request: HttpServletRequest, response: HttpServletResponse): Tokens {
        if (!isBrowser(request)) return tokens
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken, Duration.ofDays(days)).toString())
        return tokens.copy(refreshToken = "")
    }

    /** The refresh token sent: in the body, or from the web app in the cookie. */
    fun sent(body: RefreshRequest, request: HttpServletRequest): RefreshRequest =
        if (body.refreshToken.isNotBlank()) body
        else RefreshRequest(request.cookies?.firstOrNull { it.name == COOKIE }?.value.orEmpty())

    fun forget(request: HttpServletRequest, response: HttpServletResponse) {
        if (isBrowser(request)) response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
    }

    private fun cookie(value: String, age: Duration) = ResponseCookie.from(COOKIE, value)
        .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth").maxAge(age).build()

    companion object {
        const val CLIENT_HEADER = "X-Tayra-Client"
        const val WEB = "web"
        const val COOKIE = "tayra_refresh"
    }
}
