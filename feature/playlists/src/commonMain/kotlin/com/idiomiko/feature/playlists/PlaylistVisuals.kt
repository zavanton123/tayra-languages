package com.idiomiko.feature.playlists

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.playlists.PlaylistTextKind
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.coverGradient
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.math.roundToLong
import kotlin.time.Duration

/** The colours a playlist's cover can have, by the index a playlist keeps. */
internal val coverColors: List<Color> = listOf(AppAccents.blue, AppAccents.green, AppAccents.purple, AppAccents.orange, AppAccents.pink, AppAccents.teal)

internal fun coverColor(index: Int): Color = coverColors[index.mod(coverColors.size)]

/** A playlist's cover: headphones on a wash of its colour, [size] across. */
@Composable
internal fun CoverTile(cover: Int, size: Dp, modifier: Modifier = Modifier, icon: ImageVector = AppIcons.Headphones) {
    val tint = coverColor(cover)
    Box(modifier.size(size).clip(AppShapes.panel).background(tint.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = AppAccents.onWash(tint), modifier = Modifier.size(size / 2))
    }
}

/**
 * A playlist's cover: the drawn covers of its first four texts in a square of four, [size] across, with
 * [titled] tiles naming their texts. A playlist of fewer texts fills the rest with shades of its own colour;
 * an empty one is its colour with a music icon.
 */
@Composable
internal fun CoverMosaic(playlist: Playlist, size: Dp, shape: Shape, modifier: Modifier = Modifier, titled: Boolean = false) {
    val tint = coverColor(playlist.cover)
    if (playlist.items.isEmpty()) {
        Box(
            modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(tint, tint.copy(alpha = AppAlpha.muted)))).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.QueueMusic, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(size * EMPTY_ICON_SHARE))
        }
        return
    }
    val texts = playlist.items.take(MOSAIC_TILES)
    Column(
        modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surface).clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(MOSAIC_GAP),
    ) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(MOSAIC_GAP)) {
                repeat(2) { column ->
                    val index = row * 2 + column
                    val text = texts.getOrNull(index)
                    val tile = Modifier.weight(1f).fillMaxSize()
                    if (text != null) {
                        val (start, end) = coverGradient(text.title)
                        Box(tile.drawBehind { drawRect(Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(this.size.width * TILE_LIGHT_X, this.size.height))) }, contentAlignment = Alignment.BottomStart) {
                            if (titled) {
                                Text(
                                    text.title.inLearnedScript(),
                                    Modifier.padding(horizontal = AppSpacing.xs + AppSpacing.xxs, vertical = AppSpacing.xs + AppSpacing.xxs),
                                    style = AppText.badgeBold.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * TILE_TYPE, lineHeight = MaterialTheme.typography.labelSmall.fontSize * TILE_TYPE * TILE_LINE_HEIGHT),
                                    color = AppAccents.onAccent,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        // The places of texts it lacks, lighter one after another.
                        Box(tile.background(tint.copy(alpha = FILL_SHADES[(index - texts.size).coerceIn(0, FILL_SHADES.lastIndex)])))
                    }
                }
            }
        }
    }
}

/** The bars that mark the playlist being played; dimmed while it is held. */
@Composable
internal fun Equaliser(playing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val label = tr("Now playing")
    val shade = if (playing) color else color.copy(alpha = AppAlpha.half)
    Canvas(modifier.size(EQUALISER).semantics { contentDescription = label }) {
        val bar = size.width / 5
        EQUALISER_BARS.forEachIndexed { i, height ->
            val h = size.height * height
            drawRoundRect(shade, topLeft = Offset(bar * 2 * i, size.height - h), size = Size(bar, h), cornerRadius = CornerRadius(bar / 3))
        }
    }
}

/** "4 lessons, 1 book": how many texts of each kind the playlist holds. */
internal fun kindsSummary(playlist: Playlist): String {
    val counts = playlist.items.groupingBy { it.kind }.eachCount()
    return listOf(PlaylistTextKind.LESSON, PlaylistTextKind.BOOK, PlaylistTextKind.NEWS).mapNotNull { kind ->
        counts[kind]?.let { n ->
            when (kind) {
                PlaylistTextKind.LESSON -> trPlural(n, "{0} lesson", "{0} lessons")
                PlaylistTextKind.BOOK -> trPlural(n, "{0} book", "{0} books")
                PlaylistTextKind.NEWS -> trPlural(n, "{0} news article", "{0} news articles")
            }
        }
    }.joinToString(", ")
}

/** How far listening has got as a line: green once every text was heard. */
@Composable
internal fun playlistProgressColor(playlist: Playlist): Color = if (playlist.isComplete) AppAccents.green else MaterialTheme.colorScheme.primary

private const val MOSAIC_TILES = 4
private val MOSAIC_GAP = 1.5.dp
private const val EMPTY_ICON_SHARE = 0.45f
/** A tile's title, as a share of the smallest label: small enough for a word to fit across a tile. */
private const val TILE_TYPE = 0.85f
private const val TILE_LINE_HEIGHT = 1.1f
/** Where a tile's gradient ends across, as on a drawn cover: mostly downward. */
private const val TILE_LIGHT_X = 0.36f
private val FILL_SHADES = listOf(AppAlpha.strong, AppAlpha.muted, AppAlpha.line)
private val EQUALISER = 16.dp
private val EQUALISER_BARS = listOf(0.45f, 1f, 0.65f)

internal val PlaylistTextKind.icon: ImageVector
    get() = when (this) {
        PlaylistTextKind.BOOK -> AppIcons.MenuBook
        PlaylistTextKind.LESSON -> AppIcons.School
        PlaylistTextKind.NEWS -> AppIcons.Newspaper
    }

internal val PlaylistTextKind.label: String
    get() = when (this) {
        PlaylistTextKind.BOOK -> tr("Book")
        PlaylistTextKind.LESSON -> tr("Lesson")
        PlaylistTextKind.NEWS -> tr("News article")
    }

/** An estimate of listening time, rounded up to whole minutes: "4 min", "1 h 8 min". */
internal fun listeningTime(duration: Duration): String {
    val minutes = ((duration.inWholeSeconds + 59) / 60).coerceAtLeast(1)
    return if (minutes < 60) tr("{0} min", minutes) else tr("{0} h {1} min", minutes / 60, minutes % 60)
}

/** A place in a text or a sentence as a clock reads it: 1:05, or 1:02:05 past an hour. */
internal fun clockTime(duration: Duration): String {
    val seconds = (duration.inWholeMilliseconds / 1000.0).roundToLong().coerceAtLeast(0)
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}

internal fun textCount(n: Int) = trPlural(n, "{0} text", "{0} texts")

/** A playlist's length as a player counts it: "3 tracks". */
internal fun trackCount(n: Int) = trPlural(n, "{0} track", "{0} tracks")
