package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppThemes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val dictionaries: DictionaryService,
    languages: LanguageRepository,
    private val localTranslation: LocalTranslation,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings
    val packs: StateFlow<List<PackStatus>> = dictionaries.packs

    /** Whether this platform has a local (Argos) translator at all. */
    val hasLocalTranslator: Boolean get() = localTranslation.translator != null

    /** Source-language codes of the languages that have books, so the pack list can lead with them. */
    val languagesInUse: StateFlow<Set<String>> = languages.observeSummaries()
        .map { summaries -> summaries.filter { it.bookCount > 0 }.mapNotNull { LanguageCodes.codeFor(it.name) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { settings.update(transform) }
    fun download(pack: DictionaryPack) = viewModelScope.launch { dictionaries.download(pack) }
    fun remove(pack: DictionaryPack) = viewModelScope.launch { dictionaries.remove(pack) }
}

@Composable
fun SettingsScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var showAllPacks by remember { mutableStateOf(false) }
    Scaffold(topBar = { AppTopBar(title = "Settings", onNavigate = onNavigate, section = NavSection.SETTINGS) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Appearance")
            Dropdown(
                options = AppThemes.all,
                selected = AppThemes.byId(settings.themeId),
                onSelect = { theme -> viewModel.update { it.copy(themeId = theme.id) } },
                label = "Theme",
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow("Highlight terms by status", settings.showHighlights) { v -> viewModel.update { it.copy(showHighlights = v) } }
            Text("Reading font size: ${(settings.readingFontScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.readingFontScale, onValueChange = { v -> viewModel.update { it.copy(readingFontScale = v) } }, valueRange = 0.6f..2.5f)
            Text("Reading line height: ${(settings.readingLineHeight * 10).toInt() / 10f}", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.readingLineHeight, onValueChange = { v -> viewModel.update { it.copy(readingLineHeight = v) } }, valueRange = 1.0f..3.0f)

            Section("Behaviour")
            SwitchRow("Show reading streak on home page", settings.showStreakOnHome) { v -> viewModel.update { it.copy(showStreakOnHome = v) } }
            SwitchRow("Quick set status: tapping an unknown word sets status 1", settings.tapSetsStatus) { v -> viewModel.update { it.copy(tapSetsStatus = v) } }
            OutlinedTextField(
                value = settings.statsSampleSize.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.update { it.copy(statsSampleSize = n.coerceIn(UserSettings.MIN_STATS_SAMPLE_SIZE, UserSettings.MAX_STATS_SAMPLE_SIZE)) } } },
                label = { Text("Book stats page sample size") },
                supportingText = { Text("Number of pages used for book statistics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Translation")
            Dropdown(
                options = LanguageCatalog.nativeLanguages,
                selected = LanguageCatalog.nativeOption(settings.nativeLanguage),
                onSelect = { option -> viewModel.update { it.copy(nativeLanguage = option.code) } },
                label = "Native language",
                optionLabel = { it.name },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Translation suggestions and example sentence translations are shown in this language. With a downloaded dictionary for the text's language and this one, lookups work offline; otherwise English uses Wiktionary with MyMemory as fallback and other languages use MyMemory.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = settings.translationContactEmail,
                onValueChange = { v -> viewModel.update { it.copy(translationContactEmail = v.trim()) } },
                label = { Text("MyMemory contact email (optional)") },
                supportingText = { Text("Raises the free daily quota from about 5,000 to 50,000 characters.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (viewModel.hasLocalTranslator) {
                Section("Sentence translation engine")
                Dropdown(
                    options = TranslationEngine.entries,
                    selected = settings.translationEngine,
                    onSelect = { engine -> viewModel.update { it.copy(translationEngine = engine) } },
                    label = "Engine",
                    optionLabel = { it.label },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Argos Translate runs on this computer with no network. Its runtime and language packages are managed on their own screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { onNavigate(Route.OfflineTranslation) }) { Text("Translation settings") }
            }

            Section("Offline dictionaries")
            Text(
                "Downloaded dictionaries translate words without a network connection and link inflected forms to their base word.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Packs for languages with books, or already on the device, come first; the rest hide behind a toggle.
            val relevant = packs.filter { it.pack.id.sourceLanguage in inUse || it.state !is PackState.NotInstalled }
            val shown = if (showAllPacks || relevant.isEmpty()) packs else relevant
            shown.forEach { status -> PackRow(status, onDownload = { viewModel.download(status.pack) }, onRemove = { viewModel.remove(status.pack) }) }
            if (relevant.size < packs.size) {
                TextButton(onClick = { showAllPacks = !showAllPacks }) {
                    Text(if (showAllPacks) "Show only my languages" else "Show all ${packs.size} dictionaries")
                }
            }

            Section("Term popups")
            SwitchRow("Promote parent translation to term translation if possible", settings.promoteParentTranslation) { v -> viewModel.update { it.copy(promoteParentTranslation = v) } }
            SwitchRow("Show component terms", settings.showComponents) { v -> viewModel.update { it.copy(showComponents = v) } }
        }
    }
}

@Composable
internal fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PackRow(status: PackStatus, onDownload: () -> Unit, onRemove: () -> Unit) {
    val state = status.state
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(status.pack.title, style = MaterialTheme.typography.bodyMedium)
            val detail = when (state) {
                PackState.NotInstalled -> "Not downloaded"
                is PackState.Downloading -> "Downloading..."
                is PackState.Installed -> "Installed, ${formatSize(state.sizeBytes)}"
                is PackState.Failed -> "Download failed: ${state.message}"
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (state is PackState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state is PackState.Downloading) {
                val progress = state.progress
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
        when (state) {
            is PackState.Installed -> OutlinedButton(onClick = onRemove) { Text("Remove") }
            is PackState.Downloading -> {}
            else -> Button(onClick = onDownload) { Text("Download") }
        }
    }
}

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "${(bytes / 100_000) / 10.0} MB"
    bytes >= 1_000 -> "${bytes / 1_000} kB"
    else -> "$bytes B"
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
