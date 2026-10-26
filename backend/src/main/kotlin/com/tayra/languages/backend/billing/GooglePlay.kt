package com.tayra.languages.backend.billing

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.tayra.languages.backend.auth.user
import com.tayra.languages.sync.GooglePlayPurchase
import com.tayra.languages.sync.SyncJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * The Google Play Developer API, as a service account of the owner's Play Console sees it: a
 * subscription's state by its purchase token, and acknowledging a purchase, which Play refunds when
 * it is not acknowledged within three days.
 */
@Service
class GooglePlayApi(
    private val clock: Clock,
    @Value("\${tayra.billing.google.service-account}") serviceAccount: String,
    @Value("\${tayra.billing.google.api-url}") private val apiUrl: String,
) {
    private class Account(val email: String, val key: RSAPrivateKey, val tokenUri: String)

    private val account: Account? = serviceAccount.takeIf { it.isNotBlank() }?.let { text ->
        val json = SyncJson.parseToJsonElement(text) as JsonObject
        val pem = json.text("private_key")!!.replace(Regex("-----[A-Z ]+-----|\\s"), "")
        Account(
            json.text("client_email")!!,
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))) as RSAPrivateKey,
            json.text("token_uri") ?: "https://oauth2.googleapis.com/token",
        )
    }
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private var accessToken: Pair<String, Instant>? = null

    val ready: Boolean get() = account != null

    /** The subscription bought with [token], as subscriptionsv2 gives it. */
    fun subscription(packageName: String, token: String): JsonObject =
        call("GET", "/androidpublisher/v3/applications/$packageName/purchases/subscriptionsv2/tokens/${encode(token)}", null)

    fun acknowledge(packageName: String, productId: String, token: String) {
        call("POST", "/androidpublisher/v3/applications/$packageName/purchases/subscriptions/${encode(productId)}/tokens/${encode(token)}:acknowledge", "{}")
    }

    private fun call(method: String, path: String, body: String?): JsonObject {
        val request = HttpRequest.newBuilder(URI("${apiUrl.trimEnd('/')}$path")).timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer ${token()}").header("Content-Type", "application/json")
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw BadNotification("Google Play answered ${response.statusCode()}")
        return response.body().takeIf { it.isNotBlank() }?.let { SyncJson.parseToJsonElement(it) as JsonObject } ?: JsonObject(emptyMap())
    }

    /** An access token for the service account, from a JWT it signs, kept until shortly before it lapses. */
    @Synchronized
    private fun token(): String {
        val account = account ?: throw ProviderNotSetUp("Google Play")
        accessToken?.takeIf { it.second.isAfter(clock.instant().plusSeconds(60)) }?.let { return it.first }
        val now = clock.instant()
        val assertion = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
            JWTClaimsSet.Builder().issuer(account.email).audience(account.tokenUri).claim("scope", SCOPE)
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600))).build(),
        ).apply { sign(RSASSASigner(account.key)) }.serialize()
        val form = "grant_type=${encode("urn:ietf:params:oauth:grant-type:jwt-bearer")}&assertion=${encode(assertion)}"
        val response = http.send(
            HttpRequest.newBuilder(URI(account.tokenUri)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() != 200) throw BadNotification("Google refused the service account: ${response.statusCode()}")
        val json = SyncJson.parseToJsonElement(response.body()) as JsonObject
        val token = json.text("access_token") ?: throw BadNotification("No access token")
        accessToken = token to now.plusSeconds(json.text("expires_in")?.toLongOrNull() ?: 3600)
        return token
    }

    private fun encode(text: String) = URLEncoder.encode(text, Charsets.UTF_8)

    private companion object {
        const val SCOPE = "https://www.googleapis.com/auth/androidpublisher"
    }
}

/**
 * Subscriptions bought in the Android app. Play tells of each change by a real-time developer
 * notification, pushed by Pub/Sub to a URL carrying [pushToken]; it names only the purchase token,
 * so the subscription's state is read from Play each time. The app sets the purchase's obfuscated
 * account id to the account's id, and reports the purchase itself right after it is made.
 */
@Service
class GooglePlay(
    private val billing: Billing,
    private val api: GooglePlayApi,
    private val clock: Clock,
    @Value("\${tayra.billing.google.package-name}") private val packageName: String,
    @Value("\${tayra.billing.google.push-token}") private val pushToken: String,
) {
    /** Takes a Pub/Sub push of a developer notification; whether it changed a subscription. */
    fun notified(token: String?, body: String): Boolean {
        if (!api.ready || pushToken.isEmpty() || packageName.isEmpty()) throw ProviderNotSetUp("Google Play")
        if (token == null || !MessageDigest.isEqual(token.toByteArray(), pushToken.toByteArray())) throw BadNotification("Not from the push subscription")
        val push = runCatching { SyncJson.parseToJsonElement(body) as JsonObject }.getOrNull() ?: throw BadNotification("Not JSON")
        val data = (push["message"] as? JsonObject)?.text("data") ?: throw BadNotification("No message")
        val notification = runCatching { SyncJson.parseToJsonElement(String(Base64.getDecoder().decode(data))) as JsonObject }.getOrNull()
            ?: throw BadNotification("Not a developer notification")
        if (notification.text("packageName") != packageName) throw BadNotification("Another app's notification")
        val subscription = notification["subscriptionNotification"] as? JsonObject ?: return false
        return refresh(subscription.text("purchaseToken") ?: throw BadNotification("No purchase token"), user = null)
    }

    /** Takes a purchase the app made for [userId]; whether it changed a subscription. */
    fun purchased(userId: UUID, purchase: GooglePlayPurchase): Boolean {
        if (!api.ready || packageName.isEmpty()) throw ProviderNotSetUp("Google Play")
        return refresh(purchase.purchaseToken, userId)
    }

    /** Reads the subscription bought with [token] from Play, acknowledges it when new, and records it. */
    private fun refresh(token: String, user: UUID?): Boolean {
        val state = api.subscription(packageName, token)
        val owner = (state["externalAccountIdentifiers"] as? JsonObject)?.text("obfuscatedExternalAccountId")
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (user != null && owner != null && owner != user) throw BadNotification("The purchase is another account's")
        val items = (state["lineItems"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val product = items.firstOrNull()?.text("productId").orEmpty()
        val status = state.text("subscriptionState")?.removePrefix("SUBSCRIPTION_STATE_")?.lowercase() ?: throw BadNotification("No subscription state")
        val expires = items.mapNotNull { it.text("expiryTime")?.let(Instant::parse) }.maxOrNull()
        val pending = state.text("acknowledgementState") == "ACKNOWLEDGEMENT_STATE_PENDING"
        if (pending && status in ENTITLED && product.isNotEmpty()) api.acknowledge(packageName, product, token)
        // The state was read now, whatever the notification's time: it is the latest there is.
        return billing.record(SubscriptionState("google", token, user ?: owner, product, status, expires.takeIf { status in ENTITLED }, clock.instant()))
    }

    private companion object {
        /** States in which the subscription lets the account sync until it expires; a canceled one runs to its end. */
        val ENTITLED = setOf("active", "in_grace_period", "canceled")
    }
}

private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

@RestController
class GooglePlayController(private val play: GooglePlay) {
    @PostMapping("/api/v1/billing/google")
    fun notified(@RequestParam("token", required = false) token: String?, @RequestBody body: String) {
        play.notified(token, body)
    }

    @PostMapping("/api/v1/billing/google/purchases")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun purchased(@AuthenticationPrincipal jwt: Jwt, @RequestBody purchase: GooglePlayPurchase) {
        play.purchased(jwt.user, purchase)
    }
}
