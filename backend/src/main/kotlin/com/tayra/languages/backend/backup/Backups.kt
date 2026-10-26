package com.tayra.languages.backend.backup

import com.tayra.languages.backend.auth.ApiException
import com.tayra.languages.backend.auth.Entitlements
import com.tayra.languages.backend.auth.InvalidRequest
import com.tayra.languages.backend.auth.user
import kotlinx.serialization.Serializable
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.Clock
import java.util.UUID

@Serializable
data class CloudBackup(val id: String, val name: String, val deviceName: String, val createdAt: Long, val sizeBytes: Long)

class BackupNotFound : ApiException(404, "not_found", "No such backup")

class BackupTooLarge(limitMb: Long) : ApiException(413, "too_large", "Backups can be up to $limitMb MB")

/**
 * The account's library files in the cloud: a device uploads the backup it makes anyway, and any
 * device can list and download them to restore. Uploading needs an active subscription; listing and
 * downloading do not, so an account that lapsed can still take its library back.
 */
@Service
class Backups(
    private val jdbc: JdbcClient,
    private val store: BackupStore,
    private val entitlements: Entitlements,
    private val clock: Clock,
    @Value("\${tayra.backups.keep}") private val keep: Int,
    @Value("\${tayra.backups.max-megabytes}") private val maxMegabytes: Long,
) {
    fun list(userId: UUID): List<CloudBackup> =
        jdbc.sql("SELECT id, name, device_name, created_at, size_bytes FROM backups WHERE user_id = ? ORDER BY created_at DESC").param(userId)
            .query { rs, _ -> CloudBackup(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toEpochMilli(), rs.getLong(5)) }
            .list()

    /** Keeps [content], a backup file of [size] bytes, and lets the oldest go beyond the newest few. */
    fun upload(userId: UUID, name: String, deviceName: String, content: InputStream, size: Long): CloudBackup {
        entitlements.require(userId)
        if (size <= 0) throw InvalidRequest("The upload is empty")
        if (size > maxMegabytes * 1024 * 1024) throw BackupTooLarge(maxMegabytes)
        val stream = BufferedInputStream(content)
        // Only library files: a backup is an SQLite file, which says so in its first bytes.
        stream.mark(SQLITE_HEADER.size)
        val header = stream.readNBytes(SQLITE_HEADER.size)
        if (!header.contentEquals(SQLITE_HEADER)) throw InvalidRequest("This is not a Tayra Languages backup")
        stream.reset()

        val id = UUID.randomUUID()
        val key = "users/$userId/backups/$id"
        store.put(key, stream, size)
        val now = clock.instant()
        jdbc.sql("INSERT INTO backups(id, user_id, name, device_name, created_at, size_bytes, object_key) VALUES (?, ?, ?, ?, ?, ?, ?)")
            .params(id, userId, name.take(200), deviceName.take(100), java.sql.Timestamp.from(now), size, key).update()
        prune(userId)
        return CloudBackup(id.toString(), name, deviceName, now.toEpochMilli(), size)
    }

    fun download(userId: UUID, id: UUID): Pair<CloudBackup, InputStream> {
        val (backup, key) = find(userId, id)
        return backup to store.open(key)
    }

    fun delete(userId: UUID, id: UUID) {
        val (_, key) = find(userId, id)
        store.delete(key)
        jdbc.sql("DELETE FROM backups WHERE id = ?").param(id).update()
    }

    /** Removes every file of the account, before the account itself goes. */
    fun deleteAll(userId: UUID) {
        keys(userId).forEach(store::delete)
        jdbc.sql("DELETE FROM backups WHERE user_id = ?").param(userId).update()
    }

    private fun prune(userId: UUID) {
        val old = jdbc.sql("SELECT id, object_key FROM backups WHERE user_id = ? ORDER BY created_at DESC OFFSET ?").params(userId, keep)
            .query { rs, _ -> rs.getObject(1, UUID::class.java) to rs.getString(2) }.list()
        for ((id, key) in old) {
            store.delete(key)
            jdbc.sql("DELETE FROM backups WHERE id = ?").param(id).update()
        }
    }

    private fun find(userId: UUID, id: UUID): Pair<CloudBackup, String> =
        jdbc.sql("SELECT id, name, device_name, created_at, size_bytes, object_key FROM backups WHERE id = ? AND user_id = ?").params(id, userId)
            .query { rs, _ -> CloudBackup(rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant().toEpochMilli(), rs.getLong(5)) to rs.getString(6) }
            .optional().orElseThrow { BackupNotFound() }

    private fun keys(userId: UUID): List<String> =
        jdbc.sql("SELECT object_key FROM backups WHERE user_id = ?").param(userId).query(String::class.java).list().filterNotNull()

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray()
    }
}

@RestController
class BackupsController(private val backups: Backups) {
    @GetMapping("/api/v1/backups")
    fun list(@AuthenticationPrincipal jwt: Jwt) = backups.list(jwt.user)

    /** The body is the backup file itself, as bytes. */
    @PostMapping("/api/v1/backups", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam name: String,
        @RequestParam(required = false, defaultValue = "") device: String,
        @RequestHeader(HttpHeaders.CONTENT_LENGTH) length: Long,
        request: jakarta.servlet.http.HttpServletRequest,
    ) = backups.upload(jwt.user, name, device, request.inputStream, length)

    @GetMapping("/api/v1/backups/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ResponseEntity<InputStreamResource> {
        val (backup, content) = backups.download(jwt.user, id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${backup.name.replace("\"", "")}\"")
            .contentLength(backup.sizeBytes)
            .body(InputStreamResource(content))
    }

    @DeleteMapping("/api/v1/backups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) = backups.delete(jwt.user, id)
}
