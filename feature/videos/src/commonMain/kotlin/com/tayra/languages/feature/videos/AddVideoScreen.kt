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
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.media.TranscriptionModel
import com.tayra.languages.core.domain.media.TranscriptionStage
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.core.ui.media.VideoPlayer
import com.tayra.languages.core.ui.media.VideoSource
import com.tayra.languages.core.ui.media.YouTubeTrack
import com.tayra.languages.core.ui.theme.AppShapes
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
    val subtitlePicker = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.pickerExtensions)) { file ->
        if (file != null) scope.launch { viewModel.pickSubtitles(file.name, file.readBytes()) }
    }
    val translationPicker = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.pickerExtensions)) { file ->
        if (file != null) scope.launch { viewModel.pickTranslation(file.name, file.readBytes()) }
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Add video"), onNavigate = onNavigate, onBack = onBack, section = NavSection.VIDEOS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        AddVideoContent(
            state,
            AddVideoActions(
                onOrigin = viewModel::setOrigin,
                onYouTubeInput = viewModel::setYouTubeInput,
                onYouTubePlayer = viewModel::onYouTubePlayer,
                onTrack = viewModel::chooseTrack,
                onTranslate = viewModel::setTranslate,
                onLinkInput = viewModel::setLinkInput,
                onPickSubtitles = { subtitlePicker.launch() },
                onPickTranslation = { translationPicker.launch() },
                onSubtitlesUrl = viewModel::setSubtitlesUrl,
                onTranslationUrl = viewModel::setTranslationUrl,
                onFetch = viewModel::fetchSubtitles,
                onClearTranslation = viewModel::clearTranslation,
                onInstallModel = viewModel::installModel,
                onRemoveModel = viewModel::removeModel,
                onTranscribe = viewModel::transcribe,
                onCancelTranscription = viewModel::cancelTranscription,
                onTitle = viewModel::setTitle,
                onAdd = viewModel::add,
            ),
            Modifier.padding(padding),
        )
    }
}

internal class AddVideoActions(
    val onOrigin: (VideoOrigin) -> Unit = {},
    val onYouTubeInput: (String) -> Unit = {},
    val onYouTubePlayer: (VideoController) -> Unit = {},
    val onTrack: (YouTubeTrack) -> Unit = {},
    val onTranslate: (Boolean) -> Unit = {},
    val onLinkInput: (String) -> Unit = {},
    val onPickSubtitles: () -> Unit = {},
    val onPickTranslation: () -> Unit = {},
    val onSubtitlesUrl: (String) -> Unit = {},
    val onTranslationUrl: (String) -> Unit = {},
    val onFetch: (asTranslation: Boolean) -> Unit = {},
    val onClearTranslation: () -> Unit = {},
    val onInstallModel: (TranscriptionModel) -> Unit = {},
    val onRemoveModel: (TranscriptionModel) -> Unit = {},
    val onTranscribe: () -> Unit = {},
    val onCancelTranscription: () -> Unit = {},
    val onTitle: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
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
        ScreenHeader(tr("Add video"), tr("A YouTube video, or a video on the internet with its subtitles."))
        SegmentedToggle(
            listOf(Segment(VideoOrigin.YOUTUBE, AppIcons.Movie, "YouTube", testTag = "origin-youtube"), Segment(VideoOrigin.LINK, AppIcons.Link, tr("Video link"), testTag = "origin-link")),
            selected = state.origin,
            onSelect = actions.onOrigin,
        )
        if (state.origin == VideoOrigin.YOUTUBE) YouTubeForm(state, actions) else LinkForm(state, actions)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(tr("Title"), style = AppText.label)
            AppTextField(state.title, actions.onTitle, Modifier.fillMaxWidth().testTag("video-title"), singleLine = true)
        }
        state.error?.let { Notice(tr(it), tint = colors.error) }
        PrimaryButton(tr("Add video"), onClick = actions.onAdd, enabled = state.canAdd, loading = state.saving, icon = AppIcons.Movie, modifier = Modifier.testTag("add-video-confirm"))
    }
}

