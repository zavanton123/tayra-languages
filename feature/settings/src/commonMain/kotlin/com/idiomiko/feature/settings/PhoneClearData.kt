package com.idiomiko.feature.settings

import com.idiomiko.core.ui.components.ScrollColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing

/**
 * The Clear data page on a phone: what goes and what stays as two cards, the backup reminder with
 * its own full-width button, and the permanent-action box with one button that clears everything.
 */
@Composable
internal fun PhoneClearDataScreen(
    state: ClearDataUiState,
    onClear: () -> Unit,
    onBackup: () -> Unit,
    onDone: () -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirming by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Clear local data"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Remove your learning data and downloaded files from this device. Your preferences stay."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            AppCard {
                CardHeader(tr("Will be removed"), tr("Everything you added, learned or downloaded."), icon = AppIcons.RemoveCircle, tint = colors.error, size = CardHeaderSize.Dense)
                AppDivider(Modifier.padding(vertical = AppSpacing.l))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) { ClearGroup.entries.forEach { GroupRow(it, state) } }
            }
            AppCard {
                CardHeader(tr("Will remain"), tr("Your app setup is not affected."), icon = AppIcons.VerifiedUser, size = CardHeaderSize.Dense)
                AppDivider(Modifier.padding(vertical = AppSpacing.l))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) { RemainingItems() }
            }
            BackupNotice {
                SecondaryButton(
                    tr("Create backup"),
                    onClick = onBackup,
                    modifier = Modifier.fillMaxWidth().testTag("clear-backup"),
                    enabled = !state.started,
                    loading = state.backingUp,
                    size = ButtonSize.Large,
                )
            }
            ClearStatusNotice(state) {
                if (state.done) {
                    PrimaryButton(tr("Done"), onClick = onDone, modifier = Modifier.fillMaxWidth().testTag("clear-done"), size = ButtonSize.Large)
                } else {
                    PrimaryButton(
                        if (state.started) tr("Clearing…") else tr("Clear all data"),
                        onClick = { confirming = true },
                        modifier = Modifier.fillMaxWidth().testTag("clear-all"),
                        icon = AppIcons.RemoveCircle,
                        enabled = !state.started && !state.backingUp,
                        loading = state.started,
                        tone = ButtonTone.Danger,
                        size = ButtonSize.Large,
                    )
                }
            }
        }
    }
    if (confirming) ClearConfirmDialog(onConfirm = { confirming = false; onClear() }, onDismiss = { confirming = false })
}
