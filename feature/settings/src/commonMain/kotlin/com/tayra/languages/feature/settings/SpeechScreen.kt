package com.tayra.languages.feature.settings

import androidx.compose.foundation.Canvas
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.PackageTab
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderStepper
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TabToggle
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
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

    /** Reports the installation of the chosen engine and reloads its packages and voices. */
    fun check() {
        val engine = engine() ?: return
        task { refresh(engine); engine.status() }
    }

    fun install() {
        val engine = engine() ?: return
        _status.value = tr("Installing {0} into the app folder…", engine.displayName)
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
                _status.value = "${pkg.displayTitle}: ${e.message}"
                _failed.value = true
            }
            _packageBusy.update { it - pkg.id }
        }
    }

    private fun task(block: suspend () -> String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _status.value = try { block().also { _failed.value = false } } catch (e: Exception) { _failed.value = true; e.message ?: tr("unknown error") }
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
    val pill: @Composable () -> Unit = { SpeechStatusPill(engine, busy || !checked, ready, learning, voices) }

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Speech"), tr("Choose how words and passages are spoken."), onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            if (LocalWindowWidth.current.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    EngineCard(settings, viewModel, engine, Modifier.weight(1f).fillMaxHeight())
                    VoiceCard(settings, viewModel, engine, learning, voices, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                EngineCard(settings, viewModel, engine)
                VoiceCard(settings, viewModel, engine, learning, voices)
            }
            if (engine != null && packages.isNotEmpty()) {
                if (packages.size == 1) ModelCard(viewModel, engine, packages.single(), settings, learning, voices)
                else VoicesCard(viewModel, engine, packages, settings, learning, voices)
            }
        }
    }
}

@Composable
private fun SpeechStatusPill(engine: LocalSpeechEngine?, checking: Boolean, ready: Boolean, learning: LanguageOption?, voices: Map<String, List<SpeechVoice>>) {
    when {
        engine == null -> StatusPill(tr("Using system voices"), MaterialTheme.colorScheme.primary)
        checking -> StatusPill(tr("Checking speech"), MaterialTheme.colorScheme.outline)
        !ready -> StatusPill(tr("{0} is not installed", engine.displayName), StatusTints.warning)
        learning != null && voices[learning.code].isNullOrEmpty() -> StatusPill(tr("No {0} voice yet", tr(learning.name)), StatusTints.warning)
        else -> StatusPill(tr("Offline speech ready"), StatusTints.ok)
    }
}

/** The engine choice, what it is, whether it works, and a way to reinstall it. */
@Composable
private fun EngineCard(settings: UserSettings, viewModel: SpeechViewModel, engine: LocalSpeechEngine?, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    ContentCard(tr("Speech engine"), tr("Reader playback and term pronunciation use this engine."), icon = AppIcons.Memory, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Spacer(Modifier.height(10.dp))
        Text(
            engine?.description?.let { tr(it) } ?: tr("The voices that come with the operating system. They need no download; more voices are added in the system's own settings."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (engine != null) {
            Spacer(Modifier.height(14.dp))
            EngineStatus(viewModel, engine)
        }
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
private fun EngineStatus(viewModel: SpeechViewModel, engine: LocalSpeechEngine) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    var advanced by remember { mutableStateOf(false) }
    val working = ready && !failed
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        working -> StatusTints.ok
        else -> StatusTints.warning
    }
    val action: @Composable () -> Unit = {
        if (engine.hasRuntimeSetup && !ready && !busy) {
            Button(onClick = viewModel::install, shape = RoundedCornerShape(10.dp)) { Text(tr("Install")) }
        } else {
            OutlinedButton(onClick = viewModel::check, enabled = !busy, shape = RoundedCornerShape(10.dp)) { Text(tr("Check installation")) }
        }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            busy -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            working -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
            else -> Icon(Icons.Default.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    busy -> tr("Checking…")
                    working -> tr("Installed and working")
                    ready -> tr("Something went wrong")
                    else -> tr("Not installed")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (working && !busy) tint else MaterialTheme.colorScheme.onSurface,
            )
            status?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (!compact) {
            Spacer(Modifier.width(12.dp))
            action()
        }
    }
    if (compact) {
        Spacer(Modifier.height(8.dp))
        action()
    }
    progress?.let {
        Spacer(Modifier.height(10.dp))
        Text(tr(it), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
    }
    if (engine.hasRuntimeSetup) {
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))) {
            Row(
                Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (advanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("Advanced engine settings"), style = MaterialTheme.typography.bodyLarge)
            }
            if (advanced) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("{0} runs in the app's own Python, kept in the app folder. Reinstalling downloads it again, which repairs a broken installation; downloaded voices are kept.", engine.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = viewModel::install, enabled = !busy, shape = RoundedCornerShape(10.dp)) { Text(tr("Reinstall {0}", engine.displayName)) }
                }
            }
        }
    }
}

