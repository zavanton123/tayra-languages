package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.service.SetupItem
import com.idiomiko.core.domain.service.SetupKind
import com.idiomiko.core.domain.service.SetupState
import com.idiomiko.core.domain.service.SetupStatus
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppSpacing

/**
 * The voices the speech engines have for the language being learned, to download or remove in
 * place: one row a voice, or one row for an engine whose voices share a model, downloaded and
 * removed together. The last voice that reads the language offers no Remove.
 */
@Composable
internal fun VoicesCard(viewModel: SpeechViewModel, languageName: String?, modifier: Modifier = Modifier, dense: Boolean = false) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val progress by viewModel.downloadStates.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<Confirmation?>(null) }
    ContentCard(
        tr("Voices"),
        tr("Download other voices to choose from, or remove those you do not use."),
        icon = AppIcons.Download,
        dense = dense,
        modifier = modifier.testTag("speech-voices"),
    ) {
        val voices = downloads?.filter { it.kind == SetupKind.VOICE && it.status != SetupStatus.UNAVAILABLE }
        Column(Modifier.fillMaxWidth().padding(top = AppSpacing.s)) {
            when {
                voices == null -> Row(Modifier.fillMaxWidth().padding(AppSpacing.xl), horizontalArrangement = Arrangement.Center) { AppSpinner(size = SpinnerSize.Medium) }
                voices.isEmpty() -> Text(
                    tr("Not available for {0} yet", languageName ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = AppSpacing.s),
                )
                else -> VoiceRows(voices, progress, viewModel) { confirming = it }
            }
        }
    }
    confirming?.let { c -> RemovePackDialog(c.question, c.consequence, onRemove = c.remove, onDismiss = { confirming = null }) }
}

@Composable
private fun VoiceRows(voices: List<SetupItem>, progress: Map<String, SetupState>, viewModel: SpeechViewModel, confirm: (Confirmation) -> Unit) {
    var first = true
    voices.groupBy { it.engine.orEmpty() }.forEach { (engine, items) ->
        if (items.first().sharedModel) {
            if (!first) AppDivider()
            first = false
            val installed = items.firstOrNull { it.status == SetupStatus.INSTALLED }
            val fetching = items.firstNotNullOfOrNull { progress[it.id]?.takeIf { state -> state !is SetupState.Done } }
            val chosen = installed ?: items.firstOrNull { it.recommended } ?: items.first()
            ToolRow(
                tr("{0} voices", engine),
                items.joinToString(", ") { localizedVoiceName(it.name.substringAfter(" · ")) },
                chosen.copy(status = if (installed != null) SetupStatus.INSTALLED else chosen.status),
                fetching ?: progress[chosen.id],
                "voice-$engine",
                onDownload = { viewModel.install(chosen) },
                onRemove = installed?.takeIf(viewModel::canRemove)?.let { item ->
                    {
                        confirm(Confirmation(
                            tr("Remove the {0} model?", engine),
                            tr("All its voices are removed with it. You can download them again at any time."),
                        ) { viewModel.remove(item) })
                    }
                },
            )
        } else {
            items.forEach { item ->
                if (!first) AppDivider()
                first = false
                val name = localizedVoiceName(item.name.substringAfter(" · "))
                ToolRow(
                    name,
                    engine,
                    item,
                    progress[item.id],
                    "voice-${item.id}",
                    onDownload = { viewModel.install(item) },
                    onRemove = item.takeIf(viewModel::canRemove)?.let {
                        { confirm(Confirmation(tr("Remove the voice {0}?", name), tr("You can download it again at any time.")) { viewModel.remove(item) }) }
                    },
                )
            }
        }
    }
}

/**
 * The notice's way to a voice when the engine has none for the language: it downloads the one the
 * app would fetch on its own, here, and says so while any voice downloads. Nothing when there is
 * no voice to download.
 */
@Composable
internal fun DownloadVoiceButton(viewModel: SpeechViewModel, modifier: Modifier = Modifier) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val progress by viewModel.downloadStates.collectAsStateWithLifecycle()
    val voices = downloads.orEmpty().filter { it.kind == SetupKind.VOICE }
    val fetching = voices.any { progress[it.id] is SetupState.Waiting || progress[it.id] is SetupState.Running }
    val voice = viewModel.voiceToDownload(downloads)
    if (voice == null && !fetching) return
    PrimaryButton(
        if (fetching) tr("Downloading…") else tr("Download voice"),
        onClick = { voice?.let(viewModel::install) },
        icon = AppIcons.Download,
        loading = fetching,
        modifier = modifier.testTag("notice-download-voice"),
    )
}

/** Whether the notice has a voice to offer, so a phone shows its button row only then. */
internal fun SpeechViewModel.offersVoice(downloads: List<SetupItem>?, progress: Map<String, SetupState>): Boolean =
    voiceToDownload(downloads) != null ||
        downloads.orEmpty().any { it.kind == SetupKind.VOICE && (progress[it.id] is SetupState.Waiting || progress[it.id] is SetupState.Running) }
