package com.tayra.languages.feature.videos

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.VideoRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoBlur
import io.ktor.client.engine.mock.respond
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.media.SilentVideo
import com.tayra.languages.core.ui.media.YouTubeTrack
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.form.TermFormKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled

@OptIn(ExperimentalTestApi::class)
class VideosScreenTest {
    private val file = File.createTempFile("tayra-videos-ui", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val videos = VideoRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val service = VideoService(videos, languages)
    private val translated = mutableListOf<String>()
    private val translator = object : SentenceTranslator {
        override suspend fun translate(text: String, language: Language): String {
            translated += text
            return "<$text>"
        }
    }

    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }

    private val bookId = runBlocking {
        service.import(
            VideoImport(
                languageId = portuguese,
                title = "O gato",
                kind = VideoKind.LINK,
                media = "https://videos.example/gato.mp4",
                subtitles = SubtitleTrack(listOf(Cue(500, 2000, "Olá, tudo bem?"), Cue(2000, 4000, "O gato dorme na casa."), Cue(4000, 6000, "E o cão?"))),
                translation = listOf(Cue(500, 2000, "Hello, how are you?")),
                translationSource = TranslationSource.FILE,
            ),
        )
    }

    private fun watch() = WatchViewModel(bookId, videos, books, languages, reading, BookStatsService(books, languages, settings, reading), translator, settings)

