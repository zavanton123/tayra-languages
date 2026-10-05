package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseSource
import com.tayra.languages.core.domain.courses.Lesson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/** The address of the Tayra Languages server as this platform reaches it. */
expect fun backendUrl(): String

/**
 * The courses published on the Tayra Languages server. A language's courses are asked for again
 * once they are [freshFor] old. While the server cannot be reached, the courses read from it
 * earlier are used, and before any were read, the courses of [fallback].
 */
class RemoteCourses(
    private val client: HttpClient,
    private val fallback: CourseSource,
    private val baseUrl: String = backendUrl(),
    private val freshFor: Duration = 1.minutes,
    private val clock: TimeSource = TimeSource.Monotonic,
) : CourseSource {

    private class Fetched(val courses: List<Course>, val at: kotlin.time.TimeMark)

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val byLanguage = mutableMapOf<String, Fetched>()

    override suspend fun courses(languageCode: String): List<Course> = mutex.withLock {
        val code = languageCode.lowercase()
        val known = byLanguage[code]
        if (known != null && known.at.elapsedNow() < freshFor) return known.courses
        val fetched = fetch("courses in $code") { client.get("$baseUrl/api/public/courses") { parameter("language", code); timeout { requestTimeoutMillis = TIMEOUT_MILLIS } } }
            ?.let { body -> parse(body) { json.parseToJsonElement(it).jsonArray.map(::course) } }
        if (fetched != null) {
            byLanguage[code] = Fetched(fetched, clock.markNow())
            fetched
        } else {
            known?.courses ?: fallback.courses(code)
        }
    }

    override suspend fun course(id: String): Course? {
        mutex.withLock { byLanguage.values.firstNotNullOfOrNull { fetched -> fetched.courses.firstOrNull { it.id == id } } }?.let { return it }
        return fetch("course $id") { client.get("$baseUrl/api/public/courses/$id") { timeout { requestTimeoutMillis = TIMEOUT_MILLIS } } }
            ?.let { body -> parse(body) { course(json.parseToJsonElement(it)) } }
            ?: fallback.course(id)
    }

    private suspend fun fetch(what: String, request: suspend () -> io.ktor.client.statement.HttpResponse): String? = try {
        val response = request()
        if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
    } catch (e: Exception) {
        Logger.w { "Could not read $what from $baseUrl: ${e.message}" }
        null
    }

    private fun <T> parse(body: String, read: (String) -> T): T? = runCatching { read(body) }
        .onFailure { Logger.w(it) { "Could not parse the courses from $baseUrl" } }
        .getOrNull()

    private fun course(element: JsonElement): Course {
        val course = element.jsonObject
        return Course(
            id = course.text("id"),
            languageCode = course.text("languageCode").lowercase(),
            title = course.text("title"),
            description = course.text("description"),
            level = CourseLevel.entries.first { it.code == course.text("level") },
            topic = course.text("topic"),
            lessons = course.getValue("lessons").jsonArray.map { it.jsonObject }.map { lesson ->
                Lesson(lesson.text("id"), lesson.text("title"), lesson.text("summary"), lesson.text("text").trim())
            },
        )
    }

    /** The text of a field, empty when it was left out. */
    private fun JsonObject.text(name: String): String = this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
