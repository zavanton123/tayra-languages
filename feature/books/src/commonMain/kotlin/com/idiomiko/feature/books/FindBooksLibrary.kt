package com.idiomiko.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.ui.components.ActiveFilterSummary
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.CoverageLine
import com.idiomiko.core.ui.components.Crumb
import com.idiomiko.core.ui.components.DifficultyFilter
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.JourneyIllustration
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PlaceholderCards
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.ToggleButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.itemMotion
import com.idiomiko.core.ui.components.label
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * One library's own books: its search, the level shown, and its pages as the list nears its end, each book a card
 * whose tap adds it or reads it. A library that cannot be reached says so kindly and offers the others.
 */
@Composable
internal fun LibraryPage(state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    val library = state.library
    val list = rememberLazyListState()
    // The next page comes when the end of the list is near.
    LaunchedEffect(list, library.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onLoadMore() }
    }
    ScrollList(
        Modifier.widthIn(max = AppSizes.contentWidth).fillMaxSize(),
        state = list,
        contentPadding = PaddingValues(
            start = if (compact) AppSpacing.l else AppSpacing.xxxl,
            end = if (compact) AppSpacing.l else AppSpacing.xxxl + AppSizes.scrollbarRoom,
            top = if (compact) AppSpacing.l else AppSpacing.xxl,
            bottom = AppSpacing.xxl,
        ),
    ) {
        item {
            Column(Modifier.padding(bottom = AppSpacing.xl)) {
                ScreenHeader(
                    library.source.libraryName,
                    library.source.blurb,
                    crumbs = listOf(Crumb(tr("Find books"), actions.onBack), Crumb(library.source.label)),
                    titleOnCompact = true,
                )
            }
        }
        // A library that cannot be reached has nothing to search or filter.
        if (library.error != null) {
            item { LibraryBreak(state, library.source, actions, compact) }
            return@ScrollList
        }
        item { LibraryControls(library, actions, compact) }
        when {
            library.loading -> item { PlaceholderCards(4, thumbnail = true) }
            library.shown.isEmpty() -> item { EmptyBox(state, actions) }
            else -> items(library.shown.size, key = { "${library.shown[it].book.source}-${library.shown[it].book.id}" }) { i ->
                Column(itemMotion()) { FoundBookCard(library.shown[i], actions, compact, Modifier.padding(bottom = AppSpacing.m)) }
            }
        }
        if (library.loadingMore) item { Box(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxl), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Medium) } }
        if (!library.loading) item { SourceNote(library.source) }
    }
}

@Composable
private fun LibraryControls(library: LibraryUiState, actions: FindBooksActions, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        SearchField(library.query, actions.onLibraryQuery, tr("Search titles and authors"), Modifier.fillMaxWidth().testTag("library-search"))
        // On a phone the level waits in a sheet behind one button, so the books show at once.
        var sheet by remember { mutableStateOf(false) }
        if (compact) {
            ToggleButton(
                tr("Filters"),
                active = library.difficulty != null,
                onClick = { sheet = true },
                modifier = Modifier.testTag("find-books-filters"),
                icon = AppIcons.Tune,
                badge = if (library.difficulty != null) 1 else 0,
                contentDescription = if (library.difficulty != null) trPlural(1, "{0} filter on", "{0} filters on") else null,
            )
        } else {
            DifficultyFilter(library.difficulty, actions.onDifficulty)
        }
        library.difficulty?.takeIf { !library.loading }?.let { difficulty ->
            ActiveFilterSummary(
                "${difficulty.label} · ${trPlural(library.shown.size, "{0} book", "{0} books")}",
                tr("Clear filters"),
                onClear = { actions.onDifficulty(null) },
                onEdit = if (compact) ({ sheet = true }) else null,
            )
        }
        if (sheet) {
            AppBottomSheet(onDismissRequest = { sheet = false }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    Text(tr("Filters"), style = AppText.itemTitle)
                    DifficultyFilter(library.difficulty, actions.onDifficulty)
                    PrimaryButton(trPlural(library.shown.size, "Show {0} book", "Show {0} books"), onClick = { sheet = false }, modifier = Modifier.fillMaxWidth().testTag("find-books-filters-done"))
                }
            }
        }
    }
}

