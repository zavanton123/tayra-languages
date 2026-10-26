package com.tayra.languages.backend.billing

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.tayra.languages.backend.auth.user
import com.tayra.languages.sync.AppStorePurchase
import com.tayra.languages.sync.SyncJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * The App Store's word on subscriptions bought in the iOS app: its server notifications (version 2),
 * and the signed transaction the app hands in right after a purchase. Both are JWS signed with a
 * key whose certificate chains to Apple's root (Apple Root CA - G3, given as [rootCertificates]),
 * through certificates that carry Apple's marks for the purpose. The app sets the purchase's
 * appAccountToken to the account's id, which ties it to the account.
 */
@Service
class AppStore(
    private val billing: Billing,
    private val clock: Clock,
    @Value("\${tayra.billing.apple.bundle-ids}") bundleIds: String,
    @Value("\${tayra.billing.apple.root-certificates}") rootCertificates: String,
) {
    private val bundles = bundleIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    private val roots: Set<TrustAnchor> = rootCertificates.takeIf { it.isNotBlank() }?.let { pem ->
        CertificateFactory.getInstance("X.509").generateCertificates(pem.byteInputStream()).map { TrustAnchor(it as X509Certificate, null) }.toSet()
    }.orEmpty()

    /** Takes a server notification's signedPayload; whether it changed a subscription. */
    fun notified(signedPayload: String): Boolean {
        val payload = verified(signedPayload)
        val data = payload["data"] as? JsonObject ?: return false
        if (data.text("bundleId") !in bundles) throw BadNotification("Another app's notification")
        val transaction = verified(data.text("signedTransactionInfo") ?: return false)
        if (transaction.text("bundleId") !in bundles) throw BadNotification("Another app's purchase")
        val renewal = data.text("signedRenewalInfo")?.let(::verified)
        val type = payload.text("notificationType").orEmpty()
        return billing.record(state(transaction, renewal, type.lowercase(), payload.number("signedDate"), user = null))
    }

    /** Takes a purchase the app made for [userId]; whether it changed a subscription. */
    fun purchased(userId: UUID, purchase: AppStorePurchase): Boolean {
        val transaction = verified(purchase.signedTransaction)
        if (transaction.text("bundleId") !in bundles) throw BadNotification("Another app's purchase")
        val token = transaction.text("appAccountToken")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (token != null && token != userId) throw BadNotification("The purchase is another account's")
        return billing.record(state(transaction, null, "purchased", transaction.number("signedDate"), userId))
    }

    private fun state(transaction: JsonObject, renewal: JsonObject?, type: String, signedAt: Long?, user: UUID?): SubscriptionState {
        val revoked = transaction.number("revocationDate")
        val expires = listOfNotNull(transaction.number("expiresDate"), renewal?.number("gracePeriodExpiresDate")).maxOrNull()
        val ended = revoked != null || type == "expired" || type == "grace_period_expired"
        return SubscriptionState(
            source = "apple",
            externalId = transaction.text("originalTransactionId") ?: throw BadNotification("No original transaction id"),
            userId = user ?: transaction.text("appAccountToken")?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            product = transaction.text("productId").orEmpty(),
            status = if (revoked != null) "revoked" else type,
            activeUntil = if (ended) null else expires?.let(Instant::ofEpochMilli),
            eventAt = signedAt?.let(Instant::ofEpochMilli) ?: clock.instant(),
        )
    }

    /** The JWS's payload, once its chain leads to Apple's root and its signature is the leaf's. */
    private fun verified(jws: String): JsonObject {
        if (roots.isEmpty() || bundles.isEmpty()) throw ProviderNotSetUp("The App Store")
        val signed = runCatching { JWSObject.parse(jws) }.getOrNull() ?: throw BadNotification("Not a JWS")
        if (signed.header.algorithm != JWSAlgorithm.ES256) throw BadNotification("Not signed with ES256")
        val factory = CertificateFactory.getInstance("X.509")
        val chain = signed.header.x509CertChain.orEmpty().map { factory.generateCertificate(it.decode().inputStream()) as X509Certificate }
        if (chain.size < 2) throw BadNotification("No certificate chain")
        if (chain[0].getExtensionValue(LEAF_MARK) == null || chain[1].getExtensionValue(INTERMEDIATE_MARK) == null) throw BadNotification("Not Apple's certificates")
        val path = factory.generateCertPath(chain.filterNot { c -> roots.any { it.trustedCert == c } })
        runCatching {
            CertPathValidator.getInstance("PKIX").validate(path, PKIXParameters(roots).apply { isRevocationEnabled = false; date = Date.from(clock.instant()) })
        }.onFailure { throw BadNotification("The certificates do not lead to Apple's root") }
        if (!signed.verify(ECDSAVerifier(chain[0].publicKey as ECPublicKey))) throw BadNotification("Bad signature")
        return runCatching { SyncJson.parseToJsonElement(signed.payload.toString()) as JsonObject }.getOrNull() ?: throw BadNotification("Not JSON")
    }

    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.number(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull

    private companion object {
        /** Apple's marks on the certificates that sign App Store data. */
        const val LEAF_MARK = "1.2.840.113635.100.6.11.1"
        const val INTERMEDIATE_MARK = "1.2.840.113635.100.6.2.1"
    }
}

@Serializable
data class AppStoreNotification(val signedPayload: String)

@RestController
class AppStoreController(private val appStore: AppStore) {
    @PostMapping("/api/v1/billing/apple")
    fun notified(@RequestBody notification: AppStoreNotification) {
        appStore.notified(notification.signedPayload)
    }

    @PostMapping("/api/v1/billing/apple/purchases")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun purchased(@AuthenticationPrincipal jwt: Jwt, @RequestBody purchase: AppStorePurchase) {
        appStore.purchased(jwt.user, purchase)
    }
}
