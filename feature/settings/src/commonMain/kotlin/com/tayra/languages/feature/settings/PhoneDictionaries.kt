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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes

private enum class PhoneTab { INSTALLED, AVAILABLE }

/**
 * The Dictionaries page on a phone: the pair looked up with one button for its dictionary, then
 * Installed or Available under a segmented switch with a search, the available ones leading with
 * the language being learned.
 */
@Composable
internal fun PhoneDictionariesScreen(
    packs: List<PackStatus>,
    learning: LanguageOption?,
    native: String,
    nativeName: String,
    current: PackStatus?,
    viewModel: DictionariesViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onTest: (DictionaryPack) -> Unit,
    pill: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val installed = packs.filter { it.state is PackState.Installed || it.state is PackState.Downloading }
    var tab by remember { mutableStateOf(if (installed.isEmpty()) PhoneTab.AVAILABLE else PhoneTab.INSTALLED) }
    var query by remember { mutableStateOf("") }
    Scaffold(
        topBar = { AppTopBar(title = tr("Dictionaries"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Manage offline word definitions and base-form lookup."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                pill()
            }
            PhoneLookupCard(learning, nativeName, current, onDownload = { viewModel.download(it) }, onTest = onTest)
            if (viewModel.canImport) ImportDictionaryCard(viewModel, learning, native)
            TextRecognitionCard(viewModel)
            // A search belongs to the list it was typed into.
            PhoneSegments(tab, installed.size) { tab = it; query = "" }
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (tab == PhoneTab.INSTALLED) tr("Search installed dictionaries") else tr("Search dictionaries")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            when (tab) {
                PhoneTab.INSTALLED -> {
                    val shown = installed.filter { it.pack.matches(query) }.sortedBy { it.pack.title }
                    Text(tr("Installed dictionaries"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (shown.isEmpty()) {
                        Text(
                            if (query.isNotBlank()) tr("No dictionaries match “{0}”.", query.trim()) else tr("No dictionaries downloaded yet."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        ListBox { shown.forEachIndexed { i, status -> if (i > 0) HorizontalDivider(color = colors.outlineVariant); InstalledRow(status, onRemove = { viewModel.remove(status.pack) }) } }
                    }
                }
                PhoneTab.AVAILABLE -> {
                    val missing = packs.filter { it.state !is PackState.Installed }
                    // Without a search the list is the learning language's, the native language first; a search looks through every pack.
                    val browsing = query.isNotBlank() || learning == null
                    val shown = if (browsing) missing.filter { it.pack.matches(query) }.sortedBy { it.pack.title }
                    else missing.filter { it.pack.id.sourceLanguage == learning.code }.sortedWith(compareBy({ it.pack.id.targetLanguage != native }, { it.pack.title }))
                    Text(if (browsing) tr("All dictionaries") else tr("Available for {0}", tr(learning!!.name)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (shown.isEmpty()) {
                        Text(
                            when {
                                query.isNotBlank() -> tr("No dictionaries match “{0}”.", query.trim())
                                learning == null -> tr("Every dictionary is downloaded.")
                                else -> tr("Every {0} dictionary is downloaded.", tr(learning.name))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        ListBox {
                            shown.forEachIndexed { i, status ->
                                if (i > 0) HorizontalDivider(color = colors.outlineVariant)
                                PhoneAvailableRow(status, browsing, onDownload = { viewModel.download(status.pack) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The pair looked up and where its dictionary stands, with the one thing to do about it as a full-width button. */
@Composable
private fun PhoneLookupCard(learning: LanguageOption?, nativeName: String, current: PackStatus?, onDownload: (DictionaryPack) -> Unit, onTest: (DictionaryPack) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = current?.state
    Column(Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surface).border(1.dp, colors.outlineVariant, AppShapes.card).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(AppIcons.MenuBook, size = 44)
            Text(tr("Current lookup"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PairChip(learning?.code ?: "?", learning?.let { tr(it.name) } ?: tr("No language"), Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("looked up in"), tint = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp).size(22.dp))
            PairChip(LanguageCatalog.nativeLanguages.firstOrNull { it.name == nativeName }?.code ?: "", tr(nativeName), Modifier.weight(1f))
        }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = colors.outlineVariant)
        val (icon, tint, title, text) = when {
            state is PackState.Installed -> Status(Icons.Default.CheckCircle, StatusTints.ok, tr("Dictionary ready"), tr("Works offline and links inflected forms to their base word."))
            state is PackState.Downloading -> Status(Icons.Default.Info, colors.primary, tr("Downloading the dictionary…"), tr("It works offline as soon as the download finishes."))
            state is PackState.Failed -> Status(Icons.Default.Warning, colors.error, tr("Download failed"), packFailure(state.message))
            current != null -> Status(Icons.Default.Warning, StatusTints.warning, tr("{0} is not downloaded", current.pack.displayTitle), tr("Online dictionaries are used until you download it."))
            else -> Status(Icons.Default.Info, colors.outline, tr("No offline dictionary for this pair"), tr("Words are looked up in the online dictionaries set up for the language."))
        }
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (state is PackState.Installed) tint else colors.onSurface)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (state is PackState.Downloading) {
                    val progress = state.progress
                    if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    else LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
        when {
            current == null || state is PackState.Downloading -> Unit
            state is PackState.Installed -> {
                Spacer(Modifier.height(14.dp))
                SecondaryButton(onClick = { onTest(current.pack) }, modifier = Modifier.fillMaxWidth()) { Text(tr("Test lookup"), style = MaterialTheme.typography.titleMedium) }
            }
            else -> {
                Spacer(Modifier.height(14.dp))
                PrimaryButton(onClick = { onDownload(current.pack) }, modifier = Modifier.fillMaxWidth().testTag("phone-download-current")) {
                    Text(tr("Download {0}", current.pack.displayTitle), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private data class Status(val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color, val title: String, val text: String)

@Composable
private fun PairChip(code: String, name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.clip(AppShapes.control).background(colors.surfaceContainerLow).border(1.dp, colors.outlineVariant, AppShapes.control).padding(6.dp).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CodeTile(code, size = 36)
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Installed with its count, or Available: a full-width switch. */
@Composable
private fun PhoneSegments(selected: PhoneTab, installedCount: Int, onSelect: (PhoneTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(AppShapes.panel).background(colors.surfaceContainer).padding(4.dp)) {
        PhoneTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                Modifier.weight(1f).clip(AppShapes.control)
                    .background(if (active) colors.surface else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 12.dp)
                    .testTag("phone-tab-${tab.name}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (tab == PhoneTab.INSTALLED) "${tr("Installed")} $installedCount" else tr("Available"),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ListBox(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(AppShapes.panel).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.panel)) { content() }
}

/** A pack to download: the language that tells it apart, its name and the button; the state shows only when it is not plain. */
@Composable
private fun PhoneAvailableRow(status: PackStatus, browsing: Boolean, onDownload: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = status.state
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        CodeTile(if (browsing) status.pack.id.sourceLanguage else status.pack.id.targetLanguage, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // An edition's source goes on its own line: cut off at the end of the title, it would read as a duplicate.
            val edition = status.pack.editionLabel
            Text(if (edition == null) status.pack.displayTitle else "${tr(status.pack.sourceName)} \u2192 ${tr(status.pack.targetName)}", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (edition != null && state !is PackState.Failed) Text(edition, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            when (state) {
                is PackState.Downloading -> {
                    Text(tr("Downloading…"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    val progress = state.progress
                    if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    else LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                is PackState.Failed -> Text(tr("Download failed: {0}", packFailure(state.message)), style = MaterialTheme.typography.bodySmall, color = colors.error)
                else -> Unit
            }
        }
        Spacer(Modifier.width(10.dp))
        if (state !is PackState.Downloading) {
            SecondaryButton(if (state is PackState.Failed) tr("Retry") else tr("Download"), onClick = onDownload)
        }
    }
}
