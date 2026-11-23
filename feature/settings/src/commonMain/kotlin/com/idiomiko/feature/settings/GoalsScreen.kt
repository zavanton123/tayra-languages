package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSizes
import org.koin.compose.viewmodel.koinViewModel

/** Settings: the daily goal that Home measures today's reading and flashcards against, and the pages a book's stats are sampled from. */
@Composable
fun GoalsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = tr("Goals"), onNavigate = onNavigate, onBack = onBack.takeIf { compact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.GOALS, padding, onNavigate) { inner ->
            PageColumn(inner) {
                // A phone's bar holds the title, and the header would only leave a gap above the card.
                if (!compact) ScreenHeader(tr("Goals"), null, onBackToSettings = onBack)
                ContentCard(tr("Set goals"), null, icon = AppIcons.Target, modifier = Modifier.widthIn(max = AppSizes.readableWidth), dense = compact) {
                    SettingRow(
                        tr("Daily goal"),
                        tr("Minutes of reading and flashcards a day. Home shows how far today has got."),
                        stackOnCompact = true,
                        dense = compact,
                        modifier = Modifier.testTag("daily-goal"),
                    ) {
                        DailyGoalStepper(settings, viewModel)
                    }
                    SettingRow(
                        tr("Book stats page sample size"),
                        tr("Number of pages used for book statistics."),
                        divider = true,
                        stackOnCompact = true,
                        dense = compact,
                        modifier = Modifier.testTag("stats-sample-size"),
                    ) {
                        StatsSampleStepper(settings, viewModel)
                    }
                }
            }
        }
    }
}
