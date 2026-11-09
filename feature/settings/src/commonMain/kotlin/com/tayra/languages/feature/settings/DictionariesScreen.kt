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

/** The dictionary the reader looks words up in, the dictionaries imported, and a test of a lookup; downloading them is the Downloads page's. */
class DictionariesViewModel(
    settings: SettingsRepository,
    private val dictionaries: DictionaryService,
    languages: LanguageRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settings.settings
    /** The dictionaries, and the word finders that are installed: the term pane offers the others. */
    val packs: StateFlow<List<PackStatus>> = dictionaries.packs
        .map { list -> list.filter { !it.pack.id.glossary || it.state is PackState.Installed || it.state is PackState.Downloading } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dictionaries.packs.value.filter { !it.pack.id.glossary })

    /** The language being learned, whose dictionaries the page leads with. */
    val learning: StateFlow<LanguageOption?> = learningLanguage(languages, settings)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

/**
 * Where an edition comes from, such as FreeDict; a Wiktionary pack is named so only when another
 * pack covers the same pair, so the two can be told apart.
 */
internal val DictionaryPack.editionLabel: String?
    get() = id.edition?.let(DictionaryPacks::editionName)
        ?: WIKTIONARY.takeIf { id.imported == null && !id.glossary && DictionaryPacks.all.any { it != this && it.id.sourceLanguage == id.sourceLanguage && it.id.targetLanguage == id.targetLanguage } }

private const val WIKTIONARY = "Wiktionary"

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
        PhoneDictionariesScreen(learning, native, nativeName, current, viewModel, onNavigate, onBack, onTest = { testing = it }, pill)
    } else Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Dictionaries"), tr("Manage offline word definitions and base-form lookup."), onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            CurrentLookupCard(learning, nativeName, current, onDownloads = { onNavigate(Route.Downloads) }, onTest = { testing = it })
            DownloadsLinkCard(dictionariesInDownloads(), onNavigate)
            if (viewModel.canImport) ImportDictionaryCard(viewModel, learning, native)
        }
    }
    testing?.let { pack -> TestLookupDialog(pack, viewModel, onDismiss = { testing = null }) }
}

/** The pair the reader looks words up in, and whether its dictionary is on the device. */
/** What the Downloads page offers in place of this page's own downloads. */
internal fun dictionariesInDownloads(): String = tr("Download or remove dictionaries and text recognition models on the Downloads page.")

@Composable
private fun CurrentLookupCard(learning: LanguageOption?, nativeName: String, current: PackStatus?, onDownloads: () -> Unit, onTest: (DictionaryPack) -> Unit) {
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
            else -> PrimaryButton(tr("Go to Downloads"), onClick = onDownloads)
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

private val TEST_LOOKUP_MIN_WIDTH = 420.dp
private val LOOKUP_RESULT_MAX_HEIGHT = 320.dp

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
