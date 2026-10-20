package com.tayra.languages.feature.books

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
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
import com.tayra.languages.core.ui.components.TagFilterSheet
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.ToggleButton
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
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
 * The books on a phone: the one being read as a card to go on with, the progress tabs, the
 * search with a sort picker and a tag filter sheet, and the library as cards.
 */
@Composable
internal fun PhoneBooksContent(state: BooksUiState, callbacks: BooksCallbacks, selecting: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    // The book in the continue card is not listed again under it, unless books are being chosen.
    val books = state.continueWith?.takeIf { !state.archived && !showChecks }?.let { shown -> state.filteredBooks.filter { it.id != shown.id } } ?: state.filteredBooks
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
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
            state.continueWith?.let { book -> item { PhoneContinueCard(book) { callbacks.onOpen(book) } } }
        }
        item { ProgressTabs(state.counts, state.progress, callbacks.onProgress, compact = true) }
        item { SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"), Modifier.fillMaxWidth().padding(bottom = AppSpacing.m)) }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                // The sort's label is the longer, so it gets the wider button.
                ToggleButton(tr(state.sort.label), active = false, onClick = { sorting = true }, modifier = Modifier.weight(1.45f).testTag("phone-sort"), icon = AppIcons.SwapVert, trailingIcon = Icons.Default.ArrowDropDown)
                ToggleButton(tr("Filters"), active = state.tags.isNotEmpty(), onClick = { filtering = true }, modifier = Modifier.weight(1f).testTag("phone-filters"), icon = AppIcons.Tune, badge = state.tags.size)
            }
        }
        item {
            if (showChecks) {
                BooksSelectionBar(state, callbacks, onDone)
            } else {
                // The tabs above count the books, so the heading stands alone.
                Text(if (state.archived) tr("Archive") else tr("Your library"), Modifier.padding(bottom = AppSpacing.m), style = AppText.headline)
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
    TintedSurface(colors.primary, Modifier.padding(bottom = AppSpacing.l), shape = AppShapes.large, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            BookCover(book, 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Continue reading"), style = AppText.smallTitle, color = colors.primary)
                Text(book.title, style = AppText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(tr("Page {0} of {1} · {2}", book.currentPage, book.pageCount, words(book.wordCount)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            LabelledBar(tr("{0}% read", book.progressPercent), book.progressPercent, AppAccents.blue)
            LabelledBar(book.masteryPercent?.let { tr("{0}% known", it) } ?: "…", book.masteryPercent, AppAccents.green)
        }
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("continue-reading"), size = ButtonSize.Large)
    }
}

/** A label of a fixed width and the bar it describes, with no figure after the bar. */
@Composable
private fun LabelledBar(label: String, percent: Int?, color: Color) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(label, Modifier.width(PERCENT_LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
        ProgressTrack((percent ?: 0) / 100f, color, Modifier.weight(1f), height = AppSizes.trackThick)
    }
}

/** A book as a card: cover, title, its length and when it was opened, its status and tags, and the two bars. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhoneBookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().pressScale(press).clip(shape).border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .clickable(press, LocalIndication.current) { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }.padding(start = AppSpacing.l, end = AppSpacing.xs, top = AppSpacing.l, bottom = AppSpacing.l),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (selecting) Box(Modifier.padding(end = AppSpacing.xs)) { BookCheckbox(book, selected, callbacks) }
            BookCover(book, 60.dp)
            Spacer(Modifier.width(AppSpacing.l))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(book.title, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Length and when it was opened share a line, and the status its tags', so more books fit on a screen.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    val meta = MaterialTheme.typography.bodyMedium
                    Text(words(book.wordCount), style = meta, color = colors.onSurfaceVariant)
                    Text("·", style = meta, color = colors.onSurfaceVariant)
                    Text(book.lastOpened?.let { tr("Opened {0}", it.relativeTo()) } ?: tr("Not opened yet"), style = meta, color = colors.onSurfaceVariant, maxLines = 1)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    StatusTag(book.readingStatus, TagSize.Medium)
                    BookTags(book.tags)
                }
            }
            BookMenu(book, callbacks)
        }
        Spacer(Modifier.height(AppSpacing.l))
        // The labels share one width, as wide as the longer of them, so large text never breaks a word and the bars line up.
        val labelStyle = MaterialTheme.typography.bodyMedium
        val measurer = rememberTextMeasurer()
        val labels = listOf(tr("Reading"), tr("Vocabulary"))
        val labelWidth = with(LocalDensity.current) { labels.maxOf { measurer.measure(it, labelStyle).size.width }.toDp() }.coerceAtLeast(BAR_LABEL_WIDTH)
        Column(Modifier.padding(end = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(labels[0], Modifier.width(labelWidth), style = labelStyle, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
                Box(Modifier.weight(1f)) { PercentBar(book.progressPercent, AppAccents.blue) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(labels[1], Modifier.width(labelWidth), style = labelStyle, color = colors.onSurfaceVariant, maxLines = 1, softWrap = false)
                Box(Modifier.weight(1f)) { PercentBar(book.masteryPercent, AppAccents.green) }
            }
        }
    }
}

private val PERCENT_LABEL_WIDTH = 96.dp
private val BAR_LABEL_WIDTH = 84.dp
