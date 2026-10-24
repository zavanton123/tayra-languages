package com.tayra.languages.feature.books

import com.tayra.languages.core.ui.components.inLearnedScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.TagFilterSheet
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.ToggleButton
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

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
 * The books on a phone: the one being read as a card to go on with, two compact browsing rows,
 * and the library as a dense reading queue.
 */
@Composable
internal fun PhoneBooksContent(state: BooksUiState, callbacks: BooksCallbacks, selecting: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    // Selection replaces the continue card, so a visible continued book is never repeated below it.
    val continued = state.continueWith?.takeIf { !state.archived && !showChecks }
    val books = continued?.let { shown -> state.filteredBooks.filter { it.id != shown.id } } ?: state.filteredBooks
    var sheet by remember { mutableStateOf<PhoneBooksSheet?>(null) }
    ScrollList(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = AppSpacing.l, vertical = AppSpacing.m)) {
        if (state.archived && state.books.isEmpty()) {
            item { EmptyArchive() }
            return@ScrollList
        } else if (state.archived) {
            item {
                Text(tr("Books you have set aside. Unarchive one to keep reading it."), Modifier.padding(bottom = AppSpacing.l), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        } else if (state.books.isEmpty()) {
            item { EmptyLibrary(state, callbacks.onNewBook, callbacks.onDownloadBooks) }
            return@ScrollList
        } else {
            continued?.let { book -> item { PhoneContinueCard(book) { callbacks.onOpen(book) } } }
        }
        item { SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"), Modifier.fillMaxWidth().padding(bottom = AppSpacing.m)) }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                SelectButton(
                    state.progress.tabLabel,
                    onClick = { sheet = PhoneBooksSheet.PROGRESS },
                    modifier = Modifier.weight(1f).testTag("phone-progress"),
                    active = state.progress != ProgressFilter.ALL,
                )
                ToggleButton(
                    tr("Filters"),
                    active = state.tags.isNotEmpty(),
                    onClick = { sheet = PhoneBooksSheet.FILTERS },
                    modifier = Modifier.weight(1f).testTag("phone-filters"),
                    badge = if (state.tags.isEmpty()) 0 else 1,
                    enabled = state.availableTags.isNotEmpty(),
                )
                ToggleButton(tr("Sort"), active = false, onClick = { sheet = PhoneBooksSheet.SORT }, modifier = Modifier.weight(1f).testTag("phone-sort"))
            }
        }
        item {
            if (showChecks) {
                BooksSelectionBar(state, callbacks, onDone)
            } else {
                Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.archived) tr("Archive") else tr("Your library"), Modifier.weight(1f), style = AppText.sectionTitle)
                    Text(tr(state.sort.label), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        if (books.isEmpty()) {
            item { NoBooks(state.archived, callbacks.onNewBook) }
        } else {
            items(books.size, key = { books[it].id }) { i -> Column(itemMotion()) {
                PhoneBookCard(books[i], books[i].id in state.selected, showChecks, callbacks)
                Spacer(Modifier.height(AppSpacing.m))
            }}
        }
        item { Footer(state) }
    }

    when (sheet) {
        PhoneBooksSheet.PROGRESS -> PickerSheet(
            title = tr("Progress"),
            options = ProgressFilter.entries,
            selected = state.progress,
            label = { tr(it.label) },
            onDismiss = { sheet = null },
            onPick = { callbacks.onProgress(it); sheet = null },
        )
        PhoneBooksSheet.SORT -> PickerSheet(
            title = tr("Sort"),
            options = BookSort.entries,
            selected = state.sort,
            label = { tr(it.label) },
            onDismiss = { sheet = null },
            onPick = { callbacks.onSort(it); sheet = null },
        )
        PhoneBooksSheet.FILTERS -> TagFilterSheet(
            state.availableTags,
            state.tags,
            state.matchAllTags,
            onApply = { tags, all -> callbacks.onTags(tags, all); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> {}
    }
}

private enum class PhoneBooksSheet { PROGRESS, FILTERS, SORT }

/** The book being read most recently: its cover, where the reader is, the two bars and the way back in. */
@Composable
private fun PhoneContinueCard(book: BookListItem, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TintedSurface(colors.primary, Modifier.padding(bottom = AppSpacing.l), shape = AppShapes.large, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            BookCover(book, 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Continue reading"), style = AppText.smallTitle, color = colors.primary)
                Text(book.title.inLearnedScript(), style = AppText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(tr("Page {0} of {1} · {2}", book.currentPage, book.pageCount, words(book.wordCount)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            LabelledBar(tr("{0}% read", book.progressPercent), book.progressPercent, AppAccents.blue, Modifier.weight(1f))
            LabelledBar(book.masteryPercent?.let { tr("{0}% known", it) } ?: "…", book.masteryPercent, AppAccents.green, Modifier.weight(1f))
        }
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("continue-reading"), size = ButtonSize.Large)
    }
}

/** A compact metric with its label above the bar, so translated text can grow without clipping. */
@Composable
private fun LabelledBar(label: String, percent: Int?, color: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        ProgressTrack((percent ?: 0) / 100f, color, Modifier.fillMaxWidth(), height = AppSizes.trackThick)
    }
}

/** A book as a dense queue row: cover, title, one useful metadata line, and an exceptional status. */
@Composable
private fun PhoneBookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().pressScale(press).clip(shape).border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .clickable(press, LocalIndication.current) { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }
            .testTag("book-row-${book.id}").padding(start = AppSpacing.m, end = AppSpacing.xs, top = AppSpacing.m, bottom = AppSpacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) Box(Modifier.padding(end = AppSpacing.xs)) { BookCheckbox(book, selected, callbacks) }
            BookCover(book, 44.dp)
            Spacer(Modifier.width(AppSpacing.m))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(book.title.inLearnedScript(), Modifier.weight(1f), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (book.readingStatus == ReadingStatus.FINISHED) {
                        Text(tr("Finished"), style = AppText.label, color = AppAccents.green, maxLines = 1)
                    }
                }
                Text(bookMetadata(book), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildList {
                        add(tr("{0}% read", book.progressPercent))
                        book.masteryPercent?.let { add(tr("{0}% known", it)) }
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BookMenu(book, callbacks)
        }
    }
}

private fun bookMetadata(book: BookListItem): String = buildList {
    add(words(book.wordCount))
    book.sourceUri?.takeIf { it.isNotBlank() }?.let { add(compactSource(it)) }
    add(book.lastOpened?.let { tr("Opened {0}", it.relativeTo()) } ?: tr("Not opened yet"))
}.joinToString(" · ")

/** A readable host for web sources and a filename for imported local sources. */
private fun compactSource(source: String): String {
    val afterScheme = source.substringAfter("://", source)
    val host = afterScheme.substringBefore('/').removePrefix("www.")
    val filename = source.substringAfterLast('/').substringAfterLast('\\')
    return host.takeIf { it.isNotBlank() } ?: filename.ifBlank { source }
}
