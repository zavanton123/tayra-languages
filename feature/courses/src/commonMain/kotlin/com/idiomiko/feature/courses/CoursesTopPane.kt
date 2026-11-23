package com.idiomiko.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.CourseJourney
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.LevelProgress
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.TagFilterMenu
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.tint

/**
 * A wide window's pane above the courses: the journey through the levels with the reader's own, then the search
 * beside the way to a course of one's own, and the filters, the order and the view.
 */
@Composable
internal fun CoursesTopPane(state: CoursesUiState, actions: CoursesActions, onLevel: (CourseLevel) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().cardSurface(AppShapes.large).testTag("top-pane")) {
        if (state.journey.levels.isNotEmpty()) {
            JourneyRow(state.journey, onLevel, Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l))
            AppDivider()
        }
        Column(Modifier.padding(AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                SearchBox(state.search, actions.onSearch, Modifier.weight(1f))
                PrimaryButton(tr("Create your own course"), actions.onNewCourse, Modifier.height(AppSizes.control).testTag("new-course"), icon = Icons.Default.Add)
            }
            FilterRow(state, actions)
        }
    }
}

/** The journey's line: its name, the reader's level with the words to the next, and the levels to go to. */
@Composable
private fun JourneyRow(journey: CourseJourney, onLevel: (CourseLevel) -> Unit, modifier: Modifier) {
    BesideOrAbove(
        heading = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Text(tr("Your journey"), style = AppText.itemTitle, maxLines = 1)
                journey.currentProgress?.let { level ->
                    AppVerticalDivider(Modifier.height(JOURNEY_DIVIDER))
                    LevelSummary(journey, level)
                }
            }
        },
        divider = { AppVerticalDivider(Modifier.height(JOURNEY_DIVIDER)) },
        levels = {
            EvenRows(AppSpacing.xs, AppSpacing.xs) {
                journey.levels.forEach { level -> JourneyLevel(level, level.level == journey.current) { onLevel(level.level) } }
            }
        },
        modifier = modifier.fillMaxWidth().testTag("journey"),
    )
}

