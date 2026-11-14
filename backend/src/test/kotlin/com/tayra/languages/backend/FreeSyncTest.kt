package com.tayra.languages.backend

import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncRecord
import com.tayra.languages.sync.Tokens
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import kotlin.test.assertEquals

/** With the subscription gate off, an account that never paid syncs as soon as it signs in. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.require-subscription=false"])
class FreeSyncTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun register(email: String): String {
        val body = mvc.post("/api/v1/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"d"}}""" }
            .andReturn().response.contentAsString
        return "Bearer " + SyncJson.decodeFromString(Tokens.serializer(), body).accessToken
    }

    @Test
    fun anUnpaidAccountPushesAndPulls() {
        val auth = register("free-${UUID.randomUUID()}@example.com")
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect {
            status { isOk() }
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.source") { value("free") }
        }
        val record = SyncRecord("term", "pt/casa", System.currentTimeMillis(), data = buildJsonObject { put("status", 2); put("translation", "house") }, deviceId = "phone")
        mvc.post("/api/v1/sync/push") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = SyncJson.encodeToString(PushRequest.serializer(), PushRequest("phone", listOf(record)))
        }.andExpect { status { isOk() } }
        val pulled = SyncJson.decodeFromString(PullResponse.serializer(),
            mvc.get("/api/v1/sync/pull") { header("Authorization", auth); param("since", "0"); param("limit", "100") }
                .andExpect { status { isOk() } }.andReturn().response.contentAsString)
        assertEquals(listOf("pt/casa"), pulled.records.map { it.key })
    }

    @Test
    fun aPaidAccountStillShowsItsSubscription() {
        val email = "paid-${UUID.randomUUID()}@example.com"
        val auth = register(email)
        jdbc.update("UPDATE users SET subscription_active_until = now() + interval '1 day', subscription_source = 'paddle' WHERE email = ?", email)
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect {
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.source") { value("paddle") }
        }
    }
}
