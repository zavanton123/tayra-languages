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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.BookListItem
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
 * books side by side differ, with the title on it; a cover from [LARGE_COVER] wide also names the language.
 */
@Composable
internal fun BookCover(book: BookListItem, width: Dp) {
    val gradients = AppAccents.coverGradients
    val (start, end) = gradients[stableHash(book.title).mod(gradients.size)]
    val large = width >= LARGE_COVER
    val shape = RoundedCornerShape(topStart = width * 0.08f, bottomStart = width * 0.08f, topEnd = width * 0.16f, bottomEnd = width * 0.16f)
    val title = book.title.inLearnedScript()
    // The type shrinks until the title's longest word fits a line, so no word is cut in two.
    val room = width.value * (1 - TYPE_START - TYPE_END)
    val longest = title.split(' ').maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
    val typeSize = minOf(width.value * if (large) 0.15f else 0.18f, room / (longest * GLYPH_WIDTH))
    val type = AppText.sectionTitle.copy(
        color = AppAccents.onAccent,
        fontSize = typeSize.sp,
        lineHeight = (typeSize * 1.15f).sp,
        shadow = Shadow(AppAccents.ink.copy(alpha = TYPE_SHADOW), Offset(0f, 1f), 2f),
    )
    // A picture: the title beside it is what is read out.
    Box(
        Modifier.size(width = width, height = width * COVER_RATIO).clearAndSetSemantics {}.clip(shape).drawBehind {
            // Mostly downward and a little to the right, as the light falls on a book standing on a shelf.
            drawRect(Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(size.width * 0.36f, size.height)))
            // The spine.
            drawRect(AppAccents.ink.copy(alpha = SPINE_SHADE), size = Size(size.width * 0.1f, size.height))
        },
    ) {
        val inset = Modifier.fillMaxSize().padding(start = width * TYPE_START, end = width * TYPE_END, top = width * 0.12f, bottom = width * 0.12f)
        if (large) {
            Column(inset, verticalArrangement = Arrangement.SpaceBetween) {
                val code = LanguageCodes.codeFor(book.languageName)?.uppercase() ?: book.languageName.take(2).uppercase()
                Text(code, style = AppText.smallTitle.copy(fontSize = (width.value * 0.15f).sp, color = AppAccents.onAccent.copy(alpha = CODE_ALPHA)), maxLines = 1)
                Text(title, style = type, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(title, inset, style = type, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A string's hash computed the same way on every platform, so a cover looks the same everywhere. */
private fun stableHash(text: String): Int = text.fold(0) { hash, c -> 31 * hash + c.code }

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

/** From this wide a cover names the language too. */
internal val LARGE_COVER = 64.dp
internal val THUMB = 44.dp
internal val RING = 42.dp
private const val COVER_RATIO = 1.38f
private const val RING_STROKE = 0.11f

/** The cover's margins beside its type, as shares of its width: the spine's side is wider. */
private const val TYPE_START = 0.15f
private const val TYPE_END = 0.06f

/** About how wide a bold letter is, in ems. */
private const val GLYPH_WIDTH = 0.58f
private const val SPINE_SHADE = 0.14f
private const val TYPE_SHADOW = 0.15f
private const val CODE_ALPHA = 0.85f
