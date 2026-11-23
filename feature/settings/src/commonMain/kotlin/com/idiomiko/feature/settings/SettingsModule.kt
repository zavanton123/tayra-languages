package com.idiomiko.feature.settings

import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.StorePurchases
import com.idiomiko.core.domain.account.Subscriptions
import com.idiomiko.core.domain.service.LearningLanguageService
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get()) }
    viewModel { UpdatesViewModel(get(), getOrNull()) }
    viewModel { PersonalSchedulingViewModel(get(), get()) }
    viewModel { RetentionHelpViewModel(get(), get()) }
    viewModel { DictionaryImportViewModel(get()) }
    viewModel { SpeechViewModel(get(), get(), get(), getOrNull(), get(), get()) }
    viewModel { OfflineTranslationViewModel(get(), get(), get(), get()) }
    viewModel { BackupViewModel(get()) }
    viewModel { ClearDataViewModel(get(), get()) }
    viewModel {
        AccountViewModel(
            get(), get(), getOrNull<StorePurchases>()?.let { Subscriptions(it, get()) }, getOrNull<IdentitySignIn>(),
            knownWords = get<LearningLanguageService>().observeKnownWordCounts().map { it.values.sum() },
        )
    }
    viewModel { SyncPromptViewModel(get(), get()) }
    viewModel { AccountBadgeViewModel(get()) }
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    val navigate: (Route) -> Unit = { navController.navigate(it) }
    composable<Route.Settings> { SettingsScreen(onNavigate = navigate) }
    composable<Route.Goals> { GoalsScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Shortcuts> { ShortcutsScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Speech> { SpeechScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.FlashcardSettings> { FlashcardSettingsScreen(onNavigate = navigate, onBack = { navController.popBackStack() }, personal = koinViewModel(), retention = koinViewModel()) }
    composable<Route.OfflineTranslation> { OfflineTranslationScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Backups> {
        BackupScreen(
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            // Screens opened before the restore show data that is gone, so the app starts over from the books.
            onRestored = { navController.navigate(Route.Courses) { popUpTo<Route.Courses> { inclusive = true } } },
        )
    }
    composable<Route.ClearData> {
        ClearDataScreen(
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
            // As after a restore, once the result of the clearing was seen.
            onCleared = { navController.navigate(Route.Courses) { popUpTo<Route.Courses> { inclusive = true } } },
        )
    }
    composable<Route.Account> {
        AccountScreen(
            onNavigate = navigate,
            onBack = { navController.popBackStack() },
        )
    }
    composable<Route.About> { AboutScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.Updates> { UpdatesScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
    composable<Route.ExperimentalFeatures> { ExperimentalFeaturesScreen(onNavigate = navigate, onBack = { navController.popBackStack() }) }
}
