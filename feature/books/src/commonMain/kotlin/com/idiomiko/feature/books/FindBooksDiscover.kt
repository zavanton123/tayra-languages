package com.idiomiko.feature.books

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.Discovery
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.domain.books.discover.Ranked
import com.idiomiko.core.domain.books.discover.Shelf
import com.idiomiko.core.domain.books.discover.ShelfBooks
import com.idiomiko.core.domain.books.discover.Topic
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.components.Crumb
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.FindBooksIllustration
import com.idiomiko.core.ui.components.JourneyIllustration
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.color
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.placeholder
import com.idiomiko.core.ui.components.PlaceholderLine
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.math.roundToInt

/**
 * The page of every library: the search across them all with its topics, the How hard? scale, the libraries' cards,
 * then the shelves picked for the reader, or the books a search or topic found. A card picked narrows the books to
 * its library's, the further pages of its grid loading at the end.
 */
@Composable
internal fun DiscoverPage(state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit, knownWords: Int?) {
    val compact = LocalWindowWidth.current.isCompact
    PageList(compact) {
        item { Hero(state, actions, knownWords, compact) }
        item { HowHard(state.minKnown, actions.onMinKnown, compact) }
        if (state.sources.isNotEmpty() && !state.allDown) item { BrowseLibraries(state, actions, compact) }
        when {
            state.languageName.isEmpty() -> item { EmptyState(AppIcons.MenuBook, tr("Choose a language to learn to see its books.")) }
            state.sources.isEmpty() -> item { EmptyState(AppIcons.MenuBook, tr("None of the libraries has books in {0} yet.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))) }
            state.allDown -> item { OfflineCard(state, actions, compact) }
            else -> {
                item { Books(state, actions, open, compact) }
                if (state.selected != null && !state.selectedDown && !state.loading) item { LibraryFooter(state, actions, paging = state.narrowed || state.oneGrid) }
            }
        }
        // With every library down, their cards follow the card that says so.
        if (state.sources.isNotEmpty() && state.allDown) item { BrowseLibraries(state, actions, compact) }
    }
}

/** The books under the libraries' cards: the shelves, or a search's or topic's, or one library's; that library's chip first. */
@Composable
private fun Books(state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit, compact: Boolean) {
    val selected = state.selected
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (selected != null) Box(Modifier.padding(top = AppSpacing.s)) { LibraryChip(selected, actions.onClearLibrary) }
        when {
            selected != null && state.selectedDown -> LibraryBreak(state, selected, actions, compact)
            state.narrowed -> Results(state, actions, open, compact)
            state.loading -> Column(verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) { ShelfSkeleton(compact); if (!state.oneGrid) ShelfSkeleton(compact) }
            selected != null && state.books.isEmpty() -> EmptyState(AppIcons.MenuBook, tr("{0} has no books in {1}.", selected.libraryName, languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)))
            selected != null && state.oneGrid -> LibraryBooks(state, selected, actions, open, compact)
            else -> Shelves(state, actions, open, compact)
        }
    }
}

/** One shelf in full, under the How hard? scale that sets it; a library's further pages load at its end. */
@Composable
internal fun ShelfPage(state: FindBooksUiState, shelf: Shelf, actions: FindBooksActions, open: (FoundBookItem) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    val books = remember(state, shelf) { state.shelf(shelf) }
    PageList(compact) {
        item { ScreenHeader(shelf.title, shelf.subtitle(state), crumbs = listOf(Crumb(tr("Find books"), actions.onBack), Crumb(shelf.title)), titleOnCompact = true) }
        item { HowHard(state.minKnown, actions.onMinKnown, compact) }
        state.selected?.let { item { LibraryChip(it, actions.onClearLibrary) } }
        item {
            when {
                books.books.isNotEmpty() -> BookGrid(books.books, compact, actions, open) { item -> meta(item, byAuthor = shelf == Shelf.CLASSICS_WITHIN_REACH, hard = books.books.indexOf(item) >= books.hardFrom) }
                state.judgingPage || state.waiting -> ShelfSkeleton(compact, title = false)
                else -> NoMatch(state, actions, harder = 0, levelOnly = true)
            }
        }
        if (state.selected != null) item { LibraryFooter(state, actions, paging = true) }
    }
}

/** The page's column: centred on a wide window, with the same gap on either side measured to the scrollbar. */
@Composable
private fun PageList(compact: Boolean, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    ScrollList(
        Modifier.widthIn(max = AppSizes.pageWidth + AppSpacing.xxxl * 2 + AppSizes.scrollbarRoom).fillMaxSize(),
        contentPadding = PaddingValues(
            start = if (compact) AppSpacing.l else AppSpacing.xxxl,
            end = if (compact) AppSpacing.l else AppSpacing.xxxl + AppSizes.scrollbarRoom,
            top = if (compact) AppSpacing.s else AppSpacing.xxl,
            bottom = AppSpacing.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.l),
        content = content,
    )
}

/**
 * The navy card the page opens with: what it is for and the words the books are picked for, the one search over every
 * library, and the topics to narrow them to; on a wide window the mascot with its map stands beside them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(state: FindBooksUiState, actions: FindBooksActions, knownWords: Int?, compact: Boolean) {
    val language = state.languageName.takeIf { it.isNotEmpty() }?.let { languageInSentence(it, LanguageCase.PREPOSITIONAL) }
    val subtitle = when {
        language == null -> tr("Free books from online libraries, picked for the words you know.")
        knownWords == null || knownWords == 0 -> tr("Free {0} books, picked for the words you know.", language)
        else -> trPlural(knownWords, "Free {1} books, picked for the {2} word you know.", "Free {1} books, picked for the {2} words you know.", language, formatCount(knownWords))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(if (compact) HERO_RADIUS_COMPACT else HERO_RADIUS)).background(Brush.linearGradient(listOf(HERO_START, HERO_END))).testTag("find-hero")) {
        val picture = !compact && maxWidth >= HERO_PICTURE_FROM
        Column(
            Modifier.fillMaxWidth().padding(
                start = if (compact) AppSpacing.xl else AppSpacing.xxxl,
                end = if (picture) HERO_PICTURE + AppSpacing.xxxl * 2 else if (compact) AppSpacing.xl else AppSpacing.xxxl,
                top = if (compact) AppSpacing.xl else HERO_PADDING_TOP,
                bottom = if (compact) AppSpacing.xl else HERO_PADDING_TOP,
            ),
        ) {
            Text(tr("Find something to read"), style = (if (compact) AppText.headline else AppText.pageTitle).copy(letterSpacing = (-0.4).sp), color = HERO_TEXT)
            Text(subtitle, Modifier.padding(top = AppSpacing.xs).testTag("hero-subtitle"), style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = HERO_TEXT.copy(alpha = HERO_MUTED))
            HeroSearch(state.query, actions, Modifier.padding(top = if (compact) AppSpacing.l else AppSpacing.xl), compact)
            val topics = state.topics
            if (topics.isNotEmpty()) {
                val chips: @Composable () -> Unit = { topics.forEach { TopicChip(it, selected = state.topic == it) { actions.onTopic(it) } } }
                if (compact) {
                    Row(Modifier.padding(top = AppSpacing.m).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) { chips() }
                } else {
                    FlowRow(Modifier.padding(top = AppSpacing.l), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { chips() }
                }
            }
        }
        if (picture) {
            FindBooksIllustration(Modifier.align(Alignment.BottomEnd).padding(end = AppSpacing.huge, bottom = AppSpacing.m).width(HERO_MASCOT).testTag("hero-mascot"))
        }
    }
}

/** The search box over every library: the query, a way to clear it, and Search, which the keyboard's search key presses too. */
@Composable
private fun HeroSearch(query: String, actions: FindBooksActions, modifier: Modifier, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().height(if (compact) AppSizes.touch else HERO_SEARCH_HEIGHT).clip(RoundedCornerShape(HERO_SEARCH_RADIUS)).background(colors.surface)
            .padding(start = AppSpacing.l, end = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
        Spacer(Modifier.width(AppSpacing.m))
        BasicTextField(
            value = query,
            onValueChange = actions.onQuery,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { actions.onSearch() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag("hero-search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            if (compact) tr("Search every library") else tr("Search titles and authors in every library"),
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) AppIconButton(Icons.Default.Close, contentDescription = tr("Clear search"), onClick = { actions.onQuery("") }, size = IconButtonSize.Small)
        QuietButton(tr("Search"), onClick = actions.onSearch, modifier = Modifier.testTag("hero-search-button"))
    }
}

@Composable
private fun TopicChip(topic: Topic, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(AppShapes.pill).background(if (selected) HERO_TEXT else HERO_TEXT.copy(alpha = AppAlpha.soft)).clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs + AppSpacing.xxs).testTag("topic-${topic.name}"),
    ) {
        Text(topic.label, style = AppText.labelStrong, color = if (selected) HERO_END else HERO_TEXT, maxLines = 1, softWrap = false)
    }
}

/**
 * How hard the books may be: a scale from Easy to Hard over the least share of their words the reader knows, its
 * stretches in the colours of the levels, and the share chosen beside it.
 */
@Composable
private fun HowHard(minKnown: Int, onChange: (Int) -> Unit, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val threshold: @Composable () -> Unit = {
        Text(
            if (minKnown == 0) tr("All levels") else tr("{0}%+ known", minKnown),
            Modifier.testTag("how-hard-threshold"),
            style = AppText.labelBold,
            color = if (minKnown == 0) colors.onSurfaceVariant else knownTint(minKnown),
            maxLines = 1,
            softWrap = false,
        )
    }
    val hard: @Composable () -> Unit = { Text(tr("Hard"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    val easy: @Composable () -> Unit = { Text(tr("Easy"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    val title: @Composable () -> Unit = { Text(tr("How hard?"), style = AppText.labelBold) }
    if (compact) {
        Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("how-hard"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { title() }; threshold() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) { easy(); KnownScale(minKnown, onChange, Modifier.weight(1f)); hard() }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m).testTag("how-hard"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            title()
            easy()
            KnownScale(minKnown, onChange, Modifier.weight(1f))
            hard()
            Box(Modifier.widthIn(min = THRESHOLD_WIDTH), contentAlignment = Alignment.CenterEnd) { threshold() }
        }
    }
}

/**
 * The How hard? scale, easiest first: its stops evenly along a track from the most known to any, each stretch coloured
 * by the level of the stop it starts from (green for easy, orange for medium, red for hard), and a ringed thumb on the
 * stop chosen. A tap or a drag picks the nearest.
 */
@Composable
private fun KnownScale(value: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    val stops = Discovery.KNOWN_STOPS.reversed()
    val at = stops.indexOf(value).coerceAtLeast(0)
    val change by rememberUpdatedState(onChange)
    val ring = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.surface
    BoxWithConstraints(modifier.height(SCALE_HEIGHT)) {
        val inset = SCALE_THUMB / 2
        val track = maxWidth - SCALE_THUMB
        fun nearest(x: Float, width: Float): Int {
            val edge = inset.value * (width / maxWidth.value)
            return (((x - edge) / (width - 2 * edge)) * (stops.size - 1)).roundToInt().coerceIn(0, stops.size - 1)
        }
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { change(stops[nearest(it.x, size.width.toFloat())]) } }
                .pointerInput(Unit) { detectHorizontalDragGestures { pointer, _ -> change(stops[nearest(pointer.position.x, size.width.toFloat())]) } },
        ) {
            val left = inset.toPx()
            val width = size.width - 2 * left
            val y = size.height / 2
            val thick = SCALE_TRACK.toPx()
            fun x(i: Int) = left + width * i / (stops.size - 1)
            val track = Path().apply { addRoundRect(RoundRect(left, y - thick / 2, left + width, y + thick / 2, CornerRadius(thick / 2))) }
            clipPath(track) {
                // Each stretch overlaps the next by a pixel, so no seam shows between two of one colour.
                for (i in 1 until stops.size) drawRect(knownTint(stops[i - 1]), topLeft = Offset(x(i - 1), y - thick / 2), size = Size(x(i) - x(i - 1) + 1f, thick))
            }
            val r = SCALE_THUMB.toPx() / 2
            drawCircle(fill, radius = r, center = Offset(x(at), y))
            drawCircle(ring, radius = r - SCALE_RING.toPx() / 2, center = Offset(x(at), y), style = Stroke(SCALE_RING.toPx()))
        }
        // Each stop is a target of its own, for screen readers and keyboards.
        stops.forEachIndexed { i, stop ->
            Box(
                Modifier.offset(x = track * i / (stops.size - 1)).size(SCALE_THUMB, SCALE_HEIGHT).clip(AppShapes.circle).clickable { change(stop) }
                    .semantics { contentDescription = if (stop == 0) tr("All levels") else tr("{0}%+ known", stop) }.testTag("known-stop-$stop"),
            )
        }
    }
}

/** The colour of a share known, by the level it makes a text: green from 95%, orange from 85%, red below. */
private fun knownTint(percent: Int): Color = com.idiomiko.core.domain.books.discover.TextCoverage(100, percent).difficulty.color

/** Easy wins and Classics within reach, each while it has books or its books are still being judged; a note when both are empty. */
@Composable
private fun Shelves(state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit, compact: Boolean) {
    val shelves = remember(state) { Shelf.entries.associateWith { state.shelf(it) } }
    val pending = state.judging || state.waiting
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
        for ((shelf, books) in shelves) {
            when {
                books.books.isNotEmpty() -> ShelfRow(shelf, books, state, actions, open, compact)
                pending -> ShelfSkeleton(compact)
            }
        }
        if (!pending && shelves.values.all { it.books.isEmpty() }) NoMatch(state, actions, harder = 0, levelOnly = true)
    }
}

@Composable
private fun ShelfRow(shelf: Shelf, books: ShelfBooks<FoundBookItem>, state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit, compact: Boolean) {
    Column(Modifier.fillMaxWidth().testTag("shelf-${shelf.name}"), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SectionTitle(shelf.title, shelf.subtitle(state), compact) {
            LinkText(tr("See all"), onClick = { actions.onShowAll(shelf) }, modifier = Modifier.testTag("see-all-${shelf.name}"), style = AppText.labelBold)
        }
        val meta: @Composable (FoundBookItem) -> String = { item -> meta(item, byAuthor = shelf == Shelf.CLASSICS_WITHIN_REACH, hard = books.books.indexOf(item) >= books.hardFrom) }
        if (compact) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                items(books.books.take(SHELF_MAX), key = { "${it.book.source}-${it.book.id}" }) { item -> CoverTile(item, PHONE_COVER, meta(item), actions, open) }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fit = ((maxWidth + COVER_GAP) / (DESKTOP_COVER + COVER_GAP)).toInt().coerceAtLeast(1)
                Row(horizontalArrangement = Arrangement.spacedBy(COVER_GAP)) {
                    books.books.take(fit).forEach { item -> CoverTile(item, DESKTOP_COVER, meta(item), actions, open) }
                }
            }
        }
    }
}

/** A section's title with what it holds after it, and its action at the end of the line. */
@Composable
private fun SectionTitle(title: String, subtitle: String?, compact: Boolean, action: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    if (compact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = AppText.sectionTitle)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            action()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(title, Modifier.alignByBaseline(), style = AppText.headline)
                if (subtitle != null) Text(subtitle, Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            action()
        }
    }
}

