package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.tayra.languages.core.domain.playlists.PlaylistTextKind
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
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

/** An estimate of listening time, rounded up to whole minutes: "~4 min", "~1 h 8 min". */
internal fun listeningTime(duration: Duration): String {
    val minutes = ((duration.inWholeSeconds + 59) / 60).coerceAtLeast(1)
    return if (minutes < 60) tr("~{0} min", minutes) else tr("~{0} h {1} min", minutes / 60, minutes % 60)
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
