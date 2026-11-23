package com.idiomiko.feature.settings

import com.idiomiko.core.ui.theme.AppAlpha
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.practice.RecognitionModel
import com.idiomiko.core.domain.practice.RecognitionQuality
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trProgress
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * Speech recognition for speaking practice: which of the models on the device, of medium or high
 * quality, hears the answers, and each model to download or remove in place.
 */
@Composable
internal fun RecognitionCard(viewModel: SpeechViewModel, modifier: Modifier = Modifier, dense: Boolean = false) {
    if (viewModel.recognition == null) return
    val models by viewModel.recognitionModels.collectAsStateWithLifecycle()
    val settings by viewModel.state.collectAsStateWithLifecycle()
    // Looked up again with them, so a row's download stands for the model as it is now.
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val states by viewModel.downloadStates.collectAsStateWithLifecycle()
    val detail by viewModel.recognition.progress.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<Confirmation?>(null) }
    LaunchedEffect(viewModel) { viewModel.checkRecognition() }
    // The model chosen hears where it is downloaded; else the other one does.
    val installed = models.filter { it.installed }
    val hearing = installed.firstOrNull { it.quality.name == settings.speechRecognitionQuality } ?: installed.firstOrNull()
    ContentCard(
        tr("Speech recognition"),
        tr("Hears your answers when you practise speaking. It runs on this computer, with no network once downloaded."),
        icon = AppIcons.Mic,
        dense = dense,
        modifier = modifier.testTag("speech-recognition"),
        titleExtra = {
            // A phone's title has no room for it, and each model says whether it is on the device.
            if (models.isNotEmpty() && !LocalWindowWidth.current.isCompact) {
                if (hearing != null) Tag(tr("Installed"), StatusTints.ok) else Tag(tr("Not installed"), StatusTints.warning)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            models.forEach { model ->
                val item = downloads?.let { viewModel.recognitionDownload(model.quality) }
                val state = item?.let { rowState(it, states[it.id], label = detail?.let(::trProgress)) }
                ModelRow(
                    model,
                    hearing = model == hearing,
                    state = state,
                    onChoose = { viewModel.chooseRecognition(model.quality) },
                    onDownload = item?.let { { viewModel.install(it) } },
                    onRemove = item?.takeIf(viewModel::canRemove)?.let {
                        { confirming = Confirmation(tr("Remove this speech recognition model?"), tr("You can download it again at any time.")) { viewModel.remove(it) } }
                    },
                )
            }
        }
    }
    confirming?.let { c -> RemovePackDialog(c.question, c.consequence, onRemove = c.remove, onDismiss = { confirming = null }) }
}

/**
 * One model: picked to hear when it is on the device, with what it is, whether it is there, and
 * the button to download or remove it; [state] is null while its download is looked up.
 */
@Composable
private fun ModelRow(model: RecognitionModel, hearing: Boolean, state: RowState?, onChoose: () -> Unit, onDownload: (() -> Unit)?, onRemove: (() -> Unit)?) {
    val (title, about) = when (model.quality) {
        RecognitionQuality.MEDIUM -> tr("Medium quality") to tr("Whisper small: quick, well enough for a phrase")
        RecognitionQuality.HIGH -> tr("High quality") to tr("Whisper large turbo: better at an accent, a few seconds a phrase")
    }
    val tag = "recognition-${model.quality.name.lowercase()}"
    val compact = LocalWindowWidth.current.isCompact
    val side: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            when {
                model.installed -> Text(tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
                state == null -> Text(tr("Not downloaded"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state?.let { DownloadAction(it, tag, onDownload, onRemove) }
        }
    }
    // The model in use is highlighted as the voices' current one is; another on the device is chosen by its row.
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.control)
            .border(if (hearing) AppBorders.chosen() else AppBorders.hairline(), AppShapes.control)
            .background(if (hearing) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .clickable(enabled = model.installed, onClick = onChoose)
            .semantics { selected = hearing }
            .padding(AppSpacing.m).testTag("recognition-model-${model.quality.name}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(title, style = AppText.itemTitle)
                    if (hearing) CurrentTag(tag)
                }
                Text(
                    listOf(about, formatSize(model.downloadSize)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state?.takeIf { it is RowState.Working || it is RowState.Failed }?.let { DownloadProgress(it) }
            }
            if (!compact) {
                Spacer(Modifier.width(AppSpacing.m))
                side()
            }
        }
        if (compact) {
            Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs), horizontalArrangement = Arrangement.End) { side() }
        }
    }
}
