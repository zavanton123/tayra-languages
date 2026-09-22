package com.tayra.languages.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get()) }
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Settings> { SettingsScreen(onNavigate = navigate) }
    composable<Route.Shortcuts> { ShortcutsScreen(onNavigate = navigate) }
    composable<Route.About> { AboutScreen(onNavigate = navigate) }
}
