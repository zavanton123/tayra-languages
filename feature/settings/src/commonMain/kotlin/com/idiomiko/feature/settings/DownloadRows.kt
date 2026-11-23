package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.domain.service.SetupItem
import com.idiomiko.core.domain.service.SetupState
import com.idiomiko.core.domain.service.SetupStatus
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** A removal waiting for the learner to confirm it. */
internal class Confirmation(val question: String, val consequence: String, val remove: () -> Unit)

/** Where a download stands, as its row shows it. */
internal sealed interface RowState {
    data class Available(val size: String?) : RowState
    data class Working(val progress: Float?, val label: String) : RowState
    data class Installed(val bytes: Long?) : RowState
    data class Failed(val message: String) : RowState
    data object Unavailable : RowState
}

/** Where [item] stands, with the state of its download or removal under way. */
internal fun rowState(item: SetupItem, progress: SetupState?, label: String? = null): RowState {
    val installed = item.status == SetupStatus.INSTALLED
    return when {
        item.status == SetupStatus.UNAVAILABLE -> RowState.Unavailable
        progress is SetupState.Waiting -> RowState.Working(null, tr("Queued"))
        progress is SetupState.Running -> RowState.Working(null, if (installed) tr("Removing…") else label ?: tr("Downloading…"))
        progress is SetupState.Failed && !installed -> RowState.Failed(downloadFailure(progress.message))
        installed -> RowState.Installed(item.onDeviceBytes)
        else -> RowState.Available(item.sizeBytes?.let { sizeText(it, item.sizeEstimated) })
    }
}

@Composable
internal fun ToolRow(title: String, detail: String?, item: SetupItem, progress: SetupState?, tag: String, onDownload: () -> Unit, onRemove: (() -> Unit)?) {
    DownloadRow(title, detail, rowState(item, progress), tag, onDownload, onRemove)
}

/** One download: what it is, where it stands, and the button to fetch or remove it. */
@Composable
internal fun DownloadRow(title: String, detail: String?, state: RowState, tag: String, onDownload: (() -> Unit)?, onRemove: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val about: @Composable ColumnScope.() -> Unit = {
        Text(title, style = AppText.itemTitle)
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        DownloadProgress(state)
    }
    val status: @Composable () -> Unit = { DownloadStatus(state) }
    val action: @Composable () -> Unit = { DownloadAction(state, tag, onDownload, onRemove) }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.m).testTag("row-$tag"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            about()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) { status() }
                action()
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(vertical = AppSpacing.m).testTag("row-$tag"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) { about() }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                status()
                action()
            }
        }
    }
}

/** The progress of a download under way, or why it failed. */
@Composable
internal fun DownloadProgress(state: RowState) {
    val colors = MaterialTheme.colorScheme
    when (state) {
        is RowState.Working -> {
            AppProgressBar(state.progress, Modifier.padding(top = AppSpacing.xs))
            Text(state.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        is RowState.Failed -> Text(state.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
        else -> Unit
    }
}

/** On the device with its size, the size to download, or not available. */
@Composable
internal fun DownloadStatus(state: RowState) {
    val colors = MaterialTheme.colorScheme
    when (state) {
        is RowState.Installed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconSmall))
            Text(state.bytes?.let { tr("{0} on this device", formatSize(it)) } ?: tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
        }
        is RowState.Available -> state.size?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        RowState.Unavailable -> Text(tr("Not available"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else -> Unit
    }
}

/** Download (or try again) while it is not on the device, Remove once it is and may go. */
@Composable
internal fun DownloadAction(state: RowState, tag: String, onDownload: (() -> Unit)?, onRemove: (() -> Unit)?) {
    when (state) {
        is RowState.Available, is RowState.Failed -> onDownload?.let {
            SecondaryButton(
                if (state is RowState.Failed) tr("Try again") else tr("Download"),
                onClick = it,
                icon = AppIcons.Download,
                size = ButtonSize.Compact,
                modifier = Modifier.testTag("download-$tag"),
            )
        }
        is RowState.Installed -> onRemove?.let { QuietButton(tr("Remove"), onClick = it, tone = ButtonTone.Danger, size = ButtonSize.Compact, modifier = Modifier.testTag("remove-$tag")) }
        else -> Unit
    }
}

/** Asks before a download is removed: [question] names it, [consequence] says what goes with it. */
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
