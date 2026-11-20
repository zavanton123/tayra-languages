package com.idiomiko.feature.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.CourseJourney
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.courses.LevelProgress
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.ChipSize
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SelectableChip
import com.idiomiko.core.ui.components.TagFilterMenu
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.math.roundToInt

/** The level as a badge, turned a little: its code over its name, in the level's colour. */
@Composable
internal fun LevelBadge(level: CourseLevel, size: Dp, modifier: Modifier = Modifier) {
    val small = size < BADGE
    Box(
        modifier.rotate(BADGE_TILT).size(size).clip(if (small) AppShapes.card else AppShapes.large)
            .background(Brush.linearGradient(listOf(level.tint, lerp(level.tint, AppAccents.ink, BADGE_SHADE))))
            .padding(AppSpacing.xs).testTag("level-badge"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(level.code, style = if (small) AppText.sectionTitle else AppText.screenTitle, color = AppAccents.onAccent)
            if (!small) {
                Text(tr(level.label), style = MaterialTheme.typography.labelSmall, color = AppAccents.onAccent.copy(alpha = AppAlpha.strong), textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** The words known as a line, or null where nothing counts them or none are known yet. */
@Composable
private fun knownLine(journey: CourseJourney): String? = journey.known?.takeIf { it > 0 }?.let { trPlural(it, "{1} word known", "{1} words known", formatCount(it)) }

/** How many more words lead to the next level, or null. */
@Composable
private fun toNextLine(journey: CourseJourney): String? {
    val left = journey.wordsToNext ?: return null
    val next = journey.next ?: return null
    return trPlural(left, "{1} word to {2}", "{1} words to {2}", formatCount(left), next.level.code)
}

/** The reader's level as a badge, with the words they know and how many more lead to the next level. */
@Composable
internal fun LevelCard(journey: CourseJourney, modifier: Modifier = Modifier) {
    val level = journey.currentProgress ?: return
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xl).testTag("level-card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        LevelBadge(level.level, BADGE)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(knownLine(journey) ?: level.level.display, style = AppText.itemTitle)
            Text(
                toNextLine(journey) ?: trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** The detail under a level of the journey: the courses completed, and on the current level that the reader is there. */
@Composable
private fun levelDetail(level: LevelProgress, current: Boolean): String = when {
    current -> tr("{0} of {1}", level.completed, level.courses) + " · " + tr("you are here")
    level.completed > 0 -> trPlural(level.courses, "{1} of {0} course", "{1} of {0} courses", level.completed)
    else -> trPlural(level.courses, "{0} course", "{0} courses")
}

internal fun percent(share: Float) = "${(share * 100).roundToInt()}%"

/** The levels of the courses in order, as stations of a journey: those finished ticked, the reader's own picked out. Each one leads to its courses. */
@Composable
internal fun JourneyCard(journey: CourseJourney, onLevel: (CourseLevel) -> Unit, modifier: Modifier = Modifier) {
    if (journey.levels.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.m).testTag("journey"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Text(tr("Your journey"), Modifier.padding(start = AppSpacing.m, top = AppSpacing.s, bottom = AppSpacing.s), style = AppText.itemTitle)
        journey.levels.forEach { level ->
            val current = level.level == journey.current
            Row(
                Modifier.fillMaxWidth().clip(AppShapes.card)
                    .background(if (current) level.level.tint.copy(alpha = AppAlpha.tint) else colors.surface)
                    .clickable { onLevel(level.level) }
                    .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s).testTag("journey-${level.level.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            ) {
                Station(level, current)
                Column(Modifier.weight(1f)) {
                    Text(level.level.display, style = AppText.labelStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(levelDetail(level, current), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2)
                }
                if (current) Text(percent(level.share), style = AppText.labelStrong, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** A level's station: a tick when it is finished, its code in its colour where the reader is, else its code greyed. */
@Composable
private fun Station(level: LevelProgress, current: Boolean) {
    val colors = MaterialTheme.colorScheme
    val fill = when {
        level.done -> AppAccents.green
        current -> level.level.tint
        else -> colors.surfaceContainerHighest
    }
    Box(Modifier.size(STATION).clip(AppShapes.circle).background(fill), contentAlignment = Alignment.Center) {
        if (level.done) Icon(Icons.Default.Check, contentDescription = tr("Completed"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconCompact))
        else Text(level.level.code, style = AppText.badge, color = if (current) AppAccents.onAccent else colors.onSurfaceVariant)
    }
}

/**
 * A phone's level strip: the badge with the words known, and the levels as chips to scroll through, the reader's
 * own picked out, finished ones ticked. A chip leads to its level's courses.
 */
@Composable
internal fun JourneyStrip(journey: CourseJourney, onLevel: (CourseLevel) -> Unit, modifier: Modifier = Modifier) {
    val level = journey.currentProgress ?: return
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.m).testTag("journey-strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        LevelBadge(level.level, BADGE_SMALL, Modifier.padding(AppSpacing.xxs))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(
                listOfNotNull(knownLine(journey), toNextLine(journey)).joinToString(" · ").ifEmpty { level.level.display },
                style = AppText.label,
                color = colors.onSurfaceVariant,
                maxLines = 2,
            )
            // The reader's level is scrolled to, so a reader at C1 sees it without scrolling the strip.
            val scroll = rememberScrollState()
            var currentAt by remember { mutableStateOf<Int?>(null) }
            LaunchedEffect(currentAt) { currentAt?.let { scroll.scrollTo(it) } }
            Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                journey.levels.forEach { item ->
                    val current = item.level == journey.current
                    LevelChip(item, current, if (current) Modifier.onPlaced { currentAt = it.positionInParent().x.roundToInt() } else Modifier) { onLevel(item.level) }
                }
            }
        }
    }
}

/** A level on the phone's strip: its code, ticked when finished, with the share read on the reader's own. */
@Composable
private fun LevelChip(level: LevelProgress, current: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = level.level.tint
    Row(
        modifier.height(LEVEL_CHIP).clip(AppShapes.pill)
            .background(
                when {
                    current -> tint
                    level.done -> AppAccents.green.copy(alpha = AppAlpha.tint)
                    else -> colors.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick).padding(horizontal = AppSpacing.m).testTag("journey-${level.level.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        if (level.done) Icon(Icons.Default.Check, contentDescription = tr("Completed"), tint = AppAccents.onWash(AppAccents.green), modifier = Modifier.size(AppSizes.iconSmall))
        Text(
            if (current) "${level.level.code} · ${percent(level.share)}" else level.level.code,
            style = AppText.labelStrong,
            color = when {
                current -> AppAccents.onAccent
                level.done -> AppAccents.onWash(AppAccents.green)
                else -> colors.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/** The status filters as chips: all courses, those in progress, not started and completed, each with how many it holds. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatusChips(state: CoursesUiState, onStatus: (LessonStatus?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        listOf(null, LessonStatus.IN_PROGRESS, LessonStatus.NOT_STARTED, LessonStatus.COMPLETED).forEach { status ->
            SelectableChip(
                status?.label ?: tr("All"),
                selected = state.status == status,
                onClick = { onStatus(status) },
                modifier = Modifier.testTag("status-${status?.name ?: "ALL"}"),
                count = status?.let { state.counts[it] ?: 0 },
                size = ChipSize.Small,
            )
        }
    }
}

/** The courses' tags as chips to turn on and off, the most used first, then [more]: the way to the full tag filter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagChips(state: CoursesUiState, onToggle: (String) -> Unit, more: @Composable () -> Unit) {
    if (state.availableTags.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        state.availableTags.take(TAG_CHIPS).forEach { tag ->
            SelectableChip("#$tag", selected = tag in state.tags, onClick = { onToggle(tag) }, modifier = Modifier.testTag("tag-chip-$tag"), tint = AppAccents.purple, size = ChipSize.Small)
        }
        more()
    }
}

/** The way to the full tag filter, with every tag and whether a course needs them all. */
@Composable
internal fun MoreTagsLink(onClick: () -> Unit) =
    LinkText(tr("Filter by tags"), onClick, Modifier.testTag("tag-filter"), icon = AppIcons.Tag, style = MaterialTheme.typography.bodyMedium)

/** The order of the courses and whether they show as tiles or rows, as compact controls. */
@Composable
internal fun SortAndView(state: CoursesUiState, onSort: (CoursesSort) -> Unit, onView: (CoursesView) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        SelectMenu(CoursesSort.entries, state.sort, onSort, { it.label }, modifier = Modifier.weight(1f), active = state.sort != CoursesSort.RECOMMENDED, buttonModifier = Modifier.fillMaxWidth().testTag("course-sort"))
        SegmentedToggle(viewSegments(), state.view, onView, dense = true)
    }
}

/** The quiet way to make a course of one's own. */
@Composable
internal fun CreateCourseLink(onNewCourse: () -> Unit) =
    LinkText(tr("Create your own course"), onNewCourse, Modifier.testTag("new-course"), icon = Icons.Default.Add, style = MaterialTheme.typography.bodyMedium)

/** The sidebar's search and filters: the search, status, tags, order and view, and a course of one's own. */
@Composable
internal fun FiltersCard(state: CoursesUiState, actions: CoursesActions, modifier: Modifier = Modifier) {
    var tags by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l).testTag("filters"), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SearchBox(state.search, actions.onSearch, Modifier.fillMaxWidth())
        StatusChips(state, actions.onStatus)
        TagChips(state, actions.onToggleTag) {
            Box {
                MoreTagsLink { tags = true }
                TagFilterMenu(tags, onDismiss = { tags = false }, state.availableTags, state.tags, state.matchAllTags, actions.onTags)
            }
        }
        SortAndView(state, actions.onSort, actions.onView)
        CreateCourseLink(actions.onNewCourse)
    }
}

private val BADGE = 84.dp
private val BADGE_SMALL = 52.dp
private const val BADGE_TILT = -4f
private const val BADGE_SHADE = 0.22f
private val STATION = 34.dp
private val LEVEL_CHIP = 32.dp

/** How many tags show as chips; the rest are in the full tag filter. */
private const val TAG_CHIPS = 8
