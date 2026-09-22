package com.tayra.languages.feature.languages

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val languagesModule = module {
    viewModel { LanguagesViewModel(get(), get()) }
    viewModel { PredefinedLanguagesViewModel(get()) }
    viewModel { (languageId: Long?, predefinedName: String?) -> LanguageEditViewModel(languageId, predefinedName, get(), get()) }
}

fun NavGraphBuilder.languagesGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Languages> {
        LanguagesScreen(onNavigate = navigate)
    }
    composable<Route.PredefinedLanguages> {
        PredefinedLanguagesScreen(
            onNavigate = { route -> navController.navigate(route) { popUpTo<Route.Home>() } },
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.EditLanguage> { entry ->
        val route = entry.toRoute<Route.EditLanguage>()
        LanguageEditScreen(
            languageId = route.languageId,
            predefinedName = null,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
    composable<Route.NewLanguage> { entry ->
        val route = entry.toRoute<Route.NewLanguage>()
        LanguageEditScreen(
            languageId = null,
            predefinedName = route.predefinedName,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { navController.navigate(Route.Home) { popUpTo<Route.Home>() } },
        )
    }
}
