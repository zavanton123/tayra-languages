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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.AlibabaTranslation
import com.tayra.languages.core.domain.service.AzureTranslation
import com.tayra.languages.core.domain.service.BaiduTranslation
import com.tayra.languages.core.domain.service.DeeplTranslation
import com.tayra.languages.core.domain.service.QwenTranslation
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.label
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
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

/** Argos Translate on this computer: its runtime, the Python it runs on, and its language packages. */
class OfflineTranslationViewModel(
    private val settings: SettingsRepository,
    languages: LanguageRepository,
    private val localTranslation: LocalTranslation,
    private val google: GoogleTranslation,
    private val azure: AzureTranslation,
    private val alibaba: AlibabaTranslation,
    private val baidu: BaiduTranslation,
    private val deepl: DeeplTranslation,
    private val qwen: QwenTranslation,
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

    private val _azureStatus = MutableStateFlow<String?>(null)
    val azureStatus: StateFlow<String?> = _azureStatus.asStateFlow()
    private val _azureBusy = MutableStateFlow(false)
    val azureBusy: StateFlow<Boolean> = _azureBusy.asStateFlow()

    fun checkAzureKey() {
        if (_azureBusy.value) return
        _azureBusy.value = true
        viewModelScope.launch {
            _azureStatus.value = try { azure.checkKey() } catch (e: Exception) { "Microsoft Translator: ${e.message}" }
            _azureBusy.value = false
        }
    }

    private val _alibabaStatus = MutableStateFlow<String?>(null)
    val alibabaStatus: StateFlow<String?> = _alibabaStatus.asStateFlow()
    private val _alibabaBusy = MutableStateFlow(false)
    val alibabaBusy: StateFlow<Boolean> = _alibabaBusy.asStateFlow()

    fun checkAlibabaKey() {
        if (_alibabaBusy.value) return
        _alibabaBusy.value = true
        viewModelScope.launch {
            _alibabaStatus.value = try { alibaba.checkKey() } catch (e: Exception) { "Alibaba Cloud Translation: ${e.message}" }
            _alibabaBusy.value = false
        }
    }

    private val _baiduStatus = MutableStateFlow<String?>(null)
    val baiduStatus: StateFlow<String?> = _baiduStatus.asStateFlow()
    private val _baiduBusy = MutableStateFlow(false)
    val baiduBusy: StateFlow<Boolean> = _baiduBusy.asStateFlow()

    fun checkBaiduKey() {
        if (_baiduBusy.value) return
        _baiduBusy.value = true
        viewModelScope.launch {
            _baiduStatus.value = try { baidu.checkKey() } catch (e: Exception) { "Baidu Translate: ${e.message}" }
            _baiduBusy.value = false
        }
    }

    private val _deeplStatus = MutableStateFlow<String?>(null)
    val deeplStatus: StateFlow<String?> = _deeplStatus.asStateFlow()
    private val _deeplBusy = MutableStateFlow(false)
    val deeplBusy: StateFlow<Boolean> = _deeplBusy.asStateFlow()

    fun checkDeeplKey() {
        if (_deeplBusy.value) return
        _deeplBusy.value = true
        viewModelScope.launch {
            _deeplStatus.value = try { deepl.checkKey() } catch (e: Exception) { "DeepL: ${e.message}" }
            _deeplBusy.value = false
        }
    }

    private val _qwenStatus = MutableStateFlow<String?>(null)
    val qwenStatus: StateFlow<String?> = _qwenStatus.asStateFlow()
    private val _qwenBusy = MutableStateFlow(false)
    val qwenBusy: StateFlow<Boolean> = _qwenBusy.asStateFlow()

    fun checkQwenKey() {
        if (_qwenBusy.value) return
        _qwenBusy.value = true
        viewModelScope.launch {
            _qwenStatus.value = try { qwen.checkKey() } catch (e: Exception) { "Qwen-MT: ${e.message}" }
            _qwenBusy.value = false
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

    /** Source-language codes of the languages that have books, so the package list can lead with them. */
    val languagesInUse: StateFlow<Set<String>> = languages.observeSummaries()
        .map { summaries -> summaries.filter { it.bookCount > 0 }.mapNotNull { LanguageCodes.codeFor(it.name) }.toSet() }
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
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val inUse by viewModel.languagesInUse.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }
    LaunchedEffect(settings.argosPython, settings.translationEngine) { if (settings.translationEngine == TranslationEngine.ARGOS) viewModel.check() }

    Scaffold(topBar = { AppTopBar(title = "Translation", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Native language")
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

            Section("Engine")
            Dropdown(
                options = viewModel.engines,
                selected = settings.translationEngine,
                onSelect = { engine -> viewModel.update { it.copy(translationEngine = engine) } },
                label = "Translation engine",
                optionLabel = { it.label(viewModel.localName.ifEmpty { null }) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Sentence translations in the reader and term suggestions come from this engine; its own settings follow below. The online services other than MyMemory need their keys and the on-device translator its models; without them MyMemory answers instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (settings.translationEngine == TranslationEngine.MYMEMORY) {
            Section("MyMemory")
            OutlinedTextField(
                value = settings.translationContactEmail,
                onValueChange = { v -> viewModel.update { it.copy(translationContactEmail = v.trim()) } },
                label = { Text("MyMemory contact email (optional)") },
                supportingText = { Text("Raises the free daily quota from about 5,000 to 50,000 characters.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            }

            if (settings.translationEngine == TranslationEngine.GOOGLE) {
            var showKey by remember { mutableStateOf(false) }
            Section("Google Translate")
            val googleBusy by viewModel.googleBusy.collectAsStateWithLifecycle()
            val googleStatus by viewModel.googleStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.googleTranslateApiKey,
                onValueChange = { v -> viewModel.update { it.copy(googleTranslateApiKey = v.trim()) } },
                label = { Text("API key") },
                supportingText = { Text("A Google Cloud API key with the Cloud Translation API enabled. Calls are billed to that project. The key is ${viewModel.secretStorage}.") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkGoogleKey, enabled = !googleBusy && settings.googleTranslateApiKey.isNotBlank()) {
                Text(if (googleBusy) "Checking..." else "Check key")
            }
            googleStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            }

            if (settings.translationEngine == TranslationEngine.AZURE) {
            var showKey by remember { mutableStateOf(false) }
            Section("Microsoft Translator")
            val azureBusy by viewModel.azureBusy.collectAsStateWithLifecycle()
            val azureStatus by viewModel.azureStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.azureTranslatorApiKey,
                onValueChange = { v -> viewModel.update { it.copy(azureTranslatorApiKey = v.trim()) } },
                label = { Text("API key") },
                supportingText = { Text("A key of an Azure AI Translator (or multi-service) resource. Calls are billed to that resource. The key is ${viewModel.secretStorage}.") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.azureTranslatorRegion,
                onValueChange = { v -> viewModel.update { it.copy(azureTranslatorRegion = v.trim()) } },
                label = { Text("Region") },
                placeholder = { Text("westeurope") },
                supportingText = { Text("The resource's region as shown in the Azure portal, for example westeurope or eastus. Leave empty only for a global resource.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkAzureKey, enabled = !azureBusy && settings.azureTranslatorApiKey.isNotBlank()) {
                Text(if (azureBusy) "Checking..." else "Check key")
            }
            azureStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            if (settings.translationEngine == TranslationEngine.ALIBABA) {
            var showSecret by remember { mutableStateOf(false) }
            Section("Alibaba Cloud Translation")
            val alibabaBusy by viewModel.alibabaBusy.collectAsStateWithLifecycle()
            val alibabaStatus by viewModel.alibabaStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.alibabaAccessKeyId,
                onValueChange = { v -> viewModel.update { it.copy(alibabaAccessKeyId = v.trim()) } },
                label = { Text("AccessKey ID") },
                supportingText = { Text("A RAM user's AccessKey with the Machine Translation permission (AliyunMTFullAccess or read access). Both parts are ${viewModel.secretStorage}.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.alibabaAccessKeySecret,
                onValueChange = { v -> viewModel.update { it.copy(alibabaAccessKeySecret = v.trim()) } },
                label = { Text("AccessKey Secret") },
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showSecret = !showSecret }) { Text(if (showSecret) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.alibabaEndpoint,
                onValueChange = { v -> viewModel.update { it.copy(alibabaEndpoint = v.trim()) } },
                label = { Text("Endpoint") },
                placeholder = { Text("mt.aliyuncs.com") },
                supportingText = { Text("mt.aliyuncs.com works for most accounts; a regional host such as mt.cn-hangzhou.aliyuncs.com or mt.ap-southeast-1.aliyuncs.com can be used instead.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkAlibabaKey, enabled = !alibabaBusy && settings.alibabaAccessKeyId.isNotBlank() && settings.alibabaAccessKeySecret.isNotBlank()) {
                Text(if (alibabaBusy) "Checking..." else "Check AccessKey")
            }
            alibabaStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            if (settings.translationEngine == TranslationEngine.BAIDU) {
            var showSecret by remember { mutableStateOf(false) }
            Section("Baidu Translate")
            val baiduBusy by viewModel.baiduBusy.collectAsStateWithLifecycle()
            val baiduStatus by viewModel.baiduStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.baiduAppId,
                onValueChange = { v -> viewModel.update { it.copy(baiduAppId = v.trim()) } },
                label = { Text("App ID") },
                supportingText = { Text("From the Baidu Translate open platform (fanyi-api.baidu.com), general translation API. Both values are ${viewModel.secretStorage}.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = settings.baiduSecretKey,
                onValueChange = { v -> viewModel.update { it.copy(baiduSecretKey = v.trim()) } },
                label = { Text("Secret key") },
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showSecret = !showSecret }) { Text(if (showSecret) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkBaiduKey, enabled = !baiduBusy && settings.baiduAppId.isNotBlank() && settings.baiduSecretKey.isNotBlank()) {
                Text(if (baiduBusy) "Checking..." else "Check App ID")
            }
            baiduStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            if (settings.translationEngine == TranslationEngine.DEEPL) {
            var showKey by remember { mutableStateOf(false) }
            Section("DeepL")
            val deeplBusy by viewModel.deeplBusy.collectAsStateWithLifecycle()
            val deeplStatus by viewModel.deeplStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.deeplApiKey,
                onValueChange = { v -> viewModel.update { it.copy(deeplApiKey = v.trim()) } },
                label = { Text("API key") },
                supportingText = { Text("A DeepL API key (free keys end in :fx and use the free host). The key is ${viewModel.secretStorage}.") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkDeeplKey, enabled = !deeplBusy && settings.deeplApiKey.isNotBlank()) {
                Text(if (deeplBusy) "Checking..." else "Check key")
            }
            deeplStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            if (settings.translationEngine == TranslationEngine.QWEN) {
            var showKey by remember { mutableStateOf(false) }
            Section("Qwen-MT")
            val qwenBusy by viewModel.qwenBusy.collectAsStateWithLifecycle()
            val qwenStatus by viewModel.qwenStatus.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = settings.qwenApiKey,
                onValueChange = { v -> viewModel.update { it.copy(qwenApiKey = v.trim()) } },
                label = { Text("API key") },
                supportingText = { Text("An Alibaba Cloud Model Studio (DashScope) API key. Calls are billed to that account. The key is ${viewModel.secretStorage}.") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
            )
            Dropdown(
                options = listOf("qwen-mt-turbo", "qwen-mt-plus"),
                selected = settings.qwenModel,
                onSelect = { model -> viewModel.update { it.copy(qwenModel = model) } },
                label = "Model",
                optionLabel = { if (it == "qwen-mt-plus") "qwen-mt-plus (better)" else "qwen-mt-turbo (faster, cheaper)" },
                modifier = Modifier.fillMaxWidth(),
            )
            Dropdown(
                options = listOf(true, false),
                selected = settings.qwenInternational,
                onSelect = { intl -> viewModel.update { it.copy(qwenInternational = intl) } },
                label = "Region",
                optionLabel = { if (it) "International (dashscope-intl.aliyuncs.com)" else "China (dashscope.aliyuncs.com)" },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::checkQwenKey, enabled = !qwenBusy && settings.qwenApiKey.isNotBlank()) {
                Text(if (qwenBusy) "Checking..." else "Check key")
            }
            qwenStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            if (viewModel.hasLocalTranslator && settings.translationEngine == TranslationEngine.ARGOS) {
            Section(viewModel.localName)
            Text(viewModel.localDescription, style = MaterialTheme.typography.bodyMedium)

            if (viewModel.hasRuntimeSetup) Section("Runtime")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::check, enabled = !busy) { Text(if (busy) "Working..." else if (viewModel.hasRuntimeSetup) "Check installation" else "Check models") }
                if (viewModel.hasRuntimeSetup && !ready) Button(onClick = viewModel::install, enabled = !busy) { Text("Install ${viewModel.localName}") }
            }
            if (viewModel.hasRuntimeSetup && !ready) {
                Text(
                    "Install downloads a private Python into the app folder and puts argostranslate in it; nothing else on the computer is touched. Turning on offline translation in the reader does the same on its own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            progress?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            status?.let { text ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (ready) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Working", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = "Not working", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (viewModel.hasRuntimeSetup) OutlinedTextField(
                value = settings.argosPython,
                onValueChange = { v -> viewModel.update { it.copy(argosPython = v.trim()) } },
                label = { Text("Python executable (optional)") },
                placeholder = { Text("The app's own Python") },
                supportingText = { Text("Leave empty to use the Python the app downloads for itself, or give the full path to one that has argostranslate installed.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (packages.isNotEmpty()) {
                Section(if (viewModel.hasRuntimeSetup) "Language packages" else "Language models")
                Text(viewModel.localPackagesDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val native = settings.nativeLanguage.ifBlank { "en" }
                val wanted = viewModel.wantedPackages(inUse, native, packages)
                val relevant = packages.filter { it.key in wanted || it.installed }
                val shown = if (showAll || relevant.isEmpty()) packages else relevant
                // Grouped by the language translated into, the native language first.
                val groups = shown.groupBy { it.toCode }.entries.sortedWith(compareBy({ it.key != native }, { it.value.first().toName }))
                groups.forEach { (_, group) ->
                    // Per-language models all pair with English, so a heading would only repeat "English".
                    if (viewModel.hasRuntimeSetup) Text(
                        group.first().toName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    group.sortedBy { it.fromName }.forEach { pkg ->
                        PackageRow(pkg, busy = pkg.key in packageBusy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) })
                    }
                }
                if (relevant.size < packages.size) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) "Show only my languages" else "Show all ${packages.size} packages")
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PackageRow(pkg: LocalPackage, busy: Boolean, onInstall: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(pkg.title, style = MaterialTheme.typography.bodyMedium)
            val detail = when {
                busy && pkg.installed -> "Removing..."
                busy -> "Downloading..."
                pkg.installed && pkg.sizeBytes > 0 -> "Installed, ${formatSize(pkg.sizeBytes)}"
                pkg.installed -> "Installed"
                else -> "Not installed"
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