    @Test
    fun theWatchScreenShowsTheTranscriptAndFollowsTheLines() = runDesktopComposeUiTest(width = 1500, height = 900) {
        val vm = watch()
        setContent { TayraTheme { ProvideWindowWidth { WatchScreen(bookId, onNavigate = {}, onBack = {}, viewModel = vm, player = { source, onController, modifier -> SilentVideo(source, onController, modifier) }) } } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("line-2").fetchSemanticsNodes().isNotEmpty() && vm.playback.value != null }
        onNodeWithText("Hello, how are you?").assertExists()
        // Lines without a translation of their own are translated as they come near.
        waitUntil(timeoutMillis = 5_000) { "O gato dorme na casa." in translated }
        onNodeWithText("<O gato dorme na casa.>").assertExists()

        onNodeWithTag("line-1").performClick()
        waitUntil(timeoutMillis = 3_000) { vm.playback.value?.current?.value == 1 }
        assertTrue(vm.playback.value!!.playing.value, "a clicked line plays")
        onNodeWithTag("play-pause").performClick()
        waitForIdle()
        assertTrue(!vm.playback.value!!.playing.value)

        onNodeWithTag("next-line").performClick()
        waitUntil(timeoutMillis = 3_000) { vm.playback.value?.current?.value == 2 }
        onNodeWithTag("previous-line").performClick()
        waitUntil(timeoutMillis = 3_000) { vm.playback.value?.current?.value == 1 }

        onNodeWithTag("auto-pause").performClick()
        onNodeWithText("Pause after each line").assertExists()
        System.getenv("WATCH_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithTag("tab-words").performClick()
        onNodeWithTag("video-words").assertExists()
        onNodeWithText("gato").assertExists()
        System.getenv("WATCH_WORDS_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
    }

    @Test
    fun aWordOfTheTranscriptOpensInTheTermPaneWithItsLine() = runBlocking {
        val vm = watch()
        val start = System.currentTimeMillis()
        while (vm.state.value.loading && System.currentTimeMillis() - start < 5_000) Thread.sleep(20)
        val line = vm.state.value.lines[1]
        val gato = line.items.indexOfFirst { it.renderText == "gato" }
        assertTrue(gato >= 0)
        assertNotNull(line.items[gato].termId, "every word has a term once rendered")
        vm.pickWord(1, gato)
        val key = assertIs<TermFormKey.ById>(vm.state.value.term)
        assertEquals("O gato dorme na casa.", key.sentence)
        assertEquals(PickedWord(1, gato), vm.state.value.picked)
        assertTrue(line.hasUnknownWords)
        vm.closeTerm()
        assertEquals(null, vm.state.value.term)
    }

    @Test
    fun theListShowsTheVideosOfTheLanguageAndAddsNewOnes() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val visited = mutableListOf<Route>()
        val list = VideosViewModel(videos, books, settings, BookStatsService(books, languages, settings, reading))
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        VideosContent(list.state.value.copy(loading = false, videos = runBlocking { videos.observeVideos().first() }), onNavigate = { visited += it }, onSearch = {}, onDelete = {})
                    }
                }
            }
        }
        onNodeWithText("O gato").assertExists()
        onNodeWithTag("video-$bookId").performClick()
        onNodeWithTag("add-video").performClick()
        assertEquals(listOf(Route.Watch(bookId), Route.NewVideo), visited)
    }

    @Test
    fun aVideoLinkIsAddedWithSubtitlesFetchedFromTheirAddress() = runBlocking {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nBom dia.\n\n2\n00:00:02,500 --> 00:00:04,000\nComo vai?\n"
        val web = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { request ->
            when (request.url.encodedPath) {
                "/aula.pt.srt" -> respond(srt)
                else -> respond("", io.ktor.http.HttpStatusCode.NotFound)
            }
        })
        val add = AddVideoViewModel(service, videos, settings, languages, com.tayra.languages.core.data.media.SubtitleDownloader(web))
        add.setOrigin(VideoOrigin.LINK)
        add.setLinkInput("ftp://nope")
        assertEquals(null, add.state.value.link.url)
        add.setLinkInput("https://videos.example/Aula_1.mp4")
        assertEquals("Aula 1", add.state.value.title, "the file's name is the first title")
        add.setSubtitlesUrl("https://videos.example/aula.pt.srt")
        add.fetchSubtitles(asTranslation = false)
        val start = System.currentTimeMillis()
        while (add.state.value.link.subtitles == null && System.currentTimeMillis() - start < 5_000) Thread.sleep(20)
        assertEquals(2, add.state.value.link.subtitles?.cues?.size)
        add.pickTranslation("aula.en.srt", "1\n00:00:01,000 --> 00:00:02,000\nGood morning.\n".encodeToByteArray())
        add.add()
        val id = assertIs<AddVideoEvent.Added>(add.events.flow.first()).bookId
        assertEquals(VideoKind.LINK, videos.video(id)!!.kind)
        assertEquals("https://videos.example/Aula_1.mp4", videos.video(id)!!.media)
        assertEquals("Good morning.", videos.cues(id).first().cue.translation)
        add.setTranslationUrl("https://videos.example/missing.srt")
        add.fetchSubtitles(asTranslation = true)
        while (add.state.value.error == null && System.currentTimeMillis() - start < 10_000) Thread.sleep(20)
        assertEquals("The subtitles could not be fetched.", add.state.value.error)
    }

    /** Whisper as a test sees it: one model to download, and the lines it "hears" in any video. */
    private class FakeTranscriber : com.tayra.languages.core.domain.media.VideoTranscriber {
        var installed = false
        val stages = mutableListOf<com.tayra.languages.core.domain.media.TranscriptionStage>()
        override val installProgress = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        override suspend fun models() = listOf(com.tayra.languages.core.domain.media.TranscriptionModel("small", "Whisper small", 486_000_000, installed, "Quicker"))
        override suspend fun install(model: com.tayra.languages.core.domain.media.TranscriptionModel) { installed = true }
        override suspend fun remove(model: com.tayra.languages.core.domain.media.TranscriptionModel) { installed = false }
        override suspend fun transcribe(url: String, languageCode: String, onProgress: (com.tayra.languages.core.domain.media.TranscriptionStage, Float?) -> Unit): List<com.tayra.languages.core.domain.media.Cue> {
            if (url.endsWith("silent.mp4")) throw com.tayra.languages.core.domain.media.TranscriptionException("The video has no sound.")
            onProgress(com.tayra.languages.core.domain.media.TranscriptionStage.AUDIO, 0.5f).also { stages += com.tayra.languages.core.domain.media.TranscriptionStage.AUDIO }
            onProgress(com.tayra.languages.core.domain.media.TranscriptionStage.SPEECH, 1f).also { stages += com.tayra.languages.core.domain.media.TranscriptionStage.SPEECH }
            return listOf(com.tayra.languages.core.domain.media.Cue(500, 2_000, "O coelho acorda."), com.tayra.languages.core.domain.media.Cue(2_300, 4_000, "Ele vê uma borboleta."))
        }
    }

    private fun waitFor(timeoutMs: Long = 5_000, done: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!done() && System.currentTimeMillis() - start < timeoutMs) Thread.sleep(20)
    }

    @Test
    fun whisperWritesDownALinksSubtitlesOnceAModelIsDownloaded() = runBlocking {
        val whisper = FakeTranscriber()
        val web = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("", io.ktor.http.HttpStatusCode.NotFound) })
        val add = AddVideoViewModel(service, videos, settings, languages, com.tayra.languages.core.data.media.SubtitleDownloader(web), whisper)
        waitFor { add.state.value.transcription.models.isNotEmpty() }
        assertTrue(add.state.value.transcription.offered)
        assertTrue(!add.state.value.transcription.ready)
        add.installModel(add.state.value.transcription.models.single())
        waitFor { add.state.value.transcription.ready }
        add.setOrigin(VideoOrigin.LINK)
        add.setLinkInput("https://videos.example/silent.mp4")
        add.transcribe()
        waitFor { add.state.value.transcription.error != null }
        assertEquals("The video has no sound.", add.state.value.transcription.error)
        add.setLinkInput("https://videos.example/Coelho.mp4")
        add.transcribe()
        waitFor { add.state.value.link.subtitles != null && !add.state.value.transcription.running }
        assertEquals(listOf(com.tayra.languages.core.domain.media.TranscriptionStage.AUDIO, com.tayra.languages.core.domain.media.TranscriptionStage.SPEECH), whisper.stages)
        assertEquals("Whisper", add.state.value.link.subtitles?.name)
        assertEquals(null, add.state.value.transcription.error)
        add.add()
        val id = assertIs<AddVideoEvent.Added>(add.events.flow.first()).bookId
        assertEquals(com.tayra.languages.core.domain.media.SubtitleKind.TRANSCRIBED, videos.video(id)!!.subtitleKind)
        assertEquals("O coelho acorda.", videos.cues(id).first().cue.text)
    }

    @Test
    fun aVideoYouTubeRefusesToEmbedSaysWhyAndOffersTheBrowser() = runDesktopComposeUiTest(width = 1000, height = 1400) {
        val opened = mutableListOf<String>()
        val links = object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) { opened += uri }
        }
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalUriHandler provides links) {
                        AddVideoContent(
                            AddVideoUiState(youtube = YouTubePick(input = "z4Ur8yTwQ6I", id = "z4Ur8yTwQ6I", refused = "The owner of this video doesn't let it play outside YouTube.")),
                            AddVideoActions(),
                        )
                    }
                }
            }
        }
        onNodeWithText("The owner of this video doesn't let it play outside YouTube.").assertExists()
        onNodeWithTag("youtube-open-elsewhere").performClick()
        waitForIdle()
        assertEquals(listOf("https://www.youtube.com/watch?v=z4Ur8yTwQ6I"), opened)
    }

    @Test
    fun theWhisperSectionOffersADownloadThenTheTranscriptionAndItsProgress() = runDesktopComposeUiTest(width = 1000, height = 1400) {
        val model = com.tayra.languages.core.domain.media.TranscriptionModel("small", "Whisper small", 486_000_000, installed = false, note = "Quicker")
        var state by androidx.compose.runtime.mutableStateOf(
            AddVideoUiState(origin = VideoOrigin.LINK, link = LinkPick(input = "https://v.example/a.mp4", url = "https://v.example/a.mp4"), transcription = TranscriptionPick(offered = true, models = listOf(model))),
        )
        val asked = mutableListOf<String>()
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    AddVideoContent(state, AddVideoActions(onInstallModel = { asked += "install ${it.id}" }, onTranscribe = { asked += "transcribe" }, onCancelTranscription = { asked += "cancel" }))
                }
            }
        }
        onNodeWithText("486.0 MB · Quicker").assertExists()
        onNodeWithTag("install-small").performClick()
        state = state.copy(transcription = state.transcription.copy(models = listOf(model.copy(installed = true))))
        waitForIdle()
        onNodeWithTag("transcription-error").assertDoesNotExist()
        state = state.copy(transcription = state.transcription.copy(error = "The video has no sound."))
        waitForIdle()
        onNodeWithTag("transcription-error").assertExists()
        state = state.copy(transcription = state.transcription.copy(error = null))
        waitForIdle()
        onNodeWithTag("transcribe").performClick()
        state = state.copy(transcription = state.transcription.copy(running = true, stage = com.tayra.languages.core.domain.media.TranscriptionStage.SPEECH, fraction = 0.42f))
        waitForIdle()
        onNodeWithText("Writing down what is said… 42%").assertExists()
        onNodeWithTag("add-video-confirm").assertIsNotEnabled()
        onNodeWithTag("cancel-transcription").performClick()
        assertEquals(listOf("install small", "transcribe", "cancel"), asked)
    }

    @Test
    fun theSubtitlesChosenFirstArePeoplesInTheLanguageLearned() {
        val auto = YouTubeTrack("pt", true, "Português (automático)", "a")
        val made = YouTubeTrack("pt-BR", false, "Português (Brasil)", "b")
        val english = YouTubeTrack("en", false, "English", "c")
        assertEquals(made, AddVideoViewModel.bestTrack(listOf(english, auto, made), "pt"))
        assertEquals(auto, AddVideoViewModel.bestTrack(listOf(english, auto), "pt"))
        assertEquals(english, AddVideoViewModel.bestTrack(listOf(english), "pt"))
        assertEquals(null, AddVideoViewModel.bestTrack(emptyList(), "pt"))
    }

    @Test
    fun watchingChoicesAreKeptForTheNextVideo() = runBlocking {
        val vm = watch()
        vm.setBlur(VideoBlur.LINE)
        vm.setShowTranslations(false)
        vm.setPauseOnLookup(false)
        val start = System.currentTimeMillis()
        while (settings.current.videoPauseOnLookup && System.currentTimeMillis() - start < 3_000) Thread.sleep(20)
        assertEquals(VideoBlur.LINE, settings.current.videoBlur)
        assertEquals(false, settings.current.videoTranslations)
        val next = watch().state.value
        assertEquals(VideoBlur.LINE, next.blur)
        assertEquals(false, next.showTranslations)
        assertEquals(false, next.pauseOnLookup)
    }

    @Test
    fun hiddenLinesAreThoseNotHeardYetAndRevealedOnesShow() {
        val lines = WatchUiState(blur = VideoBlur.LINE, revealed = setOf(3))
        // Heard lines show; the one being spoken and those to come stay hidden, unless revealed.
        assertEquals(listOf(false, true, true, false), (0..3).map { lines.hidesLine(it, current = 1) })
        val translations = WatchUiState(blur = VideoBlur.TRANSLATION, revealed = setOf(2))
        assertEquals(listOf(true, true, false), (0..2).map { translations.hidesTranslation(it, current = 1) })
        assertEquals(false, WatchUiState().hidesTranslation(0, null))
    }

    @Test
    fun theWordsOfAVideoAreCountedOnceAndListedNewFirst() = runBlocking {
        val vm = watch()
        val start = System.currentTimeMillis()
        while (vm.state.value.loading && System.currentTimeMillis() - start < 5_000) Thread.sleep(20)
        val words = videoWords(vm.state.value.lines, WordOrder.MOST_FREQUENT)
        assertEquals(words.map { it.textLc }.toSet().size, words.size, "each word once")
        assertEquals("o", words.first().textLc, "'o' comes most often")
        assertEquals(2, words.first().count)
        val gato = words.single { it.textLc == "gato" }
        assertEquals(1, gato.firstLine)
        assertEquals("gato", vm.state.value.lines[1].items[gato.firstItem].renderText)
        assertEquals(words.sortedBy { it.textLc }, videoWords(vm.state.value.lines, WordOrder.ALPHABETICAL))
    }
}
