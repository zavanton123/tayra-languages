package com.idiomiko.feature.settings

import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.language.LanguageOption
import com.idiomiko.core.domain.service.LocalSpeechEngine
import com.idiomiko.core.domain.service.SetupItem
import com.idiomiko.core.domain.service.SetupKind
import com.idiomiko.core.domain.service.SetupState
import com.idiomiko.core.domain.service.SetupStatus
import com.idiomiko.core.domain.service.SpeechEngine
import com.idiomiko.core.domain.service.SpeechVoice
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.audio.rememberSpeaker
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** One voice of the language for the engine in use, as its row shows it. */
internal class VoiceChoice(
    /** The id the engine speaks it by; null for the system's own voice. */
    val voiceId: String?,
    val name: String,
    /** Its accent or country, its quality or kind, and the engine: "Brazil · Medium quality · Piper". */
    val detail: String,
    /** Its download, where the page downloads voices. */
    val item: SetupItem?,
    val onDevice: Boolean,
) {
    val key: String get() = item?.id ?: "voice:${voiceId ?: "system"}"
}

/**
 * The voices of the language for [engine]: those the page can download, downloaded or not, and
 * those on the device it does not list, the ones on the device first. The system voices are one
 * row, as the system picks among them.
 */
internal fun voiceChoices(engine: LocalSpeechEngine?, onDevice: List<SpeechVoice>, downloads: List<SetupItem>?): List<VoiceChoice> {
    if (engine == null) return listOf(VoiceChoice(null, tr("System voice"), tr("The system picks the voice for each language."), null, onDevice = true))
    val listed = downloads.orEmpty()
        .filter { it.kind == SetupKind.VOICE && it.status != SetupStatus.UNAVAILABLE && it.engine == engine.displayName }
        .map { item ->
            val (name, details) = voiceParts(item.name.substringAfter(" · "))
            VoiceChoice(voiceId(item, engine.engine), name, voiceDetail(details, engine.displayName), item, item.status == SetupStatus.INSTALLED)
        }
    val unlisted = onDevice.filter { voice -> listed.none { it.voiceId == voice.id } }.map { voice ->
        val (name, details) = voiceParts(voice.name)
        VoiceChoice(voice.id, name, voiceDetail(details, engine.displayName), null, onDevice = true)
    }
    return (listed + unlisted).sortedBy { !it.onDevice }
}

/** The id [engine] speaks the voice of [item] by: Piper's package, or the voice inside Kokoro's model. */
private fun voiceId(item: SetupItem, engine: SpeechEngine): String =
    item.id.removePrefix("voice:${engine.name}:").let { if (item.sharedModel) it.substringAfter(':') else it }

/** A voice's name and its details, from "Cadu · medium · Brazil" or "Dora (Brazilian, female)", the place first. */
internal fun voiceParts(name: String): Pair<String, List<String>> {
    val dots = name.split(" · ")
    if (dots.size > 1) return dots.first() to dots.drop(1).reversed()
    val open = name.lastIndexOf(" (")
    if (open < 0 || !name.endsWith(")")) return name to emptyList()
    return name.substring(0, open) to name.substring(open + 2, name.length - 1).split(", ")
}

private fun voiceDetail(details: List<String>, engine: String): String =
    (details.map(::voiceWord) + engine).joinToString(" · ")

/** A detail of a voice in the interface language: a quality said as one, anything else capitalised. */
private fun voiceWord(word: String): String = when (word.replace('_', ' ')) {
    "x low" -> tr("Very low quality")
    "low" -> tr("Low quality")
    "medium" -> tr("Medium quality")
    "high" -> tr("High quality")
    else -> tr(word).replaceFirstChar { it.uppercase() }
}

/** The voice that speaks: the one chosen if it is on the device, else the one the engine falls back to. */
private fun currentVoice(choices: List<VoiceChoice>, onDevice: List<SpeechVoice>, chosen: String?): VoiceChoice? {
    val usable = choices.filter { it.onDevice }
    return usable.firstOrNull { it.voiceId == chosen }
        ?: onDevice.firstOrNull()?.let { first -> usable.firstOrNull { it.voiceId == first.id } }
        ?: usable.firstOrNull()
}

