package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.courses.BuiltInCourses
import com.tayra.languages.core.domain.courses.CourseLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class RemoteCoursesTest {

    private val body = """
        [{"id": "pt-server", "languageCode": "pt", "title": "Do servidor", "description": "From the server.", "level": "A2", "topic": null,
          "lessons": [
            {"id": "pt-server-1", "title": "Um", "summary": null, "text": "Primeiro texto.\n"},
            {"id": "pt-server-2", "title": "Dois", "summary": "Second.", "text": "Segundo texto."}
          ]}]
    """.trimIndent()

    private val urls = mutableListOf<String>()
    private var up = true
    private val clock = TestTimeSource()
    private val client = HttpClient(MockEngine { request ->
        urls += request.url.toString()
        if (up) respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json")) else respondError(HttpStatusCode.ServiceUnavailable)
    }) { install(HttpTimeout) }
    private val courses = RemoteCourses(client, BuiltInCourses(), baseUrl = "http://server.test", freshFor = 1.minutes, clock = clock)

    @Test
    fun theServersCoursesAreRead() = runTest {
        val course = courses.courses("PT").single()
        assertEquals("http://server.test/api/public/courses?language=pt", urls.single())
        assertEquals("pt-server", course.id)
        assertEquals(CourseLevel.A2, course.level)
        assertEquals("", course.topic)
        assertEquals(listOf("pt-server-1", "pt-server-2"), course.lessons.map { it.id })
        assertEquals("Primeiro texto.", course.lessons[0].text)
        assertEquals("", course.lessons[0].summary)
    }

    @Test
    fun coursesAreAskedForAgainOnlyOnceTheyAreOld() = runTest {
        courses.courses("pt")
        clock += 30.seconds
        courses.courses("pt")
        assertEquals("pt-server", courses.course("pt-server")?.id)
        assertEquals(1, urls.size, "fresh courses are not fetched again, and a course among them is not fetched")
        clock += 31.seconds
        courses.courses("pt")
        assertEquals(2, urls.size)
    }

    @Test
    fun theBuiltInCoursesAreShownUntilTheServerAnswers() = runTest {
        up = false
        assertEquals(BuiltInCourses.ALL.filter { it.languageCode == "pt" }.map { it.id }, courses.courses("pt").map { it.id })
        assertEquals("pt-primeiros-passos", courses.course("pt-primeiros-passos")?.id)
        assertNull(courses.course("no-such-course"))
        up = true
        assertEquals(listOf("pt-server"), courses.courses("pt").map { it.id }, "a failure is not remembered")
    }

    @Test
    fun coursesReadEarlierAreKeptWhileTheServerIsDown() = runTest {
        courses.courses("pt")
        up = false
        clock += 5.minutes
        assertEquals(listOf("pt-server"), courses.courses("pt").map { it.id })
    }
}
