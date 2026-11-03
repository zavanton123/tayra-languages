package com.tayra.languages.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.flashcards.RetentionAdvisor
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The spans the helper simulates, in days. */
internal val RETENTION_SPANS = listOf(90, 365, 1095)

data class RetentionHelpState(
    val days: Int = 365,
    val advice: RetentionAdvisor.Advice? = null,
    val working: Boolean = false,
    /** No language is being learned, so there are no cards to simulate. */
    val noLanguage: Boolean = false,
)

/** Simulates the language being learned at every retention, for the span chosen, and sets the one taken. */
class RetentionHelpViewModel(private val advisor: RetentionAdvisor, private val settings: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(RetentionHelpState())
    val state: StateFlow<RetentionHelpState> = _state.asStateFlow()
    private var job: Job? = null

    fun simulate(days: Int = _state.value.days) {
        val languageId = settings.current.currentLanguageId.takeIf { it != 0L }
        if (languageId == null) return _state.update { it.copy(noLanguage = true) }
        job?.cancel()
        _state.update { it.copy(days = days, working = true) }
        job = viewModelScope.launch {
            val advice = advisor.advise(languageId, days)
            _state.update { it.copy(advice = advice, working = false) }
        }
    }

    fun use(retention: Int) {
        viewModelScope.launch {
            settings.update { it.copy(flashcardRetention = retention.coerceIn(UserSettings.MIN_FLASHCARD_RETENTION, UserSettings.MAX_FLASHCARD_RETENTION)) }
        }
    }
}

private fun spanLabel(days: Int): String = when (days) {
    90 -> tr("3 months")
    365 -> tr("1 year")
    else -> tr("3 years")
}

/**
 * The daily minutes and the words remembered at every desired retention, the current one and the
 * most efficient marked; a retention tapped on the chart shows its figures and can be taken.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RetentionHelpDialog(viewModel: RetentionHelpViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.simulate() }
    val advice = state.advice
    var chosen by remember(advice) { mutableIntStateOf(advice?.suggested ?: 0) }
    val colors = MaterialTheme.colorScheme
    AppDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.testTag("retention-help")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Help choosing desired retention"), style = AppText.screenTitle)
            Text(
                tr("Your cards and new words are simulated ahead at every retention, answered as you answer them. A higher retention keeps more words but costs more time."),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            SegmentedToggle(RETENTION_SPANS.map { Segment(it, label = spanLabel(it), testTag = "span-$it") }, state.days, onSelect = { viewModel.simulate(it) })
            when {
                state.noLanguage -> Text(tr("Choose a language to learn first."), style = MaterialTheme.typography.bodyMedium)
                advice == null || state.working -> {
                    Text(tr("Simulating…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    LoadingIndicator(Modifier.height(CHART_HEIGHT))
                }
                else -> {
                    val point = advice.points.first { it.retention == chosen }
                    val current = advice.points.firstOrNull { it.retention == advice.current }
                    RetentionChart(advice, chosen) { chosen = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        Key(tr("Minutes a day"), MINUTES)
                        Key(tr("Words remembered at the end"), WORDS)
                        Key(tr("Your setting"), colors.outline)
                        Key(tr("Most words for the time"), AppAccents.green)
                    }
                    Text(
                        tr("At {0}%: {1} minutes a day, about {2} words remembered after {3}.", chosen, oneDecimal(point.minutesPerDay), formatCount(point.remembered.roundToInt()), spanLabel(advice.days)),
                        style = AppText.itemTitle,
                        modifier = Modifier.testTag("retention-chosen"),
                    )
                    if (current != null && current.retention != chosen) {
                        Text(
                            tr("Your setting of {0}%: {1} minutes a day, about {2} words.", current.retention, oneDecimal(current.minutesPerDay), formatCount(current.remembered.roundToInt())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text(
                        tr("{0}% remembers the most words for the minutes spent; tap the chart to see any other.", advice.suggested),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    if (advice.timedAnswers < ENOUGH_TIMED) {
                        Text(
                            tr("Minutes use typical answer times until about {0} of your answers are timed; {1} are so far.", ENOUGH_TIMED, advice.timedAnswers),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                QuietButton(tr("Close"), onClick = onDismiss)
                Spacer(Modifier.padding(start = AppSpacing.m))
                val taken = chosen.coerceIn(UserSettings.MIN_FLASHCARD_RETENTION, UserSettings.MAX_FLASHCARD_RETENTION)
                PrimaryButton(
                    tr("Use {0}%", taken),
                    onClick = { viewModel.use(taken); onDismiss() },
                    enabled = advice != null && !state.working,
                    modifier = Modifier.testTag("retention-use"),
                )
            }
        }
    }
}

private fun oneDecimal(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()

@Composable
private fun Key(name: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        StatusDot(tint)
        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Minutes a day and words remembered over the retentions, each against its own scale, with the
 * setting dashed, the most efficient retention in green and the one chosen ringed.
 */
@Composable
private fun RetentionChart(advice: RetentionAdvisor.Advice, chosen: Int, onChoose: (Int) -> Unit) {
    val points = advice.points
    val first = points.first().retention
    val last = points.last().retention
    val maxMinutes = points.maxOf { it.minutesPerDay }.coerceAtLeast(0.01)
    val minWords = points.minOf { it.remembered }
    val wordSpan = (points.maxOf { it.remembered } - minWords).coerceAtLeast(1.0)
    val outline = MaterialTheme.colorScheme.outline
    val chosenColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Canvas(
            Modifier.fillMaxWidth().height(CHART_HEIGHT).testTag("retention-chart").pointerInput(points) {
                detectTapGestures { offset -> onChoose((first + (offset.x / size.width * (last - first)).roundToInt()).coerceIn(first, last)) }
            },
        ) {
            fun x(retention: Int) = (retention - first).toFloat() / (last - first) * size.width
            fun line(value: (RetentionAdvisor.Point) -> Float, color: Color) {
                val path = Path()
                points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p.retention), value(p)) else path.lineTo(x(p.retention), value(p)) }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
            val top = 6.dp.toPx()
            val height = size.height - top
            line({ top + height * (1 - (it.minutesPerDay / maxMinutes).toFloat()) }, MINUTES)
            line({ top + height * (1 - ((it.remembered - minWords) / wordSpan).toFloat()) }, WORDS)
            drawLine(outline, Offset(x(advice.current), 0f), Offset(x(advice.current), size.height), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawLine(AppAccents.green, Offset(x(advice.suggested), 0f), Offset(x(advice.suggested), size.height), 2.dp.toPx())
            drawLine(chosenColor.copy(alpha = AppAlpha.half), Offset(x(chosen), 0f), Offset(x(chosen), size.height), 1.dp.toPx())
        }
        // 85% stands near the middle of 70% to 99%.
        Row(Modifier.fillMaxWidth()) {
            Text("$first%", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.weight(1f))
            Text("85%", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.weight(1f))
            Text("$last%", style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

private val MINUTES = AppAccents.orange
private val WORDS = AppAccents.blue
private val CHART_HEIGHT = 180.dp

/** About as many timed answers as fsrs-rs leans on half-way between its defaults and the learner's own. */
private const val ENOUGH_TIMED = 100

/** Opens the help choosing the desired retention. */
@Composable
internal fun RetentionHelpButton(viewModel: RetentionHelpViewModel) {
    var open by remember { mutableStateOf(false) }
    QuietButton(tr("Help me choose"), onClick = { open = true }, modifier = Modifier.testTag("retention-help-open"), icon = AppIcons.Lightbulb)
    if (open) RetentionHelpDialog(viewModel, onDismiss = { open = false })
}
