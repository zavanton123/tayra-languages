package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.flow.SharingStarted
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.tayra.languages.core.ui.components.EdgeScrollbar
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.NoTextRecognizer
import androidx.compose.ui.text.style.TextOverflow

/** The downloadable offline dictionary packs and their state on this device. */
class DictionariesViewModel(
    settings: SettingsRepository,
    private val dictionaries: DictionaryService,
    languages: LanguageRepository,
    private val recognizer: TextRecognizer = NoTextRecognizer,
) : ViewModel() {
    private val _ocrPacks = MutableStateFlow<List<OcrPack>>(emptyList())

    /** The text recognition models downloaded for reading photos of pages. */
    val ocrPacks: StateFlow<List<OcrPack>> = _ocrPacks.asStateFlow()

    init {
        viewModelScope.launch { _ocrPacks.value = recognizer.installedPacks() }
    }

    fun removeOcr(pack: OcrPack) = viewModelScope.launch {
        recognizer.remove(pack)
        _ocrPacks.value = recognizer.installedPacks()
    }

    val settings: StateFlow<UserSettings> = settings.settings
    /** The dictionaries, and the word finders that are installed: the term pane offers the others. */
    val packs: StateFlow<List<PackStatus>> = dictionaries.packs
        .map { list -> list.filter { !it.pack.id.glossary || it.state is PackState.Installed || it.state is PackState.Downloading } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dictionaries.packs.value.filter { !it.pack.id.glossary })

    /** The language being learned, whose dictionaries the page leads with. */
    val learning: StateFlow<LanguageOption?> = learningLanguage(languages, settings)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun download(pack: DictionaryPack) = viewModelScope.launch { dictionaries.download(pack) }
    fun remove(pack: DictionaryPack) = viewModelScope.launch { dictionaries.remove(pack) }

    /** What the installed pack says about [word]. */
    suspend fun lookup(pack: DictionaryPack, word: String): DictionaryLookup = dictionaries.lookup(pack.id, word.trim())

    val canImport: Boolean get() = dictionaries.canImport

    private val _import = MutableStateFlow<DictionaryImport>(DictionaryImport.Idle)

    /** How far importing a dictionary file has got. */
    val import: StateFlow<DictionaryImport> = _import.asStateFlow()

    /** Reads the picked file's index, so the user can say which languages it is for. */
    fun pickImport(file: ByteArray) = viewModelScope.launch {
        _import.value = DictionaryImport.Reading
        _import.value = try {
            DictionaryImport.Ready(dictionaries.inspect(file), file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DictionaryImport.Failed(e.message ?: "Not a dictionary this app can import")
        }
    }

    fun startImport(sourceLanguage: String, targetLanguage: String) {
        val ready = _import.value as? DictionaryImport.Ready ?: return
        viewModelScope.launch {
            _import.value = DictionaryImport.Importing(ready.info.title, 0f)
            _import.value = try {
                val imported = dictionaries.import(ready.file, sourceLanguage, targetLanguage) { progress ->
                    _import.value = DictionaryImport.Importing(ready.info.title, progress)
                }
                DictionaryImport.Done(imported.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DictionaryImport.Failed(e.message ?: "The import failed")
            }
        }
    }

    fun dismissImport() {
        _import.value = DictionaryImport.Idle
    }
}

/** Importing a dictionary file, from picking it to its pack being installed. */
sealed interface DictionaryImport {
    data object Idle : DictionaryImport
    data object Reading : DictionaryImport
    class Ready(val info: ImportedDictionaryInfo, val file: ByteArray) : DictionaryImport
    data class Importing(val title: String, val progress: Float) : DictionaryImport
    data class Done(val title: String) : DictionaryImport
    data class Failed(val message: String) : DictionaryImport
}

/** The pack's source and target language names, from its "Source → Target" title; an imported one's title is its own. */
internal val DictionaryPack.sourceName: String
    get() = if (id.imported != null) DictionaryPacks.languageName(id.sourceLanguage) else title.substringBefore(" →")
internal val DictionaryPack.targetName: String
    get() = if (id.imported != null) DictionaryPacks.languageName(id.targetLanguage) else title.substringAfter("→ ", id.targetLanguage)

/** The pack's title with the language names in the interface language. */
internal val DictionaryPack.displayTitle: String
    get() = when {
        id.imported != null -> "$title \u00b7 ${tr(sourceName)} \u2192 ${tr(targetName)}"
        id.glossary -> "${tr(sourceName)} \u2192 ${tr(targetName)} \u00b7 " + tr("word finder")
        editionLabel != null -> "${tr(sourceName)} \u2192 ${tr(targetName)} \u00b7 $editionLabel"
        else -> "${tr(sourceName)} \u2192 ${tr(targetName)}"
    }

/** Where an edition comes from, such as FreeDict; null for the Wiktionaries' packs and the others. */
internal val DictionaryPack.editionLabel: String?
    get() = id.edition?.let(DictionaryPacks::editionName)

internal fun DictionaryPack.matches(query: String): Boolean =
    query.isBlank() || title.contains(query.trim(), ignoreCase = true) || displayTitle.contains(query.trim(), ignoreCase = true)

/**
 * A pack download failure as the user reads it: the messages the app writes itself in the
 * interface language, anything else (the network's own errors) as reported.
 */
internal fun packFailure(message: String): String =
    SERVER_ANSWERED.matchEntire(message)?.let { tr("Server answered {0}", it.groupValues[1]) } ?: tr(message)

private val SERVER_ANSWERED = Regex("""Server answered (\d+)""")

@Composable
fun DictionariesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: DictionariesViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val learning by viewModel.learning.collectAsStateWithLifecycle()
    var testing by remember { mutableStateOf<DictionaryPack?>(null) }
    val native = settings.nativeLanguage.ifBlank { "en" }
    val nativeName = LanguageCatalog.nativeOption(native).name
    // The pair's dictionary, or another edition of the pair when that one is installed instead.
    val current = learning?.let { DictionaryPacks.find(it.code, native) }?.let { pack ->
        packs.firstOrNull { it.pack.id == pack.id && it.state is PackState.Installed }
            ?: packs.firstOrNull { it.pack.id.copy(edition = null) == pack.id.copy(edition = null) && it.state is PackState.Installed }
            ?: packs.firstOrNull { it.pack.id == pack.id } ?: PackStatus(pack, PackState.NotInstalled)
    }
    val compact = LocalWindowWidth.current.isCompact
    val pill: @Composable () -> Unit = {
        when {
            current?.state is PackState.Installed -> StatusPill(tr("Offline lookup ready"), StatusTints.ok)
            current != null -> StatusPill(tr("Dictionary not downloaded"), StatusTints.warning)
            else -> StatusPill(tr("Online lookup only"), MaterialTheme.colorScheme.outline)
        }
    }

    if (compact) {
        PhoneDictionariesScreen(packs, learning, native, nativeName, current, viewModel, onNavigate, onBack, onTest = { testing = it }, pill)
    } else Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Dictionaries"), tr("Manage offline word definitions and base-form lookup."), onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            CurrentLookupCard(learning, nativeName, current, onDownload = { viewModel.download(it) }, onTest = { testing = it })
            if (LocalWindowWidth.current.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    InstalledCard(packs, nativeName, viewModel, Modifier.weight(1.6f).fillMaxHeight())
                    AvailableCard(packs, learning, native, viewModel, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                InstalledCard(packs, nativeName, viewModel)
                AvailableCard(packs, learning, native, viewModel)
            }
            if (viewModel.canImport) ImportDictionaryCard(viewModel, learning, native)
            TextRecognitionCard(viewModel)
        }
    }
    testing?.let { pack -> TestLookupDialog(pack, viewModel, onDismiss = { testing = null }) }
}

