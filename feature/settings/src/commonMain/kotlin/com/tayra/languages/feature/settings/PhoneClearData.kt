package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.CardHeader
import com.tayra.languages.core.ui.components.CardHeaderSize
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing

/**
 * The Clear data page on a phone: what goes and what stays as two cards, the backup reminder with
 * its own full-width button, and the permanent-action box with one button that clears everything.
 */
@Composable
internal fun PhoneClearDataScreen(
    state: ClearDataUiState,
    onClear: () -> Unit,
    onBackups: () -> Unit,
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
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
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
                    onClick = onBackups,
                    modifier = Modifier.fillMaxWidth().testTag("clear-backup"),
                    enabled = !state.started,
                    size = ButtonSize.Large,
                )
            }
            ClearStatusNotice(state) {
                if (state.done) {
                    PrimaryButton(tr("Done"), onClick = onDone, modifier = Modifier.fillMaxWidth().testTag("clear-done"), size = ButtonSize.Large)
                } else {
                    PrimaryButton(
                        if (state.started) tr("Clearing…") else tr("Continue to clear data"),
                        onClick = { confirming = true },
                        modifier = Modifier.fillMaxWidth().testTag("clear-all"),
                        enabled = !state.started,
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
