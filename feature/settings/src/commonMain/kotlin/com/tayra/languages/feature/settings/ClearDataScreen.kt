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
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
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

@Composable
internal fun ClearDataContent(state: ClearDataUiState, onClear: () -> Unit, onBackups: () -> Unit, onDone: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val wide = LocalWindowWidth.current.isExpanded
    var confirming by remember { mutableStateOf(false) }

    ScreenHeader(tr("Clear"), tr("Remove everything Tayra keeps on this device and start afresh."), onBackToSettings = onBack)
    val removed: @Composable (Modifier) -> Unit = { m -> RemovedCard(state, m) }
    val kept: @Composable (Modifier) -> Unit = { m -> KeptCard(m) }
    if (wide) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            removed(Modifier.weight(3f).fillMaxHeight())
            kept(Modifier.weight(2f).fillMaxHeight())
        }
    } else {
        removed(Modifier)
        kept(Modifier)
    }

    when {
        state.done && state.problems.isEmpty() -> InfoBanner(tr("Everything was cleared."), tint = StatusTints.ok, icon = Icons.Default.CheckCircle)
        state.done -> InfoBanner(tr("Some things could not be removed; the rest is gone. You can try again later from here or from their Settings pages."), tint = colors.error, icon = Icons.Default.Warning)
        else -> InfoBanner(tr("This cannot be undone. Make a backup first if you may want any of it back."))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
        if (state.done) {
            Button(onClick = onDone, modifier = Modifier.testTag("clear-done")) { Text(tr("Done")) }
        } else {
            OutlinedButton(onClick = onBackups, enabled = !state.started) { Text(tr("Back up first")) }
            Button(
                onClick = { confirming = true },
                enabled = !state.started,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                modifier = Modifier.testTag("clear-all"),
            ) {
                if (state.started) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onError)
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Clearing…"))
                } else {
                    Icon(AppIcons.RemoveCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Clear all data"))
                }
            }
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
private fun RemovedCard(state: ClearDataUiState, modifier: Modifier) {
    ContentCard(tr("What gets removed"), tr("Everything you added or downloaded."), icon = AppIcons.RemoveCircle, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ClearStep.entries.forEach { step -> StepRow(step, state) }
        }
    }
}

@Composable
private fun StepRow(step: ClearStep, state: ClearDataUiState) {
    val colors = MaterialTheme.colorScheme
    val problem = state.problems[step]
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.testTag("clear-${step.name}")) {
        IconTile(
            when (step) {
                ClearStep.LIBRARY -> AppIcons.MenuBook
                ClearStep.COURSE_PACKS -> AppIcons.Download
                ClearStep.DICTIONARIES -> AppIcons.Book
                ClearStep.VOICES -> AppIcons.VolumeUp
                ClearStep.TRANSLATION_MODELS -> AppIcons.Translate
                ClearStep.CACHES -> AppIcons.Storage
            },
            size = 40,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when (step) {
                    ClearStep.LIBRARY -> tr("Books, courses and words")
                    ClearStep.COURSE_PACKS -> tr("Downloaded courses")
                    ClearStep.DICTIONARIES -> tr("Offline dictionaries")
                    ClearStep.VOICES -> tr("Voices")
                    ClearStep.TRANSLATION_MODELS -> tr("Translation models")
                    ClearStep.CACHES -> tr("Cached audio")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when (step) {
                    ClearStep.LIBRARY -> tr("Your texts, your own courses and lessons, course progress, reading history, words and flashcards")
                    ClearStep.COURSE_PACKS -> tr("The ready-made course packs and their lessons")
                    ClearStep.DICTIONARIES -> tr("Every downloaded dictionary pack")
                    ClearStep.VOICES -> tr("Every downloaded voice; the speech engines stay")
                    ClearStep.TRANSLATION_MODELS -> tr("Every downloaded language model; the translator stays")
                    ClearStep.CACHES -> tr("Sentences read aloud and kept for replay")
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (problem != null) Text(tr("Could not remove: {0}", problem), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        if (state.started) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when {
                    problem != null -> Icon(Icons.Default.Warning, contentDescription = tr("Failed"), tint = colors.error)
                    state.running == step -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    state.done || (state.running != null && step.ordinal < state.running.ordinal) ->
                        Icon(Icons.Default.CheckCircle, contentDescription = tr("Removed"), tint = StatusTints.ok)
                }
            }
        }
    }
}

@Composable
private fun KeptCard(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    ContentCard(tr("What stays"), tr("Nothing here is touched."), icon = AppIcons.VerifiedUser, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                tr("Settings") to tr("Themes, fonts, languages and every other preference"),
                tr("Backups") to tr("Restore one afterwards to get your data back"),
                tr("Speech and translation engines") to tr("Only their downloaded voices and models go"),
            ).forEach { (title, detail) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
