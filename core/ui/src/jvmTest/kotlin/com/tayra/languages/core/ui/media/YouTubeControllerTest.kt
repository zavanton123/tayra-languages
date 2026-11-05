package com.tayra.languages.core.ui.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        @Volatile var status = "[12.5,1,243,1]"

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
            if (script.startsWith("window.__tayra ? __tayra.status()")) return status
            return null
        }
    }

    /** A page that refuses to embed, on a platform with YouTube's own page to go to when [movesOn]. */
    private inner class RefusingPage(private val movesOn: Boolean) : WebYouTubeController(scope) {
        var moves = 0

        override fun openWatchPage(): Boolean {
            if (!movesOn || moves > 0) return false
            moves++
            // The new page comes up a little later, while the old one may still answer.
            scope.launch {
                kotlinx.coroutines.delay(50)
                onMessage("""{"type":"ready"}""")
            }
            return true
        }

        override suspend fun evaluate(script: String): String? {
            val answer = Regex("""__tayra\.answer\((\d+), (.*)\)$""").find(script) ?: return null
            val value = if (moves == 0) """{"refused":true}""" else """{"title":"Own page","author":"A","length":10,"playable":true,"tracks":[]}"""
            onMessage("""{"type":"result","id":${answer.groupValues[1]},"value":${kotlinx.serialization.json.JsonPrimitive(value)}}""")
            return null
        }
    }

    @Test
    fun aVideoThatMayNotBeEmbeddedIsAProblemWhereThereIsNoOtherPage() = runBlocking {
        val page = RefusingPage(movesOn = false)
        page.onMessage("""{"type":"ready"}""")
        assertEquals(null, page.details())
        assertEquals(VideoProblem.Failed("The owner of this video doesn't let it play outside YouTube."), page.problem.value)
        page.release()
    }

    @Test
    fun aVideoThatMayNotBeEmbeddedMovesOnOnceAndItsDetailsComeFromThere() = runBlocking {
        val page = RefusingPage(movesOn = true)
        page.onMessage("""{"type":"ready"}""")
        val details = page.details()
        page.onMessage("""{"type":"refused"}""")
        assertEquals("Own page", details?.title)
        assertEquals(1, page.moves)
        assertEquals(null, page.problem.value)
        page.release()
    }

    @Test
    fun theRefusalCanArriveAsAMessageOrAnErrorCodeToo() = runBlocking {
        val told = RefusingPage(movesOn = true)
        told.onMessage("""{"type":"refused"}""")
        told.onMessage("""{"type":"refused"}""")
        assertEquals(1, told.moves)
        assertEquals("Own page", told.details()?.title)
        told.release()
        val coded = RefusingPage(movesOn = true)
        coded.onMessage("""{"type":"error","code":150}""")
        assertEquals(1, coded.moves)
        assertEquals(null, coded.problem.value)
        coded.release()
        val gone = RefusingPage(movesOn = true)
        gone.onMessage("""{"type":"error","code":100}""")
        assertEquals(0, gone.moves)
        assertEquals(VideoProblem.Failed("This video is no longer on YouTube."), gone.problem.value)
        gone.release()
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

    @Test
    fun `only YouTube's own pages stay in the player`() {
        assertTrue(YouTubeScripts.staysInPlayer("www.youtube.com", "/watch"))
        assertTrue(YouTubeScripts.staysInPlayer("consent.youtube.com", "/m"))
        assertTrue(YouTubeScripts.staysInPlayer("www.youtube-nocookie.com", "/embed/x"))
        assertFalse(YouTubeScripts.staysInPlayer("www.youtube.com", "/pagead/aclk"))
        assertFalse(YouTubeScripts.staysInPlayer("www.youtube.com", "/redirect"))
        assertFalse(YouTubeScripts.staysInPlayer("charisma-r.riseguide.com", "/"))
        assertFalse(YouTubeScripts.staysInPlayer("notyoutube.com", "/watch"))
        assertFalse(YouTubeScripts.staysInPlayer(null, null))
    }

    @Test
    fun anAdvertShowsAsPlayingButLeavesTheVideosPlaceAlone() = runBlocking {
        val page = FakePage("{}")
        page.status = "[3.0,-1,30,1,2]"
        page.onMessage("""{"type":"ready"}""")
        withTimeout(5_000) { while (!page.advertShowing.value) delay(10) }
        assertTrue(page.playing.value)
        assertEquals(0L, page.position.value)
        assertEquals(0L, page.durationMs.value)
        page.tick()
        assertEquals(0L, page.position.value)

        // Paused by the app, it is shown as paused; its state events change nothing.
        page.status = "[3.0,-1,30,1,1]"
        withTimeout(5_000) { while (page.playing.value) delay(10) }
        page.onMessage("""{"type":"state","state":1}""")
        assertFalse(page.playing.value)

        page.status = "[12.5,1,243,1,0]"
        withTimeout(5_000) { while (page.advertShowing.value) delay(10) }
        withTimeout(5_000) { while (page.position.value < 12_500L) delay(10) }
        assertEquals(243_000L, page.durationMs.value)
        assertTrue(page.playing.value)
    }

    @Test
    fun pausingAlsoPausesAnAdvert() = runBlocking {
        val page = FakePage("{}")
        page.pause()
        withTimeout(5_000) { while (page.asked.none { it.contains("ad-showing") && it.contains("v.pause()") }) delay(10) }
    }
}