/** The voice for the language being learned, the speed, and a sentence to hear them with. */
@Composable
private fun VoiceCard(
    settings: UserSettings,
    viewModel: SpeechViewModel,
    engine: LocalSpeechEngine?,
    learning: LanguageOption?,
    voices: Map<String, List<SpeechVoice>>,
    modifier: Modifier = Modifier,
) {
    val languageName = learning?.let { tr(it.name) }
    ContentCard(
        tr("Voice & playback"),
        when {
            languageName == null && engine == null -> tr("Hear how your language sounds with the system voice.")
            languageName == null && engine?.supportsSpeed == true -> tr("Choose the voice and playback speed for your language.")
            languageName == null -> tr("Choose the voice for your language and hear it.")
            engine == null -> tr("Hear how {0} sounds with the system voice.", languageName)
            engine.supportsSpeed -> tr("Choose the {0} voice and playback speed.", languageName)
            else -> tr("Choose the {0} voice and hear it.", languageName)
        },
        icon = AppIcons.VolumeUp,
        modifier = modifier,
    ) {
        val options = learning?.let { voices[it.code] }.orEmpty()
        when {
            engine == null -> Text(
                tr("The system picks the voice for each language."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            learning != null && options.isNotEmpty() -> {
                val chosen = settings.speechVoices["${engine.engine.name}:${learning.code}"]
                Dropdown(
                    options = options,
                    selected = options.firstOrNull { it.id == chosen } ?: options.first(),
                    onSelect = { voice -> viewModel.chooseVoice(engine.engine, learning.code, voice.id) },
                    label = tr("{0} voice", tr(learning.name)),
                    optionLabel = { it.name },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> InfoBanner(
                if (languageName == null) tr("{0} has no voice for your language on this device yet, so the system voice reads it.", engine.displayName)
                else tr("{0} has no {1} voice on this device yet, so the system voice reads {1}.", engine.displayName, languageName),
                tint = StatusTints.warning,
                icon = Icons.Default.Warning,
            )
        }
        if (engine?.supportsSpeed == true) {
            SettingRow(tr("Speech speed"), stackOnCompact = true) {
                SliderStepper(
                    value = settings.speechSpeed,
                    range = 0.5f..1.5f,
                    step = 0.05f,
                    label = "${kotlin.math.round(settings.speechSpeed * 100).toInt()}%",
                    name = tr("speech speed"),
                    onChange = { v -> viewModel.update { it.copy(speechSpeed = v) } },
                    modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(440.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        TryIt(viewModel, learning)
    }
}

/** A sentence in the language being learned, to hear the chosen engine and voice with. */
@Composable
private fun TryIt(viewModel: SpeechViewModel, language: LanguageOption?) {
    val speaker = rememberSpeaker(viewModel.localSpeech, viewModel.settingsRepository)
    val working by speaker.working.collectAsStateWithLifecycle()
    val playing by speaker.playing.collectAsStateWithLifecycle()
    var text by remember(language) { mutableStateOf(language?.let { SAMPLES[it.code] }.orEmpty()) }
    Text(tr("Try this voice"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(language?.let { tr("Type a sentence in {0}", tr(it.name)) } ?: tr("Type a sentence in the language you are learning")) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    val active = playing != null && playing == text
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { speaker.toggle(text, language?.code ?: "en") },
            enabled = text.isNotBlank(),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
        ) {
            Icon(if (active && !working) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    active && working -> tr("Preparing…")
                    active -> tr("Stop")
                    else -> tr("Play sample")
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(24.dp))
        Waveform(active = active && !working, modifier = Modifier.weight(1f).height(44.dp))
    }
}

/** A row of bars like a recording's waveform, in the accent colour while the sample plays. */
@Composable
private fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Canvas(modifier) {
        val bar = 3.dp.toPx()
        val gap = 4.dp.toPx()
        val count = ((size.width + gap) / (bar + gap)).toInt()
        for (i in 0 until count) {
            // A fixed pattern of louder and softer bars, so it reads as speech without real audio data.
            val level = 0.2f + 0.8f * (0.5f + 0.5f * kotlin.math.sin(i * 0.9f) * kotlin.math.cos(i * 0.37f)) * (if (i % 7 == 3) 1f else 0.7f)
            val height = size.height * level.coerceIn(0.15f, 1f)
            drawRoundRect(color, Offset(i * (bar + gap), (size.height - height) / 2), Size(bar, height), CornerRadius(bar / 2))
        }
    }
}

/** One download that holds the model and every voice, as Kokoro has. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCard(
    viewModel: SpeechViewModel,
    engine: LocalSpeechEngine,
    pkg: SpeechPackage,
    settings: UserSettings,
    learning: LanguageOption?,
    voices: Map<String, List<SpeechVoice>>,
) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val busy = pkg.id in packageBusy
    val compact = LocalWindowWidth.current.isCompact
    val voiceCount = Regex("""(\d+) voices""").find(pkg.title)?.groupValues?.get(1)
    ContentCard(
        tr("Voice model"),
        tr(engine.packagesDescription),
        icon = AppIcons.Download,
        titleExtra = { if (pkg.installed) Tag(tr("Installed"), StatusTints.ok) else Tag(tr("Not downloaded"), StatusTints.warning) },
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(12.dp),
        ) {
            val languages = pkg.group.split(", ").filter { it.isNotBlank() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(StatusTints.ok.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Storage, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("{0} voice model", engine.displayName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(voiceCount?.let { trPlural(it.toInt(), "{0} voice", "{0} voices") }, pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!compact) LanguageChips(languages)
                Spacer(Modifier.width(12.dp))
                PackageAction(pkg, busy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) })
            }
            if (compact) {
                Spacer(Modifier.height(10.dp))
                LanguageChips(languages)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        DefaultVoiceNote(engine, settings, learning, voices, others = tr("Other languages fall back to the system voice."))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageChips(names: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { name ->
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CodeTile(LanguageCodes.codeFor(name) ?: name.take(2), size = 34)
                Spacer(Modifier.width(10.dp))
                Text(tr(name), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** One download per voice, as Piper has: what is installed, and the rest to search and download. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoicesCard(
    viewModel: SpeechViewModel,
    engine: LocalSpeechEngine,
    packages: List<SpeechPackage>,
    settings: UserSettings,
    learning: LanguageOption?,
    voices: Map<String, List<SpeechVoice>>,
) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    val installed = packages.filter { it.installed }
    // Without a voice for the language being learned, the list opens on the voices to download for it.
    val hasLearningVoice = learning == null || installed.any { it.languageCode == learning.code }
    var tab by remember(hasLearningVoice) { mutableStateOf(if (hasLearningVoice) PackageTab.INSTALLED else PackageTab.AVAILABLE) }
    var query by remember { mutableStateOf("") }
    val matches = { pkg: SpeechPackage -> query.isBlank() || listOf(pkg.title, pkg.group, tr(pkg.group)).any { it.contains(query.trim(), true) } }
    val shown = when (tab) {
        PackageTab.INSTALLED -> installed.sortedWith(compareBy({ it.languageCode != learning?.code }, { it.group }, { it.title }))
        // Voices for the language being learned come first; the rest only when searched for.
        PackageTab.AVAILABLE -> packages.filter { !it.installed && (query.isNotBlank() || it.languageCode == learning?.code) }.sortedBy { it.title }
    }.filter(matches)
    val search: @Composable (Modifier) -> Unit = { m ->
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(tr("Search voices")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = m,
        )
    }
    ContentCard(
        tr("Voices"),
        tr(engine.packagesDescription),
        icon = AppIcons.Download,
        headerExtra = {
            if (!compact) {
                Tag(tr("{0} installed", installed.size), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                search(Modifier.width(260.dp))
                Spacer(Modifier.width(12.dp))
                TabToggle(tab) { tab = it }
            }
        },
    ) {
        if (compact) {
            search(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(tr("{0} installed", installed.size), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TabToggle(tab) { tab = it }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (shown.isEmpty()) {
            Text(
                when {
                    query.isNotBlank() -> tr("No voices match “{0}”.", query.trim())
                    tab == PackageTab.INSTALLED -> tr("No voices downloaded yet.")
                    learning == null -> tr("Search to find voices to download.")
                    else -> tr("Every {0} voice is downloaded. Search to find voices for other languages.", tr(learning.name))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = if (compact) 1 else 2,
        ) {
            shown.forEach { pkg ->
                VoiceTile(pkg, busy = pkg.id in packageBusy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) }, modifier = Modifier.weight(1f))
            }
            if (!compact && shown.size % 2 == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        DefaultVoiceNote(engine, settings, learning, voices, others = tr("Languages without a downloaded voice use the system voice."))
    }
}

@Composable
private fun VoiceTile(pkg: SpeechPackage, busy: Boolean, onInstall: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(pkg.languageCode ?: pkg.group.take(2))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(pkg.title, style = MaterialTheme.typography.bodyLarge, maxLines = if (LocalWindowWidth.current.isCompact) 2 else 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(tr(pkg.group), pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        Spacer(Modifier.width(12.dp))
        PackageAction(pkg, busy, onInstall, onRemove)
    }
}

/** "Installed" with a menu to remove it, a download button, or what is happening right now. */
@Composable
private fun PackageAction(pkg: SpeechPackage, busy: Boolean, onInstall: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            busy -> Text(if (pkg.installed) tr("Removing…") else tr("Downloading…"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 12.dp))
            pkg.installed -> {
                Box(Modifier.size(8.dp).clip(CircleShape).background(StatusTints.ok))
                Spacer(Modifier.width(8.dp))
                Text(tr("Installed"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreHoriz, contentDescription = tr("More for {0}", pkg.displayTitle)) }
                    AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        AppMenuItem(text = { Text(tr("Remove")) }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            else -> Button(onClick = onInstall, shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(end = 8.dp)) { Text(tr("Download")) }
        }
    }
}

/** Which voice the language being learned uses, and what happens to the others. */
@Composable
private fun DefaultVoiceNote(engine: LocalSpeechEngine, settings: UserSettings, learning: LanguageOption?, voices: Map<String, List<SpeechVoice>>, others: String) {
    val options = learning?.let { voices[it.code] }.orEmpty()
    val chosen = learning?.let { settings.speechVoices["${engine.engine.name}:${it.code}"] }
    val voice = options.firstOrNull { it.id == chosen } ?: options.firstOrNull()
    val first = when {
        learning == null -> null
        voice != null -> tr("{0} uses {1}.", tr(learning.name), voice.name)
        else -> tr("{0} has no {1} voice yet.", tr(learning.name), engine.displayName)
    }
    InfoBanner(listOfNotNull(first, others).joinToString("   •   "))
}

/** The package's title; the one-download models of Kokoro read "Kokoro model with 34 voices". */
private val SpeechPackage.displayTitle: String
    get() = MODEL_TITLE.matchEntire(title)?.let { match ->
        trPlural(match.groupValues[2].toInt(), "{1} model with {0} voice", "{1} model with {0} voices", match.groupValues[1])
    } ?: title

private val MODEL_TITLE = Regex("""(.+) model with (\d+) voices""")

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
