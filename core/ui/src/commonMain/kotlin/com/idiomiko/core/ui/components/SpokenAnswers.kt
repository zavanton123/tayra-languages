package com.idiomiko.core.ui.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.PI
import com.idiomiko.core.ui.theme.AppElevation
import com.idiomiko.core.ui.theme.AppMotion
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Icon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppAlpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import com.idiomiko.core.domain.practice.PracticeChecker
import com.idiomiko.core.domain.practice.SpeechRetry
import com.idiomiko.core.domain.practice.WordResult
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * An answer said aloud: the microphone button, held while speaking or tapped to start and again to finish;
 * what was [heard] or why it is to be said again, and the way to type it instead. Its parts are tagged
 * "[tag]-speak", "[tag]-heard" and "[tag]-type-instead".
 */
@Composable
fun SpeakAnswer(
    listening: Boolean,
    recognising: Boolean,
    heard: String,
    retry: SpeechRetry?,
    answered: Boolean,
    onListen: () -> Unit,
    onStop: () -> Unit,
    onTypeInstead: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    rightToLeft: Boolean = false,
    buttonModifier: Modifier = Modifier,
    /** How loud the microphone is while listening; with it, a meter shows the learner is heard. */
    level: StateFlow<Float>? = null,
    /** Whether the microphone is open yet once listening starts; without it, at once. */
    ready: StateFlow<Boolean>? = null,
) {
    val hearing = ready?.collectAsState()?.value ?: true
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!answered && !recognising) MicrophoneButton(listening, hearing, onListen, onStop, buttonModifier.testTag("$tag-speak"))
        if (listening && level != null) MicrophoneMeter(level, Modifier.testTag("$tag-microphone"), ready = hearing)
        val note = when {
            recognising -> tr("Recognising…")
            listening && level != null -> null
            listening -> tr("Listening…")
            heard.isNotBlank() -> tr("Heard: {0}", heard)
            retry == SpeechRetry.NOTHING -> tr("Nothing was heard. Try again.")
            retry == SpeechRetry.MUTED -> tr("The microphone gave no sound. Check it and try again.")
            retry == SpeechRetry.FAILED -> tr("That could not be made out. Try again.")
            else -> null
        }
        note?.let {
            Text(
                it,
                Modifier.testTag("$tag-heard"),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = if (rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (!answered) QuietButton(tr("Type instead"), onClick = onTypeInstead, modifier = buttonModifier.testTag("$tag-type-instead"))
    }
}

/**
 * The microphone. Held down, it listens until let go; tapped, until tapped again, or until a phone's
 * recogniser hears the speaking end. Until the microphone is [ready] it asks the learner to wait, as
 * words said before then are lost, and a buzz says when to start. It breathes and sends rings out
 * while it hears; it keeps still when motion is reduced.
 */
@Composable
private fun MicrophoneButton(listening: Boolean, ready: Boolean, onListen: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    // The gesture outlives a change of state: a press that starts listening is still held once it has.
    val isListening by rememberUpdatedState(listening)
    val listen by rememberUpdatedState(onListen)
    val stop by rememberUpdatedState(onStop)
    // Listening for as long as the button is held, rather than until it is tapped again.
    var holding by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val hearing = listening && ready
    LaunchedEffect(hearing) { if (hearing) haptics.performHapticFeedback(HapticFeedbackType.ToggleOn) }
    LaunchedEffect(listening) { if (!listening) holding = false }
    val label = when {
        !listening -> tr("Hold to speak")
        !ready -> tr("Get ready…")
        holding -> tr("Release when you're done")
        else -> tr("Finish")
    }
    val action = if (listening) tr("Stop") else tr("Speak")
    val reduced = AppMotion.reduced
    val beat by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(PULSE_MS, easing = LinearEasing)))
    val scale by animateFloatAsState(
        when {
            listening -> 1f
            pressed -> PRESSED_SCALE
            hovered -> HOVERED_SCALE
            else -> 1f
        },
        AppMotion.spec(AppMotion.short),
    )
    Column(
        modifier.clip(AppShapes.panel).hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = action
                onClick { if (isListening) stop() else listen(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (isListening) {
                        if (waitForUpOrCancellation() != null) stop()
                        return@awaitEachGesture
                    }
                    pressed = true
                    listen()
                    // Let go at once, it was a tap: listening goes on until the next. Held, it ends with the hold.
                    var cancelled = false
                    val up = withTimeoutOrNull(HOLD_MS) { waitForUpOrCancellation().also { cancelled = it == null } }
                    pressed = false
                    if (up == null) {
                        if (!cancelled) {
                            holding = true
                            waitForUpOrCancellation()
                            holding = false
                        }
                        if (isListening) {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            stop()
                        }
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(STOP_ROOM), contentAlignment = Alignment.Center) {
            if (hearing && !reduced) {
                for (offset in RING_OFFSETS) {
                    val ring = (beat + offset) % 1f
                    Box(
                        Modifier.size(STOP_SIZE).graphicsLayer {
                            val grown = 1f + (RING_SCALE - 1f) * ring
                            scaleX = grown
                            scaleY = grown
                            alpha = RING_ALPHA * (1f - ring)
                        }.clip(AppShapes.circle).background(colors.primary),
                    )
                }
            }
            if (!listening) Box(Modifier.size(HALO_SIZE).clip(AppShapes.circle).background(colors.primary.copy(alpha = if (hovered) AppAlpha.soft else AppAlpha.tint)))
            val size = if (hearing && !reduced) 1f + BREATH * sin(beat * 2f * PI.toFloat()) else scale
            Box(
                Modifier.size(STOP_SIZE).graphicsLayer { scaleX = size; scaleY = size }
                    .shadow(AppElevation.raised, AppShapes.circle).clip(AppShapes.circle)
                    // Faded while the microphone opens: not yet the time to speak.
                    .background(if (listening && !ready) colors.primary.copy(alpha = AppAlpha.half) else colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                // Tapped, it finishes with another tap, so it shows a stop; held, the finger is on the microphone.
                if (hearing && !holding) Box(Modifier.size(STOP_MARK).clip(AppShapes.compact).background(colors.onPrimary))
                else Icon(AppIcons.Mic, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(AppSizes.iconHuge))
            }
        }
        Text(label, Modifier.testTag("microphone-label"), style = AppText.itemTitle, color = colors.primary)
    }
}

/**
 * The microphone while it is listened to, in one pill: a dot and "Listening", a wave rising from
 * its middle with the sound just heard, and whether any came through, so silence from the wrong
 * or a muted microphone shows at once. On a narrow screen the three stack.
 */
@Composable
fun MicrophoneMeter(level: StateFlow<Float>, modifier: Modifier = Modifier, ready: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val open by rememberUpdatedState(ready)
    // The latest levels, oldest first; the newest is drawn in the wave's middle and older ones move outward.
    var history by remember { mutableStateOf(List(WAVE_HALF) { 0f }) }
    var heardSound by remember { mutableStateOf(false) }
    var waited by remember { mutableStateOf(0L) }
    LaunchedEffect(level) {
        while (true) {
            val now = level.value
            history = history.drop(1) + now
            if (now >= SOUND_LEVEL) heardSound = true
            // Silence counts only once the microphone is open.
            if (open) waited += METER_TICK_MS
            delay(METER_TICK_MS)
        }
    }
    val sounding = history.takeLast(RECENT_LEVELS).any { it >= SOUND_LEVEL }
    val status = when {
        !ready -> MeterStatus(tr("Get ready…"), colors.onSurfaceVariant, AppIcons.Mic)
        sounding -> MeterStatus(tr("Sound detected"), AppAccents.green, Icons.Default.CheckCircle)
        !heardSound && waited >= SILENCE_WARNING_MS -> MeterStatus(tr("No sound yet. Check the microphone."), AppAccents.amber, Icons.Default.Warning)
        else -> MeterStatus(tr("Speak now"), colors.onSurfaceVariant, AppIcons.Mic)
    }
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val wide = maxWidth >= METER_ROW_WIDTH
        val shape = if (wide) AppShapes.pill else AppShapes.panel
        val pill = Modifier.clip(shape).background(colors.primary.copy(alpha = AppAlpha.wash))
            .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.line)), shape)
            .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.m)
        if (wide) {
            Row(pill, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
                ListeningLabel()
                Wave(history)
                MeterStatusText(status, Modifier.weight(1f, fill = false))
            }
        } else {
            Column(pill, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                ListeningLabel()
                Wave(history)
                MeterStatusText(status)
            }
        }
    }
}

