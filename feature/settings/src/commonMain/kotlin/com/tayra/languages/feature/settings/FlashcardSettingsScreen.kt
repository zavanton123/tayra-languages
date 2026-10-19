package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
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
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel

/** How many flashcards a day, how reviews are timed and how the learning steps run. */
@Composable
fun FlashcardSettingsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val wide = LocalWindowWidth.current.isExpanded
    val compact = LocalWindowWidth.current.isCompact
    val reset = {
        val defaults = UserSettings()
        viewModel.update {
            it.copy(
                flashcardNewPerDay = defaults.flashcardNewPerDay,
                flashcardReviewsPerDay = defaults.flashcardReviewsPerDay,
                flashcardRetention = defaults.flashcardRetention,
                flashcardLearnSteps = defaults.flashcardLearnSteps,
                flashcardRelearnSteps = defaults.flashcardRelearnSteps,
                flashcardAutoplay = defaults.flashcardAutoplay,
                flashcardTwoButtons = defaults.flashcardTwoButtons,
            )
        }
        Unit
    }
    if (compact) {
        PhoneFlashcardSettingsScreen(settings, viewModel, onNavigate, onBack, onReset = reset)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Flashcards"), onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Flashcards"), tr("Daily limits and how reviews are scheduled."), onBackToSettings = { onNavigate(Route.Settings) }) {
                HeaderButton(tr("Reset to defaults"), Icons.Default.Refresh, onClick = reset)
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        LimitsCard(settings, viewModel)
                        SchedulingCard(settings, viewModel)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
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
    ContentCard(tr("Daily limits"), tr("How many cards a day, for each language."), icon = AppIcons.BarChart) {
        SettingRow(tr("New cards a day"), tr("Words shown as a flashcard for the first time."), stackOnCompact = true) {
            NumberStepper(settings.flashcardNewPerDay, 0..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("new cards a day"), step = 5) { n ->
                viewModel.update { it.copy(flashcardNewPerDay = n) }
            }
        }
        val limited = settings.flashcardReviewsPerDay > 0
        SwitchSetting(tr("Limit reviews a day"), tr("Off shows every review that is due."), limited, divider = true) { on ->
            viewModel.update { it.copy(flashcardReviewsPerDay = if (on) DEFAULT_REVIEW_LIMIT else 0) }
        }
        if (limited) {
            SettingRow(tr("Reviews a day"), tr("Reviews left over wait for the next day."), stackOnCompact = true) {
                NumberStepper(settings.flashcardReviewsPerDay, 1..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("reviews a day"), step = 10) { n ->
                    viewModel.update { it.copy(flashcardReviewsPerDay = n) }
                }
            }
        }
    }
}

/** Anki's default review limit. */
private const val DEFAULT_REVIEW_LIMIT = 200
private val STEPS_FIELD = 220.dp
private val STEPS_FIELD_DENSE = 170.dp

@Composable
private fun SchedulingCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Scheduling"), tr("Reviews are timed with FSRS, as in Anki."), icon = Icons.Default.Settings) {
        // The slider gets the card's width, its name and value above it, as on the phone.
        SliderSetting(
            tr("Desired retention"),
            "${settings.flashcardRetention}%",
            settings.flashcardRetention.toFloat(),
            UserSettings.MIN_FLASHCARD_RETENTION.toFloat()..UserSettings.MAX_FLASHCARD_RETENTION.toFloat(),
            onChange = { v -> viewModel.update { it.copy(flashcardRetention = kotlin.math.round(v).toInt()) } },
            modifier = Modifier.padding(vertical = AppSpacing.s),
            step = 1f,
            sliderModifier = Modifier.semantics { contentDescription = tr("desired retention") },
            description = tr("The share of cards you want to remember when they come up. Higher means more reviews."),
        )
        StepsRow(tr("Learning steps"), tr("Waits before a new card comes back, until it is learned."), settings.flashcardLearnSteps) { text ->
            viewModel.update { it.copy(flashcardLearnSteps = text) }
        }
        StepsRow(tr("Relearning steps"), tr("Waits before a forgotten card comes back."), settings.flashcardRelearnSteps) { text ->
            viewModel.update { it.copy(flashcardRelearnSteps = text) }
        }
        Text(
            tr("Write steps as minutes, or with s, m, h or d: 1m 10m, or 30s 5m 1h. Leave empty to let FSRS decide from the first answer."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AppSpacing.s),
        )
    }
}

/**
 * Steps typed as text; saved once they read as steps, with what is wrong shown until then. [dense]
 * is the phone's row, without a description or divider and with a narrower field.
 */
@Composable
internal fun StepsRow(title: String, description: String?, saved: String, dense: Boolean = false, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(saved) }
    // Typing is left alone; a value saved from elsewhere, such as a reset, replaces what is typed.
    LaunchedEffect(saved) { if (LearningSteps.parse(text)?.let(LearningSteps::format) != saved) text = saved }
    val valid = LearningSteps.parse(text) != null
    SettingRow(title, description, divider = !dense, stackOnCompact = !dense, dense = dense) {
        AppTextField(
            value = text,
            onValueChange = {
                text = it
                LearningSteps.parse(it)?.let(LearningSteps::format)?.let { steps -> if (steps != saved) onSave(steps) }
            },
            singleLine = true,
            isError = !valid,
            supportingText = if (valid) null else ({ Text(tr("Use steps such as 1m 10m")) }),
            modifier = Modifier.width(if (dense) STEPS_FIELD_DENSE else STEPS_FIELD).semantics { contentDescription = title },
        )
    }
}

@Composable
private fun AnswerCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Answer"), tr("What happens when a card's answer is shown."), icon = AppIcons.VolumeUp) {
        SwitchSetting(tr("Read aloud"), tr("Read the sentence, or the word, when the answer is shown."), settings.flashcardAutoplay) { v ->
            viewModel.update { it.copy(flashcardAutoplay = v) }
        }
        SwitchSetting(tr("Two answer buttons"), tr("Answer with Again and Good only; Hard and Easy are left out."), settings.flashcardTwoButtons, divider = true) { v ->
            viewModel.update { it.copy(flashcardTwoButtons = v) }
        }
    }
}

/** How reviews move a word's status, folded away as on the phone. */
@Composable
private fun StatusInfo() = HowItWorks()