/** The reader's level: its badge, its name, and the words known and still to go to the next one. */
@Composable
private fun LevelSummary(journey: CourseJourney, level: LevelProgress) {
    Row(Modifier.testTag("level-summary"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        LevelBadge(level.level, SUMMARY_BADGE)
        Column {
            Text(level.level.display, style = AppText.labelStrong, maxLines = 1)
            Text(
                listOfNotNull(knownLine(journey), toNextLine(journey)).joinToString(" · ")
                    .ifEmpty { trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** The detail under a level of the journey: the courses completed on the reader's own and on those begun, else how many there are. */
@Composable
private fun levelDetail(level: LevelProgress, current: Boolean): String = when {
    current -> tr("{0} of {1}", level.completed, level.courses)
    level.completed > 0 -> trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed)
    else -> trPlural(level.courses, "{0} course", "{0} courses")
}

/** A level of the journey: its station, its name and its courses, on a wash of its colour where the reader is. */
@Composable
private fun JourneyLevel(level: LevelProgress, current: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(AppShapes.card)
            .then(if (current) Modifier.background(level.level.tint.copy(alpha = AppAlpha.tint)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.s)
            .testTag("journey-${level.level.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Station(level, current)
        Column {
            Text(level.level.display, style = AppText.badge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(levelDetail(level, current), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A level's station: its colour once the reader has reached it, ticked when finished, its code greyed ahead. */
@Composable
private fun Station(level: LevelProgress, current: Boolean) {
    val colors = MaterialTheme.colorScheme
    val reached = level.done || current
    Box(Modifier.size(STATION).clip(AppShapes.circle).background(if (reached) level.level.tint else colors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        if (level.done) Icon(Icons.Default.Check, contentDescription = tr("Completed"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
        else Text(level.level.code, style = AppText.badge, color = if (current) AppAccents.onAccent else colors.onSurfaceVariant)
    }
}

/**
 * The filters in a line that wraps: the status chips, the tags and the way to the full tag filter, and, at the end,
 * the order and the view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(state: CoursesUiState, actions: CoursesActions) {
    var tags by remember { mutableStateOf(false) }
    FlowRow(
        Modifier.fillMaxWidth().testTag("filters"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Box(Modifier.align(Alignment.CenterVertically)) { StatusChips(state, actions.onStatus) }
        if (state.availableTags.isNotEmpty()) {
            Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                AppVerticalDivider(Modifier.height(FILTER_DIVIDER))
                TagChips(state, actions.onToggleTag) {
                    Box(Modifier.align(Alignment.CenterVertically)) {
                        MoreTagsLink { tags = true }
                        TagFilterMenu(tags, onDismiss = { tags = false }, state.availableTags, state.tags, state.matchAllTags, actions.onTags)
                    }
                }
            }
        }
        Row(
            Modifier.weight(1f).align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.End),
        ) {
            SelectMenu(
                CoursesSort.entries,
                state.sort,
                actions.onSort,
                { it.label },
                modifier = Modifier.width(SORT_WIDTH),
                active = state.sort != CoursesSort.RECOMMENDED,
                buttonModifier = Modifier.fillMaxWidth().testTag("course-sort"),
            )
            SegmentedToggle(viewSegments(), state.view, actions.onView, dense = true)
        }
    }
}

/**
 * Lays the [heading] and the [levels] side by side, a [divider] between them, where both fit on one line at their
 * natural widths; else the heading above the levels, which then have the whole width.
 */
@Composable
private fun BesideOrAbove(heading: @Composable () -> Unit, divider: @Composable () -> Unit, levels: @Composable () -> Unit, modifier: Modifier) {
    Layout(contents = listOf(heading, divider, levels), modifier) { (headings, dividers, levelsList), constraints ->
        val head = headings.single()
        val line = dividers.single()
        val chips = levelsList.single()
        val gap = AppSpacing.l.roundToPx()
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val headWidth = head.maxIntrinsicWidth(Constraints.Infinity)
        val lineWidth = line.maxIntrinsicWidth(Constraints.Infinity)
        val beside = headWidth + lineWidth + 2 * gap + chips.maxIntrinsicWidth(Constraints.Infinity) <= width
        val headPlaced = head.measure(loose)
        if (beside) {
            val linePlaced = line.measure(loose)
            val rest = (width - headPlaced.width - linePlaced.width - 2 * gap).coerceAtLeast(0)
            val chipsPlaced = chips.measure(Constraints(minWidth = rest, maxWidth = rest))
            val height = maxOf(headPlaced.height, linePlaced.height, chipsPlaced.height)
            layout(width, height) {
                headPlaced.place(0, (height - headPlaced.height) / 2)
                linePlaced.place(headPlaced.width + gap, (height - linePlaced.height) / 2)
                chipsPlaced.place(width - rest, (height - chipsPlaced.height) / 2)
            }
        } else {
            val chipsPlaced = chips.measure(Constraints(minWidth = width, maxWidth = width))
            val below = headPlaced.height + AppSpacing.m.roundToPx()
            layout(width, below + chipsPlaced.height) {
                headPlaced.place(0, 0)
                chipsPlaced.place(0, below)
            }
        }
    }
}

/**
 * Its items in as few rows as hold them at their natural widths, the same number in each row as far as they go
 * (six as one row, then two rows of three, then three of two): one row fills the width with the room left shared
 * out, several rows line up in even columns.
 */
@Composable
private fun EvenRows(gapX: Dp, gapY: Dp, content: @Composable () -> Unit) {
    Layout(content, measurePolicy = remember(gapX, gapY) { EvenRowsPolicy(gapX, gapY) })
}

private class EvenRowsPolicy(private val gapX: Dp, private val gapY: Dp) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        if (measurables.isEmpty()) return layout(constraints.minWidth, 0) {}
        val gap = gapX.roundToPx()
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else natural.sum() + gap * (natural.size - 1)
        val count = measurables.size
        val perRow = (1..count).map { rows -> (count + rows - 1) / rows }.distinct()
            .firstOrNull { per -> natural.chunked(per).all { row -> row.sum() + gap * (row.size - 1) <= width } } ?: 1
        // Rows under one another line up as columns where every item fits a column's width.
        val column = (width - gap * (perRow - 1)) / perRow
        val columns = perRow < count && natural.all { it <= column }
        val rows = measurables.indices.chunked(perRow).map { row ->
            val room = width - gap * (row.size - 1)
            val extra = (room - row.sumOf { natural[it] }).coerceAtLeast(0) / row.size
            row.map { index ->
                val itemWidth = if (columns) column else (natural[index] + extra).coerceAtMost(room)
                measurables[index].measure(Constraints(minWidth = itemWidth, maxWidth = itemWidth))
            }
        }
        val rowGap = gapY.roundToPx()
        val heights = rows.map { row -> row.maxOf { it.height } }
        val height = heights.sum() + rowGap * (rows.size - 1)
        return layout(width, height) {
            var y = 0
            rows.forEachIndexed { r, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, y + (heights[r] - placeable.height) / 2)
                    x += placeable.width + gap
                }
                y += heights[r] + rowGap
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.sumOf { it.maxIntrinsicWidth(height) } + gapX.roundToPx() * (measurables.size - 1).coerceAtLeast(0)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.maxOfOrNull { it.minIntrinsicHeight(width) } ?: 0
}

private val SUMMARY_BADGE = 44.dp
private val STATION = 30.dp
private val JOURNEY_DIVIDER = 40.dp
private val FILTER_DIVIDER = 24.dp
private val SORT_WIDTH = 180.dp
