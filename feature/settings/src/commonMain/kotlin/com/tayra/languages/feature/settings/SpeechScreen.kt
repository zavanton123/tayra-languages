package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** The speech engines: which one reads aloud, its runtime, and the voices to download. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechViewModel(
    private val settings: SettingsRepository,
    languages: LanguageRepository,
    val localSpeech: LocalSpeech,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings
    val settingsRepository: SettingsRepository get() = settings

    /** Engines this platform can offer. */
    val engines: List<SpeechEngine> = localSpeech.available

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()
    private val _ready = MutableStateFlow(true)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    private val _failed = MutableStateFlow(false)

    /** True when the status line reports something that went wrong. */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _packages = MutableStateFlow<List<SpeechPackage>>(emptyList())
    val packages: StateFlow<List<SpeechPackage>> = _packages.asStateFlow()
    private val _packageBusy = MutableStateFlow<Set<String>>(emptySet())
    val packageBusy: StateFlow<Set<String>> = _packageBusy.asStateFlow()
    private val _voices = MutableStateFlow<Map<String, List<SpeechVoice>>>(emptyMap())

    /** Usable voices of the chosen engine per language in use. */
    val voices: StateFlow<Map<String, List<SpeechVoice>>> = _voices.asStateFlow()

    /** What the chosen engine is downloading or installing right now. */
    val progress: StateFlow<String?> = state.map { it.speechEngine }
        .flatMapLatest { engine -> localSpeech.find(engine)?.progress ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The languages that have books, which the lists lead with. */
    val languagesInUse: StateFlow<List<LanguageOption>> = languages.observeSummaries()
        .map { summaries ->
            summaries.filter { it.bookCount > 0 }.mapNotNull { s -> LanguageCodes.codeFor(s.name)?.let { LanguageOption(it, s.name) } }.distinctBy { it.code }.sortedBy { it.name }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun engine(): LocalSpeechEngine? = localSpeech.find(state.value.speechEngine)

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun chooseVoice(engine: SpeechEngine, languageCode: String, voiceId: String) =
        update { it.copy(speechVoices = it.speechVoices + ("${engine.name}:$languageCode" to voiceId)) }

    /** Reports the installation of the chosen engine and reloads its packages and voices. */
    fun check() {
        val engine = engine() ?: return
        task { refresh(engine); engine.status() }
    }

    fun install() {
        val engine = engine() ?: return
        _status.value = "Installing ${engine.displayName} into the app folder\u2026"
        task { val summary = engine.setUp(); refresh(engine); summary }
    }

    fun installPackage(pkg: SpeechPackage) = packageTask(pkg) { it.installPackage(pkg.id) }

    fun removePackage(pkg: SpeechPackage) = packageTask(pkg) { it.removePackage(pkg.id) }

    private suspend fun refresh(engine: LocalSpeechEngine) {
        _ready.value = engine.isReady()
        _packages.value = runCatching { engine.packages() }.getOrDefault(emptyList())
        _voices.value = languagesInUse.value.associate { it.code to runCatching { engine.voices(it.code) }.getOrDefault(emptyList()) }
    }

    private fun packageTask(pkg: SpeechPackage, block: suspend (LocalSpeechEngine) -> Unit) {
        val engine = engine() ?: return
        if (pkg.id in _packageBusy.value) return
        _packageBusy.update { it + pkg.id }
        viewModelScope.launch {
            try {
                block(engine)
                refresh(engine)
                _status.value = engine.status()
                _failed.value = false
            } catch (e: Exception) {
                _status.value = "${pkg.title}: ${e.message}"
                _failed.value = true
            }
            _packageBusy.update { it - pkg.id }
        }
    }

    private fun task(block: suspend () -> String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _status.value = try { block().also { _failed.value = false } } catch (e: Exception) { _failed.value = true; e.message ?: "unknown error" }
            _busy.value = false
        }
    }
}

@Composable
fun SpeechScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SpeechViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }
    val engine = viewModel.localSpeech.find(settings.speechEngine)
    LaunchedEffect(settings.speechEngine, inUse) { viewModel.check() }

    Scaffold(topBar = { AppTopBar(title = "Speech", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Engine")
            Dropdown(
                options = viewModel.engines,
                selected = settings.speechEngine.takeIf { it in viewModel.engines } ?: SpeechEngine.SYSTEM,
                onSelect = { chosen -> viewModel.update { it.copy(speechEngine = chosen) } },
                label = "Speech engine",
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "The play buttons in the reader and the speaker buttons on terms use this engine. When it has no voice for a language, the system voice speaks instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (engine == null) {
                Section("System voices")
                Text(
                    "The voices installed in the operating system. They need no download here; more voices and languages are added in the system's own settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Section(engine.displayName)
                Text(engine.description, style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::check, enabled = !busy) { Text(if (busy) "Working..." else "Check installation") }
                    if (engine.hasRuntimeSetup && !ready) Button(onClick = viewModel::install, enabled = !busy) { Text("Install ${engine.displayName}") }
                }
                progress?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                status?.let { text ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else if (ready && !failed) Icon(Icons.Default.CheckCircle, contentDescription = "Working", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        else Icon(Icons.Default.Warning, contentDescription = "Not working", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(text, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (engine.supportsSpeed) {
                    Section("Speed")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Slider(
                            value = settings.speechSpeed,
                            onValueChange = { v -> viewModel.update { it.copy(speechSpeed = (v * 20).toInt() / 20f) } },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${(settings.speechSpeed * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                val choosable = inUse.filter { (voices[it.code]?.size ?: 0) > 0 }
                if (choosable.isNotEmpty()) {
                    Section("Voices")
                    choosable.forEach { language ->
                        val options = voices[language.code].orEmpty()
                        val chosen = settings.speechVoices["${engine.engine.name}:${language.code}"]
                        Dropdown(
                            options = options,
                            selected = options.firstOrNull { it.id == chosen } ?: options.first(),
                            onSelect = { voice -> viewModel.chooseVoice(engine.engine, language.code, voice.id) },
                            label = language.name,
                            optionLabel = { it.name },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (packages.isNotEmpty()) {
                    Section("Downloads")
                    Text(engine.packagesDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val codes = inUse.map { it.code }.toSet()
                    val relevant = packages.filter { it.installed || it.languageCode == null || it.languageCode in codes }
                    val shown = if (showAll || relevant.isEmpty()) packages else relevant
                    shown.groupBy { it.group }.entries.sortedBy { it.key }.forEach { (group, items) ->
                        Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        items.forEach { pkg ->
                            SpeechPackageRow(pkg, busy = pkg.id in packageBusy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) })
                        }
                    }
                    if (relevant.size < packages.size) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "Show only my languages" else "Show all ${packages.size} downloads")
                        }
                    }
                }
            }

            TryIt(viewModel, inUse, voices.filterValues { it.isNotEmpty() }.keys)
        }
    }
}

/** A sentence and a language to hear the chosen engine with. */
@Composable
private fun TryIt(viewModel: SpeechViewModel, languages: List<LanguageOption>, voiced: Set<String>) {
    if (languages.isEmpty()) return
    val speaker = rememberSpeaker(viewModel.localSpeech, viewModel.settingsRepository)
    val working by speaker.working.collectAsStateWithLifecycle()
    val playing by speaker.playing.collectAsStateWithLifecycle()
    // Starts on a language the chosen engine can speak, so Play demonstrates that engine.
    var language by remember(languages, voiced) {
        mutableStateOf(languages.firstOrNull { it.code in voiced } ?: languages.firstOrNull { it.code in SAMPLES } ?: languages.first())
    }
    var text by remember(language) { mutableStateOf(SAMPLES[language.code].orEmpty()) }
    Section("Try it")
    Dropdown(options = languages, selected = language, onSelect = { language = it }, label = "Language", optionLabel = { it.name }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Text to read") },
        placeholder = { Text("Type a sentence in ${language.name}") },
        modifier = Modifier.fillMaxWidth(),
    )
    val active = playing != null && playing == text
    Button(onClick = { speaker.toggle(text, language.code) }, enabled = text.isNotBlank()) {
        Icon(if (active) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            when {
                active && working -> "  Preparing..."
                active -> "  Stop"
                else -> "  Play"
            },
        )
    }
}

@Composable
private fun SpeechPackageRow(pkg: SpeechPackage, busy: Boolean, onInstall: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(pkg.title, style = MaterialTheme.typography.bodyMedium)
            val size = if (pkg.sizeBytes > 0) formatSize(pkg.sizeBytes) else ""
            val detail = when {
                busy && pkg.installed -> "Removing..."
                busy -> "Downloading..."
                pkg.installed -> listOf("Installed", size).filter { it.isNotEmpty() }.joinToString(", ")
                else -> listOf("Not installed", size).filter { it.isNotEmpty() }.joinToString(", ")
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        when {
            busy -> {}
            pkg.installed -> OutlinedButton(onClick = onRemove) { Text("Remove") }
            else -> Button(onClick = onInstall) { Text("Install") }
        }
    }
}

/** A short sentence per language so the Try it field is never empty for the common ones. */
private val SAMPLES: Map<String, String> = mapOf(
    "en" to "Reading a little every day is the best way to learn.",
    "de" to "Jeden Tag ein wenig zu lesen ist der beste Weg zu lernen.",
    "es" to "Leer un poco cada día es la mejor manera de aprender.",
    "fr" to "Lire un peu chaque jour est la meilleure façon d'apprendre.",
    "it" to "Leggere un po' ogni giorno è il modo migliore per imparare.",
    "pt" to "Ler um pouco todos os dias é a melhor maneira de aprender.",
    "ru" to "Читать понемногу каждый день \u2014 лучший способ учиться.",
    "pl" to "Czytanie po trochu każdego dnia to najlepszy sposób na naukę.",
    "nl" to "Elke dag een beetje lezen is de beste manier om te leren.",
    "sv" to "Att läsa lite varje dag är det bästa sättet att lära sig.",
    "uk" to "Читати потроху щодня \u2014 найкращий спосіб навчатися.",
    "cs" to "Číst každý den trochu je nejlepší způsob, jak se učit.",
)
