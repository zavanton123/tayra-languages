package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.components.ScrollColumn
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

/**
 * The Dictionaries page on a phone: the pair looked up with one button for its dictionary, then
 * Installed or Available under a segmented switch with a search, the available ones leading with
 * the language being learned.
 */
@Composable
internal fun PhoneDictionariesScreen(
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
    Scaffold(
        topBar = { AppTopBar(title = tr("Dictionaries"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            // Every settings page shows where it stands the same way: a pill under its subtitle.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Manage offline word definitions and base-form lookup."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                pill()
            }
            PhoneLookupCard(learning, nativeName, current, onDownloads = { onNavigate(Route.Downloads) }, onTest = onTest)
            DownloadsLinkCard(dictionariesInDownloads(), onNavigate)
            // Bringing a dictionary of one's own comes after the dictionaries themselves.
            if (viewModel.canImport) ImportDictionaryCard(viewModel, learning, native)
        }
    }
}

/** The pair looked up and where its dictionary stands, with the one thing to do about it as a full-width button. */
@Composable
private fun PhoneLookupCard(learning: LanguageOption?, nativeName: String, current: PackStatus?, onDownloads: () -> Unit, onTest: (DictionaryPack) -> Unit) {
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
                PrimaryButton(onClick = onDownloads, modifier = Modifier.fillMaxWidth().testTag("phone-download-current")) {
                    Text(tr("Go to Downloads"), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
