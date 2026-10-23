package com.tayra.languages.feature.playlists

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.PlaylistSource
import com.tayra.languages.core.domain.playlists.SpeechOutput
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.scope.Scope
import org.koin.dsl.module

/** The device's voice and audio player for the playlist player, made apart from any screen. */
internal expect fun Scope.deviceSpeechOutput(): SpeechOutput

val playlistsModule = module {
    single<SpeechOutput> { deviceSpeechOutput() }
    // The app's player: it outlives every screen, so it has a scope of its own on the main thread.
    single { PlaylistPlayer(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Main)) }
    single { NowPlayingFeed(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Main)) }
    // Made with the app, so the system's controls follow the player from the start.
    single(createdAtStart = true) { systemMediaControls() }
    viewModel { PlaylistsViewModel(get(), get(), get(), get(), get()) }
    viewModel { (playlistId: Long) -> PlaylistDetailViewModel(playlistId, get(), get(), get(), get()) }
    viewModel { (mode: PlaylistFormMode) -> PlaylistFormViewModel(mode, get(), get()) }
    viewModel { (source: PlaylistSource) -> AddToPlaylistViewModel(source, get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.playlistsGraph(navController: NavController) {
    composable<Route.Playlists> { PlaylistsScreen(onNavigate = { navController.navigate(it) }) }
    composable<Route.Playlist> { entry ->
        val route = entry.toRoute<Route.Playlist>()
        PlaylistDetailScreen(route.playlistId, onNavigate = { navController.navigate(it) }, onBack = { navController.backTo(Route.Playlists) })
    }
    composable<Route.Player> { PlayerScreen(onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() }) }
    composable<Route.EditPlaylist> { entry ->
        val route = entry.toRoute<Route.EditPlaylist>()
        PlaylistFormScreen(PlaylistFormMode.Edit(route.playlistId), onNavigate = { navController.navigate(it) }, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
    }
    composable<Route.AddToPlaylist> { entry ->
        val route = entry.toRoute<Route.AddToPlaylist>()
        PlaylistFormScreen(PlaylistFormMode.AddTexts(route.playlistId), onNavigate = { navController.navigate(it) }, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
    }
}

/** Goes back to [route] when it is on the back stack, and opens it otherwise. */
private fun NavController.backTo(route: Route) {
    if (!popBackStack(route, inclusive = false)) navigate(route) { popUpTo(route) { inclusive = true } }
}
