package com.tayra.languages.core.data

import com.tayra.languages.core.data.network.CommonsRecordingSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonsRecordingSourceTest {

    private val searches = mutableListOf<String>()

    private fun page(title: String, user: String) = """
        {"title": "$title", "videoinfo": [{"user": "$user", "descriptionurl": "https://commons.wikimedia.org/wiki/${title.replace(' ', '_')}",
          "derivatives": [{"src": "https://upload.wikimedia.org/a/$title", "transcodekey": "ogg"}, {"src": "https://upload.wikimedia.org/a/$title.mp3", "transcodekey": "mp3"}]}]}
    """

    private val source = CommonsRecordingSource(HttpClient(MockEngine { request ->
        val query = request.url.parameters["gsrsearch"]!!
        searches += query
        val pages = if ("Lingua_Libre" in query) {
            listOf(page("File:LL-Q150 (fra)-Mathsou-bonjour.wav", "Uploader"), page("File:LL-Q150 (fra)-Other-bonjour à tous.wav", "Uploader"))
        } else {
            listOf(page("File:Fr-bonjour.ogg", "Céréales Killer"), page("File:Fr-Paris-bonjour.ogg", "Someone"))
        }
        respond("""{"query": {"pages": [${pages.joinToString(",")}]}}""", headers = headersOf("Content-Type", "application/json"))
    }))

    @Test
    fun recordingsAreLinguaLibresThenWiktionarysAsMp3() = runTest {
        val found = source.recordings("bonjour", "fr")
        assertTrue(searches.first().contains("incategory:\"Lingua_Libre_pronunciation-fra\""), searches.first())
        assertEquals(listOf("Mathsou" to "Lingua Libre", "Céréales Killer" to "Wiktionary"), found.map { it.speaker to it.source }, "files named for other words are dropped")
        assertTrue(found.all { it.url.endsWith(".mp3") })
        assertEquals("https://commons.wikimedia.org/wiki/File:LL-Q150_(fra)-Mathsou-bonjour.wav", found.first().pageUrl)
    }

    @Test
    fun wordsThatWouldBreakTheSearchAreNotSearched() = runTest {
        assertEquals(emptyList(), source.recordings("a.*b", "fr"))
        assertEquals(emptyList(), searches)
    }
}
