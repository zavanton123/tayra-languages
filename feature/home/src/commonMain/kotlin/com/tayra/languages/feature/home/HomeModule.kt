package com.tayra.languages.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable<Route.Home> { HomeScreen(onNavigate = { navController.navigate(it) }) }
}
