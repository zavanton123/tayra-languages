package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.StoryWeaverCatalog
import com.tayra.languages.core.domain.books.discover.Difficulty
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** StoryWeaver's books: listed from a language's index, read from their PDFs without StoryWeaver's own pages. */
class StoryWeaverCatalogTest {

    private val index = listOf(
        listOf("ab12", "1", "190", "\"Esse peixe é meu!\" \"Não, é meu!\"", "Suraj J Menon, tr. Thomas Suzaki", "Uma pequena história sobre três amigos.", "cd34"),
        listOf("ef56", "3", "800", "A Capa de Chuva Vermelha", "Kiran Kasturia", "Pedro ganhou uma capa de chuva nova.", ""),
        listOf("ab78", "", "120", "Sem nível", "", "", ""),
    ).joinToString("") { it.joinToString("\t") + "\n" }

    /** What pdftotext gives for a StoryWeaver PDF: the cover, pages ending in their numbers, the attributions, the back cover. */
    private val fish = """
        "Esse peixe é meu!" "Não, é
        meu!"
        Author: Suraj J Menon
        Illustrator: Soumya Menon
        Translator: Thomas Suzaki
        Kicchu acordou cedo um dia. "É hora de pegar um
        peixe", ele disse.
        2
        Ele encontrou Choru no caminho para a lagoa.
        3
        Story Attribution:
        This story: "Esse peixe é meu!" is translated by Thomas Suzaki. Released under CC BY 4.0 license.
        (Portuguese)
        This is a Level 1 book for children who are eager to begin reading.
    """.trimIndent()

    private val bird = "鳥中之王\nAuthor: 某人\n從前從前，⿃兒們開了⼀次會。\n2\nThis book was made possible by Pratham Books' StoryWeaver platform."

    private val catalog = StoryWeaverCatalog(
        HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/pt.tsv") -> respond(index, HttpStatusCode.OK)
                path.endsWith(".tsv") -> respond("Not found", HttpStatusCode.NotFound)
                path.endsWith("/ab12.pdf") -> respond("%PDF-fish", HttpStatusCode.OK)
                path.endsWith("/ef56.pdf") -> respond("%PDF-bird", HttpStatusCode.OK)
                else -> respond("<html>gone</html>", HttpStatusCode.OK)
            }
        }) { install(HttpTimeout) },
        indexUrl = "https://cdn.example/storyweaver",
        storageUrl = "https://studio.example/content/storage",
        pdf = { bytes -> if (bytes.decodeToString() == "%PDF-fish") fish else bird },
    )

    @Test
    fun aLanguagesBooksComeFromItsIndexWithTheirLevelsAndCovers() = runTest {
        val page = catalog.search("pt", "", 1)
        assertEquals(3, page.total)
        assertFalse(page.hasMore)
        val fishBook = page.books.first()
        assertEquals("\"Esse peixe é meu!\" \"Não, é meu!\"", fishBook.title)
        assertEquals(listOf("Suraj J Menon", "tr. Thomas Suzaki"), fishBook.authors)
        assertEquals("Level 1", fishBook.level)
        assertEquals(Difficulty.EASY, fishBook.levelDifficulty)
        assertEquals("CC-BY-4.0", fishBook.license)
        assertEquals("https://studio.example/content/storage/a/b/ab12.pdf", fishBook.downloadUrl)
        assertEquals("https://studio.example/content/storage/c/d/cd34.jpg", fishBook.coverUrl)
        assertEquals(Difficulty.MEDIUM, page.books[1].levelDifficulty)
        assertEquals(null, page.books[2].level, "a book without a level")

        assertEquals(listOf("ef56"), catalog.search("pt", "chuva pedro", 1).books.map { it.id }, "found by its title and description")
        assertTrue(catalog.search("de", "", 1).books.isEmpty(), "a language without an index has no books")
    }

    @Test
    fun aBooksTextIsItsStoryWithoutStoryWeaversPages() = runTest {
        val (fishBook, birdBook, missing) = catalog.search("pt", "", 1).books
        // The title over two lines and the credits go, and so do the page numbers and everything from the attributions on.
        assertEquals("Kicchu acordou cedo um dia. \"É hora de pegar um peixe\", ele disse.\nEle encontrou Choru no caminho para a lagoa.", catalog.text(fishBook))
        // Kangxi radicals become the characters they stand for; Chinese punctuation stays as it is.
        assertEquals("從前從前，鳥兒們開了一次會。", catalog.text(birdBook))
        assertTrue("PDF" in assertFailsWith<Exception> { catalog.text(missing) }.message!!)
    }
}
