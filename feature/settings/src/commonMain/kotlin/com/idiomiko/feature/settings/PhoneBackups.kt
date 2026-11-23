package com.idiomiko.feature.settings

import androidx.compose.runtime.saveable.rememberSaveable
import com.idiomiko.core.ui.components.ScrollColumn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.idiomiko.core.domain.backup.Backup
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.components.Reveal

/**
 * The Data page on a phone: one card with the two backup actions, the history as a plain list with a
 * menu per backup, then clearing the data on this device. The dialogs stay with the screen.
 */
@Composable
internal fun PhoneBackupsScreen(
    state: BackupState,
    idle: Boolean,
    viewModel: BackupViewModel,
    onImport: () -> Unit,
    onRestore: (Backup) -> Unit,
    onDelete: (Backup) -> Unit,
    onCleared: () -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            AppTopBar(title = tr("Data"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS)
        },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            AppCard {
                CardHeader(tr("Protect your data"), tr("Save your library, vocabulary, progress, and preferences."), icon = AppIcons.Storage, size = CardHeaderSize.Dense)
                Spacer(Modifier.height(AppSpacing.l))
                PrimaryButton(
                    tr("Create backup"),
                    onClick = viewModel::create,
                    modifier = Modifier.fillMaxWidth().testTag("phone-create-backup"),
                    icon = AppIcons.Storage,
                    enabled = idle,
                    size = ButtonSize.Large,
                )
                Spacer(Modifier.height(AppSpacing.m))
                SecondaryButton(tr("Import backup"), onClick = onImport, modifier = Modifier.fillMaxWidth(), icon = AppIcons.UploadFile, enabled = idle, size = ButtonSize.Large)
            }
            Progress(state)
            Text(tr("Backup history"), style = AppText.headline)
            if (state.loaded && state.backups.isEmpty()) {
                Text(tr("Create a backup to keep a copy of your data."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            var more by rememberSaveable { mutableStateOf(false) }
            state.backups.recent(more).forEachIndexed { i, backup ->
                PhoneBackupRow(backup, latest = i == 0, enabled = idle, onRestore = { onRestore(backup) }, onExport = { viewModel.export(backup) }, onDelete = { onDelete(backup) })
            }
            MoreBackups(state.backups, more) { more = !more }
            PhoneClearDataBlock(state.clearing, backupEnabled = idle, onClear = viewModel::clear, onBackup = viewModel::create, onDone = onCleared)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneBackupRow(backup: Backup, latest: Boolean, enabled: Boolean, onRestore: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().cardSurface().padding(start = AppSpacing.m, end = AppSpacing.xs, top = AppSpacing.m, bottom = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(AppIcons.FileOutline, size = 44)
        Spacer(Modifier.width(AppSpacing.m))
        Column(Modifier.weight(1f)) {
            // The tag drops under the date when the two do not fit beside the buttons.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(formatBackupTime(backup, seconds = false), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (latest) Tag(tr("Latest"), colors.primary)
            }
            Text(formatSize(backup.sizeBytes), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(AppSpacing.s))
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

