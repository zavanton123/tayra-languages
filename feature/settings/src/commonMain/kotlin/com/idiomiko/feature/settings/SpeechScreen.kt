package com.idiomiko.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.language.LanguageOption
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.service.LanguageAssets
import com.idiomiko.core.domain.service.LanguageSetupService
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalSpeechEngine
import com.idiomiko.core.domain.service.SpeechEngine
import com.idiomiko.core.domain.service.SetupItem
import com.idiomiko.core.domain.service.SetupKind
import com.idiomiko.core.domain.service.SetupState
import com.idiomiko.core.domain.service.SetupStatus
import com.idiomiko.core.domain.service.SpeechPackage
import com.idiomiko.core.domain.service.SpeechVoice
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.idiomiko.core.domain.practice.SpeechRecognitionSetup
import com.idiomiko.core.domain.practice.RecognitionModel
import com.idiomiko.core.domain.practice.RecognitionQuality

/**
 * The speech engines: which one reads aloud, its runtime, and the voices and speech recognition
 * models of the language being learned, downloaded and removed in place through [setup].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechViewModel(
    private val settings: SettingsRepository,
    languages: LanguageRepository,
    val localSpeech: LocalSpeech,
    /** The recogniser to set up for speaking practice, where it needs setting up (the desktop). */
    val recognition: SpeechRecognitionSetup? = null,
    private val setup: LanguageSetupService? = null,
    /** What the app keeps on its own, which the page offers no Remove for: the last voice. */
    private val assets: LanguageAssets? = null,
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
    private val _voices = MutableStateFlow<Map<String, List<SpeechVoice>>>(emptyMap())

    /** Usable voices of the chosen engine per language in use. */
    val voices: StateFlow<Map<String, List<SpeechVoice>>> = _voices.asStateFlow()

    /** What the chosen engine is downloading or installing right now. */
    val progress: StateFlow<String?> = state.map { it.speechEngine }
        .flatMapLatest { engine -> localSpeech.find(engine)?.progress ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The language being learned, which the lists lead with. */
    val languagesInUse: StateFlow<List<LanguageOption>> = learningLanguage(languages, settings)
        .map { listOfNotNull(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun engine(): LocalSpeechEngine? = localSpeech.find(state.value.speechEngine)

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun chooseVoice(engine: SpeechEngine, languageCode: String, voiceId: String) =
        update { it.copy(speechVoices = it.speechVoices + ("${engine.name}:$languageCode" to voiceId)) }

    private val _broken = MutableStateFlow(false)

    /** True when the last check found the engine's runtime on the device but not working, which a reinstall repairs. */
    val broken: StateFlow<Boolean> = _broken.asStateFlow()

    /** Reports the installation of the chosen engine and reloads its packages and voices. */
    fun check() {
        val engine = engine() ?: return
        task(engine) { refresh(engine); engine.status() }
    }

    fun install() {
        val engine = engine() ?: return
        _status.value = tr("Installing {0} into the app folder…", engine.displayName)
        task(engine) { val summary = engine.setUp(); refresh(engine); summary }
    }

    private suspend fun refresh(engine: LocalSpeechEngine) {
        _ready.value = engine.isReady()
        _packages.value = runCatching { engine.packages() }.getOrDefault(emptyList())
        _voices.value = languagesInUse.value.associate { it.code to runCatching { engine.voices(it.code) }.getOrDefault(emptyList()) }
    }

    /** The recogniser's models, downloaded or not; empty until checked. */
    val recognitionModels = MutableStateFlow<List<RecognitionModel>>(emptyList())

    fun checkRecognition() {
        val setup = recognition ?: return
        viewModelScope.launch { recognitionModels.value = runCatching { setup.models() }.getOrDefault(emptyList()) }
    }

    fun chooseRecognition(quality: RecognitionQuality) = update { it.copy(speechRecognitionQuality = quality.name) }

    private val _downloads = MutableStateFlow<List<SetupItem>?>(null)

    /** The language's voices and speech recognition models, downloaded or not; null while they are looked up. */
    val downloads: StateFlow<List<SetupItem>?> = _downloads.asStateFlow()

    /** Where each voice or model being downloaded or removed is. */
    val downloadStates: StateFlow<Map<String, SetupState>> = setup?.states ?: MutableStateFlow(emptyMap())

    /** Whether the page downloads and removes voices and models itself. */
    val downloadsHere: Boolean get() = setup != null
    private var loadingDownloads: Job? = null

    init {
        if (setup != null) {
            viewModelScope.launch {
                settings.settings.map { it.currentLanguageId to it.speechEngine }.distinctUntilChanged().collect { loadDownloads() }
            }
            // A voice or model that finished downloading, or went, changes what the language has and what speaks it.
            viewModelScope.launch {
                var before = emptyMap<String, SetupState>()
                setup.states.collect { now ->
                    val settled = before.any { (id, was) -> was !is SetupState.Done && (now[id] is SetupState.Done || id !in now) }
                    before = now
                    if (settled) {
                        loadDownloads()
                        checkRecognition()
                        engine()?.let { refresh(it) }
                    }
                }
            }
        }
    }

    private fun loadDownloads() {
        val setup = setup ?: return
        loadingDownloads?.cancel()
        loadingDownloads = viewModelScope.launch {
            _downloads.value = try {
                setup.overview(settings.current.currentLanguageId).filter { it.kind == SetupKind.VOICE || it.kind == SetupKind.RECOGNITION }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /** Downloads a voice or a speech recognition model, which goes on when the page is left. */
    fun install(item: SetupItem) {
        setup?.install(listOf(item))
    }

    fun remove(item: SetupItem) {
        setup?.remove(item)
    }

    /** Whether [item] can be removed: on the device, and not the last voice, which the app keeps on its own. */
    fun canRemove(item: SetupItem): Boolean =
        setup?.canRemove(item) == true && assets?.required(item, downloads.value.orEmpty()) != true

    /**
     * The voice to download when the engine has none for the language: one of the engine's own,
     * else the one the app would fetch on its own; null when every voice is on the device.
     */
    fun voiceToDownload(downloads: List<SetupItem>? = this.downloads.value): SetupItem? {
        val missing = downloads.orEmpty().filter { it.kind == SetupKind.VOICE && it.status == SetupStatus.MISSING }
        val name = engine()?.displayName
        return missing.firstOrNull { it.engine == name } ?: missing.firstOrNull { it.recommended } ?: missing.firstOrNull()
    }

    /** The speech recognition model of [quality] as a download, once the downloads are looked up. */
    fun recognitionDownload(quality: RecognitionQuality): SetupItem? =
        downloads.value?.firstOrNull { it.kind == SetupKind.RECOGNITION && it.id == "recognition:${quality.name.lowercase()}" }

    private fun task(engine: LocalSpeechEngine, block: suspend () -> String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _status.value = try { block().also { _failed.value = false } } catch (e: Exception) { _failed.value = true; e.message ?: tr("unknown error") }
            _broken.value = engine.hasRuntimeSetup && (
                (_ready.value && _failed.value) ||
                    (!_ready.value && runCatching { engine.runtimeInstalled() }.getOrDefault(false))
                )
            _busy.value = false
        }
    }
}

@Composable
fun SpeechScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SpeechViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf(false) }
    val engine = viewModel.localSpeech.find(settings.speechEngine)
    LaunchedEffect(settings.speechEngine, inUse) {
        viewModel.check()
        checked = true
    }
    val learning = inUse.firstOrNull()
    val compact = LocalWindowWidth.current.isCompact
    // An engine that works but has no voice for the language learned is not ready for it.
    val voiceMissing = engine != null && learning != null && voices[learning.code].isNullOrEmpty()

    if (compact) {
        PhoneSpeechScreen(settings, viewModel, engine, learning, voices, packages, checking = busy || !checked, ready, onNavigate, onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.SPEECH, padding, onNavigate) { inner ->
            PageColumn(inner) {
                ScreenHeader(tr("Speech"), null, onBackToSettings = onBack)
                if (LocalWindowWidth.current.isExpanded) {
                    // The engine on the left with speech recognition under it, which keeps the two columns about as tall.
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                            EngineCard(settings, viewModel, engine, voiceMissing)
                            RecognitionCard(viewModel)
                        }
                        VoiceCard(settings, viewModel, engine, learning, voices, Modifier.weight(1f))
                    }
                } else {
                    EngineCard(settings, viewModel, engine, voiceMissing)
                    VoiceCard(settings, viewModel, engine, learning, voices)
                    RecognitionCard(viewModel)
                }
            }
        }
    }
}


