package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.domain.flashcards.BacklogOrder
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing

private fun BacklogOrder.label(): String = when (this) {
    BacklogOrder.RELATIVE_OVERDUENESS -> tr("Relative overdueness")
    BacklogOrder.RECALL_HIGH_FIRST -> tr("Recall chance, high first")
    BacklogOrder.READING_FIRST -> tr("Words I'm reading first")
    BacklogOrder.DUE_DATE -> tr("Due date")
}

@Composable
private fun BacklogOrder.explanation(): String = when (this) {
    BacklogOrder.RELATIVE_OVERDUENESS -> tr("The cards slipping furthest for their memory come first: those most at risk of being forgotten.")
    BacklogOrder.RECALL_HIGH_FIRST -> tr("The cards you are still most likely to remember come first, to keep what can still be kept.")
    BacklogOrder.READING_FIRST -> tr("Words on the page reached in the text read last and the five pages after it come first; the rest by relative overdueness.")
    BacklogOrder.DUE_DATE -> tr("The cards due longest come first, as on any other day.")
}

/**
 * The order of the reviews due on a day with far more of them than usual: its name and what it
 * does, with the choice of the four under it.
 */
@Composable
internal fun BacklogOrderSetting(selected: BacklogOrder, onSelect: (BacklogOrder) -> Unit, dense: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Review order when behind"), style = if (dense) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
        Text(
            tr("Used when more reviews are due than twice your usual day; other days keep the due date order."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectMenu(
            BacklogOrder.entries,
            selected,
            onSelect,
            optionLabel = { it.label() },
            modifier = Modifier.fillMaxWidth(),
            buttonModifier = Modifier.testTag("backlog-order"),
        )
        Text(selected.explanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("backlog-order-explanation"))
    }
}
