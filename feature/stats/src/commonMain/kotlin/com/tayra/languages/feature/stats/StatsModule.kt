package com.tayra.languages.feature.stats

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val statsModule = module {
    viewModel { StatsViewModel(get(), get(), get()) }
    viewModel { FlashcardStatsViewModel(get(), get(), get()) }
}

fun NavGraphBuilder.statsGraph(navController: NavController) {
    composable<Route.Stats> { StatsScreen(onNavigate = { navController.navigate(it) }) }
    composable<Route.FlashcardStats> { FlashcardStatsScreen(onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() }) }
}
