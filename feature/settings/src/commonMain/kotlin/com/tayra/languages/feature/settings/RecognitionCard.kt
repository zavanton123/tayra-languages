package com.tayra.languages.feature.settings

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.practice.RecognitionModel
import com.tayra.languages.core.domain.practice.RecognitionQuality
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * Speech recognition for speaking practice, installed into the app folder: a model of medium or high
 * quality to download, remove, and choose between when both are there.
 */
@Composable
internal fun RecognitionCard(viewModel: SpeechViewModel) {
    val setup = viewModel.recognition ?: return
    val models by viewModel.recognitionModels.collectAsStateWithLifecycle()
    val busy by viewModel.recognitionBusy.collectAsStateWithLifecycle()
    val error by viewModel.recognitionError.collectAsStateWithLifecycle()
    val progress by setup.progress.collectAsStateWithLifecycle()
    val settings by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.checkRecognition() }
    // The model chosen hears where it is downloaded; else the other one does.
    val installed = models.filter { it.installed }
    val hearing = installed.firstOrNull { it.quality.name == settings.speechRecognitionQuality } ?: installed.firstOrNull()
    ContentCard(
        tr("Speech recognition"),
        tr("Hears your answers when you practise speaking. It runs on this computer, with no network once downloaded."),
        icon = AppIcons.Mic,
        modifier = Modifier.testTag("speech-recognition"),
        titleExtra = {
            if (models.isNotEmpty()) {
                if (hearing != null) Tag(tr("Installed"), StatusTints.ok) else Tag(tr("Not installed"), StatusTints.warning)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            models.forEach { model ->
                ModelRow(
                    model,
                    hearing = model == hearing,
                    busy = busy == model.quality,
                    onChoose = { viewModel.chooseRecognition(model.quality) },
                    onInstall = { viewModel.installRecognition(model.quality) },
                    onRemove = { viewModel.removeRecognition(model.quality) },
                )
            }
        }
        if (busy != null) {
            Spacer(Modifier.height(AppSpacing.m))
            Text(progress?.let { tr(it) } ?: tr("Installing speech recognition…"), style = MaterialTheme.typography.bodySmall)
            AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
        }
        error?.let {
            Spacer(Modifier.height(AppSpacing.m))
            Text(tr("Could not install: {0}", it), style = MaterialTheme.typography.bodySmall, color = StatusTints.warning, modifier = Modifier.testTag("recognition-error"))
        }
    }
}

/** One model: picked to hear when downloaded, with what it is and the way to download or remove it. */
@Composable
private fun ModelRow(model: RecognitionModel, hearing: Boolean, busy: Boolean, onChoose: () -> Unit, onInstall: () -> Unit, onRemove: () -> Unit) {
    val (title, about) = when (model.quality) {
        RecognitionQuality.MEDIUM -> tr("Medium quality") to tr("Whisper small: quick, well enough for a phrase")
        RecognitionQuality.HIGH -> tr("High quality") to tr("Whisper large turbo: better at an accent, a few seconds a phrase")
    }
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control)
            .clickable(enabled = model.installed, onClick = onChoose).padding(AppSpacing.m).testTag("recognition-model-${model.quality.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = hearing, onClick = onChoose, enabled = model.installed)
        Spacer(Modifier.width(AppSpacing.s))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.itemTitle)
            Text(
                listOf(about, formatSize(model.downloadSize)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(AppSpacing.m))
        PackageAction(SpeechPackage(model.quality.name, title, null, "", model.downloadSize, model.installed), busy, onInstall = onInstall, onRemove = onRemove)
    }
}