@Composable
private fun FoundBookCard(item: FoundBookItem, actions: FindBooksActions, compact: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val book = item.book
    // The whole card does what its button says: reads a book already in the library, or adds it.
    Row(
        modifier.fillMaxWidth().cardSurface().clickable(enabled = !item.adding) { if (item.libraryId != null) actions.onRead(item) else actions.onAdd(item) }
            .padding(AppSpacing.l).testTag("found-${book.id}"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        if (!compact) BookCover(book.title, CARD_COVER, code = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(book.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val byline = listOfNotNull(book.authors.joinToString(", ").inLearnedScript().takeIf { it.isNotEmpty() }, book.level).joinToString(" · ")
            if (byline.isNotEmpty()) Text(byline, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.description.isNotEmpty()) Text(book.description.inLearnedScript(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // On a phone the quiet action ends the metadata line rather than taking a line of its own.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CoverageLine(item.difficulty, item.check, tag = book.id, onCheck = if (book.source.judgedOnSight) null else ({ actions.onCheck(item) })) }
                if (compact) BookAction(item, actions, Modifier)
            }
            item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        }
        if (!compact) BookAction(item, actions, Modifier.align(Alignment.CenterVertically).widthIn(min = ACTION_WIDTH))
    }
}

/** The card's one action, quiet as on every card: read a book already added, or add it, trying again after a failed download. */
@Composable
private fun BookAction(item: FoundBookItem, actions: FindBooksActions, modifier: Modifier) {
    if (item.libraryId != null) {
        QuietButton(tr("Read"), onClick = { actions.onRead(item) }, modifier = modifier.testTag("read-${item.book.id}"), trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
    } else {
        QuietButton(
            when { item.adding -> tr("Adding…"); item.error != null -> tr("Try again"); else -> tr("Add to library") },
            onClick = { actions.onAdd(item) },
            modifier = modifier.testTag("add-${item.book.id}"),
            loading = item.adding,
            icon = Icons.Default.Add,
        )
    }
}

/**
 * A library that is down or out of reach: the mascot, that it is taking a break, that the books already added from it
 * still open, the libraries to try meanwhile, and a way to ask it again with when it was last asked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryBreak(state: FindBooksUiState, source: BookSource, actions: FindBooksActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val info = state.libraries[source]
    val others = state.sources.filter { it != source && state.libraries[it]?.status == LibraryStatus.AVAILABLE }
    val text: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m), horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
            val align = if (compact) TextAlign.Center else TextAlign.Start
            Text(tr("{0} is taking a break", source.label), style = AppText.headline, textAlign = align)
            Text(
                if (info?.status == LibraryStatus.DOWN_FOR_MAINTENANCE) info.message ?: tr("Down for maintenance") else tr("It can't be reached right now. Check your connection, or try again in a moment."),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = align,
            )
            Text(tr("Books you already added from it still open in your library."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = align)
            if (others.isNotEmpty()) {
                Text(tr("Meanwhile, try another library:"), style = AppText.labelStrong, textAlign = align)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, if (compact) Alignment.CenterHorizontally else Alignment.Start), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    others.forEach { other ->
                        SecondaryButton(other.label, onClick = { actions.onOpenLibrary(other) }, modifier = Modifier.testTag("try-${other.name}"), size = ButtonSize.Compact, icon = other.icon)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l, if (compact) Alignment.CenterHorizontally else Alignment.Start), verticalArrangement = Arrangement.spacedBy(AppSpacing.s), itemVerticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(tr("Try again"), onClick = actions.onRetryLibrary, modifier = Modifier.testTag("library-retry"))
                info?.checkedAt?.let { Text(tr("Checked {0}", it.relativeTo()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            }
        }
    }
    if (compact) {
        Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xl).testTag("library-break"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            JourneyIllustration(Modifier.width(BREAK_MASCOT_COMPACT))
            text()
        }
    } else {
        Row(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl).testTag("library-break"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            JourneyIllustration(Modifier.width(BREAK_MASCOT))
            Box(Modifier.weight(1f)) { text() }
        }
    }
}

@Composable
private fun EmptyBox(state: FindBooksUiState, actions: FindBooksActions) {
    val library = state.library
    val other = state.sources.ifEmpty { BookSource.entries.toList() }.firstOrNull { it != library.source }
    EmptyState(
        AppIcons.MenuBook,
        when {
            state.languageName.isEmpty() -> tr("Choose a language to learn to see its books.")
            library.books.isNotEmpty() -> tr("None of these books is at this level yet.")
            library.query.isNotBlank() -> tr("No books match your search.")
            else -> tr("{0} has no books in {1}.", library.source.libraryName, languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))
        },
        action = if (library.books.isEmpty() && state.languageName.isNotEmpty() && other != null) ({ SecondaryButton(tr("Look in {0}", other.libraryName), onClick = { actions.onOpenLibrary(other) }) }) else null,
    )
}

@Composable
private fun SourceNote(source: BookSource) {
    Text(
        when (source) {
            BookSource.GUTENBERG -> tr("Classics from Project Gutenberg, free of copyright in the United States. Each book is judged by its first pages.")
            BookSource.WIKISOURCE -> tr("Texts transcribed by Wikisource's volunteers, free to share under CC BY-SA. Without a search, the most read works of last month. Each is judged by its first chapter.")
            BookSource.GDL -> tr("Children's books from the Global Digital Library, graded by level and free to share under open licences.")
            BookSource.STORYBOOKS -> tr("Short illustrated stories for children from Global Storybooks, a few sentences a page, free to share under CC BY. The easiest reading to start with.")
            BookSource.STORYWEAVER -> tr("Picture books from Pratham Books' StoryWeaver, graded from level 1 to 4 and free to share under CC BY 4.0. Each book is judged when you ask, as its pages are downloaded.")
            BookSource.WOLNE_LEKTURY -> tr("Polish school readings and classics from Wolne Lektury, most in the public domain. Without a search, its books for children and young people.")
            BookSource.AOZORA -> tr("Japanese works out of copyright from Aozora Bunko, without their furigana. Without a search, the most read works for children.")
            BookSource.LITTERATURBANKEN -> tr("Swedish classics from Litteraturbanken, free to share under CC0, most in the spelling of their time. Without a search, the most read.")
            BookSource.BEN_YEHUDA -> tr("Hebrew works in the public domain from Project Ben-Yehuda, without vowel points. Without a search, its fables, then its stories.")
            BookSource.ADL -> tr("Older Danish literature from the Royal Danish Library's Arkiv for Dansk Litteratur, free to share non-commercially, in the spelling of its time. Without a search, H. C. Andersen's fairy tales.")
            BookSource.GANJOOR -> tr("Classical Persian poems and stories from Ganjoor, each one text, without short vowel marks. Without a search, the stories of Saadi's Golestan.")
            BookSource.ELG -> tr("French classics from Ebooks libres et gratuits, free for non-commercial use. Without a search, its books for the young.")
            BookSource.TEXTGRID -> tr("German literature from the TextGrid Digitale Bibliothek, each tale, poem or novel a text of its own, free to share under CC BY 3.0. Without a search, the Grimms' tales and legends.")
            BookSource.LIBER_LIBER -> tr("Italian classics from Liber Liber, most free to share non-commercially. Without a search, its books for young people.")
            BookSource.ELEJANDRIA -> tr("Spanish books from Elejandría, in the public domain or under open licences. Without a search, its children's stories.")
            BookSource.ADAMASTOR -> tr("Portuguese classics from Projecto Adamastor, revised into today's spelling and free to share under CC BY-SA 4.0.")
        },
        Modifier.fillMaxWidth().padding(top = AppSpacing.s, bottom = AppSpacing.l),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private val CARD_COVER = 56.dp
private val ACTION_WIDTH = 160.dp
private val BREAK_MASCOT = 220.dp
private val BREAK_MASCOT_COMPACT = 160.dp
