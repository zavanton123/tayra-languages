package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PackageTab
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TabToggle
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.tayra.languages.core.ui.components.Reveal

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
    val localPackagesDescription: String = localTranslation.translator?.packagesDescription.orEmpty()
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
    private val _packageBusy = MutableStateFlow<Set<String>>(emptySet())
    val packageBusy: StateFlow<Set<String>> = _packageBusy.asStateFlow()

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

    fun installPackage(pkg: LocalPackage) = packageTask(pkg) { it.installPackage(pkg.fromCode, pkg.toCode) }

    fun removePackage(pkg: LocalPackage) = packageTask(pkg) { it.removePackage(pkg.fromCode, pkg.toCode) }

    private fun packageTask(pkg: LocalPackage, block: suspend (LocalSentenceTranslator) -> Unit) {
        val translator = localTranslation.translator ?: return
        if (pkg.key in _packageBusy.value) return
        _packageBusy.update { it + pkg.key }
        viewModelScope.launch {
            try {
                block(translator)
                _packages.value = translator.packages()
                _status.value = translator.status()
            } catch (e: Exception) {
                _status.value = "${pkg.displayTitle}: ${e.message}"
            }
            _packageBusy.update { it - pkg.key }
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
fun OfflineTranslationScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: OfflineTranslationViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(settings.argosPython, settings.translationEngine) {
        if (settings.translationEngine == TranslationEngine.ARGOS) {
            viewModel.check()
            checked = true
        }
    }
    val wide = LocalWindowWidth.current.isExpanded
    val learningName = LocalLearningLanguage.current?.currentName?.let { tr(it) }
    val native = settings.nativeLanguage.ifBlank { "en" }
    val nativeName = tr(LanguageCatalog.nativeOption(native).name)
    val local = viewModel.hasLocalTranslator && settings.translationEngine == TranslationEngine.ARGOS
    val wanted = if (local) viewModel.wantedPackages(inUse, native, packages) else emptySet()
    val missing = packages.filter { it.key in wanted && !it.installed }
    val pair = PairState(learningName, nativeName, missing, sameLanguage = inUse.contains(native))

    if (LocalWindowWidth.current.isCompact) {
        PhoneTranslationScreen(settings, viewModel, onNavigate, onBack, local, checking = busy || !checked, ready, pair, packages, wanted, learningName, nativeName)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Translation"), tr("Choose how translations are generated and stored."), onBackToSettings = onBack) {
                if (!LocalWindowWidth.current.isCompact) EngineStatusPill(settings, viewModel, local, busy || !checked, ready, pair)
            }
            if (LocalWindowWidth.current.isCompact) EngineStatusPill(settings, viewModel, local, busy || !checked, ready, pair)
            if (wide) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    LanguagePairCard(settings, viewModel, learningName, nativeName, onNavigate, Modifier.weight(1f).fillMaxHeight())
                    EngineCard(settings, viewModel, pair.ready, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                LanguagePairCard(settings, viewModel, learningName, nativeName, onNavigate)
                EngineCard(settings, viewModel, pair.ready)
            }
            if (local && (packages.isNotEmpty() || ready)) PackagesCard(viewModel, packages, wanted, pair)
        }
    }
}

/** Whether the language being learned can be translated into the native language with what is installed. */
internal class PairState(val learning: String?, val native: String, val missing: List<LocalPackage>, val sameLanguage: Boolean) {
    val label: String get() = "${learning ?: tr("Your language")} → $native"
    val ready: Boolean get() = missing.isEmpty()
}

@Composable
internal fun EngineStatusPill(settings: UserSettings, viewModel: OfflineTranslationViewModel, local: Boolean, checking: Boolean, ready: Boolean, pair: PairState) {
    when (settings.translationEngine) {
        TranslationEngine.ARGOS -> when {
            !local -> StatusPill(tr("Online translation: {0}", "MyMemory"), StatusTints.ok)
            checking -> StatusPill(tr("Checking offline translation"), MaterialTheme.colorScheme.outline)
            !ready -> StatusPill(tr("{0} is not installed", viewModel.localName), StatusTints.warning)
            // Named as the rest of the page names them: packages for the desktop's translator, models for the phone's.
            !pair.ready -> StatusPill(if (viewModel.hasRuntimeSetup) tr("Language packages needed") else tr("Language models needed"), StatusTints.warning)
            else -> StatusPill(tr("Offline translation ready"), StatusTints.ok)
        }
        TranslationEngine.MYMEMORY -> StatusPill(tr("Online translation: {0}", "MyMemory"), StatusTints.ok)
        TranslationEngine.GOOGLE ->
            if (settings.googleTranslateApiKey.isBlank()) StatusPill(tr("Google API key needed"), StatusTints.warning)
            else StatusPill(tr("Online translation: {0}", "Google"), StatusTints.ok)
    }
}

