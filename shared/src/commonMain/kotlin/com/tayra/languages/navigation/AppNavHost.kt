package com.tayra.languages.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tayra.languages.core.ui.components.ChooseLanguageScreen
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.MoreSheet
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PhoneNavBar
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.feature.books.booksGraph
import com.tayra.languages.feature.courses.coursesGraph
import com.tayra.languages.feature.flashcards.flashcardsGraph
import com.tayra.languages.feature.frequency.frequencyGraph
import com.tayra.languages.feature.home.homeGraph
import com.tayra.languages.feature.languages.languagesGraph
import com.tayra.languages.feature.news.newsGraph
import com.tayra.languages.feature.playlists.PlaybackProblemDialog
import com.tayra.languages.feature.playlists.PlayerBar
import com.tayra.languages.feature.playlists.playlistsGraph
import com.tayra.languages.feature.settings.LanguageSetupDialog
import com.tayra.languages.feature.reading.readingGraph
import com.tayra.languages.feature.settings.settingsGraph
import com.tayra.languages.feature.stats.statsGraph
import com.tayra.languages.feature.terms.termsGraph

@Composable
fun AppNavHost(openCourses: Int = 0) {
    val navController = rememberNavController()
    // A newly chosen language starts on its courses, with only its home page to go back to.
    LaunchedEffect(openCourses) {
        if (openCourses > 0) {
            navController.navigate(Route.Home) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            navController.navigate(Route.Courses)
        }
    }
    // Phones get the main areas as a bottom bar, except while reading; wider screens have them in the top bar.
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val phoneBar = LocalWindowWidth.current.isCompact && destination != null && !destination.hasRoute<Route.Read>() && !destination.hasRoute<Route.Practice>() && !destination.hasRoute<Route.PracticeWords>() &&
        !destination.hasRoute<Route.ChooseLanguage>()
    var more by remember { mutableStateOf(false) }
    // The language whose downloads the playlist player sent for, to get a voice for it.
    var voiceFor by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        bottomBar = {
            // The playlist player's bar stays while anything is loaded, over the phone's tabs.
            Column {
                PlayerBar(onNavigate = { navController.navigate(it) { launchSingleTop = true } }, onPlayerScreen = destination?.hasRoute<Route.Player>() == true)
                if (phoneBar) {
                    PhoneNavBar(
                        section = destination?.let(::sectionOf),
                        // A tab always lands on its own page, with only the start page (Home) left beneath it.
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        onMore = { more = true },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // The screens handle the window insets themselves; only the bar's height is taken from them, and consumed so they do not pad for it twice.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        // Screens change with a quick crossfade, and at once when motion is reduced.
        val enter = if (AppMotion.reduced) EnterTransition.None else fadeIn(AppMotion.spec(AppMotion.medium, AppMotion.enter))
        val exit = if (AppMotion.reduced) ExitTransition.None else fadeOut(AppMotion.spec(AppMotion.short, AppMotion.exit))
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { enter },
            exitTransition = { exit },
            popEnterTransition = { enter },
            popExitTransition = { exit },
        ) {
            homeGraph(navController)
            newsGraph(navController)
            playlistsGraph(navController)
            booksGraph(navController)
            coursesGraph(navController)
            languagesGraph(navController)
            termsGraph(navController)
            flashcardsGraph(navController)
            frequencyGraph(navController)
            readingGraph(navController)
            settingsGraph(navController)
            statsGraph(navController)
            composable<Route.ChooseLanguage> { ChooseLanguageScreen(onBack = { navController.popBackStack() }) }
        }
    }
    PlaybackProblemDialog(onDownloadVoice = { voiceFor = it })
    LanguageSetupDialog(voiceFor, onClosed = { voiceFor = null })
    if (more) MoreSheet(onNavigate = { more = false; navController.navigate(it) }, onDismiss = { more = false })
}

/** The area of the bottom bar a destination belongs to; pages beyond the tabs count as More. */
private fun sectionOf(destination: NavDestination): NavSection = when {
    destination.hasRoute<Route.Home>() -> NavSection.HOME
    destination.hasRoute<Route.Courses>() || destination.hasRoute<Route.Course>() || destination.hasRoute<Route.NewCourse>() ||
        destination.hasRoute<Route.EditCourse>() || destination.hasRoute<Route.NewLesson>() || destination.hasRoute<Route.EditLesson>() -> NavSection.COURSES
    destination.hasRoute<Route.Books>() || destination.hasRoute<Route.NewBook>() || destination.hasRoute<Route.FindBooks>() || destination.hasRoute<Route.EditBook>() ||
        destination.hasRoute<Route.Bookmarks>() || destination.hasRoute<Route.EditPage>() -> NavSection.BOOKS
    destination.hasRoute<Route.News>() -> NavSection.NEWS
    destination.hasRoute<Route.Playlists>() || destination.hasRoute<Route.Playlist>() || destination.hasRoute<Route.NewPlaylist>() ||
        destination.hasRoute<Route.EditPlaylist>() || destination.hasRoute<Route.AddToPlaylist>() || destination.hasRoute<Route.Player>() -> NavSection.PLAYLISTS
    destination.hasRoute<Route.Flashcards>() || destination.hasRoute<Route.BrowseFlashcards>() -> NavSection.FLASHCARDS
    destination.hasRoute<Route.Terms>() || destination.hasRoute<Route.EditTerm>() || destination.hasRoute<Route.EditTermByText>() ||
        destination.hasRoute<Route.NewTerm>() || destination.hasRoute<Route.Examples>() -> NavSection.TERMS
    else -> NavSection.SETTINGS
}
