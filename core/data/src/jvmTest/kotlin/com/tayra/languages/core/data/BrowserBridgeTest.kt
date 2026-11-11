package com.tayra.languages.core.data

import kotlin.io.path.createTempDirectory
import com.tayra.languages.core.data.bridge.NativeHostInstaller
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.bridge.DesktopBrowserBridge
import com.tayra.languages.core.data.bridge.SecurePairedBrowserStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.VideoRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.InMemorySecureStore
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.bridge.AddVideoRequest
import com.tayra.languages.core.domain.bridge.AddVideoResponse
import com.tayra.languages.core.domain.bridge.BookmarkRequest
import com.tayra.languages.core.domain.bridge.BridgeException
import com.tayra.languages.core.domain.bridge.BridgeVideos
import com.tayra.languages.core.domain.bridge.OpenVideoRequest
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.VideoBookmarks
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoOpenRequests
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.model.PageBookmark
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.flow.MutableStateFlow
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
        val frequency = object : FrequencyLists {
            override suspend fun list(languageCode: String) = FrequencyList.parse("o\to\ndormir\tdormir dorme\ngato\tgato").takeIf { languageCode == "pt" }
        }
        val piper = object : LocalSpeechEngine {
            override val engine = SpeechEngine.PIPER
            override val displayName = "Fake Piper"
            override val description = ""
            override val packagesDescription = ""
            override val hasRuntimeSetup = false
            override val progress = MutableStateFlow<String?>(null)
            override suspend fun status() = "ready"
            override suspend fun isReady() = true
            override suspend fun setUp() = "ready"
            override suspend fun packages() = emptyList<SpeechPackage>()
            override suspend fun installPackage(id: String) {}
            override suspend fun removePackage(id: String) {}
            override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
            override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float) = "$languageCode:$text".encodeToByteArray()
        }
        val speech = SentenceAudio(LocalSpeech(listOf(piper)), settings, MemorySpeechAudioCache())
        val api = BrowserBridgeApi(settings, languages, reading, termService, dictionary, translator, "9.9.9", frequency, speech)
        val videoRepository = VideoRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val opens = VideoOpenRequests()
        val videos = BridgeVideos(settings, languages, videoRepository, VideoService(videoRepository, languages), books, opens)
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
    fun wordsComeWithTheirFrequencyRankThroughAnyOfTheirForms() = runBlocking<Unit> {
        val answer = Env().api.transcript(TranscriptRequest("abc", "pt", automatic = false, original = json3(1000L to "O gato dorme.", 3000L to "Bom dia.")))
        assertEquals(mapOf("o" to 1, "dorme" to 2, "gato" to 3), answer.ranks)
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
    fun wordsAreSaidByTayrasOwnVoiceAndLeftToTheBrowserForTheSystemOne() = runBlocking<Unit> {
        val env = Env()
        env.settings.update { it.copy(speechEngine = SpeechEngine.SYSTEM) }
        assertNull(env.api.speech("manteiga"))
        env.settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
        assertEquals("pt:manteiga", env.api.speech(" manteiga ")?.decodeToString())
        assertFailsWith<BridgeException> { env.api.speech("a".repeat(BrowserBridgeApi.MAX_SPOKEN_CHARS + 1)) }
        assertFailsWith<BridgeException> { env.api.speech(" ") }
    }

    @Test
    fun aWatchedVideoIsKeptOnceWithItsBookmarksAndOpensWhereItWasLeft() = runBlocking<Unit> {
        val env = Env()
        val request = AddVideoRequest(
            "abcdefghijk", "Um vídeo", "Canal", 60_000, "pt-BR", automatic = true,
            original = json3(1000L to "O gato dorme.", 3000L to "Bom dia."), translation = json3(1000L to "The cat sleeps."),
        )
        val first = env.videos.add(request)
        assertTrue(first.added)
        assertEquals(AddVideoResponse(first.bookId, added = false), env.videos.add(request), "a video watched again is not added twice")
        val video = assertNotNull(env.videoRepository.video(first.bookId))
        assertEquals(listOf("Um vídeo", "Canal", "abcdefghijk"), listOf(video.title, video.channel, video.media))
        assertEquals(VideoKind.YOUTUBE to SubtitleKind.YOUTUBE_AUTO, video.kind to video.subtitleKind)
        assertEquals("The cat sleeps.", env.videoRepository.cues(first.bookId).first().cue.translation)
        assertFailsWith<BridgeException>("not the language learned") { env.videos.add(request.copy(trackLanguage = "en")) }
        assertFailsWith<BridgeException> { env.videos.add(request.copy(videoId = "not an id")) }

        val mark = env.videos.bookmark(BookmarkRequest(first.bookId, 3500))
        assertEquals("0:03 Bom dia." to true, mark.title to mark.added)
        assertFalse(env.videos.bookmark(BookmarkRequest(first.bookId, 3900)).added, "the same line once")
        assertEquals(listOf(mark.title), env.books.observeBookmarks(first.bookId).first().map { it.title })

        val opened = CompletableDeferred<Long>()
        launch(start = CoroutineStart.UNDISPATCHED) { opened.complete(env.opens.videos.first()) }
        env.videos.open(OpenVideoRequest(first.bookId, 42_000))
        assertEquals(first.bookId, opened.await())
        assertEquals(42_000, env.videoRepository.video(first.bookId)?.positionMs)
        assertFailsWith<BridgeException> { env.videos.open(OpenVideoRequest(first.bookId + 99, 0)) }
    }

    @Test
    fun aBookmarkFindsItsLineByItsTitleOnItsPage() {
        val lines = listOf(Triple(0, 1, Cue(1000, 2000, "Olá.")), Triple(1, 1, Cue(65_000, 66_000, "Bom dia.")), Triple(2, 2, Cue(3_700_000, 3_701_000, "Tchau.")))
        fun mark(page: Int, title: String) = PageBookmark(1, 1, page, title)
        assertEquals("1:05 Bom dia.", VideoBookmarks.title(lines[1].third))
        assertEquals("1:01:40 Tchau.", VideoBookmarks.title(lines[2].third))
        assertEquals(1, VideoBookmarks.lineOf(mark(1, "1:05 Bom dia."), lines))
        assertEquals(1, VideoBookmarks.lineOf(mark(1, "Bom dia."), lines), "by its text when the time moved")
        assertEquals(2, VideoBookmarks.lineOf(mark(2, "Page 2"), lines), "a page's own bookmark goes to its first line")
        assertNull(VideoBookmarks.lineOf(mark(3, "Page 3"), lines))
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
        val bridge = DesktopBrowserBridge(env.api, StatusChanges(env.terms, env.settings), BridgePairing(SecurePairedBrowserStore(InMemorySecureStore())), free..free, videos = env.videos)
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

            runBlocking { env.settings.update { it.copy(speechEngine = SpeechEngine.SYSTEM) } }
            assertEquals(204, call("GET", "/v1/speech?text=gato", token = token).statusCode(), "the browser's voice says it")
            runBlocking { env.settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
            val spoken = call("GET", "/v1/speech?text=gato", token = token)
            assertEquals(200, spoken.statusCode())
            assertEquals("audio/wav", spoken.headers().firstValue("Content-Type").orElse(null))
            assertEquals("pt:gato", spoken.body())

            val added = call("POST", "/v1/videos", "{\"videoId\":\"abcdefghijk\",\"title\":\"Gatos\",\"trackLanguage\":\"pt\",\"automatic\":false,\"original\":${kotlinx.serialization.json.JsonPrimitive(json3(1000L to "O gato dorme."))}}", token = token)
            assertEquals(200, added.statusCode(), added.body())
            assertTrue("\"added\":true" in added.body(), added.body())
            assertEquals(400, call("POST", "/v1/open", "{\"bookId\":9999,\"positionMs\":0}", token = token).statusCode(), "no such video")
        } finally {
            bridge.stop()
        }
    }
}

