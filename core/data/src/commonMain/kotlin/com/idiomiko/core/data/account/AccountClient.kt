package com.idiomiko.core.data.account

import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.data.sync.SyncTransport
import com.idiomiko.core.domain.account.AccountException
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.domain.account.AccountProfile
import com.idiomiko.core.domain.account.AccountProblem
import com.idiomiko.core.domain.account.AccountRepository
import com.idiomiko.core.domain.account.Plan
import com.idiomiko.core.domain.account.SignInClients
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.domain.account.SignedInDevice
import com.idiomiko.core.domain.account.StoreReceipt
import com.idiomiko.sync.ApiError
import com.idiomiko.sync.ApiErrors
import com.idiomiko.sync.AppStorePurchase
import com.idiomiko.sync.CodeRequest
import com.idiomiko.sync.Device
import com.idiomiko.sync.DeviceInfo
import com.idiomiko.sync.ForgotRequest
import com.idiomiko.sync.GooglePlayPurchase
import com.idiomiko.sync.Info
import com.idiomiko.sync.LoginRequest
import com.idiomiko.sync.Me
import com.idiomiko.sync.ProviderSignInRequest
import com.idiomiko.sync.PullResponse
import com.idiomiko.sync.PushRequest
import com.idiomiko.sync.PushResponse
import com.idiomiko.sync.RefreshRequest
import com.idiomiko.sync.RegisterRequest
import com.idiomiko.sync.ResetRequest
import com.idiomiko.sync.SyncJson
import com.idiomiko.sync.Tokens
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

/** Addresses the account service had before, by its address now: a sign-in and a sync made there carry over. */
internal val FORMER_ADDRESSES = mapOf("https://idiomiko.com" to listOf("https://95.111.244.23.sslip.io"))