/**
 * The language's voices for the engine in use, one row each: a button to hear it, its name and
 * details, and where it stands. The current one is ringed; another one on the device becomes it
 * when its row is clicked; one not on the device is downloaded in place. A long list shows those on
 * the device and a few more, the rest behind Browse more voices.
 */
@Composable
internal fun VoiceList(settings: UserSettings, viewModel: SpeechViewModel, engine: LocalSpeechEngine?, learning: LanguageOption?, voices: Map<String, List<SpeechVoice>>) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val progress by viewModel.downloadStates.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<Confirmation?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val code = learning?.code
    val onDevice = code?.let { voices[it] }.orEmpty()
    val choices = voiceChoices(engine, onDevice, if (viewModel.downloadsHere) downloads else null)
    val current = currentVoice(choices, onDevice, engine?.let { settings.speechVoices["${it.engine.name}:$code"] })

    Text(learning?.let { tr("{0} voices", languageInSentence(it.name, LanguageCase.PREPOSITIONAL)) } ?: tr("Voices"), style = AppText.itemTitle)
    if (engine != null) Text(tr("Select a voice or download another one."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(AppSpacing.m))
    when {
        choices.isEmpty() && viewModel.downloadsHere && downloads == null ->
            Row(Modifier.fillMaxWidth().padding(AppSpacing.xl), horizontalArrangement = Arrangement.Center) { AppSpinner(size = SpinnerSize.Medium) }
        choices.isEmpty() && engine != null -> InfoBanner(
            if (learning == null) tr("{0} has no voice for your language on this device yet, so the system voice reads it.", engine.displayName)
            else tr("{0} has no {1} voice on this device yet, so the system voice reads {1}.", engine.displayName, tr(learning.name)),
            tint = StatusTints.warning,
            icon = Icons.Default.Warning,
        )
        else -> {
            val speaker = rememberSpeaker(viewModel.localSpeech, viewModel.settingsRepository)
            val playing by speaker.playing.collectAsStateWithLifecycle()
            val working by speaker.working.collectAsStateWithLifecycle()
            var playingKey by remember { mutableStateOf<String?>(null) }
            val sample = code?.let { SAMPLES[it] } ?: SAMPLES.getValue("en")
            // A model the voices share is downloaded for all of them, so each of its rows shows the download.
            val modelProgress = choices.mapNotNull { it.item?.takeIf(SetupItem::sharedModel) }
                .firstNotNullOfOrNull { progress[it.id]?.takeIf { state -> state !is SetupState.Done } }
            val states = choices.associate { choice ->
                val item = choice.item
                choice.key to (if (item == null) RowState.Installed(null) else rowState(item, progress[item.id] ?: modelProgress.takeIf { _ -> item.sharedModel }))
            }
            val long = choices.size > VISIBLE_VOICES
            val shown = if (!long || expanded) choices else {
                val room = maxOf(VISIBLE_VOICES, choices.count { it.onDevice })
                choices.filterIndexed { i, choice -> i < room || states[choice.key] is RowState.Working }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                shown.forEach { choice ->
                    val item = choice.item
                    val voiceId = choice.voiceId
                    val active = playing != null && playing == sample && playingKey == choice.key
                    VoiceRow(
                        choice,
                        states.getValue(choice.key),
                        current = choice === current,
                        playing = active,
                        preparing = active && working,
                        onPlay = {
                            if (active) speaker.stop()
                            else {
                                playingKey = choice.key
                                speaker.speak(sample, code ?: "en", voiceId = voiceId)
                            }
                        },
                        onSelect = if (engine != null && code != null && voiceId != null && choice.onDevice && choice !== current) {
                            { viewModel.chooseVoice(engine.engine, code, voiceId) }
                        } else null,
                        onDownload = item?.let { { viewModel.install(it) } },
                        onRemove = item?.takeIf { choice.onDevice && viewModel.canRemove(it) }?.let { removable ->
                            { confirming = removal(removable, choice.name) { viewModel.remove(removable) } }
                        },
                    )
                }
            }
            if (long) {
                QuietButton(
                    if (expanded) tr("Show fewer") else tr("Browse more voices"),
                    onClick = { expanded = !expanded },
                    icon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                    size = ButtonSize.Compact,
                    modifier = Modifier.padding(top = AppSpacing.s).testTag("voices-more"),
                )
            }
        }
    }
    confirming?.let { c -> RemovePackDialog(c.question, c.consequence, onRemove = c.remove, onDismiss = { confirming = null }) }
}

