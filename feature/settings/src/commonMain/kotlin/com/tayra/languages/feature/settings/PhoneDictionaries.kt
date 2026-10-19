package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.largeText
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

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
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            // Every settings page shows where it stands the same way: a pill under its subtitle.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Manage offline word definitions and base-form lookup."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                pill()
            }
            PhoneLookupCard(learning, nativeName, current, onDownload = { viewModel.download(it) }, onTest = onTest)
            if (viewModel.canImport) ImportDictionaryCard(viewModel, learning, native)
            TextRecognitionCard(viewModel)
            // A search belongs to the list it was typed into.
            SegmentedToggle(
                listOf(
                    Segment(PhoneTab.INSTALLED, label = "${tr("Installed")} ${installed.size}", testTag = "phone-tab-${PhoneTab.INSTALLED.name}"),
                    Segment(PhoneTab.AVAILABLE, label = tr("Available"), testTag = "phone-tab-${PhoneTab.AVAILABLE.name}"),
                ),
                tab,
                onSelect = { tab = it; query = "" },
                fill = true,
            )
            SearchField(query, { query = it }, if (tab == PhoneTab.INSTALLED) tr("Search installed dictionaries") else tr("Search dictionaries"), Modifier.fillMaxWidth())
            when (tab) {
                PhoneTab.INSTALLED -> {
                    val shown = installed.filter { it.pack.matches(query) }.sortedBy { it.pack.title }
                    Text(tr("Installed dictionaries"), style = AppText.sectionTitle)
                    if (shown.isEmpty()) {
                        Text(
                            if (query.isNotBlank()) tr("No dictionaries match “{0}”.", query.trim()) else tr("No dictionaries downloaded yet."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        ListBox { shown.forEachIndexed { i, status -> if (i > 0) AppDivider(); InstalledRow(status, onRemove = { viewModel.remove(status.pack) }) } }
                    }
                }
                PhoneTab.AVAILABLE -> {
                    val missing = packs.filter { it.state !is PackState.Installed }
                    // Without a search the list is the learning language's, the native language first; a search looks through every pack.
                    val browsing = query.isNotBlank() || learning == null
                    val shown = if (browsing) missing.filter { it.pack.matches(query) }.sortedBy { it.pack.title }
                    else missing.filter { it.pack.id.sourceLanguage == learning.code }.sortedWith(compareBy({ it.pack.id.targetLanguage != native }, { it.pack.title }))
                    Text(if (browsing) tr("All dictionaries") else tr("Available for {0}", tr(learning!!.name)), style = AppText.sectionTitle)
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
                                if (i > 0) AppDivider()
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
    ContentCard(tr("Current lookup"), subtitle = null, icon = AppIcons.MenuBook, dense = true) {
        val fromCode = learning?.code ?: "?"
        val fromName = learning?.let { tr(it.name) } ?: tr("No language")
        val toCode = LanguageCatalog.nativeLanguages.firstOrNull { it.name == nativeName }?.code ?: ""
        // Large text leaves no room for two names side by side, so they stack.
        if (largeText) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                PairChip(fromCode, fromName, Modifier.fillMaxWidth())
                Icon(AppIcons.ArrowDown, contentDescription = tr("looked up in"), tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
                PairChip(toCode, tr(nativeName), Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PairChip(fromCode, fromName, Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("looked up in"), tint = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = AppSpacing.s).size(AppSizes.iconMedium))
                PairChip(toCode, tr(nativeName), Modifier.weight(1f))
            }
        }
        AppDivider(Modifier.padding(vertical = AppSpacing.l))
        LookupStatusRow(current, short = true)
        when {
            current == null || state is PackState.Downloading -> Unit
            state is PackState.Installed -> {
                Spacer(Modifier.height(AppSpacing.l))
                SecondaryButton(onClick = { onTest(current.pack) }, modifier = Modifier.fillMaxWidth()) { Text(tr("Test lookup"), style = MaterialTheme.typography.titleMedium) }
            }
            else -> {
                Spacer(Modifier.height(AppSpacing.l))
                PrimaryButton(onClick = { onDownload(current.pack) }, modifier = Modifier.fillMaxWidth().testTag("phone-download-current")) {
                    Text(tr("Download {0}", current.pack.displayTitle), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ListBox(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.panel)) { content() }
}

/** A pack to download: the language that tells it apart, its name and the button; the state shows only when it is not plain. */
@Composable
private fun PhoneAvailableRow(status: PackStatus, browsing: Boolean, onDownload: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = status.state
    Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.m, vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
        CodeTile(if (browsing) status.pack.id.sourceLanguage else status.pack.id.targetLanguage, size = 40)
        Spacer(Modifier.width(AppSpacing.m))
        Column(Modifier.weight(1f)) {
            // An edition's source goes on its own line: cut off at the end of the title, it would read as a duplicate.
            val edition = status.pack.editionLabel
            Text(if (edition == null) status.pack.displayTitle else "${tr(status.pack.sourceName)} \u2192 ${tr(status.pack.targetName)}", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (edition != null && state !is PackState.Failed) Text(edition, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            when (state) {
                is PackState.Downloading -> {
                    Text(tr("Downloading…"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    AppProgressBar(state.progress, Modifier.padding(top = AppSpacing.xs))
                }
                is PackState.Failed -> Text(tr("Download failed: {0}", packFailure(state.message)), style = MaterialTheme.typography.bodySmall, color = colors.error)
                else -> Unit
            }
        }
        Spacer(Modifier.width(AppSpacing.m))
        if (state !is PackState.Downloading) {
            SecondaryButton(if (state is PackState.Failed) tr("Retry") else tr("Download"), onClick = onDownload)
        }
    }
}
