package com.tayra.languages.backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals

/** A Postgres of its own for each test run, in Docker, as the service has in production. */
@TestConfiguration(proxyBeanMethods = false)
class Postgres {
    @Bean
    @ServiceConnection
    fun postgres() = PostgreSQLContainer("postgres:18")
}

/** The service starts on a fresh database, applies its schema, and answers. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(Postgres::class)
class BackendTest(@Autowired private val mvc: MockMvc, @Autowired private val jdbc: JdbcTemplate) {

    @Test
    fun itIsHealthyWithItsSchema() {
        mvc.get("/actuator/health").andExpect { status { isOk() }; jsonPath("$.status") { value("UP") } }
        val tables = jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name", String::class.java)
        assertEquals(listOf("email_codes", "flyway_schema_history", "refresh_tokens", "users"), tables)
    }

    @Test
    fun itTellsItsClockAsTheAppsWriteJson() {
        val before = System.currentTimeMillis()
        mvc.get("/api/v1/info").andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith("application/json") }
            jsonPath("$.service") { value("tayra-languages") }
            jsonPath("$.apiVersion") { value(1) }
            jsonPath("$.serverTime") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(before)) }
        }
    }
}
