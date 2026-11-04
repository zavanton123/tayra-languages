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
                kind = VideoKind.FILE,
                media = "/videos/gato.mp4",
                subtitles = SubtitleTrack(listOf(Cue(500, 2000, "Olá, tudo bem?"), Cue(2000, 4000, "O gato dorme na casa."), Cue(4000, 6000, "E o cão?"))),
                translation = listOf(Cue(500, 2000, "Hello, how are you?")),
                translationSource = TranslationSource.FILE,
            ),
        )
    }

    private fun watch() = WatchViewModel(bookId, videos, books, languages, reading, BookStatsService(books, languages, settings, reading), translator)

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
        val list = VideosViewModel(videos, books, settings)
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
    fun addingAFileFindsItsSubtitlesBesideItAndOpensTheNewVideo() = runBlocking {
        val add = AddVideoViewModel(service, videos, settings, languages)
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nBom dia.\n\n2\n00:00:02,500 --> 00:00:04,000\nComo vai?\n".encodeToByteArray()
        val english = "1\n00:00:01,000 --> 00:00:02,000\nGood morning.\n".encodeToByteArray()
        add.pickMedia("Aula 1.mp4", "/videos/Aula 1.mp4", listOf("Aula 1.en.srt" to english, "Aula 1.srt" to srt))
        val picked = assertNotNull(add.state.value.subtitles)
        assertEquals("Aula 1.srt", picked.name, "the subtitles named like the video are taken first")
        assertEquals("Aula 1", add.state.value.title)
        add.pickSidecar("Aula 1.en.srt", asTranslation = true)
        assertEquals(1, add.state.value.translation?.cues?.size)
        add.add()
        val event = add.events.flow.first()
        val id = assertIs<AddVideoEvent.Added>(event).bookId
        assertEquals(listOf("Bom dia.", "Como vai?"), videos.cues(id).map { it.cue.text })
        assertEquals("Good morning.", videos.cues(id).first().cue.translation)
        add.pickSubtitles("broken.srt", "not subtitles".encodeToByteArray())
        assertEquals("No subtitles were found in this file.", add.state.value.error)
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
}
