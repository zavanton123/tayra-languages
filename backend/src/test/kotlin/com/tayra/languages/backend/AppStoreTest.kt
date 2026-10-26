package com.tayra.languages.backend

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import kotlinx.serialization.json.JsonObject
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
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
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Subscriptions bought in the iOS app: the App Store's signed notifications, and the app's own report
 * of a purchase, taken only when signed through Apple's chain of certificates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
class AppStoreTest(@Autowired private val mvc: MockMvc) {

    /** A chain like Apple's: a root, an intermediate and a leaf, each with Apple's marks. */
    class Chain(subject: String) {
        val rootKey = keys()
        val root = certificate("CN=$subject Root", rootKey, "CN=$subject Root", rootKey, ca = true)
        private val intermediateKey = keys()
        private val intermediate = certificate("CN=$subject Intermediate", intermediateKey, "CN=$subject Root", rootKey, ca = true, mark = "1.2.840.113635.100.6.2.1")
        val leafKey = keys()
        val leaf = certificate("CN=$subject Leaf", leafKey, "CN=$subject Intermediate", intermediateKey, ca = false, mark = "1.2.840.113635.100.6.11.1")

        fun sign(payload: String, chain: List<X509Certificate> = listOf(leaf, intermediate, root)): String {
            val header = JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).x509CertChain(chain.map { Base64.encode(it.encoded) }).build()
            return JWSObject(header, Payload(payload)).apply { sign(ECDSASigner(leafKey.private as ECPrivateKey)) }.serialize()
        }

        val pem: String get() = "-----BEGIN CERTIFICATE-----\n" + java.util.Base64.getMimeEncoder().encodeToString(root.encoded) + "\n-----END CERTIFICATE-----\n"
    }

    companion object {
        val apple = Chain("Apple")
        val forger = Chain("Forger")

        private fun keys(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

        private fun certificate(subject: String, keys: KeyPair, issuer: String, issuerKeys: KeyPair, ca: Boolean, mark: String? = null): X509Certificate {
            val now = Instant.now()
            val builder = JcaX509v3CertificateBuilder(X500Name(issuer), BigInteger.valueOf(now.toEpochMilli() + subject.hashCode()),
                Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(365, ChronoUnit.DAYS)), X500Name(subject), keys.public)
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(ca))
            mark?.let { builder.addExtension(ASN1ObjectIdentifier(it), false, DERNull.INSTANCE) }
            return JcaX509CertificateConverter().getCertificate(builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(issuerKeys.private)))
        }

        @JvmStatic
        @DynamicPropertySource
        fun appStore(registry: DynamicPropertyRegistry) {
            registry.add("tayra.billing.apple.bundle-ids") { "com.tayra.languages" }
            registry.add("tayra.billing.apple.root-certificates") { apple.pem }
        }
    }

    private fun account(): Pair<String, String> {
        val body = mvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"apple-${UUID.randomUUID()}@example.com","password":"a password","device":{"id":"d"}}"""
        }.andReturn().response.contentAsString
        val tokens = SyncJson.decodeFromString(Tokens.serializer(), body)
        return tokens.user.id to "Bearer ${tokens.accessToken}"
    }

    private fun transaction(original: String, user: String?, expires: Instant, signed: Instant, revoked: Instant? = null, bundle: String = "com.tayra.languages") =
        """{"originalTransactionId":"$original","transactionId":"${UUID.randomUUID()}","bundleId":"$bundle","productId":"tayra.sync.yearly",
            "expiresDate":${expires.toEpochMilli()},"signedDate":${signed.toEpochMilli()},"type":"Auto-Renewable Subscription"
            ${user?.let { ""","appAccountToken":"$it"""" } ?: ""}${revoked?.let { ""","revocationDate":${it.toEpochMilli()}""" } ?: ""}}"""

    private fun notification(type: String, transaction: String, signed: Instant, chain: Chain = apple) = chain.sign(
        """{"notificationType":"$type","notificationUUID":"${UUID.randomUUID()}","signedDate":${signed.toEpochMilli()},
            "data":{"bundleId":"com.tayra.languages","environment":"Sandbox","signedTransactionInfo":"${chain.sign(transaction)}"}}""",
    )

    private fun notify(signedPayload: String) = mvc.post("/api/v1/billing/apple") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"signedPayload":"$signedPayload"}"""
    }

    private fun active(auth: String) = mvc.get("/api/v1/me") { header("Authorization", auth) }.andReturn().response.contentAsString
        .let { SyncJson.parseToJsonElement(it) as JsonObject }.let { (it["subscription"] as JsonObject)["active"].toString() == "true" }

    @Test
    fun aPurchaseTheAppReportsStartsSyncAndTheStoresNotificationsFollowIt() {
        val (_, auth) = account()
        val original = "2000000${UUID.randomUUID().mostSignificantBits and 0xffffff}"
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val expires = now.plus(30, ChronoUnit.DAYS)

        // The app reports the purchase it just made, with no account token on it.
        mvc.post("/api/v1/billing/apple/purchases") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"signedTransaction":"${apple.sign(transaction(original, null, expires, now))}"}"""
        }.andExpect { status { isNoContent() } }
        assertTrue(active(auth))
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect { jsonPath("$.subscription.source") { value("apple") } }

        // Renewed, then refunded: the store's notifications carry no account, and still reach this one.
        notify(notification("DID_RENEW", transaction(original, null, expires.plus(30, ChronoUnit.DAYS), now.plusSeconds(60)), now.plusSeconds(60))).andExpect { status { isOk() } }
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect { jsonPath("$.subscription.activeUntil") { value(expires.plus(30, ChronoUnit.DAYS).toEpochMilli()) } }
        notify(notification("REFUND", transaction(original, null, expires, now.plusSeconds(120), revoked = now.plusSeconds(120)), now.plusSeconds(120))).andExpect { status { isOk() } }
        assertFalse(active(auth))

        // A purchase signed for another account is refused.
        mvc.post("/api/v1/billing/apple/purchases") {
            header("Authorization", auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"signedTransaction":"${apple.sign(transaction("other-$original", UUID.randomUUID().toString(), expires, now))}"}"""
        }.andExpect { status { isBadRequest() } }
            }

    @Test
    fun aNotificationNotSignedThroughApplesRootIsRefused() {
        val (user, auth) = account()
        val now = Instant.now()
        val purchase = transaction("forged-${UUID.randomUUID()}", user, now.plus(365, ChronoUnit.DAYS), now)
        notify(notification("SUBSCRIBED", purchase, now, chain = forger)).andExpect { status { isBadRequest() } }
        // Apple's leaf with the forger's chain, or a leaf without Apple's marks, is refused too.
        notify(apple.sign("""{"notificationType":"SUBSCRIBED","data":{"bundleId":"com.tayra.languages","signedTransactionInfo":"${apple.sign(purchase)}"}}""",
            chain = listOf(apple.leaf, forger.root))).andExpect { status { isBadRequest() } }
        notify(notification("SUBSCRIBED", transaction("other-app-${UUID.randomUUID()}", user, now.plus(365, ChronoUnit.DAYS), now, bundle = "com.example.other"), now)).andExpect { status { isBadRequest() } }
        assertFalse(active(auth))
        notify(notification("SUBSCRIBED", purchase, now)).andExpect { status { isOk() } }
        assertTrue(active(auth))
    }
}
