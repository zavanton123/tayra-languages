package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** The downloadable offline dictionary packs and their state on this device. */
class DictionariesViewModel(
    settings: SettingsRepository,
    private val dictionaries: DictionaryService,
    languages: LanguageRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settings.settings
    val packs: StateFlow<List<PackStatus>> = dictionaries.packs

    /** The language being learned, whose dictionaries the page leads with. */
    val learning: StateFlow<LanguageOption?> = learningLanguage(languages, settings)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun download(pack: DictionaryPack) = viewModelScope.launch { dictionaries.download(pack) }
    fun remove(pack: DictionaryPack) = viewModelScope.launch { dictionaries.remove(pack) }

    /** What the installed pack says about [word]. */
    suspend fun lookup(pack: DictionaryPack, word: String): DictionaryLookup = dictionaries.lookup(pack.id, word.trim())
}

/** The pack's source and target language names, from its "Source → Target" title. */
private val DictionaryPack.sourceName: String get() = title.substringBefore(" →")
private val DictionaryPack.targetName: String get() = title.substringAfter("→ ", id.targetLanguage)

@Composable
fun DictionariesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: DictionariesViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    val learning by viewModel.learning.collectAsStateWithLifecycle()
    var testing by remember { mutableStateOf<DictionaryPack?>(null) }
    val native = settings.nativeLanguage.ifBlank { "en" }
    val nativeName = LanguageCatalog.nativeOption(native).name
    val current = learning?.let { DictionaryPacks.find(it.code, native) }?.let { pack -> packs.firstOrNull { it.pack.id == pack.id } ?: PackStatus(pack, PackState.NotInstalled) }
    val compact = LocalWindowWidth.current.isCompact
    val pill: @Composable () -> Unit = {
        when {
            current?.state is PackState.Installed -> StatusPill("Offline lookup ready", StatusTints.ok)
            current != null -> StatusPill("Dictionary not downloaded", StatusTints.warning)
            else -> StatusPill("Online lookup only", MaterialTheme.colorScheme.outline)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader("Dictionaries", "Manage offline word definitions and base-form lookup.", onBackToSettings = onBack) { if (!compact) pill() }
            if (compact) pill()
            CurrentLookupCard(learning, nativeName, current, onDownload = { viewModel.download(it) }, onTest = { testing = it })
            if (LocalWindowWidth.current.isExpanded) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    InstalledCard(packs, nativeName, viewModel, Modifier.weight(1.6f).fillMaxHeight())
                    AvailableCard(packs, learning, native, viewModel, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                InstalledCard(packs, nativeName, viewModel)
                AvailableCard(packs, learning, native, viewModel)
            }
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
                Spacer(Modifier.width(16.dp))
                Text("Current lookup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageChip(learning?.code ?: "?", learning?.name ?: "No language")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "looked up in", modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
                LanguageChip(LanguageCatalog.nativeLanguages.firstOrNull { it.name == nativeName }?.code ?: "", nativeName)
            }
        }
    }
    val status: @Composable (Modifier) -> Unit = { modifier ->
        val (icon, tint, title, text) = when {
            state is PackState.Installed -> LookupStatus(Icons.Default.CheckCircle, StatusTints.ok, "Dictionary ready", "Downloaded dictionaries work without a network connection and link inflected forms to their base word. Online dictionaries remain available as a fallback.")
            state is PackState.Downloading -> LookupStatus(Icons.Default.Info, MaterialTheme.colorScheme.primary, "Downloading the dictionary…", "It works offline as soon as the download finishes.")
            state is PackState.Failed -> LookupStatus(Icons.Default.Warning, MaterialTheme.colorScheme.error, "Download failed", state.message)
            current != null -> LookupStatus(Icons.Default.Warning, StatusTints.warning, "${current.pack.title} is not downloaded", "Download it to look words up offline and find their base forms. Until then the online dictionaries are used.")
            else -> LookupStatus(Icons.Default.Info, MaterialTheme.colorScheme.outline, "No offline dictionary for this pair", "Words are looked up in the online dictionaries set up for the language.")
        }
        Row(modifier, verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (state is PackState.Installed) tint else MaterialTheme.colorScheme.onSurface)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state is PackState.Downloading) {
                    val progress = state.progress
                    if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    else LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
    }
    val action: @Composable () -> Unit = {
        when {
            current == null -> {}
            state is PackState.Installed -> OutlinedButton(onClick = { onTest(current.pack) }, shape = RoundedCornerShape(50)) { Text("Test lookup") }
            state is PackState.Downloading -> {}
            else -> Button(onClick = { onDownload(current.pack) }, shape = RoundedCornerShape(50)) { Text("Download") }
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(20.dp),
    ) {
        if (compact) {
            pair()
            Spacer(Modifier.height(16.dp))
            status(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            action()
        } else {
            Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                pair()
                Box(Modifier.padding(horizontal = 32.dp).width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                status(Modifier.weight(1f))
                Spacer(Modifier.width(20.dp))
                action()
            }
        }
    }
}

private data class LookupStatus(val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: androidx.compose.ui.graphics.Color, val title: String, val text: String)

@Composable
private fun LanguageChip(code: String, name: String) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(6.dp).padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(code, size = 36)
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

/** How many installed rows show before "View all". */
private const val INSTALLED_PREVIEW = 8

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
    val matching = installed.filter { it.pack.targetName == chosen && (query.isBlank() || it.pack.title.contains(query.trim(), ignoreCase = true)) }
        .sortedBy { it.pack.title }
    val shown = if (showAll || query.isNotBlank()) matching else matching.take(INSTALLED_PREVIEW)

    ContentCard("Installed dictionaries", "Manage downloaded dictionaries for offline lookup.", icon = AppIcons.Storage, modifier = modifier) {
        if (installed.isEmpty()) {
            Text(
                "No dictionaries downloaded yet. Download one on the right to look words up offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            return@ContentCard
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search installed dictionaries") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 280.dp).weight(1f),
            )
            if (targets.size > 1) Segmented(targets, chosen) { target = it }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            val size = matching.sumOf { (it.state as? PackState.Installed)?.sizeBytes ?: 0L }
            Text(
                "${matching.size} dictionar${if (matching.size == 1) "y" else "ies"} • ${formatSize(size)} available offline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))) {
            shown.forEachIndexed { i, status ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                InstalledRow(status, onRemove = { viewModel.remove(status.pack) })
            }
            if (shown.isEmpty()) {
                Text("No dictionaries match “${query.trim()}”.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }
        if (matching.size > INSTALLED_PREVIEW && query.isBlank()) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { showAll = !showAll }, shape = RoundedCornerShape(10.dp)) {
                Text(if (showAll) "Show fewer" else "View all ${matching.size} installed dictionaries")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(3.dp)) {
        options.forEach { option ->
            val active = option == selected
            Box(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun InstalledRow(status: PackStatus, onRemove: () -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CodeTile(status.pack.id.sourceLanguage, size = 36)
        Spacer(Modifier.width(14.dp))
        Text(status.pack.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val state = status.state
        if (state is PackState.Installed && !compact) {
            Text(formatSize(state.sizeBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        }
        if (state is PackState.Downloading) {
            Text("Downloading…", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 12.dp))
        } else {
            Box(Modifier.size(8.dp).clip(CircleShape).background(StatusTints.ok))
            Spacer(Modifier.width(8.dp))
            Text("Installed", style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok, modifier = if (compact) Modifier else Modifier.width(110.dp))
            Box {
                IconButton(onClick = { menu = true }) { Icon(AppIcons.MoreHoriz, contentDescription = "More for ${status.pack.title}") }
                AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    AppMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() })
                }
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
    val shown = if (browsing || learning == null) missing.filter { query.isBlank() || it.pack.title.contains(query.trim(), ignoreCase = true) } else forLearning
    ContentCard(
        if (browsing || learning == null) "All dictionaries" else "Available for ${learning.name}",
        if (browsing || learning == null) "Every dictionary not on this device." else "Add meanings in another language.",
        icon = AppIcons.Download,
        modifier = modifier,
    ) {
        if (browsing || learning == null) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search dictionaries") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (shown.isEmpty()) {
            Text(
                if (query.isNotBlank()) "No dictionaries match “${query.trim()}”." else "Every ${learning?.name ?: ""} dictionary is downloaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        // The full catalogue is long, so it scrolls inside the card.
        val list = Modifier.fillMaxWidth().then(if (browsing) Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()) else Modifier)
        Column(list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shown.forEach { status -> AvailableRow(status, browsing, onDownload = { viewModel.download(status.pack) }) }
        }
        Spacer(Modifier.height(14.dp))
        InfoBanner("One pack covers one source language with meanings in one language.")
        if (learning != null) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { browsing = !browsing; query = "" }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(AppIcons.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (browsing) "Show ${learning.name} dictionaries" else "Browse all ${DictionaryPacks.all.size} dictionaries", modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AvailableRow(status: PackStatus, browsing: Boolean, onDownload: () -> Unit) {
    val state = status.state
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // For one source language the target is what tells the packs apart.
        CodeTile(if (browsing) status.pack.id.sourceLanguage else status.pack.id.targetLanguage, size = 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(status.pack.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                when (state) {
                    is PackState.Downloading -> "Downloading…"
                    is PackState.Failed -> "Download failed: ${state.message}"
                    else -> "Not downloaded"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state is PackState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state is PackState.Downloading) {
                val progress = state.progress
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                else LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        if (state !is PackState.Downloading) {
            OutlinedButton(onClick = onDownload, shape = RoundedCornerShape(50)) { Text(if (state is PackState.Failed) "Retry" else "Download") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test lookup") },
        text = {
            Column(Modifier.widthIn(min = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type a ${pack.sourceName} word to see what ${pack.title} says about it.", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        placeholder = { Text("A ${pack.sourceName} word") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { search() }, enabled = word.isNotBlank()) { Text("Look up") }
                }
                result?.let { (looked, lookup) ->
                    if (lookup.isEmpty) {
                        Text("No entry for “$looked”.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (lookup.lemmas.isNotEmpty()) Text("A form of ${lookup.lemmas.joinToString()}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                            lookup.entries.take(4).forEach { entry ->
                                Column {
                                    Text(
                                        listOfNotNull(entry.word, entry.pos, entry.ipa).joinToString(" · "),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
