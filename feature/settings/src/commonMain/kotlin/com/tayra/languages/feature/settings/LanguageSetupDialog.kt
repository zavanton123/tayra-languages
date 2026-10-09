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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
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
import com.tayra.languages.core.domain.service.SetupStatus
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.ScrollColumn
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
                setup.overview(languageId).sortedBy { it.status }
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
        if (s.started || s.items.orEmpty().none { it.id == id && it.status == SetupStatus.MISSING }) s else s.copy(selected = if (id in s.selected) s.selected - id else s.selected + id)
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
                    val nothingMissing = items != null && items.none { it.status == SetupStatus.MISSING }
                    Text(
                        when {
                            items == null -> tr("Checking what can be downloaded…")
                            nothingMissing -> tr("Everything available for {0} is already on this device.", tr(items.first().languageName))
                            else -> tr("Choose what to download now. You can change this later in Settings.")
                        },
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
            // The scrollbar (desktop only) runs in the gutter the cards leave at the end.
            ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                setupGroups(items).forEach { group ->
                    val item = group.first()
                    if (item.kind == SetupKind.VOICE && item.engine != null) {
                        VoicesCard(group, state.selected, state.started, progress, onToggle)
                    } else {
                        SetupCard(item, checked = item.id in state.selected, started = state.started, progress = progress[item.id], onToggle = { onToggle(item.id) })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            val chosen = items.filter { it.id in state.selected }
            val running = state.started && chosen.any { progress[it.id].let { p -> p == null || p is SetupState.Waiting || p is SetupState.Running } }
            val failed = state.started && !running && chosen.any { progress[it.id] is SetupState.Failed }
            if (items.none { it.status == SetupStatus.MISSING }) {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onClosed, modifier = Modifier.testTag("setup-done")) { Text(tr("Done")) }
                }
                return@Column
            }
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

/** The items one card each, but the voices of one engine together, where the first of them is. */
private fun setupGroups(items: List<SetupItem>): List<List<SetupItem>> {
    val voices = items.filter { it.kind == SetupKind.VOICE && it.engine != null }.groupBy { it.engine }
    return items.mapNotNull { item ->
        val engine = item.engine?.takeIf { item.kind == SetupKind.VOICE } ?: return@mapNotNull listOf(item)
        voices.getValue(engine).takeIf { it.first() === item }
    }
}

/**
 * The size of [items] together, "about" when part of it is estimated or unknown; null when nothing
 * is known. An engine several voices bring is downloaded once, so it is counted once.
 */
@Composable
private fun totalSize(items: List<SetupItem>): String? {
    val files = items.flatMap { it.files }.distinct()
    val bytes = files.mapNotNull { it.sizeBytes }.takeIf { it.isNotEmpty() }?.sum() ?: return null
    return sizeText(bytes, estimated = files.any { it.estimated || it.sizeBytes == null })
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
    val missing = item.status == SetupStatus.MISSING
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (item.status == SetupStatus.UNAVAILABLE) 0.6f else 1f)
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(enabled = !started && missing, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("setup-${item.kind.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusMark(item, checked, started, progress, onToggle)
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
                when (item.status) {
                    SetupStatus.INSTALLED -> Text(tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok, modifier = Modifier.padding(top = 2.dp))
                    SetupStatus.UNAVAILABLE -> Unit
                    SetupStatus.MISSING -> item.sizeBytes?.let {
                        Text(sizeText(it, item.sizeEstimated), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Text(
                if (item.status == SetupStatus.UNAVAILABLE) tr("Not available for {0} yet", tr(item.languageName)) else when (item.kind) {
                    SetupKind.COURSES -> tr("100 graded courses of mini stories, from A1 to C2")
                    SetupKind.DICTIONARY -> tr("Look up {0} words without internet", languageInSentence(item.languageName, LanguageCase.PREPOSITIONAL))
                    SetupKind.VOICE -> tr("Hear books and words read aloud")
                    SetupKind.TRANSLATION -> tr("Translate sentences without internet")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            // A course pack is named after its language, as the title is; a voice that brings its engine lists both files.
            if (item.kind != SetupKind.COURSES && item.name.isNotBlank() && (item.kind != SetupKind.VOICE || item.files.size <= 1)) {
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

/** The tick box before a download, or where it stands once on the device or started. */
@Composable
private fun StatusMark(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        when {
            item.status == SetupStatus.INSTALLED -> Icon(Icons.Default.CheckCircle, contentDescription = tr("On this device"), tint = StatusTints.ok, modifier = Modifier.size(26.dp))
            item.status == SetupStatus.UNAVAILABLE -> Icon(AppIcons.RemoveCircle, contentDescription = tr("Not available"), tint = colors.onSurfaceVariant, modifier = Modifier.size(26.dp))
            !started || !checked -> Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !started)
            progress is SetupState.Done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Downloaded"), tint = StatusTints.ok, modifier = Modifier.size(26.dp))
            progress is SetupState.Failed -> Icon(Icons.Default.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.size(26.dp))
            progress is SetupState.Running -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            else -> Box(Modifier.size(22.dp).border(2.dp, colors.outlineVariant, CircleShape))
        }
    }
}

/**
 * The voices one engine has for the language, each to tick on its own: "Greek voice · Piper", what
 * its voices share (the engine's runtime, Kokoro's model), downloaded once with the first voice, then a row per voice.
 */
@Composable
private fun VoicesCard(voices: List<SetupItem>, selected: Set<String>, started: Boolean, progress: Map<String, SetupState>, onToggle: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val first = voices.first()
    val engine = first.engine.orEmpty()
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("setup-VOICE-$engine"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val missing = voices.filter { it.status == SetupStatus.MISSING }
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (missing.isNotEmpty() && !started) {
                    val chosen = missing.count { it.id in selected }
                    val all = chosen == missing.size
                    TriStateCheckbox(
                        state = when (chosen) {
                            0 -> ToggleableState.Off
                            missing.size -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        },
                        // Ticks every voice, or clears them all once every one is ticked.
                        onClick = { missing.filter { (it.id in selected) == all }.forEach { onToggle(it.id) } },
                        modifier = Modifier.testTag("setup-all-$engine").semantics { contentDescription = tr("All voices") },
                    )
                }
            }
            IconTile(AppIcons.VolumeUp, size = 56)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${tr("{0} voice", tr(first.languageName))} \u00b7 $engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    // What all its voices take together, the shared model and runtime once.
                    totalSize(missing)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).testTag("setup-total-$engine"))
                    }
                }
                Text(tr("Hear books and words read aloud"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                voices.flatMap { it.files }.filter { it.kind == SetupFileKind.ENGINE }.distinct().forEach { FileLine(it) }
                if (voices.any { it.switchesEngine }) {
                    Text(tr("Becomes your speech engine"), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }
        }
        // A model the voices share is a download of its own while it is not on the device yet.
        voices.flatMap { it.files }.firstOrNull { it.kind == SetupFileKind.VOICE_MODEL }?.let { model ->
            val chosen = voices.filter { it.id in selected }
            // The first chosen voice is the one that fetches the model.
            ModelRow(engine, model, needed = chosen.isNotEmpty(), started, chosen.firstOrNull()?.let { progress[it.id] })
        }
        voices.forEach { voice -> VoiceRow(voice, voice.id in selected, started, progress[voice.id], onToggle = { onToggle(voice.id) }) }
    }
}

/** The model an engine's voices need, ticked whenever one of them is: it cannot be chosen apart from them. */
@Composable
private fun ModelRow(engine: String, model: SetupFile, needed: Boolean, started: Boolean, progress: SetupState?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("setup-model-$engine"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (started) {
            StatusMark(SetupItem("", SetupKind.VOICE, "", "", emptyList(), recommended = false), needed, started = true, progress = progress, onToggle = {})
        } else {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Checkbox(checked = needed, onCheckedChange = null) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tr("{0} model", engine), style = MaterialTheme.typography.bodyLarge)
            Text(tr("Needed by every voice, downloaded once"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        model.sizeBytes?.let { Text(sizeText(it, model.estimated), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    }
}

/** One voice of an engine: its name and, when it is a download of its own, its size. */
@Composable
private fun VoiceRow(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val missing = item.status == SetupStatus.MISSING
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !started && missing, onClick = onToggle)
            .padding(vertical = 4.dp)
            .testTag("setup-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusMark(item, checked, started, progress, onToggle)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name.substringAfter(" \u00b7 "), style = MaterialTheme.typography.bodyLarge)
            if (progress is SetupState.Failed) Text(tr("Download failed: {0}", progress.message), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        if (item.status == SetupStatus.INSTALLED) {
            Text(tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
        } else {
            val voice = item.files.firstOrNull { it.kind == SetupFileKind.VOICE }
            val size = voice?.sizeBytes?.let { sizeText(it, voice.estimated) } ?: item.voiceSizeBytes?.let(::formatSize)
            size?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
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
        SetupFileKind.VOICE_MODEL -> MODEL_TITLE.matchEntire(file.name)?.let { match ->
            trPlural(match.groupValues[2].toInt(), "{1} model with {0} voice", "{1} model with {0} voices", match.groupValues[1])
        } ?: file.name
        SetupFileKind.COURSES, SetupFileKind.DICTIONARY -> localizedNames(file.name)
    }
    val size = file.sizeBytes?.let { sizeText(it, file.estimated) } ?: tr("size unknown")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
        Text("$name · $size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Kokoro model with 34 voices", as the engines name a model of several voices. */
private val MODEL_TITLE = Regex("""(.+) model with (\d+) voices""")