private class MeterStatus(val text: String, val tint: Color, val icon: ImageVector)

@Composable
private fun ListeningLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        StatusDot(AppAccents.green, size = AppSizes.dotXLarge)
        Text(tr("Listening…"), style = AppText.itemTitle)
    }
}

@Composable
private fun MeterStatusText(status: MeterStatus, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Icon(status.icon, contentDescription = null, tint = status.tint, modifier = Modifier.size(AppSizes.iconMedium))
        Text(status.text, style = AppText.emphasis, color = status.tint, modifier = Modifier.testTag("microphone-status"))
    }
}

/**
 * The levels in [history] as a wave: the newest in the middle, older ones mirrored outward and
 * tapering to dots at the ends, so it reads as a voice's wave and a quiet microphone as a row of dots.
 */
@Composable
private fun Wave(history: List<Float>) {
    val color = AppAccents.onWash(AppAccents.blue)
    Row(Modifier.height(WAVE_HEIGHT), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BAR_GAP)) {
        for (bar in -(WAVE_HALF - 1)..(WAVE_HALF - 1)) {
            val fromMiddle = abs(bar)
            val taper = 1f - (fromMiddle.toFloat() / WAVE_HALF).let { it * it }
            val target = history[history.lastIndex - fromMiddle] * taper
            val rise by animateFloatAsState(target, tween(METER_TICK_MS.toInt()))
            Box(Modifier.width(BAR_WIDTH).height(BAR_WIDTH + (WAVE_HEIGHT - BAR_WIDTH) * rise).clip(AppShapes.pill).background(color))
        }
    }
}

