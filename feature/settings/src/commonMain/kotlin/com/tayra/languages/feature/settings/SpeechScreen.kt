package com.tayra.languages.feature.settings

import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.Reveal
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
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
import com.tayra.languages.core.domain.practice.SpeechRecognitionSetup
import com.tayra.languages.core.domain.practice.RecognitionModel
import com.tayra.languages.core.domain.practice.RecognitionQuality

/** The speech engines: which one reads aloud, its runtime, and the voices to download. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechViewModel(
    private val settings: SettingsRepository,
    languages: LanguageRepository,
    val localSpeech: LocalSpeech,
    /** The recogniser to set up for speaking practice, where it needs setting up (the desktop). */
    val recognition: SpeechRecognitionSetup? = null,
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
    // An engine that works but has no voice for the language learned is not ready for it.
    val voiceMissing = engine != null && learning != null && voices[learning.code].isNullOrEmpty()
    val pill: @Composable () -> Unit = { SpeechStatusPill(engine, busy || !checked, ready, learning, voices) }

    if (compact) {
        PhoneSpeechScreen(settings, viewModel, engine, learning, voices, packages, checking = busy || !checked, ready, onNavigate, onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Speech"), tr("Choose how words and passages are spoken."), onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            if (LocalWindowWidth.current.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    EngineCard(settings, viewModel, engine, voiceMissing, onNavigate, Modifier.weight(1f).fillMaxHeight())
                    VoiceCard(settings, viewModel, engine, learning, voices, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                EngineCard(settings, viewModel, engine, voiceMissing, onNavigate)
                VoiceCard(settings, viewModel, engine, learning, voices)
            }
            if (LocalWindowWidth.current.isExpanded && viewModel.recognition != null) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    DownloadsLinkCard(voicesInDownloads(recognition = true), onNavigate, Modifier.weight(1f).fillMaxHeight())
                    RecognitionCard(viewModel, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                DownloadsLinkCard(voicesInDownloads(viewModel.recognition != null), onNavigate)
                if (viewModel.recognition != null) RecognitionCard(viewModel)
            }
        }
    }
}

/** What the Downloads page offers in place of this page's own downloads. */
internal fun voicesInDownloads(recognition: Boolean): String =
    if (recognition) tr("Download or remove voices and speech recognition models on the Downloads page.")
    else tr("Download or remove voices on the Downloads page.")

