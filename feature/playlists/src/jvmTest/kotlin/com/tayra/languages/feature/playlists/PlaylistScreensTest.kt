package com.tayra.languages.feature.playlists

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.playlists.ListeningPosition
import com.tayra.languages.core.domain.playlists.PlaylistDraft
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** The playlists, making one, and a playlist's own page, as the screens show them. */
@OptIn(ExperimentalTestApi::class)
class PlaylistScreensTest {
    private val data = PlaylistTestData()
    private val text = "O carro parou na estrada. O motorista desceu e olhou o motor. Nada parecia errado. "

    /** Three playlists: one being listened to, one not started, one heard to the end. */
    private fun library(): List<Long> = runBlocking {
        val books = listOf("Uma compra com problema", "O dia a dia", "Na cidade", "Saúde e bem-estar").map { data.book(it, text.repeat(8)) }
        val morning = data.service.create(
            data.languageId,
            PlaylistDraft("Morning Portuguese", "Everyday listening", cover = 0),
            listOf(PlaylistSource.WholeCourse(data.course.id), PlaylistSource.Text(books[0]), PlaylistSource.Text(books[1])),
        )
        val work = data.service.create(data.languageId, PlaylistDraft("Work & Business", "Professional vocabulary", cover = 1), listOf(PlaylistSource.Text(books[2])))
        val stories = data.service.create(data.languageId, PlaylistDraft("Short stories", "Easy graded stories", cover = 2), listOf(PlaylistSource.Text(books[3])))
        val items = data.service.playlist(morning)!!.items
        items.take(2).forEach { data.repository.setFinished(it.id, Clock.System.now()) }
        data.repository.savePosition(morning, ListeningPosition(items[2].id, 1, 0), Clock.System.now())
        data.repository.setFinished(data.service.playlist(stories)!!.items[0].id, Clock.System.now())
        listOf(morning, work, stories)
    }

    @Test
    fun thePlaylistsShowTheOneToGoOnWithAndCanBeSearchedAndSorted() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val (morning, work) = library()
        val viewModel = PlaylistsViewModel(data.service, data.player, data.settings, data.languages, data.time)
        val visited = mutableListOf<Route>()
        host(data) {
            val state by viewModel.state.collectAsState()
            if (!state.loading) PlaylistsContent(state, onNavigate = { visited += it }, onSearch = viewModel::setSearch, onSort = viewModel::setSort, onPlay = viewModel::play, onDelete = viewModel::delete)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your playlists")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("continue-playlist").assertExists()
        onNodeWithText("CONTINUE LISTENING").assertExists()
        onNodeWithText("O vídeo da Júlia").assertExists()
        onNodeWithText("2 of 5 texts").assertExists()
        onNodeWithText("3 playlists · 7 texts").assertExists()
        onNodeWithText("40%").assertExists()
        screenshot("playlists")

        onNodeWithTag("playlist-$work").performClick()
        assertEquals(listOf<Route>(Route.Playlist(work)), visited)
        onNodeWithText("Search playlists").performTextInput("work")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Morning Portuguese")).fetchSemanticsNodes().size == 1 }
        onNodeWithTag("playlist-$morning").assertDoesNotExist()
    }

