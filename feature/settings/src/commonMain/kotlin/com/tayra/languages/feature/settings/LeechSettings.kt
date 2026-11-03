package com.tayra.languages.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr

/** When a card forgotten again and again becomes a leech, and whether it is then suspended or only marked. */
@Composable
internal fun LeechCard(settings: UserSettings, viewModel: SettingsViewModel, dense: Boolean = false) {
    ContentCard(tr("Leeches"), if (dense) null else tr("Cards you keep forgetting, as Anki finds them."), icon = AppIcons.Flag, dense = dense) {
        SettingRow(
            tr("Leech threshold"),
            if (dense) null else tr("Forgotten this many times, a card is a leech and gathers under Leeches in Browse. 0 makes none."),
            stackOnCompact = !dense,
            dense = dense,
        ) {
            NumberStepper(settings.flashcardLeechThreshold, 0..UserSettings.MAX_LEECH_THRESHOLD, name = tr("leech threshold")) { n ->
                viewModel.update { it.copy(flashcardLeechThreshold = n) }
            }
        }
        if (settings.flashcardLeechThreshold > 0) {
            if (dense) AppDivider()
            SwitchSetting(
                tr("Suspend leeches"),
                tr("Off only marks them, and they keep coming up."),
                settings.flashcardLeechSuspend,
                divider = !dense,
                dense = dense,
                switchModifier = Modifier.testTag("leech-suspend"),
            ) { on -> viewModel.update { it.copy(flashcardLeechSuspend = on) } }
        }
    }
}
