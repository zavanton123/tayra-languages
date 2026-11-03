package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.domain.flashcards.EasyDay
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppSpacing

private fun EasyDay.label(): String = when (this) {
    EasyDay.NORMAL -> tr("Normal")
    EasyDay.REDUCED -> tr("Reduced")
    EasyDay.MINIMUM -> tr("Minimum")
}

/** The weekdays, Monday first, as short as a chip has room for. */
private fun weekdays(): List<String> = listOf(tr("Mon"), tr("Tue"), tr("Wed"), tr("Thu"), tr("Fri"), tr("Sat"), tr("Sun"))

/**
 * Load balancing, on or off, and how much each weekday may be given: a day kept light is chosen
 * only where a review may move to another, within the days its fuzz allows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadBalancingSettings(settings: UserSettings, viewModel: SettingsViewModel, dense: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    SwitchSetting(
        tr("Load balancing"),
        tr("Each review goes to the lighter of the days it may come on, so every day has about as many."),
        settings.flashcardLoadBalancing,
        dense = dense,
        switchModifier = Modifier.testTag("load-balancing"),
    ) { on -> viewModel.update { it.copy(flashcardLoadBalancing = on) } }
    val days = EasyDay.parse(settings.flashcardEasyDays)
    // Every day kept light would leave nowhere to go.
    var noNormal by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Easy days"), style = if (dense) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
        Text(
            tr("Days to keep light: a reduced day takes about half a day's reviews, a minimum day as few as can be. Only answers from now on are moved, within the days they may come on."),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            weekdays().forEachIndexed { index, weekday ->
                SelectMenu(
                    EasyDay.entries,
                    days[index],
                    onSelect = { day ->
                        val chosen = days.toMutableList().also { it[index] = day }
                        noNormal = EasyDay.NORMAL !in chosen
                        if (!noNormal) viewModel.update { it.copy(flashcardEasyDays = EasyDay.format(chosen)) }
                    },
                    optionLabel = { it.label() },
                    value = "$weekday · ${days[index].label()}",
                    active = days[index] != EasyDay.NORMAL,
                    buttonModifier = Modifier.testTag("easy-day-$index"),
                )
            }
        }
        if (noNormal) Text(tr("At least one day must stay Normal."), style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.testTag("easy-days-no-normal"))
    }
}