/** Books as a grid of covers, as many to a row as the width takes; two to a row on a phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookGrid(books: List<FoundBookItem>, compact: Boolean, actions: FindBooksActions, open: (FoundBookItem) -> Unit, meta: @Composable (FoundBookItem) -> String) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("book-grid")) {
        val gap = if (compact) AppSpacing.m else COVER_GAP
        val columns = if (compact) PHONE_COLUMNS else ((maxWidth + gap) / (DESKTOP_COVER + gap)).toInt().coerceAtLeast(1)
        val width = if (compact) (maxWidth - gap * (columns - 1)) / columns else DESKTOP_COVER
        FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl), maxItemsInEachRow = columns) {
            books.forEach { CoverTile(it, width, meta(it), actions, open) }
        }
    }
}

/**
 * A book on a shelf: its drawn cover with the share known in its level's colour at the top left and the way to add
 * it (or read it, once added) at the top right, then its title and where it is from. A tap opens its card.
 */
@Composable
private fun CoverTile(item: FoundBookItem, width: Dp, meta: String, actions: FindBooksActions, open: (FoundBookItem) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val book = item.book
    Column(Modifier.width(width).clip(AppShapes.compact).clickable { open(item) }.testTag("cover-${book.id}")) {
        Box {
            BookCover(book.title, width, code = null, typeShare = SHELF_TYPE)
            KnownBadge(item, Modifier.align(Alignment.TopStart).padding(AppSpacing.m))
            AddBadge(item, actions, Modifier.align(Alignment.TopEnd).padding(AppSpacing.s))
        }
        Text(book.title.inLearnedScript(), Modifier.padding(top = AppSpacing.m), style = AppText.itemTitleBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(meta, Modifier.padding(top = AppSpacing.xxs), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        item.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** The share of a book's words the reader knows, in its level's colour on a white tab; a spinner while it is judged. */
@Composable
private fun KnownBadge(item: FoundBookItem, modifier: Modifier) {
    val coverage = item.coverage
    if (coverage == null && item.check != CoverageCheck.Checking) return
    Box(modifier.clip(AppShapes.compact).background(BADGE_BACKGROUND).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs).testTag("known-${item.book.id}")) {
        if (coverage == null) AppSpinner(Modifier.size(AppSizes.iconTiny), size = SpinnerSize.Small)
        else Text("${coverage.percent}%", style = AppText.badgeBold, color = coverage.difficulty.color, maxLines = 1, softWrap = false)
    }
}

/** Adds the book from its cover, without opening its card; once added, opens it to read. */
@Composable
private fun AddBadge(item: FoundBookItem, actions: FindBooksActions, modifier: Modifier) {
    val added = item.libraryId != null
    val label = if (added) tr("Read") else tr("Add to library")
    Box(
        modifier.size(ADD_BADGE).clip(AppShapes.circle).background(BADGE_BACKGROUND)
            .clickable(enabled = !item.adding) { if (added) actions.onRead(item) else actions.onAdd(item) }
            .semantics { contentDescription = label }.testTag(if (added) "read-${item.book.id}" else "add-${item.book.id}"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            item.adding -> AppSpinner(Modifier.size(AppSizes.iconTiny), size = SpinnerSize.Small)
            added -> Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconSmall))
            else -> Icon(Icons.Default.Add, contentDescription = null, tint = HERO_START, modifier = Modifier.size(AppSizes.iconSmall))
        }
    }
}

/** What a cover says under its title: the library and the minutes the book takes, or its author; a classic past reach is marked hard. */
@Composable
private fun meta(item: FoundBookItem, byAuthor: Boolean, hard: Boolean = false): String {
    val author = item.book.authors.firstOrNull()?.inLearnedScript()?.takeIf { it.isNotBlank() }
    val parts = if (byAuthor) listOf(author ?: item.book.source.label)
    else listOfNotNull(item.book.source.label, item.coverage?.minutes?.let { tr("~{0} min", it) } ?: author ?: item.book.level)
    return (parts + listOfNotNull(tr("hard").takeIf { hard })).joinToString(" · ")
}

/** The books a search or a topic found in every library, those the reader knows enough of first. */
@Composable
private fun Results(state: FindBooksUiState, actions: FindBooksActions, open: (FoundBookItem) -> Unit, compact: Boolean) {
    val results = remember(state) { state.results }
    val pending = state.waiting || state.judging
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m), modifier = Modifier.testTag("results")) {
        val title = if (state.searched.isNotBlank()) tr("Results for “{0}”", state.searched) else state.topic?.label.orEmpty()
        SectionTitle(title, count(results), compact) {
            LinkText(if (state.searched.isNotBlank()) tr("Clear search") else tr("Clear filters"), onClick = actions.onClearSearch, modifier = Modifier.testTag("clear-search"), style = AppText.labelBold)
        }
        when {
            results.books.isNotEmpty() -> BookGrid(results.books, compact, actions, open) { meta(it, byAuthor = false) }
            pending -> ShelfSkeleton(compact, title = false)
            else -> NoMatch(state, actions, results.harder, levelOnly = false)
        }
    }
}

