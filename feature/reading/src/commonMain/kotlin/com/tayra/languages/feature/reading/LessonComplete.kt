package com.tayra.languages.feature.reading

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.exp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.courses.LessonSummary
import com.tayra.languages.core.domain.model.ReadingStage
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Shown once a lesson's last stage is checked, with confetti over the whole window: what the lesson came to, and the ways
 * on, to the course, back to the text, or to the next lesson.
 */
@Composable
internal fun LessonCompleteDialog(
    title: String,
    summary: LessonSummary,
    onClose: () -> Unit,
    onCourse: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppDialogSurface(onDismissRequest = onClose, modifier = Modifier.testTag("lesson-complete"), maxWidth = LESSON_COMPLETE_WIDTH) {
        Box {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                Box(Modifier.size(BADGE_SIZE).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconHuge))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(tr("Lesson complete"), style = AppText.pageTitle, textAlign = TextAlign.Center)
                    Text(title, style = AppText.headline, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text(tr("You completed all {0} stages.", ReadingStage.entries.size), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Stats(summary)
                if (summary.words > 0) {
                    TintedSurface(AppAccents.green, padding = PaddingValues(AppSpacing.l)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconXLarge))
                            Column {
                                Text(tr("{0} of {1} lesson words are now known", summary.known, summary.words), style = AppText.itemTitle)
                                Text(tr("Your vocabulary grew during this lesson."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                AppDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    LinkText(tr("Back to course"), onCourse, modifier = Modifier.testTag("lesson-complete-course"))
                    Spacer(Modifier.weight(1f))
                    SecondaryButton(tr("Review lesson"), onClick = onClose, modifier = Modifier.testTag("lesson-complete-review"))
                    if (summary.next != null) {
                        PrimaryButton(tr("Next lesson"), onClick = onNext, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward, modifier = Modifier.testTag("lesson-complete-next"))
                    }
                }
            }
            AppIconButton(Icons.Default.Close, tr("Close"), onClick = onClose, style = IconButtonStyle.Outlined, modifier = Modifier.align(Alignment.TopEnd).padding(AppSpacing.l))
        }
        Confetti()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stats(summary: LessonSummary) {
    val percent = if (summary.quizAsked > 0) (100f * summary.quizRight / summary.quizAsked).roundToInt() else null
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        maxItemsInEachRow = 3,
    ) {
        val cell = Modifier.weight(1f).widthIn(min = STAT_MIN_WIDTH)
        Stat(AppIcons.Book, summary.learning.toString(), tr("New words"), tr("Being learned now"), cell)
        Stat(AppIcons.Style, summary.flashcards.toString(), tr("Flashcards"), tr("Ready to review"), cell)
        Stat(
            AppIcons.School,
            if (percent != null) "${summary.quizRight} / ${summary.quizAsked}" else "—",
            tr("Quiz answers"),
            if (percent != null) tr("{0}% correct", percent) else tr("No answers yet"),
            cell,
        )
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String, note: String, modifier: Modifier) {
    Column(modifier.cardSurface().padding(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        IconTile(icon, size = 40)
        Spacer(Modifier.size(AppSpacing.s))
        Text(value, style = AppText.screenTitle)
        Text(label, style = AppText.itemTitle)
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Fireworks of confetti over the whole window: bursts one after another, each flying out all round,
 * slowing, drifting down and fading, a few seconds in all, over everything, a dialog too. Opened from inside a dialog, it stays above it on every platform.
 */
@Composable
internal fun Confetti() {
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) { time.animateTo(1f, tween(CONFETTI_MILLIS, easing = LinearEasing)) }
    if (time.value >= 1f) return
    Popup(popupPositionProvider = WindowOrigin, properties = PopupProperties(focusable = false)) {
        ConfettiPieces(time.value, Modifier.fillMaxSize())
    }
}

/** Places a popup at the window's corner, whatever opened it. */
private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) = IntOffset.Zero
}

@Composable
private fun ConfettiPieces(progress: Float, modifier: Modifier) {
    val bursts = remember { List(BURSTS) { Burst(it, Random.Default) } }
    val palette = AppAccents.sequence
    Canvas(modifier.testTag("confetti")) {
        val now = progress * CONFETTI_MILLIS / 1000f
        // Distances go by the window's shorter side, so a burst is round on any window.
        val reach = minOf(size.width, size.height)
        val unit = PIECE_LENGTH.toPx()
        bursts.forEach { burst ->
            val t = now - burst.delay
            if (t <= 0f || t >= LIFE) return@forEach
            val centre = Offset(size.width * burst.x, size.height * burst.y)
            // The first moment is a bright flash where the burst opens.
            if (t < FLASH) drawCircle(palette[burst.colors.first() % palette.size], radius = reach * FLASH_REACH * (t / FLASH), center = centre, alpha = 1f - t / FLASH)
            // Air slows the pieces fast, then they drift down and fade.
            val travelled = (1f - exp(-DRAG * t)) / DRAG
            val alpha = ((LIFE - t) / (LIFE * FADE)).coerceIn(0f, 1f)
            burst.pieces.forEach { piece ->
                val x = centre.x + piece.dx * reach * travelled
                val y = centre.y + piece.dy * reach * travelled + reach * GRAVITY * t * t / 2
                val w = unit * piece.scale
                val h = w * piece.ratio
                val color = palette[burst.colors[piece.color] % palette.size]
                rotate(piece.angle + piece.spin * t, pivot = Offset(x, y)) {
                    drawRect(color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = alpha)
                }
            }
        }
    }
}

/** One firework: where and when it bursts, its two colours, and its pieces flying out all round. */
private class Burst(index: Int, random: Random) {
    val x = 0.15f + random.nextFloat() * 0.7f
    val y = 0.12f + random.nextFloat() * 0.38f
    // One after another, the first at once.
    val delay = index * BURST_GAP + random.nextFloat() * BURST_GAP / 2
    val colors = List(2) { random.nextInt(0, 64) }
    val pieces = List(PIECES_PER_BURST) { Piece(random) }
}

/** A piece of a burst: the way it flies, as a share of the window each second, and how it looks. */
private class Piece(random: Random) {
    private val heading = random.nextFloat() * 2 * PI.toFloat()
    // Most pieces fly near the edge of the burst, which keeps its ring.
    private val speed = SPEED * (0.55f + 0.45f * sqrt(random.nextFloat()))
    val dx = cos(heading) * speed
    val dy = sin(heading) * speed
    val angle = random.nextFloat() * 360f
    val spin = (random.nextFloat() - 0.5f) * 900f
    val scale = 0.5f + random.nextFloat() * 0.7f
    val ratio = 0.4f + random.nextFloat() * 0.3f
    val color = random.nextInt(0, 2)
}

private const val BURSTS = 7
private const val PIECES_PER_BURST = 80
private const val BURST_GAP = 0.12f
private const val LIFE = 2.6f
private const val FLASH = 0.18f
private const val FLASH_REACH = 0.035f
private const val SPEED = 1.2f
private const val DRAG = 3.2f
private const val GRAVITY = 0.12f
private const val FADE = 0.45f
private const val CONFETTI_MILLIS = 3_500
private val PIECE_LENGTH = 14.dp
private val BADGE_SIZE = 88.dp
private val STAT_MIN_WIDTH = 160.dp
private val LESSON_COMPLETE_WIDTH = 900.dp
