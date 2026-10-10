package com.tayra.languages.feature.courses

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coursesModule = module {
    viewModel { CoursesViewModel(get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseViewModel(courseId, get()) }
    viewModel { (courseId: String?) -> CourseFormViewModel(courseId, get(), get(), get()) }
    viewModel { (courseId: String, lessonId: String?) -> LessonFormViewModel(courseId, lessonId, get(), get()) }
}

fun NavGraphBuilder.coursesGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Courses> { CoursesScreen(onNavigate = navigate) }
    composable<Route.Course> { entry ->
        val route = entry.toRoute<Route.Course>()
        CourseScreen(courseId = route.courseId, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
    composable<Route.NewCourse> {
        CourseFormScreen(
            courseId = null,
            onNavigate = navigate,
            // The new course's page takes the form's place, so going back from it returns to the list.
            onSaved = { id -> navController.navigate(Route.Course(id)) { popUpTo<Route.NewCourse> { inclusive = true } } },
            onCancel = { navController.popBackStack() },
        )
    }
    composable<Route.EditCourse> { entry ->
        val route = entry.toRoute<Route.EditCourse>()
        CourseFormScreen(courseId = route.courseId, onNavigate = navigate, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
    }
    composable<Route.NewLesson> { entry ->
        val route = entry.toRoute<Route.NewLesson>()
        LessonFormScreen(courseId = route.courseId, lessonId = null, onNavigate = navigate, onDone = { navController.popBackStack() })
    }
    composable<Route.EditLesson> { entry ->
        val route = entry.toRoute<Route.EditLesson>()
        LessonFormScreen(courseId = route.courseId, lessonId = route.lessonId, onNavigate = navigate, onDone = { navController.popBackStack() })
    }
}
