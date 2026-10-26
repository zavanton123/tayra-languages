package com.tayra.languages.backend

import com.tayra.languages.backend.billing.Billing
import com.tayra.languages.backend.billing.SubscriptionState
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** An account without a subscription is sent to the web checkout; one with a store's subscription to that store. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(
    properties = [
        "tayra.billing.paddle.client-token=test_7d279f61a3499fed520f7cd8c08",
        "tayra.billing.paddle.environment=sandbox",
        "tayra.billing.paddle.monthly-price=pri_01monthly",
        "tayra.billing.paddle.yearly-price=pri_01yearly",
        "tayra.billing.google.package-name=com.tayra.languages",
    ],
)
class CheckoutTest(@Autowired private val mvc: MockMvc, @Autowired private val billing: Billing) {

    private fun account(email: String = "checkout-${UUID.randomUUID()}@example.com"): Pair<String, String> {
        val body = mvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"a password","device":{"id":"d"}}"""
        }.andReturn().response.contentAsString
        val tokens = SyncJson.decodeFromString(Tokens.serializer(), body)
        return tokens.user.id to "Bearer ${tokens.accessToken}"
    }

    @Test
    fun anAccountWithoutASubscriptionGetsTheCheckoutPage() {
        val (user, auth) = account("reader+</script>@example.com")
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect {
            jsonPath("$.subscription.checkoutUrl") { value("http://localhost/checkout?account=$user") }
            jsonPath("$.subscription.manageUrl") { doesNotExist() }
        }
        mvc.get("/checkout") { param("account", user) }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.TEXT_HTML) }
            content { string(containsString("https://cdn.paddle.com/paddle/v2/paddle.js")) }
            content { string(containsString("Paddle.Environment.set('sandbox')")) }
            content { string(containsString("buy('pri_01monthly')")) }
            content { string(containsString("buy('pri_01yearly')")) }
            content { string(containsString("user_id: \"$user\"")) }
            content { string(not(containsString("</script>@example.com"))) }
        }
        mvc.get("/checkout") { param("account", UUID.randomUUID().toString()) }.andExpect { status { isNotFound() } }
        mvc.get("/checkout") { param("account", "nobody") }.andExpect { status { isNotFound() } }
    }

    @Test
    fun aStoresSubscriptionIsManagedInThatStore() {
        val (user, auth) = account()
        billing.record(SubscriptionState("google", "token-$user", UUID.fromString(user), "tayra.sync.yearly", "active",
            Instant.now().plus(30, ChronoUnit.DAYS), Instant.now()))
        mvc.get("/api/v1/me") { header("Authorization", auth) }.andExpect {
            jsonPath("$.subscription.active") { value(true) }
            jsonPath("$.subscription.checkoutUrl") { doesNotExist() }
            jsonPath("$.subscription.manageUrl") { value("https://play.google.com/store/account/subscriptions?package=com.tayra.languages&sku=tayra.sync.yearly") }
        }
        val (apple, appleAuth) = account()
        billing.record(SubscriptionState("apple", "orig-$apple", UUID.fromString(apple), "tayra.sync.yearly", "subscribed",
            Instant.now().plus(30, ChronoUnit.DAYS), Instant.now()))
        mvc.get("/api/v1/me") { header("Authorization", appleAuth) }.andExpect {
            jsonPath("$.subscription.manageUrl") { value("https://apps.apple.com/account/subscriptions") }
        }
    }
}
