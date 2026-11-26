package com.idiomiko.feature.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.playlists.ListeningPosition
import com.idiomiko.core.domain.playlists.PlayerStatus
import com.idiomiko.core.domain.playlists.PlaylistDraft
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.domain.service.SentenceTranslator
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The Playlists page with the chosen playlist beside the list, the phone's list and playlist pages, and making playlists. */
@OptIn(ExperimentalTestApi::class)
class PlaylistScreensTest {
    private val data = PlaylistTestData()
    private val text = "O carro parou na estrada. O motorista desceu e olhou o motor. Nada parecia errado. "

    /** Three playlists: one being listened to (at its third text), one not started, one heard to the end. */
    private fun library(): List<Long> = runBlocking {
        val books = listOf("Uma compra com problema", "O dia a dia", "Na cidade", "Saúde e bem-estar").map { data.book(it, text.repeat(8)) }
        val morning = data.service.create(
            data.languageId,
            PlaylistDraft("Morning Portuguese", "Everyday listening", cover = 0),
            listOf(PlaylistSource.WholeCourse(data.course.id), PlaylistSource.Text(books[0]), PlaylistSource.Text(books[1])),
        )
        val work = data.service.create(data.languageId, PlaylistDraft("Work & Business", "Professional vocabulary", cover = 5), listOf(PlaylistSource.Text(books[2])))
        val stories = data.service.create(data.languageId, PlaylistDraft("Short stories", "Easy graded stories", cover = 2), listOf(PlaylistSource.Text(books[3])))
        val items = data.service.playlist(morning)!!.items
        items.take(2).forEach { data.repository.setFinished(it.id, Clock.System.now()) }
        data.repository.savePosition(morning, ListeningPosition(items[2].id, 1, 0), Clock.System.now())
        data.repository.setFinished(data.service.playlist(stories)!!.items[0].id, Clock.System.now())
        data.know(
            "o", "a", "e", "na", "carro", "parou", "estrada", "motorista", "desceu", "olhou", "motor", "nada", "parecia", "caixa", "chegou", "produto",
            "não", "júlia", "gravou", "um", "vídeo", "do", "escreveu", "à", "loja", "chamar", "aparelho", "de", "resolve", "disse", "ela", "ao", "marido", "que", "é", "problema",
        )
        listOf(morning, work, stories)
    }

    /** Plays [playlist] from where it got to and waits until its first sentence is being said. */
    private fun playing(playlist: Long) {
        data.player.play(playlist)
        val end = System.currentTimeMillis() + 10_000
        while (data.output.done == null || data.player.state.value.status != PlayerStatus.PLAYING) {
            check(System.currentTimeMillis() < end) { "not playing: ${data.player.state.value}" }
            Thread.sleep(10)
        }
    }

    private val translator = object : SentenceTranslator {
        override suspend fun translate(text: String, language: Language): String? = null
    }

    /** The Playlists page as the app shows it, with the player's bar under it. */
    private fun ComposeUiTest.page(viewModel: PlaylistsViewModel, theme: AppTheme = AppThemes.default, visited: MutableList<Route> = mutableListOf(), content: (@Composable () -> Unit)? = null) {
        val bar = PlayerViewModel(data.player, data.service, translator, data.languages, data.settings, data.time)
        setContent {
            IdiomikoTheme(theme) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(data.languageId to "Portuguese"), data.languageId) {}) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                Box(Modifier.weight(1f)) {
                                    content?.invoke() ?: PlaylistsScreen(onNavigate = { visited += it }, viewModel = viewModel, detail = { id -> remember(id) { data.detail(id) } })
                                }
                                PlayerBar(onNavigate = { visited += it }, onPlayerScreen = false, viewModel = bar)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.shown(tag: String) = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun order(playlist: Long) = runBlocking { data.service.playlist(playlist)!!.items.map { it.id } }

