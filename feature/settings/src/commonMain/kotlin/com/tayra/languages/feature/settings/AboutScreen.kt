package com.tayra.languages.feature.settings

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
import com.tayra.languages.core.ui.navigation.Route

@Composable
fun AboutScreen(onNavigate: (Route) -> Unit) {
    Scaffold(topBar = { AppTopBar(title = "About", onNavigate = onNavigate) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tayra Languages", style = MaterialTheme.typography.headlineSmall)
            Text("Learn languages by reading. Import texts, click words to look them up and track what you know.", style = MaterialTheme.typography.bodyLarge)
            Text("A Kotlin Multiplatform port of Lute (Learning Using Texts), running on Android, iOS, desktop and the web with a shared Compose UI.", style = MaterialTheme.typography.bodyMedium)
            Text("Language definitions and sample texts come from the Lute language definitions project.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
