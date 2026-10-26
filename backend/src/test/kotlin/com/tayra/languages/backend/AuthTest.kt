package com.tayra.languages.backend

import com.tayra.languages.backend.auth.Tokens
import com.tayra.languages.sync.SyncJson
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Signing up and in with an email and password, staying signed in, and signing out, device by device. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class)
class AuthTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun email() = "reader-${UUID.randomUUID()}@example.com"

    private fun post(path: String, body: String) = mvc.post(path) { contentType = MediaType.APPLICATION_JSON; content = body }

    private fun device(id: String) = """{"id":"$id","name":"Desk","platform":"desktop"}"""

    private fun register(email: String, password: String = "correct horse", device: String = "desk-1"): Tokens {
        val body = post("/api/v1/auth/register", """{"email":"$email","password":"$password","device":${device(device)}}""")
            .andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return SyncJson.decodeFromString(Tokens.serializer(), body)
    }

    private fun login(email: String, password: String, device: String) =
        post("/api/v1/auth/login", """{"email":"$email","password":"$password","device":${device(device)}}""")

    @Test
    fun anAccountIsMadeAndSignedInto() {
        val email = email()
        val tokens = register(email.uppercase())
        assertEquals(email, tokens.user.email, "emails are kept in lower case")
        assertEquals(900, tokens.expiresIn)
        mvc.get("/api/v1/me") { header("Authorization", "Bearer ${tokens.accessToken}") }
            .andExpect { status { isOk() }; jsonPath("$.email") { value(email) }; jsonPath("$.emailVerified") { value(false) } }
        val stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String::class.java, email)!!
        assertTrue(stored.startsWith("{bcrypt}"), "the password is kept only as a bcrypt hash")
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE token_hash = ?", Int::class.java, tokens.refreshToken), "and so is the refresh token")
    }

    @Test
    fun refusalsSayWhy() {
        val email = email()
        register(email)
        post("/api/v1/auth/register", """{"email":"$email","password":"another one","device":${device("x")}}""")
            .andExpect { status { isConflict() }; jsonPath("$.error") { value("email_taken") } }
        login(email, "wrong password", "x").andExpect { status { isUnauthorized() }; jsonPath("$.error") { value("bad_credentials") } }
        login("nobody-${UUID.randomUUID()}@example.com", "whatever1", "x").andExpect { status { isUnauthorized() }; jsonPath("$.error") { value("bad_credentials") } }
        post("/api/v1/auth/register", """{"email":"${email()}","password":"short","device":${device("x")}}""")
            .andExpect { status { isBadRequest() }; jsonPath("$.error") { value("invalid_request") } }
        post("/api/v1/auth/register", """{"email":"not an email","password":"long enough","device":${device("x")}}""")
            .andExpect { status { isBadRequest() } }
        post("/api/v1/auth/register", """{"nonsense":true}""").andExpect { status { isBadRequest() }; jsonPath("$.error") { value("invalid_request") } }
        mvc.get("/api/v1/me").andExpect { status { isUnauthorized() } }
        mvc.get("/api/v1/me") { header("Authorization", "Bearer not.a.token") }.andExpect { status { isUnauthorized() } }
    }

    /** A refresh token works once: each use gives a new one, and the spent one signs nothing in. */
    @Test
    fun aRefreshTokenIsSpentByUse() {
        val first = register(email())
        val second = SyncJson.decodeFromString(Tokens.serializer(),
            post("/api/v1/auth/refresh", """{"refreshToken":"${first.refreshToken}"}""").andExpect { status { isOk() } }.andReturn().response.contentAsString)
        assertNotEquals(first.refreshToken, second.refreshToken)
        post("/api/v1/auth/refresh", """{"refreshToken":"${first.refreshToken}"}""")
            .andExpect { status { isUnauthorized() }; jsonPath("$.error") { value("signed_out") } }
        mvc.get("/api/v1/me") { header("Authorization", "Bearer ${second.accessToken}") }.andExpect { status { isOk() } }
    }

    /** Each device has its own sign-in; signing one out leaves the others. */
    @Test
    fun devicesAreSignedInAndOutOneByOne() {
        val email = email()
        val desk = register(email, device = "desk-1")
        val phone = SyncJson.decodeFromString(Tokens.serializer(), login(email, "correct horse", "phone-1").andExpect { status { isOk() } }.andReturn().response.contentAsString)
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens r JOIN users u ON u.id = r.user_id WHERE u.email = ?", Int::class.java, email))
        // Signing in again on the same device replaces its sign-in.
        login(email, "correct horse", "phone-1").andExpect { status { isOk() } }
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens r JOIN users u ON u.id = r.user_id WHERE u.email = ?", Int::class.java, email))

        post("/api/v1/auth/logout", """{"refreshToken":"${desk.refreshToken}"}""").andExpect { status { isNoContent() } }
        post("/api/v1/auth/refresh", """{"refreshToken":"${desk.refreshToken}"}""").andExpect { status { isUnauthorized() } }
        post("/api/v1/auth/refresh", """{"refreshToken":"${phone.refreshToken}"}""").andExpect { status { isUnauthorized() } }
    }
}
