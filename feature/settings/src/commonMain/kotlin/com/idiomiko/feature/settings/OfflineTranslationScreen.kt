package com.idiomiko.feature.settings

import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.service.GoogleTranslation
import com.idiomiko.core.domain.service.LocalPackage
import com.idiomiko.core.domain.service.LocalSentenceTranslator
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTextField
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.idiomiko.core.ui.components.Reveal

/** Argos Translate on this computer: its runtime, the Python it runs on, and its language packages. */
class OfflineTranslationViewModel(
    private val settings: SettingsRepository,
    languages: LanguageRepository,
    private val localTranslation: LocalTranslation,
    private val google: GoogleTranslation,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    /** Whether this platform has an on-device translator (Argos on desktop, ML Kit on phones). */
    val hasLocalTranslator: Boolean = localTranslation.translator != null
    val localName: String = localTranslation.translator?.displayName.orEmpty()
    val localDescription: String = localTranslation.translator?.description.orEmpty()
    val hasRuntimeSetup: Boolean = localTranslation.translator?.hasRuntimeSetup == true

    /** Package keys the books in use need, as the translator counts them. */
    fun wantedPackages(inUse: Set<String>, native: String, catalog: List<LocalPackage>): Set<String> {
        val translator = localTranslation.translator ?: return emptySet()
        return inUse.filter { it != native }.flatMap { translator.requiredPackages(it, native, catalog) }.toSet()
    }

    /** Where the API key is kept on this platform. */
    val secretStorage: String get() = settings.secretStorage

    /** Engines this platform can offer. */
    val engines: List<TranslationEngine> = TranslationEngine.entries.filter { it != TranslationEngine.ARGOS || hasLocalTranslator }

    private val _googleStatus = MutableStateFlow<String?>(null)
    val googleStatus: StateFlow<String?> = _googleStatus.asStateFlow()
    private val _googleBusy = MutableStateFlow(false)
    val googleBusy: StateFlow<Boolean> = _googleBusy.asStateFlow()

    fun checkGoogleKey() {
        if (_googleBusy.value) return
        _googleBusy.value = true
        viewModelScope.launch {
            _googleStatus.value = try { google.checkKey() } catch (e: Exception) { "Google Translate: ${e.message}" }
            _googleBusy.value = false
        }
    }

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _packages = MutableStateFlow<List<LocalPackage>>(emptyList())
    val packages: StateFlow<List<LocalPackage>> = _packages.asStateFlow()

    private val _ready = MutableStateFlow(true)
    /** False once a check finds no working runtime, which reveals the install button. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** What the translator is downloading or installing right now, for the progress line. */
    val progress: StateFlow<String?> = localTranslation.translator?.progress ?: MutableStateFlow(null)

    /** The code of the language being learned, so the package list can lead with what it needs. */
    val languagesInUse: StateFlow<Set<String>> = learningLanguage(languages, settings)
        .map { setOfNotNull(it?.code) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    /** Reports the installation and reloads the package list. */
    fun check() = task { translator ->
        val status = translator.status()
        val packages = runCatching { translator.packages() }
        _ready.value = packages.isSuccess
        _packages.value = packages.getOrDefault(emptyList())
        status
    }

    /** Downloads the app's own Python with argostranslate; slow, so the status says so meanwhile. */
    fun install() {
        _status.value = tr("Installing {0} into the app folder. This downloads about a gigabyte and takes a few minutes…", localName)
        task { translator ->
            val summary = translator.setUp()
            val packages = runCatching { translator.packages() }
            _ready.value = packages.isSuccess
            _packages.value = packages.getOrDefault(emptyList())
            summary + "\n" + translator.status()
        }
    }

    private fun task(block: suspend (LocalSentenceTranslator) -> String) {
        val translator = localTranslation.translator ?: return
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            _status.value = try { block(translator) } catch (e: Exception) { "$localName: ${e.message}" }
            _busy.value = false
        }
    }
}


