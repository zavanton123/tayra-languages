package com.tayra.languages.feature.videos

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val videosModule = module {
    viewModel { VideosViewModel(get(), get(), get()) }
    viewModel { AddVideoViewModel(get(), get(), get(), get()) }
    viewModel { (bookId: Long) -> WatchViewModel(bookId, get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.videosGraph(navController: NavController) {
    composable<Route.Videos> { VideosScreen(onNavigate = { navController.navigate(it) }) }
    composable<Route.NewVideo> {
        AddVideoScreen(
            onNavigate = { navController.navigate(it) },
            onBack = { navController.popBackStack() },
            onAdded = { id -> navController.navigate(Route.Watch(id)) { popUpTo<Route.NewVideo> { inclusive = true } } },
        )
    }
    composable<Route.Watch> { entry ->
        val route = entry.toRoute<Route.Watch>()
        WatchScreen(route.bookId, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
}