/**
 * The books of a library the shelves would mostly leave out, children's books or books judged on request, as one
 * grid: those the reader knows enough of, the best known first, then those not judged yet.
 */
@Composable
private fun LibraryBooks(state: FindBooksUiState, source: BookSource, actions: FindBooksActions, open: (FoundBookItem) -> Unit, compact: Boolean) {
    val results = remember(state) { state.results }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m), modifier = Modifier.testTag("library-books")) {
        SectionTitle(source.libraryName, count(results, tr("best known first")), compact)
        when {
            results.books.isNotEmpty() -> BookGrid(results.books, compact, actions, open) { meta(it, byAuthor = false) }
            state.waiting || state.judging -> ShelfSkeleton(compact, title = false)
            else -> NoMatch(state, actions, results.harder, levelOnly = true)
        }
    }
}

/** How many books a grid holds, and how many harder ones it leaves out. */
@Composable
private fun count(results: Ranked<FoundBookItem>, vararg more: String): String = (
    listOfNotNull(
        trPlural(results.books.size, "{0} book", "{0} books"),
        results.harder.takeIf { it > 0 }?.let { trPlural(it, "{0} harder book left out", "{0} harder books left out") },
    ) + more
).joinToString(" · ")

/**
 * Nothing to show: no book matches the search or topic, or none is as easy as asked. The mascot, and the ways out:
 * clearing the search or topic, and letting in books of every level.
 */
