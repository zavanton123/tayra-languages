package com.tayra.languages.backend

import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Subscriptions bought through Paddle: its signed webhooks start, renew and end an account's sync. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.billing.paddle.webhook-secret=pdl_ntfset_test_secret"])
class PaddleTest(@Autowired private val mvc: MockMvc) {

    private fun account(): Pair<String, String> {
        val body = mvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"paddle-${UUID.randomUUID()}@example.com","password":"a password","device":{"id":"d"}}"""
        }.andReturn().response.contentAsString
        val tokens = SyncJson.decodeFromString(Tokens.serializer(), body)
        return tokens.user.id to "Bearer ${tokens.accessToken}"
    }

    private fun event(type: String, subscription: String, user: String?, status: String, endsAt: Instant, occurredAt: Instant) = """
        {"event_id":"evt_${UUID.randomUUID()}","event_type":"$type","occurred_at":"$occurredAt","notification_id":"ntf_1",
         "data":{"id":"$subscription","status":"$status","customer_id":"ctm_1",
           "custom_data":${user?.let { """{"user_id":"$it"}""" } ?: "null"},
           "items":[{"price":{"id":"pri_yearly"},"quantity":1}],
           "current_billing_period":{"starts_at":"${endsAt.minus(365, ChronoUnit.DAYS)}","ends_at":"$endsAt"}}}
    """.trimIndent()

    private fun signed(body: String, secret: String = "pdl_ntfset_test_secret", at: Instant = Instant.now()): String {
        val ts = at.epochSecond
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")) }
        return "ts=$ts;h1=${HexFormat.of().formatHex(mac.doFinal("$ts:$body".toByteArray()))}"
    }

    private fun send(body: String, signature: String = signed(body)) = mvc.post("/api/v1/billing/paddle") {
        header("Paddle-Signature", signature)
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun plan(auth: String) = mvc.get("/api/v1/me") { header("Authorization", auth) }

    @Test
    fun aSubscriptionStartsAndEndsTheAccountsSync() {
        val (user, auth) = account()
        val subscription = "sub_${UUID.randomUUID()}"
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val ends = now.plus(365, ChronoUnit.DAYS)
        mvc.get("/api/v1/sync/pull") { header("Authorization", auth) }.andExpect { status { isPaymentRequired() } }

        send(event("subscription.created", subscription, user, "active", ends, now)).andExpect { status { isOk() } }
        plan(auth).andExpect {
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.source") { value("paddle") }
            jsonPath("$.subscription.activeUntil") { value(ends.toEpochMilli()) }
        }
        mvc.get("/api/v1/sync/pull") { header("Authorization", auth) }.andExpect { status { isOk() } }

        // Canceled; an update from before it, arriving late, does not bring it back.
        send(event("subscription.canceled", subscription, user, "canceled", ends, now.plusSeconds(60))).andExpect { status { isOk() } }
        send(event("subscription.updated", subscription, null, "active", ends, now.plusSeconds(30))).andExpect { status { isOk() } }
        plan(auth).andExpect { jsonPath("$.subscription.active") { value(false) } }
        mvc.get("/api/v1/sync/pull") { header("Authorization", auth) }.andExpect { status { isPaymentRequired() } }
    }

    @Test
    fun onlyPaddlesSignatureIsTaken() {
        val (user, auth) = account()
        val body = event("subscription.created", "sub_${UUID.randomUUID()}", user, "active", Instant.now().plus(30, ChronoUnit.DAYS), Instant.now())
        send(body, signed(body, secret = "someone else's")).andExpect { status { isBadRequest() } }
        send(body, signed(body, at = Instant.now().minus(1, ChronoUnit.HOURS))).andExpect { status { isBadRequest() } }
        send(body, "").andExpect { status { isBadRequest() } }
        send(body.replace("active", "trialing"), signed(body)).andExpect { status { isBadRequest() } }
        plan(auth).andExpect { jsonPath("$.subscription.active") { value(false) } }

        // Events that are not about subscriptions are taken and passed over.
        val other = """{"event_type":"transaction.completed","occurred_at":"${Instant.now()}","data":{"id":"txn_1"}}"""
        send(other).andExpect { status { isOk() } }
    }
}
