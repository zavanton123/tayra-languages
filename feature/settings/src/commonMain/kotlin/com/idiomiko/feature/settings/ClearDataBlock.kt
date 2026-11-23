package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.domain.service.ClearStep
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.ConfirmDialog
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SectionHeader
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

data class ClearDataUiState(
    /** Whether the clearing was started; it cannot be started twice. */
    val started: Boolean = false,
    val running: ClearStep? = null,
    val done: Boolean = false,
    /** The steps that failed, with why. */
    val problems: Map<ClearStep, String> = emptyMap(),
) {
    val inProgress: Boolean get() = started && !done
}

/** The three things the block talks about, each standing for the steps behind it. */
internal enum class ClearGroup(val steps: List<ClearStep>) {
    LEARNING(listOf(ClearStep.LIBRARY)),
    OFFLINE(listOf(ClearStep.COURSE_PACKS, ClearStep.DICTIONARIES, ClearStep.VOICES, ClearStep.TRANSLATION_MODELS)),
    CACHE(listOf(ClearStep.CACHES)),
}

/**
 * The foot of the Data page: what clearing removes beside what it leaves, the reminder to back up
 * first, and the permanent-action notice with the one button that clears everything after a confirmation.
 */
@Composable
internal fun ClearDataBlock(state: ClearDataUiState, backupEnabled: Boolean, onClear: () -> Unit, onBackup: () -> Unit, onDone: () -> Unit) {
    val wide = LocalWindowWidth.current.isExpanded
    var confirming by remember { mutableStateOf(false) }

    SectionHeader(tr("Clear local data"), subtitle = tr("Remove your learning data and downloaded files from this device. Your preferences stay."))
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl)) {
        if (wide) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                RemovedColumn(state, Modifier.weight(1f).padding(end = AppSpacing.xxl))
                AppVerticalDivider()
                RemainsColumn(onBackup, enabled = backupEnabled && !state.started, Modifier.weight(1f).padding(start = AppSpacing.xxl))
            }
        } else {
            RemovedColumn(state, Modifier.fillMaxWidth())
            AppDivider(Modifier.padding(vertical = AppSpacing.xl))
            RemainsColumn(onBackup, enabled = backupEnabled && !state.started, Modifier.fillMaxWidth())
        }
    }
    ClearStatusNotice(state) {
        if (state.done) {
            PrimaryButton(onClick = onDone, modifier = Modifier.testTag("clear-done")) { Text(tr("Done")) }
        } else {
            PrimaryButton(
                if (state.started) tr("Clearing…") else tr("Clear all data"),
                onClick = { confirming = true },
                modifier = Modifier.testTag("clear-all"),
                icon = AppIcons.RemoveCircle,
                enabled = !state.started,
                loading = state.started,
                tone = ButtonTone.Danger,
            )
        }
    }

    if (confirming) ClearConfirmDialog(onConfirm = { confirming = false; onClear() }, onDismiss = { confirming = false })
}

/** The same block on a phone, stacked: what goes, what stays, the backup reminder and the permanent-action notice, each with a full-width button. */
@Composable
internal fun PhoneClearDataBlock(state: ClearDataUiState, backupEnabled: Boolean, onClear: () -> Unit, onBackup: () -> Unit, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var confirming by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Text(tr("Clear local data"), style = AppText.headline)
        Text(tr("Remove your learning data and downloaded files from this device. Your preferences stay."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
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
            enabled = backupEnabled && !state.started,
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
                enabled = !state.started,
                loading = state.started,
                tone = ButtonTone.Danger,
                size = ButtonSize.Large,
            )
        }
    }
    if (confirming) ClearConfirmDialog(onConfirm = { confirming = false; onClear() }, onDismiss = { confirming = false })
}

@Composable
private fun ClearConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = tr("Clear all data?"),
        text = tr("This removes your books and texts, your courses and lessons with their progress, your words and flashcards, and the downloaded dictionaries, voices and translation models. Settings and backups stay. This cannot be undone."),
        confirmLabel = tr("Clear everything"),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** The permanent-action notice: a warning before, what happened after. */
