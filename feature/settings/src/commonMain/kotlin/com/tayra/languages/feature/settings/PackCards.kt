package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSizes

/** A downloadable pack of courses or books, as its card shows it. */
internal class PackInfo(val id: String, val languageCode: String, val title: String, val summary: String, val downloadSize: Long)

/** What a pack's card calls the pack and its contents, already translated: course packs and book bundles differ only in these. */
internal class PackWords(
    val kind: String,
    val downloading: String,
    val onDevice: String,
    val downloadAction: String,
    val removeAction: String,
)

@Composable
private fun PackStatePill(state: PackState) {
    val colors = MaterialTheme.colorScheme
    when (state) {
        is PackState.Installed -> StatusPill(tr("Installed"), StatusTints.ok)
        is PackState.Downloading -> StatusPill(tr("Downloading"), colors.primary)
        is PackState.Failed -> StatusPill(tr("Download failed"), colors.error)
        PackState.NotInstalled -> StatusPill(tr("Not downloaded"), colors.outline)
    }
}

/** A pack on the wide screen: its summary, its state and size, and the button to download or remove it. */
@Composable
internal fun PackCard(pack: PackInfo, state: PackState, words: PackWords, onDownload: () -> Unit, onRemove: () -> Unit) {
    ContentCard(
        tr(pack.title),
        words.kind,
        iconText = pack.languageCode.uppercase(),
        modifier = Modifier.testTag("pack-${pack.id}"),
        titleExtra = { PackStatePill(state) },
    ) {
        Text(tr(pack.summary), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.padding(top = 12.dp))
        when (state) {
            is PackState.Downloading -> {
                AppProgressBar(state.progress)
                Text(words.downloading, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            is PackState.Failed -> Text(packFailure(state.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
        Spacer(Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        // No file size when the contents alone are on the device, restored from a backup without the file.
                        is PackState.Installed -> if (state.sizeBytes > 0) tr("{0} on this device", formatSize(state.sizeBytes)) else words.onDevice
                        else -> tr("{0} to download", formatSize(pack.downloadSize))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state) {
                is PackState.Installed -> SecondaryButton(tr("Remove"), onClick = onRemove, modifier = Modifier.testTag("remove-${pack.id}"))
                is PackState.Downloading -> Unit
                else -> PrimaryButton(if (state is PackState.Failed) tr("Try again") else tr("Download"), onClick = onDownload, modifier = Modifier.testTag("download-${pack.id}"), icon = AppIcons.Download)
            }
        }
    }
}

/** A pack on a phone: its header, what it holds ([about]), its state and size, and one full-width button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhonePackCard(
    pack: PackInfo,
    state: PackState,
    words: PackWords,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    about: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(Modifier.testTag("pack-${pack.id}"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(null, pack.languageCode.uppercase(), size = 52)
            Column(Modifier.weight(1f)) {
                // A long title pushes the pill onto the next line rather than squeezing it.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(tr(pack.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    PackStatePill(state)
                }
                Text(words.kind, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        about()
        when (state) {
            is PackState.Downloading -> {
                AppProgressBar(state.progress)
                Text(words.downloading, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            is PackState.Failed -> Text(packFailure(state.message), style = MaterialTheme.typography.bodyMedium, color = colors.error)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state is PackState.Installed) AppIcons.Storage else AppIcons.Download, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        // No file size when the contents alone are on the device, restored from a backup without the file.
                        state is PackState.Installed && state.sizeBytes > 0 -> tr("{0} on this device", formatSize(state.sizeBytes))
                        state is PackState.Installed -> words.onDevice
                        else -> formatSize(pack.downloadSize)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        when (state) {
            is PackState.Installed -> SecondaryButton(
                words.removeAction,
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().testTag("remove-${pack.id}"),
                size = ButtonSize.Large,
            )
            is PackState.Downloading -> Unit
            else -> PrimaryButton(
                if (state is PackState.Failed) tr("Try again") else words.downloadAction,
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().testTag("download-${pack.id}"),
                icon = AppIcons.Download,
                size = ButtonSize.Large,
            )
        }
    }
}

/** Asks before a pack is removed: [question] names it, [consequence] says what goes with it. */
@Composable
internal fun RemovePackDialog(question: String, consequence: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(question) },
        text = { Text(consequence) },
        confirmButton = {
            QuietButton(tr("Remove"), onClick = { onRemove(); onDismiss() }, modifier = Modifier.testTag("confirm-remove"), tone = ButtonTone.Danger)
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}
