package com.tayra.languages.backend

import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncRecord
import com.tayra.languages.sync.Tokens
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

/** Guessing at sign-ins is slowed down by address; the owner, and only the owner, sees how sync does. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.rate-limits.auth-per-minute=5", "tayra.free-emails=metrics@example.com"])
class OperationsTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun login(address: String) = mvc.post("/api/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"nobody-${UUID.randomUUID()}@example.com","password":"a guess","device":{"id":"d"}}"""
        with { it.remoteAddr = address; it }
    }

    @Test
    fun tooManySignInsFromOneAddressWait() {
        repeat(5) { login("203.0.113.7").andExpect { status { isUnauthorized() } } }
        login("203.0.113.7").andExpect {
            status { isTooManyRequests() }
            header { exists("Retry-After") }
            jsonPath("$.error") { value(ApiErrors.RATE_LIMITED) }
        }
        login("203.0.113.8").andExpect { status { isUnauthorized() } }
        mvc.get("/api/v1/info") { with { it.remoteAddr = "203.0.113.7"; it } }.andExpect { status { isOk() } }
    }

    @Test
    fun metricsNeedTheAdminToken() {
        val body = mvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"metrics@example.com","password":"a password","device":{"id":"d"}}"""
            with { it.remoteAddr = "198.51.100.1"; it }
        }.andReturn().response.contentAsString
        val auth = "Bearer " + SyncJson.decodeFromString(Tokens.serializer(), body).accessToken
        mvc.post("/api/v1/sync/push") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = SyncJson.encodeToString(PushRequest.serializer(), PushRequest("d", listOf(SyncRecord("level", "pt", 1, deviceId = "d"))))
        }.andExpect { status { isOk() } }

        mvc.get("/actuator/metrics/tayra.sync.pushed").andExpect { status { isUnauthorized() } }
        mvc.get("/actuator/metrics/tayra.sync.pushed") { header("X-Admin-Token", "a guess") }.andExpect { status { isUnauthorized() } }
        mvc.get("/actuator/metrics/tayra.sync.pushed") { header("X-Admin-Token", "test-admin-token") }.andExpect {
            status { isOk() }
            jsonPath("$.measurements[0].value") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(1.0)) }
        }
    }
}
