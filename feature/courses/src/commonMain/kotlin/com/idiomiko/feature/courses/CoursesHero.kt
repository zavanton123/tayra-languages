package com.idiomiko.feature.courses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.home.NextUp
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.JourneyIllustration
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * The lesson to read next, first on the page: the course it is in, its title, how long it takes and what it brings,
 * the course's lessons as a path with this one to play, and the ways in. A wide card has a picture beside it.
 */
@Composable
internal fun NextLessonHero(next: NextUp.Lesson, onStart: () -> Unit, onOpenLesson: (lessonId: String) -> Unit, onAllLessons: () -> Unit, compact: Boolean, picture: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val course = next.course.course
    val eyebrow = (if (next.started) tr("Keep going") else tr("Start here")) + " · " + course.title.inLearnedScript()
    val facts = listOfNotNull(
        tr("~{0} min", next.minutes),
        next.newWords.takeIf { it > 0 }?.let { trPlural(it, "{1} new word", "{1} new words", formatCount(it)) },
        tr("with audio").takeIf { next.lesson.book?.hasAudio == true },
    ).joinToString(" · ")
    val reading = next.lesson.status == LessonStatus.IN_PROGRESS
    val text: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(eyebrow.uppercase(), style = AppText.overline, color = AppAccents.onWash(AppAccents.orange), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("hero-eyebrow"))
            Text(
                tr("Lesson {0}: {1}", next.number, next.lesson.lesson.title.inLearnedScript()),
                style = if (compact) AppText.headline else AppText.screenTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(facts, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
    }
    val path: @Composable () -> Unit = { LessonPath(next.course.lessons, next.lesson, onOpenLesson, compact) }
    val buttons: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            PrimaryButton(
                if (reading) tr("Continue lesson") else tr("Start lesson"),
                onClick = onStart,
                modifier = Modifier.testTag("start-lesson").then(if (compact) Modifier.weight(PRIMARY_SHARE) else Modifier),
                size = ButtonSize.Large,
                icon = if (compact) null else AppIcons.PlayArrow,
            )
            SecondaryButton(tr("All lessons"), onClick = onAllLessons, modifier = Modifier.testTag("all-lessons").then(if (compact) Modifier.weight(1f) else Modifier), size = ButtonSize.Large)
        }
    }
    if (compact) {
        Column(
            modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l).testTag("next-lesson"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            text()
            path()
            buttons()
        }
        return
    }
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).cardSurface(AppShapes.large).padding(AppSpacing.xxl).testTag("next-lesson"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            text()
            path()
            buttons()
        }
        if (picture) JourneyIllustration(Modifier.width(SCENE_WIDTH).fillMaxHeight().testTag("hero-scene"))
    }
}

/**
 * A course's lessons as a path of stops: those read filled with a tick, [current] a large stop to play, the rest
 * numbers still to come. A long course shows the ten stops around the current one. Each stop opens its lesson.
 */
@Composable
private fun LessonPath(lessons: List<LessonProgress>, current: LessonProgress, onOpenLesson: (lessonId: String) -> Unit, compact: Boolean) {
    val at = lessons.indexOf(current).coerceAtLeast(0)
    val first = (at - PATH_BEFORE).coerceAtMost(lessons.size - PATH_STOPS).coerceAtLeast(0)
    val shown = lessons.subList(first, minOf(lessons.size, first + PATH_STOPS))
    val small = if (compact) STOP_COMPACT else STOP
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = AppSpacing.xs).testTag("lesson-path"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.m),
    ) {
        shown.forEachIndexed { i, lesson ->
            val number = first + i + 1
            val kind = when {
                lesson === current -> StopKind.CURRENT
                lesson.status == LessonStatus.COMPLETED -> StopKind.DONE
                else -> StopKind.AHEAD
            }
            LessonStop(number, kind, if (kind == StopKind.CURRENT) small * CURRENT_SCALE else small) { onOpenLesson(lesson.lesson.id) }
        }
    }
}

private enum class StopKind { DONE, CURRENT, AHEAD }

/** One stop of the lesson path, on a darker rim below it as if raised; the current one ringed as well. */
@Composable
private fun LessonStop(number: Int, kind: StopKind, size: Dp, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (fill, rim, content) = when (kind) {
        StopKind.DONE -> Triple(AppAccents.green, lerp(AppAccents.green, AppAccents.ink, RIM_SHADE), AppAccents.onAccent)
        StopKind.CURRENT -> Triple(AppAccents.orange, lerp(AppAccents.orange, AppAccents.ink, RIM_SHADE), AppAccents.onAccent)
        StopKind.AHEAD -> Triple(colors.surfaceContainerHigh, colors.outlineVariant, colors.onSurfaceVariant)
    }
    val halo = AppAccents.orange.copy(alpha = AppAlpha.line)
    Box(
        Modifier.padding(if (kind == StopKind.CURRENT) AppSpacing.xs else 0.dp).size(size)
            .drawBehind {
                val radius = this.size.minDimension / 2
                if (kind == StopKind.CURRENT) drawCircle(halo, radius + HALO.toPx(), center)
                drawCircle(rim, radius, center + Offset(0f, RIM.toPx()))
                drawCircle(fill, radius, center)
            }
            .clip(AppShapes.circle).clickable(onClick = onClick).testTag("lesson-stop-$number"),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            StopKind.DONE -> Icon(Icons.Default.Check, contentDescription = tr("Lesson {0}", number), tint = content, modifier = Modifier.size(AppSizes.icon))
            StopKind.CURRENT -> Icon(AppIcons.PlayArrow, contentDescription = tr("Lesson {0}", number), tint = content, modifier = Modifier.size(AppSizes.iconXLarge))
            StopKind.AHEAD -> Text("$number", style = AppText.smallTitle, color = content)
        }
    }
}

/** How many stops of the lesson path show at most, and how many of them before the current one. */
private const val PATH_STOPS = 10
private const val PATH_BEFORE = 4

private val STOP = 46.dp
private val STOP_COMPACT = 38.dp
private const val CURRENT_SCALE = 1.22f

/** On a phone the main button takes more of the row than All lessons, so its longer label stays on one line. */
private const val PRIMARY_SHARE = 1.35f
private val RIM = 4.dp
private val HALO = 6.dp
private const val RIM_SHADE = 0.25f
private val SCENE_WIDTH = 300.dp
