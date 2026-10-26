package com.tayra.languages.backend

import com.tayra.languages.backend.auth.Mailer
import com.tayra.languages.backend.auth.Tokens
import com.tayra.languages.sync.SyncJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The mail the service sends, kept instead of sent. */
class Outbox : Mailer {
    val sent = mutableListOf<Triple<String, String, String>>()
    override fun send(to: String, subject: String, text: String) { sent += Triple(to, subject, text) }
    fun codeFor(email: String): String = Regex("""\b\d{6}\b""").find(sent.last { it.first == email }.second)!!.value
}

@TestConfiguration(proxyBeanMethods = false)
class TestMail {
    @Bean
    @Primary
    fun outbox() = Outbox()
}

/** Codes sent by email confirm the address and replace a forgotten password. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
class EmailCodesTest(@Autowired private val mvc: MockMvc, @Autowired private val outbox: Outbox) {

    @BeforeEach
    fun clear() = outbox.sent.clear()

    private fun post(path: String, body: String, token: String? = null) = mvc.post(path) {
        contentType = MediaType.APPLICATION_JSON
        content = body
        token?.let { header("Authorization", "Bearer $it") }
    }

    private fun register(email: String) = SyncJson.decodeFromString(Tokens.serializer(),
        post("/api/v1/auth/register", """{"email":"$email","password":"first password","device":{"id":"d1"}}""").andReturn().response.contentAsString)

    @Test
    fun theCodeSentOnSignUpConfirmsTheAddress() {
        val email = "new-${UUID.randomUUID()}@example.com"
        val tokens = register(email)
        assertEquals(email, outbox.sent.single().first, "the code goes out on sign-up")
        val code = outbox.codeFor(email)
        val wrong = if (code == "000000") "111111" else "000000"
        post("/api/v1/me/email/verify", """{"code":"$wrong"}""", tokens.accessToken).andExpect { status { isBadRequest() }; jsonPath("$.error") { value("wrong_code") } }
        post("/api/v1/me/email/verify", """{"code":"$code"}""", tokens.accessToken).andExpect { status { isNoContent() } }
        mvc.get("/api/v1/me") { header("Authorization", "Bearer ${tokens.accessToken}") }.andExpect { jsonPath("$.emailVerified") { value(true) } }
        post("/api/v1/me/email/verify", """{"code":"$code"}""", tokens.accessToken).andExpect { status { isBadRequest() } }
    }

    @Test
    fun aCodeAllowsFiveTries() {
        val email = "tries-${UUID.randomUUID()}@example.com"
        val tokens = register(email)
        val code = outbox.codeFor(email)
        val wrong = if (code == "000000") "111111" else "000000"
        repeat(5) { post("/api/v1/me/email/verify", """{"code":"$wrong"}""", tokens.accessToken).andExpect { status { isBadRequest() } } }
        post("/api/v1/me/email/verify", """{"code":"$code"}""", tokens.accessToken).andExpect { status { isBadRequest() } }
        post("/api/v1/me/email/send-code", "", tokens.accessToken).andExpect { status { isNoContent() } }
        post("/api/v1/me/email/verify", """{"code":"${outbox.codeFor(email)}"}""", tokens.accessToken).andExpect { status { isNoContent() } }
    }

    @Test
    fun aForgottenPasswordIsReplacedAndEveryDeviceSignedOut() {
        val email = "forgot-${UUID.randomUUID()}@example.com"
        val tokens = register(email)
        outbox.sent.clear()
        post("/api/v1/auth/password/forgot", """{"email":"nobody-${UUID.randomUUID()}@example.com"}""").andExpect { status { isNoContent() } }
        assertTrue(outbox.sent.isEmpty(), "no account, no mail, and the answer is the same")

        post("/api/v1/auth/password/forgot", """{"email":"${email.uppercase()}"}""").andExpect { status { isNoContent() } }
        val code = outbox.codeFor(email)
        post("/api/v1/auth/password/reset", """{"email":"$email","code":"$code","password":"second password"}""").andExpect { status { isNoContent() } }
        post("/api/v1/auth/login", """{"email":"$email","password":"first password","device":{"id":"d2"}}""").andExpect { status { isUnauthorized() } }
        post("/api/v1/auth/login", """{"email":"$email","password":"second password","device":{"id":"d2"}}""").andExpect { status { isOk() } }
        post("/api/v1/auth/refresh", """{"refreshToken":"${tokens.refreshToken}"}""").andExpect { status { isUnauthorized() } }
    }
}
