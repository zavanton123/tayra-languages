package com.tayra.languages.feature.frequency

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.WordFrequencyService
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val frequencyModule = module {
    single<FrequencyLists> { ResourceFrequencyLists() }
    single { WordFrequencyService(get(), get(), get()) }
    viewModel { WordFrequencyViewModel(get(), get(), get(), get()) }
}

fun NavGraphBuilder.frequencyGraph(navController: NavController) {
    composable<Route.WordFrequency> { WordFrequencyScreen(onNavigate = { navController.navigate(it) }) }
}