/** The engine choice, what it is, whether it works, and a way to reinstall it. */
@Composable
private fun EngineCard(settings: UserSettings, viewModel: SpeechViewModel, engine: LocalSpeechEngine?, voiceMissing: Boolean, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    ContentCard(tr("Speech engine"), tr("Reader playback and term pronunciation use this engine."), icon = AppIcons.Memory, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Dropdown(
                options = viewModel.engines,
                selected = settings.speechEngine.takeIf { it in viewModel.engines } ?: SpeechEngine.SYSTEM,
                onSelect = { chosen -> viewModel.update { it.copy(speechEngine = chosen) } },
                label = tr("Speech engine"),
                optionLabel = { tr(it.label) },
                modifier = Modifier.weight(1f),
            )
            if (!compact) EngineTags(settings.speechEngine)
        }
        Spacer(Modifier.height(AppSpacing.m))
        Text(
            engine?.description?.let { tr(it) } ?: tr("The voices that come with the operating system. They need no download; more voices are added in the system's own settings."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (engine != null) EngineStatus(viewModel, engine, voiceMissing)
    }
}

@Composable
private fun EngineTags(engine: SpeechEngine) {
    when (engine) {
        SpeechEngine.SYSTEM -> Tag(tr("Built in"), MaterialTheme.colorScheme.primary)
        SpeechEngine.PIPER -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("Many languages"), MaterialTheme.colorScheme.primary) }
        SpeechEngine.KOKORO -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("High quality"), MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun EngineStatus(viewModel: SpeechViewModel, engine: LocalSpeechEngine, voiceMissing: Boolean) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val broken by viewModel.broken.collectAsStateWithLifecycle()
    // The way to a voice is in the notice that says one is missing: it downloads one in place.
    val working = ready && !failed
    // The check runs on its own when the page opens or the engine changes: a working engine, or one
    // still being checked, needs no box; only a problem, or an install under way, shows one.
    if (progress == null && (busy || (working && !voiceMissing))) return
    Spacer(Modifier.height(AppSpacing.l))
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        working && !voiceMissing -> StatusTints.ok
        else -> StatusTints.warning
    }
    Notice(
        title = when {
            busy -> tr("Checking…")
            working && voiceMissing -> tr("Installed; no voice for this language yet")
            working -> tr("Installed and working")
            ready || broken -> tr("Something went wrong")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) },
        tint = tint,
        // No tick while the language still has no voice: the box is amber, and says what is missing.
        icon = if (working && !voiceMissing) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = {
            when {
                engine.hasRuntimeSetup && !ready && !broken && !busy -> PrimaryButton(tr("Install"), onClick = viewModel::install)
                working && voiceMissing && viewModel.offersVoice(downloads, downloadStates) -> DownloadVoiceButton(viewModel)
                else -> SecondaryButton(tr("Check installation"), onClick = viewModel::check, enabled = !busy)
            }
        },
    )
    progress?.let {
        Spacer(Modifier.height(AppSpacing.m))
        Text(tr(it), style = MaterialTheme.typography.bodySmall)
        AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
    }
    if (broken && !busy) {
        Spacer(Modifier.height(AppSpacing.m))
        ReinstallEngine(viewModel, engine)
    }
}