/** The helper the browsers start for the extension, so it connects with no pairing code. */
class NativeHostInstallerTest {

    @Test
    fun theHelperIsRegisteredWithTheBrowsersThatAreThere() {
        val home = createTempDirectory("tayra-home").toFile()
        File(home, "Library/Application Support/Google/Chrome").mkdirs()
        File(home, "Library/Application Support/Mozilla").mkdirs()
        val folder = File(home, "data/browser-extension")
        val installer = NativeHostInstaller(folder, home, "mac os x")
        assertEquals(listOf("Chrome", "Firefox"), installer.install())
        val chrome = File(home, "Library/Application Support/Google/Chrome/NativeMessagingHosts/app.tayra.bridge.json").readText()
        assertTrue("chrome-extension://emhgfoemkgabcjfadhmekfkjgjjhhfcd/" in chrome && File(folder, "tayra-bridge-host").absolutePath in chrome, chrome)
        val firefox = File(home, "Library/Application Support/Mozilla/NativeMessagingHosts/app.tayra.bridge.json").readText()
        assertTrue("youtube@tayra.app" in firefox && "allowed_extensions" in firefox, firefox)
        assertFalse(File(home, "Library/Application Support/Microsoft Edge").exists(), "no folder made for a browser that isn't there")
        assertEquals(installer.token(), NativeHostInstaller(folder, home, "mac os x").token(), "the token is kept")
    }

    /** The helper answers as native messaging wants: the length in four little-endian bytes, then the JSON. */
    @Test
    fun theHelperGivesTheToken() {
        if (System.getProperty("os.name").lowercase().contains("win")) return
        val folder = createTempDirectory("tayra-host").toFile()
        val installer = NativeHostInstaller(folder, createTempDirectory("tayra-home").toFile(), "linux")
        installer.install()
        val process = ProcessBuilder(File(folder, "tayra-bridge-host").absolutePath).start()
        process.outputStream.use { it.write(byteArrayOf(2, 0, 0, 0, '{'.code.toByte(), '}'.code.toByte())) }
        val bytes = process.inputStream.readAllBytes()
        assertEquals(0, process.waitFor())
        val length = (bytes[0].toInt() and 255) or ((bytes[1].toInt() and 255) shl 8) or ((bytes[2].toInt() and 255) shl 16) or ((bytes[3].toInt() and 255) shl 24)
        val message = bytes.copyOfRange(4, bytes.size).decodeToString()
        assertEquals(message.length, length)
        assertEquals("""{"token":"${installer.token()}"}""", message)
    }

    @Test
    fun onWindowsTheBrowsersFindTheHelperInTheRegistry() {
        val folder = createTempDirectory("tayra-win").toFile()
        val commands = mutableListOf<List<String>>()
        assertEquals(listOf("Chrome", "Edge", "Firefox"), NativeHostInstaller(folder, folder, "windows 11") { commands += it }.install())
        assertTrue(commands.first().containsAll(listOf("reg", "add", "HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\app.tayra.bridge")))
        assertTrue(File(folder, "tayra-bridge-host.bat").isFile && File(folder, "app.tayra.bridge.firefox.json").isFile)
    }

    @Test
    fun theHelpersTokenIsAccepted() {
        val folder = createTempDirectory("tayra-accept").toFile()
        val installer = NativeHostInstaller(folder, folder, "linux")
        val pairing = BridgePairing(SecurePairedBrowserStore(InMemorySecureStore()), automaticToken = installer::token)
        assertTrue(pairing.accepts(installer.token()))
        assertFalse(pairing.accepts("0".repeat(64)))
    }
}
