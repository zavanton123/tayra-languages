package com.tayra.languages.feature.reading

import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.feature.reading.practice.PracticeScreen
import com.tayra.languages.feature.reading.practice.PracticeSource
import com.tayra.languages.feature.reading.practice.PracticeViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val readingModule = module {
    viewModel { (bookId: Long, page: Int?) -> ReadingViewModel(bookId, page, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), lessonOf = get<CourseService>()::lessonReading, wordReadings = getOrNull(), chineseScripts = getOrNull()) }
    viewModel { (source: PracticeSource, kinds: Set<ExerciseKind>) -> PracticeViewModel(source, kinds, get(), get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.readingGraph(navController: NavController) {
    composable<Route.Read> { entry ->
        val route = entry.toRoute<Route.Read>()
        ReadingScreen(
            bookId = route.bookId,
            initialPage = route.page,
            onNavigate = { navController.navigate(it) },
            onHome = { navController.navigate(Route.Books) { popUpTo<Route.Courses>() } },
            // Back to the course the lesson was opened from, or to the course itself when the lesson was opened straight from the course list.
            onCourse = { courseId -> if (!navController.popBackStack(Route.Course(courseId), inclusive = false)) navController.navigate(Route.Course(courseId)) { popUpTo<Route.Courses>() } },
        )
    }
    composable<Route.Practice> { entry ->
        val route = entry.toRoute<Route.Practice>()
        val kinds = route.kinds.mapNotNull { name -> ExerciseKind.entries.firstOrNull { it.name == name } }.toSet().ifEmpty { PracticeBuilder.KINDS.toSet() }
        val source = route.page?.let { PracticeSource.Page(route.bookId, it) } ?: PracticeSource.Book(route.bookId)
        PracticeScreen(source, kinds, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
    composable<Route.PracticeWords> { entry ->
        val route = entry.toRoute<Route.PracticeWords>()
        val kinds = route.kinds.mapNotNull { name -> ExerciseKind.entries.firstOrNull { it.name == name } }.toSet().ifEmpty { PracticeBuilder.KINDS.toSet() }
        PracticeScreen(PracticeSource.Words(route.termIds), kinds, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
}