@Composable
internal fun SpeechStatusPill(engine: LocalSpeechEngine?, checking: Boolean, ready: Boolean, learning: LanguageOption?, voices: Map<String, List<SpeechVoice>>) {
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
private fun EngineCard(settings: UserSettings, viewModel: SpeechViewModel, engine: LocalSpeechEngine?, voiceMissing: Boolean, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
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
        if (engine != null) {
            Spacer(Modifier.height(AppSpacing.l))
            EngineStatus(viewModel, engine, voiceMissing, onNavigate)
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
private fun EngineStatus(viewModel: SpeechViewModel, engine: LocalSpeechEngine, voiceMissing: Boolean, onNavigate: (Route) -> Unit) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var advanced by remember { mutableStateOf(false) }
    // The way to a voice is in the notice that says one is missing: the language's downloads.
    val working = ready && !failed
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
            ready -> tr("Something went wrong")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) },
        tint = tint,
        // No tick while the language still has no voice: the box is amber, and says what is missing.
        icon = if (working && !voiceMissing) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = {
            when {
                engine.hasRuntimeSetup && !ready && !busy -> PrimaryButton(tr("Install"), onClick = viewModel::install)
                working && voiceMissing -> PrimaryButton(tr("Go to Downloads"), onClick = { onNavigate(Route.Downloads) }, modifier = Modifier.testTag("notice-download-voice"))
                else -> SecondaryButton(tr("Check installation"), onClick = viewModel::check, enabled = !busy)
            }
        },
    )
    progress?.let {
        Spacer(Modifier.height(AppSpacing.m))
        Text(tr(it), style = MaterialTheme.typography.bodySmall)
        AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
    }
    if (engine.hasRuntimeSetup) {
        Spacer(Modifier.height(AppSpacing.m))
        Column(Modifier.fillMaxWidth().clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel)) {
            Row(
                Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (advanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(AppSizes.icon))
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("Advanced engine settings"), style = MaterialTheme.typography.bodyLarge)
            }
            Reveal(advanced) {
                Column(Modifier.padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(
                        tr("{0} runs in the app's own Python, kept in the app folder. Reinstalling downloads it again, which repairs a broken installation; downloaded voices are kept.", engine.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(tr("Reinstall {0}", engine.displayName), onClick = viewModel::install, enabled = !busy)
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
                    optionLabel = { localizedVoiceName(it.name) },
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
            SliderSetting(
                tr("Speech speed"),
                "${kotlin.math.round(settings.speechSpeed * 100).toInt()}%",
                settings.speechSpeed,
                0.5f..1.5f,
                onChange = { v -> viewModel.update { it.copy(speechSpeed = v) } },
                modifier = Modifier.padding(vertical = AppSpacing.m),
                step = 0.05f,
                sliderModifier = Modifier.semantics { contentDescription = tr("speech speed") },
            )
        }
        AppDivider(Modifier.padding(vertical = AppSpacing.m))
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
    Text(tr("Try this voice"), style = AppText.itemTitle)
    Spacer(Modifier.height(AppSpacing.s))
    AppTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(language?.let { tr("Type a sentence in {0}", tr(it.name)) } ?: tr("Type a sentence in the language you are learning")) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(AppSpacing.l))
    val active = playing != null && playing == text
    Row(verticalAlignment = Alignment.CenterVertically) {
        PrimaryButton(
            when {
                active && working -> tr("Preparing…")
                active -> tr("Stop")
                else -> tr("Play sample")
            },
            onClick = { speaker.toggle(text, language?.code ?: "en") },
            icon = if (active && !working) AppIcons.Stop else AppIcons.VolumeUp,
            enabled = text.isNotBlank(),
        )
        Spacer(Modifier.width(AppSpacing.xxl))
        Waveform(active = active && !working, modifier = Modifier.weight(1f).height(WAVEFORM_HEIGHT))
    }
}

/** A row of bars like a recording's waveform, in the accent colour while the sample plays. */
@Composable
private fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.disabled)
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

/** The package's title; the one-download models of Kokoro read "Kokoro model with 34 voices". */
internal val SpeechPackage.displayTitle: String
    get() = MODEL_TITLE.matchEntire(title)?.let { match ->
        trPlural(match.groupValues[2].toInt(), "{1} model with {0} voice", "{1} model with {0} voices", match.groupValues[1])
    } ?: title

private val MODEL_TITLE = Regex("""(.+) model with (\d+) voices""")

private val WAVEFORM_HEIGHT = 44.dp
private val VOICE_SEARCH_WIDTH = 260.dp

/** A short sentence per language so the Try it field is never empty for the common ones. */
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

/**
 * A voice's name with its details in the interface language, in brackets, "Dora (Brazilian, female)",
 * or after dots, "Cadu · medium · Brazil". A word without a translation stays as it is.
 */
internal fun localizedVoiceName(name: String): String {
    val parts = name.split(" · ")
    if (parts.size > 1) return (listOf(parts.first()) + parts.drop(1).map { tr(it) }).joinToString(" · ")
    val open = name.lastIndexOf(" (")
    if (open < 0 || !name.endsWith(")")) return name
    val details = name.substring(open + 2, name.length - 1).split(", ").joinToString(", ") { tr(it) }
    return "${name.substring(0, open)} ($details)"
}

/** "Kokoro voice model", or "Kokoro compact model" for a compact copy downloaded before only full models were offered. */
@Composable
internal fun modelTitle(engine: LocalSpeechEngine, pkg: SpeechPackage): String =
    if ("compact model" in pkg.title) tr("{0} compact model", engine.displayName) else tr("{0} voice model", engine.displayName)
