package com.tayra.languages.backend.billing

import com.tayra.languages.sync.SyncJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Paddle Billing's webhooks, for subscriptions bought on the web and the desktop. Each is signed:
 * `Paddle-Signature: ts=<unix seconds>;h1=<hex HMAC-SHA256 of "ts:body" with the endpoint's secret>`.
 * The checkout carries the account in `custom_data.user_id`, which every later event repeats.
 */
@Service
class Paddle(
    private val billing: Billing,
    private val clock: Clock,
    @Value("\${tayra.billing.paddle.webhook-secret}") private val secret: String,
) {
    /** Takes a webhook's [body] as sent, signed by [signature]; whether it changed a subscription. */
    fun notified(signature: String?, body: String): Boolean {
        if (secret.isEmpty()) throw ProviderNotSetUp("Paddle")
        verify(signature ?: throw BadNotification("No Paddle-Signature"), body)
        val event = runCatching { SyncJson.parseToJsonElement(body) as JsonObject }.getOrNull() ?: throw BadNotification("Not JSON")
        val type = event.text("event_type").orEmpty()
        if (!type.startsWith("subscription.")) return false
        val data = event["data"] as? JsonObject ?: throw BadNotification("No data")
        val status = data.text("status") ?: throw BadNotification("No status")
        val at = event.text("occurred_at")?.let(Instant::parse) ?: clock.instant()
        val until = when (status) {
            // Past due keeps it while Paddle tries the payment again.
            "active", "trialing", "past_due" -> (data["current_billing_period"] as? JsonObject)?.text("ends_at")?.let(Instant::parse)
            else -> null
        }
        val user = (data["custom_data"] as? JsonObject)?.text("user_id")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val product = ((data["items"] as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.let { it["price"] as? JsonObject }?.text("id").orEmpty()
        return billing.record(SubscriptionState("paddle", data.text("id") ?: throw BadNotification("No subscription id"), user, product, status, until, at))
    }

    private fun verify(signature: String, body: String) {
        val parts = signature.split(';').mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }
        val ts = parts.firstOrNull { it.first == "ts" }?.second?.toLongOrNull() ?: throw BadNotification("No signature time")
        if (Duration.between(Instant.ofEpochSecond(ts), clock.instant()).abs() > TOLERANCE) throw BadNotification("Signature too old")
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secret.toByteArray(), "HmacSHA256")) }
        val expected = HexFormat.of().formatHex(mac.doFinal("$ts:$body".toByteArray()))
        // Paddle may send more than one h1 while a secret is being rotated.
        if (parts.none { it.first == "h1" && MessageDigest.isEqual(it.second.toByteArray(), expected.toByteArray()) }) throw BadNotification("Bad signature")
    }

    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

    private companion object {
        val TOLERANCE: Duration = Duration.ofMinutes(5)
    }
}

@RestController
class PaddleController(private val paddle: Paddle) {
    @PostMapping("/api/v1/billing/paddle")
    fun notified(@RequestHeader("Paddle-Signature", required = false) signature: String?, @RequestBody body: String) {
        paddle.notified(signature, body)
    }
}
