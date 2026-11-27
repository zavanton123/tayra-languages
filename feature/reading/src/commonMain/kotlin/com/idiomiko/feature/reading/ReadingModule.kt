package com.idiomiko.feature.reading

import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.repository.BookRepository
import com.idiomiko.feature.reading.practice.PracticeScreen
import com.idiomiko.core.domain.practice.PracticeSource
import com.idiomiko.feature.reading.practice.practiceOf
import com.idiomiko.feature.reading.practice.PracticeViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.idiomiko.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.idiomiko.core.domain.practice.PracticeSetup
import com.idiomiko.core.domain.practice.PracticeCompletions

val readingModule = module {
    viewModel { (bookId: Long, page: Int?) -> ReadingViewModel(bookId, page, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), lessonOf = get<CourseService>()::lessonReading, lessonSummaries = getOrNull(), openLesson = get<CourseService>().let { courses -> { course: String, lesson: String -> courses.openLesson(course, lesson) } }, isNews = get<BookRepository>().let { books -> { id: Long -> books.newsUrl(id) != null } }, wordReadings = getOrNull(), chineseScripts = getOrNull(), anki = getOrNull(), ankiConnection = getOrNull(), practiceCompletions = getOrNull()) }
    viewModel { (source: PracticeSource, setup: PracticeSetup) -> PracticeViewModel(source, setup, get(), get(), get(), get(), getOrNull(), getOrNull(), getOrNull(), completions = getOrNull()) }
    single { PracticeCompletions() }
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
