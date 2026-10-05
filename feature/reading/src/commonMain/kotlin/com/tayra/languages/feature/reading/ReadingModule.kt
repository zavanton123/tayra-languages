package com.tayra.languages.feature.reading

import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.feature.reading.practice.PracticeScreen
import com.tayra.languages.feature.reading.practice.PracticeViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val readingModule = module {
    viewModel { (bookId: Long, page: Int?) -> ReadingViewModel(bookId, page, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), lessonOf = get<CourseService>()::lessonReading) }
    viewModel { (bookId: Long, page: Int) -> PracticeViewModel(bookId, page, get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.readingGraph(navController: NavController) {
    composable<Route.Read> { entry ->
        val route = entry.toRoute<Route.Read>()
        ReadingScreen(
            bookId = route.bookId,
            initialPage = route.page,
            onNavigate = { navController.navigate(it) },
            // A lesson goes back to its course; a book, to the library.
            onHome = { if (!navController.popBackStack<Route.Course>(inclusive = false)) navController.navigate(Route.Home) { popUpTo<Route.Home>() } },
        )
    }
    composable<Route.Practice> { entry ->
        val route = entry.toRoute<Route.Practice>()
        PracticeScreen(bookId = route.bookId, page = route.page, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
}
