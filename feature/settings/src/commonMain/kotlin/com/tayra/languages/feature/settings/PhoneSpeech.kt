package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CodeTile
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.RoundSlider
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes

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
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Choose how words and passages are spoken."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            StatusBanner(engine, checking, ready, learning, voices)
            PhoneCard(tr("Speech engine"), AppIcons.Memory) {
                val chosen = settings.speechEngine.takeIf { it in viewModel.engines } ?: SpeechEngine.SYSTEM
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.control).border(1.dp, colors.outline.copy(alpha = 0.5f), AppShapes.control)
                        .clickable { engineSheet = true }.padding(horizontal = 14.dp, vertical = 14.dp).testTag("phone-speech-engine"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(engine?.displayName ?: tr(chosen.label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (chosen) {
                        SpeechEngine.SYSTEM -> Tag(tr("Built in"), colors.primary)
                        SpeechEngine.PIPER -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("Many languages"), colors.primary) }
                        SpeechEngine.KOKORO -> { Tag(tr("Offline"), StatusTints.ok); Tag(tr("High quality"), colors.primary) }
                    }
                }
                if (engine != null) {
                    Spacer(Modifier.height(12.dp))
                    PhoneEngineStatus(viewModel, engine)
                }
            }
            PhoneCard(tr("Playback"), AppIcons.VolumeUp) {
                val options = learning?.let { voices[it.code] }.orEmpty()
                if (engine != null && learning != null && options.isNotEmpty()) {
                    val current = settings.speechVoices["${engine.engine.name}:${learning.code}"]
                    var voiceSheet by remember { mutableStateOf(false) }
                    val selected = options.firstOrNull { it.id == current } ?: options.first()
                    Row(
                        Modifier.fillMaxWidth().clip(AppShapes.control).border(1.dp, colors.outline.copy(alpha = 0.5f), AppShapes.control)
                            .clickable { voiceSheet = true }.padding(horizontal = 14.dp, vertical = 12.dp).testTag("phone-speech-voice"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("{0} voice", tr(learning.name)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            Text(selected.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
                    }
                    if (voiceSheet) {
                        PickerSheet(tr("{0} voice", tr(learning.name)), options, selected, label = { it.name }, onDismiss = { voiceSheet = false }, onPick = { voice ->
                            voiceSheet = false
                            viewModel.chooseVoice(engine.engine, learning.code, voice.id)
                        })
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (engine?.supportsSpeed == true) {
                    val percent = kotlin.math.round(settings.speechSpeed * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Speech speed"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text("$percent%", Modifier.clip(AppShapes.compact).background(colors.surfaceContainerLow).padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                    // Twenty steps of 5% between half and one-and-a-half speed.
                    RoundSlider(
                        value = settings.speechSpeed,
                        onValueChange = { v -> val snapped = (kotlin.math.round(v * 20) / 20f).coerceIn(0.5f, 1.5f); if (snapped != settings.speechSpeed) viewModel.update { it.copy(speechSpeed = snapped) } },
                        valueRange = 0.5f..1.5f,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("speech speed") }.testTag("phone-speech-speed"),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                PlaySample(viewModel, learning)
            }
            if (engine != null && packages.isNotEmpty()) {
                if (packages.size == 1) PhoneModelCard(viewModel, engine, packages.single())
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

/** Where speech stands for the language being learned, as one line across the top. */
@Composable
private fun StatusBanner(engine: LocalSpeechEngine?, checking: Boolean, ready: Boolean, learning: LanguageOption?, voices: Map<String, List<SpeechVoice>>) {
    val colors = MaterialTheme.colorScheme
    val (icon, tint, text) = when {
        engine == null -> Triple(Icons.Default.Info, colors.primary, tr("Using system voices"))
        checking -> Triple(Icons.Default.Info, colors.outline, tr("Checking speech"))
        !ready -> Triple(Icons.Default.Warning, StatusTints.warning, tr("{0} is not installed", engine.displayName))
        learning != null && voices[learning.code].isNullOrEmpty() -> Triple(Icons.Default.Warning, StatusTints.warning, tr("No {0} voice downloaded. System voice is being used.", tr(learning.name)))
        else -> Triple(Icons.Default.CheckCircle, StatusTints.ok, tr("Offline speech ready"))
    }
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.panel).background(tint.copy(alpha = 0.10f)).border(1.dp, tint.copy(alpha = 0.3f), AppShapes.panel).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (tint == StatusTints.warning) tint else colors.onSurface)
    }
}

@Composable
private fun PhoneCard(title: String, icon: ImageVector, titleExtra: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surface).border(1.dp, colors.outlineVariant, AppShapes.card).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon, size = 44)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            titleExtra()
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

/** Whether the engine works; the install button shows only while it does not. */
@Composable
private fun PhoneEngineStatus(viewModel: SpeechViewModel, engine: LocalSpeechEngine) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val working = ready && !failed
    val tint = when {
        busy -> colors.outline
        working -> StatusTints.ok
        else -> StatusTints.warning
    }
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.panel).background(tint.copy(alpha = 0.08f)).border(1.dp, tint.copy(alpha = 0.25f), AppShapes.panel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                busy -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp)
                working -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
                else -> Icon(Icons.Default.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        busy -> tr("Checking…")
                        working -> tr("Installed and working")
                        ready -> tr("Something went wrong")
                        else -> tr("Not installed")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (working && !busy) tint else colors.onSurface,
                )
                status?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            if (engine.hasRuntimeSetup && !ready && !busy) {
                Spacer(Modifier.width(10.dp))
                PrimaryButton(tr("Install"), onClick = viewModel::install)
            }
        }
        progress?.let {
            Text(tr(it), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(Modifier.fillMaxWidth())
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
    SecondaryButton(onClick = { speaker.toggle(text, language?.code ?: "en") }, modifier = Modifier.fillMaxWidth()) {
        Icon(if (active && !working) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            when {
                active && working -> tr("Preparing…")
                active -> tr("Stop")
                else -> tr("Play sample")
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** One download holding the model and every voice, as Kokoro has. */
@Composable
private fun PhoneModelCard(viewModel: SpeechViewModel, engine: LocalSpeechEngine, pkg: SpeechPackage) {
    val packageBusy by viewModel.packageBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val busy = pkg.id in packageBusy
    val voiceCount = Regex("""(\d+) voices""").find(pkg.title)?.groupValues?.get(1)
    PhoneCard(tr("Voice model"), AppIcons.Download, titleExtra = { if (pkg.installed) Tag(tr("Installed"), StatusTints.ok) else Tag(tr("Not downloaded"), StatusTints.warning) }) {
        Text(tr("{0} voice model", engine.displayName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(voiceCount?.let { trPlural(it.toInt(), "{0} voice", "{0} voices") }, pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pkg.group.split(", ").filter { it.isNotBlank() }.take(6).forEach { name -> CodeTile(LanguageCodes.codeFor(name) ?: name.take(2), size = 34) }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
        Spacer(Modifier.height(14.dp))
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
    AppTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(tr("Search voices")) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (installed.isNotEmpty()) {
        Text(tr("Installed voices"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Tag(tr("{0} installed", packages.count { it.installed }), colors.primary)
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
    Column(Modifier.fillMaxWidth().clip(AppShapes.panel).background(colors.surface).border(1.dp, colors.outlineVariant, AppShapes.panel)) {
        voices.forEachIndexed { i, pkg ->
            if (i > 0) HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CodeTile(pkg.languageCode ?: pkg.group.take(2), size = 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(pkg.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(tr(pkg.group), pkg.sizeBytes.takeIf { it > 0 }?.let(::formatSize)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (pkg.id in packageBusy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                Spacer(Modifier.width(8.dp))
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
    Column(Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surface).border(1.dp, colors.outlineVariant, AppShapes.card)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(AppIcons.Tune, size = 44)
            Text(tr("Advanced engine settings"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        if (open) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