@Composable
fun OfflineTranslationScreen(
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: OfflineTranslationViewModel = koinViewModel(),
    dictionaries: DictionaryImportViewModel = koinViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(settings.translationEngine) {
        if (settings.translationEngine == TranslationEngine.ARGOS) {
            viewModel.check()
            checked = true
        }
    }
    val native = settings.nativeLanguage.ifBlank { "en" }
    val local = viewModel.hasLocalTranslator && settings.translationEngine == TranslationEngine.ARGOS
    val wanted = if (local) viewModel.wantedPackages(inUse, native, packages) else emptySet()
    // Whether the language being learned can be translated into the native one with what is installed.
    val pairReady = packages.none { it.key in wanted && !it.installed }

    val learningCode = LocalLearningLanguage.current?.currentName?.let(LanguageCodes::codeFor)
    val importCard: @Composable (Modifier) -> Unit = { modifier ->
        if (dictionaries.canImport) ImportDictionaryCard(dictionaries, learningCode, native, modifier)
    }

    if (LocalWindowWidth.current.isCompact) {
        PhoneTranslationScreen(settings, viewModel, onNavigate, onBack, local, checking = busy || !checked, ready, pairReady, importCard)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.TRANSLATION, padding, onNavigate) { inner ->
            PageColumn(inner) {
                ScreenHeader(tr("Translation"), tr("Choose how translations are generated, and add dictionaries of your own."), onBackToSettings = onBack) {
                    if (!LocalWindowWidth.current.isCompact) EngineStatusPill(settings, viewModel, local, busy || !checked, ready, pairReady)
                }
                if (LocalWindowWidth.current.isExpanded && dictionaries.canImport) {
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        EngineCard(settings, viewModel, pairReady, Modifier.weight(1f).fillMaxHeight())
                        importCard(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    EngineCard(settings, viewModel, pairReady)
                    importCard(Modifier)
                }
            }
        }
    }
}

@Composable
internal fun EngineStatusPill(settings: UserSettings, viewModel: OfflineTranslationViewModel, local: Boolean, checking: Boolean, ready: Boolean, pairReady: Boolean) {
    when (settings.translationEngine) {
        TranslationEngine.ARGOS -> when {
            !local -> StatusPill(tr("Online translation: {0}", "MyMemory"), StatusTints.ok)
            checking -> StatusPill(tr("Checking offline translation"), MaterialTheme.colorScheme.outline)
            !ready -> StatusPill(tr("{0} is not installed", viewModel.localName), StatusTints.warning)
            // Named as the rest of the page names them: packages for the desktop's translator, models for the phone's.
            !pairReady -> StatusPill(if (viewModel.hasRuntimeSetup) tr("Language packages needed") else tr("Language models needed"), StatusTints.warning)
            else -> StatusPill(tr("Offline translation ready"), StatusTints.ok)
        }
        TranslationEngine.MYMEMORY -> StatusPill(tr("Online translation: {0}", "MyMemory"), StatusTints.ok)
        TranslationEngine.GOOGLE ->
            if (settings.googleTranslateApiKey.isBlank()) StatusPill(tr("Google API key needed"), StatusTints.warning)
            else StatusPill(tr("Online translation: {0}", "Google"), StatusTints.ok)
    }
}

