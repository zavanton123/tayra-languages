package com.tayra.languages.backend.auth

import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.Tokens
import com.tayra.languages.sync.ProviderSignInRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.util.UUID

class ProviderNotSetUp(provider: String) : ApiException(503, ApiErrors.PROVIDER_NOT_SET_UP, "Signing in with $provider is not set up on this service")

class BadIdToken : ApiException(401, ApiErrors.BAD_ID_TOKEN, "The sign-in could not be confirmed; try again")

/**
 * One identity provider whose ID tokens the apps obtain natively and send: Google or Apple. A token
 * counts when the provider signed it, it is for one of our apps ([clientIds]), and it has not expired.
 */
class IdentityProvider(
    val name: String,
    private val column: String,
    jwksUri: String,
    private val issuers: Set<String>,
    private val clientIds: Set<String>,
) {
    private val decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build().apply {
        setJwtValidator(DelegatingOAuth2TokenValidator(JwtTimestampValidator(), { jwt: Jwt ->
            val issuerOk = jwt.getClaimAsString("iss") in issuers
            val audienceOk = jwt.audience.orEmpty().any { it in clientIds }
            if (issuerOk && audienceOk) OAuth2TokenValidatorResult.success()
            else OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Wrong issuer or audience", null))
        }))
    }

    val setUp: Boolean get() = clientIds.isNotEmpty()

    /** The user's id at the provider, their email, and whether the provider confirmed it. */
    fun verify(token: String): Identity {
        if (!setUp) throw ProviderNotSetUp(name)
        val jwt = try {
            decoder.decode(token)
        } catch (e: JwtException) {
            throw BadIdToken()
        }
        val email = jwt.getClaimAsString("email")?.trim()?.lowercase() ?: throw BadIdToken()
        // Google sends a boolean, Apple a string.
        val verified = jwt.claims["email_verified"].toString() == "true"
        return Identity(column, jwt.subject ?: throw BadIdToken(), email, verified)
    }
}

data class Identity(val column: String, val subject: String, val email: String, val emailVerified: Boolean)

@Service
class ProviderSignIn(
    private val jdbc: JdbcClient,
    private val accounts: Accounts,
    private val auth: AuthService,
    private val clock: Clock,
    @Value("\${tayra.auth.google.jwks-uri}") googleJwks: String,
    @Value("\${tayra.auth.google.issuers}") googleIssuers: String,
    @Value("\${tayra.auth.google.client-ids}") googleClients: String,
    @Value("\${tayra.auth.google.web-client-id}") googleWeb: String,
    @Value("\${tayra.auth.google.ios-client-id}") googleIos: String,
    @Value("\${tayra.auth.google.desktop-client-id}") googleDesktop: String,
    @Value("\${tayra.auth.apple.jwks-uri}") appleJwks: String,
    @Value("\${tayra.auth.apple.issuers}") appleIssuers: String,
    @Value("\${tayra.auth.apple.client-ids}") appleClients: String,
) {
    private val providers = mapOf(
        "google" to IdentityProvider("Google", "google_sub", googleJwks, list(googleIssuers), list(googleClients) + list("$googleWeb,$googleIos,$googleDesktop")),
        "apple" to IdentityProvider("Apple", "apple_sub", appleJwks, list(appleIssuers), list(appleClients)),
    )

    /**
     * Signs in the account the provider's user has: found by their id there; or, the first time, the
     * account with the same confirmed email, which the provider is then linked to; or a new account.
     */
    @Transactional
    fun signIn(provider: String, request: ProviderSignInRequest): Tokens {
        val identity = (providers[provider] ?: throw InvalidRequest("No such provider")).verify(request.idToken)
        if (request.device.id.isBlank()) throw InvalidRequest("The device needs an id")
        val column = identity.column
        val known = jdbc.sql("SELECT id FROM users WHERE $column = ?").param(identity.subject).query(UUID::class.java).optional().orElse(null)
        val user = when {
            known != null -> accounts.byId(known)!!
            else -> {
                val byEmail = accounts.byEmail(identity.email)
                when {
                    byEmail != null && identity.emailVerified -> {
                        jdbc.sql("UPDATE users SET $column = ?, email_verified = TRUE WHERE id = ?").params(identity.subject, byEmail.id).update()
                        accounts.byId(byEmail.id)!!
                    }
                    // An address the provider has not confirmed cannot take over an account that has it.
                    byEmail != null -> throw EmailTaken()
                    else -> {
                        val created = accounts.create(identity.email, passwordHash = null, now = clock.instant())
                        jdbc.sql("UPDATE users SET $column = ?, email_verified = ? WHERE id = ?").params(identity.subject, identity.emailVerified, created.id).update()
                        accounts.byId(created.id)!!
                    }
                }
            }
        }
        return auth.signIn(user, request.device)
    }

    private fun list(text: String): Set<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

@RestController
class ProviderSignInController(private val signIn: ProviderSignIn, private val browser: BrowserTokens) {
    @PostMapping("/api/v1/auth/{provider}")
    fun signIn(@PathVariable provider: String, @RequestBody body: ProviderSignInRequest, request: HttpServletRequest, response: HttpServletResponse) =
        browser.hand(signIn.signIn(provider, body), request, response)
}
