package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.bridge.DesktopBrowserBridge
import com.tayra.languages.core.data.bridge.SecurePairedBrowserStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.InMemorySecureStore
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.bridge.BridgeException
import com.tayra.languages.core.domain.bridge.BridgePairing
import com.tayra.languages.core.domain.bridge.BrowserBridgeApi
import com.tayra.languages.core.domain.bridge.SaveTermRequest
import com.tayra.languages.core.domain.bridge.StatusChanges
import com.tayra.languages.core.domain.bridge.TranscriptRequest
import com.tayra.languages.core.domain.bridge.TranslateRequest
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The app's side of the browser extension: pairing, what it answers, and who it answers. */
class BrowserBridgeTest {

    private class Env {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-bridge", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages)
        val reading = ReadingService(BookRepositoryImpl(provider), languages, terms, WordsReadRepositoryImpl(provider), termService)
        val looked = mutableListOf<Pair<DictionaryId, String>>()
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = true
            override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup {
                looked += dictionary to text
                return if (text.lowercase() == "gato") DictionaryLookup(listOf(DictionaryEntry("gato", "noun", null, listOf(DictionarySense(listOf("cat", "tomcat"))))), emptyList())
                else DictionaryLookup.EMPTY
            }
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language) = "<$text>"
        }
        val api = BrowserBridgeApi(settings, languages, reading, termService, dictionary, translator, "9.9.9")
        val portuguese = runBlocking { languages.save(Language(name = "Portuguese")) }

        init {
            runBlocking { settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "en") } }
        }
    }

    private fun json3(vararg lines: Pair<Long, String>) =
        """{"events":[${lines.joinToString(",") { (at, text) -> """{"tStartMs":$at,"dDurationMs":1500,"segs":[{"utf8":"$text"}]}""" }}]}"""

    @Test
    fun linesComeCutIntoWordsWithTheirStatuses() = runBlocking<Unit> {
        val env = Env()
        env.api.save(SaveTermRequest("gato", TermStatus.NEW_2.value, sentence = "O gato dorme."))
        val answer = env.api.transcript(
            TranscriptRequest("abc", "pt-BR", automatic = false, original = json3(1000L to "O gato dorme.", 3000L to "Bom dia."), translation = json3(1000L to "The cat sleeps.", 3000L to "Good morning.")),
        )
        assertTrue(answer.languageMatches)
        assertEquals(listOf("O gato dorme.", "Bom dia."), answer.lines.map { it.text })
        assertEquals(listOf("The cat sleeps.", "Good morning."), answer.lines.map { it.translation })
        val first = answer.lines.first().segments
        assertEquals("O gato dorme.", first.joinToString("") { it.text })
        val gato = first.single { it.text == "gato" }
        assertEquals(TermStatus.NEW_2.value, gato.status)
        assertEquals("gato", gato.lc)
        assertEquals(TermStatus.UNKNOWN.value, first.single { it.text == "dorme" }.status)
        assertNull(first.single { it.text == "." }.lc, "punctuation is no word")
    }

    @Test
    fun aTrackInAnotherLanguageComesWithoutWords() = runBlocking<Unit> {
        val answer = Env().api.transcript(TranscriptRequest("abc", "en", automatic = false, original = json3(0L to "Hello there.")))
        assertFalse(answer.languageMatches)
        assertEquals("Hello there.", answer.lines.single().text)
        assertTrue(answer.lines.single().segments.isEmpty())
    }

    @Test
    fun emptySubtitlesAreRefused() = runBlocking<Unit> {
        assertFailsWith<BridgeException> { Env().api.transcript(TranscriptRequest("abc", "pt", automatic = true, original = "")) }
    }

    @Test
    fun aLookupGivesTheDictionaryAndTheWordAsSaved() = runBlocking<Unit> {
        val env = Env()
        val fresh = env.api.lookup("Gato")
        assertEquals(TermStatus.UNKNOWN.value, fresh.status)
        assertEquals(listOf(listOf("cat", "tomcat")), fresh.entries.single().senses)
        assertEquals(DictionaryId("pt", "en"), env.looked.last().first.copy(edition = null))

        val saved = env.api.save(SaveTermRequest("gato", TermStatus.NEW_1.value, translation = "cat", sentence = "O gato dorme."))
        assertEquals("gato", saved.lc)
        val again = env.api.lookup("gato")
        assertEquals(TermStatus.NEW_1.value, again.status)
        assertEquals("cat", again.translation)
        assertEquals("O gato dorme.", env.terms.findByTextLc(env.portuguese, "gato")?.sentence)
    }

    @Test
    fun linesAreTranslatedByTheAppsTranslator() = runBlocking<Unit> {
        val answer = Env().api.translate(TranslateRequest(listOf("Bom dia.", " ")))
        assertEquals(listOf("<Bom dia.>", null), answer.translations)
    }

    @Test
    fun aStatusChangedAnywhereIsReported() = runBlocking<Unit> {
        val env = Env()
        val changes = StatusChanges(env.terms, env.settings)
        val scope = CoroutineScope(Dispatchers.Default)
        try {
            changes.start(scope)
            val start = withTimeout(5_000) { var c = changes.since(0); while (c.seq == 0L) { delay(20); c = changes.since(0) }; c.seq }
            env.api.save(SaveTermRequest("gato", TermStatus.LEARNING_3.value))
            val found = withTimeout(5_000) { var c = changes.since(start); while (c.statuses.isEmpty()) { delay(20); c = changes.since(start) }; c }
            assertEquals(mapOf("gato" to TermStatus.LEARNING_3.value), found.statuses)
            assertTrue(changes.since(found.seq).statuses.isEmpty())
            assertTrue(changes.since(found.seq + 100).reset, "a number never given starts over")
        } finally {
            scope.cancel()
        }
    }

    private class TestClock(var now: Instant = Instant.fromEpochMilliseconds(1_000_000)) : Clock {
        override fun now() = now
    }

    @Test
    fun pairingTakesTheCodeOnceAndBeforeItExpires() {
        val clock = TestClock()
        val secrets = InMemorySecureStore()
        val pairing = BridgePairing(SecurePairedBrowserStore(secrets), clock, Random(7))
        assertNull(pairing.pair("123456", "Chrome"), "no code shown")
        val code = pairing.start()
        assertEquals(BridgePairing.CODE_LENGTH, code.length)
        val token = assertNotNull(pairing.pair(code, "Chrome"))
        assertTrue(pairing.accepts(token))
        assertFalse(pairing.accepts(token.dropLast(1) + "x"))
        assertNull(pairing.pair(code, "Firefox"), "a code works once")
        assertEquals(listOf("Chrome"), BridgePairing(SecurePairedBrowserStore(secrets), clock).browsers.map { it.browser }, "kept between runs")

        val late = pairing.start()
        clock.now += 6.minutes
        assertNull(pairing.pair(late, "Firefox"), "expired")

        val guessed = pairing.start()
        repeat(BridgePairing.MAX_WRONG_TRIES) { assertNull(pairing.pair("000000".takeIf { it != guessed } ?: "111111", "Firefox")) }
        assertNull(pairing.pair(guessed, "Firefox"), "too many wrong codes end the pairing")

        pairing.unpair(pairing.browsers.single().id)
        assertFalse(pairing.accepts(token))
    }

    @Test
    fun theServerAnswersOnlyThePairedExtension() {
        val env = Env()
        val free = ServerSocket(0).use { it.localPort }
        val bridge = DesktopBrowserBridge(env.api, StatusChanges(env.terms, env.settings), BridgePairing(SecurePairedBrowserStore(InMemorySecureStore())), free..free)
        bridge.start()
        try {
            assertEquals(free, bridge.state.value.port)
            val client = HttpClient.newHttpClient()
            fun call(method: String, path: String, body: String? = null, origin: String? = "chrome-extension://abc", token: String? = null): HttpResponse<String> {
                val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$free$path"))
                    .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
                origin?.let { request.header("Origin", it) }
                token?.let { request.header("Authorization", "Bearer $it") }
                return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
            }
            assertEquals(200, call("GET", "/v1/hello").statusCode())
            assertEquals(403, call("GET", "/v1/hello", origin = "https://evil.example").statusCode(), "web pages are turned away")
            assertEquals(401, call("GET", "/v1/status").statusCode(), "unpaired")
            assertEquals(403, call("POST", "/v1/pair", """{"code":"000000"}""").statusCode())

            bridge.startPairing()
            val code = assertNotNull(bridge.state.value.code)
            val paired = call("POST", "/v1/pair", """{"code":"$code","browser":"Chrome"}""")
            assertEquals(200, paired.statusCode())
            val token = paired.body().substringAfter("\"token\":\"").substringBefore('"')
            assertNull(bridge.state.value.code)
            assertEquals(listOf("Chrome"), bridge.state.value.browsers.map { it.browser })

            val status = call("GET", "/v1/status", token = token)
            assertEquals(200, status.statusCode())
            assertTrue("\"Portuguese\"" in status.body() && "\"9.9.9\"" in status.body(), status.body())
            val lookup = call("GET", "/v1/lookup?term=gato", token = token)
            assertTrue("tomcat" in lookup.body(), lookup.body())
            assertEquals(400, call("POST", "/v1/terms", """{"term":"gato","status":7}""", token = token).statusCode(), "no such status")
            assertEquals(400, call("POST", "/v1/transcript", "not json", token = token).statusCode())
            assertEquals(404, call("GET", "/v1/nothing", token = token).statusCode())
        } finally {
            bridge.stop()
        }
    }
}