@Composable
private fun EngineCard(settings: UserSettings, viewModel: OfflineTranslationViewModel, pairReady: Boolean, modifier: Modifier = Modifier) {
    val engine = settings.translationEngine
    val local = engine == TranslationEngine.ARGOS && viewModel.hasLocalTranslator
    val subtitle = when {
        local && viewModel.hasRuntimeSetup -> tr("Runs on this computer. No network connection required.")
        local -> tr("Runs on this device. No network connection required.")
        engine == TranslationEngine.GOOGLE -> tr("Google Cloud Translation, billed to your Google Cloud project.")
        else -> tr("A free online service. Sentences are sent over the network.")
    }
    val compact = LocalWindowWidth.current.isCompact
    ContentCard(tr("Translation engine"), subtitle, icon = AppIcons.Memory, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Dropdown(
                options = viewModel.engines,
                selected = engine,
                onSelect = { e -> viewModel.update { it.copy(translationEngine = e) } },
                label = null,
                // Wide screens show "Offline" and "Free" as tags beside the name.
                optionLabel = { if (compact) it.displayLabel(viewModel.localName.ifEmpty { null }) else it.shortName(viewModel.localName) },
                modifier = Modifier.weight(1f),
            )
            if (!compact) {
                if (local) Tag(tr("Offline"), StatusTints.ok) else Tag(tr("Online"), MaterialTheme.colorScheme.primary)
                if (engine == TranslationEngine.GOOGLE) Tag(tr("Paid"), StatusTints.warning) else Tag(tr("Free"), MaterialTheme.colorScheme.primary)
            }
        }
        if (!local) Spacer(Modifier.height(AppSpacing.l))
        when {
            local -> LocalEngineStatus(settings, viewModel, pairReady)
            engine == TranslationEngine.GOOGLE -> GoogleSettings(settings, viewModel)
            else -> AppTextField(
                value = settings.translationContactEmail,
                onValueChange = { v -> viewModel.update { it.copy(translationContactEmail = v.trim()) } },
                label = { Text(tr("Contact email (optional)")) },
                supportingText = { Text(tr("Raises MyMemory's free daily quota from about 5,000 to 50,000 characters.")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Whether the on-device translator works, with buttons to check it or install it, and its runtime settings. */
@Composable
private fun LocalEngineStatus(settings: UserSettings, viewModel: OfflineTranslationViewModel, pairReady: Boolean) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    // The check runs on its own when the page opens or the engine changes: a translator that works with
    // the pair's packages in, or one still being checked, needs no box; only a problem, or an install, shows one.
    if (progress == null && (busy || (ready && pairReady))) return
    Spacer(Modifier.height(AppSpacing.l))
    // The engine working is not the whole story: until the pair's packages are in, it is amber, as the status pill above says.
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        ready && pairReady -> StatusTints.ok
        else -> StatusTints.warning
    }
    Notice(
        title = when {
            busy -> tr("Checking…")
            ready && pairReady -> tr("Installed and working")
            ready -> tr("Installed. The language pair still needs a download.")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) } ?: if (!ready && viewModel.hasRuntimeSetup) tr("Install downloads a private Python into the app folder with argostranslate in it.") else null,
        tint = tint,
        // A tick only when everything works; amber with a warning sign while something is still missing.
        icon = if (ready && pairReady) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = {
            if (viewModel.hasRuntimeSetup && !ready && !busy) {
                PrimaryButton(tr("Install"), onClick = viewModel::install)
            } else {
                SecondaryButton(if (viewModel.hasRuntimeSetup) tr("Check installation") else tr("Check models"), onClick = viewModel::check, enabled = !busy)
            }
        },
    )
    progress?.let {
        Spacer(Modifier.height(AppSpacing.m))
        Text(tr(it), style = MaterialTheme.typography.bodySmall)
        AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
    }
}

@Composable
internal fun GoogleSettings(settings: UserSettings, viewModel: OfflineTranslationViewModel) {
    var showKey by remember { mutableStateOf(false) }
    val googleBusy by viewModel.googleBusy.collectAsStateWithLifecycle()
    val googleStatus by viewModel.googleStatus.collectAsStateWithLifecycle()
    AppTextField(
        value = settings.googleTranslateApiKey,
        onValueChange = { v -> viewModel.update { it.copy(googleTranslateApiKey = v.trim()) } },
        label = { Text(tr("API key")) },
        supportingText = { Text(tr("A Google Cloud API key with the Cloud Translation API enabled. The key is {0}.", tr(viewModel.secretStorage))) },
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { QuietButton(if (showKey) tr("Hide") else tr("Show"), onClick = { showKey = !showKey }) },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SecondaryButton(if (googleBusy) tr("Checking…") else tr("Check key"), onClick = viewModel::checkGoogleKey, enabled = !googleBusy && settings.googleTranslateApiKey.isNotBlank())
        googleStatus?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) }
    }
}

/** The engine's name in menus, as [label] gives it, in the interface language. */
internal fun TranslationEngine.displayLabel(localTranslatorName: String?): String =
    if (this == TranslationEngine.ARGOS && localTranslatorName != null) tr("{0} (offline, free)", localTranslatorName) else tr(label)

/** The local translator's packages are named "from → to" with the languages' English names, or by [LocalPackage.label]. */
internal val LocalPackage.displayTitle: String get() = label?.let { tr(it) } ?: "${tr(fromName)} \u2192 ${tr(toName)}"

internal fun TranslationEngine.shortName(localName: String): String = when (this) {
    TranslationEngine.ARGOS -> localName.ifEmpty { tr("On this device") }
    TranslationEngine.MYMEMORY -> "MyMemory"
    TranslationEngine.GOOGLE -> "Google Translate"
}

