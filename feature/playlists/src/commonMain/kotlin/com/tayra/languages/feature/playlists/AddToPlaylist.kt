package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.PopupCard
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The reader's playlist button, round and raised like the reader's other floating buttons and
 * [size] across: it opens a menu of the language's playlists, each ticked when it holds the text
 * with [bookId], to put the text in or take it out, and a way to make a new playlist with it. The
 * menu's right edge is [menuShift] to the right of the button's, so it can line up with the end of
 * the controls beside it and stay inside the window.
 */
@Composable
fun AddToPlaylistButton(
    bookId: Long,
    size: Dp,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    menuShift: Dp = 0.dp,
    viewModel: AddToPlaylistViewModel = koinViewModel(key = "add-to-playlist-$bookId") { parametersOf(bookId) },
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val description = tr("Add to playlist")
    Box(modifier) {
        AppTooltip(description) {
            Box(
                Modifier.size(size).shadow(AppElevation.raised, AppShapes.circle).clip(AppShapes.circle)
                    .background(if (open) colors.primary.copy(alpha = AppAlpha.soft) else colors.surface)
                    .border(AppBorders.selected(colors.primary), AppShapes.circle)
                    .clickable { open = !open; viewModel.clearAdded() }
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
    // The confirmation goes after a while, as a toast would.
    LaunchedEffect(state.added) { if (state.added != null) { delay(CONFIRMATION_MILLIS); viewModel.clearAdded() } }
    PopupCard(Modifier.width(MENU_WIDTH).testTag("add-to-playlist-menu")) {
        Column(Modifier.padding(AppSpacing.s)) {
            Text(tr("Add to playlist"), Modifier.padding(horizontal = AppSpacing.s, vertical = AppSpacing.s), style = AppText.itemTitle)
            Column(Modifier.heightIn(max = LIST_HEIGHT).verticalScroll(rememberScrollState())) {
                if (state.playlists.isEmpty()) {
                    Text(tr("No playlists yet"), Modifier.padding(AppSpacing.s), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                state.playlists.forEach { playlist ->
                    val contains = viewModel.contains(playlist)
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
            state.added?.let { name ->
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.control).background(AppAccents.green.copy(alpha = AppAlpha.tint)).padding(AppSpacing.m).testTag("added-to-playlist"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.icon))
                    Text(tr("Added to {0}", name), style = AppText.label, color = AppAccents.onWash(AppAccents.green))
                }
            }
        }
    }
}

private val MENU_WIDTH = 360.dp
private val LIST_HEIGHT = 320.dp
private const val CONFIRMATION_MILLIS = 3_000L
