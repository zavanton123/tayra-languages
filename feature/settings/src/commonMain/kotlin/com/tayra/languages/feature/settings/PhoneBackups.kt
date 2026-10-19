package com.tayra.languages.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route

/**
 * The Backups page on a phone: one card with the two actions, the history as a plain list with a
 * menu per backup, and what a backup holds folded away under a row. The dialogs stay with the screen.
 */
@Composable
internal fun PhoneBackupsScreen(
    state: BackupState,
    idle: Boolean,
    viewModel: BackupViewModel,
    onImport: () -> Unit,
    onRestore: (Backup) -> Unit,
    onDelete: (Backup) -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val count = state.backups.size
    Scaffold(
        topBar = {
            AppTopBar(title = tr("Backups"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS, actions = {
                if (state.loaded) StatusPill(if (count == 0) tr("None yet") else trPlural(count, "{0} local", "{0} local"), if (count == 0) StatusTints.warning else StatusTints.ok)
                Spacer(Modifier.width(8.dp))
            })
        },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconTile(AppIcons.Storage, size = 56)
                    Column {
                        Text(tr("Protect your data"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(tr("Save your library, vocabulary, progress, and preferences."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(onClick = viewModel::create, enabled = idle, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("phone-create-backup")) {
                    Icon(AppIcons.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Create backup"), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(onClick = onImport, enabled = idle, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(AppIcons.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Import backup"), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("Backups are stored on this device."), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Progress(state)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr("Backup history"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (count > 0) Tag(trPlural(count, "{0} backup", "{0} backups"), colors.primary)
                }
                Text(tr("Newest first."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (state.loaded && state.backups.isEmpty()) {
                Text(tr("No backups yet. Create one to keep a copy of your data."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            state.backups.forEachIndexed { i, backup ->
                PhoneBackupRow(backup, latest = i == 0, enabled = idle, onRestore = { onRestore(backup) }, onExport = { viewModel.export(backup) }, onDelete = { onDelete(backup) })
            }
            IncludedRow()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneBackupRow(backup: Backup, latest: Boolean, enabled: Boolean, onRestore: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().cardSurface().padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(AppIcons.FileOutline, size = 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The tag drops under the date when the two do not fit beside the buttons.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(formatBackupTime(backup, seconds = false), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (latest) Tag(tr("Latest"), colors.primary)
            }
            Row {
                Text("${formatSize(backup.sizeBytes)} • ", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(tr("Ready"), style = MaterialTheme.typography.bodySmall, color = StatusTints.ok)
            }
        }
        Spacer(Modifier.width(8.dp))
        SecondaryButton(onClick = onRestore, enabled = enabled) { Text(tr("Restore")) }
        Box {
            AppIconButton(AppIcons.MoreHoriz, contentDescription = tr("More for the backup of {0}", formatBackupTime(backup)), onClick = { menu = true }, enabled = enabled)
            AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AppMenuItem(text = { Text(tr("Export")) }, onClick = { menu = false; onExport() })
                AppMenuItem(text = { Text(tr("Delete")) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** What a backup holds, folded away under a row. */
@Composable
private fun IncludedRow() {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().cardSurface()) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp).testTag("phone-whats-included"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(AppIcons.VerifiedUser, size = 44)
            Text(tr("What's included in a backup"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        if (open) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                listOf(tr("Languages and books"), tr("Vocabulary"), tr("Reading history"), tr("Settings")).forEach { IncludedLine(it, Icons.Default.CheckCircle, StatusTints.ok) }
                AppDivider(Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(tr("Not included"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                listOf(tr("Audio files"), tr("Downloaded dictionaries"), tr("Voices and translation models"), tr("API keys")).forEach { IncludedLine(it, AppIcons.RemoveCircle, colors.outline) }
            }
        }
    }
}
