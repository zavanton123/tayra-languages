package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing

/** Speech recognition for speaking practice, installed into the app folder: whether it is, and the way to install it. */
@Composable
internal fun RecognitionCard(viewModel: SpeechViewModel) {
    val setup = viewModel.recognition ?: return
    val ready by viewModel.recognitionReady.collectAsStateWithLifecycle()
    val busy by viewModel.recognitionBusy.collectAsStateWithLifecycle()
    val error by viewModel.recognitionError.collectAsStateWithLifecycle()
    val size by viewModel.recognitionSize.collectAsStateWithLifecycle()
    val progress by setup.progress.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.checkRecognition() }
    ContentCard(
        tr("Speech recognition"),
        tr("Hears your answers when you practise speaking. It runs on this computer, with no network once installed."),
        icon = AppIcons.Mic,
        modifier = Modifier.testTag("speech-recognition"),
        titleExtra = {
            when (ready) {
                true -> Tag(tr("Installed"), StatusTints.ok)
                false -> Tag(tr("Not installed"), StatusTints.warning)
                null -> {}
            }
        },
    ) {
        if (ready == false) {
            Text(
                tr("Whisper's large turbo model and what it runs on, about {0} to download.", formatSize(size)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(AppSpacing.m))
            PrimaryButton(tr("Install"), onClick = viewModel::installRecognition, loading = busy, icon = AppIcons.Download, modifier = Modifier.testTag("install-recognition"))
        }
        if (busy) {
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
