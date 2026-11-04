package com.tayra.languages.feature.videos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.data.media.SubtitleFiles
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.media.sidecarSubtitleFiles
import com.tayra.languages.core.ui.media.videoAddress
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AddVideoScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, onAdded: (Long) -> Unit, viewModel: AddVideoViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is AddVideoEvent.Added -> onAdded(event.bookId)
        }
    }
    val scope = rememberCoroutineScope()
    val mediaPicker = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.mediaExtensions)) { file ->
        if (file != null) scope.launch {
            val address = file.videoAddress()
            val sidecars = sidecarSubtitleFiles(address, SubtitleFiles.subtitleExtensions).map { it.name to it.readBytes() }
            viewModel.pickMedia(file.name, address, sidecars)
        }
    }
    val subtitlePicker = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.subtitleExtensions)) { file ->
        if (file != null) scope.launch { viewModel.pickSubtitles(file.name, file.readBytes()) }
    }
    val translationPicker = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.subtitleExtensions)) { file ->
        if (file != null) scope.launch { viewModel.pickTranslation(file.name, file.readBytes()) }
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Add video"), onNavigate = onNavigate, onBack = onBack, section = NavSection.VIDEOS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        AddVideoContent(
            state,
            AddVideoActions(
                onPickMedia = { mediaPicker.launch() },
                onPickSubtitles = { subtitlePicker.launch() },
                onPickTranslation = { translationPicker.launch() },
                onSidecar = viewModel::pickSidecar,
                onClearTranslation = viewModel::clearTranslation,
                onTitle = viewModel::setTitle,
                onAdd = viewModel::add,
            ),
            Modifier.padding(padding),
        )
    }
}

internal class AddVideoActions(
    val onPickMedia: () -> Unit,
    val onPickSubtitles: () -> Unit,
    val onPickTranslation: () -> Unit,
    val onSidecar: (name: String, asTranslation: Boolean) -> Unit,
    val onClearTranslation: () -> Unit,
    val onTitle: (String) -> Unit,
    val onAdd: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddVideoContent(state: AddVideoUiState, actions: AddVideoActions, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val colors = MaterialTheme.colorScheme
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl).widthIn(max = FORM_WIDTH),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(tr("Add video"), tr("A video or audio file with its subtitles."))
        AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(tr("Video file"), style = AppText.itemTitle)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SecondaryButton(tr("Choose file"), onClick = actions.onPickMedia, icon = AppIcons.Movie, modifier = Modifier.testTag("pick-media"))
                Text(state.mediaName ?: tr("No file chosen"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false))
            }
        }
        AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(tr("Subtitles"), style = AppText.itemTitle)
            Text(tr("SRT, WebVTT or ASS, in the language of the video."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SecondaryButton(tr("Choose subtitles"), onClick = actions.onPickSubtitles, icon = AppIcons.Subtitles, modifier = Modifier.testTag("pick-subtitles"))
                state.subtitles?.let { Text(describe(it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false).testTag("subtitles-picked")) }
            }
            if (state.sidecars.isNotEmpty()) {
                Text(tr("Found beside the video"), style = AppText.label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    state.sidecars.forEach { name ->
                        SelectableChip(name, selected = state.subtitles?.name == name, onClick = { actions.onSidecar(name, false) })
                    }
                }
            }
        }
        AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(tr("Translation (optional)"), style = AppText.itemTitle)
            Text(tr("Subtitles in your language, shown under each line. Without them, lines are translated as you watch."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SecondaryButton(tr("Choose translation"), onClick = actions.onPickTranslation, icon = AppIcons.Translate, modifier = Modifier.testTag("pick-translation"))
                state.translation?.let {
                    Text(describe(it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false))
                    QuietButton(tr("Remove"), onClick = actions.onClearTranslation, modifier = Modifier.testTag("remove-translation"))
                }
            }
            if (state.sidecars.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    state.sidecars.filter { it != state.subtitles?.name }.forEach { name ->
                        SelectableChip(name, selected = state.translation?.name == name, onClick = { actions.onSidecar(name, true) })
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(tr("Title"), style = AppText.label)
            AppTextField(state.title, actions.onTitle, Modifier.fillMaxWidth().testTag("video-title"), singleLine = true)
        }
        state.error?.let { Notice(tr(it), tint = colors.error) }
        PrimaryButton(tr("Add video"), onClick = actions.onAdd, enabled = state.canAdd, loading = state.saving, icon = AppIcons.Movie, modifier = Modifier.testTag("add-video-confirm"))
    }
}

@Composable
private fun describe(picked: PickedSubtitles): String =
    "${picked.name} · ${trPlural(picked.cues.size, "{0} line", "{0} lines")} · ${formatClock(picked.lastMs)}"

private val FORM_WIDTH = 720.dp
