package com.tayra.languages.feature.languages

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val languagesModule = module {
    viewModel { LanguagesViewModel(get()) }
    viewModel { (languageId: Long) -> LanguageEditViewModel(languageId, get(), get()) }
    viewModel { (languageId: Long) -> ManageDictionariesViewModel(languageId, get(), get(), get()) }
}

fun NavGraphBuilder.languagesGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Languages> {
        LanguagesScreen(onNavigate = navigate)
    }
    composable<Route.ManageDictionaries> { entry ->
        val route = entry.toRoute<Route.ManageDictionaries>()
        ManageDictionariesScreen(languageId = route.languageId, onNavigate = navigate, onBack = { navController.popBackStack() })
    }
    composable<Route.EditLanguage> { entry ->
        val route = entry.toRoute<Route.EditLanguage>()
        LanguageEditScreen(
            languageId = route.languageId,
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
}
