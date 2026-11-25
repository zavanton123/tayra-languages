package com.idiomiko.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.LevelEdge
import com.idiomiko.core.domain.frequency.LevelEdges
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.Reveal
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.core.ui.components.rememberToastState
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VocabularySettingsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit = { onNavigate(Route.Settings) }, viewModel: VocabularySettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    if (LocalWindowWidth.current.isCompact && !state.loading) {
        PhoneVocabularySettingsScreen(state, onPick = viewModel::pick, onSave = viewModel::save, onNavigate = onNavigate, onBack = onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Vocabulary settings"), onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.VOCABULARY, padding, onNavigate) { inner ->
            if (state.loading) {
                LoadingIndicator(Modifier.padding(inner))
                return@SettingsPane
            }
            PageColumn(inner) {
                VocabularySettingsContent(
                    state,
                    onPick = viewModel::pick,
                    onSave = viewModel::save,
                    onBackToSettings = { onNavigate(Route.Settings) },
                )
            }
        }
    }
}

@Composable
internal fun VocabularySettingsContent(
    state: VocabularySettingsUiState,
    onPick: (Int) -> Unit,
    onSave: () -> Unit,
    onBackToSettings: (() -> Unit)? = null,
) {
    var confirming by remember { mutableStateOf(false) }
    ScreenHeader(
        tr("Vocabulary settings"),
        null,
        onBackToSettings = onBackToSettings,
    )
    val list = state.list
    if (list == null) {
        ContentCard(
            tr("Vocabulary level"),
            if (state.languageName.isEmpty()) tr("There is no word frequency list for this language, so a level cannot be set.")
            else tr("There is no word frequency list for {0}, so a level cannot be set.", languageInSentence(state.languageName, LanguageCase.GENITIVE)),
            icon = AppIcons.BarChart,
        ) {}
        return
    }
    ContentCard(tr("Your level"), null, icon = AppIcons.BarChart) {
        LevelPicker(state.edges, state.selectedEdge(), onPick, compact = false, current = state.currentEdge())
        Spacer(Modifier.height(AppSpacing.l))
        SettingsLevelStrip(state)
        Spacer(Modifier.height(AppSpacing.l))
        ExactLevelSection(state, list, onPick)
        Spacer(Modifier.height(AppSpacing.xl))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            PrimaryButton(setLevelLabel(state), onClick = { confirming = true }, enabled = state.picked != null && !state.saving, modifier = Modifier.testTag("set-vocabulary-level"), size = ButtonSize.Large)
            if (state.saving) {
                AppSpinner(size = SpinnerSize.Small)
                Text(tr("Saving…"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val picked = state.picked
    if (confirming && picked != null) {
        LevelConfirmDialog(from = state.level, to = picked, first = !state.chosen, onConfirm = { confirming = false; onSave() }, onDismiss = { confirming = false })
    }
}

/**
 * The slider to an exact number of words, with the words around it and the sample text, folded away
 * under a row: the level cards are enough for most. Two columns on a wide window, one under the other else.
 */
@Composable
internal fun ExactLevelSection(state: VocabularySettingsUiState, list: FrequencyList, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var open by remember { mutableStateOf(false) }
    val shape = if (compact) AppShapes.card else AppShapes.panel
    Column(modifier.fillMaxWidth().clip(shape).border(AppBorders.hairline(), shape).background(colors.surface)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("exact-level-toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Icon(AppIcons.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
            Text(tr("Set an exact number of words"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        Reveal(open) {
            val gap = if (compact) AppSpacing.m else AppSpacing.l
            val inner = Modifier.padding(start = gap, end = gap, bottom = gap)
            if (LocalWindowWidth.current.isExpanded) {
                Row(inner.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    EstimateCard(list, state.shownLevel, onPick, Modifier.weight(1f).fillMaxHeight())
                    PreviewCard(state, state.shownLevel, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(inner, verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.xl)) {
                    EstimateCard(list, state.shownLevel, onPick, Modifier.fillMaxWidth())
                    PreviewCard(state, state.shownLevel, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** The card shown chosen: the level picked, else the one set; none for a number of words between two levels. */
internal fun VocabularySettingsUiState.selectedEdge(): LevelEdge? =
    if (picked == null && !chosen) null else edges.firstOrNull { it.words == shownLevel }

/** The card of the level set, when one was and it is one of the levels. */
internal fun VocabularySettingsUiState.currentEdge(): LevelEdge? = if (chosen) edges.firstOrNull { it.words == level } else null

/** Set level, or what it sets the level to once one is picked. */
@Composable
internal fun setLevelLabel(state: VocabularySettingsUiState): String {
    val picked = state.picked ?: return tr("Set level")
    return tr("Set to {0}", state.edges.firstOrNull { it.words == picked }?.code() ?: formatCount(picked))
}

/**
 * The line under the level cards: the level set, or the one picked and what setting it does. A number of
 * words that is no level's is placed between the two levels around it.
 */
@Composable
internal fun SettingsLevelStrip(state: VocabularySettingsUiState) {
    val picked = state.picked
    if (picked == null && !state.chosen) {
        LevelStrip(tr("No level set yet"), tr("Choose the level closest to yours."), icon = Icons.Default.Info)
        return
    }
    val words = state.shownLevel
    val edge = state.edges.firstOrNull { it.words == words }
    val count = formatCount(words)
    val below = LevelEdges.reached(state.edges, words).code()
    val above = state.edges.firstOrNull { it.words > words }?.code()
    val title = when {
        edge != null && picked == null -> tr("Your level: {0}", edge.code())
        edge != null -> tr("{0} level selected", edge.code())
        picked == null && above == null -> trPlural(words, "Your level: {1} word, beyond {2}", "Your level: {1} words, beyond {2}", count, below)
        picked == null -> trPlural(words, "Your level: {1} word, between {2} and {3}", "Your level: {1} words, between {2} and {3}", count, below, above)
        above == null -> trPlural(words, "{1} word selected, beyond {2}", "{1} words selected, beyond {2}", count, below)
        else -> trPlural(words, "{1} word selected, between {2} and {3}", "{1} words selected, between {2} and {3}", count, below, above)
    }
    val what = when {
        picked == null && words == 0 -> tr("No words are marked as known.")
        picked == null -> trPlural(words, "The {1} most common word is marked as known.", "The {1} most common words are marked as known.", count)
        !state.chosen && words == 0 -> tr("We'll begin with the basics and mark no words as already known.")
        !state.chosen || picked > state.level -> trPlural(words, "We'll mark the {1} most common word as known.", "We'll mark the {1} most common words as known.", count)
        words == 0 -> tr("The words the level marked as known are taken off your vocabulary, unless you have changed them since.")
        else -> tr("The words past the {0} most common that the level marked as known are taken off your vocabulary, unless you have changed them since.", count)
    }
    LevelStrip(title, what, icon = if (edge != null) Icons.Default.CheckCircle else Icons.Default.Info)
}

/** Six words spread over the ranks a level adds. */
internal fun samples(list: FrequencyList, from: Int, to: Int): List<String> {
    val span = to - from
    if (span <= 0) return emptyList()
    return (0 until 6).map { i -> list.words[(from + span * (2 * i + 1) / 12).coerceIn(from, to - 1)].word }.distinct()
}