@Composable
private fun NoMatch(state: FindBooksUiState, actions: FindBooksActions, harder: Int, levelOnly: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl).testTag("no-match"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        JourneyIllustration(Modifier.width(NO_MATCH_MASCOT))
        Text(
            when {
                levelOnly -> tr("No book here is this easy for you yet.")
                state.searched.isNotBlank() -> tr("No books match your search.")
                else -> tr("No books of this kind at this level yet.")
            },
            style = AppText.sectionTitle,
            textAlign = TextAlign.Center,
        )
        Text(
            if (levelOnly) tr("Let in harder books, or look in a library below.") else tr("Try another search, clear it, or let in harder books."),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            if (!levelOnly) SecondaryButton(if (state.searched.isNotBlank()) tr("Clear search") else tr("Clear filters"), onClick = actions.onClearSearch, modifier = Modifier.testTag("no-match-clear"))
            if (state.minKnown > 0 && (levelOnly || harder > 0)) {
                SecondaryButton(trPlural(harder, "Show {0} harder book", "Show {0} harder books").takeIf { harder > 0 } ?: tr("All levels"), onClick = { actions.onMinKnown(0) }, modifier = Modifier.testTag("no-match-levels"))
            }
        }
    }
}

/** Every library failed: the reader is offline, or all of them are down. Books already added still open. */
@Composable
private fun OfflineCard(state: FindBooksUiState, actions: FindBooksActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(if (compact) AppSpacing.xl else AppSpacing.xxxl).testTag("all-down"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        JourneyIllustration(Modifier.width(if (compact) NO_MATCH_MASCOT else OFFLINE_MASCOT))
        Text(tr("The libraries can't be reached right now"), style = AppText.headline, textAlign = TextAlign.Center)
        Text(tr("Check your connection and try again. The books you already added still open in your library."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        PrimaryButton(tr("Try again"), onClick = actions.onRetry, modifier = Modifier.testTag("retry-all"))
        state.checkedAt?.let { Text(tr("Checked {0}", it.relativeTo()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    }
}

/** A shelf still to come: its title and a row of covers in the placeholder grey. */
@Composable
private fun ShelfSkeleton(compact: Boolean, title: Boolean = true) {
    val loading = tr("Loading...")
    val cover = if (compact) PHONE_COVER else DESKTOP_COVER
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = loading }.testTag("shelf-skeleton"), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (title) PlaceholderLine(if (compact) 0.5f else 0.25f, height = AppSizes.titleLine)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = if (compact) AppSpacing.m else COVER_GAP
            val fit = ((maxWidth + gap) / (cover + gap)).toInt().coerceAtLeast(1) + if (compact) 1 else 0
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(fit) {
                    Column(Modifier.width(cover), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        Box(Modifier.size(cover, cover * COVER_RATIO).placeholder(AppShapes.compact))
                        PlaceholderLine(0.8f)
                        PlaceholderLine(0.5f)
                    }
                }
            }
        }
    }
}

/**
 * Every library of the language as a card: what it holds and whether it can be reached now. A tap narrows the books
 * below to the library's, and a second tap lets it go.
 */
@Composable
private fun BrowseLibraries(state: FindBooksUiState, actions: FindBooksActions, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SectionTitle(tr("Browse a library"), null, compact)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = if (compact) AppSpacing.m else AppSpacing.l
            val columns = if (compact) PHONE_COLUMNS else ((maxWidth + gap) / (LIBRARY_CARD_MIN + gap)).toInt().coerceIn(1, LIBRARY_COLUMNS).coerceAtMost(state.sources.size.coerceAtLeast(1))
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                // The libraries that can be reached come first; one that is down waits at the end.
                state.sources.sortedBy { state.libraries[it]?.status?.isDown == true }.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { LibraryCard(it, state.libraries[it]?.status ?: LibraryStatus.CHECKING, selected = it == state.selected, { actions.onLibrary(it) }, Modifier.weight(1f).fillMaxHeight()) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** A library's card; the one picked has a ring in the primary colour, a tint of it, and a check. */
@Composable
private fun LibraryCard(source: BookSource, status: LibraryStatus, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.large
    val background = when {
        selected -> colors.primary.copy(alpha = AppAlpha.wash).compositeOver(colors.surface)
        status.isDown -> lerp(colors.surface, AppAccents.red, DOWN_WASH)
        else -> colors.surface
    }
    Column(
        modifier.clip(shape).background(background).border(if (selected) AppBorders.selected() else AppBorders.hairline(), shape)
            .selectable(selected = selected, onClick = onClick).padding(AppSpacing.l).testTag("library-${source.name}"),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(source.icon, size = LIBRARY_ICON, tint = source.tint)
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(Modifier.size(LIBRARY_CHECK).clip(AppShapes.circle).background(colors.primary).testTag("library-check-${source.name}"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(AppSizes.iconTiny))
                }
            }
        }
        Text(source.label, Modifier.padding(top = AppSpacing.m), style = AppText.itemTitleBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(source.blurb, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f).height(AppSpacing.m))
        val (text, tint) = when (status) {
            LibraryStatus.CHECKING -> tr("Checking…") to colors.onSurfaceVariant
            LibraryStatus.AVAILABLE -> tr("Available") to AppAccents.green
            LibraryStatus.DOWN_FOR_MAINTENANCE -> tr("Down for maintenance") to AppAccents.red
            LibraryStatus.UNREACHABLE -> tr("Can't reach right now") to AppAccents.orange
        }
        Row(Modifier.padding(top = AppSpacing.m).testTag("library-status-${source.name}"), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Box(Modifier.padding(top = AppSpacing.xs).size(AppSizes.dotSmall).clip(AppShapes.circle).background(tint))
            Text(text, style = AppText.badgeBold, color = AppAccents.onWash(tint), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal val Topic.label: String
    get() = when (this) {
        Topic.SHORT_STORIES -> tr("Short stories")
        Topic.CHILDRENS_BOOKS -> tr("Children's books")
        Topic.POEMS -> tr("Poems")
        Topic.CLASSIC_NOVELS -> tr("Classic novels")
    }

internal val Shelf.title: String
    get() = when (this) {
        Shelf.EASY_WINS -> tr("Easy wins")
        Shelf.CLASSICS_WITHIN_REACH -> tr("Classics within reach")
    }

/** What a shelf holds at the level the reader asked for. */
internal fun Shelf.subtitle(state: FindBooksUiState): String {
    val known = state.minKnown
    val language = state.languageName.takeIf { it.isNotEmpty() }?.let { languageInSentence(it, LanguageCase.PREPOSITIONAL) }.orEmpty()
    return when (this) {
        Shelf.EASY_WINS -> when {
            known >= Discovery.DEFAULT_MIN_KNOWN -> tr("Short, and you know almost every word")
            known > 0 -> tr("Short, and you know {0}% of the words or more", known)
            else -> tr("Short books of every level")
        }
        Shelf.CLASSICS_WITHIN_REACH ->
            if (known > Discovery.REACH) tr("Famous {0} books at {1}–{2}% known", language, known - Discovery.REACH, known - 1)
            else tr("Famous {0} books, those you know best first", language)
    }
}

/** A line on what a library holds. */
internal val BookSource.blurb: String
    get() = when (this) {
        BookSource.GUTENBERG -> tr("Classic novels")
        BookSource.WIKISOURCE -> tr("Poems, stories")
        BookSource.GDL -> tr("Graded, short")
        BookSource.STORYBOOKS -> tr("Picture stories")
        BookSource.STORYWEAVER -> tr("Picture books")
        BookSource.WOLNE_LEKTURY -> tr("School readings")
        BookSource.ADL -> tr("Older literature")
        BookSource.GANJOOR -> tr("Classical poems")
        BookSource.TEXTGRID -> tr("Tales, poems, novels")
        BookSource.ADAMASTOR, BookSource.AOZORA, BookSource.BEN_YEHUDA -> tr("Literature")
        BookSource.LITTERATURBANKEN, BookSource.ELG, BookSource.LIBER_LIBER, BookSource.ELEJANDRIA -> tr("Classics")
    }

internal val BookSource.icon: ImageVector
    get() = when (this) {
        BookSource.GUTENBERG -> AppIcons.BookClosed
        BookSource.WIKISOURCE -> AppIcons.Description
        BookSource.GDL -> AppIcons.School
        BookSource.STORYBOOKS -> AppIcons.Lightbulb
        BookSource.STORYWEAVER -> AppIcons.Palette
        BookSource.ADAMASTOR -> AppIcons.Explore
        else -> AppIcons.MenuBook
    }

private val BookSource.tint: Color get() = AppAccents.sequence[ordinal % AppAccents.sequence.size]

/** The hero's navy, from the top left to the bottom right, under white type. */
private val HERO_START = AppAccents.heroStart
private val HERO_END = AppAccents.heroEnd
private val HERO_TEXT = AppAccents.onAccent
private const val HERO_MUTED = 0.82f
private val HERO_RADIUS = 26.dp
private val HERO_RADIUS_COMPACT = 20.dp
private val HERO_PADDING_TOP = 28.dp
private val HERO_SEARCH_HEIGHT = 52.dp
private val HERO_SEARCH_RADIUS = 14.dp
private val HERO_PICTURE = 270.dp
/** The mascot's width: about as tall as the hero's words, so it sits inside the hero whole. */
private val HERO_MASCOT = 210.dp
private val HERO_PICTURE_FROM = 760.dp

/** The white tab under a cover's share and its add button, a touch see-through. */
private val BADGE_BACKGROUND = AppAccents.onAccent
private val ADD_BADGE = 30.dp

private val SCALE_HEIGHT = 28.dp
private val SCALE_THUMB = 20.dp
private val SCALE_TRACK = 8.dp
private val SCALE_RING = 3.dp
private val THRESHOLD_WIDTH = 96.dp

private val SECTION_GAP = 26.dp

/** A shelf cover's title, small enough to leave the cover to its colours and badges. */
private const val SHELF_TYPE = 0.105f
private val DESKTOP_COVER = 170.dp
private val PHONE_COVER = 128.dp
private val COVER_GAP = 18.dp
private const val PHONE_COLUMNS = 2
private const val SHELF_MAX = 12

private val LIBRARY_CARD_MIN = 150.dp
private const val LIBRARY_COLUMNS = 6
private const val LIBRARY_ICON = 40
private val LIBRARY_CHECK = 22.dp
private const val DOWN_WASH = 0.05f

private val NO_MATCH_MASCOT = 150.dp
private val OFFLINE_MASCOT = 200.dp
