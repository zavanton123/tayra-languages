package com.tayra.languages.feature.books

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.TagFilterSheet
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes

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
        Spacer(Modifier.width(8.dp))
    }
}

/**
 * The books on a phone: the one being read as a card to go on with, the progress tabs, the
 * search with a sort picker and a tag filter sheet, and the library as cards.
 */
@Composable
internal fun PhoneBooksContent(state: BooksUiState, callbacks: BooksCallbacks, selecting: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val books = state.filteredBooks
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    ScrollList(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        if (state.archived) {
            item {
                Text(tr("Books you have set aside. Unarchive one to keep reading it."), Modifier.padding(bottom = 16.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        } else if (state.books.isEmpty()) {
            item { EmptyLibrary(state, callbacks.onNewBook, callbacks.onDownloadBooks) }
            return@ScrollList
        } else {
            state.continueWith?.let { book -> item { PhoneContinueCard(book) { callbacks.onOpen(book) } } }
        }
        item { ProgressTabs(state.counts, state.progress, callbacks.onProgress, compact = true) }
        item { SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"), Modifier.fillMaxWidth().padding(bottom = 10.dp)) }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // The sort's label is the longer, so it gets the wider button.
                ToolbarButton(AppIcons.SwapVert, tr(state.sort.label), Modifier.weight(1.45f).testTag("phone-sort"), trailing = Icons.Default.ArrowDropDown) { sorting = true }
                ToolbarButton(AppIcons.Tune, tr("Filters"), Modifier.weight(1f).testTag("phone-filters"), badge = state.tags.size, active = state.tags.isNotEmpty()) { filtering = true }
            }
        }
        item {
            if (showChecks) {
                BooksSelectionBar(state, callbacks, onDone)
            } else {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(if (state.archived) tr("Archive") else tr("Your library"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(trPlural(books.size, "{1} book", "{1} books", formatCount(books.size)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
            }
        }
        if (books.isEmpty()) {
            item { NoBooks(state.archived, callbacks.onNewBook) }
        } else {
            items(books.size, key = { books[it].id }) { i ->
                PhoneBookCard(books[i], books[i].id in state.selected, showChecks, callbacks)
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Footer(state) }
    }

    if (sorting) {
        PickerSheet(
            title = tr("Sort"),
            options = BookSort.entries,
            selected = state.sort,
            label = { tr(it.label) },
            onDismiss = { sorting = false },
            onPick = { callbacks.onSort(it); sorting = false },
        )
    }
    if (filtering) {
        TagFilterSheet(state.availableTags, state.tags, state.matchAllTags, onApply = { tags, all -> callbacks.onTags(tags, all); filtering = false }, onDismiss = { filtering = false })
    }
}

/** The book being read most recently: its cover, where the reader is, the two bars and the way back in. */
@Composable
private fun PhoneContinueCard(book: BookListItem, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.large
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(shape).background(colors.primary.copy(alpha = 0.06f)).border(AppBorders.hairline(colors.primary.copy(alpha = 0.16f)), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(book, 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Continue reading"), style = MaterialTheme.typography.titleSmall, color = colors.primary, fontWeight = FontWeight.SemiBold)
                Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tr("Page {0} of {1} · {2}", book.currentPage, book.pageCount, words(book.wordCount)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelledBar(tr("{0}% read", book.progressPercent), book.progressPercent, AppAccents.blue)
            LabelledBar(book.masteryPercent?.let { tr("{0}% known", it) } ?: "…", book.masteryPercent, AppAccents.green)
        }
        PrimaryButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("continue-reading")) {
            Text(tr("Continue"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A label of a fixed width and the bar it describes, with no figure after the bar. */
@Composable
private fun LabelledBar(label: String, percent: Int?, color: Color) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
        ProgressTrack((percent ?: 0) / 100f, color, Modifier.weight(1f), height = 8.dp)
    }
}

/** An outlined control in the toolbar: an icon, a label, and a badge or a trailing icon. */
@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badge: Int = 0,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (active) colors.primary else colors.onSurface
    Row(
        modifier.height(48.dp).clip(AppShapes.control).background(colors.surface)
            .border(if (active) BorderStroke(1.5.dp, colors.primary) else AppBorders.hairline(), AppShapes.control)
            .clickable(onClick = onClick).padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (badge > 0) {
            Text(
                "$badge",
                Modifier.clip(AppShapes.pill).background(colors.primary).padding(horizontal = 8.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (trailing != null) Icon(trailing, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** A book as a card: cover, title, words and status, the two bars, and when it was opened. */
@Composable
private fun PhoneBookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    Column(
        Modifier.fillMaxWidth().clip(shape).border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = 0.05f) else colors.surface)
            .clickable { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }.padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (selecting) Box(Modifier.padding(end = 4.dp)) { BookCheckbox(book, selected, callbacks) }
            BookCover(book, 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row { StatusTag(book.readingStatus) }
                BookTags(book.tags)
            }
            BookMenu(book, callbacks)
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Reading"), Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) { PercentBar(book.progressPercent, AppAccents.blue) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Vocabulary"), Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Box(Modifier.weight(1f)) { PercentBar(book.masteryPercent, AppAccents.green) }
            }
        }
        AppDivider(Modifier.padding(top = 14.dp, bottom = 12.dp, end = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(AppIcons.History, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(book.lastOpened?.let { tr("Opened {0}", it.relativeTo()) } ?: tr("Not opened yet"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}