/** A sync account ("address id") with a former address of the service given its address now. */
internal fun withCurrentAddress(account: String): String = FORMER_ADDRESSES.entries.firstNotNullOfOrNull { (now, before) ->
    before.firstOrNull { account.startsWith("$it ") }?.let { now + account.removePrefix(it) }
} ?: account

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
    /** Whether the refresh token is a browser cookie the service's own pages share (the web app). */
    private val sharedSignIn: Boolean = false,
) : AccountRepository, SyncTransport {
    init {
        carryOverFormerSignIn()
    }

    private val _signedIn = MutableStateFlow(secure.get(key(EMAIL))?.takeIf { secure.get(key(REFRESH)) != null })
    override val signedIn: StateFlow<String?> = _signedIn.asStateFlow()

    private val _profile = MutableStateFlow(_signedIn.value?.let { AccountProfile(it, secure.get(key(NAME)), secure.get(key(PICTURE))) })
    override val profile: StateFlow<AccountProfile?> = _profile.asStateFlow()

    /** The picture last fetched, by which picture it is. */
    private var picture: Pair<String, ByteArray>? = null

    override val available: Boolean get() = !server.url().isNullOrBlank()

    private var accessToken: String? = null
    private var accessExpires: Instant = Instant.DISTANT_PAST
    private val refreshing = Mutex()

    override suspend fun joinSharedSignIn(): Boolean {
        if (!sharedSignIn || !available || _signedIn.value != null) return false
        val tokens = try {
            call(HttpMethod.Post, "/auth/refresh", json(RefreshRequest.serializer(), RefreshRequest("")), Tokens.serializer(), signedIn = false)
        } catch (_: AccountException) {
            return false
        }
        keep(tokens)
        return true
    }

    override suspend fun register(email: String, password: String): AccountInfo {
        keep(call(HttpMethod.Post, "/auth/register", json(RegisterRequest.serializer(), RegisterRequest(email, password, device())), Tokens.serializer(), signedIn = false))
        return account()
    }

    override suspend fun signIn(email: String, password: String): AccountInfo {
        keep(call(HttpMethod.Post, "/auth/login", json(LoginRequest.serializer(), LoginRequest(email, password, device())), Tokens.serializer(), signedIn = false))
        return account()
    }

    override suspend fun signInWith(provider: SignInProvider, idToken: String): AccountInfo {
        val path = if (provider == SignInProvider.GOOGLE) "/auth/google" else "/auth/apple"
        keep(call(HttpMethod.Post, path, json(ProviderSignInRequest.serializer(), ProviderSignInRequest(idToken, device())), Tokens.serializer(), signedIn = false))
        return account()
    }

    override suspend fun signInClients(): SignInClients = try {
        call(HttpMethod.Get, "/info", null, Info.serializer(), signedIn = false).signIn.let {
            SignInClients(it.googleWebClientId, it.googleIosClientId, it.googleDesktopClientId, it.googleDesktopClientSecret, it.apple)
        }
    } catch (_: AccountException) {
        SignInClients()
    }

    override suspend fun signOut() {
        val refresh = secure.get(key(REFRESH))
        forget()
        // The service ends the sign-in too when it can be reached; signed out here either way.
        if (refresh != null) runCatching { send(HttpMethod.Post, "/auth/logout", json(RefreshRequest.serializer(), RefreshRequest(refresh)), signedIn = false) }
    }

    override suspend fun account(): AccountInfo = call(HttpMethod.Get, "/me", null, Me.serializer()).let { me ->
        keepProfile(me)
        val sub = me.subscription
        // The stores require their own billing in the phone apps: those never send to the web checkout.
        val checkout = sub.checkoutUrl.takeIf { device().platform !in STORE_PLATFORMS }
        AccountInfo(
            me.email, me.emailVerified, me.hasPassword, me.providers,
            Plan(sub.active, sub.activeUntil?.let(Instant::fromEpochMilliseconds), sub.source, checkout, sub.manageUrl), me.id, me.name, me.picture,
        )
    }

    override suspend fun picture(): ByteArray? {
        val id = _profile.value?.picture ?: return null
        picture?.takeIf { it.first == id }?.let { return it.second }
        val bytes = try {
            exchange(HttpMethod.Get, "/me/picture", signedIn = true) {}.bodyAsBytes()
        } catch (_: AccountException) {
            return null
        }
        picture = id to bytes
        return bytes
    }

    /** Keeps the account's name and picture as the service has them, for the bar to show from the start. */
    private fun keepProfile(me: Me) {
        if (_signedIn.value == null) return
        putOrRemove(NAME, me.name)
        putOrRemove(PICTURE, me.picture)
        _profile.value = AccountProfile(me.email, me.name, me.picture)
    }

    private fun putOrRemove(name: String, value: String?) = if (value == null) secure.remove(key(name)) else secure.put(key(name), value)

    override suspend fun reportPurchase(receipt: StoreReceipt) = when (receipt) {
        is StoreReceipt.GooglePlay -> send(HttpMethod.Post, "/billing/google/purchases", json(GooglePlayPurchase.serializer(), GooglePlayPurchase(receipt.productId, receipt.purchaseToken)))
        is StoreReceipt.AppStore -> send(HttpMethod.Post, "/billing/apple/purchases", json(AppStorePurchase.serializer(), AppStorePurchase(receipt.signedTransaction)))
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

    // By the account's id, so an account deleted and made again with the same email is another one.
    override fun syncAccount(): String? = signedIn.value?.let { email -> "${server.url()?.trimEnd('/').orEmpty()} ${secure.get(key(USER)) ?: email}" }

    override suspend fun push(request: PushRequest): PushResponse = SyncJson.decodeFromString(PushResponse.serializer(),
        exchange(HttpMethod.Post, "/sync/push", signedIn = true) {
            contentType(ContentType.Application.Json)
            setBody(json(PushRequest.serializer(), request))
            timeout { requestTimeoutMillis = SYNC_TIMEOUT_MS; socketTimeoutMillis = SYNC_SOCKET_TIMEOUT_MS }
        }.bodyAsText())

    override suspend fun pull(since: Long, limit: Int): PullResponse = SyncJson.decodeFromString(PullResponse.serializer(),
        exchange(HttpMethod.Get, "/sync/pull", signedIn = true) {
            parameter("since", since)
            parameter("limit", limit)
            timeout { requestTimeoutMillis = SYNC_TIMEOUT_MS; socketTimeoutMillis = SYNC_SOCKET_TIMEOUT_MS }
        }.bodyAsText())

    private fun keep(tokens: Tokens) {
        // Another account's name and picture are not this one's.
        if (secure.get(key(USER)) != tokens.user.id) {
            secure.remove(key(NAME))
            secure.remove(key(PICTURE))
        }
        // Empty for the web app, whose refresh token is a cookie the service reads itself.
        secure.put(key(REFRESH), tokens.refreshToken)
        secure.put(key(EMAIL), tokens.user.email)
        secure.put(key(USER), tokens.user.id)
        accessToken = tokens.accessToken
        // Renewed a little early, so a request never leaves with a token about to lapse.
        accessExpires = clock.now() + kotlin.time.Duration.parse("${tokens.expiresIn - EARLY_SECONDS}s")
        _signedIn.value = tokens.user.email
        _profile.value = AccountProfile(tokens.user.email, secure.get(key(NAME)), secure.get(key(PICTURE)))
    }

    private fun forget() {
        secure.remove(key(REFRESH))
        secure.remove(key(EMAIL))
        secure.remove(key(USER))
        secure.remove(key(NAME))
        secure.remove(key(PICTURE))
        accessToken = null
        accessExpires = Instant.DISTANT_PAST
        _signedIn.value = null
        _profile.value = null
        picture = null
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
            ApiErrors.RATE_LIMITED -> AccountProblem.BUSY
            ApiErrors.TOO_MANY_CODES -> AccountProblem.TOO_MANY_CODES
            ApiErrors.NOT_FOUND -> AccountProblem.NOT_FOUND
            ApiErrors.INVALID_REQUEST -> AccountProblem.INVALID
            else -> when (response.status.value) {
                401 -> AccountProblem.SIGNED_OUT
                402 -> AccountProblem.SUBSCRIPTION_REQUIRED
                404 -> AccountProblem.NOT_FOUND
                429 -> AccountProblem.BUSY
                in 400..499 -> AccountProblem.INVALID
                else -> AccountProblem.SERVER
            }
        }
        return AccountException(problem, error?.message ?: "The account service answered ${response.status.value}")
    }

    private fun carryOverFormerSignIn() {
        val now = server.url()?.trimEnd('/') ?: return
        if (secure.get(key(REFRESH)) != null) return
        val before = FORMER_ADDRESSES[now].orEmpty().firstOrNull { secure.get("account.$it.$REFRESH") != null } ?: return
        for (name in listOf(REFRESH, EMAIL, USER)) {
            secure.get("account.$before.$name")?.let { secure.put(key(name), it) }
            secure.remove("account.$before.$name")
        }
    }

    /** Where a sign-in is kept: per service, as a token is good only for the service that gave it. */
    private fun key(name: String) = "account.${server.url()?.trimEnd('/').orEmpty()}.$name"

    private fun <T> json(serializer: KSerializer<T>, value: T): String = SyncJson.encodeToString(serializer, value)

    private companion object {
        const val REFRESH = "refresh_token"
        const val EMAIL = "email"
        val STORE_PLATFORMS = setOf("android", "ios")
        const val USER = "user"
        const val NAME = "name"
        const val PICTURE = "picture"
        const val EARLY_SECONDS = 30
        // A batch of sync records: a stalled connection fails, and the sync with it, instead of hanging on.
        const val SYNC_TIMEOUT_MS = 3 * 60_000L
        const val SYNC_SOCKET_TIMEOUT_MS = 60_000L
    }
}