    @Test
    fun aWidePageShowsTheLastPlayedPlaylistBesideTheListAndAnotherWhenChosen() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (morning, work, stories) = library()
        playing(morning)
        page(data.playlists())
        waitUntil(timeoutMillis = 10_000) { shown("now-playing") && onAllNodes(hasText("% known", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("playlist-$morning").assertIsSelected()
        onNodeWithTag("playing-$morning", useUnmergedTree = true).assertExists()
        onNodeWithText("Short stories").assertExists()
        onNodeWithText("1 track · 1 min · ✓ done").assertExists()
        onNodeWithTag("playlist-facts").assertTextContains("2 of 5 played", substring = true)
        onNodeWithTag("playlist-facts").assertTextContains("3 lessons, 2 books", substring = true)
        assertEquals(3, onAllNodes(hasText("Lesson · Compras e serviços")).fetchSemanticsNodes().size)
        onNodeWithText("Pause").assertExists()
        // One gap at the window's edge and between the two panes, and both panes start at the same height.
        val sidebar = onNodeWithTag("sidebar").fetchSemanticsNode().boundsInRoot
        val header = onNodeWithTag("playlist-header").fetchSemanticsNode().boundsInRoot
        val inset = with(density) { 24.dp.toPx() }
        assertEquals(sidebar.left, header.left - inset - sidebar.right, 1f)
        assertEquals(sidebar.top, header.top - inset, 1f)
        screenshot("playlists")

        // Choosing another playlist shows it, and the page opens on it again for the rest of the session.
        onNodeWithTag("playlist-$work").performClick()
        waitUntil(timeoutMillis = 5_000) { !shown("now-playing") }
        onNodeWithTag("playlist-$work").assertIsSelected()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-facts") and hasText("0 of 1 played", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("play-all").assertTextContains("Play")
        assertEquals(work, data.selection.id.value)

        onNodeWithText("Search playlists").performTextInput("stor")
        waitUntil(timeoutMillis = 5_000) { !shown("playlist-$morning") }
        onNodeWithTag("playlist-$stories").assertExists()
    }

    @Test
    fun theWidePageInTheDarkTheme() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (morning) = library()
        playing(morning)
        page(data.playlists(), theme = AppThemes.night)
        waitUntil(timeoutMillis = 10_000) { shown("now-playing") && onAllNodes(hasText("% known", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlists-dark")
    }

    @Test
    fun aRouteToAPlaylistOpensThePageWithItChosen() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (_, work) = library()
        val viewModel = data.playlists()
        page(viewModel) { PlaylistsScreen(onNavigate = {}, selectId = work, viewModel = viewModel, detail = { id -> remember(id) { data.detail(id) } }) }
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") }
        onNodeWithTag("playlist-$work").assertIsSelected()
    }

    @Test
    fun theNowPlayingStripFollowsTheSentenceAndOpensThePlayer() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (morning) = library()
        val visited = mutableListOf<Route>()
        page(data.playlists(), visited = visited)
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") }
        // Before anything plays there is no strip, and the third text is the one to go on with.
        assertTrue(!shown("now-playing"))
        val items = runBlocking { data.service.playlist(morning)!!.items }
        onNodeWithTag("status-${items[2].id}", useUnmergedTree = true).assertTextContains("Up next")
        onNodeWithTag("status-${items[0].id}", useUnmergedTree = true).assertTextContains("✓ Played")
        onNodeWithTag("play-all").assertTextContains("Continue")

        onNodeWithTag("play-all").performClick()
        waitUntil(timeoutMillis = 10_000) { data.output.done != null && shown("now-playing") }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A Júlia gravou um vídeo", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        val sentences = data.player.state.value.sentences
        onNodeWithTag("now-playing-sentence").assertTextContains("A Júlia gravou um vídeo", substring = true)
        onNodeWithTag("now-playing").assertExists()
        onNodeWithText("Now playing · O vídeo da Júlia · sentence 1 of ${sentences.size}").assertExists()
        onNodeWithTag("status-${items[2].id}", useUnmergedTree = true).assertTextContains("Playing · 1 of ${sentences.size}")
        onNodeWithTag("status-${items[3].id}", useUnmergedTree = true).assertTextContains("Up next")

        data.output.finish()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Now playing · O vídeo da Júlia · sentence 2 of ${sentences.size}")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("now-playing-open").performClick()
        assertEquals(listOf<Route>(Route.Player), visited)
    }

    @Test
    fun aTextIsPlayedFromItsButtonOrByAClickThatOpensThePlayer() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (morning) = library()
        val visited = mutableListOf<Route>()
        page(data.playlists(), visited = visited)
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") }
        val items = runBlocking { data.service.playlist(morning)!!.items }
        onNodeWithTag("play-item-${items[3].id}").performClick()
        waitUntil(timeoutMillis = 10_000) { data.player.state.value.let { it.playlist?.id == morning && it.itemId == items[3].id } }
        waitUntil(timeoutMillis = 10_000) { data.player.state.value.isPlaying && onAllNodes(hasText("Pause")).fetchSemanticsNodes().isNotEmpty() }

        // The current text's mark holds it.
        onNodeWithTag("play-item-${items[3].id}").performClick()
        waitUntil(timeoutMillis = 5_000) { data.player.state.value.status == PlayerStatus.PAUSED }

        assertTrue(visited.isEmpty(), "the button plays without leaving the page")
        onNodeWithTag("item-${items[1].id}").performMouseInput { moveTo(center) }
        waitForIdle()
        screenshot("playlist-row-hover")

        // A click on the row plays the text and opens the player on it.
        onNodeWithTag("item-${items[0].id}").performClick()
        waitUntil(timeoutMillis = 10_000) { data.player.state.value.itemId == items[0].id && data.player.state.value.isPlaying }
        assertEquals(listOf<Route>(Route.Player), visited)
    }

    @Test
    fun textsAreReorderedByDraggingOrFromTheirMenu() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val (morning) = library()
        page(data.playlists())
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") }
        val items = order(morning)

        onNodeWithTag("item-menu-${items[0]}").performClick()
        onNodeWithText("Move down").performClick()
        waitUntil(timeoutMillis = 5_000) { order(morning) == listOf(items[1], items[0]) + items.drop(2) }

        onNodeWithTag("drag-${items[4]}", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(20) { moveBy(Offset(0f, -10f), delayMillis = 16) }
            up()
        }
        waitUntil(timeoutMillis = 5_000) { order(morning).last() != items[4] }
        assertNotEquals(items[4], order(morning).last())
    }

