package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.flashcards.LearningSteps
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.RoundSlider
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route

/** The flashcard settings on a phone: compact cards of plain rows, the explanation folded away, Reset at the bottom. */
@Composable
internal fun PhoneFlashcardSettingsScreen(settings: UserSettings, viewModel: SettingsViewModel, onNavigate: (Route) -> Unit, onBack: () -> Unit, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { AppTopBar(title = tr("Flashcards"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Daily limits and review scheduling."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PhoneCard(tr("Daily limits"), AppIcons.BarChart) {
                ControlRow(tr("New cards a day")) {
                    NumberStepper(settings.flashcardNewPerDay, 0..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("new cards a day"), step = 5) { n ->
                        viewModel.update { it.copy(flashcardNewPerDay = n) }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                val limited = settings.flashcardReviewsPerDay > 0
                ControlRow(tr("Limit reviews a day")) {
                    Switch(checked = limited, onCheckedChange = { on -> viewModel.update { it.copy(flashcardReviewsPerDay = if (on) PHONE_DEFAULT_REVIEW_LIMIT else 0) } })
                }
                if (limited) {
                    // Indented, as it hangs off the switch above.
                    ControlRow(tr("Reviews a day"), indent = 24.dp) {
                        NumberStepper(settings.flashcardReviewsPerDay, 1..UserSettings.MAX_FLASHCARDS_PER_DAY, name = tr("reviews a day"), step = 10) { n ->
                            viewModel.update { it.copy(flashcardReviewsPerDay = n) }
                        }
                    }
                }
            }
            PhoneCard(tr("Scheduling"), Icons.Default.Settings) {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Desired retention"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("${settings.flashcardRetention}%", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                RoundSlider(
                    value = settings.flashcardRetention.toFloat(),
                    onValueChange = { v -> val n = kotlin.math.round(v).toInt(); if (n != settings.flashcardRetention) viewModel.update { it.copy(flashcardRetention = n) } },
                    valueRange = UserSettings.MIN_FLASHCARD_RETENTION.toFloat()..UserSettings.MAX_FLASHCARD_RETENTION.toFloat(),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = tr("desired retention") }.testTag("phone-retention"),
                )
                HorizontalDivider(Modifier.padding(top = 4.dp), color = colors.outlineVariant)
                StepsField(tr("Learning steps"), settings.flashcardLearnSteps) { text -> viewModel.update { it.copy(flashcardLearnSteps = text) } }
                HorizontalDivider(color = colors.outlineVariant)
                StepsField(tr("Relearning steps"), settings.flashcardRelearnSteps) { text -> viewModel.update { it.copy(flashcardRelearnSteps = text) } }
                Text(tr("Use s, m, h, or d."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            PhoneCard(tr("Answer"), AppIcons.VolumeUp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Read aloud"), style = MaterialTheme.typography.bodyLarge)
                        Text(tr("Play audio when the answer is shown."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Switch(checked = settings.flashcardAutoplay, onCheckedChange = { v -> viewModel.update { it.copy(flashcardAutoplay = v) } })
                }
            }
            HowItWorks()
            OutlinedButton(onClick = onReset, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp).testTag("phone-reset-flashcards")) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("Reset to defaults"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Anki's default review limit. */
private const val PHONE_DEFAULT_REVIEW_LIMIT = 200

@Composable
private fun PhoneCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon, size = 44)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** A label with its control at the end. */
@Composable
private fun ControlRow(label: String, indent: androidx.compose.ui.unit.Dp = 0.dp, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).padding(start = indent), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        control()
    }
}

/** Steps typed as text; saved once they read as steps, with what is wrong shown until then. */
@Composable
private fun StepsField(title: String, saved: String, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(saved) }
    // Typing is left alone; a value saved from elsewhere, such as a reset, replaces what is typed.
    LaunchedEffect(saved) { if (LearningSteps.parse(text)?.let(LearningSteps::format) != saved) text = saved }
    val valid = LearningSteps.parse(text) != null
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                LearningSteps.parse(it)?.let(LearningSteps::format)?.let { steps -> if (steps != saved) onSave(steps) }
            },
            singleLine = true,
            isError = !valid,
            supportingText = if (valid) null else ({ Text(tr("Use steps such as 1m 10m")) }),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(170.dp).semantics { contentDescription = title },
        )
    }
}

/** How a card's reviews move a word's status, folded away under a row. */
@Composable
private fun HowItWorks() {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp).testTag("phone-how-it-works"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
            Text(tr("How flashcard scheduling works"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        if (open) {
            Text(
                tr("Every word at status 1 to 4 has a card. Its status follows the card: waiting a day or more is 2, a week 3, three weeks 4, and ninety days Known. Forgetting a review drops it one level. A status you set yourself wins: Known or Ignored retires the card."),
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