@Composable
private fun LinkForm(state: AddVideoUiState, actions: AddVideoActions) {
    val colors = MaterialTheme.colorScheme
    val link = state.link
    AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(tr("Video link"), style = AppText.itemTitle)
        Text(tr("The address of a video file or stream (MP4, WebM, HLS)."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        AppTextField(link.input, actions.onLinkInput, Modifier.fillMaxWidth().testTag("video-link"), singleLine = true, placeholder = { Text("https://…/video.mp4") })
        if (link.input.isNotBlank() && link.url == null) Text(tr("This is not a web address."), style = MaterialTheme.typography.bodySmall, color = colors.error)
    }
    SubtitlesCard(
        title = tr("Subtitles"),
        note = tr("SRT, WebVTT or ASS, in the language of the video: a file, or its address."),
        picked = link.subtitles,
        url = link.subtitlesUrl,
        fetching = link.fetching,
        tag = "subtitles",
        onPick = actions.onPickSubtitles,
        onUrl = actions.onSubtitlesUrl,
        onFetch = { actions.onFetch(false) },
        extra = if (state.transcription.offered) ({ WhisperSection(state, actions) }) else null,
    )
    SubtitlesCard(
        title = tr("Translation (optional)"),
        note = tr("Subtitles in your language, shown under each line. Without them, lines are translated as you watch."),
        picked = link.translation,
        url = link.translationUrl,
        fetching = link.fetching,
        tag = "translation",
        onPick = actions.onPickTranslation,
        onUrl = actions.onTranslationUrl,
        onFetch = { actions.onFetch(true) },
        onClear = actions.onClearTranslation,
    )
}

@Composable
private fun SubtitlesCard(
    title: String,
    note: String,
    picked: PickedSubtitles?,
    url: String,
    fetching: Boolean,
    tag: String,
    onPick: () -> Unit,
    onUrl: (String) -> Unit,
    onFetch: () -> Unit,
    onClear: (() -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(title, style = AppText.itemTitle)
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            SecondaryButton(tr("Choose file"), onClick = onPick, icon = AppIcons.Subtitles, modifier = Modifier.testTag("pick-$tag"))
            picked?.let {
                Text(describe(it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false).testTag("$tag-picked"))
                onClear?.let { clear -> QuietButton(tr("Remove"), onClick = clear) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            AppTextField(url, onUrl, Modifier.weight(1f).testTag("$tag-url"), singleLine = true, placeholder = { Text("https://…/subtitles.vtt") })
            SecondaryButton(tr("Fetch"), onClick = onFetch, enabled = url.isNotBlank() && !fetching, loading = fetching, modifier = Modifier.testTag("fetch-$tag"))
        }
        extra?.invoke()
    }
}

/** Whisper, offered for a link without subtitles: a model to download first, then the transcription and its progress. */
@Composable
private fun WhisperSection(state: AddVideoUiState, actions: AddVideoActions) {
    val colors = MaterialTheme.colorScheme
    val t = state.transcription
    Text(tr("Or let Whisper write them down"), style = AppText.label, modifier = Modifier.padding(top = AppSpacing.s))
    Text(
        tr("Whisper listens to the video on this device and writes down what is said. A long video takes a while."),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
    )
    when {
        t.running -> Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s), modifier = Modifier.testTag("transcription-progress")) {
            AppProgressBar(t.fraction)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val stage = if (t.stage == TranscriptionStage.SPEECH) tr("Writing down what is said…") else tr("Reading the video's sound…")
                val percent = t.fraction?.let { " ${(it * 100).toInt()}%" }.orEmpty()
                Text(stage + percent, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                QuietButton(tr("Cancel"), onClick = actions.onCancelTranscription, modifier = Modifier.testTag("cancel-transcription"))
            }
        }
        t.ready -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            SecondaryButton(
                tr("Transcribe with Whisper"),
                onClick = actions.onTranscribe,
                enabled = state.link.url != null,
                icon = AppIcons.Mic,
                modifier = Modifier.testTag("transcribe"),
            )
            t.models.firstOrNull { it.installed }?.let { model ->
                Text(model.name, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false))
                QuietButton(tr("Remove"), onClick = { actions.onRemoveModel(model) }, modifier = Modifier.testTag("remove-${model.id}"))
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(tr("Download a Whisper model first; it stays on this device."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            t.models.forEach { model ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Column(Modifier.weight(1f)) {
                        Text(model.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOfNotNull(formatSize(model.downloadSize), model.note.takeIf { it.isNotBlank() }?.let { tr(it) }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    SecondaryButton(
                        tr("Download"),
                        onClick = { actions.onInstallModel(model) },
                        enabled = t.installing == null,
                        loading = t.installing == model.id,
                        icon = AppIcons.Download,
                        modifier = Modifier.testTag("install-${model.id}"),
                    )
                }
            }
            t.installProgress?.takeIf { t.installing != null }?.let { Text(tr(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YouTubeForm(state: AddVideoUiState, actions: AddVideoActions) {
    val colors = MaterialTheme.colorScheme
    val pick = state.youtube
    AppCard(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(tr("YouTube link"), style = AppText.itemTitle)
        AppTextField(
            pick.input,
            actions.onYouTubeInput,
            Modifier.fillMaxWidth().testTag("youtube-link"),
            singleLine = true,
            placeholder = { Text("https://www.youtube.com/watch?v=…") },
        )
        if (pick.input.isNotBlank() && pick.id == null) {
            Text(tr("This is not a YouTube video link."), style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
        pick.id?.let { id ->
            val source = remember(id, state.languageCode) { VideoSource.YouTube(id, state.languageCode) }
            Box(Modifier.fillMaxWidth().widthIn(max = PREVIEW_WIDTH).aspectRatio(PREVIEW_RATIO).clip(AppShapes.small)) {
                VideoPlayer(source, onController = actions.onYouTubePlayer, modifier = Modifier.fillMaxSize())
            }
        }
        when {
            pick.unsupported -> Notice(tr("YouTube videos can't be added on this device yet."), tint = colors.error)
            pick.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                AppSpinner(size = SpinnerSize.Small)
                Text(tr("Reading the video…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        pick.details?.let { details ->
            details.channel?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            if (details.tracks.isNotEmpty()) {
                Text(tr("Subtitles"), style = AppText.label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    val labels = details.tracks.map { trackLabel(it) }
                    details.tracks.forEachIndexed { i, track ->
                        // Two tracks of one language (Brazil's and Portugal's, say) go by YouTube's own names for them.
                        val label = if (labels.count { it == labels[i] } > 1) track.name.ifBlank { track.languageCode } else labels[i]
                        SelectableChip(label, selected = pick.track == track, onClick = { actions.onTrack(track) }, modifier = Modifier.testTag("track-${track.languageCode}-${track.automatic}"))
                    }
                }
                pick.track?.takeIf { it.automatic }?.let {
                    Text(tr("Automatic subtitles have no punctuation; their pieces are joined into lines."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                SwitchSetting(
                    tr("YouTube's translation"),
                    tr("Show YouTube's translation of each line into your language."),
                    checked = pick.translate,
                    onChange = actions.onTranslate,
                )
            }
        }
    }
}

@Composable
private fun trackLabel(track: YouTubeTrack): String {
    val language = LanguageCodes.option(track.languageCode.substringBefore('-'))?.name ?: track.name.ifBlank { track.languageCode }
    return if (track.automatic) tr("{0} (automatic)", language) else language
}

@Composable
private fun describe(picked: PickedSubtitles): String =
    "${picked.name} · ${trPlural(picked.cues.size, "{0} line", "{0} lines")} · ${formatClock(picked.lastMs)}"

private val FORM_WIDTH = 720.dp
private val PREVIEW_WIDTH = 560.dp
private const val PREVIEW_RATIO = 16f / 9f
