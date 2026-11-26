package com.idiomiko.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(onNavigate: (Route) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    // Back from reading or reviewing, the day's figures have changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    Scaffold(
        topBar = {
            AppTopBar(title = tr("Home"), onNavigate = onNavigate, section = NavSection.HOME)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            PageColumn(padding) { HomeSkeleton() }
            return@Scaffold
        }
        PageColumn(padding) {
            HomeContent(state, HomeActions(onNavigate, onOpenLesson = viewModel::openLesson, onContinuePlaylist = viewModel::continuePlaylist, onSavedToday = viewModel::showSavedToday))
        }
    }
    val savedToday by viewModel.savedToday.collectAsStateWithLifecycle()
    savedToday?.let { words ->
        SavedTodayDialog(words, onOpen = { viewModel.closeSavedToday(); onNavigate(Route.EditTerm(it)) }, onDismiss = viewModel::closeSavedToday)
    }
}

/** What the page's parts do: go somewhere, open a lesson, play the playlist on, list the words saved today. */
internal class HomeActions(
    val onNavigate: (Route) -> Unit,
    val onOpenLesson: (CourseProgress, LessonProgress) -> Unit = { _, _ -> },
    val onContinuePlaylist: () -> Unit = {},
    val onSavedToday: () -> Unit = {},
)

/**
 * The day, then the one step to take first, the rest under way, and the progress around them. A wide window greets
 * the reader above two columns, the doing on the left and the progress on the right, ending level; a phone has
 * the day's card and one column.
 */
@Composable
internal fun HomeContent(state: HomeUiState, actions: HomeActions) {
    if (LocalWindowWidth.current.isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            PhoneTodayCard(state, actions)
            NextUpCard(state, actions)
            ContinueCard(state, actions)
            VocabularyRow(actions)
            RecentWordsCard(state, actions)
            RecommendedCard(state, actions)
            RecentActivityCard(state, actions)
        }
        return
    }
    WideHeader(state, actions)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= TWO_COLUMNS) {
            LevelColumns(
                left = {
                    NextUpCard(state, actions)
                    ContinueCard(state, actions)
                    RecommendedCard(state, actions)
                },
                right = {
                    VocabularyCard(state, actions)
                    RecentWordsCard(state, actions)
                    RecentActivityCard(state, actions)
                },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                NextUpCard(state, actions)
                ContinueCard(state, actions)
                RecentWordsCard(state, actions)
                VocabularyCard(state, actions)
                RecommendedCard(state, actions)
                RecentActivityCard(state, actions)
            }
        }
    }
}

/**
 * Two columns of cards, the left one wider, whose last cards grow so both columns end level; a column may leave
 * cards out. Each card is one layout that takes a least height given to it.
 *
 * The left column's last card is asked its height before it is measured, so it must not be one that composes on
 * measuring (a lazy list, a BoxWithConstraints); the right column's is measured once, at least as tall as the left.
 */
@Composable
private fun LevelColumns(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Layout(contents = listOf(left, right)) { (lefts, rights), constraints ->
        val gap = AppSpacing.xl.roundToPx()
        val leftWidth = ((constraints.maxWidth - gap) * LEFT_SHARE).toInt()
        val rightWidth = constraints.maxWidth - gap - leftWidth
        fun measureAllButLast(cards: List<Measurable>, width: Int) = cards.dropLast(1).map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
        val leftTop = measureAllButLast(lefts, leftWidth)
        val rightTop = measureAllButLast(rights, rightWidth)
        fun heightOf(placed: List<Placeable>) = placed.sumOf { it.height } + gap * placed.size
        val leftLast = lefts.lastOrNull()
        val rightLast = rights.lastOrNull()
        val leftNatural = heightOf(leftTop) + (leftLast?.maxIntrinsicHeight(leftWidth) ?: -gap)
        val rightPlaced = rightLast?.measure(Constraints(minWidth = rightWidth, maxWidth = rightWidth, minHeight = (leftNatural - heightOf(rightTop)).coerceAtLeast(0)))
        val rightHeight = heightOf(rightTop) + (rightPlaced?.height ?: -gap)
        val leftPlaced = leftLast?.measure(Constraints(minWidth = leftWidth, maxWidth = leftWidth, minHeight = (rightHeight - heightOf(leftTop)).coerceAtLeast(0)))
        val lefty = leftTop + listOfNotNull(leftPlaced)
        val righty = rightTop + listOfNotNull(rightPlaced)
        val height = maxOf(heightOf(lefty), heightOf(righty)) - gap
        layout(constraints.maxWidth, height.coerceAtLeast(0)) {
            var y = 0
            lefty.forEach { it.placeRelative(0, y); y += it.height + gap }
            y = 0
            righty.forEach { it.placeRelative(leftWidth + gap, y); y += it.height + gap }
        }
    }
}

/** Wide enough for the doing and the progress side by side. */
private val TWO_COLUMNS = 1040.dp

/** The doing's share of the width, as the design has it: 11 to the progress's 7. */
private const val LEFT_SHARE = 11f / 18f
