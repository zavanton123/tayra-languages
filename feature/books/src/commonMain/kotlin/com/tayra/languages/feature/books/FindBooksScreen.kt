package com.tayra.languages.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel

/** Free books to add to the library, found in online libraries and judged by the words the reader knows. */
@Composable
fun FindBooksScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: FindBooksViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is FindBooksEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Find books"), onNavigate = onNavigate, onBack = onBack, section = NavSection.BOOKS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        FindBooksContent(
            state,
            FindBooksActions(
                onSource = viewModel::setSource,
                onQuery = viewModel::setQuery,
                onDifficulty = viewModel::setDifficulty,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                onCheck = { viewModel.check(it.book) },
                onAdd = { viewModel.add(it.book) },
                onRead = viewModel::read,
            ),
            Modifier.padding(padding),
        )
    }
}

internal class FindBooksActions(
    val onSource: (BookSource) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onDifficulty: (Difficulty?) -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCheck: (FoundBookItem) -> Unit = {},
    val onAdd: (FoundBookItem) -> Unit = {},
    val onRead: (FoundBookItem) -> Unit = {},
)

@Composable
internal fun FindBooksContent(state: FindBooksUiState, actions: FindBooksActions, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val list = rememberLazyListState()
    // The next page comes when the end of the list is near.
    LaunchedEffect(list, state.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onLoadMore() }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ScrollList(
            Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
        ) {
            item { Header(state, compact) }
            item { Controls(state, actions, compact) }
            when {
                state.loading -> item { Centered { CircularProgressIndicator() } }
                state.error != null -> item { ErrorBox(state, actions) }
                state.shown.isEmpty() -> item { EmptyBox(state, actions) }
                else -> items(state.shown.size, key = { "${state.shown[it].book.source}-${state.shown[it].book.id}" }) { i ->
                    FoundBookCard(state.shown[i], actions, compact, Modifier.padding(bottom = 12.dp))
                }
            }
            if (state.loadingMore) item { Centered { CircularProgressIndicator(Modifier.size(28.dp)) } }
            if (!state.loading && state.error == null) item { SourceNote(state.source) }
        }
    }
}

