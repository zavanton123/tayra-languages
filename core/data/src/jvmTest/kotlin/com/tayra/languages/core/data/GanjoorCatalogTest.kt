package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.GanjoorCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ganjoor's texts: the Golestan's stories chapter by chapter, a search through every poet, and texts without their short vowel marks. */
class GanjoorCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private val catalog = GanjoorCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/ganjoor/cat/1665" -> respond(cat("گلستان", "/saadi/golestan", children = listOf(1666, 1667), poems = listOf(41293 to "دیباچه")), HttpStatusCode.OK, json)
                "/api/ganjoor/cat/1666" -> respond(cat("باب اول در سیرت پادشاهان", "/saadi/golestan/gbab1", poems = (1..25).map { 41293 + it to "حکایت شمارهٔ $it" }), HttpStatusCode.OK, json)
                "/api/ganjoor/cat/1667" -> respond(cat("باب دوم در اخلاق درویشان", "/saadi/golestan/gbab2", poems = listOf(41400 to "حکایت شمارهٔ ۱")), HttpStatusCode.OK, json)
                "/api/ganjoor/poems/search" -> respond(
                    """[{"id": 9325, "title": "غزل شمارهٔ  ۳", "fullTitle": "سعدی » دیوان اشعار » غزلیات » غزل شمارهٔ  ۳", "fullUrl": "/saadi/divan/ghazals/sh3", "plainText": "روی تو خوش می نماید آینه ما"}]""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType to listOf("application/json"), "paging-headers" to listOf("""{"totalCount":318,"pageSize":20,"currentPage":1,"totalPages":16,"hasPreviousPage":false,"hasNextPage":true}""")),
                )
                "/api/ganjoor/poem/41294" -> respond(
                    """{"id": 41294, "title": "حکایت شمارهٔ ۱", "plainText": "پادشاهی را شنیدم", "htmlText": "<div class=\"n\" id=\"bn1\"><p>پادشاهی را شنیدم به کُشتنِ اسیری اشارت کرد.</p></div>\n<div class=\"b\" id=\"bn2\"><div class=\"m1\"><p>وقتِ ضرورت چو نَمانَد گریز</p></div>\n<div class=\"m2\"><p>دست بگیرَد سَرِ شَمشیرِ تیز</p></div></div>"}""",
                    HttpStatusCode.OK, json,
                )
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheGolestansStoriesChapterByChapter() = runTest {
        val first = catalog.search("fa", "", 1)
        assertEquals(listOf("/api/ganjoor/cat/1665", "/api/ganjoor/cat/1666"), requests.map { it.url.encodedPath }, "chapters are fetched as the list needs them")
        assertEquals("true", requests.first().url.parameters["poems"])
        assertEquals(20, first.books.size)
        assertTrue(first.hasMore)
        assertEquals(null, first.total)

        val preface = first.books.first()
        assertEquals("41293", preface.id)
        assertEquals("دیباچه", preface.title)
        assertEquals(listOf("سعدی"), preface.authors)
        assertEquals("گلستان", preface.description)
        assertEquals("https://ganjoor.net/saadi/golestan/sh41293", preface.pageUrl)
        val story = first.books[1]
        assertEquals("حکایت شمارهٔ 1", story.title)
        assertEquals("باب اول در سیرت پادشاهان", story.description)
        assertEquals("https://api.ganjoor.net/api/ganjoor/poem/41294", story.downloadUrl)

        val second = catalog.search("fa", "", 2)
        assertEquals(7, second.books.size)
        assertFalse(second.hasMore)
        assertEquals(27, second.total)
    }

    @Test
    fun aSearchLooksThroughEveryPoet() = runTest {
        val page = catalog.search("fa", "آینه", 2)
        val q = requests.single().url.parameters
        assertEquals("آینه", q["term"])
        assertEquals("0", q["poetId"])
        assertEquals("2", q["PageNumber"])
        assertEquals(318, page.total)
        assertTrue(page.hasMore)
        val ghazal = page.books.single()
        assertEquals("غزل شمارهٔ  ۳", ghazal.title)
        assertEquals(listOf("سعدی"), ghazal.authors)
        assertEquals("دیوان اشعار » غزلیات", ghazal.description)
        assertEquals("https://ganjoor.net/saadi/divan/ghazals/sh3", ghazal.pageUrl)
    }

    @Test
    fun aTextKeepsItsLinesAndLosesItsVowelMarks() = runTest {
        val story = catalog.search("fa", "", 1).books[1]
        val expected = "پادشاهی را شنیدم به کشتن اسیری اشارت کرد.\nوقت ضرورت چو نماند گریز\nدست بگیرد سر شمشیر تیز"
        assertEquals(expected, catalog.text(story))
        assertEquals(expected, catalog.sample(story))
        assertEquals("false", requests.last().url.parameters["comments"], "the poem comes alone")
    }

    private fun cat(title: String, url: String, children: List<Int> = emptyList(), poems: List<Pair<Int, String>>) = """{"poet": {"id": 7}, "cat": {
        "id": 1, "title": "$title", "fullUrl": "$url",
        "children": [${children.joinToString { """{"id": $it, "title": "باب"}""" }}],
        "poems": [${poems.joinToString { (id, name) -> """{"id": $id, "title": "$name", "urlSlug": "sh$id", "excerpt": ""}""" }}]}}"""
}
