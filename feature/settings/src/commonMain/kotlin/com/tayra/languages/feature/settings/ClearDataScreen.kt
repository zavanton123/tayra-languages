package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.ClearDataService
import com.tayra.languages.core.domain.service.ClearStep
import com.tayra.languages.core.ui.components.APP_NAME
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ClearDataUiState(
    /** Whether the clearing was started; it cannot be started twice. */
    val started: Boolean = false,
    val running: ClearStep? = null,
    val done: Boolean = false,
    /** The steps that failed, with why. */
    val problems: Map<ClearStep, String> = emptyMap(),
)

class ClearDataViewModel(private val service: ClearDataService) : ViewModel() {
    private val _state = MutableStateFlow(ClearDataUiState())
    val state: StateFlow<ClearDataUiState> = combine(_state, service.running) { s, running -> s.copy(running = running) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClearDataUiState())

    fun clear() {
        if (_state.value.started) return
        _state.update { it.copy(started = true) }
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
    Scaffold(
        topBar = { AppTopBar(title = APP_NAME, onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ClearDataContent(state, onClear = viewModel::clear, onBackups = { onNavigate(Route.Backups) }, onDone = onCleared, onBack = onBack)
        }
    }
}

/** The three things the page talks about, each standing for the steps behind it. */
private enum class ClearGroup(val steps: List<ClearStep>) {
    LEARNING(listOf(ClearStep.LIBRARY)),
    OFFLINE(listOf(ClearStep.COURSE_PACKS, ClearStep.DICTIONARIES, ClearStep.VOICES, ClearStep.TRANSLATION_MODELS)),
    CACHE(listOf(ClearStep.CACHES)),
}

@Composable
internal fun ClearDataContent(state: ClearDataUiState, onClear: () -> Unit, onBackups: () -> Unit, onDone: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val wide = LocalWindowWidth.current.isExpanded
    var confirming by remember { mutableStateOf(false) }

    ScreenHeader(tr("Clear local data"), tr("Remove your learning data and downloaded files from this device. Your preferences stay."), onBackToSettings = onBack)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(16.dp)).padding(24.dp),
    ) {
        if (wide) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                RemovedColumn(state, Modifier.weight(1f).padding(end = 24.dp))
                VerticalDivider(color = colors.outlineVariant)
                RemainsColumn(onBackups, enabled = !state.started, Modifier.weight(1f).padding(start = 24.dp))
            }
        } else {
            RemovedColumn(state, Modifier.fillMaxWidth())
            HorizontalDivider(Modifier.padding(vertical = 20.dp), color = colors.outlineVariant)
            RemainsColumn(onBackups, enabled = !state.started, Modifier.fillMaxWidth())
        }
    }

    // The permanent-action bar: a warning before, what happened after.
    val tint = when {
        state.done && state.problems.isEmpty() -> StatusTints.ok
        else -> colors.error
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.07f)).border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(16.dp)).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Tile(if (state.done && state.problems.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning, tint, size = 48)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when {
                    state.done && state.problems.isEmpty() -> tr("Everything was cleared")
                    state.done -> tr("Some things could not be removed")
                    state.started -> tr("Clearing…")
                    else -> tr("This action is permanent")
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(
                when {
                    state.done && state.problems.isEmpty() -> tr("Your library is empty and the downloads are gone. Your preferences stay as they were.")
                    state.done -> tr("The rest is gone. You can try again later from here or from their Settings pages.")
                    state.started -> tr("This takes a moment; the items above show how far it is.")
                    else -> tr("Cleared data cannot be recovered unless you created a backup.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        if (wide) ActionButtons(state, onCancel = onBack, onClear = { confirming = true }, onDone = onDone)
    }
    if (!wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
            ActionButtons(state, onCancel = onBack, onClear = { confirming = true }, onDone = onDone)
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = tr("Clear all data?"),
            text = tr("This removes your books and texts, your courses and lessons with their progress, your words and flashcards, and the downloaded dictionaries, voices and translation models. Settings and backups stay. This cannot be undone."),
            confirmLabel = tr("Clear everything"),
            destructive = true,
            onConfirm = { confirming = false; onClear() },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun ActionButtons(state: ClearDataUiState, onCancel: () -> Unit, onClear: () -> Unit, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (state.done) {
        Button(onClick = onDone, modifier = Modifier.testTag("clear-done")) { Text(tr("Done")) }
        return
    }
    OutlinedButton(onClick = onCancel, enabled = !state.started, shape = RoundedCornerShape(50)) { Text(tr("Cancel")) }
    Button(
        onClick = onClear,
        enabled = !state.started,
        colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
        shape = RoundedCornerShape(50),
        modifier = Modifier.testTag("clear-all"),
    ) {
        if (state.started) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onError)
        } else {
            Icon(AppIcons.RemoveCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(if (state.started) tr("Clearing…") else tr("Clear all data"))
    }
}

@Composable
private fun RemovedColumn(state: ClearDataUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ColumnHeader(AppIcons.RemoveCircle, colors.error, tr("Will be removed"), tr("Everything you added, learned or downloaded."))
        ClearGroup.entries.forEach { group -> GroupRow(group, state) }
    }
}

@Composable
private fun GroupRow(group: ClearGroup, state: ClearDataUiState) {
    val colors = MaterialTheme.colorScheme
    val problems = group.steps.mapNotNull { state.problems[it] }
    val running = state.running != null && state.running in group.steps
    val done = state.done || (state.running != null && group.steps.all { it.ordinal < state.running.ordinal })
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("clear-${group.name}")) {
        Tile(
            when (group) {
                ClearGroup.LEARNING -> AppIcons.MenuBook
                ClearGroup.OFFLINE -> AppIcons.Download
                ClearGroup.CACHE -> AppIcons.VolumeUp
            },
            colors.primary,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when (group) {
                    ClearGroup.LEARNING -> tr("Learning data")
                    ClearGroup.OFFLINE -> tr("Offline content")
                    ClearGroup.CACHE -> tr("Cached audio")
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when {
                    problems.isNotEmpty() -> Icon(Icons.Default.Warning, contentDescription = tr("Failed"), tint = colors.error)
                    running -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Removed"), tint = StatusTints.ok)
                }
            }
        }
    }
}

@Composable
private fun RemainsColumn(onBackups: () -> Unit, enabled: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ColumnHeader(AppIcons.VerifiedUser, colors.primary, tr("Will remain"), tr("Your app setup is not affected."))
        listOf(
            Triple(Icons.Default.Settings, tr("Settings"), tr("Themes, fonts, languages and preferences")),
            Triple(AppIcons.FileOutline, tr("Backup files"), tr("Restore one later to recover your learning data")),
            Triple(AppIcons.Translate, tr("Installed engines"), tr("Speech and translation engines remain installed")),
        ).forEach { (icon, title, detail) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Tile(icon, colors.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.07f)).border(1.dp, colors.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Back up before clearing"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.primary)
                Text(tr("Create a recovery file in case you change your mind."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBackups, enabled = enabled, shape = RoundedCornerShape(50), modifier = Modifier.testTag("clear-backup")) { Text(tr("Create backup")) }
        }
    }
}

@Composable
private fun ColumnHeader(icon: ImageVector, tint: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Tile(icon, tint, size = 56)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A rounded square in a tint, with the icon in it. */
@Composable
private fun Tile(icon: ImageVector, tint: Color, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.09f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}