/** The pair the reader looks words up in, and whether its dictionary is on the device. */
@Composable
private fun CurrentLookupCard(learning: LanguageOption?, nativeName: String, current: PackStatus?, onDownload: (DictionaryPack) -> Unit, onTest: (DictionaryPack) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    val state = current?.state
    val pair: @Composable () -> Unit = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(AppIcons.MenuBook)
                Spacer(Modifier.width(AppSpacing.l))
                Text(tr("Current lookup"), style = AppText.sectionTitle)
            }
            Spacer(Modifier.height(AppSpacing.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PairChip(learning?.code ?: "?", learning?.let { tr(it.name) } ?: tr("No language"))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("looked up in"), modifier = Modifier.padding(horizontal = AppSpacing.m).size(AppSizes.icon))
                PairChip(LanguageCatalog.nativeLanguages.firstOrNull { it.name == nativeName }?.code ?: "", tr(nativeName))
            }
        }
    }
    val action: @Composable () -> Unit = {
        when {
            current == null -> {}
            state is PackState.Installed -> SecondaryButton(tr("Test lookup"), onClick = { onTest(current.pack) })
            state is PackState.Downloading -> {}
            else -> PrimaryButton(tr("Download"), onClick = { onDownload(current.pack) })
        }
    }
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(AppSpacing.xl),
    ) {
        if (compact) {
            pair()
            Spacer(Modifier.height(AppSpacing.l))
            LookupStatusRow(current, short = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.m))
            action()
        } else {
            Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                pair()
                AppVerticalDivider(Modifier.padding(horizontal = AppSpacing.xxxl).fillMaxHeight())
                LookupStatusRow(current, short = false, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(AppSpacing.xl))
                action()
            }
        }
    }
}

