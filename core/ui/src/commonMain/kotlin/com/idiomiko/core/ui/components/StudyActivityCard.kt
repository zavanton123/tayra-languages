package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.idiomiko.core.domain.stats.StudyActivity
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import kotlinx.datetime.LocalDate

/**
 * Each day of [range], darker the more was studied: words read, flashcards answered or words saved. It counts
 * the days as the streaks do (see [StudyActivity]), on Home and on Statistics alike.
 */
@Composable
fun StudyActivityCard(activity: StudyActivity, range: ClosedRange<LocalDate>, subtitle: String? = null, action: (@Composable RowScope.() -> Unit)? = null, modifier: Modifier = Modifier) {
    ActivityHeatmapCard(
        title = tr("Study activity"),
        subtitle = { subtitle },
        today = activity.today,
        levels = { activity.levels(range.start, range.endInclusive) },
        range = range,
        icon = AppIcons.History,
        action = action,
        modifier = modifier,
        describe = { date ->
            val day = activity.days[date]
            listOfNotNull(
                day?.wordsRead?.takeIf { it > 0 }?.let { trPlural(it, "{1} word read", "{1} words read", formatCount(it)) },
                day?.reviews?.takeIf { it > 0 }?.let { trPlural(it, "{1} review", "{1} reviews", formatCount(it)) },
                day?.wordsSaved?.takeIf { it > 0 }?.let { trPlural(it, "{1} word saved", "{1} words saved", formatCount(it)) },
            ).joinToString(" · ").ifEmpty { tr("Nothing studied") }
        },
    )
}