/** What removing [item] asks: a voice of a shared model goes with the whole model. */
private fun removal(item: SetupItem, name: String, remove: () -> Unit): Confirmation =
    if (item.sharedModel) {
        Confirmation(tr("Remove the {0} model?", item.engine.orEmpty()), tr("All its voices are removed with it. You can download them again at any time."), remove)
    } else {
        Confirmation(tr("Remove the voice {0}?", name), tr("You can download it again at any time."), remove)
    }

/**
 * One voice: the round button that plays the sample in it (grey until it is on the device), its
 * name and details, and on the right its size, whether it is the current one, and Download or
 * Remove. On a phone the size and the button go under the details.
 */
@Composable
private fun VoiceRow(
    choice: VoiceChoice,
    state: RowState,
    current: Boolean,
    playing: Boolean,
    preparing: Boolean,
    onPlay: () -> Unit,
    onSelect: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val shape = AppShapes.panel
    val tag = "voice-${choice.key}"
    // The system's voice has nothing to download or remove.
    val downloadable = choice.voiceId != null
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .border(if (current) AppBorders.chosen() else AppBorders.hairline(), shape)
            .background(if (current) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .then(if (onSelect != null) Modifier.clickable(onClick = onSelect) else Modifier)
            .semantics { selected = current }
            .testTag("row-$tag")
            // Tighter on a phone, so the size and Remove share a line under the details.
            .padding(horizontal = if (compact) AppSpacing.m else AppSpacing.l, vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.l),
    ) {
        AppIconButton(
            if (playing && !preparing) AppIcons.Stop else AppIcons.PlayArrow,
            contentDescription = if (playing) tr("Stop") else tr("Play sample"),
            onClick = onPlay,
            enabled = choice.onDevice,
            style = IconButtonStyle.Filled,
            shape = AppShapes.circle,
            loading = preparing,
            modifier = Modifier.testTag("play-$tag"),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(choice.name, style = AppText.itemTitleBold)
                if (current && compact) CurrentTag(tag)
            }
            Text(choice.detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            DownloadProgress(state)
            if (compact && downloadable) {
                Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f)) { VoiceStatus(state) }
                    DownloadAction(state, tag, onDownload, onRemove)
                }
            }
        }
        if (!compact) {
            if (downloadable) VoiceStatus(state)
            if (current) CurrentTag(tag)
            DownloadAction(state, tag, onDownload, onRemove)
        }
    }
}

@Composable
private fun CurrentTag(tag: String) = Tag(tr("Current"), MaterialTheme.colorScheme.primary, Modifier.testTag("current-$tag"))

/** On the device with its size, in green, or the size to download; smaller on a phone, to keep to one line beside the button. */
@Composable
private fun VoiceStatus(state: RowState) {
    val style = if (LocalWindowWidth.current.isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    when (state) {
        is RowState.Installed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconSmall))
            Text(
                listOfNotNull(state.bytes?.let(::formatSize), tr("On this device")).joinToString(" · "),
                style = style,
                color = StatusTints.ok,
            )
        }
        is RowState.Available -> state.size?.let { Text(it, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else -> Unit
    }
}

/** How many voices a long list shows before Browse more voices, unless more are on the device. */
private const val VISIBLE_VOICES = 4

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
