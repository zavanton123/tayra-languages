package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppAlpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.tayra.languages.core.domain.practice.PracticeChecker
import com.tayra.languages.core.domain.practice.SpeechRetry
import com.tayra.languages.core.domain.practice.WordResult
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * An answer said aloud: a button to start and stop hearing it, what was [heard] or why it is to be
 * said again, and the way to type it instead. Its parts are tagged "[tag]-speak", "[tag]-heard"
 * and "[tag]-type-instead".
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
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!answered && !recognising) {
            PrimaryButton(
                if (listening) tr("Stop") else tr("Speak"),
                onClick = if (listening) onStop else onListen,
                icon = AppIcons.Mic,
                size = ButtonSize.Large,
                modifier = buttonModifier.testTag("$tag-speak"),
            )
        }
        if (listening && level != null) MicrophoneMeter(level, Modifier.testTag("$tag-microphone"))
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
 * The microphone while it is listened to: a dot that it is on, a row of bars rising with the sound
 * just heard, and whether any came through, so silence from the wrong or a muted microphone shows
 * at once.
 */
@Composable
fun MicrophoneMeter(level: StateFlow<Float>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // The latest levels, oldest first; the bars move along as new ones come in.
    var history by remember { mutableStateOf(List(METER_BARS) { 0f }) }
    var heardSound by remember { mutableStateOf(false) }
    var waited by remember { mutableStateOf(0L) }
    LaunchedEffect(level) {
        while (true) {
            val now = level.value
            history = history.drop(1) + now
            if (now >= SOUND_LEVEL) heardSound = true
            waited += METER_TICK_MS
            delay(METER_TICK_MS)
        }
    }
    val sounding = history.takeLast(RECENT_BARS).any { it >= SOUND_LEVEL }
    val (status, tint) = when {
        sounding -> tr("Sound detected") to AppAccents.green
        !heardSound && waited >= SILENCE_WARNING_MS -> tr("No sound yet. Check the microphone.") to AppAccents.amber
        else -> tr("Speak now") to colors.onSurfaceVariant
    }
    Column(
        modifier.widthIn(max = METER_WIDTH).fillMaxWidth().clip(AppShapes.panel).background(colors.primary.copy(alpha = AppAlpha.wash))
            .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.line)), AppShapes.panel).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            StatusDot(AppAccents.green, size = AppSizes.dotLarge)
            Text(tr("Microphone active"), style = AppText.itemTitle, modifier = Modifier.weight(1f))
            Row(Modifier.height(BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BAR_GAP)) {
                history.forEach { value ->
                    // A quiet bar keeps a stub, so the row reads as a meter even in silence.
                    val height by animateFloatAsState(MIN_BAR + (1f - MIN_BAR) * value, tween(METER_TICK_MS.toInt()))
                    Box(Modifier.width(BAR_WIDTH).fillMaxHeight(height).clip(AppShapes.small).background(colors.primary))
                }
            }
        }
        Text(status, style = AppText.emphasis, color = tint, modifier = Modifier.testTag("microphone-status"))
    }
}

private const val METER_BARS = 16
private const val RECENT_BARS = 6
private const val METER_TICK_MS = 80L
private const val SILENCE_WARNING_MS = 3_000L
private const val SOUND_LEVEL = 0.2f
private const val MIN_BAR = 0.15f
private val METER_WIDTH = 480.dp
private val BAR_HEIGHT = 28.dp
private val BAR_WIDTH = 4.dp
private val BAR_GAP = 3.dp

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
