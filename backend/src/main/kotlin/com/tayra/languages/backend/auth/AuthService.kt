package com.tayra.languages.backend.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Signing up and in with an email and a password, and keeping a device signed in: a short-lived
 * access token for each request, and a refresh token per device, kept only as its hash and
 * replaced every time it is used.
 */
@Service
class AuthService(
    private val accounts: Accounts,
    private val passwords: PasswordEncoder,
    private val jwt: JwtEncoder,
    private val codes: EmailCodes,
    private val clock: Clock,
    @Value("\${tayra.auth.access-token-minutes}") private val accessMinutes: Long,
    @Value("\${tayra.auth.refresh-token-days}") private val refreshDays: Long,
) {
    private val random = SecureRandom()
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    // The hash of a password nobody has, so a missing account costs the same check as a wrong password.
    private val unusableHash by lazy { passwords.encode(newRefreshToken())!! }

    @Transactional
    fun register(request: RegisterRequest): Tokens {
        val email = normalized(request.email)
        if (request.password.length < MIN_PASSWORD) throw InvalidRequest("The password needs at least $MIN_PASSWORD characters")
        requireDevice(request.device)
        val user = accounts.create(email, passwords.encode(request.password), clock.instant())
        // The confirmation code goes out at once; mail being down must not undo the sign-up, as it can be sent again.
        runCatching { codes.sendVerification(user.id) }.onFailure { log.warn("Could not send the confirmation code to {}", email, it) }
        return signIn(user, request.device)
    }

    @Transactional
    fun login(request: LoginRequest): Tokens {
        requireDevice(request.device)
        val user = accounts.byEmail(normalized(request.email))
        val ok = passwords.matches(request.password, user?.passwordHash ?: unusableHash)
        if (user?.passwordHash == null || !ok) throw BadCredentials()
        return signIn(user, request.device)
    }

    /** New tokens for the device whose refresh token this is; the token given is spent. */
    @Transactional
    fun refresh(request: RefreshRequest): Tokens {
        val now = clock.instant()
        val session = accounts.sessionByToken(hash(request.refreshToken))?.takeIf { !it.revoked && it.expiresAt.isAfter(now) } ?: throw SignedOut()
        val user = accounts.byId(session.userId) ?: throw SignedOut()
        val refresh = newRefreshToken()
        accounts.rotate(session.id, hash(refresh), now, now.plus(Duration.ofDays(refreshDays)))
        return tokens(user, session.deviceId, refresh)
    }

    @Transactional
    fun logout(request: RefreshRequest) {
        accounts.sessionByToken(hash(request.refreshToken))?.let { accounts.endSession(it.id) }
    }

    fun signIn(user: User, device: DeviceInfo): Tokens {
        val now = clock.instant()
        val refresh = newRefreshToken()
        accounts.startSession(user.id, device, hash(refresh), now, now.plus(Duration.ofDays(refreshDays)))
        return tokens(user, device.id, refresh)
    }

    private fun tokens(user: User, deviceId: String, refresh: String): Tokens {
        val now = clock.instant()
        val expires = Duration.ofMinutes(accessMinutes)
        val claims = JwtClaimsSet.builder()
            .issuer(ISSUER).subject(user.id.toString()).issuedAt(now).expiresAt(now.plus(expires))
            .claim(DEVICE_CLAIM, deviceId)
            .build()
        val access = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue
        return Tokens(access, refresh, expires.seconds, Account(user.id.toString(), user.email, user.emailVerified))
    }

    private fun normalized(email: String): String {
        val e = email.trim().lowercase()
        if (!EMAIL.matches(e)) throw InvalidRequest("That is not an email address")
        return e
    }

    private fun requireDevice(device: DeviceInfo) {
        if (device.id.isBlank() || device.id.length > 64) throw InvalidRequest("The device needs an id")
    }

    private fun newRefreshToken(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MIN_PASSWORD = 8
        const val ISSUER = "tayra-languages"
        const val DEVICE_CLAIM = "device"
        val EMAIL = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")
    }
}
