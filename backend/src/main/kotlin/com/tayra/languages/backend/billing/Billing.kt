package com.tayra.languages.backend.billing

import com.tayra.languages.sync.ApiError
import com.tayra.languages.sync.ApiErrors
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** A subscription as its provider reported it at [eventAt]. */
data class SubscriptionState(
    val source: String,
    val externalId: String,
    /** The account it was bought for, when the event says; an event that does not keeps the one known. */
    val userId: UUID?,
    val product: String,
    val status: String,
    /** Until when it lets the account sync; null when it does not. */
    val activeUntil: Instant?,
    val eventAt: Instant,
)

/** Thrown when a provider's notification cannot be trusted or read: answered 400, so the provider sends it again. */
class BadNotification(message: String) : RuntimeException(message)

/** Thrown for a provider not set up on this service: answered 404. */
class ProviderNotSetUp(provider: String) : RuntimeException("$provider is not set up")

/**
 * Keeps each subscription as its provider last reported it, and each account's subscription columns
 * to the latest end among its subscriptions, which is what [com.tayra.languages.backend.auth.Entitlements] reads.
 */
@Service
class Billing(private val jdbc: JdbcClient, private val clock: Clock) {

    /** Records [state] unless a later event about the subscription came first; whether it did. */
    @Transactional
    fun record(state: SubscriptionState): Boolean {
        val stored = jdbc.sql("SELECT user_id, event_at FROM subscriptions WHERE source = ? AND external_id = ? FOR UPDATE")
            .params(state.source, state.externalId)
            .query { rs, _ -> (rs.getObject(1) as UUID?) to rs.getTimestamp(2).toInstant() }
            .optional().orElse(null)
        if (stored != null && stored.second.isAfter(state.eventAt)) return false
        val user = state.userId ?: stored?.first
        jdbc.sql(
            """INSERT INTO subscriptions(source, external_id, user_id, product, status, active_until, event_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT (source, external_id) DO UPDATE SET user_id = EXCLUDED.user_id, product = EXCLUDED.product, status = EXCLUDED.status,
               active_until = EXCLUDED.active_until, event_at = EXCLUDED.event_at, updated_at = EXCLUDED.updated_at""",
        ).params(listOf(state.source, state.externalId, user, state.product, state.status, state.activeUntil?.let(Timestamp::from),
            Timestamp.from(state.eventAt), Timestamp.from(clock.instant()))).update()
        if (stored?.first != null && stored.first != user) refresh(stored.first!!)
        user?.let(::refresh)
        return true
    }

    /** The account a subscription is tied to, if any. */
    fun owner(source: String, externalId: String): UUID? =
        jdbc.sql("SELECT user_id FROM subscriptions WHERE source = ? AND external_id = ?").params(source, externalId)
            .query { rs, _ -> rs.getObject(1) as UUID? }.optional().orElse(null)

    /** Sets the account's subscription to the one of its subscriptions that runs longest. */
    private fun refresh(userId: UUID) {
        data class Best(val source: String, val status: String, val until: Timestamp?)
        val best = jdbc.sql("SELECT source, status, active_until FROM subscriptions WHERE user_id = ? ORDER BY active_until DESC NULLS LAST, event_at DESC LIMIT 1")
            .param(userId).query { rs, _ -> Best(rs.getString(1), rs.getString(2), rs.getTimestamp(3)) }.optional().orElse(null) ?: return
        jdbc.sql("UPDATE users SET subscription_source = ?, subscription_status = ?, subscription_active_until = ? WHERE id = ?")
            .params(listOf(best.source, best.status, best.until, userId)).update()
    }
}

/** How the providers are answered when a notification is refused. */
@RestControllerAdvice
class BillingErrors {
    @ExceptionHandler(BadNotification::class)
    fun bad(e: BadNotification) = ResponseEntity.badRequest().body(ApiError(ApiErrors.INVALID_REQUEST, e.message.orEmpty()))

    @ExceptionHandler(ProviderNotSetUp::class)
    fun notSetUp(e: ProviderNotSetUp) = ResponseEntity.status(404).body(ApiError(ApiErrors.NOT_FOUND, e.message.orEmpty()))
}
