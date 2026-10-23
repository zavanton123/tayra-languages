package com.tayra.languages.feature.playlists

import com.tayra.languages.core.ui.components.ScrollColumn
import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.domain.playlists.PlaylistSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.PopupCard
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The reader's playlist button, round and raised like the reader's other floating buttons and
 * [size] across: it opens a menu of the language's playlists, each ticked when it holds the text
 * with [bookId], to put the text in or take it out, and a way to make a new playlist with it.
 * Putting the text in a playlist closes the menu and hands the playlist's name to [onAdded], for
 * the screen to confirm. The menu's right edge is [menuShift] to the right of the button's, so it
 * can line up with the end of the controls beside it and stay inside the window.
 */
@Composable
fun AddToPlaylistButton(
    bookId: Long,
    size: Dp,
    onCreate: () -> Unit,
    onAdded: (playlist: String) -> Unit,
    modifier: Modifier = Modifier,
    menuShift: Dp = 0.dp,
    viewModel: AddToPlaylistViewModel = koinViewModel(key = "add-to-playlist-$bookId") { parametersOf(PlaylistSource.Text(bookId)) },
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val description = tr("Add to playlist")
    CollectEvents(viewModel.added) { name -> open = false; onAdded(name) }
    Box(modifier) {
        AppTooltip(description) {
            Box(
                Modifier.size(size).shadow(AppElevation.raised, AppShapes.circle).clip(AppShapes.circle)
                    .background(if (open) colors.primary.copy(alpha = AppAlpha.soft) else colors.surface)
                    .border(AppBorders.selected(colors.primary), AppShapes.circle)
                    .clickable { open = !open }
                    .semantics { contentDescription = description }
                    .testTag("add-to-playlist"),
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.PlaylistAdd, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconMedium)) }
        }
        if (open) {
            Popup(
                alignment = Alignment.BottomEnd,
                offset = with(LocalDensity.current) { IntOffset(menuShift.roundToPx(), -(size + AppSpacing.m).roundToPx()) },
                onDismissRequest = { open = false },
            ) { AddToPlaylistMenu(viewModel, onCreate = { open = false; onCreate() }) }
        }
    }
}

@Composable
private fun AddToPlaylistMenu(viewModel: AddToPlaylistViewModel, onCreate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    PopupCard(Modifier.width(MENU_WIDTH).testTag("add-to-playlist-menu")) {
        Column(Modifier.padding(AppSpacing.s)) {
            Text(tr("Add to playlist"), Modifier.padding(horizontal = AppSpacing.s, vertical = AppSpacing.s), style = AppText.itemTitle)
            ScrollColumn(Modifier.heightIn(max = LIST_HEIGHT)) {
                if (state.playlists.isEmpty()) {
                    Text(tr("No playlists yet"), Modifier.padding(AppSpacing.s), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                state.playlists.forEach { playlist ->
                    val contains = state.contains(playlist)
                    Row(
                        Modifier.fillMaxWidth().clip(AppShapes.control)
                            .background(if (contains) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
                            .clickable { viewModel.toggle(playlist.id) }
                            .padding(AppSpacing.s).testTag("menu-playlist-${playlist.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
                    ) {
                        Box(Modifier.size(AppSizes.tile).clip(AppShapes.compact).background(coverColor(playlist.cover).copy(alpha = AppAlpha.line)))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = AppText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(textCount(playlist.items.size), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        if (contains) {
                            Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).background(colors.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = tr("In this playlist"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
                            }
                        } else {
                            Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).border(AppBorders.hairline(colors.outline), AppShapes.circle))
                        }
                    }
                }
            }
            AppDivider(Modifier.padding(vertical = AppSpacing.xs))
            Row(
                Modifier.fillMaxWidth().clip(AppShapes.control).clickable(onClick = onCreate).padding(AppSpacing.m).testTag("menu-new-playlist"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(AppSizes.icon))
                Text(tr("Create new playlist"), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * Puts [source], a lesson or a whole course, into a playlist, or takes it out, with the reader's
 * playlist menu opening below the button (above it where there is no room). A playlist is ticked
 * when it holds all of [source]. The button is an icon, or a full-width button with [label] where
 * there is room. Putting it in a playlist closes the menu and hands the playlist's name to [onAdded].
 */
@Composable
fun AddToPlaylistMenuButton(
    source: PlaylistSource,
    onAdded: (playlist: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    viewModel: AddToPlaylistViewModel = koinViewModel(key = "add-to-playlist-$source") { parametersOf(source) },
) {
    var open by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    CollectEvents(viewModel.added) { name -> open = false; onAdded(name) }
    val tag = when (source) {
        is PlaylistSource.WholeCourse -> "add-course-to-playlist-${source.courseId}"
        is PlaylistSource.Lesson -> "add-lesson-to-playlist-${source.lessonId}"
        is PlaylistSource.Text -> "add-text-to-playlist-${source.bookId}"
    }
    val gap = with(LocalDensity.current) { AppSpacing.xs.roundToPx() }
    Box(modifier) {
        if (label != null) {
            SecondaryButton(label, onClick = { open = !open }, modifier = Modifier.fillMaxWidth().testTag(tag), icon = AppIcons.PlaylistAdd)
        } else {
            AppTooltip(tr("Add to playlist")) {
                AppIconButton(AppIcons.PlaylistAdd, contentDescription = tr("Add to playlist"), onClick = { open = !open }, modifier = Modifier.testTag(tag), tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (open) {
            Popup(popupPositionProvider = remember(gap) { BelowOrAbove(gap) }, onDismissRequest = { open = false }) {
                AddToPlaylistMenu(viewModel, onCreate = { open = false; creating = true })
            }
        }
    }
    if (creating) {
        NewPlaylistDialog(bookId = null, source = source, onCreated = { _, name -> creating = false; onAdded(name) }, onDismiss = { creating = false })
    }
}

/** Puts a popup under its anchor, its end edge on the anchor's, or above the anchor when the window has no room below. */
private class BelowOrAbove(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val end = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val x = end.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below else maxOf(0, anchorBounds.top - gap - popupContentSize.height)
        return IntOffset(x, y)
    }
}

private val MENU_WIDTH = 360.dp
private val LIST_HEIGHT = 320.dp
