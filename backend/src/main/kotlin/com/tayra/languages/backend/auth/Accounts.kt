package com.tayra.languages.backend.auth

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

/** An account as stored. */
data class User(val id: UUID, val email: String, val emailVerified: Boolean, val passwordHash: String?)

/** A signed-in device as stored. */
data class Session(val id: UUID, val userId: UUID, val deviceId: String, val expiresAt: Instant, val revoked: Boolean)

@Repository
class Accounts(private val jdbc: JdbcClient) {

    /** Makes the account; throws [EmailTaken] when the email has one. */
    fun create(email: String, passwordHash: String?, now: Instant): User {
        val user = User(UUID.randomUUID(), email, emailVerified = false, passwordHash = passwordHash)
        try {
            jdbc.sql("INSERT INTO users(id, email, password_hash, created_at) VALUES (?, ?, ?, ?)")
                .params(listOf(user.id, email, passwordHash, now.toTimestamp())).update()
        } catch (e: DuplicateKeyException) {
            throw EmailTaken()
        }
        return user
    }

    fun byEmail(email: String): User? = jdbc.sql("SELECT * FROM users WHERE email = ?").param(email).query(::user).optional().orElse(null)

    fun byId(id: UUID): User? = jdbc.sql("SELECT * FROM users WHERE id = ?").param(id).query(::user).optional().orElse(null)

    /** Keeps a new sign-in for the device, replacing the one it had: one sign-in per device. */
    fun startSession(userId: UUID, device: DeviceInfo, tokenHash: String, now: Instant, expiresAt: Instant) {
        jdbc.sql("DELETE FROM refresh_tokens WHERE user_id = ? AND device_id = ?").params(userId, device.id).update()
        jdbc.sql(
            """INSERT INTO refresh_tokens(id, user_id, token_hash, device_id, device_name, platform, created_at, last_used_at, expires_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        ).params(UUID.randomUUID(), userId, tokenHash, device.id, device.name, device.platform, now.toTimestamp(), now.toTimestamp(), expiresAt.toTimestamp()).update()
    }

    fun sessionByToken(tokenHash: String): Session? =
        jdbc.sql("SELECT id, user_id, device_id, expires_at, revoked FROM refresh_tokens WHERE token_hash = ?").param(tokenHash)
            .query { rs, _ -> Session(rs.getObject("id", UUID::class.java), rs.getObject("user_id", UUID::class.java), rs.getString("device_id"), rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("revoked")) }
            .optional().orElse(null)

    /** Swaps the session's refresh token for a new one: the old one never works again. */
    fun rotate(sessionId: UUID, newHash: String, now: Instant, expiresAt: Instant) {
        jdbc.sql("UPDATE refresh_tokens SET token_hash = ?, last_used_at = ?, expires_at = ? WHERE id = ?")
            .params(newHash, now.toTimestamp(), expiresAt.toTimestamp(), sessionId).update()
    }

    fun endSession(sessionId: UUID) {
        jdbc.sql("DELETE FROM refresh_tokens WHERE id = ?").param(sessionId).update()
    }

    private fun user(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) =
        User(rs.getObject("id", UUID::class.java), rs.getString("email"), rs.getBoolean("email_verified"), rs.getString("password_hash"))
}

internal fun Instant.toTimestamp(): java.sql.Timestamp = java.sql.Timestamp.from(this)