@Composable
private fun LanguagePairCard(settings: UserSettings, viewModel: OfflineTranslationViewModel, learningName: String?, nativeName: String, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    ContentCard(tr("Language pair"), tr("Translations and example sentences use your native language."), icon = AppIcons.SwapHoriz, modifier = modifier) {
        val compact = LocalWindowWidth.current.isCompact
        val learning: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Dropdown(
                    options = listOfNotNull(learningName),
                    selected = learningName,
                    onSelect = {},
                    label = tr("Learning language"),
                    optionLabel = { it },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                // The language learned is chosen on Languages, which this leads to, as the phone's page does.
                LinkText(tr("Change the language you learn"), onClick = { onNavigate(Route.Languages) }, modifier = Modifier.padding(start = AppSpacing.l, top = AppSpacing.xs), style = MaterialTheme.typography.bodySmall)
            }
        }
        val nativeField: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                // The language being learned cannot be the native one too, so it is listed dimmed.
                val learningCode = LocalLearningLanguage.current?.currentName?.let(LanguageCodes::codeFor)
                Dropdown(
                    options = LanguageCatalog.nativeLanguages,
                    unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
                    selected = LanguageCatalog.nativeOption(settings.nativeLanguage),
                    onSelect = { option -> if (option.code != learningCode) viewModel.update { it.copy(nativeLanguage = option.code) } },
                    label = tr("Native language"),
                    optionLabel = { tr(it.name) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Keeps both fields the same height when side by side.
                Text(" ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = AppSpacing.xs))
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                learning(Modifier.fillMaxWidth())
                nativeField(Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                learning(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = tr("translated into"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppSpacing.m).padding(top = AppSpacing.xxl).size(AppSizes.iconMedium),
                )
                nativeField(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(AppSpacing.m))
        InfoBanner(learningName?.let { tr("{0} terms will be translated into {1}.", it, nativeName) } ?: tr("Your terms will be translated into {0}.", nativeName))
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
        Spacer(Modifier.height(AppSpacing.l))
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
    var advanced by remember { mutableStateOf(false) }
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
    if (viewModel.hasRuntimeSetup) {
        Spacer(Modifier.height(AppSpacing.m))
        Column(
            Modifier.fillMaxWidth().clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (advanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(AppSizes.icon))
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("Advanced runtime settings"), style = MaterialTheme.typography.bodyLarge)
            }
            Reveal(advanced) {
                AppTextField(
                    value = settings.argosPython,
                    onValueChange = { v -> viewModel.update { it.copy(argosPython = v.trim()) } },
                    label = { Text(tr("Python executable (optional)")) },
                    placeholder = { Text(tr("The app's own Python")) },
                    supportingText = { Text(tr("Leave empty to use the Python the app downloads for itself, or give the full path to one that has argostranslate installed.")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.m),
                )
            }
        }
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

/** The translator's language packages: what is installed, what the language pair needs, and the rest to download. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackagesCard(viewModel: OfflineTranslationViewModel, packages: List<LocalPackage>, wanted: Set<String>, pair: PairState) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(PackageTab.INSTALLED) }
    var query by remember { mutableStateOf("") }
    val compact = LocalWindowWidth.current.isCompact
    val installed = packages.filter { it.installed }
    val runtime = viewModel.hasRuntimeSetup
    val title = if (runtime) tr("Language packages") else tr("Language models")
    val matches = { pkg: LocalPackage -> query.isBlank() || pkg.title.contains(query.trim(), ignoreCase = true) || pkg.displayTitle.contains(query.trim(), ignoreCase = true) }
    val shown = when (tab) {
        PackageTab.INSTALLED -> installed
        PackageTab.AVAILABLE -> packages.filter { !it.installed }.sortedWith(compareBy({ it.key !in wanted }, { it.title }))
    }.filter(matches).let { list -> if (tab == PackageTab.INSTALLED) list.sortedBy { it.title } else list }

    val search: @Composable (Modifier) -> Unit = { modifier ->
        SearchField(query, { query = it }, if (runtime) tr("Search packages") else tr("Search models"), modifier)
    }
    ContentCard(
        title,
        if (runtime) tr("Download language models for offline translation.") else tr("Download language models for translation on this device."),
        icon = AppIcons.Download,
        headerExtra = {
            if (!compact) {
                Tag(tr("{0} downloaded", installed.size), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(AppSpacing.l))
                search(Modifier.width(PACKAGE_SEARCH_WIDTH))
                Spacer(Modifier.width(AppSpacing.m))
                TabToggle(tab) { tab = it }
            }
        },
    ) {
        if (compact) {
            search(Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(tr("{0} downloaded", installed.size), MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TabToggle(tab) { tab = it }
            }
            Spacer(Modifier.height(AppSpacing.m))
        }
        PackageSummary(viewModel, installed, pair, packageBusy)
        Spacer(Modifier.height(AppSpacing.m))
        if (shown.isEmpty()) {
            Text(
                when {
                    query.isNotBlank() -> if (runtime) tr("No packages match “{0}”.", query.trim()) else tr("No models match “{0}”.", query.trim())
                    tab == PackageTab.INSTALLED -> if (runtime) tr("No packages installed yet.") else tr("No models installed yet.")
                    else -> if (runtime) tr("All packages are installed.") else tr("All models are installed.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.m),
            )
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
            maxItemsInEachRow = if (compact) 1 else 2,
        ) {
            shown.forEach { pkg ->
                PackageTile(
                    pkg,
                    busy = pkg.key in packageBusy,
                    needed = pkg.key in wanted,
                    onInstall = { viewModel.installPackage(pkg) },
                    onRemove = { viewModel.removePackage(pkg) },
                    modifier = Modifier.weight(1f),
                )
            }
            // An odd last tile keeps half the width instead of stretching across.
            if (!compact && shown.size % 2 == 1) Spacer(Modifier.weight(1f))
        }
        if (tab == PackageTab.INSTALLED && packages.size > installed.size) {
            Spacer(Modifier.height(AppSpacing.l))
            SecondaryButton(
                if (runtime) tr("Browse all {0} packages", packages.size) else tr("Browse all {0} models", packages.size),
                onClick = { tab = PackageTab.AVAILABLE; query = "" },
                icon = AppIcons.OpenInNew,
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}

/** Disk used, whether the language pair is covered (with a button for what it lacks), and how pairs are bridged. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageSummary(viewModel: OfflineTranslationViewModel, installed: List<LocalPackage>, pair: PairState, packageBusy: Set<String>) {
    FlowRow(
        Modifier.fillMaxWidth().clip(AppShapes.control).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(AppBorders.hairline(), AppShapes.control).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val used = installed.sumOf { it.sizeBytes }
        if (used > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Storage, contentDescription = null, modifier = Modifier.size(AppSizes.icon), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("{0} used", formatSize(used)), style = AppText.labelStrong)
            }
            AppVerticalDivider(Modifier.height(SUMMARY_DIVIDER_HEIGHT))
        }
        if (pair.learning != null && !pair.sameLanguage) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pair.ready) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.icon))
                    Spacer(Modifier.width(AppSpacing.s))
                    Text(tr("{0} is ready", pair.label), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusTints.warning, modifier = Modifier.size(AppSizes.icon))
                    Spacer(Modifier.width(AppSpacing.s))
                    Text(tr("{0} needs {1}", pair.label, pair.missing.joinToString { it.displayTitle }), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(AppSpacing.m))
                    val installing = pair.missing.any { it.key in packageBusy }
                    PrimaryButton(
                        onClick = { pair.missing.forEach(viewModel::installPackage) },
                        enabled = !installing,
                        size = ButtonSize.Compact,
                    ) { Text(if (installing) tr("Installing…") else tr("Install")) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (viewModel.hasRuntimeSetup) tr("When a direct package is unavailable, Argos can translate through English.")
            else tr("Each language has its own model; translations go through English."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PackageTile(pkg: LocalPackage, busy: Boolean, needed: Boolean, onInstall: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control)
            .padding(start = AppSpacing.m, end = AppSpacing.xs, top = AppSpacing.s, bottom = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(pkg.fromCode)
        Spacer(Modifier.width(AppSpacing.l))
        Column(Modifier.weight(1f)) {
            Text(pkg.displayTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = listOfNotNull(pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize), tr("Needed for your language").takeIf { needed && !pkg.installed })
            if (detail.isNotEmpty()) Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
        }
        Spacer(Modifier.width(AppSpacing.m))
        when {
            busy -> Text(if (pkg.installed) tr("Removing…") else tr("Downloading…"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = AppSpacing.m))
            pkg.installed -> {
                StatusDot(StatusTints.ok, size = AppSizes.dot)
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("Downloaded"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
                Spacer(Modifier.width(AppSpacing.xs))
                OverflowMenu(tr("More for {0}", pkg.displayTitle)) { close ->
                    AppMenuItem(text = { Text(tr("Remove")) }, onClick = { close(); onRemove() })
                }
            }
            else -> PrimaryButton(tr("Install"), onClick = onInstall, modifier = Modifier.padding(end = AppSpacing.s))
        }
    }
}

private val PACKAGE_SEARCH_WIDTH = 260.dp
private val SUMMARY_DIVIDER_HEIGHT = 22.dp
