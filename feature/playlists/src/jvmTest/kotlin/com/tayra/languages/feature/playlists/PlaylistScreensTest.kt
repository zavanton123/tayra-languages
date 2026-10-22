package com.tayra.languages.feature.playlists

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun aPlaylistIsMadeFromBooksArticlesAndLessons() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val book = data.book("Uma compra com problema", text.repeat(4))
        data.newsArticle("Chuva forte em São Paulo", text)
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null), data.service, data.settings)
        var saved: Long? = null
        host(data, "Create playlist") {
            com.tayra.languages.core.ui.state.CollectEvents(viewModel.events) { if (it is PlaylistFormEvent.Saved) saved = it.playlistId }
            val state by viewModel.state.collectAsState()
            if (!state.loading) PlaylistFormContent(state, viewModel, onCancel = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Compras e serviços")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("playlist-name").performTextInput("Morning practice")
        onNodeWithText("16 / 60").assertExists()
        onNodeWithTag("playlist-description").performTextInput("Short Portuguese texts for my commute.")
        onNodeWithTag("cover-2").performClick()
        onNodeWithTag("pick-text-$book").performClick()
        onNodeWithTag("expand-${data.course.id}").performClick()
        onNodeWithTag("pick-lesson-${data.course.id}-pt-test-compras-1").performClick()
        onNodeWithTag("pick-lesson-${data.course.id}-pt-test-compras-3").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("3 selected", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        screenshot("playlist-create")

        // The Courses filter shows the courses alone.
        onNodeWithTag("filter-COURSES").performClick()
        onNodeWithTag("pick-text-$book").assertDoesNotExist()
        onNodeWithTag("form-save").performClick()
        waitUntil(timeoutMillis = 5_000) { saved != null }
        val playlist = runBlocking { data.service.playlist(saved!!)!! }
        assertEquals("Morning practice", playlist.name)
        assertEquals(2, playlist.cover)
        assertEquals(listOf("Uma compra com problema", "O ar-condicionado em promoção", "O vídeo da Júlia"), playlist.items.map { it.title })
    }

    @Test
    fun onAPhoneTheFormIsOneColumn() = runDesktopComposeUiTest(width = 400, height = 1800) {
        data.book("Uma compra com problema", text.repeat(4))
        val viewModel = PlaylistFormViewModel(PlaylistFormMode.Create(null), data.service, data.settings)
        host(data, "Create playlist") {
            val state by viewModel.state.collectAsState()
            if (!state.loading) PlaylistFormContent(state, viewModel, onCancel = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Compras e serviços")).fetchSemanticsNodes().isNotEmpty() }
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
        onNodeWithText("Move down").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { data.service.playlist(morning)!!.items.map { it.id } } == listOf(items[1].id, items[0].id) + items.drop(2).map { it.id } }

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
