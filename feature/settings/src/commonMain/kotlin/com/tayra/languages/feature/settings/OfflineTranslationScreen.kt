package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.PackageTab
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TabToggle
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.label
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

import org.koin.compose.viewmodel.koinViewModel

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
        _status.value = "Installing $localName into the app folder. This downloads about a gigabyte and takes a few minutes\u2026"
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
                _status.value = "${pkg.title}: ${e.message}"
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
    val learningName = LocalLearningLanguage.current?.currentName
    val native = settings.nativeLanguage.ifBlank { "en" }
    val nativeName = LanguageCatalog.nativeOption(native).name
    val local = viewModel.hasLocalTranslator && settings.translationEngine == TranslationEngine.ARGOS
    val wanted = if (local) viewModel.wantedPackages(inUse, native, packages) else emptySet()
    val missing = packages.filter { it.key in wanted && !it.installed }
    val pair = PairState(learningName, nativeName, missing, sameLanguage = inUse.contains(native))

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader("Translation", "Choose how translations are generated and stored.", onBackToSettings = onBack) {
                if (!LocalWindowWidth.current.isCompact) EngineStatusPill(settings, viewModel, local, busy || !checked, ready, pair)
            }
            if (LocalWindowWidth.current.isCompact) EngineStatusPill(settings, viewModel, local, busy || !checked, ready, pair)
            if (wide) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LanguagePairCard(settings, viewModel, learningName, nativeName, Modifier.weight(1f).fillMaxHeight())
                    EngineCard(settings, viewModel, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                LanguagePairCard(settings, viewModel, learningName, nativeName)
                EngineCard(settings, viewModel)
            }
            if (local && (packages.isNotEmpty() || ready)) PackagesCard(viewModel, packages, wanted, pair)
        }
    }
}

/** Whether the language being learned can be translated into the native language with what is installed. */
private class PairState(val learning: String?, val native: String, val missing: List<LocalPackage>, val sameLanguage: Boolean) {
    val label: String get() = "${learning ?: "Your language"} → $native"
    val ready: Boolean get() = missing.isEmpty()
}

@Composable
private fun EngineStatusPill(settings: UserSettings, viewModel: OfflineTranslationViewModel, local: Boolean, checking: Boolean, ready: Boolean, pair: PairState) {
    when (settings.translationEngine) {
        TranslationEngine.ARGOS -> when {
            !local -> StatusPill("Online translation: MyMemory", StatusTints.ok)
            checking -> StatusPill("Checking offline translation", MaterialTheme.colorScheme.outline)
            !ready -> StatusPill("${viewModel.localName} is not installed", StatusTints.warning)
            !pair.ready -> StatusPill("Language packages needed", StatusTints.warning)
            else -> StatusPill("Offline translation ready", StatusTints.ok)
        }
        TranslationEngine.MYMEMORY -> StatusPill("Online translation: MyMemory", StatusTints.ok)
        TranslationEngine.GOOGLE ->
            if (settings.googleTranslateApiKey.isBlank()) StatusPill("Google API key needed", StatusTints.warning)
            else StatusPill("Online translation: Google", StatusTints.ok)
    }
}

