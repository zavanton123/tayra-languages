package com.tayra.languages.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.feature.books.booksGraph
import com.tayra.languages.feature.languages.languagesGraph
import com.tayra.languages.feature.terms.termsGraph

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Home) {
        booksGraph(navController)
        languagesGraph(navController)
        termsGraph(navController)
    }
}