/** Shown only while the check finds the engine broken: what a reinstall does, and the button for it. */
@Composable
internal fun ReinstallEngine(viewModel: SpeechViewModel, engine: LocalSpeechEngine) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(
            tr("{0} runs in the app's own Python, kept in the app folder. Reinstalling downloads it again, which repairs a broken installation; downloaded voices are kept.", engine.displayName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(tr("Reinstall {0}", engine.displayName), onClick = viewModel::install, modifier = Modifier.testTag("reinstall-engine"))
    }
}

/**
 * The speed and the language's voices for the engine in use, each to hear, choose, download or
 * remove in place.
 */
@Composable
private fun VoiceCard(
    settings: UserSettings,
    viewModel: SpeechViewModel,
    engine: LocalSpeechEngine?,
    learning: LanguageOption?,
    voices: Map<String, List<SpeechVoice>>,
    modifier: Modifier = Modifier,
) {
    ContentCard(
        tr("Voice & playback"),
        null,
        icon = AppIcons.VolumeUp,
        modifier = modifier.testTag("speech-voices"),
    ) {
        if (engine?.supportsSpeed == true) {
            SliderSetting(
                tr("Speech speed"),
                "${kotlin.math.round(settings.speechSpeed * 100).toInt()}%",
                settings.speechSpeed,
                0.5f..1.5f,
                onChange = { v -> viewModel.update { it.copy(speechSpeed = v) } },
                modifier = Modifier.padding(top = AppSpacing.s),
                step = 0.05f,
                sliderModifier = Modifier.semantics { contentDescription = tr("speech speed") },
            )
            AppDivider(Modifier.padding(vertical = AppSpacing.l))
        }
        VoiceList(settings, viewModel, engine, learning, voices)
    }
}

/** The package's title; the one-download models of Kokoro read "Kokoro model with 34 voices". */
internal val SpeechPackage.displayTitle: String
    get() = MODEL_TITLE.matchEntire(title)?.let { match ->
        trPlural(match.groupValues[2].toInt(), "{1} model with {0} voice", "{1} model with {0} voices", match.groupValues[1])
    } ?: title

private val MODEL_TITLE = Regex("""(.+) model with (\d+) voices""")

/** A short sentence per language, which each voice's play button reads. */
internal val SAMPLES: Map<String, String> = mapOf(
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

/** "Kokoro voice model", or "Kokoro compact model" for a compact copy downloaded before only full models were offered. */
@Composable
internal fun modelTitle(engine: LocalSpeechEngine, pkg: SpeechPackage): String =
    if ("compact model" in pkg.title) tr("{0} compact model", engine.displayName) else tr("{0} voice model", engine.displayName)
