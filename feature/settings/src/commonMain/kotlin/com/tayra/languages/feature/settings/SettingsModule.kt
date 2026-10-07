package com.tayra.languages.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tayra.languages.core.ui.navigation.Route
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), getOrNull()) }
    viewModel { DictionariesViewModel(get(), get(), get()) }
    viewModel { CoursePacksViewModel(get()) }
    viewModel { SpeechViewModel(get(), get(), get()) }
    viewModel { OfflineTranslationViewModel(get(), get(), get(), get()) }
    viewModel { BackupViewModel(get()) }
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Settings> { SettingsScreen(onNavigate = navigate) }
    composable<Route.Shortcuts> { ShortcutsScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Speech> { SpeechScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.FlashcardSettings> { FlashcardSettingsScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.OfflineDictionaries> { DictionariesScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.CoursePacks> { CoursePacksScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.OfflineTranslation> { OfflineTranslationScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Backups> {
        BackupScreen(
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            // Screens opened before the restore show data that is gone, so the app starts over from the books.
            onRestored = { navController.navigate(Route.Courses) { popUpTo<Route.Courses> { inclusive = true } } },
        )
    }
    composable<Route.About> { AboutScreen(onNavigate = navigate) }
}
