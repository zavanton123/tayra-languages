package com.idiomiko.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.TableHeader
import com.idiomiko.core.ui.components.TableHeaderCell
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The books as a table: book, progress, words known, when opened, and each book's menu; the count and the way into ticking several below. */
@Composable
internal fun BookTable(state: BooksUiState, books: List<BookListItem>, selecting: Boolean, callbacks: BooksCallbacks, onSelect: (() -> Unit)?) {
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppAlpha.muted)
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).testTag("book-table")) {
        BookTableHeader(state.sort, selecting, callbacks.onSort)
        books.forEach { book ->
            AppDivider(color = line)
            BookTableRow(book, book.id in state.selected, selecting, callbacks)
        }
        AppDivider(color = line)
        LibraryFooter(state, onSelect, Modifier.padding(start = AppSpacing.xl, end = AppSpacing.l, top = AppSpacing.s, bottom = AppSpacing.s))
    }
}

@Composable
private fun BookTableHeader(sort: BookSort, selecting: Boolean, onSort: (BookSort) -> Unit) {
    val style = AppText.overline
    TableHeader(padding = PaddingValues(start = rowStart(selecting), end = AppSpacing.m, top = AppSpacing.m, bottom = AppSpacing.m)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP)) {
            // The checkboxes' column, so the names stay over their columns while books are ticked.
            if (selecting) Spacer(Modifier.width(CHECKBOX))
            TableHeaderCell(tr("Book").uppercase(), Modifier.weight(BOOK_WEIGHT), active = sort == BookSort.TITLE, style = style) { onSort(BookSort.TITLE) }
            TableHeaderCell(tr("Progress").uppercase(), Modifier.weight(PROGRESS_WEIGHT), style = style)
            TableHeaderCell(tr("Words known").uppercase(), Modifier.weight(KNOWN_WEIGHT), active = sort == BookSort.MASTERY, style = style) { onSort(BookSort.MASTERY) }
            TableHeaderCell(tr("Opened").uppercase(), Modifier.weight(OPENED_WEIGHT), active = sort == BookSort.RECENT, style = style) { onSort(BookSort.RECENT) }
            Spacer(Modifier.width(MENU))
        }
    }
}

/**
 * A book as a row of the table; the whole row opens it, or ticks it while books are being ticked. Under the
 * pointer the Opened cell gives way to the quick actions: reading on (or again) and practising its words.
 */
@Composable
private fun BookTableRow(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> colors.primary.copy(alpha = AppAlpha.soft)
        hovered -> colors.primary.copy(alpha = AppAlpha.faint)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background).hoverable(interaction)
            // The row draws its own hover wash, lighter than the indication's.
            .clickable(interaction, indication = null) { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }
            .testTag("book-row-${book.id}")
            .padding(start = rowStart(selecting), end = AppSpacing.m, top = AppSpacing.m, bottom = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
    ) {
        if (selecting) Box(Modifier.width(CHECKBOX)) { BookCheckbox(book, selected, callbacks) }
        Row(Modifier.weight(BOOK_WEIGHT), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            BookCover(book, THUMB)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(book.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BookLength(book)
            }
        }
        Row(Modifier.weight(PROGRESS_WEIGHT), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            ProgressRing(book)
            StatusPill(book)
        }
        KnownShare(book, Modifier.weight(KNOWN_WEIGHT))
        Box(Modifier.weight(OPENED_WEIGHT)) {
            if (hovered && !selecting) QuickActions(book, callbacks)
            else Text(book.lastOpened?.relativeTo() ?: "—", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.width(MENU), contentAlignment = Alignment.Center) { BookMenu(book, callbacks, selecting) }
    }
}

/** A row's quick actions: Continue, Read again or Start, and, for a book opened before, practising its words when there is room. */
@Composable
private fun QuickActions(book: BookListItem, callbacks: BooksCallbacks) {
    val read = when (book.readingStatus) {
        ReadingStatus.READING -> tr("Continue")
        ReadingStatus.FINISHED -> tr("Read again")
        ReadingStatus.NOT_STARTED -> tr("Start")
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val both = book.lastOpened != null && maxWidth >= BOTH_ACTIONS
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            SecondaryButton(read, onClick = { callbacks.onOpen(book) }, modifier = Modifier.testTag("quick-read-${book.id}"), size = ButtonSize.Compact)
            if (both) SecondaryButton(tr("Practice"), onClick = { callbacks.onPractice(book) }, modifier = Modifier.testTag("quick-practice-${book.id}"), size = ButtonSize.Compact)
        }
    }
}

private fun rowStart(selecting: Boolean) = if (selecting) AppSpacing.s else AppSpacing.xl

// Column weights shared by the table header and rows.
private const val BOOK_WEIGHT = 2.4f
private const val PROGRESS_WEIGHT = 1.4f
private const val KNOWN_WEIGHT = 1.1f
private const val OPENED_WEIGHT = 1f
private val COLUMN_GAP = 18.dp
private val MENU = 40.dp
private val CHECKBOX = 40.dp

/** The room the Opened cell needs for both quick actions side by side. */
private val BOTH_ACTIONS = 190.dp
