package com.tayra.languages.backend

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Subscriptions bought in the Android app: the app's report of a purchase and Play's pushed
 * notifications, each read back from a Play Developer API that stands in for Google's here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
class GooglePlayTest(@Autowired private val mvc: MockMvc) {

    /** Google's token endpoint and the Play Developer API, as far as the service uses them. */
    object FakeGoogle {
        val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        /** Each purchase token's subscription, as subscriptionsv2 answers it. */
        val subscriptions = ConcurrentHashMap<String, String>()
        val acknowledged = ConcurrentHashMap.newKeySet<String>()
        val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/token") { exchange ->
                val form = exchange.requestBody.readAllBytes().decodeToString().split('&').associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8) }
                val assertion = SignedJWT.parse(form.getValue("assertion"))
                val ok = assertion.verify(RSASSAVerifier(keys.public as RSAPublicKey)) &&
                    assertion.jwtClaimsSet.getStringClaim("scope") == "https://www.googleapis.com/auth/androidpublisher"
                respond(exchange, if (ok) 200 else 400, """{"access_token":"ya29.test","expires_in":3600,"token_type":"Bearer"}""")
            }
            createContext("/androidpublisher/v3/applications/com.tayra.languages/purchases/") { exchange ->
                if (exchange.requestHeaders.getFirst("Authorization") != "Bearer ya29.test") return@createContext respond(exchange, 401, "{}")
                val path = exchange.requestURI.path
                when {
                    path.endsWith(":acknowledge") -> {
                        val token = path.substringAfter("/tokens/").removeSuffix(":acknowledge")
                        acknowledged += token
                        subscriptions.computeIfPresent(token) { _, s -> s.replace("ACKNOWLEDGEMENT_STATE_PENDING", "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED") }
                        respond(exchange, 200, "")
                    }
                    else -> subscriptions[path.substringAfter("/tokens/")]?.let { respond(exchange, 200, it) } ?: respond(exchange, 404, "{}")
                }
            }
            start()
        }

        private fun respond(exchange: HttpExchange, status: Int, body: String) {
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }

        val url get() = "http://127.0.0.1:${server.address.port}"

        val serviceAccount: String get() {
            val pem = "-----BEGIN PRIVATE KEY-----\\n" + Base64.getEncoder().encodeToString(keys.private.encoded) + "\\n-----END PRIVATE KEY-----\\n"
            return """{"type":"service_account","client_email":"tayra-billing@example.iam.gserviceaccount.com","private_key":"$pem","token_uri":"$url/token"}"""
        }

        fun subscription(state: String, user: String?, expires: Instant, acknowledged: Boolean = false) = """
            {"kind":"androidpublisher#subscriptionPurchaseV2","subscriptionState":"SUBSCRIPTION_STATE_$state",
             "acknowledgementState":"ACKNOWLEDGEMENT_STATE_${if (acknowledged) "ACKNOWLEDGED" else "PENDING"}",
             "lineItems":[{"productId":"tayra.sync.yearly","expiryTime":"$expires"}]
             ${user?.let { ""","externalAccountIdentifiers":{"obfuscatedExternalAccountId":"$it"}""" } ?: ""}}
        """.trimIndent()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun googlePlay(registry: DynamicPropertyRegistry) {
            registry.add("tayra.billing.google.package-name") { "com.tayra.languages" }
            registry.add("tayra.billing.google.service-account") { FakeGoogle.serviceAccount }
            registry.add("tayra.billing.google.push-token") { "push-secret" }
            registry.add("tayra.billing.google.api-url") { FakeGoogle.url }
        }
    }

    private fun account(): Pair<String, String> {
        val body = mvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"play-${UUID.randomUUID()}@example.com","password":"a password","device":{"id":"d"}}"""
        }.andReturn().response.contentAsString
        val tokens = SyncJson.decodeFromString(Tokens.serializer(), body)
        return tokens.user.id to "Bearer ${tokens.accessToken}"
    }

    private fun push(token: String, packageName: String = "com.tayra.languages", secret: String = "push-secret") = mvc.post("/api/v1/billing/google?token=$secret") {
        contentType = MediaType.APPLICATION_JSON
        val data = """{"version":"1.0","packageName":"$packageName","eventTimeMillis":"${Instant.now().toEpochMilli()}",
            "subscriptionNotification":{"version":"1.0","notificationType":13,"purchaseToken":"$token","subscriptionId":"tayra.sync.yearly"}}"""
        content = """{"message":{"data":"${Base64.getEncoder().encodeToString(data.toByteArray())}","messageId":"1"},"subscription":"projects/tayra/subscriptions/play"}"""
    }

    private fun active(auth: String) = mvc.get("/api/v1/me") { header("Authorization", auth) }.andReturn().response.contentAsString
        .let { SyncJson.parseToJsonElement(it) as JsonObject }.let { (it["subscription"] as JsonObject)["active"].toString() == "true" }

    @Test
    fun aPurchaseIsAcknowledgedAndPlaysNotificationsFollowIt() {
        val (user, auth) = account()
        val token = "token-${UUID.randomUUID()}"
        val expires = Instant.now().plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
        FakeGoogle.subscriptions[token] = FakeGoogle.subscription("ACTIVE", user, expires)

        mvc.post("/api/v1/billing/google/purchases") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":"tayra.sync.yearly","purchaseToken":"$token"}"""
        }.andExpect { status { isNoContent() } }
        assertTrue(active(auth))
        assertTrue(token in FakeGoogle.acknowledged, "acknowledged, so Play does not refund it")
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect {
            jsonPath("$.subscription.source") { value("google") }
            jsonPath("$.subscription.activeUntil") { value(expires.toEpochMilli()) }
        }

        // Play pushes that it expired: the state is read back, and the account no longer syncs.
        FakeGoogle.subscriptions[token] = FakeGoogle.subscription("EXPIRED", user, Instant.now().minusSeconds(60), acknowledged = true)
        push(token).andExpect { status { isOk() } }
        assertFalse(active(auth))
    }

    @Test
    fun onlyThePushSubscriptionsNotificationsAndTheBuyersOwnReportsAreTaken() {
        val (user, auth) = account()
        val token = "token-${UUID.randomUUID()}"
        FakeGoogle.subscriptions[token] = FakeGoogle.subscription("ACTIVE", user, Instant.now().plus(30, ChronoUnit.DAYS))
        push(token, secret = "a guess").andExpect { status { isBadRequest() } }
        push(token, packageName = "com.example.other").andExpect { status { isBadRequest() } }
        assertFalse(active(auth))

        val (_, someoneElse) = account()
        mvc.post("/api/v1/billing/google/purchases") {
            header("Authorization", someoneElse)
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":"tayra.sync.yearly","purchaseToken":"$token"}"""
        }.andExpect { status { isBadRequest() } }
        assertFalse(active(someoneElse))

        // A notification of a purchase the app has not reported yet still reaches the buyer by the account id on it.
        push(token).andExpect { status { isOk() } }
        assertTrue(active(auth))
        assertEquals(1, FakeGoogle.acknowledged.count { it == token })
    }
}
