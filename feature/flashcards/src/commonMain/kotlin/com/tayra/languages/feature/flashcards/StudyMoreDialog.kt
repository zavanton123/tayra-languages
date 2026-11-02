package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.tayra.languages.core.domain.flashcards.ExtraStudy
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.CheckSetting
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** How many cards each way of studying more gives, as the dialog asks while it is open. */
interface StudyMoreCounts {
    suspend fun newBeyondToday(): Int

    suspend fun extraCount(study: ExtraStudy): Int
}

/** The ways of studying past the day's limits, as the dialog offers them. */
private enum class More { NEW, AHEAD, FORGOTTEN, PRACTISE }

private val PRACTISED = listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4, TermStatus.WELL_KNOWN)

/**
 * Studying past the day's limits: more new cards today, the reviews of the coming days, the cards
 * forgotten lately, or words practised at random. Each choice says how many cards it gives, and
 * every answer in it is a review like any other.
 */
@Composable
internal fun StudyMoreDialog(counts: StudyMoreCounts, onAddNew: (Int) -> Unit, onStart: (ExtraStudy) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(More.NEW) }
    var newCards by remember { mutableStateOf(10) }
    var aheadDays by remember { mutableStateOf(3) }
    var forgottenDays by remember { mutableStateOf(1) }
    var statuses by remember { mutableStateOf(PRACTISED.toSet()) }
    var practised by remember { mutableStateOf(20) }
    val study = when (chosen) {
        More.NEW -> null
        More.AHEAD -> ExtraStudy.Ahead(aheadDays)
        More.FORGOTTEN -> ExtraStudy.Forgotten(forgottenDays)
        More.PRACTISE -> ExtraStudy.Practise(statuses, practised)
    }
    val waiting by produceState<Int?>(null) { value = counts.newBeyondToday() }
    val cards by produceState<Int?>(null, study) { value = study?.let { counts.extraCount(it) } }
    val available = if (chosen == More.NEW) waiting else cards
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("study-more-dialog"),
        title = { Text(tr("Study more")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Option(More.NEW, chosen, tr("More new cards today"), tr("Raise today's limit of new cards."), { chosen = it }) {
                    Amount(tr("Extra new cards"), newCards, 5..100, step = 5) { newCards = it }
                    Count(waiting?.let { trPlural(it, "{0} new card waits beyond today's limit.", "{0} new cards wait beyond today's limit.") })
                }
                Option(More.AHEAD, chosen, tr("Review ahead"), tr("Review the cards due in the coming days, the soonest first."), { chosen = it }) {
                    Amount(tr("Days ahead"), aheadDays, 1..30) { aheadDays = it }
                    Count(cards?.let { trPlural(it, "{0} card is due in that time.", "{0} cards are due in that time.") })
                }
                Option(More.FORGOTTEN, chosen, tr("Forgotten cards again"), tr("Go over the cards you answered Again; today is the first day."), { chosen = it }) {
                    Amount(tr("Days back"), forgottenDays, 1..30) { forgottenDays = it }
                    Count(cards?.let { trPlural(it, "{0} card was forgotten in that time.", "{0} cards were forgotten in that time.") })
                }
                Option(More.PRACTISE, chosen, tr("Practise words"), tr("Review answered cards of the statuses chosen, in random order."), { chosen = it }) {
                    PRACTISED.forEach { status ->
                        CheckSetting(
                            tr(status.label),
                            checked = status in statuses,
                            onChange = { on -> statuses = if (on) statuses + status else statuses - status },
                            checkboxModifier = Modifier.testTag("practise-status-${status.name}"),
                        )
                    }
                    Amount(tr("Cards"), practised, 5..200, step = 5) { practised = it }
                    Count(cards?.let { trPlural(it, "{0} card to practise.", "{0} cards to practise.") })
                }
                Text(tr("Every answer counts as a review."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            PrimaryButton(
                tr("Start"),
                onClick = {
                    if (study == null) onAddNew(newCards) else onStart(study)
                    onDismiss()
                },
                enabled = (available ?: 0) > 0,
                modifier = Modifier.testTag("study-more-start"),
            )
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}

/** One way of studying more: its name and what it does, and once chosen, what it asks. */
@Composable
private fun Option(kind: More, chosen: More, title: String, description: String, onChoose: (More) -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val active = kind == chosen
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card)
            .background(if (active) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .border(if (active) AppBorders.selected() else AppBorders.hairline(), AppShapes.card)
            .clickable { onChoose(kind) }
            .semantics { selected = active }
            .padding(AppSpacing.l)
            .testTag("study-more-${kind.name}"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(title, style = AppText.itemTitle, color = if (active) colors.primary else colors.onSurface)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        if (active) content()
    }
}

/** A number the choice asks for, named, with minus and plus. */
@Composable
private fun Amount(name: String, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        NumberStepper(value, range, name, step, onChange)
    }
}

/** How many cards the choice gives, once that is known. */
@Composable
private fun Count(text: String?) {
    Text(text ?: tr("Counting…"), Modifier.testTag("study-more-count"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
