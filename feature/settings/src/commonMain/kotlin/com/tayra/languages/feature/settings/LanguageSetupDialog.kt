package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LocalWindowWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
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
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTriStateCheckbox
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
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

    // A phone gets a bottom sheet; wider screens the floating dialog.
    if (LocalWindowWidth.current.isCompact) {
        PhoneSetupSheet(state, progress, onToggle = viewModel::toggle, onStart = viewModel::start, onRetry = viewModel::retry, onClosed = close)
        return
    }
    AppDialogSurface(onDismissRequest = close, maxWidth = 640.dp) {
        LanguageSetupContent(state, progress, onToggle = viewModel::toggle, onStart = viewModel::start, onRetry = viewModel::retry, onClosed = close)
    }
}

/** The downloads as a phone's bottom sheet: the question, the cards to tick, and the choice summed up beside the download button. */
@Composable
internal fun PhoneSetupSheet(
    state: LanguageSetupUiState,
    progress: Map<String, SetupState>,
    onToggle: (String) -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onClosed: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val items = state.items
    AppBottomSheet(onDismissRequest = onClosed, modifier = Modifier.testTag("language-setup")) {
        // The list gives way, so the buttons below it stay on screen however long it is.
        Column(Modifier.weight(1f, fill = false).padding(horizontal = AppSpacing.l)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppIconButton(
                    Icons.Default.Close,
                    contentDescription = tr("Close"),
                    onClick = onClosed,
                    modifier = Modifier.testTag("setup-close"),
                    style = IconButtonStyle.Tonal,
                    tint = colors.onSurface,
                )
            }
            val name = items?.firstOrNull()?.languageName
            Text(
                if (name != null) tr("Get ready to learn {0}", languageInSentence(name, LanguageCase.NOMINATIVE)) else tr("Getting ready…"),
                style = AppText.headline,
            )
            Spacer(Modifier.height(AppSpacing.s))
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
            Spacer(Modifier.height(AppSpacing.l))
            if (items == null) {
                Box(Modifier.fillMaxWidth().heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
                return@Column
            }
            ScrollColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                setupGroups(items).forEach { group ->
                    val item = group.first()
                    if (item.kind == SetupKind.VOICE && item.engine != null) {
                        VoicesCard(group, state.selected, state.started, progress, onToggle, compact = true)
                    } else {
                        SetupCard(item, checked = item.id in state.selected, started = state.started, progress = progress[item.id], onToggle = { onToggle(item.id) }, compact = true)
                    }
                }
                // Without courses a new learner needs another way in: the texts the app can find, or one of their own.
                if (items.any { it.kind == SetupKind.COURSES && it.status == SetupStatus.UNAVAILABLE }) InfoBanner(tr("No ready-made courses yet. Read Wikipedia articles under News, find free books, or add a text of your own."))
            }
            Spacer(Modifier.height(AppSpacing.m))
        }
        AppDivider()
        val chosen = items?.filter { it.id in state.selected }.orEmpty()
        val running = state.started && chosen.any { progress[it.id].let { p -> p == null || p is SetupState.Waiting || p is SetupState.Running } }
        val failed = state.started && !running && chosen.any { progress[it.id] is SetupState.Failed }
        Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            when {
                items != null && items.none { it.status == SetupStatus.MISSING } -> {
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(onClick = onClosed, modifier = Modifier.testTag("setup-done")) { Text(tr("Done")) }
                }
                !state.started -> {
                    // What is chosen, and the way out, stacked at the start; the download button takes the rest.
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                        Text(
                            listOfNotNull(trPlural(chosen.size, "{0} download selected", "{0} downloads selected"), totalSize(chosen)).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        QuietButton(onClick = onClosed, size = ButtonSize.Compact) { Text(tr("Set up later"), style = MaterialTheme.typography.titleMedium) }
                    }
                    PrimaryButton(
                        tr("Download selected"),
                        onClick = onStart,
                        modifier = Modifier.weight(1f).testTag("setup-download"),
                        enabled = chosen.isNotEmpty(),
                        size = ButtonSize.Large,
                    )
                }
                else -> {
                    Text(
                        when {
                            running -> tr("Downloads go on in the background if you close this.")
                            failed -> tr("Some downloads failed.")
                            else -> tr("Everything is downloaded.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (failed) QuietButton(onClick = onRetry) { Text(tr("Try again")) }
                    PrimaryButton(onClick = onClosed) { Text(if (running) tr("Close") else tr("Done")) }
                }
            }
        }
    }
}

/** The dialog's content: the downloads to choose, then their progress. */
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
    Column(Modifier.testTag("language-setup").padding(start = AppSpacing.xxxl, end = AppSpacing.xl, top = AppSpacing.xl, bottom = AppSpacing.xxl)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = AppSpacing.m, end = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                val name = items?.firstOrNull()?.languageName
                Text(
                    if (name != null) tr("Get ready to learn {0}", languageInSentence(name, LanguageCase.NOMINATIVE)) else tr("Getting ready…"),
                    style = AppText.headline,
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
            AppIconButton(
                Icons.Default.Close,
                contentDescription = tr("Close"),
                onClick = onClosed,
                modifier = Modifier.testTag("setup-close"),
                style = IconButtonStyle.Tonal,
                tint = colors.onSurface,
            )
        }
        Spacer(Modifier.height(AppSpacing.xl))
        if (items == null) {
            Box(Modifier.fillMaxWidth().padding(end = AppSpacing.s).heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
            return@Column
        }
        // The scrollbar (desktop only) runs in the gutter the cards leave at the end.
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(end = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            setupGroups(items).forEach { group ->
                val item = group.first()
                if (item.kind == SetupKind.VOICE && item.engine != null) {
                    VoicesCard(group, state.selected, state.started, progress, onToggle)
                } else {
                    SetupCard(item, checked = item.id in state.selected, started = state.started, progress = progress[item.id], onToggle = { onToggle(item.id) })
                }
            }
            // Without courses a new learner needs another way in: the texts the app can find, or one of their own.
            if (items.any { it.kind == SetupKind.COURSES && it.status == SetupStatus.UNAVAILABLE }) InfoBanner(tr("No ready-made courses yet. Read Wikipedia articles under News, find free books, or add a text of your own."))
        }
        Spacer(Modifier.height(AppSpacing.xl))
        val chosen = items.filter { it.id in state.selected }
        val running = state.started && chosen.any { progress[it.id].let { p -> p == null || p is SetupState.Waiting || p is SetupState.Running } }
        val failed = state.started && !running && chosen.any { progress[it.id] is SetupState.Failed }
        if (items.none { it.status == SetupStatus.MISSING }) {
            Row(Modifier.fillMaxWidth().padding(end = AppSpacing.s), horizontalArrangement = Arrangement.End) {
                PrimaryButton(onClick = onClosed, modifier = Modifier.testTag("setup-done")) { Text(tr("Done")) }
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(end = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
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
                QuietButton(onClick = onClosed) { Text(tr("Set up later")) }
                PrimaryButton(onClick = onStart, enabled = chosen.isNotEmpty(), modifier = Modifier.testTag("setup-download")) {
                    Text(tr("Download selected"))
                }
            } else {
                if (failed) QuietButton(onClick = onRetry) { Text(tr("Try again")) }
                PrimaryButton(onClick = onClosed) { Text(if (running) tr("Close") else tr("Done")) }
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

/** One download to tick; [compact] is the phone's tighter card without the file lines. */
@Composable
private fun SetupCard(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.large
    val missing = item.status == SetupStatus.MISSING
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (item.status == SetupStatus.UNAVAILABLE) AppAlpha.muted else 1f)
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = AppAlpha.line))
            .border(AppBorders.hairline(), shape)
            .clickable(enabled = !started && missing, onClick = onToggle)
            .padding(horizontal = if (compact) AppSpacing.s else AppSpacing.l, vertical = if (compact) AppSpacing.m else AppSpacing.l)
            .testTag("setup-${item.kind.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.l),
    ) {
        StatusMark(item, checked, started, progress, onToggle)
        IconTile(
            when (item.kind) {
                SetupKind.COURSES -> AppIcons.MenuBook
                SetupKind.DICTIONARY -> AppIcons.Book
                SetupKind.VOICE -> AppIcons.VolumeUp
                SetupKind.TRANSLATION -> AppIcons.Translate
            },
            size = if (compact) 48 else 56,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(
                    when (item.kind) {
                        SetupKind.COURSES -> tr("{0} course pack", tr(item.languageName))
                        SetupKind.DICTIONARY -> tr("Offline dictionary")
                        SetupKind.VOICE -> tr("{0} voice", tr(item.languageName))
                        SetupKind.TRANSLATION -> tr("Offline translation")
                    },
                    style = AppText.itemTitle,
                    modifier = Modifier.weight(1f),
                )
                when (item.status) {
                    SetupStatus.INSTALLED -> Text(tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok, modifier = Modifier.padding(top = AppSpacing.xxs))
                    SetupStatus.UNAVAILABLE -> Unit
                    SetupStatus.MISSING -> item.sizeBytes?.let {
                        Text(sizeText(it, item.sizeEstimated), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = AppSpacing.xxs))
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
                Text(localizedNames(item.name), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = AppAlpha.strong))
            }
            if (item.files.size > 1 && !compact) {
                Column(Modifier.padding(top = AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
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
            if (progress is SetupState.Failed) Text(downloadFailure(progress.message), style = MaterialTheme.typography.bodySmall, color = colors.error)
            // A download waiting its turn says so, rather than showing an empty box like one not chosen.
            if (started && checked && (progress == null || progress is SetupState.Waiting)) Text(tr("Queued"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** The tick box before a download, or where it stands once on the device or started. */
@Composable
private fun StatusMark(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(MARK_SLOT), contentAlignment = Alignment.Center) {
        when {
            item.status == SetupStatus.INSTALLED -> Icon(Icons.Default.CheckCircle, contentDescription = tr("On this device"), tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconLarge))
            item.status == SetupStatus.UNAVAILABLE -> Icon(AppIcons.RemoveCircle, contentDescription = tr("Not available"), tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconLarge))
            !started || !checked -> AppCheckbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !started)
            progress is SetupState.Done -> Icon(Icons.Default.CheckCircle, contentDescription = tr("Downloaded"), tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconLarge))
            progress is SetupState.Failed -> Icon(Icons.Default.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.size(AppSizes.iconLarge))
            progress is SetupState.Running -> AppSpinner(size = SpinnerSize.Medium)
            else -> Icon(AppIcons.History, contentDescription = tr("Queued"), tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconMedium))
        }
    }
}

/**
 * The voices one engine has for the language, each to tick on its own: "Greek voice · Piper", what
 * its voices share (the engine's runtime, Kokoro's model), downloaded once with the first voice, then a row per voice.
 */
@Composable
private fun VoicesCard(voices: List<SetupItem>, selected: Set<String>, started: Boolean, progress: Map<String, SetupState>, onToggle: (String) -> Unit, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.large
    val first = voices.first()
    val engine = first.engine.orEmpty()
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = AppAlpha.line))
            .border(AppBorders.hairline(), shape)
            .padding(horizontal = if (compact) AppSpacing.s else AppSpacing.l, vertical = if (compact) AppSpacing.m else AppSpacing.l)
            .testTag("setup-VOICE-$engine"),
        verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.xs else AppSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.l)) {
            val missing = voices.filter { it.status == SetupStatus.MISSING }
            Box(Modifier.size(MARK_SLOT), contentAlignment = Alignment.Center) {
                if (missing.isNotEmpty() && !started) {
                    val chosen = missing.count { it.id in selected }
                    val all = chosen == missing.size
                    AppTriStateCheckbox(
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
            IconTile(AppIcons.VolumeUp, size = if (compact) 48 else 56)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(
                        "${tr("{0} voice", tr(first.languageName))} \u00b7 $engine",
                        style = AppText.itemTitle,
                        modifier = Modifier.weight(1f),
                    )
                    // What all its voices take together, the shared model and runtime once.
                    totalSize(missing)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = AppSpacing.xxs).testTag("setup-total-$engine"))
                    }
                }
                Text(tr("Hear books and words read aloud"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (!compact) voices.flatMap { it.files }.filter { it.kind == SetupFileKind.ENGINE }.distinct().forEach { FileLine(it) }
                if (voices.any { it.switchesEngine }) {
                    Text(tr("Becomes your speech engine"), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }
        }
        if (compact) AppDivider(Modifier.padding(top = AppSpacing.xs))
        // A model the voices share is a download of its own while it is not on the device yet.
        voices.flatMap { it.files }.firstOrNull { it.kind == SetupFileKind.VOICE_MODEL }?.let { model ->
            val chosen = voices.filter { it.id in selected }
            // The first chosen voice is the one that fetches the model.
            ModelRow(engine, model, needed = chosen.isNotEmpty(), started, chosen.firstOrNull()?.let { progress[it.id] })
            if (compact) AppDivider()
        }
        voices.forEachIndexed { i, voice ->
            if (compact && i > 0) AppDivider()
            VoiceRow(voice, voice.id in selected, started, progress[voice.id], onToggle = { onToggle(voice.id) }, compact = compact)
        }
    }
}

/** The model an engine's voices need, ticked whenever one of them is: it cannot be chosen apart from them. */
@Composable
private fun ModelRow(engine: String, model: SetupFile, needed: Boolean, started: Boolean, progress: SetupState?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs).testTag("setup-model-$engine"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        if (started) {
            StatusMark(SetupItem("", SetupKind.VOICE, "", "", emptyList(), recommended = false), needed, started = true, progress = progress, onToggle = {})
        } else {
            Box(Modifier.size(MARK_SLOT), contentAlignment = Alignment.Center) { AppCheckbox(checked = needed, onCheckedChange = null) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(tr("{0} model", engine), style = MaterialTheme.typography.bodyLarge)
            Text(tr("Needed by every voice, downloaded once"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        model.sizeBytes?.let { Text(sizeText(it, model.estimated), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    }
}

/** One voice of an engine: its name and, when it is a download of its own, its size. */
@Composable
private fun VoiceRow(item: SetupItem, checked: Boolean, started: Boolean, progress: SetupState?, onToggle: () -> Unit, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val missing = item.status == SetupStatus.MISSING
    Row(
        Modifier.fillMaxWidth()
            .clip(AppShapes.panel)
            .clickable(enabled = !started && missing, onClick = onToggle)
            .padding(vertical = AppSpacing.xs)
            .testTag("setup-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.l),
    ) {
        StatusMark(item, checked, started, progress, onToggle)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(localizedVoiceName(item.name.substringAfter(" \u00b7 ")), style = MaterialTheme.typography.bodyLarge)
            if (progress is SetupState.Failed) Text(downloadFailure(progress.message), style = MaterialTheme.typography.bodySmall, color = colors.error)
            // A download waiting its turn says so, rather than showing an empty box like one not chosen.
            if (started && checked && (progress == null || progress is SetupState.Waiting)) Text(tr("Queued"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        StatusDot(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.muted), size = 4.dp)
        Text("$name · $size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val LOADING_HEIGHT = 120.dp

/** The square the tick box or a download's state stands in. */
private val MARK_SLOT = 40.dp

/** "Kokoro model with 34 voices", as the engines name a model of several voices. */
private val MODEL_TITLE = Regex("""(.+) model with (\d+) voices""")

/** Why a download failed, in words: a server's status code is no help to a reader; the footer offers Try again. */
private fun downloadFailure(message: String): String {
    val code = Regex("""HTTP (\d{3})""").find(message)?.groupValues?.get(1)?.toIntOrNull()
    return when {
        code == null -> tr("Download failed: {0}", message)
        code >= 500 -> tr("The download server is busy. Try again in a moment.")
        code == 404 -> tr("The file is no longer on the download server.")
        else -> tr("Download failed: {0}", message)
    }
}
