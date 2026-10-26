package com.tayra.languages.core.data.account

import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.sync.SyncTransport
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountInfo
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.account.AccountRepository
import com.tayra.languages.core.domain.account.CloudBackupInfo
import com.tayra.languages.core.domain.account.CloudBackupStore
import com.tayra.languages.core.domain.account.Plan
import com.tayra.languages.core.domain.account.SignedInDevice
import com.tayra.languages.sync.ApiError
import com.tayra.languages.sync.ApiErrors
import com.tayra.languages.sync.CloudBackup
import com.tayra.languages.sync.CodeRequest
import com.tayra.languages.sync.Device
import com.tayra.languages.sync.DeviceInfo
import com.tayra.languages.sync.ForgotRequest
import com.tayra.languages.sync.LoginRequest
import com.tayra.languages.sync.Me
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.RefreshRequest
import com.tayra.languages.sync.RegisterRequest
import com.tayra.languages.sync.ResetRequest
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Clock
import kotlin.time.Instant

/** The account service's address for this device, or null when this build has none. */
fun interface AccountServer {
    fun url(): String?
}

/**
 * This device's side of the account service. The refresh token and the account's email are kept in
 * the platform's protected storage; the access token lives in memory and is renewed with the refresh
 * token when it runs out or is refused. A refresh the service refuses signs the device out.
 */