private const val WAVE_HALF = 10
private const val RECENT_LEVELS = 6
private const val METER_TICK_MS = 80L
private const val SILENCE_WARNING_MS = 3_000L
private const val SOUND_LEVEL = 0.2f
private const val PULSE_MS = 1_600
/** A press let go sooner than this is a tap, which leaves the microphone on until the next. */
private const val HOLD_MS = 300L
private const val RING_SCALE = 1.35f
private const val RING_ALPHA = 0.3f
private const val BREATH = 0.04f
private const val HOVERED_SCALE = 1.04f
private const val PRESSED_SCALE = 0.95f
private val RING_OFFSETS = listOf(0f, 0.5f)
private val STOP_SIZE = 88.dp
private val STOP_MARK = 30.dp
private val STOP_ROOM = 120.dp
private val HALO_SIZE = 106.dp
private val METER_ROW_WIDTH = 520.dp
private val WAVE_HEIGHT = 36.dp
private val BAR_WIDTH = 4.dp
private val BAR_GAP = 4.dp

/** The sentence [expected], word by word as it was typed or heard: right, right but for accents, wrong (with what was given), or missing. */
@Composable
fun SentenceCheck(expected: String, given: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val checks = remember(expected, given) { PracticeChecker.compareWords(expected, given) }
    Text(
        buildAnnotatedString {
            checks.forEachIndexed { index, check ->
                if (index > 0) append(" ")
                when (check.result) {
                    WordResult.RIGHT -> withStyle(SpanStyle(color = AppAccents.green)) { append(check.word) }
                    WordResult.ACCENT -> withStyle(SpanStyle(color = AppAccents.amber, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                    WordResult.WRONG -> {
                        withStyle(SpanStyle(color = AppAccents.red, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                        withStyle(SpanStyle(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)) { append(" ${check.typed}") }
                    }
                    WordResult.MISSING -> withStyle(SpanStyle(color = AppAccents.red, textDecoration = TextDecoration.Underline)) { append(check.word) }
                }
            }
        },
        modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
