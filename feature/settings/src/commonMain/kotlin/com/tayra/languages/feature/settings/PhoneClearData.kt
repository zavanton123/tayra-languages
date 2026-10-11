package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route

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
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Remove your learning data and downloaded files from this device. Your preferences stay."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            PhoneCard {
                CardHeader(AppIcons.RemoveCircle, colors.error, tr("Will be removed"), tr("Everything you added, learned or downloaded."))
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = colors.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { ClearGroup.entries.forEach { GroupRow(it, state) } }
            }
            PhoneCard {
                CardHeader(AppIcons.VerifiedUser, colors.primary, tr("Will remain"), tr("Your app setup is not affected."))
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = colors.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    remainingItems().forEach { (icon, title, detail) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Tile(icon, colors.primary)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            TintedBox(colors.primary) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("Back up before clearing"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.primary)
                        Text(tr("Create a recovery file in case you change your mind."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onBackups, enabled = !state.started, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface), modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear-backup")) {
                    Text(tr("Create backup"), style = MaterialTheme.typography.titleMedium)
                }
            }
            val tint = clearStatusTint(state)
            TintedBox(tint) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Tile(if (state.done && state.problems.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning, tint)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(clearStatusTitle(state), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tint)
                        Text(clearStatusText(state), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (state.done) {
                    Button(onClick = onDone, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear-done")) {
                        Text(tr("Done"), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = { confirming = true },
                        enabled = !state.started,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear-all"),
                    ) {
                        if (state.started) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onError)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (state.started) tr("Clearing…") else tr("Continue to clear data"), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
    if (confirming) ClearConfirmDialog(onConfirm = { confirming = false; onClear() }, onDismiss = { confirming = false })
}

@Composable
private fun PhoneCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(16.dp)) { content() }
}

@Composable
private fun TintedBox(tint: Color, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.07f)).border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(14.dp)).padding(16.dp)) { content() }
}

@Composable
private fun CardHeader(icon: ImageVector, tint: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Tile(icon, tint, size = 56)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
