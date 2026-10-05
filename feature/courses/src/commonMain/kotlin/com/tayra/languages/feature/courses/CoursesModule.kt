package com.tayra.languages.feature.courses

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coursesModule = module {
    viewModel { CoursesViewModel(get(), get(), get()) }
    viewModel { (courseId: String) -> CourseViewModel(courseId, get()) }
}

fun NavGraphBuilder.coursesGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Courses> { CoursesScreen(onNavigate = navigate) }
    composable<Route.Course> { entry ->
        val route = entry.toRoute<Route.Course>()
        CourseScreen(courseId = route.courseId, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
}
