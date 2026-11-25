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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.backup.BackupException
import com.idiomiko.core.domain.backup.BackupRepository
import com.idiomiko.core.domain.service.ClearDataService
import com.idiomiko.core.domain.service.ClearStep
import com.idiomiko.core.ui.components.APP_NAME
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.ConfirmDialog
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.rememberToastState
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ClearDataUiState(
    /** Whether the clearing was started; it cannot be started twice. */
    val started: Boolean = false,
    val running: ClearStep? = null,
    val done: Boolean = false,
    /** The steps that failed, with why. */
    val problems: Map<ClearStep, String> = emptyMap(),
    /** Whether the backup the reminder offers is being made; clearing waits for it. */
    val backingUp: Boolean = false,
    /** How that backup went, shown for a moment over the page. */
    val message: String? = null,
)

class ClearDataViewModel(private val service: ClearDataService, private val backups: BackupRepository) : ViewModel() {
    private val _state = MutableStateFlow(ClearDataUiState())
    val state: StateFlow<ClearDataUiState> = combine(_state, service.running) { s, running -> s.copy(running = running) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClearDataUiState())

    /** Makes a backup before clearing, as the reminder suggests. */
    fun backup() {
        if (_state.value.started || _state.value.backingUp) return
        _state.update { it.copy(backingUp = true, message = null) }
        viewModelScope.launch {
            val message = try {
                tr("Backup of {0} created", formatBackupTime(backups.create()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                // The repository's messages are fixed texts, translated like the screen's own.
                e.message?.let(::tr)
            } catch (e: Exception) {
                Logger.w(e) { "Could not back up before clearing" }
                tr("Could not create the backup: {0}", e.message ?: e::class.simpleName.orEmpty())
            }
            _state.update { it.copy(backingUp = false, message = message) }
        }
    }

    fun messageSeen() = _state.update { it.copy(message = null) }

    /** Removes everything the app keeps on the device except the settings and the backups; it runs once. */
    fun clear() {
        if (_state.value.started || _state.value.backingUp) return
        _state.update { it.copy(started = true, message = null) }
        viewModelScope.launch {
            val problems = service.clearEverything()
            _state.update { it.copy(done = true, problems = problems) }
        }
    }
}

/** Settings: one button that removes everything the app keeps on the device, after a confirmation. */
@Composable
fun ClearDataScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, onCleared: () -> Unit, viewModel: ClearDataViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    LaunchedEffect(state.message) {
        state.message?.let { toast.show(it); viewModel.messageSeen() }
    }
    if (LocalWindowWidth.current.isCompact) {
        PhoneClearDataScreen(state, onClear = viewModel::clear, onBackup = viewModel::backup, onDone = onCleared, onNavigate = onNavigate, onBack = onBack)
    } else Scaffold(
        topBar = { AppTopBar(title = APP_NAME, onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.CLEAR_DATA, padding, onNavigate) { inner ->
            PageColumn(inner) {
                ClearDataContent(state, onClear = viewModel::clear, onBackup = viewModel::backup, onDone = onCleared, onBack = onBack)
            }
        }
    }
    ToastHost(toast)
}

/** The three things the page talks about, each standing for the steps behind it. */
internal enum class ClearGroup(val steps: List<ClearStep>) {
    LEARNING(listOf(ClearStep.LIBRARY)),
    OFFLINE(listOf(ClearStep.COURSE_PACKS, ClearStep.DICTIONARIES, ClearStep.VOICES, ClearStep.TRANSLATION_MODELS)),
    CACHE(listOf(ClearStep.CACHES)),
}

/**
 * The page below the bar: what clearing removes beside what it leaves, the reminder to back up first, and the
 * permanent-action notice with Cancel, which goes back, and the one button that clears everything after a confirmation.
 */
@Composable
internal fun ClearDataContent(state: ClearDataUiState, onClear: () -> Unit, onBackup: () -> Unit, onDone: () -> Unit, onBack: () -> Unit) {
    val wide = LocalWindowWidth.current.isExpanded
    var confirming by remember { mutableStateOf(false) }

    ScreenHeader(tr("Clear local data"), null, onBackToSettings = onBack)
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl)) {
        if (wide) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                RemovedColumn(state, Modifier.weight(1f).padding(end = AppSpacing.xxl))
                AppVerticalDivider()
                RemainsColumn(state, onBackup, Modifier.weight(1f).padding(start = AppSpacing.xxl))
            }
        } else {
            RemovedColumn(state, Modifier.fillMaxWidth())
            AppDivider(Modifier.padding(vertical = AppSpacing.xl))
            RemainsColumn(state, onBackup, Modifier.fillMaxWidth())
        }
    }

    val buttons: @Composable () -> Unit = { ActionButtons(state, onCancel = onBack, onClear = { confirming = true }, onDone = onDone) }
    ClearStatusNotice(state, action = if (wide) ({ Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) { buttons() } }) else null)
    if (!wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.End)) { buttons() }
    }

    if (confirming) ClearConfirmDialog(onConfirm = { confirming = false; onClear() }, onDismiss = { confirming = false })
}

@Composable
internal fun ClearConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
internal fun ClearStatusNotice(state: ClearDataUiState, action: (@Composable () -> Unit)?) {
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
private fun clearStatusText(state: ClearDataUiState): String? = when {
    state.done && state.problems.isEmpty() -> tr("Your library is empty and the downloads are gone. Your preferences stay as they were.")
    state.done -> tr("The rest is gone. You can try again later from here or from their Settings pages.")
    state.started -> tr("This takes a moment; the items above show how far it is.")
    else -> null
}

@Composable
private fun ActionButtons(state: ClearDataUiState, onCancel: () -> Unit, onClear: () -> Unit, onDone: () -> Unit) {
    if (state.done) {
        PrimaryButton(onClick = onDone, modifier = Modifier.testTag("clear-done")) { Text(tr("Done")) }
        return
    }
    SecondaryButton(onClick = onCancel, enabled = !state.started, modifier = Modifier.testTag("clear-cancel")) { Text(tr("Cancel")) }
    PrimaryButton(
        if (state.started) tr("Clearing…") else tr("Clear all data"),
        onClick = onClear,
        modifier = Modifier.testTag("clear-all"),
        icon = AppIcons.RemoveCircle,
        enabled = !state.started && !state.backingUp,
        loading = state.started,
        tone = ButtonTone.Danger,
    )
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
internal fun GroupRow(group: ClearGroup, state: ClearDataUiState) {
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
private fun RemainsColumn(state: ClearDataUiState, onBackup: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        CardHeader(tr("Will remain"), tr("Your app setup is not affected."), icon = AppIcons.VerifiedUser, size = CardHeaderSize.Large)
        RemainingItems()
        Spacer(Modifier.weight(1f, fill = false))
        BackupNotice {
            SecondaryButton(tr("Create backup"), onClick = onBackup, modifier = Modifier.testTag("clear-backup"), enabled = !state.started, loading = state.backingUp)
        }
    }
}

/** What clearing leaves alone, a row each. */
@Composable
internal fun RemainingItems() {
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
internal fun BackupNotice(action: @Composable () -> Unit) {
    Notice(tr("Back up before clearing"), action = action)
}
