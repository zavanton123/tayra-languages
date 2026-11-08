package com.tayra.languages.feature.flashcards

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val flashcardsModule = module {
    viewModel { FlashcardsViewModel(get(), get(), get(), get(), get(), get(), get(), books = get(), sentences = get(), speech = getOrNull()) }
    // A text's words only, in a dialog over the text.
    viewModel(named(TEXT_WORDS)) { (words: List<Long>) -> FlashcardsViewModel(get(), get(), get(), get(), get(), get(), get(), books = get(), sentences = get(), speech = getOrNull(), textWords = words) }
    viewModel { BrowseFlashcardsViewModel(get(), get(), get(), get(), get(), deckExport = get(), sentences = get()) }
}

fun NavGraphBuilder.flashcardsGraph(navController: NavController) {
    composable<Route.Flashcards> { FlashcardsScreen(onNavigate = { navController.navigate(it) }) }
    composable<Route.BrowseFlashcards> { entry ->
        BrowseFlashcardsScreen(termId = entry.toRoute<Route.BrowseFlashcards>().termId, onNavigate = { navController.navigate(it) }, onBack = { navController.popBackStack() })
    }
}

/** The qualifier of the flashcards of a text's words. */
internal const val TEXT_WORDS = "text-words"
