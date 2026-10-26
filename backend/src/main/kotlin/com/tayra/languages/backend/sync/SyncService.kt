package com.tayra.languages.backend.sync

import com.tayra.languages.backend.auth.Entitlements
import com.tayra.languages.backend.auth.InvalidRequest
import com.tayra.languages.backend.auth.user
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.json.JsonObject
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.ResultSet
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Sync's two calls. A push merges each record sent into the account's version of it, by the same
 * rules every device merges by, and gives each one that changed the account's next change number. A
 * pull returns the records changed after a change number, oldest first. Both need an active
 * subscription.
 */
@Service
class SyncService(private val jdbc: JdbcClient, private val entitlements: Entitlements, private val clock: Clock, metrics: MeterRegistry) {
    private val pushed = metrics.counter("tayra.sync.pushed")
    private val changed = metrics.counter("tayra.sync.changed")
    /** Records sent that lost to the account's version, wholly or in part: how often devices disagree. */
    private val conflicts = metrics.counter("tayra.sync.conflicts")
    private val pulled = metrics.counter("tayra.sync.pulled")

    @Transactional
    fun push(userId: UUID, request: PushRequest): PushResponse {
        entitlements.require(userId)
        if (request.records.size > MAX_PUSH) throw InvalidRequest("At most $MAX_PUSH records a push")
        // One push at a time per account: the change numbers are handed out in order.
        jdbc.sql("INSERT INTO user_seq(user_id) VALUES (?) ON CONFLICT DO NOTHING").param(userId).update()
        var seq = jdbc.sql("SELECT last_seq FROM user_seq WHERE user_id = ? FOR UPDATE").param(userId).query(Long::class.java).single()
        // A device whose clock runs ahead would win every merge; its times are held to the server's.
        val latest = clock.millis() + MAX_AHEAD.toMillis()
        for (sent in request.records) {
            val incoming = sent.copy(deviceId = sent.deviceId.ifEmpty { request.deviceId }, updatedAt = minOf(sent.updatedAt, latest))
            val stored = jdbc.sql("SELECT kind, key, updated_at, deleted, device_id, data FROM records WHERE user_id = ? AND kind = ? AND key = ? FOR UPDATE")
                .params(userId, incoming.kind, incoming.key).query(::record).optional().orElse(null)
            val merged = if (stored == null) incoming else SyncMerge.merge(stored, incoming)
            pushed.increment()
            if (stored != null && !same(merged, incoming)) conflicts.increment()
            if (merged == stored) continue
            changed.increment()
            seq++
            jdbc.sql(
                """INSERT INTO records(user_id, kind, key, seq, updated_at, deleted, device_id, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                   ON CONFLICT (user_id, kind, key) DO UPDATE SET seq = EXCLUDED.seq, updated_at = EXCLUDED.updated_at,
                   deleted = EXCLUDED.deleted, device_id = EXCLUDED.device_id, data = EXCLUDED.data""",
            ).params(listOf(userId, merged.kind, merged.key, seq, merged.updatedAt, merged.deleted, merged.deviceId, merged.data?.toString())).update()
        }
        jdbc.sql("UPDATE user_seq SET last_seq = ? WHERE user_id = ?").params(seq, userId).update()
        return PushResponse(seq, clock.millis())
    }

    fun pull(userId: UUID, since: Long, limit: Int): PullResponse {
        entitlements.require(userId)
        val size = limit.coerceIn(1, MAX_PULL)
        val rows = jdbc.sql("SELECT kind, key, updated_at, deleted, device_id, data, seq FROM records WHERE user_id = ? AND seq > ? ORDER BY seq LIMIT ?")
            .params(userId, since, size + 1)
            .query { rs, n -> record(rs, n) to rs.getLong("seq") }.list()
        val page = rows.take(size)
        pulled.increment(page.size.toDouble())
        return PullResponse(page.map { it.first }, page.lastOrNull()?.second ?: since, more = rows.size > size)
    }

    /** The same version, whichever device it came from. */
    private fun same(a: SyncRecord, b: SyncRecord) = a.updatedAt == b.updatedAt && a.deleted == b.deleted && a.data == b.data

    private fun record(rs: ResultSet, @Suppress("UNUSED_PARAMETER") row: Int) = SyncRecord(
        kind = rs.getString("kind"),
        key = rs.getString("key"),
        updatedAt = rs.getLong("updated_at"),
        deleted = rs.getBoolean("deleted"),
        data = rs.getString("data")?.let { SyncJson.decodeFromString(JsonObject.serializer(), it) },
        deviceId = rs.getString("device_id"),
    )

    private companion object {
        const val MAX_PUSH = 2_000
        const val MAX_PULL = 2_000
        val MAX_AHEAD: Duration = Duration.ofMinutes(5)
    }
}

@RestController
class SyncController(private val sync: SyncService) {
    @PostMapping("/api/v1/sync/push")
    fun push(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: PushRequest) = sync.push(jwt.user, request)

    @GetMapping("/api/v1/sync/pull")
    fun pull(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "0") since: Long, @RequestParam(defaultValue = "1000") limit: Int) =
        sync.pull(jwt.user, since, limit)
}
