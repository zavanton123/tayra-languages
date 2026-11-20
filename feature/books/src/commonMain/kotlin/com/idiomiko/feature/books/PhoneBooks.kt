package com.idiomiko.feature.books

import com.idiomiko.core.ui.components.inLearnedScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ChipSize
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SelectableChip
import com.idiomiko.core.ui.components.TagFilterSheet
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.itemMotion
import com.idiomiko.core.ui.components.pressScale
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The phone bar's list menu (ticking books, the archive) and the button for a new book. */
@Composable
internal fun PhoneBooksMenu(archived: Boolean, selecting: Boolean, onSelecting: (Boolean) -> Unit, onNewBook: () -> Unit, onArchive: () -> Unit, onFindBooks: () -> Unit = {}) {
    OverflowMenu(tr("List actions")) { close ->
        AppMenuItem(text = { Text(if (selecting) tr("Done selecting") else tr("Select books")) }, onClick = { close(); onSelecting(!selecting) })
        if (!archived) AppMenuItem(text = { Text(tr("Find free books")) }, onClick = { close(); onFindBooks() })
        if (!archived) AppMenuItem(text = { Text(tr("Book archive")) }, onClick = { close(); onArchive() })
    }
    if (!archived) {
        AppIconButton(Icons.Default.Add, contentDescription = tr("New book"), onClick = onNewBook, modifier = Modifier.testTag("phone-new-book"), size = IconButtonSize.Large, style = IconButtonStyle.Filled)
        Spacer(Modifier.width(AppSpacing.s))
    }
}

/**
 * The books on a phone, in one column: the one being read as a card to go on with, the search with the
 * status, tags and order behind Filters, and the library as cards.
 */
