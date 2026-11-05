package com.tayra.languages.core.ui.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The app's side of the YouTube page: what it asks the page and how it reads the answers. */
class YouTubeControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterTest
    fun stop() = scope.cancel()

    /** A page that answers as YouTube's would, and remembers what it was asked. */
    private inner class FakePage(private val details: String?, private val tracks: Map<String, String?> = emptyMap()) : WebYouTubeController(scope) {
        val asked = mutableListOf<String>()

        override suspend fun evaluate(script: String): String? {
            asked += script
            val answer = Regex("""__tayra\.answer\((\d+), (.*)\)$""").find(script)
            if (answer != null) {
                val id = answer.groupValues[1]
                val call = answer.groupValues[2]
                val value = when {
                    call.startsWith("__tayra.details") -> details
                    call.startsWith("__tayra.token") -> "the-token"
                    call.startsWith("__tayra.track") -> tracks[if (call.contains("\"en\"")) "en" else "original"]
                    else -> null
                }
                val json = if (value == null) "null" else kotlinx.serialization.json.JsonPrimitive(value).toString()
                onMessage("""{"type":"result","id":$id,"value":$json}""")
            }
            if (script.startsWith("window.__tayra ? __tayra.status()")) return "[12.5,1,243,1]"
            return null
        }
    }

    @Test
    fun detailsAreReadOnceThePageIsReady() = runBlocking {
        val page = FakePage(
            """{"title":"Entenda em 5 pontos","author":"Band Jornalismo","length":243,"playable":true,
               "tracks":[{"lang":"pt","kind":"","name":"Português","url":"/api/timedtext?v=x&lang=pt"},{"lang":"pt","kind":"asr","name":"Português (automático)","url":"/api/timedtext?v=x&lang=pt&kind=asr"}]}""",
        )
        page.onMessage("""{"type":"ready"}""")
        val details = page.details()!!
        assertEquals("Entenda em 5 pontos", details.title)
        assertEquals("Band Jornalismo", details.channel)
        assertEquals(243_000, details.lengthMs)
        assertEquals(listOf(false, true), details.tracks.map { it.automatic })
        page.release()
    }

    @Test
    fun captionsAskForTheTokenThenTheTrackAndItsTranslation() = runBlocking {
        val page = FakePage(null, mapOf("original" to "{\"events\":[]}", "en" to "{\"events\":[1]}"))
        page.onMessage("""{"type":"ready"}""")
        val (original, translated) = page.captions(YouTubeTrack("pt", false, "Português", "/api/timedtext?v=x"), "en")
        assertEquals("{\"events\":[]}", original)
        assertEquals("{\"events\":[1]}", translated)
        val calls = page.asked.filter { "__tayra.answer" in it }
        assertTrue(calls[0].contains("__tayra.token(\"pt\")"), calls[0])
        assertTrue(calls[1].contains("__tayra.track(\"/api/timedtext?v=x\", null)"), calls[1])
        page.release()
    }

    @Test
    fun theClockFollowsThePlayerAndItsErrorsBecomeProblems() = runBlocking {
        val page = FakePage(null)
        page.onMessage("""{"type":"ready"}""")
        val start = System.currentTimeMillis()
        while (page.position.value != 12_500L && System.currentTimeMillis() - start < 3_000) Thread.sleep(20)
        assertEquals(12_500L, page.position.value)
        assertEquals(243_000L, page.durationMs.value)
        assertTrue(page.playing.value)
        page.onMessage("""{"type":"state","state":2}""")
        assertTrue(!page.playing.value)
        page.onMessage("""{"type":"error","code":150}""")
        assertIs<VideoProblem.Failed>(page.problem.value)
        page.release()
    }

    @Test
    fun aTranslationYouTubeRefusesIsNone() = runBlocking {
        val page = FakePage(null, mapOf("original" to "{\"events\":[]}", "en" to null))
        page.onMessage("""{"type":"ready"}""")
        val (original, translated) = page.captions(YouTubeTrack("pt", false, "Português", "/api/timedtext?v=x"), "en")
        assertEquals("{\"events\":[]}", original)
        assertNull(translated)
        page.release()
    }

    @Test
    fun aVideoWithAProblemDoesNotStartPlaying() = runBlocking {
        val page = FakePage(null)
        page.onMessage("""{"type":"error","code":150}""")
        page.play()
        assertTrue(!page.playing.value)
        page.release()
    }

    @Test
    fun aPageThatNeverAnswersGivesNoDetails() = runBlocking {
        val page = FakePage(null)
        page.onMessage("""{"type":"ready"}""")
        assertNull(page.details())
        page.release()
    }

    @Test
    fun theEmbedAsksForTheLanguageAndCarriesNoControls() {
        val url = YouTubeScripts.embedUrl("dQw4w9WgXcQ", "pt")
        assertTrue(url.startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?controls=0"))
        assertTrue("hl=pt" in url && "cc_lang_pref=pt" in url && "origin=https://tayra.app" in url)
    }
}
