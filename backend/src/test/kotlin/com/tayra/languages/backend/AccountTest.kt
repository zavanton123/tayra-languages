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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals

/** The account as the apps show it: its subscription, its devices, and deleting it. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.free-emails=owner@example.com"])
class AccountTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun signIn(email: String, device: String, register: Boolean): Tokens {
        val path = if (register) "/api/v1/auth/register" else "/api/v1/auth/login"
        val body = mvc.post(path) { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"$device","name":"$device name","platform":"test"}}""" }
            .andReturn().response.contentAsString
        return SyncJson.decodeFromString(Tokens.serializer(), body)
    }

    private fun Tokens.bearer() = "Bearer $accessToken"

    @Test
    fun theSubscriptionIsActiveWhilePaidForOrGivenFree() {
        val email = "paying-${UUID.randomUUID()}@example.com"
        val tokens = signIn(email, "desk", register = true)
        mvc.get("/api/v1/me") { header("Authorization", tokens.bearer()) }.andExpect {
            jsonPath("$.hasPassword") { value(true) }
            jsonPath("$.providers.length()") { value(0) }
            jsonPath("$.subscription.active") { value(false) }
        }
        val until = System.currentTimeMillis() + 86_400_000
        jdbc.update("UPDATE users SET subscription_active_until = to_timestamp(? / 1000.0), subscription_source = 'paddle' WHERE email = ?", until, email)
        mvc.get("/api/v1/me") { header("Authorization", tokens.bearer()) }.andExpect {
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.source") { value("paddle") }
        }
        jdbc.update("UPDATE users SET subscription_active_until = now() - interval '1 minute' WHERE email = ?", email)
        mvc.get("/api/v1/me") { header("Authorization", tokens.bearer()) }.andExpect { jsonPath("$.subscription.active") { value(false) } }

        val owner = signIn("owner@example.com", "desk", register = true)
        mvc.get("/api/v1/me") { header("Authorization", owner.bearer()) }.andExpect {
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.source") { value("free") }
        }
    }

    @Test
    fun devicesAreListedAndSignedOutFromAnother() {
        val email = "devices-${UUID.randomUUID()}@example.com"
        val desk = signIn(email, "desk", register = true)
        val phone = signIn(email, "phone", register = false)
        mvc.get("/api/v1/me/devices") { header("Authorization", desk.bearer()) }.andExpect {
            jsonPath("$.length()") { value(2) }
            jsonPath("$[?(@.id == 'desk')].current") { value(true) }
            jsonPath("$[?(@.id == 'phone')].current") { value(false) }
            jsonPath("$[?(@.id == 'phone')].name") { value("phone name") }
        }
        mvc.delete("/api/v1/me/devices/phone") { header("Authorization", desk.bearer()) }.andExpect { status { isNoContent() } }
        mvc.post("/api/v1/auth/refresh") { contentType = MediaType.APPLICATION_JSON; content = """{"refreshToken":"${phone.refreshToken}"}""" }
            .andExpect { status { isUnauthorized() } }
        mvc.get("/api/v1/me/devices") { header("Authorization", desk.bearer()) }.andExpect { jsonPath("$.length()") { value(1) } }
    }

    @Test
    fun anAccountIsDeletedWithEverythingKeptForIt() {
        val email = "leaving-${UUID.randomUUID()}@example.com"
        val tokens = signIn(email, "desk", register = true)
        mvc.delete("/api/v1/me") { header("Authorization", tokens.bearer()) }.andExpect { status { isNoContent() } }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Int::class.java, email))
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM refresh_tokens r WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = r.user_id)", Int::class.java))
        mvc.get("/api/v1/me") { header("Authorization", tokens.bearer()) }.andExpect { status { isUnauthorized() } }
        mvc.post("/api/v1/auth/login") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"desk"}}""" }
            .andExpect { status { isUnauthorized() } }
    }
}