@Composable
private fun Header(state: FindBooksUiState, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!compact) Text(tr("Find books"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (state.languageName.isEmpty()) tr("Free books from online libraries, with how much of each you already know.")
            else tr("Free books in {0} from online libraries, with how much of each you already know.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(state: FindBooksUiState, actions: FindBooksActions, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val sources = BookSource.entries
        SingleChoiceSegmentedButtonRow(Modifier.then(if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 460.dp))) {
            sources.forEachIndexed { i, source ->
                SegmentedButton(
                    selected = state.source == source,
                    onClick = { actions.onSource(source) },
                    shape = SegmentedButtonDefaults.itemShape(i, sources.size),
                    modifier = Modifier.testTag("source-${source.name}"),
                ) { Text(source.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        SearchBox(state.query, actions.onQuery, Modifier.fillMaxWidth(), placeholder = tr("Search titles and authors"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<Difficulty?>(null) + Difficulty.entries).forEach { difficulty ->
                FilterChip(
                    selected = state.difficulty == difficulty,
                    onClick = { actions.onDifficulty(difficulty) },
                    label = { Text(difficulty?.label ?: tr("All levels")) },
                    leadingIcon = difficulty?.let { { Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(it.color)) } },
                    modifier = Modifier.testTag("difficulty-${difficulty?.name ?: "ALL"}"),
                )
            }
        }
    }
}

@Composable
private fun FoundBookCard(item: FoundBookItem, actions: FindBooksActions, compact: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val book = item.book
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape).padding(16.dp).testTag("found-${book.id}"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!compact) {
            Box(Modifier.size(56.dp, 76.dp).clip(RoundedCornerShape(6.dp)).background((item.difficulty?.color ?: colors.primary).copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = item.difficulty?.color ?: colors.primary, modifier = Modifier.size(28.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val byline = listOfNotNull(book.authors.joinToString(", ").takeIf { it.isNotEmpty() }, book.level).joinToString(" · ")
            if (byline.isNotEmpty()) Text(byline, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.description.isNotEmpty()) Text(book.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            DifficultyLine(item, actions)
            item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            if (compact) {
                Spacer(Modifier.height(4.dp))
                BookAction(item, actions, Modifier.fillMaxWidth())
            }
        }
        if (!compact) BookAction(item, actions, Modifier.align(Alignment.CenterVertically).widthIn(min = 160.dp))
    }
}

@Composable
private fun DifficultyLine(item: FoundBookItem, actions: FindBooksActions) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item.difficulty?.let { DifficultyTag(it) }
        when (val check = item.check) {
            CoverageCheck.Checking -> {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(tr("Checking the words…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            is CoverageCheck.Checked -> Text(
                tr("You know {0}% of the words", check.coverage.percent),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag("coverage-${item.book.id}"),
            )
            is CoverageCheck.Failed -> Text(check.message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CoverageCheck.NotChecked -> if (item.book.source == BookSource.GDL) {
                TextButton(onClick = { actions.onCheck(item) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp).testTag("check-${item.book.id}")) {
                    Text(tr("Check the words"))
                }
            }
        }
    }
}

@Composable
private fun DifficultyTag(difficulty: Difficulty) {
    Text(
        difficulty.label,
        Modifier.clip(RoundedCornerShape(50)).background(difficulty.color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge,
        color = difficulty.color,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BookAction(item: FoundBookItem, actions: FindBooksActions, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    when {
        item.libraryId != null -> OutlinedButton(onClick = { actions.onRead(item) }, shape = shape, modifier = modifier.testTag("read-${item.book.id}")) {
            Text(tr("Read"), fontWeight = FontWeight.SemiBold)
        }
        else -> Button(onClick = { actions.onAdd(item) }, enabled = !item.adding, shape = shape, modifier = modifier.testTag("add-${item.book.id}")) {
            if (item.adding) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                Spacer(Modifier.width(8.dp))
                Text(tr("Adding…"), fontWeight = FontWeight.SemiBold)
            } else {
                Text(tr("Add to library"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ErrorBox(state: FindBooksUiState, actions: FindBooksActions) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Could not reach {0}.", state.source.libraryName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
        OutlinedButton(onClick = actions.onRetry, shape = RoundedCornerShape(10.dp)) { Text(tr("Try again")) }
    }
}

@Composable
private fun EmptyBox(state: FindBooksUiState, actions: FindBooksActions) {
    val other = BookSource.entries.first { it != state.source }
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when {
                state.languageName.isEmpty() -> tr("Choose a language to learn to see its books.")
                state.books.isNotEmpty() -> tr("None of these books is at this level yet.")
                state.query.isNotBlank() -> tr("No books match your search.")
                else -> tr("{0} has no books in {1}.", state.source.libraryName, languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (state.books.isEmpty() && state.languageName.isNotEmpty()) {
            OutlinedButton(onClick = { actions.onSource(other) }, shape = RoundedCornerShape(10.dp)) { Text(tr("Look in {0}", other.libraryName)) }
        }
    }
}

@Composable
private fun SourceNote(source: BookSource) {
    Text(
        when (source) {
            BookSource.GUTENBERG -> tr("Classics from Project Gutenberg, free of copyright in the United States. Each book is judged by its first pages.")
            BookSource.GDL -> tr("Children's books from the Global Digital Library, graded by level and free to share under open licences.")
        },
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { content() }
}

private val BookSource.label: String
    get() = when (this) {
        BookSource.GUTENBERG -> tr("Classics")
        BookSource.GDL -> tr("Easy readers")
    }

private val BookSource.libraryName: String
    get() = when (this) {
        BookSource.GUTENBERG -> "Project Gutenberg"
        BookSource.GDL -> "Global Digital Library"
    }

private val Difficulty.label: String
    get() = when (this) {
        Difficulty.EASY -> tr("Easy")
        Difficulty.MEDIUM -> tr("Medium")
        Difficulty.HARD -> tr("Hard")
    }

private val Difficulty.color: Color
    get() = when (this) {
        Difficulty.EASY -> Color(0xFF2E9D57)
        Difficulty.MEDIUM -> Color(0xFFE08A1E)
        Difficulty.HARD -> Color(0xFFD64545)
    }
