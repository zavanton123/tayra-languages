package com.tayra.languages.backend

import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.SyncJson
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import com.tayra.languages.sync.Tokens
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A hundred devices of one account sync at once over HTTP, each changing words the others change
 * too: every one ends with the account's records, which are what the shared rules make of all
 * their changes, and none waits long.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(Postgres::class, TestMail::class)
@TestPropertySource(properties = ["tayra.free-emails=hundred@example.com"])
class ManyDevicesTest(@LocalServerPort private val port: Int, @Autowired private val jdbc: JdbcTemplate) {
    private val http = HttpClient.newHttpClient()

    private fun call(path: String, body: String?, auth: String? = null): String {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/api/v1$path")).apply {
            auth?.let { header("Authorization", it) }
            if (body != null) header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)) else GET()
        }.build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "$path answered ${response.statusCode()}: ${response.body()}" }
        return response.body()
    }

    private fun pullAll(auth: String): List<SyncRecord> {
        val all = mutableListOf<SyncRecord>()
        var since = 0L
        do {
            val page = SyncJson.decodeFromString(PullResponse.serializer(), call("/sync/pull?since=$since&limit=500", null, auth))
            all += page.records
            since = page.seq
        } while (page.more)
        return all
    }

    @Test
    fun aHundredDevicesConverge() {
        val auth = "Bearer " + SyncJson.decodeFromString(Tokens.serializer(),
            call("/auth/register", """{"email":"hundred@example.com","password":"a password","device":{"id":"d0"}}""")).accessToken
        val random = Random(7)
        val now = System.currentTimeMillis() - 3_600_000
        val sent = (1..100).map { device ->
            (1..20).map {
                val word = random.nextInt(200)
                val at = now + random.nextLong(1_000_000)
                SyncRecord("term", "pt/w$word", at, deleted = random.nextInt(10) == 0, data = buildJsonObject {
                    put("text", "w$word"); put("status", random.nextInt(1, 6)); put("statusChangedAt", now + random.nextLong(1_000_000)); put("translation", "t$device")
                }, deviceId = "device-$device")
            }
        }
        val pool = Executors.newFixedThreadPool(32)
        val started = System.nanoTime()
        val slowest = sent.mapIndexed { index, records ->
            pool.submit<Long> {
                val t = System.nanoTime()
                call("/sync/push", SyncJson.encodeToString(PushRequest.serializer(), PushRequest("device-${index + 1}", records)), auth)
                pullAll(auth)
                (System.nanoTime() - t) / 1_000_000
            }
        }.maxOf { it.get() }
        pool.shutdown()
        val total = (System.nanoTime() - started) / 1_000_000

        val expected = sent.flatten().groupBy { it.key }.mapValues { (_, versions) -> versions.reduce(SyncMerge::merge) }
        val kept = pullAll(auth).associateBy { it.key }
        assertEquals(expected.keys, kept.keys)
        for ((key, record) in expected) assertEquals(record, kept.getValue(key), key)
        assertTrue(slowest < 10_000, "each device synced within seconds: the slowest took $slowest ms, all $total ms")
        println("100 devices synced in $total ms, the slowest in $slowest ms")
        assertEquals(expected.size, jdbc.queryForObject("SELECT count(*) FROM records", Int::class.java))
    }
}
