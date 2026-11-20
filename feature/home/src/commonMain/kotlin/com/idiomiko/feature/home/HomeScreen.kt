package com.idiomiko.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.stats.DailyGoal
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.animatedCount
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
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
            AppTopBar(title = tr("Home"), onNavigate = onNavigate, section = NavSection.HOME, actions = { QuickAddButton(state.readsWebPages, onNavigate) })
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
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
 * Today's line, then the one step to take first, the rest under way, and the progress around them: in two
 * columns on a wide window, the doing on the left and the progress on the right, one column otherwise.
 */
@Composable
internal fun HomeContent(state: HomeUiState, actions: HomeActions) {
    TodayLine(state, actions)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= TWO_COLUMNS) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    NextUpCard(state, actions)
                    ContinueCard(state, actions)
                    SuggestionsCard(state, actions)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    VocabularyCard(state, actions)
                    RecentWordsCard(state, actions)
                    RecentActivityCard(state, actions)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                NextUpCard(state, actions)
                ContinueCard(state, actions)
                RecentWordsCard(state, actions)
                VocabularyCard(state, actions)
                SuggestionsCard(state, actions)
                RecentActivityCard(state, actions)
            }
        }
    }
}

/**
 * The day at a glance, in place of a page heading: the daily goal's ring with the minutes studied, and a
 * strip of figures: the streak, the words saved today (which lists them) and the words known.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayLine(state: HomeUiState, actions: HomeActions) {
    val colors = MaterialTheme.colorScheme
    val goal = state.goal
    FlowRow(
        Modifier.fillMaxWidth().testTag("today"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // The goal is set in Settings, where the ring leads.
        Row(
            Modifier.clip(AppShapes.control).clickable(onClickLabel = tr("Change the daily goal"), role = Role.Button) { actions.onNavigate(Route.Settings) }
                .padding(end = AppSpacing.s).testTag("daily-goal"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            GoalRing(goal)
            Column {
                Text(
                    if (goal.reached) tr("Daily goal reached") else trPlural(goal.goal, "{1} of {0} minute today", "{1} of {0} minutes today", goal.done),
                    style = AppText.itemTitle,
                )
                Text(
                    if (goal.reached) trPlural(goal.done, "{0} minute of study today", "{0} minutes of study today")
                    else trPlural(goal.left, "{0} minute to go: reading and flashcards count", "{0} minutes to go: reading and flashcards count"),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        val activity = state.activity
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            // Figures of nothing yet are left out, but for the words saved today, which lists them.
            val streak = animatedCount(activity?.streak ?: 0)
            if (streak > 0) Figure(AppIcons.Flame, AppAccents.pink, trPlural(streak, "{0}-day streak", "{0}-day streak"), "streak")
            val saved = activity?.savedToday ?: 0
            Figure(AppIcons.Abc, AppAccents.orange, trPlural(saved, "{1} word saved today", "{1} words saved today", formatCount(saved)), "saved-today", actions.onSavedToday)
            state.vocabulary?.takeIf { it.known > 0 }?.let { words ->
                Figure(AppIcons.DoneAll, AppAccents.green, trPlural(words.known, "{1} word known", "{1} words known", formatCount(words.known)), "known") { actions.onNavigate(Route.WordFrequency) }
            }
        }
    }
}

/** The day's minutes against the goal as a ring, the minutes in its middle; green once the goal is reached. */
@Composable
private fun GoalRing(goal: DailyGoal) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.tint)
    val fill = if (goal.reached) AppAccents.green else MaterialTheme.colorScheme.primary
    Box(Modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RING_SIZE)) {
            val stroke = RING_STROKE.toPx()
            val corner = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = corner, size = arc, style = Stroke(stroke))
            if (goal.fraction > 0f) drawArc(fill, -90f, 360f * goal.fraction, useCenter = false, topLeft = corner, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${goal.done}", style = AppText.labelStrong, maxLines = 1)
    }
}

/** A figure of the day as a small pill; with [onClick], it leads on. */
@Composable
private fun Figure(icon: ImageVector, tint: Color, text: String, tag: String, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(AppShapes.pill).background(colors.surface).border(AppBorders.hairline(), AppShapes.pill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s).testTag("figure-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconSmall))
        Text(text, style = AppText.label, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Wide enough for the doing and the progress side by side. */
private val TWO_COLUMNS = 1040.dp
private val RING_SIZE = 52.dp
private val RING_STROKE = 6.dp
