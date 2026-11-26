package com.idiomiko.feature.frequency

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.frequency.LevelEdge
import com.idiomiko.core.domain.frequency.LevelEdges
import com.idiomiko.core.ui.components.AppScreenDialog
import com.idiomiko.core.ui.components.AppDialogSurface
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.ToastHost
import com.idiomiko.core.ui.components.rememberToastState
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Asks for the vocabulary level of the language with [languageId], just chosen to learn, when no
 * level was ever chosen for it. [onClosed] is called once a level is set or the reader puts it off;
 * put off, they are asked again the next time they choose the language.
 */
@Composable
fun VocabularyLevelPrompt(languageId: Long?, onClosed: () -> Unit, viewModel: VocabularySettingsViewModel = koinViewModel(key = "vocabulary-level-prompt")) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    if (languageId == null) return
    // The view model follows the language being learned, which has just become this one.
    val ready = !state.loading && state.languageId == languageId && state.list != null
    LaunchedEffect(languageId, state.chosen, state.saving, ready) {
        if (ready && state.chosen && !state.saving) onClosed()
    }
    // A card is always chosen, so one is picked from the start: the level reached, else Starter. The
    // levels can move when the courses arrive, and the pick then moves onto one of them.
    LaunchedEffect(ready, state.picked, state.edges) {
        if (ready && !state.chosen && state.edges.none { it.words == state.picked }) viewModel.pick(LevelEdges.reached(state.edges, state.shownLevel).words)
    }
    val later = { viewModel.forgetPick(); onClosed() }
    // On a phone the prompt is a bottom sheet; elsewhere a floating dialog.
    if (LocalWindowWidth.current.isCompact) {
        PhonePromptScreen(ready, state, onPick = viewModel::pick, onSave = viewModel::save, onLater = later)
        return
    }
    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.testTag("vocabulary-level-prompt"), maxWidth = DIALOG_WIDTH, dismissible = false) {
        if (!ready) {
            Box(Modifier.fillMaxWidth().heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
            return@AppDialogSurface
        }
        VocabularyLevelPromptContent(state, onPick = viewModel::pick, onSave = viewModel::save, onLater = later)
    }
}

/** The prompt on a phone, on the whole screen: the question, the levels one under another, and the buttons at the foot; Back puts the question off, like Skip for now. */
@Composable
private fun PhonePromptScreen(ready: Boolean, state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppScreenDialog(onDismissRequest = onLater) {
        Column(Modifier.fillMaxSize().testTag("vocabulary-level-prompt")) {
            if (!ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
                return@Column
            }
            val chosen = LevelEdges.reached(state.edges, state.shownLevel)
            // The question scrolls on a short screen; the buttons stay at the screen's foot.
            ScrollColumn(Modifier.weight(1f), contentModifier = Modifier.padding(horizontal = AppSpacing.xl).padding(top = AppSpacing.l)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), Modifier.weight(1f), style = AppText.headline)
                    AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving, style = IconButtonStyle.Tonal, tint = colors.onSurface)
                }
                Spacer(Modifier.height(AppSpacing.xl))
                LevelPicker(state.edges, chosen, onPick, compact = true)
                Spacer(Modifier.height(AppSpacing.l))
                ChosenLevelStrip(chosen)
                Spacer(Modifier.height(AppSpacing.l))
            }
            AppDivider()
            PromptButtons(state, chosen, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m), compact = true)
        }
    }
}

@Composable
internal fun VocabularyLevelPromptContent(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val chosen = LevelEdges.reached(state.edges, state.shownLevel)
    Column {
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xxxl)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)),
                    Modifier.weight(1f).padding(top = AppSpacing.s),
                    style = AppText.screenTitle,
                )
                AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving)
            }
            Spacer(Modifier.height(AppSpacing.xxl))
            val shape = AppShapes.large
            Column(
                Modifier.fillMaxWidth().clip(shape).border(AppBorders.hairline(), shape).padding(AppSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                Text(tr("Choose your level"), style = AppText.sectionTitle)
                LevelPicker(state.edges, chosen, onPick, compact = false)
                ChosenLevelStrip(chosen)
            }
        }
        AppDivider()
        PromptButtons(state, chosen, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xl))
    }
}

/** Skip for now on the left, the saving state and Continue on the right. */
@Composable
private fun PromptButtons(state: VocabularySettingsUiState, chosen: LevelEdge, onSave: () -> Unit, onLater: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        QuietButton(onClick = onLater, enabled = !state.saving, size = if (compact) ButtonSize.Compact else ButtonSize.Regular) {
            Text(tr("Skip for now"), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        if (state.saving) {
            AppSpinner(size = SpinnerSize.Small)
            if (!compact) Text(tr("Saving…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        PrimaryButton(
            onClick = onSave,
            enabled = state.picked != null && !state.saving,
            modifier = Modifier.testTag("prompt-set-level"),
        ) {
            Text(tr("Continue with {0}", chosen.code()), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val DIALOG_WIDTH = 1080.dp
private val LOADING_HEIGHT = 240.dp
