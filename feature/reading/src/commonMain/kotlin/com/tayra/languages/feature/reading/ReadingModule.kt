package com.tayra.languages.feature.reading

import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.feature.reading.practice.PracticeScreen
import com.tayra.languages.core.domain.practice.PracticeSource
import com.tayra.languages.feature.reading.practice.practiceOf
import com.tayra.languages.feature.reading.practice.PracticeViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.tayra.languages.core.domain.practice.PracticeSetup

val readingModule = module {
    viewModel { (bookId: Long, page: Int?) -> ReadingViewModel(bookId, page, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), lessonOf = get<CourseService>()::lessonReading, isNews = get<BookRepository>().let { books -> { id: Long -> books.newsUrl(id) != null } }, wordReadings = getOrNull(), chineseScripts = getOrNull(), anki = getOrNull(), ankiConnection = getOrNull()) }
    viewModel { (source: PracticeSource, setup: PracticeSetup) -> PracticeViewModel(source, setup, get(), get(), get(), get()) }
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
            // Back to the news the article was opened from.
            onNews = { if (!navController.popBackStack(Route.News, inclusive = false)) navController.navigate(Route.News) },
        )
    }
    composable<Route.Practice> { entry ->
        val (source, setup) = practiceOf(entry.toRoute<Route.Practice>())!!
        PracticeScreen(source, setup, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
    composable<Route.PracticeWords> { entry ->
        val (source, setup) = practiceOf(entry.toRoute<Route.PracticeWords>())!!
        PracticeScreen(source, setup, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
}