private data class LookupStatus(val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: androidx.compose.ui.graphics.Color, val title: String, val text: String)

/** Where the pair's dictionary stands, with the download's progress; [short] is the phone's wording. */
@Composable
internal fun LookupStatusRow(current: PackStatus?, short: Boolean, modifier: Modifier = Modifier) {
    val state = current?.state
    val (icon, tint, title, text) = when {
        state is PackState.Installed -> LookupStatus(
            Icons.Default.CheckCircle, StatusTints.ok, tr("Dictionary ready"),
            if (short) tr("Works offline and links inflected forms to their base word.")
            else tr("Downloaded dictionaries work without a network connection and link inflected forms to their base word. Online dictionaries remain available as a fallback."),
        )
        state is PackState.Downloading -> LookupStatus(Icons.Default.Info, MaterialTheme.colorScheme.primary, tr("Downloading the dictionary…"), tr("It works offline as soon as the download finishes."))
        state is PackState.Failed -> LookupStatus(Icons.Default.Warning, MaterialTheme.colorScheme.error, tr("Download failed"), packFailure(state.message))
        current != null -> LookupStatus(
            Icons.Default.Warning, StatusTints.warning, tr("{0} is not downloaded", current.pack.displayTitle),
            if (short) tr("Online dictionaries are used until you download it.")
            else tr("Download it to look words up offline and find their base forms. Until then the online dictionaries are used."),
        )
        else -> LookupStatus(Icons.Default.Info, MaterialTheme.colorScheme.outline, tr("No offline dictionary for this pair"), tr("Words are looked up in the online dictionaries set up for the language."))
    }
    Row(modifier, verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconXLarge))
        Spacer(Modifier.width(AppSpacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.itemTitle, color = if (state is PackState.Installed) tint else MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state is PackState.Downloading) {
                AppProgressBar(state.progress, Modifier.padding(top = AppSpacing.s))
            }
        }
    }
}

