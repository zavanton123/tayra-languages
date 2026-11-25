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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.stats.DailyGoal
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTooltip
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.animatedCount
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The part of the day a greeting fits. */
enum class DayPart {
    MORNING, AFTERNOON, EVENING;

    companion object {
        /** Morning from 5 to noon, afternoon until 6 pm, evening the rest of the night. */
        fun at(hour: Int): DayPart = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}

/**
 * Morning, afternoon and evening greetings in the languages learned, by their English name. The afternoon has no
 * greeting of its own in some of them, which say good day then, or good morning until the evening in French.
 */
private val GREETINGS: Map<String, List<String>> = mapOf(
    "portuguese" to listOf("Bom dia!", "Boa tarde!", "Boa noite!"),
    "spanish" to listOf("¡Buenos días!", "¡Buenas tardes!", "¡Buenas noches!"),
    "french" to listOf("Bonjour !", "Bonjour !", "Bonsoir !"),
    "italian" to listOf("Buongiorno!", "Buon pomeriggio!", "Buonasera!"),
    "catalan" to listOf("Bon dia!", "Bona tarda!", "Bon vespre!"),
    "romanian" to listOf("Bună dimineața!", "Bună ziua!", "Bună seara!"),
    "german" to listOf("Guten Morgen!", "Guten Tag!", "Guten Abend!"),
    "dutch" to listOf("Goedemorgen!", "Goedemiddag!", "Goedenavond!"),
    "swedish" to listOf("God morgon!", "God eftermiddag!", "God kväll!"),
    "norwegian" to listOf("God morgen!", "God ettermiddag!", "God kveld!"),
    "danish" to listOf("Godmorgen!", "God eftermiddag!", "Godaften!"),
    "russian" to listOf("Доброе утро!", "Добрый день!", "Добрый вечер!"),
    "ukrainian" to listOf("Доброго ранку!", "Добрий день!", "Добрий вечір!"),
    "polish" to listOf("Dzień dobry!", "Dzień dobry!", "Dobry wieczór!"),
    "czech" to listOf("Dobré ráno!", "Dobré odpoledne!", "Dobrý večer!"),
    "greek" to listOf("Καλημέρα!", "Καλό απόγευμα!", "Καλησπέρα!"),
    "finnish" to listOf("Hyvää huomenta!", "Hyvää päivää!", "Hyvää iltaa!"),
    "hungarian" to listOf("Jó reggelt!", "Jó napot!", "Jó estét!"),
    "turkish" to listOf("Günaydın!", "İyi günler!", "İyi akşamlar!"),
    "esperanto" to listOf("Bonan matenon!", "Bonan tagon!", "Bonan vesperon!"),
    "indonesian" to listOf("Selamat pagi!", "Selamat siang!", "Selamat malam!"),
    "vietnamese" to listOf("Chào buổi sáng!", "Chào buổi chiều!", "Chào buổi tối!"),
    "chinese" to listOf("早上好！", "下午好！", "晚上好！"),
    "mandarin chinese" to listOf("早上好！", "下午好！", "晚上好！"),
    "japanese" to listOf("おはようございます！", "こんにちは！", "こんばんは！"),
)

/** A greeting for the time of day in the language being learned where it is known, else in the interface's. */
@Composable
internal fun greeting(languageName: String, part: DayPart): String =
    GREETINGS[languageName.trim().lowercase()]?.get(part.ordinal)?.inLearnedScript() ?: when (part) {
        DayPart.MORNING -> tr("Good morning!")
        DayPart.AFTERNOON -> tr("Good afternoon!")
        DayPart.EVENING -> tr("Good evening!")
    }

/** How far the day's study has come, said to cheer it on: not begun, begun, nearly there, done. */
@Composable
private fun goalCheer(goal: DailyGoal): String = when {
    goal.reached -> tr("You reached today's goal. Well done!")
    goal.done == 0 -> trPlural(goal.goal, "Ready when you are: today's goal is {0} minute.", "Ready when you are: today's goal is {0} minutes.")
    goal.fraction >= 0.5f -> tr("Keep going — you're almost at today's goal!")
    else -> tr("Good start — keep going towards today's goal.")
}

/**
 * The head of a wide page, in place of a title: a greeting for the time of day with how the day's goal goes, and
 * one card of the day's figures: the goal's ring and minutes, then the streak (which leads to the statistics), the
 * words saved today (which lists them) and the words known.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WideHeader(state: HomeUiState, actions: HomeActions) {
    FlowRow(
        Modifier.fillMaxWidth().testTag("today"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = AppSpacing.xxl)) {
            Text(greeting(state.languageName, state.dayPart), style = AppText.pageTitle, modifier = Modifier.testTag("greeting"))
            Text(goalCheer(state.goal), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("goal-cheer"))
        }
        BoxWithConstraints {
            val figures = @Composable { modifier: Modifier ->
                FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    val activity = state.activity
                    // Figures of nothing yet are left out, but for the words saved today, which lists them.
                    val streak = animatedCount(activity?.streak ?: 0)
                    if (streak > 0) Figure(AppIcons.Flame, AppAccents.pink, trPlural(streak, "{0}-day streak", "{0}-day streak"), "streak") { actions.onNavigate(Route.Stats) }
                    val saved = activity?.savedToday ?: 0
                    Figure(AppIcons.Abc, AppAccents.orange, trPlural(saved, "{1} word saved today", "{1} words saved today", formatCount(saved)), "saved-today", actions.onSavedToday)
                    state.vocabulary?.takeIf { it.known > 0 }?.let { words ->
                        Figure(Icons.Default.Check, AppAccents.green, trPlural(words.known, "{1} word known", "{1} words known", formatCount(words.known)), "known") { actions.onNavigate(Route.WordFrequency) }
                    }
                }
            }
            val card = Modifier.cardSurface(AppShapes.card).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m)
            // The figures beside the goal where the line has room for them all, under it where it has not.
            if (maxWidth >= ONE_LINE_CARD) {
                Row(card, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    GoalSummary(state.goal, RING_WIDE, actions, today = false)
                    figures(Modifier.weight(1f, fill = false))
                }
            } else {
                Column(card.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    GoalSummary(state.goal, RING_WIDE, actions, today = false)
                    AppDivider()
                    figures(Modifier)
                }
            }
        }
    }
}

/** The phone's card of the day: the goal's ring and minutes, and under them the streak and the words known side by side. */
@Composable
internal fun PhoneTodayCard(state: HomeUiState, actions: HomeActions) {
    val streak = animatedCount(state.activity?.streak ?: 0).takeIf { it > 0 }
    val known = state.vocabulary?.known?.takeIf { it > 0 }
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(horizontal = AppSpacing.m, vertical = AppSpacing.l).testTag("today")) {
        Box(Modifier.padding(horizontal = AppSpacing.xs)) { GoalSummary(state.goal, RING_PHONE, actions, today = true) }
        if (streak != null || known != null) {
            AppDivider(Modifier.padding(vertical = AppSpacing.m))
            // Each figure at its own width, spread evenly; the streak is short, the words known take what room is left.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                streak?.let { PhoneFigure(AppIcons.Flame, AppAccents.pink, trPlural(it, "{0}-day streak", "{0}-day streak"), "streak", Modifier) { actions.onNavigate(Route.Stats) } }
                if (streak != null && known != null) AppVerticalDivider(Modifier.padding(horizontal = AppSpacing.xs).height(PHONE_DIVIDER_HEIGHT))
                known?.let {
                    PhoneFigure(Icons.Default.Check, AppAccents.green, trPlural(it, "{1} word known", "{1} words known", formatCount(it)), "known", Modifier.weight(1f, fill = false)) {
                        actions.onNavigate(Route.WordFrequency)
                    }
                }
            }
        }
    }
}

