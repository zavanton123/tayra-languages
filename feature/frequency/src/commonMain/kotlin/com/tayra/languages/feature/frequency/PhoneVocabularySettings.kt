package com.tayra.languages.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppShapes

/**
 * The vocabulary level page on a phone: the estimate with its slider and sample words, the
 * coloured preview, the way to the word frequency page, and one button to set the level.
 */
@Composable
internal fun PhoneVocabularySettingsScreen(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var confirming by remember { mutableStateOf(false) }
    val list = state.list
    Scaffold(
        topBar = { AppTopBar(title = tr("Vocabulary"), onNavigate = onNavigate, onBack = { onNavigate(Route.Settings) }, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (list == null) {
                Text(
                    if (state.languageName.isEmpty()) tr("There is no word frequency list for this language, so a level cannot be set.")
                    else tr("There is no word frequency list for {0}, so a level cannot be set.", languageInSentence(state.languageName, LanguageCase.GENITIVE)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                return@Column
            }
            val level = state.shownLevel
            val choices = remember(list) { VocabularyLevelService.choices(list.words.size) }
            val index = choices.indexOf(level).coerceAtLeast(0)
            AppCard {
                Text(tr("Estimated vocabulary"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatCount(level), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(trPlural(level, "word", "words"), Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                }
                Text(levelName(level), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LevelSlider(choices, index, onPick, named = setOf(0, 500, 1000, 3000, 5000, choices.last()), sparse = true, caption = false)
                Spacer(Modifier.height(14.dp))
                val words = remember(list, level, choices) {
                    val previous = if (index == 0) 0 else choices[index - 1]
                    if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
                }
                Column(
                    Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = 0.06f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    words.take(6).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { word ->
                                Text(
                                    word,
                                    Modifier.weight(1f).clip(AppShapes.pill).background(colors.primary.copy(alpha = 0.10f)).padding(vertical = 10.dp, horizontal = 8.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text(
                        if (level == 0) tr("Not familiar yet? Start from scratch, or move the slider.") else tr("Move the slider until these words feel familiar."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            AppCard {
                Text(tr("Reading preview"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (state.example.isEmpty()) {
                    Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
                } else {
                    Text(
                        colouredExample(state, level),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    PreviewLegend(AppAccents.green, tr("Known"))
                    PreviewLegend(colors.primary, tr("New"))
                    PreviewLegend(colors.onSurfaceVariant.copy(alpha = 0.55f), tr("Outside frequency list"))
                }
            }
            Row(
                Modifier.fillMaxWidth().cardSurface(AppShapes.card)
                    .clickable { onNavigate(Route.WordFrequency) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(AppIcons.ViewList, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                Text(tr("Explore word frequency"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            PrimaryButton(
                if (state.saving) tr("Saving…") else tr("Set vocabulary level"),
                onClick = { confirming = true },
                enabled = state.picked != null && !state.saving,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("set-vocabulary-level"),
            )
        }
    }
    val picked = state.picked
    if (confirming && picked != null) {
        LevelConfirmDialog(from = state.level, to = picked, first = !state.chosen, onConfirm = { confirming = false; onSave() }, onDismiss = { confirming = false })
    }
}
