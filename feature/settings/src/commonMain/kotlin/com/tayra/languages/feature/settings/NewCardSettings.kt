package com.tayra.languages.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.domain.flashcards.NewCardMix
import com.tayra.languages.core.domain.flashcards.NewCardOrder
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.i18n.tr

private fun NewCardOrder.label(): String = when (this) {
    NewCardOrder.SAVED -> tr("As saved")
    NewCardOrder.NEWEST -> tr("Newest first")
    NewCardOrder.RANDOM -> tr("Random")
}

private fun NewCardMix.label(): String = when (this) {
    NewCardMix.MIXED -> tr("Mixed with reviews")
    NewCardMix.AFTER_REVIEWS -> tr("After reviews")
    NewCardMix.BEFORE_REVIEWS -> tr("Before reviews")
}

/** The order new cards come in, and where they come among the day's reviews. */
@Composable
internal fun NewCardSettings(settings: UserSettings, viewModel: SettingsViewModel, dense: Boolean = false) {
    if (dense) AppDivider()
    SettingRow(
        tr("New card order"),
        if (dense) null else tr("Random keeps words saved from one text from all coming at once; it is shuffled again each day."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        SelectMenu(NewCardOrder.entries, settings.flashcardNewOrder, { order -> viewModel.update { it.copy(flashcardNewOrder = order) } }, optionLabel = { it.label() }, buttonModifier = Modifier.testTag("new-order"))
    }
    if (dense) AppDivider()
    SettingRow(
        tr("New cards among reviews"),
        if (dense) null else tr("Mixed spreads them evenly through the day's reviews, so a long day still brings new words early on."),
        divider = !dense,
        stackOnCompact = !dense,
        dense = dense,
    ) {
        SelectMenu(NewCardMix.entries, settings.flashcardNewMix, { mix -> viewModel.update { it.copy(flashcardNewMix = mix) } }, optionLabel = { it.label() }, buttonModifier = Modifier.testTag("new-mix"))
    }
}
