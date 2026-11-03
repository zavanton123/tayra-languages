package com.tayra.languages.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.domain.flashcards.AutoAnswer
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr

private fun AutoAnswer.label(): String = when (this) {
    AutoAnswer.AGAIN -> tr("Again")
    AutoAnswer.HARD -> tr("Hard")
    AutoAnswer.GOOD -> tr("Good")
    AutoAnswer.SKIP -> tr("Skip until tomorrow")
}

/** The timer on the card, and the card moving on by itself: its answer shown, then answered, after the seconds set. */
@Composable
internal fun AutoAdvanceSettings(settings: UserSettings, viewModel: SettingsViewModel, dense: Boolean = false) {
    if (dense) AppDivider()
    SwitchSetting(
        tr("Show a timer"),
        if (dense) null else tr("How long the card has been on show, up to the minute an answer is counted for."),
        settings.flashcardShowTimer,
        divider = !dense,
        dense = dense,
        switchModifier = Modifier.testTag("show-timer"),
    ) { on -> viewModel.update { it.copy(flashcardShowTimer = on) } }
    if (dense) AppDivider()
    SettingRow(
        tr("Show the answer after"),
        if (dense) null else tr("Seconds before a card's answer shows by itself, for cards answered by showing; 0 never."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        NumberStepper(settings.flashcardAutoReveal, 0..UserSettings.MAX_AUTO_SECONDS, name = tr("seconds before the answer shows")) { n ->
            viewModel.update { it.copy(flashcardAutoReveal = n) }
        }
    }
    if (dense) AppDivider()
    SettingRow(
        tr("Answer after"),
        if (dense) null else tr("Seconds the answer shows before the card is answered by itself; 0 never. A word typed or said gets the answer it earned."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        NumberStepper(settings.flashcardAutoAnswer, 0..UserSettings.MAX_AUTO_SECONDS, name = tr("seconds before the card is answered")) { n ->
            viewModel.update { it.copy(flashcardAutoAnswer = n) }
        }
    }
    if (settings.flashcardAutoAnswer > 0) {
        if (dense) AppDivider()
        SettingRow(tr("Answered as"), divider = !dense, dense = dense) {
            SelectMenu(AutoAnswer.entries, settings.flashcardAutoAnswerAction, { action -> viewModel.update { it.copy(flashcardAutoAnswerAction = action) } }, optionLabel = { it.label() }, buttonModifier = Modifier.testTag("auto-answer-action"))
        }
    }
}
