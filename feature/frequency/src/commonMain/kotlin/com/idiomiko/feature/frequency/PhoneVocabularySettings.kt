package com.idiomiko.feature.frequency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * The vocabulary level page on a phone: the levels one under another with the line on the one set or
 * picked, the exact number of words folded away, one button to set the level, and the way to the word
 * frequency page.
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
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            if (list == null) {
                Text(
                    if (state.languageName.isEmpty()) tr("There is no word frequency list for this language, so a level cannot be set.")
                    else tr("There is no word frequency list for {0}, so a level cannot be set.", languageInSentence(state.languageName, LanguageCase.GENITIVE)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                return@ScrollColumn
            }
            AppCard {
                Text(tr("Your level"), style = AppText.sectionTitle)
                Spacer(Modifier.height(AppSpacing.l))
                LevelPicker(state.edges, state.selectedEdge(), onPick, compact = true, current = state.currentEdge())
                Spacer(Modifier.height(AppSpacing.l))
                SettingsLevelStrip(state)
            }
            ExactLevelSection(state, list, onPick)
            PrimaryButton(
                if (state.saving) tr("Saving…") else setLevelLabel(state),
                onClick = { confirming = true },
                enabled = state.picked != null && !state.saving,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth().testTag("set-vocabulary-level"),
                size = ButtonSize.Large,
            )
        }
    }
    val picked = state.picked
    if (confirming && picked != null) {
        LevelConfirmDialog(from = state.level, to = picked, first = !state.chosen, onConfirm = { confirming = false; onSave() }, onDismiss = { confirming = false })
    }
}
