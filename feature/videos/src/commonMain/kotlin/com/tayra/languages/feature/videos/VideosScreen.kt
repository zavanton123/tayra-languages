package com.tayra.languages.feature.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoListItem
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VideosScreen(onNavigate: (Route) -> Unit, viewModel: VideosViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = tr("Videos"), onNavigate = onNavigate, section = NavSection.VIDEOS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        VideosContent(state, onNavigate, viewModel::setSearch, viewModel::delete, Modifier.padding(padding))
    }
}

@Composable
internal fun VideosContent(
    state: VideosUiState,
    onNavigate: (Route) -> Unit,
    onSearch: (String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = LocalWindowWidth.current.isCompact
    var deleting by remember { mutableStateOf<VideoListItem?>(null) }
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(
            tr("Videos"),
            tr("Watch with the subtitles as text you can read and look up."),
            actions = {
                if (state.videos.isNotEmpty()) {
                    PrimaryButton(tr("Add video"), onClick = { onNavigate(Route.NewVideo) }, icon = Icons.Default.Add, modifier = Modifier.testTag("add-video"))
                }
            },
        )
        if (state.videos.isEmpty()) {
            EmptyState(
                AppIcons.Movie,
                tr("No videos yet"),
                message = tr("Add a video file with its subtitles, or a YouTube link."),
                action = {
                    Spacer(Modifier.height(AppSpacing.s))
                    PrimaryButton(tr("Add video"), onClick = { onNavigate(Route.NewVideo) }, icon = Icons.Default.Add, modifier = Modifier.testTag("add-video"))
                },
            )
            return@ScrollColumn
        }
        if (state.videos.size > SEARCH_FROM) SearchField(state.search, onSearch, tr("Search videos"), Modifier.fillMaxWidth())
        val shown = state.shown
        if (shown.isEmpty()) {
            EmptyState(AppIcons.Movie, tr("No videos match"), message = tr("Try another search."))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                shown.forEach { item -> VideoCard(item, onOpen = { onNavigate(Route.Watch(item.video.bookId)) }, onDelete = { deleting = item }) }
            }
        }
    }
    deleting?.let { item ->
        ConfirmDialog(
            title = tr("Delete video?"),
            text = tr("Its transcript and reading progress are removed. Your saved words stay."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { onDelete(item.video.bookId); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun VideoCard(item: VideoListItem, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val video = item.video
    Row(
        Modifier.fillMaxWidth().cardSurface().clickable(onClick = onOpen).padding(AppSpacing.l).testTag("video-${video.bookId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Box(
            Modifier.size(width = THUMB_WIDTH, height = THUMB_HEIGHT).clip(AppShapes.small).background(colors.primary.copy(alpha = AppAlpha.soft)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Movie, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(video.title, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (video.kind == VideoKind.YOUTUBE) "YouTube" else tr("Video file"),
                    video.channel,
                    video.durationMs.takeIf { it > 0 }?.let { formatClock(it) },
                    item.knownPercent?.let { tr("{0}% known", it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (video.durationMs > 0 && video.positionMs > 0) {
                AppProgressBar(video.positionMs.toFloat() / video.durationMs, Modifier.width(PROGRESS_WIDTH).padding(top = AppSpacing.xs))
            }
            item.distinctUnknowns?.takeIf { it > 0 }?.let {
                Text(trPlural(it, "{0} new word", "{0} new words"), style = MaterialTheme.typography.bodySmall, color = colors.primary)
            }
        }
        OverflowMenu(tr("More")) { close ->
            AppMenuItem(text = { Text(tr("Delete")) }, onClick = { close(); onDelete() }, modifier = Modifier.testTag("delete-video-${video.bookId}"))
        }
    }
}

/** Milliseconds as a clock: "4:05", or "1:02:03" past an hour. */
internal fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    else "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** The search shows once the list is long enough to need it. */
private const val SEARCH_FROM = 6

private val THUMB_WIDTH = 96.dp
private val THUMB_HEIGHT = 54.dp
private val PROGRESS_WIDTH = 160.dp
