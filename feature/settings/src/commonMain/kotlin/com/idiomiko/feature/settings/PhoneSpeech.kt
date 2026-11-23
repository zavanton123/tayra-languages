package com.idiomiko.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.idiomiko.core.ui.components.ScrollColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.language.LanguageOption
import com.idiomiko.core.domain.service.LocalSpeechEngine
import com.idiomiko.core.domain.service.SpeechEngine
import com.idiomiko.core.domain.service.SpeechPackage
import com.idiomiko.core.domain.service.SpeechVoice
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppProgressBar
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.Notice
import com.idiomiko.core.ui.components.PickerSheet
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SelectButton
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing

/**
 * The Speech page on a phone: a banner saying where speech stands, the engine picked from a sheet
 * with its status under it, playback with the voice and the speed, and the voices and speech
 * recognition models of the language being learned, to download or remove.
 */
@Composable
internal fun PhoneSpeechScreen(
    settings: UserSettings,
    viewModel: SpeechViewModel,
    engine: LocalSpeechEngine?,
    learning: LanguageOption?,
    voices: Map<String, List<SpeechVoice>>,
    packages: List<SpeechPackage>,
    checking: Boolean,
    ready: Boolean,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var engineSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Speech"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Choose how words and passages are spoken."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                SpeechStatusPill(engine, checking, ready, learning, voices)
            }
            ContentCard(tr("Speech engine"), subtitle = null, icon = AppIcons.Memory, dense = true) {
                val chosen = settings.speechEngine.takeIf { it in viewModel.engines } ?: SpeechEngine.SYSTEM
                SelectButton(engine?.displayName ?: tr(chosen.label), onClick = { engineSheet = true }, modifier = Modifier.fillMaxWidth().testTag("phone-speech-engine"))
                Spacer(Modifier.height(AppSpacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    when (chosen) {
                        SpeechEngine.SYSTEM -> Tag(tr("Built in"), colors.primary)
                        SpeechEngine.PIPER -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("Many languages"), colors.primary) }
                        SpeechEngine.KOKORO -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("High quality"), colors.primary) }
                    }
                }
                if (engine != null) {
                    Spacer(Modifier.height(AppSpacing.m))
                    PhoneEngineStatus(viewModel, engine, voiceMissing = learning != null && voices[learning.code].isNullOrEmpty())
                }
            }
            ContentCard(tr("Voice & playback"), subtitle = null, icon = AppIcons.VolumeUp, dense = true, modifier = Modifier.testTag("speech-voices")) {
                if (engine?.supportsSpeed == true) {
                    SliderSetting(
                        tr("Speech speed"),
                        "${kotlin.math.round(settings.speechSpeed * 100).toInt()}%",
                        settings.speechSpeed,
                        0.5f..1.5f,
                        onChange = { v -> viewModel.update { it.copy(speechSpeed = v) } },
                        step = 0.05f,
                        sliderModifier = Modifier.semantics { contentDescription = tr("speech speed") }.testTag("phone-speech-speed"),
                    )
                } else {
                    // Said rather than left out, so a missing slider is not a mystery.
                    Text(tr("The system voices set their own speed; an offline engine lets you change it."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppDivider(Modifier.padding(vertical = AppSpacing.l))
                VoiceList(settings, viewModel, engine, learning, voices)
            }
            if (viewModel.recognition != null) RecognitionCard(viewModel, dense = true)
        }
    }
    if (engineSheet) {
        PickerSheet(
            tr("Speech engine"), viewModel.engines, selected = settings.speechEngine.takeIf { it in viewModel.engines } ?: SpeechEngine.SYSTEM,
            label = { tr(it.label) },
            onDismiss = { engineSheet = false },
            onPick = { e -> engineSheet = false; viewModel.update { it.copy(speechEngine = e) } },
        )
    }
}

/** Whether the engine works; the install button shows only while it does not. */
@Composable
private fun PhoneEngineStatus(viewModel: SpeechViewModel, engine: LocalSpeechEngine, voiceMissing: Boolean) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val broken by viewModel.broken.collectAsStateWithLifecycle()
    // The way to a voice is in the notice that says one is missing: it downloads one in place.
    val working = ready && !failed
    // Amber while the language learned has no voice, as the status pill above says.
    val tint = when {
        busy -> MaterialTheme.colorScheme.outline
        working && !voiceMissing -> StatusTints.ok
        else -> StatusTints.warning
    }
    Notice(
        title = when {
            busy -> tr("Checking…")
            working && voiceMissing -> tr("Installed; no voice for this language yet")
            working -> tr("Installed and working")
            ready || broken -> tr("Something went wrong")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) },
        tint = tint,
        // No tick while the language still has no voice: the box is amber, and says what is missing.
        icon = if (working && !voiceMissing) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = when {
            engine.hasRuntimeSetup && !ready && !broken && !busy -> { { PrimaryButton(tr("Install"), onClick = viewModel::install) } }
            working && voiceMissing && viewModel.offersVoice(downloads, downloadStates) -> { { DownloadVoiceButton(viewModel) } }
            else -> null
        },
    ) {
        progress?.let {
            Text(tr(it), style = MaterialTheme.typography.bodySmall)
            AppProgressBar(null)
        }
        if (broken && !busy) ReinstallEngine(viewModel, engine)
    }
}
