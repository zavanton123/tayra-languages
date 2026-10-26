package com.tayra.languages.backend.auth

import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.CodeRequest
import com.tayra.languages.sync.ForgotRequest
import com.tayra.languages.sync.ResetRequest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.UUID

class WrongCode : ApiException(400, ApiErrors.WRONG_CODE, "That code is wrong or has expired; ask for a new one")

/**
 * Six-digit codes sent by email, typed into the app: one confirms the account's address, another
 * lets a forgotten password be replaced. A code is kept only as its hash, lasts a short while, and
 * allows a few tries; asking again replaces it.
 */
@Service
class EmailCodes(
    private val jdbc: JdbcClient,
    private val accounts: Accounts,
    private val mailer: Mailer,
    private val passwords: PasswordEncoder,
    private val clock: Clock,
) {
    private val random = SecureRandom()

    @Transactional
    fun sendVerification(userId: UUID) {
        val user = accounts.byId(userId) ?: throw SignedOut()
        if (user.emailVerified) return
        val code = issue(userId, VERIFY, Duration.ofHours(24))
        mailer.send(user.email, "Your Tayra Languages code: $code", "Enter $code in Tayra Languages to confirm your email address.\n\nThe code works for 24 hours.")
    }

    // A wrong code's spent try is kept, though the call fails.
    @Transactional(noRollbackFor = [WrongCode::class])
    fun verify(userId: UUID, code: String) {
        spend(userId, VERIFY, code)
        jdbc.sql("UPDATE users SET email_verified = TRUE WHERE id = ?").param(userId).update()
    }

    /** Sends a reset code when the account exists; says nothing either way, so no one learns who has one. */
    @Transactional
    fun sendReset(email: String) {
        val user = accounts.byEmail(email.trim().lowercase()) ?: return
        val code = issue(user.id, RESET, Duration.ofHours(1))
        mailer.send(user.email, "Reset your Tayra Languages password: $code", "Enter $code in Tayra Languages to choose a new password.\n\nThe code works for an hour. If you did not ask for it, ignore this email.")
    }

    /** Sets the new password and signs every device out; the address is confirmed too, as the code reached it. */
    @Transactional(noRollbackFor = [WrongCode::class])
    fun reset(request: ResetRequest) {
        if (request.password.length < 8) throw InvalidRequest("The password needs at least 8 characters")
        val user = accounts.byEmail(request.email.trim().lowercase()) ?: throw WrongCode()
        spend(user.id, RESET, request.code)
        jdbc.sql("UPDATE users SET password_hash = ?, email_verified = TRUE WHERE id = ?").params(passwords.encode(request.password)!!, user.id).update()
        jdbc.sql("DELETE FROM refresh_tokens WHERE user_id = ?").param(user.id).update()
    }

    private fun issue(userId: UUID, purpose: String, lasts: Duration): String {
        val code = (random.nextInt(1_000_000)).toString().padStart(6, '0')
        jdbc.sql("DELETE FROM email_codes WHERE user_id = ? AND purpose = ?").params(userId, purpose).update()
        jdbc.sql("INSERT INTO email_codes(id, user_id, purpose, code_hash, expires_at) VALUES (?, ?, ?, ?, ?)")
            .params(UUID.randomUUID(), userId, purpose, hash(userId, code), clock.instant().plus(lasts).toTimestamp()).update()
        return code
    }

    /** Uses up the code if it is right; a wrong one costs a try, and the last try spends the code. */
    private fun spend(userId: UUID, purpose: String, code: String) {
        val row = jdbc.sql("SELECT id, code_hash, expires_at, attempts FROM email_codes WHERE user_id = ? AND purpose = ? FOR UPDATE")
            .params(userId, purpose)
            .query { rs, _ -> Triple(rs.getObject("id", UUID::class.java), rs.getString("code_hash"), rs.getTimestamp("expires_at").toInstant() to rs.getInt("attempts")) }
            .optional().orElse(null) ?: throw WrongCode()
        val (id, stored, rest) = row
        val (expires, attempts) = rest
        val right = MessageDigest.isEqual(stored.toByteArray(), hash(userId, code.trim()).toByteArray())
        if (right && expires.isAfter(clock.instant())) {
            jdbc.sql("DELETE FROM email_codes WHERE id = ?").param(id).update()
            return
        }
        if (attempts + 1 >= MAX_ATTEMPTS) jdbc.sql("DELETE FROM email_codes WHERE id = ?").param(id).update()
        else jdbc.sql("UPDATE email_codes SET attempts = attempts + 1 WHERE id = ?").param(id).update()
        throw WrongCode()
    }

    // Salted with the account, so the same code for two accounts hashes apart.
    private fun hash(userId: UUID, code: String): String =
        MessageDigest.getInstance("SHA-256").digest("$userId:$code".toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val VERIFY = "verify"
        const val RESET = "reset"
        const val MAX_ATTEMPTS = 5
    }
}

@RestController
class EmailCodesController(private val codes: EmailCodes) {
    @PostMapping("/api/v1/me/email/send-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendVerification(@AuthenticationPrincipal jwt: Jwt) = codes.sendVerification(UUID.fromString(jwt.subject))

    @PostMapping("/api/v1/me/email/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verify(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: CodeRequest) = codes.verify(UUID.fromString(jwt.subject), request.code)

    @PostMapping("/api/v1/auth/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun forgot(@RequestBody request: ForgotRequest) = codes.sendReset(request.email)

    @PostMapping("/api/v1/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reset(@RequestBody request: ResetRequest) = codes.reset(request)
}
