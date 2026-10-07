package com.tayra.languages.feature.frequency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.trPlural
import org.koin.compose.viewmodel.koinViewModel

/** The colour of words the level covers, in the picker and the example. */
internal val KNOWN_GREEN = Color(0xFF2E9D57)

@Composable
fun VocabularySettingsScreen(onNavigate: (Route) -> Unit, viewModel: VocabularySettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    Scaffold(
        topBar = { AppTopBar(title = tr("Vocabulary settings"), onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PageColumn(padding) {
            VocabularySettingsContent(
                state,
                onPick = viewModel::pick,
                onSave = viewModel::save,
                onWordFrequency = { onNavigate(Route.WordFrequency) },
                onBackToSettings = { onNavigate(Route.Settings) },
            )
        }
    }
}

@Composable
internal fun VocabularySettingsContent(
    state: VocabularySettingsUiState,
    onPick: (Int) -> Unit,
    onSave: () -> Unit,
    onWordFrequency: () -> Unit,
    onBackToSettings: (() -> Unit)? = null,
) {
    val wide = LocalWindowWidth.current.isExpanded
    var confirming by remember { mutableStateOf(false) }
    ScreenHeader(
        tr("Vocabulary"),
        if (state.languageName.isEmpty()) tr("How much of the language you know already.") else tr("How much {0} you know already.", languageInSentence(state.languageName, LanguageCase.NOMINATIVE)),
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
    ContentCard(
        tr("Vocabulary level"),
        state.shownLevel.coerceAtLeast(1).let { n ->
            trPlural(
                n,
                "Click the last row where you know all the words. The most common {1} word and its forms are then saved as known; words you have saved already keep their status.",
                "Click the last row where you know all the words. The most common {1} words and their forms are then saved as known; words you have saved already keep their status.",
                formatCount(n),
            )
        },
        icon = AppIcons.BarChart,
        titleExtra = {
            Text(
                if (state.chosen) tr("Set to {0}", formatCount(state.level)) else tr("Not set"),
                Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
    ) {
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LevelPicker(list, state.shownLevel, onPick, Modifier.weight(1.1f))
                ExampleText(state, Modifier.weight(1f))
            }
        } else {
            LevelPicker(list, state.shownLevel, onPick, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            ExampleText(state, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { confirming = true }, enabled = state.picked != null && !state.saving, modifier = Modifier.testTag("set-vocabulary-level")) {
                Text(tr("Set vocabulary level"))
            }
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(tr("Saving…"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text(
                tr("Word frequency"),
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onWordFrequency).padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    val picked = state.picked
    if (confirming && picked != null) {
        LevelConfirmDialog(from = state.level, to = picked, first = !state.chosen, onConfirm = { confirming = false; onSave() }, onDismiss = { confirming = false })
    }
}

/** The levels, each with six of the words it adds; the last row picked has a dashed line under it. */
@Composable
internal fun LevelPicker(list: FrequencyList, shown: Int?, onPick: (Int) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val choices = remember(list) { VocabularyLevelService.choices(list.words.size) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.heightIn(max = 560.dp).clip(shape).border(1.dp, colors.outlineVariant, shape).verticalScroll(rememberScrollState()),
    ) {
        choices.forEachIndexed { i, level ->
            val previous = if (i == 0) 0 else choices[i - 1]
            val selected = level == shown
            val known = shown != null && level <= shown
            Row(
                Modifier.fillMaxWidth()
                    .background(if (selected) colors.primary.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable { onPick(level) }
                    .then(
                        if (selected) {
                            Modifier.drawBehind {
                                drawLine(KNOWN_GREEN, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                            }
                        } else {
                            Modifier.drawBehind { drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("level-$level"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatCount(level), Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (level == 0) {
                    Text(tr("I'm just starting out"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                } else {
                    val samples = remember(list, previous, level) { samples(list, previous, level) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        samples.chunked(3).forEach { row ->
                            Row {
                                row.forEach { word ->
                                    Text(
                                        word,
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (known) KNOWN_GREEN else colors.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Six words spread over the ranks a level adds. */
internal fun samples(list: FrequencyList, from: Int, to: Int): List<String> {
    val span = to - from
    if (span <= 0) return emptyList()
    return (0 until 6).map { i -> list.words[(from + span * (2 * i + 1) / 12).coerceIn(from, to - 1)].word }.distinct()
}

/** A sample text coloured by the level picked: the words it knows, the ones it doesn't, and rarer ones. */
@Composable
internal fun ExampleText(state: VocabularySettingsUiState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val level = state.shownLevel
    val rare = colors.onSurfaceVariant.copy(alpha = 0.55f)
    val shape = RoundedCornerShape(12.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Example text"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().clip(shape).border(1.dp, colors.outlineVariant, shape).padding(20.dp)) {
            if (state.example.isEmpty()) {
                Text(tr("No sample text for {0}.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)), color = colors.onSurfaceVariant)
            } else {
                Text(
                    buildAnnotatedString {
                        for (token in state.example) {
                            val color = when {
                                !token.isWord -> colors.onSurface
                                token.rank == null -> rare
                                token.rank <= level -> KNOWN_GREEN
                                else -> colors.primary
                            }
                            withStyle(SpanStyle(color = color)) { append(token.text) }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Legend(KNOWN_GREEN, tr("Known at this level"))
            Legend(colors.primary, tr("New"))
            Legend(rare, tr("Rarer than the {0} most common", formatCount(state.list?.words?.size ?: 0)))
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