@Composable
internal fun PhoneBooksContent(state: BooksUiState, callbacks: BooksCallbacks, selecting: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    // Selection replaces the continue card, so a visible continued book is never repeated below it.
    val continued = state.continueWith?.takeIf { !state.archived && !showChecks }
    val books = continued?.let { shown -> state.filteredBooks.filter { it.id != shown.id } } ?: state.filteredBooks
    var sheet by remember { mutableStateOf<PhoneBooksSheet?>(null) }
    ScrollList(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        if (state.archived && state.books.isEmpty()) {
            item { EmptyArchive() }
            return@ScrollList
        } else if (state.archived) {
            item { Text(tr("Books you have set aside. Unarchive one to keep reading it."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
        } else if (state.books.isEmpty()) {
            item { EmptyLibrary(state, callbacks.onNewBook, callbacks.onDownloadBooks, callbacks.onFindBooks) }
            return@ScrollList
        } else {
            continued?.let { book -> item { PhoneContinueCard(book) { callbacks.onOpen(book) } } }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"), Modifier.weight(1f))
                SecondaryButton(
                    tr("Filters"),
                    onClick = { sheet = PhoneBooksSheet.FILTERS },
                    modifier = Modifier.testTag("phone-filters"),
                    icon = AppIcons.Tune,
                    badge = state.filterCount.takeIf { it > 0 },
                )
            }
        }
        if (showChecks) item { BooksSelectionBar(state, callbacks, onDone) }
        if (books.isEmpty()) {
            item { NoBooks(state.archived, callbacks.onNewBook, onlyContinued = state.filteredBooks.isNotEmpty(), onFindBooks = callbacks.onFindBooks) }
        } else {
            items(books.size, key = { books[it].id }) { i -> Box(itemMotion()) { PhoneBookCard(books[i], books[i].id in state.selected, showChecks, callbacks) } }
        }
        item { LibraryFooter(state, onSelect = null, Modifier.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs)) }
    }

    when (sheet) {
        PhoneBooksSheet.FILTERS -> PhoneFiltersSheet(state, callbacks, onTags = { sheet = PhoneBooksSheet.TAGS }, onDismiss = { sheet = null })
        PhoneBooksSheet.TAGS -> TagFilterSheet(
            state.availableTags,
            state.tags,
            state.matchAllTags,
            onApply = { tags, all -> callbacks.onTags(tags, all); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> {}
    }
}

private enum class PhoneBooksSheet { FILTERS, TAGS }

/** The filters behind the Filters button: the status, the most used tags with the way to all of them, and the order. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneFiltersSheet(state: BooksUiState, callbacks: BooksCallbacks, onTags: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val chips: @Composable (@Composable () -> Unit) -> Unit = { content ->
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { content() }
    }
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m).testTag("filters-sheet"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Text(tr("Filters"), style = AppText.itemTitle)
            Text(tr("Status"), style = AppText.emphasis, color = colors.onSurfaceVariant)
            chips {
                ProgressFilter.entries.forEach { filter ->
                    SelectableChip(
                        tr(filter.label),
                        selected = state.progress == filter,
                        onClick = { callbacks.onProgress(filter) },
                        modifier = Modifier.testTag("status-${filter.name}"),
                        count = state.counts[filter] ?: 0,
                        size = ChipSize.Small,
                    )
                }
            }
            if (state.availableTags.isNotEmpty()) {
                Text(tr("Tags"), style = AppText.emphasis, color = colors.onSurfaceVariant)
                chips {
                    state.availableTags.take(SHEET_TAGS).forEach { tag ->
                        SelectableChip(
                            "#$tag",
                            selected = tag in state.tags,
                            onClick = { callbacks.onTags(if (tag in state.tags) state.tags - tag else state.tags + tag, state.matchAllTags) },
                            modifier = Modifier.testTag("tag-chip-$tag"),
                            tint = AppAccents.purple,
                            size = ChipSize.Small,
                        )
                    }
                    LinkText(tr("Filter by tags"), onTags, Modifier.testTag("tag-filter"), icon = AppIcons.Tag, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(tr("Sort"), style = AppText.emphasis, color = colors.onSurfaceVariant)
            chips {
                BookSort.entries.forEach { sort ->
                    SelectableChip(tr(sort.label), selected = state.sort == sort, onClick = { callbacks.onSort(sort) }, modifier = Modifier.testTag("sort-${sort.name}"), size = ChipSize.Small)
                }
            }
        }
    }
}

/** The book being read most recently: its cover, where the reader is, the two shares and the way back in. */
@Composable
internal fun PhoneContinueCard(book: BookListItem, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l).testTag("continue-card"), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            BookCover(book, PHONE_CONTINUE_COVER)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Continue reading").uppercase(), style = AppText.overline, color = colors.primary)
                Text(book.title.inLearnedScript(), style = AppText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(placeLine(book), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        ContinueBars(book, Modifier.fillMaxWidth(), spacing = AppSpacing.l, vertical = false)
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("continue-reading"), size = ButtonSize.Large)
    }
}

/** A book as a card: cover, title, length, the share of its words known, and its progress ring. */
@Composable
private fun PhoneBookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    val press = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().pressScale(press).clip(shape).border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .clickable(press, LocalIndication.current) { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }
            .testTag("book-row-${book.id}").padding(start = AppSpacing.m, end = AppSpacing.xs, top = AppSpacing.m, bottom = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) Box(Modifier.padding(end = AppSpacing.xs)) { BookCheckbox(book, selected, callbacks) }
        BookCover(book, THUMB)
        Spacer(Modifier.width(AppSpacing.m))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(book.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(lengthLine(book), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            KnownShare(book, labelled = true)
        }
        Spacer(Modifier.width(AppSpacing.s))
        ProgressRing(book, SMALL_RING)
        BookMenu(book, callbacks, selecting)
    }
}

private val PHONE_CONTINUE_COVER = 56.dp

/** The most used tags the Filters sheet offers as chips; the rest are behind Filter by tags. */
private const val SHEET_TAGS = 6
