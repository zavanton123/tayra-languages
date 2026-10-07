package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.components.ScreenTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route

@Composable
fun AboutScreen(onNavigate: (Route) -> Unit) {
    Scaffold(topBar = { AppTopBar(title = tr("About"), onNavigate = onNavigate, section = NavSection.SETTINGS) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle(tr("About"))
            Text("Tayra Languages", style = MaterialTheme.typography.headlineSmall)
            Text(tr("Learn languages by reading. Import texts, click words to look them up and track what you know."), style = MaterialTheme.typography.bodyLarge)
            Text(tr("A reader for learning languages through texts, running on Android, iOS, desktop and the web with a shared Compose UI."), style = MaterialTheme.typography.bodyMedium)
            Text(tr("The offline English-Russian dictionary is built from the Russian and English Wiktionaries via kaikki.org, licensed CC BY-SA 4.0. Example sentences come from Tatoeba, licensed CC BY 2.0 FR. Word frequency lists come from wordfreq by Robyn Speer and from FrequencyWords by Hermit Dave (OpenSubtitles counts), both licensed CC BY-SA 4.0, and from the Leipzig Corpora Collection (Universität Leipzig)."), style = MaterialTheme.typography.bodySmall)
        }
    }
}
