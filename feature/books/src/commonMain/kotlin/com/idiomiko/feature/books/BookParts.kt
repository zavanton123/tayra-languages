package com.idiomiko.feature.books

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.ui.components.BOOK_COVER_RATIO
import com.idiomiko.core.ui.components.BookCover as DrawnCover
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.TagSize
import com.idiomiko.core.ui.components.TagStyle
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * A book's drawn cover: a gradient picked from its title, so a book keeps its look wherever it shows and
 * books side by side differ, with the title on it; a cover from [LARGE_BOOK_COVER] wide also names the language.
 */
@Composable
internal fun BookCover(book: BookListItem, width: Dp) =
    DrawnCover(book.title, width, LanguageCodes.codeFor(book.languageName)?.uppercase() ?: book.languageName.take(2).uppercase())

/** How far through the book the reader is, as a ring with the share in it; a finished book is a green tick. */
@Composable
internal fun ProgressRing(book: BookListItem, size: Dp = RING) {
    val colors = MaterialTheme.colorScheme
    val percent = book.progressPercent
    val finished = book.readingStatus == ReadingStatus.FINISHED
    Box(Modifier.size(size).semantics { contentDescription = if (finished) tr("Finished") else "$percent%" }, contentAlignment = Alignment.Center) {
        if (finished) {
            Box(Modifier.fillMaxSize().clip(AppShapes.circle).background(AppAccents.green))
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(size * 0.45f))
            return@Box
        }
        val track = colors.surfaceContainerHighest
        val fill = colors.primary
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.width * RING_STROKE
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
            if (percent > 0) drawArc(fill, -90f, 360f * percent / 100f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
        }
        Text(
            "$percent%",
            style = AppText.sectionTitle.copy(fontSize = (size.value * 0.25f).sp, lineHeight = (size.value * 0.3f).sp),
            color = if (percent == 0) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Where the reader is in a book: the page under way, Finished, or Not started. */
@Composable
internal fun StatusPill(book: BookListItem) {
    when (book.readingStatus) {
        ReadingStatus.READING -> Tag(tr("Page {0} of {1}", book.currentPage, book.pageCount), MaterialTheme.colorScheme.primary, size = TagSize.Medium)
        ReadingStatus.FINISHED -> Tag(tr("Finished"), AppAccents.green, size = TagSize.Medium)
        ReadingStatus.NOT_STARTED -> Tag(tr("Not started"), size = TagSize.Medium, style = TagStyle.Neutral)
    }
}

/** The colour of a known share: green while the book reads well, orange once it is hard. */
internal fun knownColor(percent: Int): Color = if (knownLevel(percent) == KnownLevel.COMFORTABLE) AppAccents.green else AppAccents.orange

/** What a low known share means for the reader, or null while the book reads well. */
internal fun knownHint(percent: Int): String? = when (knownLevel(percent)) {
    KnownLevel.COMFORTABLE -> null
    KnownLevel.HARD -> tr("hard for you")
    KnownLevel.TOO_HARD -> tr("too hard now")
}

/**
 * The share of a book's words the reader knows, in green, or in orange with what it means when low; "…" before
 * it is counted. [labelled] says "known" after it, where no column header names it.
 */
@Composable
internal fun KnownShare(book: BookListItem, modifier: Modifier = Modifier, labelled: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val percent = book.masteryPercent
    Row(modifier.testTag("known-${book.id}"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        if (percent == null) {
            Text("…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            return@Row
        }
        Text(if (labelled) tr("{0}% known", percent) else "$percent%", style = AppText.labelStrong, color = knownColor(percent), maxLines = 1, softWrap = false)
        knownHint(percent)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** A labelled share above its bar: the label on the left, the figure on the right in [valueColor]. */
@Composable
internal fun LabelledBar(label: String, percent: Int?, color: Color, valueColor: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(percent?.let { "$it%" } ?: "…", style = AppText.labelStrong, color = valueColor, maxLines = 1, softWrap = false)
        }
        ProgressTrack((percent ?: 0) / 100f, color, Modifier.fillMaxWidth(), height = AppSizes.trackThick)
    }
}

/** A book's length: its words and the minutes they take at the app's reading pace, then its tags as small chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookLength(book: BookListItem, tags: Boolean = true, maxTags: Int = 2) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), itemVerticalAlignment = Alignment.CenterVertically) {
        Text(lengthLine(book), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        if (tags) {
            (book.tags.take(maxTags) + listOfNotNull(if (book.tags.size > maxTags) "+${book.tags.size - maxTags}" else null)).forEach { tag ->
                Tag(tag, size = TagSize.Small, style = TagStyle.Neutral, shape = AppShapes.small)
            }
        }
    }
}

/** "1,223 words · ~21 min". */
internal fun lengthLine(book: BookListItem): String = words(book.wordCount) + " · " + tr("~{0} min", book.readingMinutes)

internal val THUMB = 44.dp
internal val RING = 42.dp
internal const val COVER_RATIO = BOOK_COVER_RATIO
private const val RING_STROKE = 0.11f