class AccountClient(
    private val http: HttpClient,
    private val server: AccountServer,
    private val secure: SecureStore,
    private val device: suspend () -> DeviceInfo,
    private val clock: Clock = Clock.System,
) : AccountRepository, CloudBackupStore, SyncTransport {
    private val _signedIn = MutableStateFlow(secure.get(key(EMAIL))?.takeIf { secure.get(key(REFRESH)) != null })
    override val signedIn: StateFlow<String?> = _signedIn.asStateFlow()

    override val available: Boolean get() = !server.url().isNullOrBlank()

    private var accessToken: String? = null
    private var accessExpires: Instant = Instant.DISTANT_PAST
    private val refreshing = Mutex()

    override suspend fun register(email: String, password: String): AccountInfo {
        keep(call(HttpMethod.Post, "/auth/register", json(RegisterRequest.serializer(), RegisterRequest(email, password, device())), Tokens.serializer(), signedIn = false))
        return account()
    }

    override suspend fun signIn(email: String, password: String): AccountInfo {
        keep(call(HttpMethod.Post, "/auth/login", json(LoginRequest.serializer(), LoginRequest(email, password, device())), Tokens.serializer(), signedIn = false))
        return account()
    }

    override suspend fun signOut() {
        val refresh = secure.get(key(REFRESH))
        forget()
        // The service ends the sign-in too when it can be reached; signed out here either way.
        if (refresh != null) runCatching { send(HttpMethod.Post, "/auth/logout", json(RefreshRequest.serializer(), RefreshRequest(refresh)), signedIn = false) }
    }

    override suspend fun account(): AccountInfo = call(HttpMethod.Get, "/me", null, Me.serializer()).let { me ->
        AccountInfo(me.email, me.emailVerified, me.hasPassword, me.providers, Plan(me.subscription.active, me.subscription.activeUntil?.let(Instant::fromEpochMilliseconds), me.subscription.source))
    }

    override suspend fun sendConfirmationCode() = send(HttpMethod.Post, "/me/email/send-code", null)

    override suspend fun confirmEmail(code: String) = send(HttpMethod.Post, "/me/email/verify", json(CodeRequest.serializer(), CodeRequest(code)))

    override suspend fun forgotPassword(email: String) = send(HttpMethod.Post, "/auth/password/forgot", json(ForgotRequest.serializer(), ForgotRequest(email)), signedIn = false)

    override suspend fun resetPassword(email: String, code: String, password: String) =
        send(HttpMethod.Post, "/auth/password/reset", json(ResetRequest.serializer(), ResetRequest(email, code, password)), signedIn = false)

    override suspend fun devices(): List<SignedInDevice> = call(HttpMethod.Get, "/me/devices", null, ListSerializer(Device.serializer())).map {
        SignedInDevice(it.id, it.name, it.platform, Instant.fromEpochMilliseconds(it.lastUsedAt), it.current)
    }

    override suspend fun signOutDevice(id: String) = send(HttpMethod.Delete, "/me/devices/$id", null)

    override suspend fun deleteAccount() {
        send(HttpMethod.Delete, "/me", null)
        forget()
    }

    override suspend fun list(): List<CloudBackupInfo> = call(HttpMethod.Get, "/backups", null, ListSerializer(CloudBackup.serializer())).map { it.info() }

    override suspend fun upload(name: String, content: ByteArray): CloudBackupInfo {
        val label = device().name
        return SyncJson.decodeFromString(CloudBackup.serializer(), exchange(HttpMethod.Post, "/backups", signedIn = true) {
            parameter("name", name)
            parameter("device", label)
            contentType(ContentType.Application.OctetStream)
            setBody(content)
            timeout { requestTimeoutMillis = TRANSFER_TIMEOUT_MS }
        }.bodyAsText()).info()
    }

    override suspend fun download(id: String): ByteArray =
        exchange(HttpMethod.Get, "/backups/$id", signedIn = true) { timeout { requestTimeoutMillis = TRANSFER_TIMEOUT_MS } }.bodyAsBytes()

    override suspend fun delete(id: String) = send(HttpMethod.Delete, "/backups/$id", null)

    // By the account's id, so an account deleted and made again with the same email is another one.
    override fun syncAccount(): String? = signedIn.value?.let { email -> "${server.url()?.trimEnd('/').orEmpty()} ${secure.get(key(USER)) ?: email}" }

    override suspend fun push(request: PushRequest): PushResponse = SyncJson.decodeFromString(PushResponse.serializer(),
        exchange(HttpMethod.Post, "/sync/push", signedIn = true) {
            contentType(ContentType.Application.Json)
            setBody(json(PushRequest.serializer(), request))
            timeout { requestTimeoutMillis = TRANSFER_TIMEOUT_MS }
        }.bodyAsText())

    override suspend fun pull(since: Long, limit: Int): PullResponse = SyncJson.decodeFromString(PullResponse.serializer(),
        exchange(HttpMethod.Get, "/sync/pull", signedIn = true) {
            parameter("since", since)
            parameter("limit", limit)
            timeout { requestTimeoutMillis = TRANSFER_TIMEOUT_MS }
        }.bodyAsText())

    private fun CloudBackup.info() = CloudBackupInfo(id, name, deviceName, Instant.fromEpochMilliseconds(createdAt), sizeBytes)

    private fun keep(tokens: Tokens) {
        // Empty for the web app, whose refresh token is a cookie the service reads itself.
        secure.put(key(REFRESH), tokens.refreshToken)
        secure.put(key(EMAIL), tokens.user.email)
        secure.put(key(USER), tokens.user.id)
        accessToken = tokens.accessToken
        // Renewed a little early, so a request never leaves with a token about to lapse.
        accessExpires = clock.now() + kotlin.time.Duration.parse("${tokens.expiresIn - EARLY_SECONDS}s")
        _signedIn.value = tokens.user.email
    }

    private fun forget() {
        secure.remove(key(REFRESH))
        secure.remove(key(EMAIL))
        secure.remove(key(USER))
        accessToken = null
        accessExpires = Instant.DISTANT_PAST
        _signedIn.value = null
    }

    /** A valid access token, renewed first when it has run out; [stale] is one the service just refused. */
    private suspend fun access(stale: String? = null): String = refreshing.withLock {
        val current = accessToken
        if (current != null && current != stale && clock.now() < accessExpires) return current
        val refresh = secure.get(key(REFRESH)) ?: throw AccountException(AccountProblem.SIGNED_OUT, "Not signed in")
        val tokens = try {
            call(HttpMethod.Post, "/auth/refresh", json(RefreshRequest.serializer(), RefreshRequest(refresh)), Tokens.serializer(), signedIn = false)
        } catch (e: AccountException) {
            if (e.problem == AccountProblem.SIGNED_OUT) forget()
            throw e
        }
        keep(tokens)
        tokens.accessToken
    }

    private suspend fun <T> call(method: HttpMethod, path: String, body: String?, answer: KSerializer<T>, signedIn: Boolean = true): T =
        SyncJson.decodeFromString(answer, exchange(method, path, signedIn) { body?.let { contentType(ContentType.Application.Json); setBody(it) } }.bodyAsText())

    private suspend fun send(method: HttpMethod, path: String, body: String?, signedIn: Boolean = true) {
        exchange(method, path, signedIn) { body?.let { contentType(ContentType.Application.Json); setBody(it) } }
    }

    /** One request, signed in when asked, retried once with a renewed token if the service refuses the one sent. */
    private suspend fun exchange(method: HttpMethod, path: String, signedIn: Boolean, configure: HttpRequestBuilder.() -> Unit): HttpResponse {
        val base = server.url()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: throw AccountException(AccountProblem.NOT_SET_UP, "No account service is set up")
        suspend fun once(token: String?): HttpResponse = try {
            http.request("$base/api/v1$path") {
                this.method = method
                token?.let { header("Authorization", "Bearer $it") }
                configure()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AccountException(AccountProblem.OFFLINE, e.message ?: "The account service cannot be reached")
        }
        var token = if (signedIn) access() else null
        var response = once(token)
        if (signedIn && response.status.value == 401) {
            token = access(stale = token)
            response = once(token)
        }
        if (!response.status.isSuccess()) throw refusal(response)
        return response
    }

    private suspend fun refusal(response: HttpResponse): AccountException {
        val error = runCatching { SyncJson.decodeFromString(ApiError.serializer(), response.bodyAsText()) }.getOrNull()
        val problem = when (error?.error) {
            ApiErrors.EMAIL_TAKEN -> AccountProblem.EMAIL_TAKEN
            ApiErrors.BAD_CREDENTIALS -> AccountProblem.BAD_CREDENTIALS
            ApiErrors.WRONG_CODE -> AccountProblem.WRONG_CODE
            ApiErrors.SIGNED_OUT -> AccountProblem.SIGNED_OUT
            ApiErrors.SUBSCRIPTION_REQUIRED -> AccountProblem.SUBSCRIPTION_REQUIRED
            ApiErrors.TOO_LARGE -> AccountProblem.TOO_LARGE
            ApiErrors.RATE_LIMITED -> AccountProblem.BUSY
            ApiErrors.NOT_FOUND -> AccountProblem.NOT_FOUND
            ApiErrors.INVALID_REQUEST -> AccountProblem.INVALID
            else -> when (response.status.value) {
                401 -> AccountProblem.SIGNED_OUT
                402 -> AccountProblem.SUBSCRIPTION_REQUIRED
                404 -> AccountProblem.NOT_FOUND
                413 -> AccountProblem.TOO_LARGE
                429 -> AccountProblem.BUSY
                in 400..499 -> AccountProblem.INVALID
                else -> AccountProblem.SERVER
            }
        }
        return AccountException(problem, error?.message ?: "The account service answered ${response.status.value}")
    }

    /** Where a sign-in is kept: per service, as a token is good only for the service that gave it. */
    private fun key(name: String) = "account.${server.url()?.trimEnd('/').orEmpty()}.$name"

    private fun <T> json(serializer: KSerializer<T>, value: T): String = SyncJson.encodeToString(serializer, value)

    private companion object {
        const val REFRESH = "refresh_token"
        const val EMAIL = "email"
        const val USER = "user"
        const val EARLY_SECONDS = 30
        const val TRANSFER_TIMEOUT_MS = 10 * 60_000L
    }
}
