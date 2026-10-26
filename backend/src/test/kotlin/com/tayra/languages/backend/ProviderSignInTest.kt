package com.tayra.languages.backend

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import com.tayra.languages.sync.Tokens
import com.tayra.languages.sync.SyncJson
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.net.InetSocketAddress
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Signing in with Google and Apple ID tokens, against keys published here as the providers publish
 * theirs, so the whole check runs: signature, issuer, audience, expiry, and the account found or made.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
class ProviderSignInTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    companion object {
        private val key: RSAKey = RSAKeyGenerator(2048).keyID("provider-key").generate()
        private val stranger: RSAKey = RSAKeyGenerator(2048).keyID("provider-key").generate()
        private val keys = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/keys") { exchange ->
                val body = JWKSet(key.toPublicJWK()).toString().toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun providers(registry: DynamicPropertyRegistry) {
            val jwks = "http://127.0.0.1:${keys.address.port}/keys"
            registry.add("tayra.auth.google.jwks-uri") { jwks }
            registry.add("tayra.auth.google.client-ids") { "android-client,ios-client" }
            registry.add("tayra.auth.apple.jwks-uri") { jwks }
            registry.add("tayra.auth.apple.client-ids") { "com.tayra.languages" }
        }
    }

    private fun idToken(
        sub: String, email: String, verified: Any = true, audience: String = "android-client",
        issuer: String = "https://accounts.google.com", expires: Date = Date(System.currentTimeMillis() + 600_000), signer: RSAKey = key,
    ): String = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signer.keyID).build(), JWTClaimsSet.Builder()
        .issuer(issuer).subject(sub).audience(audience).expirationTime(expires).issueTime(Date())
        .claim("email", email).claim("email_verified", verified).build())
        .apply { sign(RSASSASigner(signer)) }.serialize()

    private fun signIn(provider: String, token: String) = mvc.post("/api/v1/auth/$provider") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"idToken":"$token","device":{"id":"phone","platform":"android"}}"""
    }

    private fun tokens(provider: String, token: String) =
        SyncJson.decodeFromString(Tokens.serializer(), signIn(provider, token).andExpect { status { isOk() } }.andReturn().response.contentAsString)

    @Test
    fun aGoogleUserGetsAnAccountAndKeepsIt() {
        val sub = UUID.randomUUID().toString()
        val email = "g-$sub@gmail.com"
        val first = tokens("google", idToken(sub, email.uppercase()))
        assertEquals(email, first.user.email)
        assertEquals(true, first.user.emailVerified)
        assertEquals(null, jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?::uuid", String::class.java, first.user.id), "no password")
        // Google may change the email later; the account goes by Google's id for the user.
        val again = tokens("google", idToken(sub, "renamed-$sub@gmail.com", audience = "ios-client", issuer = "accounts.google.com"))
        assertEquals(first.user.id, again.user.id)
    }

    @Test
    fun aConfirmedEmailLinksTheProviderToTheAccountThatHasIt() {
        val email = "both-${UUID.randomUUID()}@example.com"
        val made = mvc.post("/api/v1/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"desk"}}""" }
            .andReturn().response.contentAsString.let { SyncJson.decodeFromString(Tokens.serializer(), it) }
        val apple = tokens("apple", idToken("apple-${UUID.randomUUID()}", email, verified = "true", audience = "com.tayra.languages", issuer = "https://appleid.apple.com"))
        assertEquals(made.user.id, apple.user.id)
        assertEquals(true, apple.user.emailVerified, "the provider confirmed the address")

        val other = "unconfirmed-${UUID.randomUUID()}@example.com"
        mvc.post("/api/v1/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$other","password":"a password","device":{"id":"desk"}}""" }
        signIn("google", idToken(UUID.randomUUID().toString(), other, verified = false))
            .andExpect { status { isConflict() }; jsonPath("$.error") { value("email_taken") } }
    }

    @Test
    fun tokensNotMeantForUsAreRefused() {
        val sub = UUID.randomUUID().toString()
        val email = "x-$sub@gmail.com"
        for (token in listOf(
            idToken(sub, email, audience = "someone-elses-app"),
            idToken(sub, email, issuer = "https://evil.example"),
            idToken(sub, email, expires = Date(System.currentTimeMillis() - 600_000)),
            idToken(sub, email, signer = stranger),
            "not a token",
        )) signIn("google", token).andExpect { status { isUnauthorized() }; jsonPath("$.error") { value("bad_id_token") } }
        signIn("apple", idToken(sub, email)).andExpect { status { isUnauthorized() } }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Int::class.java, email))
        assertNotEquals("", key.keyID)
    }
}
