package com.idiomiko.feature.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.ChipSize
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectableChip
import com.idiomiko.core.ui.components.TagFilterSheet
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The courses on a phone: one column, under the bar that names the page. */
@Composable
internal fun PhoneCoursesContent(state: CoursesUiState, actions: CoursesActions, modifier: Modifier = Modifier) =
    CoursesColumn(state, actions, phone = true, modifier = modifier)

/** Which sheet is open over the page. */
private enum class CoursesSheet { FILTERS, TAGS }

/**
 * The courses in one column, on a phone or a window too narrow for the pane above them: the level and the journey as a strip,
 * the search with the filters behind a button, the next lesson, and the courses by level.
 */
@Composable
internal fun CoursesColumn(state: CoursesUiState, actions: CoursesActions, phone: Boolean, modifier: Modifier = Modifier) {
    var sheet by remember { mutableStateOf<CoursesSheet?>(null) }
    val nav = rememberJourneyNav()
    val edge = if (phone) AppSpacing.l else AppSpacing.xxxl
    ScrollColumn(
        modifier.fillMaxSize(),
        state = nav.scroll,
        contentModifier = Modifier.padding(horizontal = edge, vertical = if (phone) AppSpacing.l else AppSpacing.xxl).onPlaced { nav.content = it },
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        // A phone's bar names the page; a wider window's column has its title.
        if (!phone) Text(tr("Courses"), style = AppText.pageTitle)
        if (state.courses.isEmpty()) {
            EmptyCourses(state, actions.onNewCourse, actions.onDownloadCourses)
            return@ScrollColumn
        }
        JourneyStrip(state.journey, onLevel = { nav.goTo(it, state.journey) })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            SearchBox(state.search, actions.onSearch, Modifier.weight(1f))
            SecondaryButton(
                tr("Filters"),
                onClick = { sheet = CoursesSheet.FILTERS },
                modifier = Modifier.testTag("filters-button"),
                icon = AppIcons.Tune,
                badge = state.filterCount.takeIf { it > 0 },
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= THREE_COLUMNS) 3 else 2
            Catalogue(state, actions, nav, columns, compact = phone, picture = !phone && maxWidth >= PICTURE_WIDTH)
        }
    }
    when (sheet) {
        CoursesSheet.FILTERS -> FiltersSheet(state, actions, listView = !phone, onTags = { sheet = CoursesSheet.TAGS }, onDismiss = { sheet = null })
        CoursesSheet.TAGS -> TagFilterSheet(
            state.availableTags,
            state.tags,
            state.matchAllTags,
            onApply = { tags, all -> actions.onTags(tags, all); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> {}
    }
}

/** The filters behind the Filters button: status, tags, order and, where tiles and rows both fit, the view; and a course of one's own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(state: CoursesUiState, actions: CoursesActions, listView: Boolean, onTags: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m).testTag("filters-sheet"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Text(tr("Filters"), style = AppText.itemTitle)
            Text(tr("Status"), style = AppText.emphasis, color = colors.onSurfaceVariant)
            StatusChips(state, actions.onStatus)
            if (state.availableTags.isNotEmpty()) {
                Text(tr("Tags"), style = AppText.emphasis, color = colors.onSurfaceVariant)
                TagChips(state, actions.onToggleTag) { MoreTagsLink(onTags) }
            }
            Text(tr("Sort"), style = AppText.emphasis, color = colors.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                CoursesSort.entries.forEach { sort ->
                    SelectableChip(sort.label, selected = state.sort == sort, onClick = { actions.onSort(sort) }, modifier = Modifier.testTag("sort-${sort.name}"), size = ChipSize.Small)
                }
            }
            if (listView) SegmentedToggle(viewSegments(), state.view, actions.onView)
            CreateCourseLink { onDismiss(); actions.onNewCourse() }
        }
    }
}

private val THREE_COLUMNS = 560.dp
private val PICTURE_WIDTH = 760.dp
