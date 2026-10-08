package com.tayra.languages.navigation

import com.tayra.languages.feature.courses.coursesGraph
import com.tayra.languages.feature.flashcards.flashcardsGraph
import com.tayra.languages.feature.frequency.frequencyGraph
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.feature.books.booksGraph
import com.tayra.languages.feature.languages.languagesGraph
import com.tayra.languages.feature.reading.readingGraph
import com.tayra.languages.feature.settings.settingsGraph
import com.tayra.languages.feature.stats.statsGraph
import com.tayra.languages.feature.terms.termsGraph

@Composable
fun AppNavHost(openCourses: Int = 0) {
    val navController = rememberNavController()
    // A newly chosen language starts on its courses, with nothing of the old one to go back to.
    LaunchedEffect(openCourses) {
        if (openCourses > 0) {
            navController.navigate(Route.Courses) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    NavHost(navController = navController, startDestination = Route.Courses) {
        booksGraph(navController)
        coursesGraph(navController)
        languagesGraph(navController)
        termsGraph(navController)
        flashcardsGraph(navController)
        frequencyGraph(navController)
        readingGraph(navController)
        settingsGraph(navController)
        statsGraph(navController)
    }
}