@Composable
private fun LanguagePairCard(settings: UserSettings, viewModel: OfflineTranslationViewModel, learningName: String?, nativeName: String, modifier: Modifier = Modifier) {
    ContentCard("Language pair", "Translations and example sentences use your native language.", icon = AppIcons.SwapHoriz, modifier = modifier) {
        val compact = LocalWindowWidth.current.isCompact
        val learning: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Dropdown(
                    options = listOfNotNull(learningName),
                    selected = learningName,
                    onSelect = {},
                    label = "Learning language",
                    optionLabel = { it },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Managed from the header",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
        }
        val nativeField: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Dropdown(
                    options = LanguageCatalog.nativeLanguages,
                    selected = LanguageCatalog.nativeOption(settings.nativeLanguage),
                    onSelect = { option -> viewModel.update { it.copy(nativeLanguage = option.code) } },
                    label = "Native language",
                    optionLabel = { it.name },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Keeps both fields the same height when side by side.
                Text(" ", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                learning(Modifier.fillMaxWidth())
                nativeField(Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                learning(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "translated into",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(top = 22.dp).size(22.dp),
                )
                nativeField(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoBanner("${learningName ?: "Your"} terms will be translated into $nativeName.")
    }
}

@Composable
private fun EngineCard(settings: UserSettings, viewModel: OfflineTranslationViewModel, modifier: Modifier = Modifier) {
    val engine = settings.translationEngine
    val local = engine == TranslationEngine.ARGOS && viewModel.hasLocalTranslator
    val subtitle = when {
        local && viewModel.hasRuntimeSetup -> "Runs on this computer. No network connection required."
        local -> "Runs on this device. No network connection required."
        engine == TranslationEngine.GOOGLE -> "Google Cloud Translation, billed to your Google Cloud project."
        else -> "A free online service. Sentences are sent over the network."
    }
    val compact = LocalWindowWidth.current.isCompact
    ContentCard("Translation engine", subtitle, icon = AppIcons.Memory, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dropdown(
                options = viewModel.engines,
                selected = engine,
                onSelect = { e -> viewModel.update { it.copy(translationEngine = e) } },
                label = null,
                // Wide screens show "Offline" and "Free" as tags beside the name.
                optionLabel = { if (compact) it.label(viewModel.localName.ifEmpty { null }) else it.shortName(viewModel.localName) },
                modifier = Modifier.weight(1f),
            )
            if (!compact) {
                if (local) Tag("Offline", StatusTints.ok) else Tag("Online", MaterialTheme.colorScheme.primary)
                if (engine == TranslationEngine.GOOGLE) Tag("Paid", StatusTints.warning) else Tag("Free", MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            local -> LocalEngineStatus(settings, viewModel)
            engine == TranslationEngine.GOOGLE -> GoogleSettings(settings, viewModel)
            else -> OutlinedTextField(
                value = settings.translationContactEmail,
                onValueChange = { v -> viewModel.update { it.copy(translationContactEmail = v.trim()) } },
                label = { Text("Contact email (optional)") },
                supportingText = { Text("Raises MyMemory's free daily quota from about 5,000 to 50,000 characters.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Whether the on-device translator works, with buttons to check it or install it, and its runtime settings. */
@Composable
private fun LocalEngineStatus(settings: UserSettings, viewModel: OfflineTranslationViewModel) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var advanced by remember { mutableStateOf(false) }
    val compact = LocalWindowWidth.current.isCompact
    val action: @Composable () -> Unit = {
        if (viewModel.hasRuntimeSetup && !ready && !busy) {
            Button(onClick = viewModel::install, shape = RoundedCornerShape(10.dp)) { Text("Install") }
        } else {
            OutlinedButton(onClick = viewModel::check, enabled = !busy, shape = RoundedCornerShape(10.dp)) {
                Text(if (viewModel.hasRuntimeSetup) "Check installation" else "Check models")
            }
        }
    }
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        ready -> StatusTints.ok
        else -> StatusTints.warning
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            busy -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            ready -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
            else -> Icon(Icons.Default.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    busy -> "Checking…"
                    ready -> "Installed and working"
                    else -> "Not installed"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val detail = status ?: if (!ready && viewModel.hasRuntimeSetup) "Install downloads a private Python into the app folder with argostranslate in it." else null
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        Text(it, style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
    }
    if (viewModel.hasRuntimeSetup) {
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (advanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Advanced runtime settings", style = MaterialTheme.typography.bodyLarge)
            }
            if (advanced) {
                OutlinedTextField(
                    value = settings.argosPython,
                    onValueChange = { v -> viewModel.update { it.copy(argosPython = v.trim()) } },
                    label = { Text("Python executable (optional)") },
                    placeholder = { Text("The app's own Python") },
                    supportingText = { Text("Leave empty to use the Python the app downloads for itself, or give the full path to one that has argostranslate installed.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun GoogleSettings(settings: UserSettings, viewModel: OfflineTranslationViewModel) {
    var showKey by remember { mutableStateOf(false) }
    val googleBusy by viewModel.googleBusy.collectAsStateWithLifecycle()
    val googleStatus by viewModel.googleStatus.collectAsStateWithLifecycle()
    OutlinedTextField(
        value = settings.googleTranslateApiKey,
        onValueChange = { v -> viewModel.update { it.copy(googleTranslateApiKey = v.trim()) } },
        label = { Text("API key") },
        supportingText = { Text("A Google Cloud API key with the Cloud Translation API enabled. The key is ${viewModel.secretStorage}.") },
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::checkGoogleKey, enabled = !googleBusy && settings.googleTranslateApiKey.isNotBlank(), shape = RoundedCornerShape(10.dp)) {
            Text(if (googleBusy) "Checking…" else "Check key")
        }
        googleStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)) }
    }
}

private fun TranslationEngine.shortName(localName: String): String = when (this) {
    TranslationEngine.ARGOS -> localName.ifEmpty { "On this device" }
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
    val noun = if (viewModel.hasRuntimeSetup) "packages" else "models"
    val title = if (viewModel.hasRuntimeSetup) "Language packages" else "Language models"
    val matches = { pkg: LocalPackage -> query.isBlank() || pkg.title.contains(query.trim(), ignoreCase = true) }
    val shown = when (tab) {
        PackageTab.INSTALLED -> installed
        PackageTab.AVAILABLE -> packages.filter { !it.installed }.sortedWith(compareBy({ it.key !in wanted }, { it.title }))
    }.filter(matches).let { list -> if (tab == PackageTab.INSTALLED) list.sortedBy { it.title } else list }

    val search: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search $noun") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = modifier,
        )
    }
    ContentCard(
        title,
        if (viewModel.hasRuntimeSetup) "Download language models for offline translation." else "Download language models for translation on this device.",
        icon = AppIcons.Download,
        headerExtra = {
            if (!compact) {
                Tag("${installed.size} installed", MaterialTheme.colorScheme.primary)
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
                Tag("${installed.size} installed", MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TabToggle(tab) { tab = it }
            }
            Spacer(Modifier.height(12.dp))
        }
        PackageSummary(viewModel, installed, pair, packageBusy)
        Spacer(Modifier.height(12.dp))
        if (shown.isEmpty()) {
            Text(
                if (query.isNotBlank()) "No $noun match “${query.trim()}”." else if (tab == PackageTab.INSTALLED) "No $noun installed yet." else "All $noun are installed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { tab = PackageTab.AVAILABLE; query = "" }, shape = RoundedCornerShape(10.dp)) {
                Icon(AppIcons.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse all ${packages.size} $noun")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Disk used, whether the language pair is covered (with a button for what it lacks), and how pairs are bridged. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackageSummary(viewModel: OfflineTranslationViewModel, installed: List<LocalPackage>, pair: PairState, packageBusy: Set<String>) {
    FlowRow(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val used = installed.sumOf { it.sizeBytes }
        if (used > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Storage, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("${formatSize(used)} used", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        if (pair.learning != null && !pair.sameLanguage) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pair.ready) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${pair.label} is ready", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusTints.warning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${pair.label} needs ${pair.missing.joinToString { it.title }}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(12.dp))
                    val installing = pair.missing.any { it.key in packageBusy }
                    Button(
                        onClick = { pair.missing.forEach(viewModel::installPackage) },
                        enabled = !installing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) { Text(if (installing) "Installing…" else "Install") }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (viewModel.hasRuntimeSetup) "When a direct package is unavailable, Argos can translate through English."
            else "Each language has its own model; translations go through English.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PackageTile(pkg: LocalPackage, busy: Boolean, needed: Boolean, onInstall: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(pkg.fromCode)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(pkg.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = listOfNotNull(pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize), "Needed for your language".takeIf { needed && !pkg.installed })
            if (detail.isNotEmpty()) Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
        }
        Spacer(Modifier.width(12.dp))
        when {
            busy -> Text(if (pkg.installed) "Removing…" else "Downloading…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 12.dp))
            pkg.installed -> {
                Box(Modifier.size(8.dp).clip(CircleShape).background(StatusTints.ok))
                Spacer(Modifier.width(8.dp))
                Text("Installed", style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreHoriz, contentDescription = "More for ${pkg.title}") }
                    AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        AppMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() })
                    }
                }
            }
            else -> Button(onClick = onInstall, shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(end = 8.dp)) { Text("Install") }
        }
    }
}
