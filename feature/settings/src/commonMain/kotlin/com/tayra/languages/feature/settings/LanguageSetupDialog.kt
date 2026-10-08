package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.tayra.languages.core.domain.service.SetupFile
import com.tayra.languages.core.domain.service.SetupFileKind
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
import com.tayra.languages.core.ui.i18n.trPlural
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
    val colors = MaterialTheme.colorScheme
    val items = state.items
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surface, modifier = Modifier.widthIn(max = 640.dp).padding(vertical = 24.dp).testTag("language-setup")) {
        Column(Modifier.padding(start = 28.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 12.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val name = items?.firstOrNull()?.languageName
                    Text(
                        if (name != null) tr("Get ready to learn {0}", languageInSentence(name, LanguageCase.NOMINATIVE)) else tr("Getting ready…"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (items == null) tr("Checking what can be downloaded…") else tr("Choose what to download now. You can change this later in Settings."),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onClosed,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = colors.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("setup-close"),
                ) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
            }
            Spacer(Modifier.height(20.dp))
            if (items == null) {
                Box(Modifier.fillMaxWidth().padding(end = 8.dp).heightIn(min = 120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            Column(Modifier.weight(1f, fill = false).padding(end = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    SetupCard(item, checked = item.id in state.selected, started = state.started, progress = progress[item.id], onToggle = { onToggle(item.id) })
                }
            }
            Spacer(Modifier.height(20.dp))
            val chosen = items.filter { it.id in state.selected }
            val running = state.started && chosen.any { progress[it.id].let { p -> p == null || p is SetupState.Waiting || p is SetupState.Running } }
            val failed = state.started && !running && chosen.any { progress[it.id] is SetupState.Failed }
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        !state.started -> listOfNotNull(
                            trPlural(chosen.size, "{0} download selected", "{0} downloads selected"),
                            totalSize(chosen),
                        ).joinToString(" · ")
                        running -> tr("Downloads go on in the background if you close this.")
                        failed -> tr("Some downloads failed.")
                        else -> tr("Everything is downloaded.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!state.started) {
                    TextButton(onClick = onClosed) { Text(tr("Set up later")) }
                    Button(onClick = onStart, enabled = chosen.isNotEmpty(), modifier = Modifier.testTag("setup-download")) {
                        Text(tr("Download selected"))
                    }
                } else {
                    if (failed) TextButton(onClick = onRetry) { Text(tr("Try again")) }
                    Button(onClick = onClosed) { Text(if (running) tr("Close") else tr("Done")) }
                }
            }
        }
    }
}

/** The size of [items] together, "about" when part of it is estimated or unknown; null when nothing is known. */
@Composable
private fun totalSize(items: List<SetupItem>): String? {
    val bytes = items.mapNotNull { it.sizeBytes }.takeIf { it.isNotEmpty() }?.sum() ?: return null
    return sizeText(bytes, estimated = items.any { it.sizeEstimated })
}

private fun sizeText(bytes: Long, estimated: Boolean): String = if (estimated) tr("about {0}", formatSize(bytes)) else formatSize(bytes)

/** "Greek → Russian" with the language names in the interface language, and each part of a "·" list the same way. */
private fun localizedNames(text: String): String = text.split(" · ").joinToString(" · ") { part ->
    if (" \u2192 " in part) part.split(" \u2192 ").joinToString(" \u2192 ") { tr(it) } else part
}

@Composable
private fun SetupCard(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(enabled = !started, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("setup-${item.kind.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            when {
                !started || !checked -> Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !started)
                progress is SetupState.Done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Downloaded"), tint = StatusTints.ok, modifier = Modifier.size(26.dp))
                progress is SetupState.Failed -> Icon(Icons.Default.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.size(26.dp))
                progress is SetupState.Running -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                else -> Box(Modifier.size(22.dp).border(2.dp, colors.outlineVariant, CircleShape))
            }
        }
        IconTile(
            when (item.kind) {
                SetupKind.COURSES -> AppIcons.MenuBook
                SetupKind.DICTIONARY -> AppIcons.Book
                SetupKind.VOICE -> AppIcons.VolumeUp
                SetupKind.TRANSLATION -> AppIcons.Translate
            },
            size = 56,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when (item.kind) {
                        SetupKind.COURSES -> tr("{0} course pack", tr(item.languageName))
                        SetupKind.DICTIONARY -> tr("Offline dictionary")
                        SetupKind.VOICE -> tr("{0} voice", tr(item.languageName))
                        SetupKind.TRANSLATION -> tr("Offline translation")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                item.sizeBytes?.let {
                    Text(sizeText(it, item.sizeEstimated), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(
                when (item.kind) {
                    SetupKind.COURSES -> tr("100 graded courses of mini stories, from A1 to C2")
                    SetupKind.DICTIONARY -> tr("Look up {0} words without internet", languageInSentence(item.languageName, LanguageCase.PREPOSITIONAL))
                    SetupKind.VOICE -> tr("Hear books and words read aloud")
                    SetupKind.TRANSLATION -> tr("Translate sentences without internet")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            // A course pack is named after its language, as the title is; a voice that brings its engine lists both files.
            if (item.kind != SetupKind.COURSES && (item.kind != SetupKind.VOICE || item.files.size == 1)) {
                Text(localizedNames(item.name), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.8f))
            }
            if (item.files.size > 1) {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    item.files.forEach { file -> FileLine(file) }
                }
            }
            if (item.switchesEngine) {
                Text(
                    if (item.kind == SetupKind.VOICE) tr("Becomes your speech engine") else tr("Becomes your translation engine"),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                )
            }
            if (progress is SetupState.Failed) Text(tr("Download failed: {0}", progress.message), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
    }
}

/** One file of a download with its size: "Piper engine · about 86 MB". */
@Composable
private fun FileLine(file: SetupFile) {
    val name = when (file.kind) {
        SetupFileKind.ENGINE -> tr("{0} engine", file.name)
        SetupFileKind.VOICE -> tr("Voice: {0}", file.name)
        SetupFileKind.MODEL -> tr("{0} model", localizedNames(file.name))
        SetupFileKind.COURSES, SetupFileKind.DICTIONARY -> localizedNames(file.name)
    }
    val size = file.sizeBytes?.let { sizeText(it, file.estimated) } ?: tr("size unknown")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
        Text("$name · $size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
