package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.components.ScrollColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.components.Reveal

/**
 * The Speech page on a phone: a banner saying where speech stands, the engine picked from a sheet
 * with its status under it, playback with the voice and the speed, and the voices to download for
 * the language being learned, with a search for the rest.
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
            ContentCard(tr("Playback"), subtitle = null, icon = AppIcons.VolumeUp, dense = true) {
                val options = learning?.let { voices[it.code] }.orEmpty()
                if (engine != null && learning != null && options.isNotEmpty()) {
                    val current = settings.speechVoices["${engine.engine.name}:${learning.code}"]
                    var voiceSheet by remember { mutableStateOf(false) }
                    val selected = options.firstOrNull { it.id == current } ?: options.first()
                    SelectButton(
                        localizedVoiceName(selected.name),
                        onClick = { voiceSheet = true },
                        modifier = Modifier.fillMaxWidth().testTag("phone-speech-voice"),
                        label = tr("{0} voice", tr(learning.name)),
                    )
                    if (voiceSheet) {
                        PickerSheet(tr("{0} voice", tr(learning.name)), options, selected, label = { localizedVoiceName(it.name) }, onDismiss = { voiceSheet = false }, onPick = { voice ->
                            voiceSheet = false
                            viewModel.chooseVoice(engine.engine, learning.code, voice.id)
                        })
                    }
                    Spacer(Modifier.height(AppSpacing.m))
                }
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
                    Spacer(Modifier.height(AppSpacing.s))
                } else {
                    // Said rather than left out, so a missing slider is not a mystery.
                    Text(tr("The system voices set their own speed; an offline engine lets you change it."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(AppSpacing.s))
                }
                PlaySample(viewModel, learning)
            }
            if (engine != null && packages.isNotEmpty()) {
                if (packages.all { it.languageCode == null }) packages.forEach { PhoneModelCard(viewModel, engine, it) }
                else PhoneVoices(viewModel, packages, learning)
            }
            if (engine != null && engine.hasRuntimeSetup) AdvancedRow(viewModel, engine)
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
            ready -> tr("Something went wrong")
            else -> tr("Not installed")
        },
        message = status?.let { tr(it) },
        tint = tint,
        // No tick while the language still has no voice: the box is amber, and says what is missing.
        icon = if (working && !voiceMissing) Icons.Default.CheckCircle else Icons.Default.Warning,
        busy = busy,
        action = if (engine.hasRuntimeSetup && !ready && !busy) { { PrimaryButton(tr("Install"), onClick = viewModel::install) } } else null,
    ) {
        progress?.let {
            Text(tr(it), style = MaterialTheme.typography.bodySmall)
            AppProgressBar(null)
        }
    }
}

/** Plays the language's sample sentence with the chosen engine and voice. */
@Composable
private fun PlaySample(viewModel: SpeechViewModel, language: LanguageOption?) {
    val speaker = rememberSpeaker(viewModel.localSpeech, viewModel.settingsRepository)
    val working by speaker.working.collectAsStateWithLifecycle()
    val playing by speaker.playing.collectAsStateWithLifecycle()
    val text = language?.let { SAMPLES[it.code] } ?: SAMPLES.getValue("en")
    val active = playing != null && playing == text
    SecondaryButton(
        when {
            active && working -> tr("Preparing…")
            active -> tr("Stop")
            else -> tr("Play sample")
        },
        onClick = { speaker.toggle(text, language?.code ?: "en") },
        modifier = Modifier.fillMaxWidth(),
        icon = if (active && !working) AppIcons.Stop else AppIcons.VolumeUp,
    )
}

