package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.LearningSteps
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderStepper
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/** How many flashcards a day, how reviews are timed and how the learning steps run. */
@Composable
fun FlashcardSettingsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val wide = LocalWindowWidth.current.isExpanded
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = "Flashcards", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader("Flashcards", "Daily limits and how reviews are scheduled.", onBackToSettings = { onNavigate(Route.Settings) }) {
                HeaderButton(if (compact) "Reset" else "Reset to defaults", Icons.Default.Refresh, onClick = {
                    val defaults = UserSettings()
                    viewModel.update {
                        it.copy(
                            flashcardNewPerDay = defaults.flashcardNewPerDay,
                            flashcardReviewsPerDay = defaults.flashcardReviewsPerDay,
                            flashcardRetention = defaults.flashcardRetention,
                            flashcardLearnSteps = defaults.flashcardLearnSteps,
                            flashcardRelearnSteps = defaults.flashcardRelearnSteps,
                            flashcardAutoplay = defaults.flashcardAutoplay,
                        )
                    }
                })
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        LimitsCard(settings, viewModel)
                        SchedulingCard(settings, viewModel)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AnswerCard(settings, viewModel)
                        StatusInfo()
                    }
                }
            } else {
                LimitsCard(settings, viewModel)
                SchedulingCard(settings, viewModel)
                AnswerCard(settings, viewModel)
                StatusInfo()
            }
        }
    }
}

@Composable
private fun LimitsCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard("Daily limits", "How many cards a day, for each language.", icon = AppIcons.BarChart) {
        SettingRow("New cards a day", "Words shown as a flashcard for the first time.", stackOnCompact = true) {
            NumberStepper(settings.flashcardNewPerDay, 0..UserSettings.MAX_FLASHCARDS_PER_DAY, name = "new cards a day", step = 5) { n ->
                viewModel.update { it.copy(flashcardNewPerDay = n) }
            }
        }
        val limited = settings.flashcardReviewsPerDay > 0
        SwitchSetting("Limit reviews a day", "Off shows every review that is due.", limited, divider = true) { on ->
            viewModel.update { it.copy(flashcardReviewsPerDay = if (on) DEFAULT_REVIEW_LIMIT else 0) }
        }
        if (limited) {
            SettingRow("Reviews a day", "Reviews left over wait for the next day.", stackOnCompact = true) {
                NumberStepper(settings.flashcardReviewsPerDay, 1..UserSettings.MAX_FLASHCARDS_PER_DAY, name = "reviews a day", step = 10) { n ->
                    viewModel.update { it.copy(flashcardReviewsPerDay = n) }
                }
            }
        }
    }
}

/** Anki's default review limit. */
private const val DEFAULT_REVIEW_LIMIT = 200

@Composable
private fun SchedulingCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard("Scheduling", "Reviews are timed with FSRS, as in Anki.", icon = Icons.Default.Settings) {
        SettingRow("Desired retention", "The share of cards you want to remember when they come up. Higher means more reviews.", stackOnCompact = true) {
            SliderStepper(
                value = settings.flashcardRetention.toFloat(),
                range = UserSettings.MIN_FLASHCARD_RETENTION.toFloat()..UserSettings.MAX_FLASHCARD_RETENTION.toFloat(),
                step = 1f,
                label = "${settings.flashcardRetention}%",
                name = "desired retention",
                onChange = { v -> viewModel.update { it.copy(flashcardRetention = kotlin.math.round(v).toInt()) } },
                modifier = Modifier.width(280.dp),
            )
        }
        StepsRow("Learning steps", "Waits before a new card comes back, until it is learned.", settings.flashcardLearnSteps) { text ->
            viewModel.update { it.copy(flashcardLearnSteps = text) }
        }
        StepsRow("Relearning steps", "Waits before a forgotten card comes back.", settings.flashcardRelearnSteps) { text ->
            viewModel.update { it.copy(flashcardRelearnSteps = text) }
        }
        Text(
            "Write steps as minutes, or with s, m, h or d: 1m 10m, or 30s 5m 1h. Leave empty to let FSRS decide from the first answer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Steps typed as text; saved once they read as steps, with what is wrong shown until then. */
@Composable
private fun StepsRow(title: String, description: String, saved: String, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(saved) }
    // Typing is left alone; a value saved from elsewhere, such as a reset, replaces what is typed.
    LaunchedEffect(saved) { if (LearningSteps.parse(text)?.let(LearningSteps::format) != saved) text = saved }
    val valid = LearningSteps.parse(text) != null
    SettingRow(title, description, divider = true, stackOnCompact = true) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                LearningSteps.parse(it)?.let(LearningSteps::format)?.let { steps -> if (steps != saved) onSave(steps) }
            },
            singleLine = true,
            isError = !valid,
            supportingText = if (valid) null else ({ Text("Use steps such as 1m 10m") }),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(220.dp).semantics { contentDescription = title },
        )
    }
}

@Composable
private fun AnswerCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard("Answer", "What happens when a card's answer is shown.", icon = AppIcons.VolumeUp) {
        SwitchSetting("Read aloud", "Read the sentence, or the word, when the answer is shown.", settings.flashcardAutoplay) { v ->
            viewModel.update { it.copy(flashcardAutoplay = v) }
        }
    }
}

@Composable
private fun StatusInfo() {
    InfoBanner(
        "Every word at status 1 to 4 has a card. Its status follows the card: waiting a day or more is 2, a week 3, three weeks 4, " +
            "and ninety days Known. Forgetting a review drops it one level. A status you set yourself wins: Known or Ignored retires the card.",
    )
}
