package com.tayra.languages.backend.auth

import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.Device
import com.tayra.languages.sync.Me
import com.tayra.languages.sync.Subscription
import com.tayra.languages.backend.backup.Backups
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.util.UUID

class SubscriptionRequired : ApiException(402, ApiErrors.SUBSCRIPTION_REQUIRED, "Sync and cloud backups need an active subscription")

/**
 * Who may sync and back up: an account whose subscription runs past now, or one of the
 * [freeEmails], the accounts given it without paying (the owner's own, before billing exists).
 */
@Service
class Entitlements(
    private val jdbc: JdbcClient,
    private val clock: Clock,
    @Value("\${tayra.free-emails}") freeEmails: String,
) {
    private val free = freeEmails.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun of(userId: UUID): Subscription {
        val row = jdbc.sql("SELECT email, subscription_active_until, subscription_source FROM users WHERE id = ?").param(userId)
            .query { rs, _ -> Triple(rs.getString(1), rs.getTimestamp(2)?.toInstant(), rs.getString(3)) }
            .optional().orElse(null) ?: throw SignedOut()
        val (email, until, source) = row
        if (email in free) return Subscription(active = true, source = "free")
        val active = until != null && until.isAfter(clock.instant())
        return Subscription(active, until?.toEpochMilli(), source)
    }

    /** Refuses with 402 unless the account may sync. */
    fun require(userId: UUID) {
        if (!of(userId).active) throw SubscriptionRequired()
    }
}

@Service
class AccountService(private val jdbc: JdbcClient, private val entitlements: Entitlements, private val backups: Backups) {

    fun me(userId: UUID): Me {
        data class Row(val email: String, val verified: Boolean, val password: Boolean, val google: Boolean, val apple: Boolean)
        val row = jdbc.sql("SELECT email, email_verified, password_hash IS NOT NULL, google_sub IS NOT NULL, apple_sub IS NOT NULL FROM users WHERE id = ?")
            .param(userId).query { rs, _ -> Row(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4), rs.getBoolean(5)) }
            .optional().orElse(null) ?: throw SignedOut()
        return Me(userId.toString(), row.email, row.verified, row.password, listOfNotNull("google".takeIf { row.google }, "apple".takeIf { row.apple }), entitlements.of(userId))
    }

    fun devices(userId: UUID, currentDevice: String?): List<Device> =
        jdbc.sql("SELECT device_id, device_name, platform, last_used_at FROM refresh_tokens WHERE user_id = ? ORDER BY last_used_at DESC").param(userId)
            .query { rs, _ ->
                val id = rs.getString(1)
                Device(id, rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toEpochMilli(), current = id == currentDevice)
            }.list()

    /** Signs the device out; its access token lapses within minutes, and it cannot renew it. */
    fun signOut(userId: UUID, deviceId: String) {
        jdbc.sql("DELETE FROM refresh_tokens WHERE user_id = ? AND device_id = ?").params(userId, deviceId).update()
    }

    /** Deletes the account and everything kept for it, its backup files first; every device is signed out with it. */
    @Transactional
    fun delete(userId: UUID) {
        backups.deleteAll(userId)
        jdbc.sql("DELETE FROM users WHERE id = ?").param(userId).update()
    }
}

@RestController
class AccountController(private val accounts: AccountService) {
    @GetMapping("/api/v1/me")
    fun me(@AuthenticationPrincipal jwt: Jwt) = accounts.me(jwt.user)

    @GetMapping("/api/v1/me/devices")
    fun devices(@AuthenticationPrincipal jwt: Jwt) = accounts.devices(jwt.user, jwt.getClaimAsString("device"))

    @DeleteMapping("/api/v1/me/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun signOut(@AuthenticationPrincipal jwt: Jwt, @PathVariable deviceId: String) = accounts.signOut(jwt.user, deviceId)

    @DeleteMapping("/api/v1/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt) = accounts.delete(jwt.user)
}

/** The account a request's access token was issued to. */
val Jwt.user: UUID get() = UUID.fromString(subject)
