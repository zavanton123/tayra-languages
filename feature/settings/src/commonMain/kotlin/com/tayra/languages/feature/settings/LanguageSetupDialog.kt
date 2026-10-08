package com.tayra.languages.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.LanguageSetupService
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class LanguageSetupUiState(
    /** The language the downloads are for. */
    val languageId: Long? = null,
    /** Null while the downloads are being looked up. */
    val items: List<SetupItem>? = null,
    val selected: Set<String> = emptySet(),
    /** Whether the chosen downloads were started. */
    val started: Boolean = false,
)

class LanguageSetupViewModel(private val setup: LanguageSetupService) : ViewModel() {
    private val _state = MutableStateFlow(LanguageSetupUiState())
    val state: StateFlow<LanguageSetupUiState> = _state.asStateFlow()

    val progress: StateFlow<Map<String, SetupState>> = setup.states

    private var loading: Job? = null

    /** Looks up [languageId]'s downloads afresh, each time the dialog opens. */
    fun load(languageId: Long) {
        if (_state.value.languageId == languageId) return
        loading?.cancel()
        _state.value = LanguageSetupUiState(languageId)
        loading = viewModelScope.launch {
            val items = try {
                setup.missing(languageId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.value = LanguageSetupUiState(languageId, items, items.filter { it.recommended }.map { it.id }.toSet())
        }
    }

    /** Forgets the closed dialog's language, so the next opening looks again. */
    fun reset() {
        loading?.cancel()
        _state.value = LanguageSetupUiState()
    }

    fun toggle(id: String) = _state.update { s ->
        if (s.started) s else s.copy(selected = if (id in s.selected) s.selected - id else s.selected + id)
    }

    fun start() {
        val s = _state.value
        val items = s.items.orEmpty().filter { it.id in s.selected }
        if (items.isEmpty() || s.started) return
        setup.install(items)
        _state.update { it.copy(started = true) }
    }

    /** Starts the failed downloads again. */
    fun retry() {
        val failed = _state.value.items.orEmpty().filter { progress.value[it.id] is SetupState.Failed }
        if (failed.isNotEmpty()) setup.install(failed)
    }
}

/**
 * The downloads a newly chosen language can use, shown once it is chosen: what is not on the
 * device yet, the usual ones ticked. Closing it leaves started downloads running. Nothing is shown
 * when the language has everything already.
 */
@Composable
fun LanguageSetupDialog(languageId: Long?, onClosed: () -> Unit, viewModel: LanguageSetupViewModel = koinViewModel()) {
    if (languageId == null) return
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    LaunchedEffect(languageId) { viewModel.load(languageId) }
    // What an earlier opening left for another language is not this one's: it is still loading.
    val state = loaded.takeIf { it.languageId == languageId } ?: LanguageSetupUiState(languageId)
    val close = {
        viewModel.reset()
        onClosed()
    }
    val items = state.items
    LaunchedEffect(items) { if (items != null && items.isEmpty()) close() }
    if (items != null && items.isEmpty()) return

    Dialog(onDismissRequest = close) {
        LanguageSetupContent(state, progress, onToggle = viewModel::toggle, onStart = viewModel::start, onRetry = viewModel::retry, onClosed = close)
    }
}

/** The dialog's card: the downloads to choose, then their progress. */
@Composable
fun LanguageSetupContent(
    state: LanguageSetupUiState,
    progress: Map<String, SetupState>,
    onToggle: (String) -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onClosed: () -> Unit,
) {
    val items = state.items
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.widthIn(max = 600.dp).testTag("language-setup")) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val name = items?.firstOrNull()?.languageName
            Text(
                if (name != null) tr("Get ready to learn {0}", languageInSentence(name, LanguageCase.NOMINATIVE)) else tr("Getting ready…"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (items == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(tr("Checking what can be downloaded…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClosed) { Text(tr("Not now")) } }
                return@Column
            }
            Text(
                tr("Choose what to download now. Everything stays available later in Settings."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                items.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SetupRow(item, checked = item.id in state.selected, started = state.started, progress = progress[item.id], onToggle = { onToggle(item.id) })
                }
            }
            val running = state.started && items.any { it.id in state.selected && progress[it.id].let { p -> p == null || p is SetupState.Waiting || p is SetupState.Running } }
            if (running) {
                Text(tr("Downloads go on in the background if you close this."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                if (!state.started) {
                    TextButton(onClick = onClosed) { Text(tr("Not now")) }
                    Button(onClick = onStart, enabled = state.selected.isNotEmpty(), modifier = Modifier.testTag("setup-download")) {
                        Text(tr("Download ({0})", state.selected.size))
                    }
                } else {
                    val failed = !running && items.any { progress[it.id] is SetupState.Failed }
                    if (failed) TextButton(onClick = onRetry) { Text(tr("Try again")) }
                    Button(onClick = onClosed) { Text(if (running) tr("Close") else tr("Done")) }
                }
            }
        }
    }
}

@Composable
private fun SetupRow(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled = !started, onClick = onToggle).padding(vertical = 12.dp).testTag("setup-${item.kind.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            !started || !checked -> Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !started)
            progress is SetupState.Done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Downloaded"), tint = StatusTints.ok, modifier = Modifier.padding(12.dp).size(24.dp))
            progress is SetupState.Failed -> Icon(Icons.Default.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.padding(12.dp).size(24.dp))
            progress is SetupState.Running -> CircularProgressIndicator(Modifier.padding(14.dp).size(20.dp), strokeWidth = 2.dp)
            else -> Box(Modifier.padding(14.dp).size(20.dp).border(2.dp, colors.outlineVariant, CircleShape))
        }
        IconTile(
            when (item.kind) {
                SetupKind.COURSES -> AppIcons.MenuBook
                SetupKind.DICTIONARY -> AppIcons.Book
                SetupKind.VOICE -> AppIcons.VolumeUp
                SetupKind.TRANSLATION -> AppIcons.Translate
            },
            size = 40,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                when (item.kind) {
                    SetupKind.COURSES -> tr("Courses")
                    SetupKind.DICTIONARY -> tr("Offline dictionary")
                    SetupKind.VOICE -> tr("Voice")
                    SetupKind.TRANSLATION -> tr("Offline translation")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when (item.kind) {
                    SetupKind.COURSES -> tr("100 graded courses of mini stories, from A1 to C2")
                    SetupKind.DICTIONARY -> tr("{0}: look words up without internet", item.name)
                    SetupKind.VOICE -> tr("{0}: hear texts read aloud offline", item.name)
                    SetupKind.TRANSLATION -> tr("{0}: translate sentences without internet", item.name)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            val notes = listOfNotNull(
                tr("Includes the engine itself, a large download").takeIf { item.includesRuntime },
                when {
                    !item.switchesEngine -> null
                    item.kind == SetupKind.VOICE -> tr("Becomes your speech engine")
                    else -> tr("Becomes your translation engine")
                },
            )
            if (notes.isNotEmpty()) Text(notes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.primary)
            if (progress is SetupState.Failed) Text(tr("Download failed: {0}", progress.message), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        item.sizeBytes?.let { Text(formatSize(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        Spacer(Modifier.size(4.dp))
    }
}
