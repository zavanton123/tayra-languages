package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Standalone term editor (from the term listing). */
@Composable
fun TermEditScreen(
    key: TermFormKey,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenParent: (languageId: Long, text: String) -> Unit,
) {
    val viewModel = koinViewModel<TermFormViewModel>(key = "term-form-$key") { parametersOf(key) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> if (!event.keepOpen) onDone()
            TermFormEvent.Deleted -> onDone()
            is TermFormEvent.OpenParent -> onOpenParent(event.languageId, event.text)
        }
    }
    val isNew = key is TermFormKey.New
    val compact = LocalWindowWidth.current.isCompact
    val gutter = if (compact) 16.dp else 32.dp
    Scaffold(topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PageHeader(
                title = if (isNew) "New term" else "Edit term",
                subtitle = if (isNew) "Add a word or phrase to your vocabulary." else "Update the term, translation, and learning status.",
                compact = compact,
                gutter = gutter,
                saving = state.saving,
                canSave = state.draft.text.isNotBlank(),
                onBack = onBack,
                onTerms = { onNavigate(Route.Terms()) },
                onSave = viewModel::save,
            )
            TermFormPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f).padding(horizontal = gutter - 16.dp),
                onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
                onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
            )
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    compact: Boolean,
    gutter: androidx.compose.ui.unit.Dp,
    saving: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!compact) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Text("Terms", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.clickable(onClick = onTerms))
            Text("/", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onSave, enabled = canSave && !saving, shape = RoundedCornerShape(10.dp)) { Text(if (saving) "Saving..." else "Save changes") }
        }
    }
}
