package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.StorybooksCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Global Storybooks: a language's stories from every collection, each African Storybook story once, read as they are listed. */
class StorybooksCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val listings = mapOf(
        "sbc-source" to listOf("/pt/0001_um-homem-muito-alto.md", "/pt/0002_veja-os-animais.md", "/de/0001_ein-sehr-großer-mann.md", "/README.md"),
        "asp-source" to listOf("/pt/0001_um-homem-muito-alto.md", "/pt/0368_o-leão.md", "/ha-ne/0005_labari.md"),
        "sbno-source" to listOf("/nb/0001_en-veldig-hoy-mann.md", "/nn/0001_ein-svært-høg-mann.md"),
        "lida-source" to listOf("/pt/0001_a-escola.md"),
    )

    private val catalog = StorybooksCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val path = request.url.encodedPath.decodeURLPart()
            when {
                request.url.host == "data.jsdelivr.com" -> {
                    val collection = path.substringAfter("/global-asp/").substringBefore('@')
                    val files = listings[collection].orEmpty().joinToString(",") { """{"name": "$it", "size": 600}""" }
                    respond("""{"default": "/README.md", "files": [$files]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                path.endsWith("0001_um-homem-muito-alto.md") -> respond(TALL_MAN, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
                path.endsWith("0002_veja-os-animais.md") -> respond("not here", HttpStatusCode.NotFound)
                else -> respond("# ${path.substringAfterLast('_').removeSuffix(".md")}\n\n##\nUma frase.\n\n##\n* License: [CC-BY]\n* Text: Alguém\n", HttpStatusCode.OK)
            }
        }) { install(HttpTimeout) },
        listUrl = "https://data.jsdelivr.com/v1/package/gh/global-asp",
        fileUrl = "https://cdn.jsdelivr.net/gh/global-asp",
    )

    @Test
    fun aLanguagesStoriesComeFromEveryCollectionEachOnce() = runTest {
        val page = catalog.search("pt", "", 1)
        // Story 0001 is in two collections; the first has it. LIDA numbers its stories its own way.
        assertEquals(listOf("sbc-source/pt/0001_um-homem-muito-alto", "sbc-source/pt/0002_veja-os-animais", "asp-source/pt/0368_o-leão", "lida-source/pt/0001_a-escola"), page.books.map { it.id })
        assertEquals(4, page.total)
        assertFalse(page.hasMore)

        val tall = page.books.first()
        assertEquals("Um homem muito alto", tall.title, "the title the story gives")
        assertEquals(listOf("Cornelius Gulere"), tall.authors)
        assertEquals("CC-BY", tall.license)
        assertEquals("https://cdn.jsdelivr.net/gh/global-asp/sbc-source@master/pt/0001_um-homem-muito-alto.md", tall.downloadUrl)
        assertEquals("https://github.com/global-asp/sbc-source/blob/master/pt/0001_um-homem-muito-alto.md", tall.pageUrl)
        val unread = page.books[1]
        assertEquals("Veja os animais", unread.title, "a story that cannot be read keeps the title of its file")

        catalog.search("pt", "", 1)
        assertEquals(10, requests.count { it.url.host == "data.jsdelivr.com" }, "each collection is listed once")
    }

    @Test
    fun aStoryIsItsPagesWithoutItsTitleAndCredits() = runTest {
        val tall = catalog.search("pt", "homem", 1).books.single()
        val before = requests.size
        assertEquals("A enxada dele era muito pequena.\nA porta dele era muito baixa.\nEle viveu por muitos anos.", catalog.text(tall))
        assertEquals(tall.let { catalog.sample(it) }, catalog.text(tall))
        assertEquals(before, requests.size, "a story read for its title is not fetched again")
    }

    @Test
    fun norwegianIsBokmaalAndARegionalFolderCountsForItsLanguage() = runTest {
        assertEquals(listOf("sbno-source/nb/0001_en-veldig-hoy-mann"), catalog.search("no", "", 1).books.map { it.id })
        assertEquals(listOf("asp-source/ha-ne/0005_labari"), catalog.search("ha", "", 1).books.map { it.id })
        assertTrue(catalog.search("ru", "", 1).books.isEmpty())
    }

    private companion object {
        val TALL_MAN = "# Um homem muito alto\n\n##\nA enxada dele era muito pequena.\n\n##\nA porta dele era muito baixa.\n\n##\nEle viveu por muitos anos.\n\n##\n" +
            "* License: [CC-BY]\n* Text: Cornelius Gulere\n* Illustration: Catherine Groenewald\n* Translation: Alguém\n* Language: pt\n"
    }
}
