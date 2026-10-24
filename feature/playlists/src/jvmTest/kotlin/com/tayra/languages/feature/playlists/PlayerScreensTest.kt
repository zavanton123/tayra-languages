package com.tayra.languages.feature.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.playlists.PlaylistDraft
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The player screen, the app-wide player bar, the reasons a text cannot play and the reader's playlist menu. */
@OptIn(ExperimentalTestApi::class)
class PlayerScreensTest {
    private val data = PlaylistTestData()

    /** Russian for every sentence, as the translation shown under it. */
    private val translator = object : SentenceTranslator {
        override suspend fun translate(text: String, language: Language): String? =
            if (text.startsWith("\"Chamar")) "«Называть устройство барахлом ничего не решает», — сказала она мужу." else "Перевод."
    }

    private fun viewModel() = PlayerViewModel(data.player, data.service, translator, data.languages, data.settings, data.time)

    /** A playlist of the course's three lessons and a book, playing its first sentence. */
    private fun playing(): Long = runBlocking {
        val book = data.book("Mudanças", "A família mudou de casa no verão. As caixas ficaram na sala por um mês. ".repeat(5))
        val id = data.service.create(data.languageId, PlaylistDraft("Morning Portuguese", "Everyday listening"), listOf(PlaylistSource.WholeCourse(data.course.id), PlaylistSource.Text(book)))
        data.player.play(id)
        id
    }

