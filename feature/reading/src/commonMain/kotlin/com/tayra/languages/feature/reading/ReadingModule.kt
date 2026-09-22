package com.tayra.languages.feature.reading

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val readingModule = module {
    viewModel { (bookId: Long, page: Int?) -> ReadingViewModel(bookId, page, get(), get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.readingGraph(navController: NavController) {
    composable<Route.Read> { entry ->
        val route = entry.toRoute<Route.Read>()
        ReadingScreen(
            bookId = route.bookId,
            initialPage = route.page,
            onNavigate = { navController.navigate(it) },
            onHome = { navController.navigate(Route.Home) { popUpTo<Route.Home>() } },
        )
    }
}
