package com.tayra.languages.backend.billing

import com.tayra.languages.sync.Me
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URLEncoder
import java.util.UUID

/**
 * Subscribing on the web, for the desktop and web apps: a page of the service's own that opens
 * Paddle's checkout for the monthly or yearly price, carrying the account's id, which Paddle's
 * webhooks repeat (see [Paddle]). And where each store lets a subscription bought there be managed.
 */
@Service
class Checkout(
    private val jdbc: JdbcClient,
    @Value("\${tayra.billing.paddle.client-token}") private val clientToken: String,
    @Value("\${tayra.billing.paddle.environment}") private val environment: String,
    @Value("\${tayra.billing.paddle.monthly-price}") private val monthlyPrice: String,
    @Value("\${tayra.billing.paddle.yearly-price}") private val yearlyPrice: String,
    @Value("\${tayra.billing.google.package-name}") private val googlePackage: String,
    @Value("\${tayra.billing.paddle.portal-url}") private val paddlePortal: String,
) {
    private val prices = listOf("monthly" to monthlyPrice, "yearly" to yearlyPrice).filter { it.second.matches(PRICE) }
    private val ready = clientToken.matches(TOKEN) && prices.isNotEmpty()

    /** [me] with where the account can subscribe, or manage its subscription; [base] is the service's own address. */
    fun withLinks(me: Me, base: String): Me {
        val subscription = me.subscription
        val checkout = "$base/checkout?account=${me.id}".takeIf { ready && !subscription.active }
        val manage = when (subscription.source) {
            "paddle" -> paddlePortal.takeIf { it.startsWith("https://") }
            "apple" -> "https://apps.apple.com/account/subscriptions"
            "google" -> {
                val product = jdbc.sql("SELECT product FROM subscriptions WHERE user_id = ? AND source = 'google' ORDER BY active_until DESC NULLS LAST LIMIT 1")
                    .param(UUID.fromString(me.id)).query(String::class.java).optional().orElse("")
                "https://play.google.com/store/account/subscriptions?package=${encode(googlePackage)}&sku=${encode(product)}"
            }
            else -> null
        }
        return me.copy(subscription = subscription.copy(checkoutUrl = checkout, manageUrl = manage))
    }

    /** The checkout page for [account], or null when the service does not sell on the web or there is no such account. */
    fun page(account: String): String? {
        if (!ready) return null
        val id = runCatching { UUID.fromString(account) }.getOrNull() ?: return null
        val email = jdbc.sql("SELECT email FROM users WHERE id = ?").param(id).query(String::class.java).optional().orElse(null) ?: return null
        val buttons = prices.joinToString("\n") { (period, price) ->
            """<button onclick="buy('$price')">${if (period == "monthly") "Monthly" else "Yearly"}</button>"""
        }
        return """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Subscribe · Tayra Languages</title>
<style>body{font-family:system-ui,sans-serif;max-width:32rem;margin:4rem auto;padding:0 1rem;color:#1b2a3a}
button{font-size:1rem;padding:.75rem 1.5rem;margin:.25rem;border:0;border-radius:.5rem;background:#1f5f8b;color:#fff;cursor:pointer}</style>
<script src="https://cdn.paddle.com/paddle/v2/paddle.js"></script></head>
<body><h1>Tayra Languages</h1>
<p>Sync your library and keep it backed up in the cloud, on all your devices.</p>
$buttons
<p id="done" hidden>Thank you. Go back to the app: your subscription shows there within a minute.</p>
<script>
${if (environment == "sandbox") "Paddle.Environment.set('sandbox');" else ""}
Paddle.Initialize({ token: ${js(clientToken)}, eventCallback: function (e) { if (e.name === 'checkout.completed') document.getElementById('done').hidden = false; } });
function buy(price) {
  Paddle.Checkout.open({ items: [{ priceId: price, quantity: 1 }], customer: { email: ${js(email)} }, customData: { user_id: ${js(id.toString())} } });
}
</script></body></html>"""
    }

    /** [text] as a JavaScript string literal that cannot end the script it is in. */
    private fun js(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c").replace("\n", "\\n") + "\""

    private fun encode(text: String) = URLEncoder.encode(text, Charsets.UTF_8)

    private companion object {
        val PRICE = Regex("pri_[a-z0-9]+")
        val TOKEN = Regex("(test|live)_[A-Za-z0-9]+")
    }
}

@RestController
class CheckoutController(private val checkout: Checkout) {
    @GetMapping("/checkout", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(@RequestParam account: String): ResponseEntity<String> =
        checkout.page(account)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found")

    companion object {
        /** The service's own address, as the request reached it. */
        fun base(): String = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
    }
}