    private fun until(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!condition()) {
            check(System.currentTimeMillis() < end) { "timed out; ${data.player.state.value}" }
            Thread.sleep(10)
        }
    }

    @Test
    fun thePlayerShowsTheSentenceItsWordAndTranslationAndWhatComesNext() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val id = playing()
        until { data.output.done != null }
        data.output.onWord!!(22..29)
        until { data.player.state.value.word == 22..29 }
        val viewModel = viewModel()
        val visited = mutableListOf<Route>()
        host(data) {
            val state by viewModel.state.collectAsState()
            Column {
                Box(Modifier.weight(1f)) { PlayerContent(state, viewModel, onNavigate = { visited += it }) }
                PlayerBar(onNavigate = { visited += it }, onPlayerScreen = true, viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Up next")).fetchSemanticsNodes().isNotEmpty() && onAllNodes(hasText("ничего не решает", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 of 4").assertExists()
        onNodeWithText("Sentence 1 of 12").assertExists()
        onNodeWithTag("player-bar").assertExists()
        screenshot("player")

        // On the player, the bar's text goes back to the text in the reader.
        onNodeWithTag("back-to-text").assertDoesNotExist()
        onNodeWithTag("open-text").performClick()
        val bookId = data.player.state.value.item!!.bookId
        assertEquals(Route.Read(bookId, 1), visited.last())
        onNodeWithTag("next-sentence").performClick()
        until { data.player.state.value.sentence == 1 }
        onNodeWithTag("bar-next").performClick()
        until { data.player.state.value.queueIndex == 1 }
        onNodeWithTag("play-pause").performClick()
        until { data.player.state.value.status == PlayerStatus.PAUSED }
        onNodeWithTag("repeat").performClick()
        until { data.player.state.value.repeat == com.tayra.languages.core.domain.playlists.RepeatMode.ALL }
        assertTrue(id > 0)
    }

    @Test
    fun aPlaylistHeardToTheEndSaysSoAndPlaysAgain() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        playing()
        // Every sentence of every text heard.
        until { data.output.done != null }
        while (data.player.state.value.status != PlayerStatus.COMPLETE) {
            data.output.finish()
            until { data.output.done != null || data.player.state.value.status == PlayerStatus.COMPLETE }
        }
        val viewModel = viewModel()
        host(data) {
            val state by viewModel.state.collectAsState()
            Column {
                Box(Modifier.weight(1f)) { PlayerContent(state, viewModel, onNavigate = {}) }
                PlayerBar(onNavigate = {}, onPlayerScreen = true, viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Playlist complete")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("You listened to all 4 texts", substring = true).assertExists()
        screenshot("player-complete")
        onNodeWithTag("play-again").performClick()
        until { data.player.state.value.status == PlayerStatus.PLAYING && data.player.state.value.queueIndex == 0 }
    }

    @Test
    fun onAPhoneThePlayerHasItsControlsOnTheScreen() = runDesktopComposeUiTest(width = 400, height = 1700) {
        playing()
        until { data.output.done != null }
        val viewModel = viewModel()
        host(data, "Morning Portuguese") {
            val state by viewModel.state.collectAsState()
            PlayerContent(state, viewModel, onNavigate = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Up next")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("player-phone")
    }

    @Test
    fun onAPhoneTheBarIsSmallAndOpensThePlayer() = runDesktopComposeUiTest(width = 400, height = 300) {
        playing()
        until { data.output.done != null }
        val viewModel = viewModel()
        val visited = mutableListOf<Route>()
        host(data) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f))
                PlayerBar(onNavigate = { visited += it }, onPlayerScreen = false, viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Morning Portuguese")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("player-bar-phone")
        onNodeWithTag("open-player").performClick()
        assertEquals(listOf<Route>(Route.Player), visited)
    }

    /** The bar alone at the foot of a window [width] wide, until its close button stops the player. */
    private fun closingTheBar(width: Int, name: String) = runDesktopComposeUiTest(width = width, height = 300) {
        val id = playing()
        until { data.output.done != null }
        data.output.finish()
        until { data.player.state.value.sentence == 1 && data.output.done != null }
        val viewModel = viewModel()
        host(data) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f))
                PlayerBar(onNavigate = {}, onPlayerScreen = false, viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Morning Portuguese")).fetchSemanticsNodes().isNotEmpty() }
        screenshot(name)
        onNodeWithTag("close-player").performClick()
        until { data.player.state.value.status == PlayerStatus.IDLE }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("player-bar")).fetchSemanticsNodes().isEmpty() }
        // Stopped where it was, to go on from there when the playlist is played again.
        val position = runBlocking { data.service.playlist(id)!!.position }
        assertEquals(1, position?.sentence)
        data.player.play(id)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("player-bar")).fetchSemanticsNodes().isNotEmpty() }
        until { data.player.state.value.sentence == 1 && data.player.state.value.status == PlayerStatus.PLAYING }
    }

    @Test
    fun theWideBarsControlsNameThemselvesOnHover() = runDesktopComposeUiTest(width = 1586, height = 300) {
        playing()
        until { data.output.done != null }
        val viewModel = viewModel()
        val visited = mutableListOf<Route>()
        host(data) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f))
                PlayerBar(onNavigate = { visited += it }, onPlayerScreen = false, viewModel = viewModel)
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Morning Portuguese")).fetchSemanticsNodes().isNotEmpty() }
        for ((tag, tip) in listOf("shuffle" to "Shuffle off", "bar-previous" to "Previous text", "bar-next" to "Next text", "repeat" to "Repeat off", "seek" to "Position in the text", "speed" to "Playback speed", "close-player" to "Close player")) {
            onNodeWithTag(tag).performMouseInput { moveTo(center) }
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(tip)).fetchSemanticsNodes().isNotEmpty() }
            if (tag == "seek") screenshot("player-bar-tooltip")
            onNodeWithTag(tag).performMouseInput { exit() }
        }

        // Off the player, the text opens the player.
        onNodeWithTag("back-to-text").assertDoesNotExist()
        onNodeWithTag("open-player").performClick()
        assertEquals(listOf<Route>(Route.Player), visited)
    }

    @Test
    fun theWideBarClosesThePlayerKeepingItsPlace() = closingTheBar(width = 1586, name = "player-bar-close")

    @Test
    fun thePhoneBarClosesThePlayerKeepingItsPlace() = closingTheBar(width = 400, name = "player-bar-close-phone")

    @Test
    fun aTextWithoutAVoiceOffersADownloadTheSystemVoiceOrASkip() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val problem = com.tayra.languages.core.domain.playlists.PlaybackProblem.NoVoice("Piper", data.languageId, "Portuguese")
        var chosen = ""
        host(data) {
            ProblemDialog(problem, title = "O vídeo da Júlia", place = "Text 3 of 8", onDownload = { chosen = "download" }, onSystemVoice = { chosen = "system" }, onSkip = { chosen = "skip" }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Can't play this text")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("The Portuguese voice used by this playlist is not available on this device.").assertExists()
        screenshot("player-problem")
        onNodeWithTag("system-voice").performClick()
        assertEquals("system", chosen)
    }

    /** A text with nothing to read aloud can be taken out of the playlist or passed over, with no voice to offer. */
    @Test
    fun aTextWithNoWordsOffersToTakeItOutOrSkipIt() = runDesktopComposeUiTest(width = 400, height = 900) {
        var chosen = ""
        host(data) {
            ProblemDialog(
                com.tayra.languages.core.domain.playlists.PlaybackProblem.NoText("O vídeo da Júlia"), title = "O vídeo da Júlia", place = "Text 3 of 8",
                onDownload = { chosen = "download" }, onSystemVoice = { chosen = "system" }, onSkip = { chosen = "skip" }, onDismiss = {}, onRemove = { chosen = "remove" },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("This text has no words to read aloud.")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(onAllNodes(hasTestTag("download-voice")).fetchSemanticsNodes().isEmpty() && onAllNodes(hasTestTag("system-voice")).fetchSemanticsNodes().isEmpty())
        onNodeWithTag("skip-text").assertExists()
        screenshot("player-problem-no-text")
        onNodeWithTag("remove-text").performClick()
        assertEquals("remove", chosen)
    }

    /** A text deleted from the library is named once; there is nothing to choose, listening has gone on. */
    @Test
    fun aDeletedTextIsToldWithOnlyAnOk() = runDesktopComposeUiTest(width = 400, height = 900) {
        var dismissed = 0
        host(data) {
            ProblemDialog(
                com.tayra.languages.core.domain.playlists.PlaybackProblem.TextGone("O vídeo da Júlia"), title = "", place = "",
                onDownload = {}, onSystemVoice = {}, onSkip = {}, onDismiss = { dismissed++ },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Text no longer available")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("\"O vídeo da Júlia\" was deleted from your library, so it left the playlist. Listening goes on with the next text.").assertExists()
        assertTrue(onAllNodes(hasTestTag("skip-text")).fetchSemanticsNodes().isEmpty())
        onNodeWithTag("text-gone-ok").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theReadersPlaylistMenuPutsTheTextInAndTakesItOut() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val book = data.book("O ar-condicionado em promoção", "Chamar o aparelho de porcaria não resolve nada.")
        val (morning, work) = runBlocking {
            listOf(data.service.create(data.languageId, PlaylistDraft("Morning Portuguese")), data.service.create(data.languageId, PlaylistDraft("Work & Business", cover = 1)))
        }
        val viewModel = AddToPlaylistViewModel(PlaylistSource.Text(book), data.service, data.settings)
        var created = false
        var added: String? = null
        host(data, "Reader") {
            Box(Modifier.fillMaxSize()) {
                AddToPlaylistButton(book, AppSpacing.giant, onCreate = { created = true }, onAdded = { added = it }, modifier = Modifier.align(Alignment.BottomEnd).padding(AppSpacing.xxl), viewModel = viewModel)
            }
        }
        onNodeWithTag("add-to-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Work & Business")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("reader-add-to-playlist")
        onNodeWithTag("menu-playlist-$morning").performClick()
        // Putting the text in a playlist closes the menu and leaves the confirmation to the screen.
        waitUntil(timeoutMillis = 5_000) { added == "Morning Portuguese" }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("add-to-playlist-menu").fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf(book), runBlocking { data.service.playlist(morning)!!.items.map { it.bookId } })
        onNodeWithTag("add-to-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Work & Business")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("menu-playlist-$morning").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { data.service.playlist(morning)!!.items.isEmpty() } }
        onNodeWithTag("menu-new-playlist").performClick()
        assertTrue(created)
        assertTrue(work > 0 && LanguageCodes.codeFor("Portuguese") == "pt")
    }
}