@Composable
private fun ClearStatusNotice(state: ClearDataUiState, action: @Composable () -> Unit) {
    val cleared = state.done && state.problems.isEmpty()
    Notice(
        clearStatusTitle(state),
        message = clearStatusText(state),
        tint = if (cleared) StatusTints.ok else MaterialTheme.colorScheme.error,
        icon = if (cleared) Icons.Default.CheckCircle else Icons.Default.Warning,
        action = action,
    )
}

@Composable
private fun clearStatusTitle(state: ClearDataUiState): String = when {
    state.done && state.problems.isEmpty() -> tr("Everything was cleared")
    state.done -> tr("Some things could not be removed")
    state.started -> tr("Clearing…")
    else -> tr("This action is permanent")
}

@Composable
private fun clearStatusText(state: ClearDataUiState): String = when {
    state.done && state.problems.isEmpty() -> tr("Your library is empty and the downloads are gone. Your preferences stay as they were.")
    state.done -> tr("The rest is gone. You can try again later from here or from their Settings pages.")
    state.started -> tr("This takes a moment; the items above show how far it is.")
    else -> tr("Cleared data cannot be recovered unless you created a backup.")
}

@Composable
private fun RemovedColumn(state: ClearDataUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        CardHeader(tr("Will be removed"), tr("Everything you added, learned or downloaded."), icon = AppIcons.RemoveCircle, tint = colors.error, size = CardHeaderSize.Large)
        ClearGroup.entries.forEach { group -> GroupRow(group, state) }
    }
}

@Composable
private fun GroupRow(group: ClearGroup, state: ClearDataUiState) {
    val colors = MaterialTheme.colorScheme
    val problems = group.steps.mapNotNull { state.problems[it] }
    val running = state.running != null && state.running in group.steps
    val done = state.done || (state.running != null && group.steps.all { it.ordinal < state.running.ordinal })
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), modifier = Modifier.testTag("clear-${group.name}")) {
        IconTile(
            when (group) {
                ClearGroup.LEARNING -> AppIcons.MenuBook
                ClearGroup.OFFLINE -> AppIcons.Download
                ClearGroup.CACHE -> AppIcons.VolumeUp
            },
            tint = colors.primary,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(
                when (group) {
                    ClearGroup.LEARNING -> tr("Learning data")
                    ClearGroup.OFFLINE -> tr("Offline content")
                    ClearGroup.CACHE -> tr("Cached audio")
                },
                style = AppText.itemTitle,
            )
            Text(
                when (group) {
                    ClearGroup.LEARNING -> tr("Books, courses, progress, vocabulary and flashcards")
                    ClearGroup.OFFLINE -> tr("Downloaded courses, dictionaries, voices and translation models")
                    ClearGroup.CACHE -> tr("Sentences stored for replay")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            problems.forEach { Text(tr("Could not remove: {0}", it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
        }
        if (state.started) {
            Box(Modifier.size(AppSizes.iconLarge), contentAlignment = Alignment.Center) {
                when {
                    problems.isNotEmpty() -> Icon(Icons.Default.Warning, contentDescription = tr("Failed"), tint = colors.error)
                    running -> AppSpinner(size = SpinnerSize.Small)
                    done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Removed"), tint = StatusTints.ok)
                }
            }
        }
    }
}

@Composable
private fun RemainsColumn(onBackup: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        CardHeader(tr("Will remain"), tr("Your app setup is not affected."), icon = AppIcons.VerifiedUser, size = CardHeaderSize.Large)
        RemainingItems()
        Spacer(Modifier.weight(1f, fill = false))
        BackupNotice {
            SecondaryButton(onClick = onBackup, enabled = enabled, modifier = Modifier.testTag("clear-backup")) { Text(tr("Create backup")) }
        }
    }
}

/** What clearing leaves alone, a row each. */
@Composable
private fun RemainingItems() {
    listOf(
        Triple(Icons.Default.Settings, tr("Settings"), tr("Themes, fonts, languages and preferences")),
        Triple(AppIcons.FileOutline, tr("Backup files"), tr("Restore one later to recover your learning data")),
        Triple(AppIcons.Translate, tr("Installed engines"), tr("Speech and translation engines remain installed")),
    ).forEach { (icon, title, detail) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(icon)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(title, style = AppText.itemTitle)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The reminder to back up first, with [action] making the backup. */
@Composable
private fun BackupNotice(action: @Composable () -> Unit) {
    Notice(tr("Back up before clearing"), message = tr("Create a recovery file in case you change your mind."), action = action)
}
