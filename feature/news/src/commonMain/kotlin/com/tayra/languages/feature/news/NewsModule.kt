package com.tayra.languages.feature.news

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val newsModule = module {
    viewModel { NewsViewModel(get(), get(), get()) }
}

fun NavGraphBuilder.newsGraph(navController: NavController) {
    composable<Route.News> { NewsScreen(onNavigate = { navController.navigate(it) }) }
}
