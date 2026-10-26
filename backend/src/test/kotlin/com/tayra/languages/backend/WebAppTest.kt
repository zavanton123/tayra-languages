package com.tayra.languages.backend

import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.Tokens
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The web app, on an origin of its own, signs in with the refresh token in a cookie its scripts cannot read. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.web-origins=https://app.example.com"])
class WebAppTest(@Autowired private val mvc: MockMvc) {

    private val app = "https://app.example.com"

    @Test
    fun onlyTheWebAppsOriginMayCallWithCredentials() {
        mvc.options("/api/v1/auth/login") {
            header("Origin", app)
            header("Access-Control-Request-Method", "POST")
            header("Access-Control-Request-Headers", "content-type,x-tayra-client")
        }.andExpect {
            status { isOk() }
            header { string("Access-Control-Allow-Origin", app) }
            header { string("Access-Control-Allow-Credentials", "true") }
        }
        mvc.options("/api/v1/auth/login") {
            header("Origin", "https://elsewhere.example.com")
            header("Access-Control-Request-Method", "POST")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun theRefreshTokenStaysInTheCookie() {
        val email = "web-${UUID.randomUUID()}@example.com"
        val signedUp = mvc.post("/api/v1/auth/register") {
            header("Origin", app)
            header("X-Tayra-Client", "web")
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"a password","device":{"id":"browser-1","name":"Firefox","platform":"web"}}"""
        }.andExpect { status { isCreated() } }.andReturn().response
        val tokens = SyncJson.decodeFromString(Tokens.serializer(), signedUp.contentAsString)
        assertEquals("", tokens.refreshToken, "not where the page's scripts could read it")
        val cookie = signedUp.getCookie("tayra_refresh")!!
        assertTrue(cookie.isHttpOnly && cookie.secure && cookie.path == "/api/v1/auth" && cookie.value.isNotBlank())
        assertTrue(signedUp.getHeader("Set-Cookie")!!.contains("SameSite=Strict"))
        mvc.get("/api/v1/me") { header("Authorization", "Bearer ${tokens.accessToken}") }.andExpect { status { isOk() } }

        // The page renews its access token with the cookie alone, and gets a new cookie for the next time.
        val renewed = mvc.post("/api/v1/auth/refresh") {
            header("X-Tayra-Client", "web")
            cookie(Cookie("tayra_refresh", cookie.value))
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":""}"""
        }.andExpect { status { isOk() } }.andReturn().response
        assertEquals("", SyncJson.decodeFromString(Tokens.serializer(), renewed.contentAsString).refreshToken)
        val next = renewed.getCookie("tayra_refresh")!!.value

        // Signing out ends the sign-in and empties the cookie.
        val out = mvc.post("/api/v1/auth/logout") {
            header("X-Tayra-Client", "web")
            cookie(Cookie("tayra_refresh", next))
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":""}"""
        }.andExpect { status { isNoContent() } }.andReturn().response
        assertEquals(0, out.getCookie("tayra_refresh")!!.maxAge)
        mvc.post("/api/v1/auth/refresh") {
            header("X-Tayra-Client", "web")
            cookie(Cookie("tayra_refresh", next))
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":""}"""
        }.andExpect { status { isUnauthorized() } }
    }
}
