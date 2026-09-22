package com.tayra.languages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.bootstrap.AppBootstrapViewModel
import com.tayra.languages.bootstrap.BootstrapState
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.navigation.AppNavHost
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    KoinContext {
        val settingsRepository = koinInject<SettingsRepository>()
        val settings by settingsRepository.settings.collectAsStateWithLifecycle()
        TayraTheme(AppThemes.byId(settings.themeId)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ProvideWindowWidth {
                    val bootstrap = koinViewModel<AppBootstrapViewModel>()
                    val state by bootstrap.state.collectAsStateWithLifecycle()
                    when (val s = state) {
                        BootstrapState.Loading -> LoadingIndicator()
                        is BootstrapState.Failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Could not start: ${s.message}", color = MaterialTheme.colorScheme.error)
                        }
                        BootstrapState.Ready -> AppNavHost()
                    }
                }
            }
        }
    }
}
