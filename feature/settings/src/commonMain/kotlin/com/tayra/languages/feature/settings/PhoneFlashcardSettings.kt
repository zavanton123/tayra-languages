package com.tayra.languages.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.components.Reveal

/** The flashcard settings on a phone: compact cards of plain rows, the explanation folded away, Reset at the bottom. */
@Composable
internal fun PhoneFlashcardSettingsScreen(settings: UserSettings, viewModel: SettingsViewModel, onNavigate: (Route) -> Unit, onBack: () -> Unit, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { AppTopBar(title = tr("Flashcard settings"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Daily limits and review scheduling."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            ContentCard(tr("Daily limits"), subtitle = null, icon = AppIcons.BarChart, dense = true) {
                SettingRow(tr("New cards a day"), dense = true) {
                    NumberStepper(settings.flashcardNewPerDay, 0..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("new cards a day"), step = 5) { n ->
                        viewModel.update { it.copy(flashcardNewPerDay = n) }
                    }
                }
                AppDivider()
                val limited = settings.flashcardReviewsPerDay > 0
                SwitchSetting(tr("Limit reviews a day"), description = null, checked = limited, dense = true) { on ->
                    viewModel.update { it.copy(flashcardReviewsPerDay = if (on) PHONE_DEFAULT_REVIEW_LIMIT else 0) }
                }
                if (limited) {
                    // Indented, as it hangs off the switch above.
                    SettingRow(tr("Reviews a day"), dense = true, indent = AppSpacing.xxl) {
                        NumberStepper(settings.flashcardReviewsPerDay, 1..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("reviews a day"), step = 10) { n ->
                            viewModel.update { it.copy(flashcardReviewsPerDay = n) }
                        }
                    }
                }
            }
            ContentCard(tr("Scheduling"), subtitle = null, icon = Icons.Default.Settings, dense = true) {
                SliderSetting(
                    tr("Desired retention"),
                    "${settings.flashcardRetention}%",
                    settings.flashcardRetention.toFloat(),
                    UserSettings.MIN_FLASHCARD_RETENTION.toFloat()..UserSettings.MAX_FLASHCARD_RETENTION.toFloat(),
                    onChange = { v -> viewModel.update { it.copy(flashcardRetention = kotlin.math.round(v).toInt()) } },
                    modifier = Modifier.padding(top = AppSpacing.xs),
                    step = 1f,
                    sliderModifier = Modifier.semantics { contentDescription = tr("desired retention") }.testTag("phone-retention"),
                )
                AppDivider(Modifier.padding(top = AppSpacing.xs))
                StepsRow(tr("Learning steps"), description = null, saved = settings.flashcardLearnSteps, dense = true) { text -> viewModel.update { it.copy(flashcardLearnSteps = text) } }
                AppDivider()
                StepsRow(tr("Relearning steps"), description = null, saved = settings.flashcardRelearnSteps, dense = true) { text -> viewModel.update { it.copy(flashcardRelearnSteps = text) } }
                Text(tr("Use s, m, h, or d."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = AppSpacing.xs))
            }
            ContentCard(tr("Answer"), subtitle = null, icon = AppIcons.VolumeUp, dense = true) {
                SwitchSetting(tr("Read aloud"), tr("Play audio when the answer is shown."), settings.flashcardAutoplay, dense = true) { v ->
                    viewModel.update { it.copy(flashcardAutoplay = v) }
                }
                AppDivider()
                SwitchSetting(
                    tr("Two answer buttons"),
                    tr("Show only Again and Good."),
                    settings.flashcardTwoButtons,
                    dense = true,
                    switchModifier = Modifier.testTag("phone-two-buttons"),
                ) { v -> viewModel.update { it.copy(flashcardTwoButtons = v) } }
            }
            HowItWorks()
            SecondaryButton(
                tr("Reset to defaults"),
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().testTag("phone-reset-flashcards"),
                icon = Icons.Default.Refresh,
                size = ButtonSize.Large,
            )
        }
    }
}

/** Anki's default review limit. */
private const val PHONE_DEFAULT_REVIEW_LIMIT = 200

/** How a card's reviews move a word's status, folded away under a row. */
@Composable
internal fun HowItWorks() {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().cardSurface()) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(AppSpacing.l).testTag("phone-how-it-works"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
            Text(tr("How flashcard scheduling works"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        Reveal(open) {
            Text(
                tr("Every word at status 1 to 4 has a card. Its status follows the card: waiting a day or more is 2, a week 3, three weeks 4, and ninety days Known. Forgetting a review drops it one level. A status you set yourself wins: Known or Ignored retires the card."),
                Modifier.padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
