package com.tayra.languages.feature.languages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.LanguageSummary
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/** Lists the catalog languages; only their dictionaries and text settings can be changed. */
@Composable
fun LanguagesScreen(onNavigate: (Route) -> Unit, viewModel: LanguagesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = "Languages", onNavigate = onNavigate, section = NavSection.SETTINGS) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Languages you can learn. Tap one to change its dictionaries and text settings.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.loading) {
                LoadingIndicator()
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.languages, key = { it.id }) { language ->
                        LanguageRow(language = language, onEdit = { onNavigate(Route.EditLanguage(language.id)) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(language: LanguageSummary, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(language.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${language.bookCount} books, ${language.termCount} terms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
    }
}