/** A language of the looked-up pair: its code tile and its name. */
@Composable
internal fun PairChip(code: String, name: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(AppShapes.control).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(AppBorders.hairline(), AppShapes.control).padding(AppSpacing.xs).padding(end = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(code, size = 36)
        Spacer(Modifier.width(AppSpacing.m))
        Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** How many installed rows show before "View all". */
private const val INSTALLED_PREVIEW = 8

private val INSTALLED_ROW_HEIGHT = 52.dp
private val INSTALLED_SEARCH_MIN_WIDTH = 280.dp
private val INSTALLED_SIZE_COLUMN = 100.dp
private val CATALOGUE_MAX_HEIGHT = 520.dp
private val TEST_LOOKUP_MIN_WIDTH = 420.dp
private val LOOKUP_RESULT_MAX_HEIGHT = 320.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstalledCard(packs: List<PackStatus>, nativeName: String, viewModel: DictionariesViewModel, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val installed = packs.filter { it.state is PackState.Installed || it.state is PackState.Downloading }
    // Meanings in the native language come first; other target languages are one click away.
    val targets = installed.map { it.pack.targetName }.distinct().sortedWith(compareBy({ it != nativeName }, { it }))
    var target by remember { mutableStateOf<String?>(null) }
    val chosen = target?.takeIf { it in targets } ?: targets.firstOrNull()
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val matching = installed.filter { it.pack.targetName == chosen && it.pack.matches(query) }
        .sortedBy { it.pack.title }
    val shown = if (showAll || query.isNotBlank()) matching else matching.take(INSTALLED_PREVIEW)

    ContentCard(tr("Installed dictionaries"), tr("Manage downloaded dictionaries for offline lookup."), icon = AppIcons.Storage, modifier = modifier) {
        if (installed.isEmpty()) {
            Text(
                tr("No dictionaries downloaded yet. Download one on the right to look words up offline."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.s),
            )
            return@ContentCard
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m), itemVerticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query,
                { query = it },
                tr("Search installed dictionaries"),
                if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = INSTALLED_SEARCH_MIN_WIDTH).weight(1f),
            )
            if (targets.size > 1) SegmentedToggle(targets.map { Segment(it, label = tr(it)) }, chosen ?: targets.first(), onSelect = { target = it })
        }
        Spacer(Modifier.height(AppSpacing.m))
        val bytes = matching.sumOf { (it.state as? PackState.Installed)?.sizeBytes ?: 0L }
        InfoBanner(
            trPlural(matching.size, "{0} dictionary • {1} available offline", "{0} dictionaries • {1} available offline", formatSize(bytes)),
            icon = AppIcons.Storage,
            dense = true,
        )
        Spacer(Modifier.height(AppSpacing.s))
        Column(Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control)) {
            shown.forEachIndexed { i, status ->
                if (i > 0) AppDivider()
                InstalledRow(status, onRemove = { viewModel.remove(status.pack) })
            }
            if (shown.isEmpty()) {
                Text(tr("No dictionaries match “{0}”.", query.trim()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(AppSpacing.l))
            }
        }
        if (matching.size > INSTALLED_PREVIEW && query.isBlank()) {
            Spacer(Modifier.height(AppSpacing.l))
            SecondaryButton(
                if (showAll) tr("Show fewer") else tr("View all {0} installed dictionaries", matching.size),
                onClick = { showAll = !showAll },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}

@Composable
internal fun InstalledRow(status: PackStatus, onRemove: () -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    Row(Modifier.fillMaxWidth().heightIn(min = INSTALLED_ROW_HEIGHT).padding(start = AppSpacing.s, end = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        CodeTile(status.pack.id.sourceLanguage, size = 36)
        Spacer(Modifier.width(AppSpacing.l))
        Text(status.pack.displayTitle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val state = status.state
        if (state is PackState.Installed && !compact) {
            Text(formatSize(state.sizeBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(INSTALLED_SIZE_COLUMN))
        }
        if (state is PackState.Downloading) {
            Text(tr("Downloading…"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = AppSpacing.m))
        } else {
            // The list is headed Installed; its rows need not say so each.
            OverflowMenu(tr("More for {0}", status.pack.displayTitle)) { close ->
                AppMenuItem(text = { Text(tr("Remove")) }, onClick = { close(); onRemove() })
            }
        }
    }
}

/**
 * Packs to download: those for the language being learned, the native language first; "Browse all"
 * opens every pack not on the device, with a search.
 */
@Composable
private fun AvailableCard(packs: List<PackStatus>, learning: LanguageOption?, native: String, viewModel: DictionariesViewModel, modifier: Modifier = Modifier) {
    var browsing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val missing = packs.filter { it.state !is PackState.Installed }
    val forLearning = missing.filter { it.pack.id.sourceLanguage == learning?.code }
        .sortedWith(compareBy({ it.pack.id.targetLanguage != native }, { it.pack.title }))
    val shown = if (browsing || learning == null) missing.filter { it.pack.matches(query) } else forLearning
    ContentCard(
        if (browsing || learning == null) tr("All dictionaries") else tr("Available for {0}", tr(learning.name)),
        if (browsing || learning == null) tr("Every dictionary not on this device.") else tr("Add meanings in another language."),
        icon = AppIcons.Download,
        modifier = modifier,
    ) {
        if (browsing || learning == null) {
            SearchField(query, { query = it }, tr("Search dictionaries"), Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.m))
        }
        if (shown.isEmpty()) {
            Text(
                when {
                    query.isNotBlank() -> tr("No dictionaries match “{0}”.", query.trim())
                    learning == null -> tr("Every dictionary is downloaded.")
                    else -> tr("Every {0} dictionary is downloaded.", tr(learning.name))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.s),
            )
        }
        // The full catalogue is long, so it scrolls inside the card.
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxWidth().then(if (browsing) Modifier.heightIn(max = CATALOGUE_MAX_HEIGHT) else Modifier)) {
            val list = Modifier.fillMaxWidth().then(if (browsing) Modifier.verticalScroll(scroll).padding(end = AppSpacing.l) else Modifier)
            Column(list, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                shown.forEach { status -> AvailableRow(status, browsing, onDownload = { viewModel.download(status.pack) }) }
            }
            if (browsing) EdgeScrollbar(scroll)
        }
        Spacer(Modifier.height(AppSpacing.l))
        InfoBanner(tr("One pack covers one source language with meanings in one language."))
        if (learning != null) {
            Spacer(Modifier.height(AppSpacing.l))
            SecondaryButton(
                if (browsing) tr("Show {0} dictionaries", tr(learning.name)) else tr("Browse all {0} dictionaries", DictionaryPacks.all.size),
                onClick = { browsing = !browsing; query = "" },
                modifier = Modifier.fillMaxWidth(),
                icon = AppIcons.OpenInNew,
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}

@Composable
private fun AvailableRow(status: PackStatus, browsing: Boolean, onDownload: () -> Unit) {
    val state = status.state
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control).padding(AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // For one source language the target is what tells the packs apart.
        CodeTile(if (browsing) status.pack.id.sourceLanguage else status.pack.id.targetLanguage, size = 40)
        Spacer(Modifier.width(AppSpacing.l))
        Column(Modifier.weight(1f)) {
            Text(status.pack.displayTitle, style = MaterialTheme.typography.bodyLarge)
            Text(
                when (state) {
                    is PackState.Downloading -> tr("Downloading…")
                    is PackState.Failed -> tr("Download failed: {0}", packFailure(state.message))
                    else -> tr("Not downloaded")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state is PackState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state is PackState.Downloading) {
                AppProgressBar(state.progress, Modifier.padding(top = AppSpacing.xs))
            }
        }
        Spacer(Modifier.width(AppSpacing.m))
        if (state !is PackState.Downloading) {
            SecondaryButton(if (state is PackState.Failed) tr("Retry") else tr("Download"), onClick = onDownload)
        }
    }
}

/** Looks a word up in the pack, to check what the reader will show for it. */
@Composable
private fun TestLookupDialog(pack: DictionaryPack, viewModel: DictionariesViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var word by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Pair<String, DictionaryLookup>?>(null) }
    val search = { if (word.isNotBlank()) scope.launch { result = word.trim() to viewModel.lookup(pack, word) } }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Test lookup")) },
        text = {
            Column(Modifier.widthIn(min = TEST_LOOKUP_MIN_WIDTH), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("Type a {0} word to see what {1} says about it.", tr(pack.sourceName), pack.displayTitle), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        value = word,
                        onValueChange = { word = it },
                        placeholder = { Text(tr("A {0} word", tr(pack.sourceName))) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(AppSpacing.m))
                    PrimaryButton(tr("Look up"), onClick = { search() }, enabled = word.isNotBlank())
                }
                result?.let { (looked, lookup) ->
                    if (lookup.isEmpty) {
                        Text(tr("No entry for “{0}”.", looked), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ScrollColumn(Modifier.heightIn(max = LOOKUP_RESULT_MAX_HEIGHT), contentModifier = Modifier.padding(end = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                            if (lookup.lemmas.isNotEmpty()) Text(tr("A form of {0}", lookup.lemmas.joinToString()), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                            lookup.entries.take(4).forEach { entry ->
                                Column {
                                    Text(
                                        listOfNotNull(entry.word, entry.pos, entry.ipa).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = AppText.smallTitle,
                                    )
                                    entry.senses.take(3).forEachIndexed { i, sense ->
                                        Text("${i + 1}. ${sense.glosses.joinToString("; ")}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { QuietButton(tr("Close"), onClick = onDismiss) },
    )
}
