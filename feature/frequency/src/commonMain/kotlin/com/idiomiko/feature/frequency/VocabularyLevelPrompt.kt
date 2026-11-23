package com.idiomiko.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.frequency.LevelEdge
import com.idiomiko.core.domain.frequency.LevelEdges
import com.idiomiko.core.ui.components.AppBottomSheet
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
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.tint
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
        PhonePromptSheet(ready, state, onPick = viewModel::pick, onSave = viewModel::save, onLater = later)
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

/** The prompt on a phone: the question, the levels one under another, and the buttons; swiping it away puts the question off, like Skip for now. */
@Composable
private fun PhonePromptSheet(ready: Boolean, state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onLater, modifier = Modifier.testTag("vocabulary-level-prompt")) {
        if (!ready) {
            Box(Modifier.fillMaxWidth().heightIn(min = LOADING_HEIGHT), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) }
            return@AppBottomSheet
        }
        val chosen = LevelEdges.reached(state.edges, state.shownLevel)
        // The question scrolls on a short screen, so the buttons below it keep their size.
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(horizontal = AppSpacing.xl)) {
            // The close button shares the title's row, so the sheet starts with the question.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            }
            Spacer(Modifier.height(AppSpacing.s))
            PromptSubtitle()
            Spacer(Modifier.height(AppSpacing.xl))
            // One level to a row, so each name has the sheet's width.
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                state.edges.forEach { edge -> LevelCard(edge, edge == chosen, { onPick(edge.words) }, Modifier.fillMaxWidth(), compact = true) }
            }
            Spacer(Modifier.height(AppSpacing.l))
            ChosenLevelStrip(chosen)
            Spacer(Modifier.height(AppSpacing.l))
        }
        AppDivider()
        PromptButtons(state, chosen, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m), compact = true)
    }
}

@Composable
internal fun VocabularyLevelPromptContent(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val chosen = LevelEdges.reached(state.edges, state.shownLevel)
    Column {
        ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xxxl)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(tr("How much {0} do you know?", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)), style = AppText.screenTitle)
                    PromptSubtitle()
                }
                AppIconButton(Icons.Default.Close, tr("Close"), onLater, enabled = !state.saving)
            }
            Spacer(Modifier.height(AppSpacing.xxl))
            val shape = AppShapes.large
            Column(
                Modifier.fillMaxWidth().clip(shape).border(AppBorders.hairline(), shape).padding(AppSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                Text(tr("Choose your level"), style = AppText.sectionTitle)
                // One row of cards where they fit at a readable width, else two rows.
                BoxWithConstraints {
                    val gap = AppSpacing.m
                    val perRow = if (fitsOneRow(maxWidth, state.edges.size, gap)) state.edges.size else (state.edges.size + 1) / 2
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        state.edges.chunked(perRow).forEach { row ->
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                row.forEach { edge -> LevelCard(edge, edge == chosen, { onPick(edge.words) }, Modifier.weight(1f).fillMaxHeight()) }
                                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                ChosenLevelStrip(chosen)
            }
        }
        AppDivider()
        PromptButtons(state, chosen, onSave, onLater, Modifier.padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xl))
    }
}

private fun fitsOneRow(width: Dp, cards: Int, gap: Dp) = width >= CARD_MIN_WIDTH * cards + gap * (cards - 1)

/** What the level is for, and where it can be changed. */
@Composable
private fun PromptSubtitle() {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(tr("Choose the level that feels closest. We'll use it to personalize your courses and vocabulary."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Text(tr("You can change this later in Settings → Vocabulary."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = AppAlpha.muted))
    }
}

/** The level's short name: its code, or Starter. */
@Composable
private fun LevelEdge.code(): String = level?.code ?: tr("Starter")

@Composable
private fun LevelEdge.name(): String = level?.let { tr(it.label) } ?: tr("I'm just beginning")

/** The level's colour, as the courses show it; Starter has the theme's. */
@Composable
private fun LevelEdge.color(): Color = level?.tint ?: MaterialTheme.colorScheme.primary

/**
 * A level to choose: its badge, its code and its name, stacked, or in a row when [compact]. The chosen
 * one has a ring, a wash and a check, in its corner or at the row's end.
 */
@Composable
private fun LevelCard(edge: LevelEdge, selected: Boolean, onClick: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    Box(
        modifier.clip(shape)
            .border(if (selected) AppBorders.chosen() else AppBorders.hairline(), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag("level-card-${edge.level?.code ?: "Starter"}"),
    ) {
        if (compact) {
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.m, vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                LevelBadge(edge, AppSizes.tileLarge)
                Column(Modifier.weight(1f)) {
                    Text(edge.code(), style = AppText.itemTitleBold, maxLines = 1)
                    Text(edge.name(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (selected) ChosenCheck()
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.s, vertical = AppSpacing.l), horizontalAlignment = Alignment.CenterHorizontally) {
                LevelBadge(edge, AppSizes.tileXLarge)
                Spacer(Modifier.height(AppSpacing.m))
                Text(edge.code(), style = AppText.itemTitleBold, maxLines = 1)
                Text(edge.name(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, minLines = 2)
            }
            if (selected) ChosenCheck(Modifier.align(Alignment.TopEnd).padding(AppSpacing.s))
        }
    }
}

/** The small filled check on the chosen card. */
@Composable
private fun ChosenCheck(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(AppSizes.icon).clip(AppShapes.circle).background(colors.primary), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(AppSizes.iconTiny))
    }
}

/** The level's code on a rounded square of its colour; Starter's reads Starter, smaller on a smaller badge. */
@Composable
private fun LevelBadge(edge: LevelEdge, size: Dp) {
    val starter = edge.level == null
    val starterStyle = if (size < AppSizes.tileXLarge) AppText.badgeBold.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize) else AppText.badgeBold
    Box(Modifier.size(size).clip(AppShapes.card).background(edge.color()), contentAlignment = Alignment.Center) {
        Text(
            edge.code(),
            style = if (starter) starterStyle else AppText.sectionTitle,
            color = if (starter) MaterialTheme.colorScheme.onPrimary else AppAccents.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/** What choosing [edge] does: which of the most common words start as known. */
@Composable
private fun ChosenLevelStrip(edge: LevelEdge) {
    val colors = MaterialTheme.colorScheme
    val what = if (edge.words == 0) tr("We'll begin with the basics and mark no words as already known.")
    else trPlural(edge.words, "We'll mark the {1} most common word as known.", "We'll mark the {1} most common words as known.", formatCount(edge.words))
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("chosen-level"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
        val selected = tr("{0} level selected", edge.code())
        Text(
            buildAnnotatedString {
                withStyle(AppText.labelBold.toSpanStyle()) { append(selected) }
                append("  ")
                append(what)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
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

/** The narrowest a level card gets in one row before the cards take two rows: "Intermediate" on one line. */
private val CARD_MIN_WIDTH = 120.dp
private val DIALOG_WIDTH = 1080.dp
private val LOADING_HEIGHT = 240.dp