/**
 * The goal's ring beside the minutes studied and those to go; the goal is set in Settings, where it leads.
 * [today] says "today" after the minutes, where no greeting above says it already.
 */
@Composable
private fun GoalSummary(goal: DailyGoal, ring: Dp, actions: HomeActions, today: Boolean) {
    val colors = MaterialTheme.colorScheme
    AppTooltip(tr("Reading and flashcards count towards the daily goal.")) {
        Row(
            Modifier.clip(AppShapes.control).clickable(onClickLabel = tr("Change the daily goal"), role = Role.Button) { actions.onNavigate(Route.Goals) }
                .padding(end = if (today) 0.dp else AppSpacing.s).testTag("daily-goal"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            GoalRing(goal, ring)
            if (today) AppVerticalDivider(Modifier.height(ring * 0.7f))
            Column {
                Text(
                    when {
                        goal.reached -> tr("Daily goal reached")
                        today -> trPlural(goal.goal, "{1} of {0} minute today", "{1} of {0} minutes today", goal.done)
                        else -> trPlural(goal.goal, "{1} of {0} minute", "{1} of {0} minutes", goal.done)
                    },
                    style = AppText.sectionTitle,
                )
                if (goal.reached) Text(
                    trPlural(goal.done, "{0} minute of study today", "{0} minutes of study today"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** The day's minutes against the goal as a ring, the minutes in its middle; green once the goal is reached. */
@Composable
private fun GoalRing(goal: DailyGoal, ring: Dp) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.tint)
    val fill = if (goal.reached) AppAccents.green else MaterialTheme.colorScheme.primary
    Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ring)) {
            val stroke = RING_STROKE.toPx()
            val corner = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = corner, size = arc, style = Stroke(stroke))
            if (goal.fraction > 0f) drawArc(fill, -90f, 360f * goal.fraction, useCenter = false, topLeft = corner, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${goal.done}", style = AppText.headline, maxLines = 1)
    }
}

/** A figure of the day as a pill; with [onClick], it leads on. */
@Composable
private fun Figure(icon: ImageVector, tint: Color, text: String, tag: String, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(AppShapes.pill).background(colors.surface).border(AppBorders.hairline(), AppShapes.pill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("figure-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.icon))
        Text(text, style = AppText.emphasis, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A figure of the phone's day card: centred in its half, with no pill around it. */
@Composable
private fun PhoneFigure(icon: ImageVector, tint: Color, text: String, tag: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.clip(AppShapes.control).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = AppSpacing.xs).testTag("figure-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconLarge))
        Text(text, style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


private val RING_WIDE = 64.dp
private val RING_PHONE = 58.dp
private val RING_STROKE = 7.dp

/** Room for the goal and the three figures on one line. */
private val ONE_LINE_CARD = 860.dp
private val PHONE_DIVIDER_HEIGHT = 28.dp
