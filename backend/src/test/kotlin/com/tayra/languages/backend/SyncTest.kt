package com.tayra.languages.backend

import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncRecord
import com.tayra.languages.sync.Tokens
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import kotlin.test.assertTrue

/** Pushing changes merges them by the shared rules and numbers them; pulling returns what came after a number. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
class SyncTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun account(paid: Boolean = true): String {
        val email = "sync-${UUID.randomUUID()}@example.com"
        val body = mvc.post("/api/v1/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"d"}}""" }
            .andReturn().response.contentAsString
        if (paid) jdbc.update("UPDATE users SET subscription_active_until = now() + interval '1 day' WHERE email = ?", email)
        return "Bearer " + SyncJson.decodeFromString(Tokens.serializer(), body).accessToken
    }

    private fun term(at: Long, status: Int, statusAt: Long, translation: String, device: String) =
        SyncRecord("term", "pt/casa", at, data = buildJsonObject { put("status", status); put("statusChangedAt", statusAt); put("translation", translation) }, deviceId = device)

    private fun push(auth: String, device: String, vararg records: SyncRecord): PushResponse = SyncJson.decodeFromString(PushResponse.serializer(),
        mvc.post("/api/v1/sync/push") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = SyncJson.encodeToString(PushRequest.serializer(), PushRequest(device, records.toList()))
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString)

    private fun pull(auth: String, since: Long, limit: Int = 1000): PullResponse = SyncJson.decodeFromString(PullResponse.serializer(),
        mvc.get("/api/v1/sync/pull") { header("Authorization", auth); param("since", "$since"); param("limit", "$limit") }
            .andExpect { status { isOk() } }.andReturn().response.contentAsString)

    @Test
    fun twoDevicesChangesMergeOnTheServer() {
        val auth = account()
        val now = System.currentTimeMillis()
        push(auth, "phone", term(now - 10_000, status = 3, statusAt = now - 10_000, translation = "house", device = "phone"))
        // The desktop edits the translation later, from an older status.
        val after = push(auth, "desktop", term(now - 5_000, status = 1, statusAt = now - 60_000, translation = "home", device = "desktop"))
        val kept = pull(auth, 0).records.single()
        assertEquals(JsonPrimitive(3), kept.data!!["status"], "the status set last")
        assertEquals(JsonPrimitive("home"), kept.data!!["translation"], "the translation edited last")
        assertEquals(2, after.seq)

        // Sending what the account has already changes nothing and numbers nothing.
        assertEquals(2, push(auth, "desktop", kept).seq)
        assertEquals(emptyList(), pull(auth, 2).records)
    }

    @Test
    fun aPullPagesThroughChangesInOrder() {
        val auth = account()
        val records = (1..5).map { SyncRecord("review", "$it/pt/casa", 1_000L + it, data = buildJsonObject { put("rating", it) }, deviceId = "d") }
        push(auth, "d", *records.toTypedArray())
        val first = pull(auth, 0, limit = 2)
        assertEquals(listOf("1/pt/casa", "2/pt/casa"), first.records.map { it.key })
        assertTrue(first.more)
        val rest = generateSequence(first) { if (it.more) pull(auth, it.seq, limit = 2) else null }.toList()
        assertEquals(records.map { it.key }, rest.flatMap { p -> p.records.map { it.key } })
        assertEquals(false, rest.last().more)
    }

    @Test
    fun aClockAheadIsHeldToTheServers() {
        val auth = account()
        val tomorrow = System.currentTimeMillis() + 86_400_000
        push(auth, "d", term(tomorrow, 2, tomorrow, "x", "d"))
        assertTrue(pull(auth, 0).records.single().updatedAt < System.currentTimeMillis() + 6 * 60_000, "no more than minutes ahead")
    }

    @Test
    fun syncNeedsASubscriptionAndKeepsAccountsApart() {
        val unpaid = account(paid = false)
        mvc.post("/api/v1/sync/push") { header("Authorization", unpaid); contentType = MediaType.APPLICATION_JSON; content = """{"deviceId":"d","records":[]}""" }
            .andExpect { status { isPaymentRequired() } }
        mvc.get("/api/v1/sync/pull") { header("Authorization", unpaid) }.andExpect { status { isPaymentRequired() } }

        val one = account()
        val other = account()
        push(one, "d", term(1_000, 1, 1_000, "mine", "d"))
        assertEquals(emptyList(), pull(other, 0).records)
    }
}
