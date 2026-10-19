package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LanguagePickerSheet
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PackageTab
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TabToggle
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes

/**
 * The Translation page on a phone: the language pair as two chips, the engine picked from a sheet
 * with its status under it, and the packages the pair needs with one button to install them all;
 * the whole catalogue is a sheet behind "Browse all".
 */
@Composable
internal fun PhoneTranslationScreen(
    settings: UserSettings,
    viewModel: OfflineTranslationViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    local: Boolean,
    checking: Boolean,
    ready: Boolean,
    pair: PairState,
    packages: List<LocalPackage>,
    wanted: Set<String>,
    learningName: String?,
    nativeName: String,
) {
    val colors = MaterialTheme.colorScheme
    val learningEnglish = LocalLearningLanguage.current?.currentName
    val learningCode = learningEnglish?.let(LanguageCodes::codeFor)
    var nativeSheet by remember { mutableStateOf(false) }
    var engineSheet by remember { mutableStateOf(false) }
    var browse by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Translation"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Choose how translations are generated and stored."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                EngineStatusPill(settings, viewModel, local, checking, ready, pair)
            }
            ContentCard(tr("Language pair"), subtitle = null, icon = AppIcons.SwapHoriz, dense = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageChip(learningEnglish, learningName ?: tr("Your language"), Modifier.weight(1f), enabled = false)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("translated into"), tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    LanguageChip(LanguageCatalog.nativeOption(settings.nativeLanguage).name, nativeName, Modifier.weight(1f).testTag("phone-native")) { nativeSheet = true }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    learningName?.let { tr("{0} terms will be translated into {1}.", it, nativeName) } ?: tr("Your terms will be translated into {0}.", nativeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            ContentCard(tr("Translation engine"), subtitle = null, icon = AppIcons.Memory, dense = true) {
                val engine = settings.translationEngine
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(colors.outline.copy(alpha = 0.5f)), AppShapes.control)
                        .clickable { engineSheet = true }.padding(horizontal = 14.dp, vertical = 14.dp).testTag("phone-engine"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(engine.shortName(viewModel.localName), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (local) Tag(tr("Offline"), StatusTints.ok) else Tag(tr("Online"), colors.primary)
                    if (engine == TranslationEngine.GOOGLE) Tag(tr("Paid"), StatusTints.warning) else Tag(tr("Free"), colors.primary)
                }
                Spacer(Modifier.height(12.dp))
                when {
                    local -> PhoneEngineStatus(settings, viewModel)
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
            if (local && (packages.isNotEmpty() || ready)) {
                PhonePackagesCard(viewModel, packages, wanted, pair, onBrowse = { browse = true })
            }
        }
    }
    if (nativeSheet) {
        LanguagePickerSheet(
            tr("Native language"), LanguageCatalog.nativeLanguages, selected = LanguageCatalog.nativeOption(settings.nativeLanguage),
            name = { tr(it.name) }, flagName = { it.name },
            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
            onDismiss = { nativeSheet = false },
            onPick = { option -> nativeSheet = false; viewModel.update { it.copy(nativeLanguage = option.code) } },
        )
    }
    if (engineSheet) {
        PickerSheet(
            tr("Translation engine"), viewModel.engines, selected = settings.translationEngine,
            label = { it.displayLabel(viewModel.localName.ifEmpty { null }) },
            onDismiss = { engineSheet = false },
            onPick = { e -> engineSheet = false; viewModel.update { it.copy(translationEngine = e) } },
        )
    }
    if (browse) PackagesSheet(viewModel, packages, wanted, onDismiss = { browse = false })
}

/** A language with its flag in a bordered box; one that [enabled] opens a choice shows a chevron, one that cannot be changed is tinted. */
@Composable
private fun LanguageChip(flagName: String?, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.clip(AppShapes.control)
            .background(if (enabled) colors.surface else colors.surfaceContainerLow)
            .border(AppBorders.hairline(if (enabled) colors.outline.copy(alpha = 0.5f) else colors.outlineVariant), AppShapes.control)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flagName != null) {
            LanguageFlag(flagName, 24.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (enabled) Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Whether the on-device translator works, with the button to check or install it beside; the runtime path field is left to the desktop. */
@Composable
private fun PhoneEngineStatus(settings: UserSettings, viewModel: OfflineTranslationViewModel) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val tint = when {
        busy -> colors.outline
        ready -> StatusTints.ok
        else -> StatusTints.warning
    }
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.panel).background(tint.copy(alpha = 0.08f)).border(AppBorders.hairline(tint.copy(alpha = 0.25f)), AppShapes.panel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                busy -> AppSpinner(size = SpinnerSize.Medium)
                ready -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
                else -> Icon(Icons.Default.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        busy -> tr("Checking…")
                        ready -> tr("Installed and working")
                        else -> tr("Not installed")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                val detail = status?.let { tr(it) } ?: if (!ready && viewModel.hasRuntimeSetup) tr("Install downloads a private Python into the app folder with argostranslate in it.") else null
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            Spacer(Modifier.width(10.dp))
            if (viewModel.hasRuntimeSetup && !ready && !busy) {
                PrimaryButton(tr("Install"), onClick = viewModel::install)
            } else {
                SecondaryButton(onClick = viewModel::check, enabled = !busy) {
                    Text(if (viewModel.hasRuntimeSetup) tr("Check installation") else tr("Check models"), maxLines = 1)
                }
            }
        }
        progress?.let {
            Text(tr(it), style = MaterialTheme.typography.bodySmall)
            AppProgressBar(null)
        }
    }
}

/** What the language pair still needs, with one button for all of it, and the way into the whole catalogue. */
@Composable
private fun PhonePackagesCard(viewModel: OfflineTranslationViewModel, packages: List<LocalPackage>, wanted: Set<String>, pair: PairState, onBrowse: () -> Unit) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val runtime = viewModel.hasRuntimeSetup
    val installed = packages.filter { it.installed }
    ContentCard(if (runtime) tr("Required packages") else tr("Required models"), subtitle = null, icon = AppIcons.Download, dense = true, headerExtra = { Tag(tr("{0} installed", installed.size), colors.primary) }) {
        if (pair.learning != null && !pair.sameLanguage) {
            if (pair.ready) {
                NoticeBox(Icons.Default.CheckCircle, StatusTints.ok, tr("{0} is ready", pair.label))
                val needed = packages.filter { it.key in wanted }
                needed.forEach { PackageLine(it) }
            } else {
                // Two packages mean the pair goes through English.
                if (pair.missing.size > 1) NoticeBox(Icons.Default.Warning, StatusTints.warning, tr("{0} translates through English.", pair.label))
                pair.missing.forEach { PackageLine(it) }
                val installing = pair.missing.any { it.key in packageBusy }
                val size = pair.missing.sumOf { it.sizeBytes }
                PrimaryButton(
                    onClick = { pair.missing.forEach(viewModel::installPackage) },
                    enabled = !installing,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("phone-install-missing"),
                ) {
                    Text(
                        when {
                            installing -> tr("Installing…")
                            runtime && size > 0 -> trPlural(pair.missing.size, "Install {0} package · {1}", "Install {0} packages · {1}", formatSize(size))
                            runtime -> trPlural(pair.missing.size, "Install {0} package", "Install {0} packages")
                            size > 0 -> trPlural(pair.missing.size, "Install {0} model · {1}", "Install {0} models · {1}", formatSize(size))
                            else -> trPlural(pair.missing.size, "Install {0} model", "Install {0} models")
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else if (installed.isEmpty()) {
            Text(if (runtime) tr("No packages installed yet.") else tr("No models installed yet."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
            QuietButton(onClick = onBrowse, modifier = Modifier.testTag("phone-browse-packages")) {
                Text(if (runtime) tr("Browse all language packages") else tr("Browse all language models"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun NoticeBox(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).background(tint.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 10.dp).padding(bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
}

/** One package the pair needs: its name and size, or that it is installed. */
@Composable
private fun PackageLine(pkg: LocalPackage) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(pkg.displayTitle, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (pkg.installed) tr("Installed") else pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize).orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (pkg.installed) StatusTints.ok else colors.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))
}

/** The whole catalogue in a sheet: a search, Installed or Available, and a tile per package to install or remove. */
@Composable
private fun PackagesSheet(viewModel: OfflineTranslationViewModel, packages: List<LocalPackage>, wanted: Set<String>, onDismiss: () -> Unit) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val runtime = viewModel.hasRuntimeSetup
    var tab by remember { mutableStateOf(PackageTab.AVAILABLE) }
    var query by remember { mutableStateOf("") }
    val matches = { pkg: LocalPackage -> query.isBlank() || pkg.title.contains(query.trim(), ignoreCase = true) || pkg.displayTitle.contains(query.trim(), ignoreCase = true) }
    val shown = when (tab) {
        PackageTab.INSTALLED -> packages.filter { it.installed }.sortedBy { it.title }
        PackageTab.AVAILABLE -> packages.filter { !it.installed }.sortedWith(compareBy({ it.key !in wanted }, { it.title }))
    }.filter(matches)
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (runtime) tr("Language packages") else tr("Language models"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            SearchField(query, { query = it }, if (runtime) tr("Search packages") else tr("Search models"), Modifier.fillMaxWidth())
            TabToggle(tab, fill = true) { tab = it }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shown.isEmpty()) {
                    Text(
                        when {
                            query.isNotBlank() -> if (runtime) tr("No packages match “{0}”.", query.trim()) else tr("No models match “{0}”.", query.trim())
                            tab == PackageTab.INSTALLED -> if (runtime) tr("No packages installed yet.") else tr("No models installed yet.")
                            else -> if (runtime) tr("All packages are installed.") else tr("All models are installed.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                shown.forEach { pkg ->
                    PackageTile(pkg, busy = pkg.key in packageBusy, needed = pkg.key in wanted, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
