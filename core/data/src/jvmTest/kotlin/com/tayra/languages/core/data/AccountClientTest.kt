package com.tayra.languages.core.data

import com.tayra.languages.core.data.account.AccountClient
import com.tayra.languages.core.data.account.AccountServer
import com.tayra.languages.core.data.settings.InMemorySecureStore
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.sync.DeviceInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** The device's side of the account service, against answers made up here. */
class AccountClientTest {
    private val secure = InMemorySecureStore()
    private val seen = mutableListOf<String>()

    private fun tokens(access: String, refresh: String) =
        """{"accessToken":"$access","refreshToken":"$refresh","expiresIn":900,"user":{"id":"u1","email":"reader@example.com","emailVerified":false}}"""

    private val me = """{"id":"u1","email":"reader@example.com","emailVerified":true,"hasPassword":true,"providers":[],"subscription":{"active":true,"activeUntil":1800000000000,"source":"paddle"}}"""

    private fun client(server: String? = "http://service.test", answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = AccountClient(
        HttpClient(MockEngine { request ->
            seen += "${request.method.value} ${request.url.encodedPath} ${request.headers[HttpHeaders.Authorization].orEmpty()}".trim()
            answer(request)
        }) { install(HttpTimeout) },
        AccountServer { server },
        secure,
        device = { DeviceInfo("device-1", "Desk", "desktop") },
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun withoutAServiceNothingIsTried() = runTest {
        val client = client(server = null) { error("no request should be made") }
        assertFalse(client.available)
        assertEquals(AccountProblem.NOT_SET_UP, assertFailsWith<AccountException> { client.register("a@b.c", "password") }.problem)
    }

    @Test
    fun signingUpKeepsTheSignInAndAsksWithIt() = runTest {
        val client = client { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/register" -> {
                    assertEquals(
                        """{"email":"reader@example.com","password":"correct horse","device":{"id":"device-1","name":"Desk","platform":"desktop"}}""",
                        String(request.body.toByteArray()),
                    )
                    json(tokens("access-1", "refresh-1"), HttpStatusCode.Created)
                }
                "/api/v1/me" -> json(me)
                else -> error(request.url.encodedPath)
            }
        }
        val account = client.register("reader@example.com", "correct horse")
        assertEquals("reader@example.com", client.signedIn.value)
        assertEquals("refresh-1", secure.get("account.http://service.test.refresh_token"))
        assertEquals(true, account.plan.active)
        assertEquals("paddle", account.plan.source)
        assertEquals(listOf("POST /api/v1/auth/register", "GET /api/v1/me Bearer access-1"), seen)
        assertEquals("http://service.test u1", client.syncAccount(), "sync knows the account by its id")
        client.signOut()
        assertNull(client.syncAccount())
    }

    /** An access token the service refuses is renewed once with the refresh token, and the request made again. */
    @Test
    fun aRefusedAccessTokenIsRenewedOnce() = runTest {
        secure.put("account.http://service.test.refresh_token", "refresh-1")
        secure.put("account.http://service.test.email", "reader@example.com")
        var refused = false
        val client = client { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/refresh" -> json(tokens(if (refused) "access-3" else "access-2", "refresh-2"))
                "/api/v1/me" -> if (!refused) { refused = true; json("""{"error":"signed_out","message":"x"}""", HttpStatusCode.Unauthorized) } else json(me)
                else -> error(request.url.encodedPath)
            }
        }
        assertEquals("reader@example.com", client.signedIn.value, "signed in from what was kept")
        client.account()
        assertEquals(listOf("POST /api/v1/auth/refresh", "GET /api/v1/me Bearer access-2", "POST /api/v1/auth/refresh", "GET /api/v1/me Bearer access-3"), seen)
        assertEquals("refresh-2", secure.get("account.http://service.test.refresh_token"), "the spent refresh token is replaced")
    }

    @Test
    fun aRefusedRenewalSignsTheDeviceOut() = runTest {
        secure.put("account.http://service.test.refresh_token", "spent")
        secure.put("account.http://service.test.email", "reader@example.com")
        val client = client { json("""{"error":"signed_out","message":"This device is signed out"}""", HttpStatusCode.Unauthorized) }
        assertEquals(AccountProblem.SIGNED_OUT, assertFailsWith<AccountException> { client.devices() }.problem)
        assertNull(client.signedIn.value)
        assertNull(secure.get("account.http://service.test.refresh_token"))
    }

    @Test
    fun refusalsAndFailuresSayWhy() = runTest {
        val taken = client { json("""{"error":"email_taken","message":"An account with this email exists already"}""", HttpStatusCode.Conflict) }
        val e = assertFailsWith<AccountException> { taken.register("reader@example.com", "password") }
        assertEquals(AccountProblem.EMAIL_TAKEN, e.problem)
        assertEquals("An account with this email exists already", e.message)
        val down = client { throw IOException("Connection refused") }
        assertEquals(AccountProblem.OFFLINE, assertFailsWith<AccountException> { down.signIn("reader@example.com", "password") }.problem)
        val broken = client { respond("oops", HttpStatusCode.BadGateway) }
        assertEquals(AccountProblem.SERVER, assertFailsWith<AccountException> { broken.forgotPassword("reader@example.com") }.problem)
        val busy = client { json("""{"error":"rate_limited","message":"Too many requests; try again in a minute"}""", HttpStatusCode.TooManyRequests) }
        assertEquals(AccountProblem.BUSY, assertFailsWith<AccountException> { busy.signIn("reader@example.com", "password") }.problem)
    }

    @Test
    fun aLibraryFileGoesUpAsBytesAndComesBack() = runTest {
        secure.put("account.http://service.test.refresh_token", "refresh-1")
        val file = byteArrayOf(1, 2, 3, 4)
        var sent = ByteArray(0)
        val client = client { request ->
            when {
                request.url.encodedPath == "/api/v1/auth/refresh" -> json(tokens("access-2", "refresh-2"))
                request.method.value == "POST" -> {
                    sent = request.body.toByteArray()
                    assertEquals("tayra-backup.tayra", request.url.parameters["name"])
                    assertEquals("Desk", request.url.parameters["device"])
                    json("""{"id":"b1","name":"tayra-backup.tayra","deviceName":"Desk","createdAt":1700000000000,"sizeBytes":4}""", HttpStatusCode.Created)
                }
                else -> respond(file, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/octet-stream"))
            }
        }
        assertEquals("b1", client.upload("tayra-backup.tayra", file).id)
        assertContentEquals(file, sent)
        assertContentEquals(file, client.download("b1"))
    }
}