    @Test
    fun aPlaylistIsDeletedFromItsMenu() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val (morning, work) = library()
        page(data.playlists())
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") }
        onNodeWithTag("playlist-actions").performClick()
        onNodeWithTag("delete-playlist").performClick()
        onNodeWithText("Morning Portuguese will be removed. The books, courses and texts inside it will stay in your library.").assertExists()
        onNodeWithTag("confirm-delete-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { data.service.playlist(morning) } == null }
        // The page goes on with another playlist.
        waitUntil(timeoutMillis = 5_000) { shown("playlist-header") && !shown("playlist-$morning") }
        assertTrue(shown("playlist-$work"))
    }

    @Test
    fun anEmptyPlaylistSaysHowToFillIt() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val id = runBlocking { data.service.create(data.languageId, PlaylistDraft("Travel Portuguese")) }
        page(data.playlists())
        waitUntil(timeoutMillis = 5_000) { shown("playlist-$id") }
        onNodeWithTag("playlist-$id").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("This playlist is empty")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("playlist-$id").assertIsSelected()
        onNodeWithText("You can also add the current text from the reader.").assertDoesNotExist()
        // The empty card reaches down the pane, not stopping under its button.
        val card = onNodeWithTag("empty-playlist").fetchSemanticsNode().boundsInRoot
        val root = onAllNodes(isRoot())[0].fetchSemanticsNode().boundsInRoot
        assertTrue(root.bottom - card.bottom < 60f, "the card fills the pane's height: ${card.bottom} of ${root.bottom}")
        onNodeWithTag("play-all").assertIsNotEnabled()
        screenshot("playlist-empty")
    }

    @Test
    fun aPhonesEmptyPlaylistSaysHowToFillIt() = runDesktopComposeUiTest(width = 400, height = 900) {
        val id = runBlocking { data.service.create(data.languageId, PlaylistDraft("Travel Portuguese")) }
        page(data.playlists()) { PlaylistDetailScreen(id, onNavigate = {}, onBack = {}, viewModel = data.detail(id)) }
        waitUntil(timeoutMillis = 5_000) { shown("empty-playlist") }
        onNodeWithTag("add-texts").assertExists()
        // The bar's back arrow leads back; the card has no link of its own.
        onNodeWithText("Back to playlists").assertDoesNotExist()
        screenshot("playlist-empty-phone")
    }

    /** A language always has its main playlist: there from the start, and with no way to delete it. */
    @Test
    fun aLanguageAlwaysHasItsMainPlaylist() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(data.playlists())
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Main")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(!shown("no-playlists"), "never without a playlist")
        val main = runBlocking { data.service.observePlaylists(data.languageId).first() }.single()
        assertTrue(main.main)
        onNodeWithTag("playlist-actions").performClick()
        assertTrue(!shown("delete-playlist"), "the main playlist cannot be deleted")
        screenshot("playlists-main")
    }

    @Test
    fun aMediumWindowMakesAPlaylistFromTheRowUnderTheListOnly() = runDesktopComposeUiTest(width = 800, height = 900) {
        library()
        page(data.playlists())
        waitUntil(timeoutMillis = 5_000) { shown("new-playlist-row") }
        assertTrue(!shown("new-playlist"), "the row under the list makes a playlist, not a button in the header")
        screenshot("playlists-medium")
    }

    @Test
    fun aPhonesListPlaysAPlaylistFromItsRowWithoutOpeningIt() = runDesktopComposeUiTest(width = 400, height = 900) {
        val (morning, work, stories) = library()
        playing(morning)
        val visited = mutableListOf<Route>()
        page(data.playlists(), visited = visited)
        waitUntil(timeoutMillis = 5_000) { shown("new-playlist-row") }
        assertTrue(!shown("new-playlist"), "the row under the list makes a playlist, not a button in the bar")
        onNodeWithText("2 of 5 played · ", substring = true).assertExists()
        onNodeWithText("1 track · ✓ done").assertExists()
        onAllNodesWithContentDescription("Play again").fetchSemanticsNodes().size.let { assertEquals(1, it) }
        screenshot("playlists-phone")

        onNodeWithTag("play-playlist-$work").performClick()
        waitUntil(timeoutMillis = 10_000) { data.player.state.value.playlist?.id == work && data.player.state.value.isPlaying }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("play-playlist-$work").performClick()
        waitUntil(timeoutMillis = 5_000) { data.player.state.value.status == PlayerStatus.PAUSED }
        assertTrue(visited.isEmpty())

        onNodeWithTag("playlist-$stories").performClick()
        assertEquals(listOf<Route>(Route.Playlist(stories)), visited)
    }

    @Test
    fun aPhonesPlaylistPageShowsWhatIsPlayingAndItsTexts() = runDesktopComposeUiTest(width = 400, height = 900) {
        val (morning) = library()
        playing(morning)
        val viewModel = data.detail(morning)
        var left = false
        val visited = mutableListOf<Route>()
        page(data.playlists()) { PlaylistDetailScreen(morning, onNavigate = { visited += it }, onBack = { left = true }, viewModel = viewModel) }
        waitUntil(timeoutMillis = 10_000) { shown("now-playing") }
        onNodeWithTag("playlist-facts").assertTextContains("2 of 5 played · ", substring = true)
        val items = runBlocking { data.service.playlist(morning)!!.items }
        screenshot("playlist-phone")

        // A tap on a row plays it and opens the player; the one playing goes on.
        val playing = data.player.state.value.itemId!!
        onNodeWithTag("item-$playing").performClick()
        waitForIdle()
        assertEquals(playing, data.player.state.value.itemId)
        assertTrue(data.player.state.value.isPlaying)
        onNodeWithTag("item-${items[4].id}").performClick()
        waitUntil(timeoutMillis = 10_000) { data.player.state.value.itemId == items[4].id }
        assertEquals(listOf<Route>(Route.Player, Route.Player), visited)

        onNodeWithTag("playlist-actions").performClick()
        onNodeWithTag("delete-playlist").performClick()
        onNodeWithTag("confirm-delete-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { left }
        assertNull(runBlocking { data.service.playlist(morning) })
    }

    @Test
    fun aPlaylistIsMadeInADialogFromItsDetails() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null), data.service, data.settings)
        var created: Pair<Long, String>? = null
        host(data, "Playlists") {
            NewPlaylistDialog(viewModel, onCreated = { id, name -> created = id to name }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-name")).fetchSemanticsNodes().isNotEmpty() }
        // The texts are added later, from the playlist's page or the reader.
        assertTrue(onAllNodes(hasText("Compras e serviços")).fetchSemanticsNodes().isEmpty())
        onNodeWithTag("playlist-name").performTextInput("Morning practice")
        onNodeWithText("16 / 60").assertExists()
        onNodeWithTag("playlist-description").performTextInput("Short Portuguese texts for my commute.")
        onNodeWithTag("cover-2").performClick()
        screenshot("playlist-create")
        onNodeWithTag("form-save").performClick()
        waitUntil(timeoutMillis = 5_000) { created != null }
        assertEquals("Morning practice", created!!.second)
        val playlist = runBlocking { data.service.playlist(created!!.first)!! }
        assertEquals("Morning practice", playlist.name)
        assertEquals("Short Portuguese texts for my commute.", playlist.description)
        assertEquals(2, playlist.cover)
        assertTrue(playlist.items.isEmpty())
    }

    @Test
    fun aPlaylistMadeFromTheReaderStartsWithTheOpenText() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val book = data.book("Uma compra com problema", text.repeat(4))
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(book), data.service, data.settings)
        var created: Long? = null
        host(data, "Reader") {
            NewPlaylistDialog(viewModel, onCreated = { id, _ -> created = id }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-name")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("playlist-name").performTextInput("Shopping")
        onNodeWithTag("form-save").performClick()
        waitUntil(timeoutMillis = 5_000) { created != null }
        assertEquals(listOf(book), runBlocking { data.service.playlist(created!!)!!.items.map { it.bookId } })
    }

    /**
     * The course pages' menu is the reader's: a lesson or a whole course goes into a playlist, the
     * playlist is ticked once it holds all of it, and choosing a ticked one takes it out again.
     */
    @Test
    fun aCourseOrALessonGoesIntoAPlaylistAndOutFromItsMenu() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val book = data.book("Na cidade", text.repeat(4))
        val playlist = runBlocking { data.service.create(data.languageId, PlaylistDraft("Commute", "", cover = 0), listOf(PlaylistSource.Text(book))) }
        val lesson = data.course.lessons[1]
        val lessonMenu = AddToPlaylistViewModel(PlaylistSource.Lesson(data.course.id, lesson.id), data.service, data.settings)
        val courseMenu = AddToPlaylistViewModel(PlaylistSource.WholeCourse(data.course.id), data.service, data.settings)
        val added = mutableListOf<String>()
        host(data, "Course") {
            AddToPlaylistMenuButton(PlaylistSource.Lesson(data.course.id, lesson.id), onAdded = { added += it }, viewModel = lessonMenu)
            AddToPlaylistMenuButton(PlaylistSource.WholeCourse(data.course.id), onAdded = { added += it }, label = "Add course to playlist", viewModel = courseMenu)
        }
        fun titles() = runBlocking { data.service.playlist(playlist)!!.items.map { it.title } }
        fun open(tag: String) {
            onNodeWithTag(tag).performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("menu-playlist-$playlist")).fetchSemanticsNodes().isNotEmpty() }
        }
        fun ticked() = onAllNodesWithContentDescription("In this playlist").fetchSemanticsNodes().size

        open("add-lesson-to-playlist-${lesson.id}")
        assertEquals(0, ticked())
        onNodeWithTag("menu-playlist-$playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { added.size == 1 }
        assertEquals(listOf("Na cidade", lesson.title), titles())

        // Opened again, the playlist is ticked; choosing it takes the lesson out, and the menu stays.
        open("add-lesson-to-playlist-${lesson.id}")
        waitUntil(timeoutMillis = 5_000) { ticked() == 1 }
        screenshot("playlist-picker")
        onNodeWithTag("menu-playlist-$playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { titles() == listOf("Na cidade") }
        waitUntil(timeoutMillis = 5_000) { ticked() == 0 }
        onNodeWithTag("add-lesson-to-playlist-${lesson.id}").performClick()

        // The whole course goes in, and is ticked only once every lesson is there.
        open("add-course-to-playlist-${data.course.id}")
        assertEquals(0, ticked())
        onNodeWithTag("menu-playlist-$playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { added.size == 2 }
        assertEquals(listOf("Commute", "Commute"), added)
        assertEquals(listOf("Na cidade") + data.course.lessons.map { it.title }, titles())
        open("add-course-to-playlist-${data.course.id}")
        waitUntil(timeoutMillis = 5_000) { ticked() == 1 }
        onNodeWithTag("menu-playlist-$playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { titles() == listOf("Na cidade") }
    }

    @Test
    fun aPlaylistMadeFromACoursePageStartsWithTheCourse() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null, PlaylistSource.WholeCourse(data.course.id)), data.service, data.settings)
        var created: Long? = null
        host(data, "Course") {
            NewPlaylistDialog(viewModel, onCreated = { id, _ -> created = id }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-name")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("playlist-name").performTextInput("Shopping")
        onNodeWithTag("form-save").performClick()
        waitUntil(timeoutMillis = 5_000) { created != null }
        assertEquals(data.course.lessons.map { it.title }, runBlocking { data.service.playlist(created!!)!!.items.map { it.title } })
    }

    /** However long the list of texts, the page's buttons stay in view on a wide screen as on a phone. */
    @Test
    fun theAddTextsButtonsStayInViewOverALongList() = runDesktopComposeUiTest(width = 1586, height = 900) {
        val playlist = runBlocking { data.service.create(data.languageId, PlaylistDraft("Commute", "", cover = 0), emptyList()) }
        repeat(20) { data.book("Texto $it", text) }
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.AddTexts(playlist), data.service, data.settings)
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(data.languageId to "Portuguese"), data.languageId) {}) {
                        PlaylistFormScreen(PlaylistFormMode.AddTexts(playlist), onNavigate = {}, onSaved = {}, onCancel = {}, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Texto 19")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("form-save").assertIsDisplayed()
        onNodeWithText("Cancel").assertIsDisplayed()
        assertTrue(onAllNodes(hasText("reorder texts later", substring = true)).fetchSemanticsNodes().isEmpty())
        screenshot("playlist-add-texts")
    }

    /** On a phone the bar holds the page's title, and the texts' card starts right under it. */
    @Test
    fun onAPhoneTheAddTextsCardStartsUnderTheBar() = runDesktopComposeUiTest(width = 400, height = 900) {
        val playlist = runBlocking { data.service.create(data.languageId, PlaylistDraft("Commute", "", cover = 0), emptyList()) }
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.AddTexts(playlist), data.service, data.settings)
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(data.languageId to "Portuguese"), data.languageId) {}) {
                        PlaylistFormScreen(PlaylistFormMode.AddTexts(playlist), onNavigate = {}, onSaved = {}, onCancel = {}, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Search books, courses and lessons")).fetchSemanticsNodes().isNotEmpty() }
        val back = onNodeWithContentDescription("Back").getBoundsInRoot()
        val search = onNodeWithText("Search books, courses and lessons").getBoundsInRoot()
        // The bar's own room, the page's padding and the card's: no empty header's gap besides.
        assertTrue(search.top - back.bottom < 60.dp, "the search starts ${search.top - back.bottom} under the bar")
        screenshot("playlist-add-texts-phone")
    }

    @Test
    fun onAPhoneTheNewPlaylistDialogFits() = runDesktopComposeUiTest(width = 400, height = 900) {
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null), data.service, data.settings)
        host(data, "Playlists") {
            NewPlaylistDialog(viewModel, onCreated = { _, _ -> }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-name")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlist-create-phone")
    }

}