    @Test
    fun onAPhoneThePlaylistsAreOneColumn() = runDesktopComposeUiTest(width = 400, height = 1500) {
        library()
        val viewModel = PlaylistsViewModel(data.service, data.player, data.settings, data.languages, data.time)
        host(data) {
            val state by viewModel.state.collectAsState()
            if (!state.loading) PlaylistsContent(state, onNavigate = {}, onSearch = viewModel::setSearch, onSort = viewModel::setSort, onPlay = viewModel::play, onDelete = viewModel::delete)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your playlists")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlists-phone")
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

    /** The course pages' menu puts a whole course, or one lesson, after a playlist's texts. */
    @Test
    fun aCourseOrALessonGoesIntoAPlaylistFromItsMenu() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val book = data.book("Na cidade", text.repeat(4))
        val playlist = runBlocking { data.service.create(data.languageId, PlaylistDraft("Commute", "", cover = 0), listOf(PlaylistSource.Text(book))) }
        val picker = PlaylistPickerViewModel(data.service, data.settings)
        val added = mutableListOf<String>()
        val lesson = data.course.lessons[1]
        host(data, "Course") {
            AddToPlaylistMenuButton(PlaylistSource.Lesson(data.course.id, lesson.id), onAdded = { added += it }, viewModel = picker)
            AddToPlaylistMenuButton(PlaylistSource.WholeCourse(data.course.id), onAdded = { added += it }, label = "Add course to playlist", viewModel = picker)
        }
        onNodeWithTag("add-lesson-to-playlist-${lesson.id}").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("picker-playlist-$playlist"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlist-picker")
        onNodeWithTag("picker-playlist-$playlist", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 5_000) { added.size == 1 }
        assertEquals(listOf("Na cidade", lesson.title), runBlocking { data.service.playlist(playlist)!!.items.map { it.title } })

        // The whole course follows, its lesson already there keeping its place.
        onNodeWithText("Add course to playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("picker-playlist-$playlist"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("picker-playlist-$playlist", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 5_000) { added.size == 2 }
        assertEquals(listOf("Commute", "Commute"), added)
        val titles = runBlocking { data.service.playlist(playlist)!!.items.map { it.title } }
        assertEquals(listOf("Na cidade", lesson.title, data.course.lessons[0].title, data.course.lessons[2].title), titles)
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

    @Test
    fun onAPhoneTheNewPlaylistDialogFits() = runDesktopComposeUiTest(width = 400, height = 900) {
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null), data.service, data.settings)
        host(data, "Playlists") {
            NewPlaylistDialog(viewModel, onCreated = { _, _ -> }, onDismiss = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("playlist-name")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlist-create-phone")
    }

    @Test
    fun aPlaylistsPageReordersItsTextsAndDeletesIt() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val (morning) = library()
        val viewModel = PlaylistDetailViewModel(morning, data.service, data.player, data.time, data.settings)
        var left = false
        host(data) {
            val state by viewModel.state.collectAsState()
            val playlist = state.playlist
            if (!state.loading && playlist != null) PlaylistDetailContent(state, playlist, viewModel, onNavigate = {}, onBack = { left = true })
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Drag to reorder.")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("2 of 5 played").assertExists()
        onNodeWithText("Current").assertExists()
        screenshot("playlist-detail")

        val items = runBlocking { data.service.playlist(morning)!!.items }
        onNodeWithTag("item-menu-${items[0].id}").performClick()
        // Playing a text is the row's own button, not a menu item.
        assertTrue(onAllNodes(hasText("Play from here")).fetchSemanticsNodes().isEmpty())
        onNodeWithText("Move down").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { data.service.playlist(morning)!!.items.map { it.id } } == listOf(items[1].id, items[0].id) + items.drop(2).map { it.id } }

        onNodeWithTag("play-item-${items[2].id}").performClick()
        waitUntil(timeoutMillis = 5_000) { data.player.state.value.let { it.playlist?.id == morning && it.itemId == items[2].id } }

        onNodeWithTag("delete-playlist").performClick()
        onNodeWithTag("delete-playlist-dialog").assertExists()
        onNodeWithText("Morning Portuguese will be removed. The books, courses and texts inside it will stay in your library.").assertExists()
        screenshot("playlist-delete")
        onNodeWithTag("confirm-delete-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { left }
        assertNull(runBlocking { data.service.playlist(morning) })
    }

    @Test
    fun anEmptyPlaylistSaysHowToFillIt() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val id = runBlocking { data.service.create(data.languageId, PlaylistDraft("Travel Portuguese")) }
        val viewModel = PlaylistDetailViewModel(id, data.service, data.player, data.time, data.settings)
        host(data) {
            val state by viewModel.state.collectAsState()
            val playlist = state.playlist
            if (!state.loading && playlist != null) PlaylistDetailContent(state, playlist, viewModel, onNavigate = {}, onBack = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("This playlist is empty")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("0 texts").assertExists()
        onNodeWithText("You can also add the current text from the reader.").assertExists()
        onNodeWithTag("play-all").assertIsNotEnabled()
        screenshot("playlist-empty")
    }

    @Test
    fun onAPhoneAPlaylistsPageIsOneColumn() = runDesktopComposeUiTest(width = 400, height = 1600) {
        val (morning) = library()
        val viewModel = PlaylistDetailViewModel(morning, data.service, data.player, data.time, data.settings)
        host(data, "Morning Portuguese") {
            val state by viewModel.state.collectAsState()
            val playlist = state.playlist
            if (!state.loading && playlist != null) PlaylistDetailContent(state, playlist, viewModel, onNavigate = {}, onBack = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Drag to reorder.")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlist-detail-phone")
    }
}
