package com.tayra.languages.backend

import com.tayra.languages.backend.auth.Tokens
import com.tayra.languages.sync.SyncJson
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** S3-compatible storage of its own for the test run, as the service has in production. */
@TestConfiguration(proxyBeanMethods = false)
class Storage {
    @Bean
    fun s3(): GenericContainer<*> = GenericContainer("adobe/s3mock:4.7.0").withExposedPorts(9090).waitingFor(Wait.forHttp("/").forPort(9090)).apply { start() }

    @Bean
    fun s3Properties(s3: GenericContainer<*>) = DynamicPropertyRegistrar { registry ->
        registry.add("tayra.storage.endpoint") { "http://${s3.host}:${s3.getMappedPort(9090)}" }
    }
}

/** Library files kept in the cloud: uploaded with a subscription, listed and taken back without one. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class, TestMail::class, Storage::class)
@TestPropertySource(properties = ["tayra.free-emails=backer@example.com", "tayra.backups.keep=2"])
class BackupsTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    private fun signIn(email: String): String {
        val body = mvc.post("/api/v1/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email","password":"a password","device":{"id":"desk"}}""" }
            .andReturn().response.contentAsString
        return "Bearer " + SyncJson.decodeFromString(Tokens.serializer(), body).accessToken
    }

    /** A file that starts like an SQLite file, as a backup does. */
    private fun library(text: String) = "SQLite format 3\u0000".toByteArray() + text.toByteArray()

    private fun upload(auth: String, name: String, bytes: ByteArray) = mvc.post("/api/v1/backups") {
        header("Authorization", auth)
        contentType = MediaType.APPLICATION_OCTET_STREAM
        param("name", name)
        param("device", "Desk")
        content = bytes
    }

    @Test
    fun aBackupIsKeptListedAndTakenBack() {
        val auth = signIn("backer@example.com")
        val bytes = library("the whole library")
        upload(auth, "first.tayra", bytes).andExpect { status { isCreated() }; jsonPath("$.sizeBytes") { value(bytes.size) } }
        val list = mvc.get("/api/v1/backups") { header("Authorization", auth) }.andExpect { jsonPath("$.length()") { value(1) } }.andReturn().response.contentAsString
        val id = Regex(""""id":"([^"]+)"""").find(list)!!.groupValues[1]
        val back = mvc.get("/api/v1/backups/$id") { header("Authorization", auth) }.andExpect { status { isOk() } }.andReturn().response.contentAsByteArray
        assertContentEquals(bytes, back)

        // Only the newest are kept.
        upload(auth, "second.tayra", library("2")).andExpect { status { isCreated() } }
        upload(auth, "third.tayra", library("3")).andExpect { status { isCreated() } }
        mvc.get("/api/v1/backups") { header("Authorization", auth) }.andExpect {
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].name") { value("third.tayra") }
            jsonPath("$[1].name") { value("second.tayra") }
        }
        mvc.get("/api/v1/backups/$id") { header("Authorization", auth) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun uploadingNeedsASubscriptionAndALibraryFile() {
        val email = "lapsed-${UUID.randomUUID()}@example.com"
        val auth = signIn(email)
        upload(auth, "x.tayra", library("x")).andExpect { status { isPaymentRequired() }; jsonPath("$.error") { value("subscription_required") } }
        mvc.get("/api/v1/backups") { header("Authorization", auth) }.andExpect { status { isOk() }; jsonPath("$.length()") { value(0) } }

        jdbc.update("UPDATE users SET subscription_active_until = now() + interval '1 day' WHERE email = ?", email)
        upload(auth, "notes.txt", "just some text".toByteArray()).andExpect { status { isBadRequest() } }
        upload(auth, "lib.tayra", library("ok")).andExpect { status { isCreated() } }
        // The subscription lapses; the library can still be taken back.
        jdbc.update("UPDATE users SET subscription_active_until = now() - interval '1 day' WHERE email = ?", email)
        val id = Regex(""""id":"([^"]+)"""").find(mvc.get("/api/v1/backups") { header("Authorization", auth) }.andReturn().response.contentAsString)!!.groupValues[1]
        mvc.get("/api/v1/backups/$id") { header("Authorization", auth) }.andExpect { status { isOk() } }
    }

    @Test
    fun noOneElsesBackupsAreReachable() {
        val email = "owner-${UUID.randomUUID()}@example.com"
        val owner = signIn(email)
        jdbc.update("UPDATE users SET subscription_active_until = now() + interval '1 day' WHERE email = ?", email)
        upload(owner, "mine.tayra", library("mine")).andExpect { status { isCreated() } }
        val id = Regex(""""id":"([^"]+)"""").find(mvc.get("/api/v1/backups") { header("Authorization", owner) }.andReturn().response.contentAsString)!!.groupValues[1]
        val stranger = signIn("stranger-${UUID.randomUUID()}@example.com")
        mvc.get("/api/v1/backups/$id") { header("Authorization", stranger) }.andExpect { status { isNotFound() } }
        mvc.delete("/api/v1/backups/$id") { header("Authorization", stranger) }.andExpect { status { isNotFound() } }

        // Deleting the account takes its files with it.
        mvc.delete("/api/v1/me") { header("Authorization", owner) }.andExpect { status { isNoContent() } }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM backups WHERE id = ?::uuid", Int::class.java, id))
    }
}
