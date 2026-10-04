package com.tayra.languages.feature.flashcards

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val flashcardsModule = module {
    viewModel { FlashcardsViewModel(get(), get(), get(), get(), get(), get()) }
}

fun NavGraphBuilder.flashcardsGraph(navController: NavController) {
    composable<Route.Flashcards> { FlashcardsScreen(onNavigate = { navController.navigate(it) }) }
}
