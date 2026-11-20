package com.idiomiko.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.JourneyIllustration
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.coroutines.flow.first

/** The library the books are narrowed to, as a chip whose tap lets it go and shows every library's books again. */
@Composable
internal fun LibraryChip(source: BookSource, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(AppShapes.pill).background(colors.primary.copy(alpha = AppAlpha.soft)).clickable(role = Role.Button, onClick = onClear)
            .padding(start = AppSpacing.m, end = AppSpacing.s, top = AppSpacing.xs, bottom = AppSpacing.xs).testTag("library-filter"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(source.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconTiny))
        Text(source.label, Modifier.padding(start = AppSpacing.s, end = AppSpacing.xs), style = AppText.labelBold, color = colors.primary, maxLines = 1)
        Icon(Icons.Default.Close, contentDescription = tr("Show every library"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconTiny))
    }
}

/**
 * The end of a library's books: with [paging], its next page, asked for each time this comes into view and the books
 * above are judged, a button for it while it stays in view, and why it failed if it did; then what the library is.
 */
@Composable
internal fun LibraryFooter(state: FindBooksUiState, actions: FindBooksActions, paging: Boolean) {
    val source = state.selected ?: return
    val more = state.more
    if (paging) {
        val ready by rememberUpdatedState(state.canLoadMore)
        val load by rememberUpdatedState(actions.onLoadMore)
        LaunchedEffect(Unit) {
            snapshotFlow { ready }.first { it }
            load()
        }
    }
    Column(Modifier.fillMaxWidth().testTag("library-footer"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (paging) {
            when {
                more.error != null -> {
                    Text(more.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    QuietButton(tr("Try again"), onClick = actions.onLoadMore, modifier = Modifier.testTag("load-more-retry"))
                }
                more.loading || (state.hasMore && state.judgingPage) -> Box(Modifier.padding(vertical = AppSpacing.l).testTag("loading-more")) { AppSpinner(size = SpinnerSize.Medium) }
                state.hasMore -> QuietButton(tr("Load more"), onClick = actions.onLoadMore, modifier = Modifier.testTag("load-more"))
            }
        }
        SourceNote(source)
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
                        SecondaryButton(other.label, onClick = { actions.onLibrary(other) }, modifier = Modifier.testTag("try-${other.name}"), size = ButtonSize.Compact, icon = other.icon)
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

private val BREAK_MASCOT = 220.dp
private val BREAK_MASCOT_COMPACT = 160.dp
