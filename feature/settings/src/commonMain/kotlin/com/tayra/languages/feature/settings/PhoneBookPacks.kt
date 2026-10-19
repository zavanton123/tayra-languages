package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackStatus
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes

/** The book packs page on a phone: each bundle a card with its summary, its size and one full-width button. */
@Composable
internal fun PhoneBookPacksScreen(
    packs: List<BookPackStatus>,
    languageName: String?,
    onDownload: (BookPack) -> Unit,
    onRemove: (BookPack) -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var removing by remember { mutableStateOf<BookPack?>(null) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Book downloads"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Download a bundle of free books, graded by level."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            InfoBanner(tr("Downloaded bundles appear in Books and can be edited."))
            if (packs.isEmpty() && languageName != null) {
                Text(tr("There is no book bundle for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            packs.forEach { status -> PhonePackCard(status, onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) }
        }
    }
    removing?.let { pack ->
        AppDialog(
            onDismissRequest = { removing = null },
            title = { Text(tr("Remove the {0} books?", tr(pack.title))) },
            text = { Text(tr("The bundle's books are deleted, with how far you read them and your bookmarks in them. Books you added yourself stay. You can download the bundle again at any time.")) },
            confirmButton = {
                QuietButton(tr("Remove"), onClick = { onRemove(pack); removing = null }, modifier = Modifier.testTag("confirm-remove"), tone = ButtonTone.Danger)
            },
            dismissButton = { QuietButton(onClick = { removing = null }) { Text(tr("Cancel")) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhonePackCard(status: BookPackStatus, onDownload: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pack = status.pack
    val state = status.state
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surface).border(1.dp, colors.outlineVariant, AppShapes.card).padding(16.dp).testTag("pack-${pack.id}"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(null, pack.languageCode.uppercase(), size = 52)
            Column(Modifier.weight(1f)) {
                // A long title pushes the pill onto the next line rather than squeezing it.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(tr(pack.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    when (state) {
                        is PackState.Installed -> StatusPill(tr("Installed"), StatusTints.ok)
                        is PackState.Downloading -> StatusPill(tr("Downloading"), colors.primary)
                        is PackState.Failed -> StatusPill(tr("Download failed"), colors.error)
                        PackState.NotInstalled -> StatusPill(tr("Not downloaded"), colors.outline)
                    }
                }
                Text(tr("Book bundle"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Text(tr(pack.summary), style = MaterialTheme.typography.bodyLarge)
        when (state) {
            is PackState.Downloading -> {
                val progress = state.progress
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(tr("Downloading and adding the books…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            is PackState.Failed -> Text(packFailure(state.message), style = MaterialTheme.typography.bodyMedium, color = colors.error)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state is PackState.Installed) AppIcons.Storage else AppIcons.Download, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        // No file size when the courses alone are on the device, restored from a backup without the file.
                        state is PackState.Installed && state.sizeBytes > 0 -> tr("{0} on this device", formatSize(state.sizeBytes))
                        state is PackState.Installed -> tr("The books are on this device")
                        else -> formatSize(pack.downloadSize)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        when (state) {
            is PackState.Installed -> SecondaryButton(onClick = onRemove, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("remove-${pack.id}")) {
                Text(tr("Remove book bundle"), style = MaterialTheme.typography.titleMedium)
            }
            is PackState.Downloading -> Unit
            else -> PrimaryButton(onClick = onDownload, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("download-${pack.id}")) {
                Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (state is PackState.Failed) tr("Try again") else tr("Download book bundle"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
