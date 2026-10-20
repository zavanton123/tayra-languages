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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
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
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.TayraTheme

/**
 * The vocabulary level page on a phone: the estimate with its slider and sample words, the
 * coloured preview, the way to the word frequency page, and one button to set the level.
 */
@Composable
internal fun PhoneVocabularySettingsScreen(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var confirming by remember { mutableStateOf(false) }
    val list = state.list
    Scaffold(
        topBar = { AppTopBar(title = tr("Vocabulary settings"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
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
                    Text(formatCount(level), style = AppText.figure, color = colors.primary)
                    Spacer(Modifier.width(AppSpacing.m))
                    Text(trPlural(level, "word", "words"), Modifier.padding(bottom = AppSpacing.s), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
                }
                Text(levelName(level), style = AppText.sectionTitle)
                Spacer(Modifier.height(AppSpacing.s))
                LevelSlider(choices, index, onPick, named = landmarks(choices), sparse = true, caption = false)
                Spacer(Modifier.height(AppSpacing.l))
                val words = remember(list, level, choices) {
                    val previous = if (index == 0) 0 else choices[index - 1]
                    if (level == 0) samples(list, 0, minOf(100, list.words.size)) else samples(list, previous, level)
                }
                Column(
                    Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
                ) {
                    Text(if (level == 0) tr("Your first words") else tr("Words around this level"), style = AppText.itemTitle)
                    words.take(6).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                            row.forEach { word ->
                                Text(
                                    word,
                                    Modifier.weight(1f).clip(AppShapes.pill).background(colors.primary.copy(alpha = AppAlpha.tint)).padding(vertical = AppSpacing.s, horizontal = AppSpacing.s),
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
                Text(tr("Reading preview"), style = AppText.sectionTitle)
                Spacer(Modifier.height(AppSpacing.m))
                if (state.example.isEmpty()) {
                    Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
                } else {
                    Text(
                        colouredExample(state, level),
                        style = AppText.emphasis.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(AppSpacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.CenterVertically) {
                    PreviewLegend(null, tr("Known"))
                    PreviewLegend(TayraTheme.current.statusColors.background(TermStatus.UNKNOWN), tr("Unknown"))
                    PreviewLegend(colors.onSurfaceVariant, tr("Outside frequency list"))
                }
            }
            // Where the level stands, and what saving it does, right above the button that saves it.
            Text(
                listOf(
                    if (state.chosen) tr("Set to {0}", formatCount(state.level)) else tr("Not set"),
                    state.shownLevel.coerceAtLeast(1).let { n -> trPlural(n, "Saving marks the most common {1} word as known.", "Saving marks the most common {1} words as known.", formatCount(n)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            PrimaryButton(
                if (state.saving) tr("Saving…") else tr("Set vocabulary level"),
                onClick = { confirming = true },
                enabled = state.picked != null && !state.saving,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth().testTag("set-vocabulary-level"),
                size = ButtonSize.Large,
            )
            Row(
                Modifier.fillMaxWidth().cardSurface(AppShapes.card)
                    .clickable { onNavigate(Route.WordFrequency) }.padding(AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                Icon(AppIcons.ViewList, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
                Text(tr("Explore word frequency"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        }
    }
    val picked = state.picked
    if (confirming && picked != null) {
        LevelConfirmDialog(from = state.level, to = picked, first = !state.chosen, onConfirm = { confirming = false; onSave() }, onDismiss = { confirming = false })
    }
}
