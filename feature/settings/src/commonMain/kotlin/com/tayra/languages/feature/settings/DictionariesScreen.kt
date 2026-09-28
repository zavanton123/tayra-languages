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
import androidx.compose.material3.Scaffold
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
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** The downloadable offline dictionary packs and their state on this device. */
class DictionariesViewModel(
    settings: SettingsRepository,
    private val dictionaries: DictionaryService,
    languages: LanguageRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settings.settings
    val packs: StateFlow<List<PackStatus>> = dictionaries.packs

    /** Source-language codes of the languages that have books, so the list can lead with them. */
    val languagesInUse: StateFlow<Set<String>> = languages.observeSummaries()
        .map { summaries -> summaries.filter { it.bookCount > 0 }.mapNotNull { LanguageCodes.codeFor(it.name) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun download(pack: DictionaryPack) = viewModelScope.launch { dictionaries.download(pack) }
    fun remove(pack: DictionaryPack) = viewModelScope.launch { dictionaries.remove(pack) }
}

@Composable
fun DictionariesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: DictionariesViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar(title = "Dictionaries", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Downloaded dictionaries translate words without a network connection and link inflected forms to their base word. One pack covers one language, with meanings in one language.",
                style = MaterialTheme.typography.bodyMedium,
            )
            // Packs for languages with books, or already on the device, come first; the rest hide behind a toggle.
            val relevant = packs.filter { it.pack.id.sourceLanguage in inUse || it.state !is PackState.NotInstalled }
            val shown = if (showAll || relevant.isEmpty()) packs else relevant
            // Grouped by the language the meanings are in, the native language first.
            val native = settings.nativeLanguage.ifBlank { "en" }
            val groups = shown.groupBy { it.pack.id.targetLanguage }.entries
                .sortedWith(compareBy({ it.key != native }, { targetName(it.value.first().pack) }))
            groups.forEach { (_, group) ->
                Text(
                    targetName(group.first().pack),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                group.sortedBy { it.pack.title }.forEach { status ->
                    PackRow(status, onDownload = { viewModel.download(status.pack) }, onRemove = { viewModel.remove(status.pack) })
                }
            }
            if (relevant.size < packs.size) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Show only my languages" else "Show all ${packs.size} dictionaries")
                }
            }
        }
    }
}

/** The gloss language of a pack, from its "Source → Target" title. */
private fun targetName(pack: DictionaryPack): String = pack.title.substringAfter("\u2192 ", pack.id.targetLanguage)

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
