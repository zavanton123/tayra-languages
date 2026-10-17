package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.BenYehudaCatalog
import com.tayra.languages.core.data.discover.Csv
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

/** Project Ben-Yehuda's works: its catalog fetched once, fables then stories unsearched, a search without vowel points, and texts without their title. */
class BenYehudaCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val base = "https://raw.githubusercontent.com/projectbenyehuda/public_domain_dump/master"

    private val catalog = BenYehudaCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val text = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")
            when (request.url.encodedPath) {
                "/projectbenyehuda/public_domain_dump/master/pseudocatalogue.csv" -> respond(CATALOG, HttpStatusCode.OK, text)
                "/projectbenyehuda/public_domain_dump/master/txt_stripped/p31/m11.txt" -> respond("אגדת ההיכל\n\t\n\n\n\nבתוך: \"הפועל הצעיר\" 30.7.1908\n\n\n(מוקדש למוסד אחד בירושלים)\n\nכממלכת-סתרים, בארץ-פלאות היתה עיר.\n", HttpStatusCode.OK, text)
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheFablesThenTheStories() = runTest {
        val page = catalog.search("he", "", 1)
        assertEquals(listOf("40", "11", "17"), page.books.map { it.id })
        assertEquals(3, page.total)
        assertFalse(page.hasMore)

        val fable = page.books.first()
        assertEquals("הזאב והכבש", fable.title)
        assertEquals(listOf("יהודה שטיינברג"), fable.authors)
        assertEquals(listOf("fables"), fable.subjects)
        assertEquals("https://benyehuda.org/read/40", fable.pageUrl)
        assertEquals("$base/txt_stripped/p12/m40.txt", fable.downloadUrl)

        catalog.search("he", "", 1)
        assertEquals(1, requests.size, "the catalog is fetched once")
    }

    @Test
    fun aSearchMatchesTitlesAuthorsAndTranslatorsWhateverTheirVowelPoints() = runTest {
        assertEquals(listOf("11"), catalog.search("he", "אֲגָדַת", 1).books.map { it.id }, "a pointed word finds the pointed title")
        assertEquals(listOf("11"), catalog.search("he", "אגדת יאן", 1).books.map { it.id })
        assertEquals(listOf("16"), catalog.search("he", "גורדון", 1).books.map { it.id }, "a poem is found by a search")
        assertEquals(listOf("20"), catalog.search("he", "ביאליק", 1).books.map { it.id }, "and a translation by its translator")
        assertEquals(emptyList(), catalog.search("he", "מילון", 1).books, "reference works are left out")
    }

    @Test
    fun aTextLosesItsTitle() = runTest {
        val book = catalog.search("he", "אגדת", 1).books.single()
        val expected = "בתוך: \"הפועל הצעיר\" 30.7.1908\n(מוקדש למוסד אחד בירושלים)\nכממלכת-סתרים, בארץ-פלאות היתה עיר."
        assertEquals(expected, catalog.text(book))
        assertEquals(expected, catalog.sample(book))
        assertEquals("bytes=0-65535", requests.last().headers[HttpHeaders.Range])
    }

    @Test
    fun csvFieldsInQuotesKeepTheirCommasQuotesAndLineBreaks() {
        assertEquals(
            listOf(listOf("a", "b, c", "say \"hi\"", ""), listOf("line\nbreak", "x", "", "y")),
            Csv.rows("a,\"b, c\",\"say \"\"hi\"\"\",\r\n\"line\nbreak\",x,,y\n\n"),
        )
        assertEquals(listOf(mapOf("ti_id" to "1", "titel" to "Max Havelaar")), Csv.records("﻿ti_id|titel\n1|Max Havelaar\n", '|'))
    }

    private companion object {
        val CATALOG = """
            ID,path,title,authors,translators,author_uris,translator_uris,original_language,genre,source_edition
            11,/p31/m11,אֲגָדַת הַהֵיכָל,אירה יאן,"",https://wikidata.org/wiki/Q4203038,"","",Translation missing: he.prose,""
            16,/p46/m16,לידידי הרב החכם רב־הפעלים,יהודה ליב גורדון,"",https://wikidata.org/wiki/Q1376036,"","",Translation missing: he.poetry,"מתוך ""כתבי יהודה ליב גורדון"", תל אביב: דביר, תשי""ט 1959"
            17,/p87/m17,יצחק מאיר בילדותו,עזריאל נתן פרנק,"",https://wikidata.org/wiki/Q9251083,"","",Translation missing: he.prose,""
            20,/p1/m20,וילהלם טל,פרידריך שילר,חיים נחמן ביאליק,"","",de,Translation missing: he.drama,""
            30,/p2/m30,מילון עברי,אליעזר בן־יהודה,"","","","",Translation missing: he.reference,""
            40,/p12/m40,הזאב והכבש,יהודה שטיינברג,"","","","",Translation missing: he.fables,""
        """.trimIndent()
    }
}
