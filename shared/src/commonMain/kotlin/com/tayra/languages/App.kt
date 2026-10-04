package com.tayra.languages

import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.ui.components.LocalFlashcardsDue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.bootstrap.AppBootstrapViewModel
import com.tayra.languages.bootstrap.BootstrapState
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.navigation.AppNavHost
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Gives every screen's top bar the choice of the language being learned, and picks a language
 * again whenever the chosen one is gone (deleted, or the database was reset).
 */
@Composable
private fun ProvideLearningLanguage(currentId: Long, content: @Composable () -> Unit) {
    val languages by koinInject<LanguageRepository>().observeAll().collectAsStateWithLifecycle(emptyList())
    val learning = koinInject<LearningLanguageService>()
    val scope = rememberCoroutineScope()
    LaunchedEffect(currentId, languages) {
        if (languages.isNotEmpty() && languages.none { it.id == currentId }) learning.ensure()
    }
    val state = remember(languages, currentId) {
        LearningLanguageState(languages.sortedBy { it.name }.map { it.id to it.name }, currentId) { id -> scope.launch { learning.select(id) } }
    }
    CompositionLocalProvider(LocalLearningLanguage provides state, content = content)
}

/**
 * @param titleBarInset height of a transparent native title bar the content extends under
 *   (macOS desktop); painted in the app bar colour so it matches the theme.
 */
@Composable
fun App(titleBarInset: Dp = 0.dp) {
    KoinContext {
        val settingsRepository = koinInject<SettingsRepository>()
        val settings by settingsRepository.settings.collectAsStateWithLifecycle()
        TayraTheme(AppThemes.byId(settings.themeId)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column {
                    if (titleBarInset > 0.dp) {
                        Box(Modifier.fillMaxWidth().height(titleBarInset).background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                    ProvideWindowWidth {
                    val bootstrap = koinViewModel<AppBootstrapViewModel>()
                    val state by bootstrap.state.collectAsStateWithLifecycle()
                    when (val s = state) {
                        BootstrapState.Loading -> LoadingIndicator()
                        is BootstrapState.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Could not start: ${s.message}", color = MaterialTheme.colorScheme.error)
                        }
                        BootstrapState.Ready -> ProvideLearningLanguage(settings.currentLanguageId) {
                            val flashcards = koinInject<FlashcardService>()
                            val due by remember(flashcards) { flashcards.observeDueCount() }.collectAsStateWithLifecycle(0)
                            CompositionLocalProvider(LocalFlashcardsDue provides due) { AppNavHost() }
                        }
                    }
                    }
                }
            }
        }
    }
}