/** One download holding the model and every voice, as Kokoro has. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneModelCard(viewModel: SpeechViewModel, engine: LocalSpeechEngine, pkg: SpeechPackage) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val busy = pkg.id in packageBusy
    val voiceCount = Regex("""(\d+) voices""").find(pkg.title)?.groupValues?.get(1)
    ContentCard(tr("Voice model"), subtitle = null, icon = AppIcons.Download, dense = true, headerExtra = { if (pkg.installed) Tag(tr("Downloaded"), StatusTints.ok) else Tag(tr("Not downloaded"), StatusTints.warning) }) {
        Text(modelTitle(engine, pkg), style = AppText.itemTitle)
        Text(
            listOfNotNull(voiceCount?.let { trPlural(it.toInt(), "{0} voice", "{0} voices") }, pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(AppSpacing.m))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            pkg.group.split(", ").filter { it.isNotBlank() }.forEach { name -> CodeTile(LanguageCodes.codeFor(name) ?: name.take(2), size = 34) }
        }
        if (busy) AppProgressBar(null, Modifier.padding(top = AppSpacing.m))
        Spacer(Modifier.height(AppSpacing.l))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PackageAction(pkg, busy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) })
        }
    }
}

/** One download per voice, as Piper has: the voices on the device, then those to download for the language, with a search for the rest. */
@Composable
private fun PhoneVoices(viewModel: SpeechViewModel, packages: List<SpeechPackage>, learning: LanguageOption?) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val matches = { pkg: SpeechPackage -> query.isBlank() || listOf(pkg.title, pkg.group, tr(pkg.group)).any { it.contains(query.trim(), true) } }
    val installed = packages.filter { it.installed }.filter(matches).sortedWith(compareBy({ it.languageCode != learning?.code }, { it.title }))
    // Voices for the language being learned come first; the rest only when searched for.
    val available = packages.filter { !it.installed && (query.isNotBlank() || it.languageCode == learning?.code) }.filter(matches).sortedBy { it.title }
    val languageName = learning?.let { tr(it.name) }
    SearchField(query, { query = it }, tr("Search voices"), Modifier.fillMaxWidth())
    if (installed.isNotEmpty()) {
        Text(tr("Downloaded voices"), style = AppText.sectionTitle)
        VoiceList(installed, packageBusy, viewModel)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                query.isNotBlank() -> tr("All voices")
                languageName != null -> tr("Available {0} voices", languageName)
                else -> tr("Voices")
            },
            Modifier.weight(1f),
            style = AppText.sectionTitle,
        )
        Tag(tr("{0} downloaded", packages.count { it.installed }), colors.primary)
    }
    if (available.isEmpty()) {
        Text(
            when {
                query.isNotBlank() -> tr("No voices match “{0}”.", query.trim())
                learning == null -> tr("Search to find voices to download.")
                else -> tr("Every {0} voice is downloaded. Search to find voices for other languages.", tr(learning.name))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    } else {
        VoiceList(available, packageBusy, viewModel)
    }
}

@Composable
private fun VoiceList(voices: List<SpeechPackage>, packageBusy: Set<String>, viewModel: SpeechViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.panel)) {
        voices.forEachIndexed { i, pkg ->
            if (i > 0) AppDivider()
            Row(Modifier.fillMaxWidth().padding(start = AppSpacing.m, end = AppSpacing.xs, top = AppSpacing.s, bottom = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                CodeTile(pkg.languageCode ?: pkg.group.take(2), size = 40)
                Spacer(Modifier.width(AppSpacing.m))
                Column(Modifier.weight(1f)) {
                    Text(pkg.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(tr(pkg.group), pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (pkg.id in packageBusy) AppProgressBar(null, Modifier.padding(top = AppSpacing.xs))
                }
                Spacer(Modifier.width(AppSpacing.s))
                PackageAction(pkg, busy = pkg.id in packageBusy, onInstall = { viewModel.installPackage(pkg) }, onRemove = { viewModel.removePackage(pkg) })
            }
        }
    }
}

/** The runtime's reinstall, folded away under a row. */
@Composable
private fun AdvancedRow(viewModel: SpeechViewModel, engine: LocalSpeechEngine) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.card)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(AppSpacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(AppIcons.Tune, size = 44)
            Text(tr("Advanced engine settings"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        Reveal(open) {
            Column(Modifier.padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(
                    tr("{0} runs in the app's own Python, kept in the app folder. Reinstalling downloads it again, which repairs a broken installation; downloaded voices are kept.", engine.displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                SecondaryButton(tr("Reinstall {0}", engine.displayName), onClick = viewModel::install, enabled = !busy)
            }
        }
    }
}
