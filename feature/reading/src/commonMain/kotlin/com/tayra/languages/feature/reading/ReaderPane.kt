package com.tayra.languages.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.PageBookmark
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.RoundSlider
import com.tayra.languages.core.ui.components.TextInputDialog
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.core.ui.theme.fontFamily
import kotlin.math.roundToInt

/** What the pane's links and tools open outside it. */
internal class ReaderPaneActions(
    val onEditBook: () -> Unit,
    val onEditPage: () -> Unit,
    val onTermList: () -> Unit,
    val onSource: () -> Unit,
    val onSpeechSettings: () -> Unit,
    val onTranslationSettings: () -> Unit,
)

private enum class PaneTab { READING, AUDIO, APPEARANCE }

/**
 * The reader's left pane: its settings in three tabs (reading, audio, appearance) or the book's
 * bookmarks, with the reader tools always at the bottom. Every change is saved as it is made.
 */
@Composable
internal fun ReaderPane(state: ReadingUiState, viewModel: ReadingViewModel, actions: ReaderPaneActions, onClose: () -> Unit) {
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(PaneTab.READING) }
    var confirmReset by remember { mutableStateOf(false) }
    val bookmarks by viewModel.bookmarks.collectAsState()
    val colors = MaterialTheme.colorScheme
    val title = state.book?.title.orEmpty()

    Column(Modifier.fillMaxHeight()) {
        PaneHeader(
            title = if (showBookmarks) tr("Bookmarks") else tr("Reader settings"),
            subtitle = if (showBookmarks) trPlural(bookmarks.size, "{1} · {0} saved", "{1} · {0} saved", title) else tr("{0} · Saved automatically", title),
            onBack = if (showBookmarks) ({ showBookmarks = false }) else null,
            onClose = onClose,
        )
        HorizontalDivider(color = colors.outlineVariant)
        if (!showBookmarks) {
            PaneTabs(tab) { tab = it }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().background(colors.surfaceVariant.copy(alpha = 0.35f)).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showBookmarks) {
                BookmarksView(state, bookmarks, viewModel, onOpen = { viewModel.goToPage(it); onClose() })
            } else {
                when (tab) {
                    PaneTab.READING -> ReadingTab(state, viewModel, onTranslationSettings = { onClose(); actions.onTranslationSettings() })
                    PaneTab.AUDIO -> AudioTab(state, viewModel, onSpeechSettings = { onClose(); actions.onSpeechSettings() })
                    PaneTab.APPEARANCE -> AppearanceTab(state, viewModel)
                }
            }
        }
        ReaderTools(
            state,
            bookmarksOpen = showBookmarks,
            onBookmarks = { showBookmarks = !showBookmarks },
            actions = actions,
            onClose = onClose,
            onReset = { confirmReset = true },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset reader settings?")) },
            text = { Text(tr("Reading, audio and appearance go back to their defaults. Your translation and speech engines and voices stay.")) },
            confirmButton = { Button(onClick = { confirmReset = false; viewModel.resetReaderSettings() }) { Text(tr("Reset")) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
private fun PaneHeader(title: String, subtitle: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant.copy(alpha = 0.6f)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), modifier = Modifier.size(20.dp))
            }
        } else {
            Icon(AppIcons.Otter, contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant.copy(alpha = 0.6f)).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Close, contentDescription = tr("Close menu"), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun PaneTabs(selected: PaneTab, onSelect: (PaneTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A phone's pane has no room for the icons beside the names.
    val icons = !LocalWindowWidth.current.isCompact
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(PaneTab.READING, AppIcons.MenuBook, tr("Reading")),
            Triple(PaneTab.AUDIO, AppIcons.VolumeUp, tr("Audio")),
            Triple(PaneTab.APPEARANCE, AppIcons.FormatSize, tr("Appearance")),
        ).forEach { (tab, icon, label) ->
            val active = tab == selected
            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (active) colors.primary.copy(alpha = 0.1f) else Color.Transparent).clickable { onSelect(tab) }.testTag("pane-tab-${tab.name}")) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    if (icons) Icon(icon, contentDescription = null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = if (active) colors.primary else colors.onSurfaceVariant, maxLines = 1)
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (active) colors.primary else Color.Transparent))
            }
        }
    }
}

/** A white card with a heading and its settings. */
@Composable
private fun PaneCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SettingText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchSetting(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        SettingText(title, subtitle, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

/** Two or more choices side by side, the chosen one filled; with [fill] they share the row's width. */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(3.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            Text(
                label,
                (if (fill) Modifier.weight(1f) else Modifier).clip(RoundedCornerShape(8.dp)).background(if (active) colors.primary else Color.Transparent)
                    .clickable { onSelect(value) }.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LabeledRow(icon: ImageVector?, title: String, control: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        control()
    }
}

/** A setting named beside its [Segmented] choices, or above them on a phone. */
@Composable
private fun <T> SegmentedSetting(icon: ImageVector?, title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    if (LocalWindowWidth.current.isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledRow(icon, title) {}
            Segmented(options, selected, onSelect, Modifier.fillMaxWidth(), fill = true)
        }
    } else {
        LabeledRow(icon, title) { Segmented(options, selected, onSelect) }
    }
}

/** A choice shown as a bordered row (its name, a note and the current value) that opens a menu of the options. */
@Composable
private fun <T> ChoiceCard(
    icon: ImageVector,
    title: String,
    note: String?,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    optionEnabled: (T) -> Boolean = { true },
    optionFont: ((T) -> ReadingFont)? = null,
    onSelect: (T) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
                .clickable(enabled = options.size > 1) { open = true }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            if (LocalWindowWidth.current.isCompact) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            } else {
                SettingText(title, note, Modifier.weight(1f))
                // Sized to the value, so the name and its note keep the rest of the row.
                Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 130.dp))
            }
            Icon(AppIcons.UnfoldMore, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(
                    text = { Text(optionLabel(option), fontFamily = optionFont?.invoke(option)?.fontFamily()) },
                    onClick = { open = false; onSelect(option) },
                    enabled = optionEnabled(option),
                )
            }
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.primary)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
    }
}

/** A slider with its name and value above it. */
@Composable
private fun SliderSetting(title: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, tag: String) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RoundSlider(value = current.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().testTag(tag))
    }
}

// ---- Reading

@Composable
private fun ReadingTab(state: ReadingUiState, viewModel: ReadingViewModel, onTranslationSettings: () -> Unit) {
    val prefs = state.settings
    PaneCard(tr("Reading experience")) {
        SwitchSetting(AppIcons.Fullscreen, tr("Focus mode"), tr("Hide navigation and distractions"), prefs.focusMode) { viewModel.toggleFocusMode() }
        SwitchSetting(AppIcons.Palette, tr("Highlight terms"), tr("Color words by learning status"), prefs.showHighlights) { viewModel.toggleHighlights() }
        if (prefs.showHighlights) {
            val statusColors = TayraTheme.current.statusColors
            Row(Modifier.padding(start = 36.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(TermStatus.UNKNOWN to tr("New"), TermStatus.NEW_1 to tr("Learning")).forEach { (status, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(statusColors.background(status)))
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SegmentedSetting(AppIcons.LineSpacing, tr("Sentence layout"), listOf(false to tr("Continuous"), true to tr("One per line")), prefs.splitSentences, viewModel::setSplitSentences)
    }
    PaneCard(tr("Translations")) {
        SwitchSetting(AppIcons.Translate, tr("Show translations"), null, prefs.showTranslations) { viewModel.toggleShowTranslations() }
        SegmentedSetting(AppIcons.ViewColumn, tr("Translation layout"), listOf(true to tr("Side by side"), false to tr("Below")), prefs.sideBySideTranslations, viewModel::setSideBySideTranslations)
        val keyed = prefs.googleTranslateApiKey.isNotBlank()
        fun usable(engine: TranslationEngine) = engine != TranslationEngine.GOOGLE || keyed
        ChoiceCard(
            icon = AppIcons.Globe,
            title = tr("Translation engine"),
            note = translationNote(prefs.translationEngine),
            value = translationName(prefs.translationEngine, viewModel.localTranslatorName),
            options = viewModel.availableEngines,
            optionLabel = { if (usable(it)) translationName(it, viewModel.localTranslatorName) else tr("{0} – add a key in Settings", translationName(it, viewModel.localTranslatorName)) },
            optionEnabled = ::usable,
            onSelect = viewModel::setTranslationEngine,
        )
        LinkRow(AppIcons.Tune, tr("Translation settings"), onTranslationSettings)
    }
}

private fun translationName(engine: TranslationEngine, local: String?): String = when (engine) {
    TranslationEngine.ARGOS -> local ?: tr("On this device")
    TranslationEngine.MYMEMORY -> "MyMemory"
    TranslationEngine.GOOGLE -> "Google Translate"
}

private fun translationNote(engine: TranslationEngine): String = when (engine) {
    TranslationEngine.ARGOS -> tr("Offline · Free")
    TranslationEngine.MYMEMORY -> tr("Online · Free")
    TranslationEngine.GOOGLE -> tr("Online · API key")
}

// ---- Audio

@Composable
private fun AudioTab(state: ReadingUiState, viewModel: ReadingViewModel, onSpeechSettings: () -> Unit) {
    val prefs = state.settings
    val voices by viewModel.speechVoices.collectAsState()
    LaunchedEffect(prefs.speechEngine, state.language?.id) { viewModel.loadSpeechVoices() }
    val engine = prefs.speechEngine.takeIf { it in viewModel.speechEngines } ?: SpeechEngine.SYSTEM
    val local = engine != SpeechEngine.SYSTEM
    val languageName = state.language?.name.orEmpty()

    PaneCard(tr("Playback")) {
        SwitchSetting(AppIcons.PlayArrow, tr("Play audio"), tr("Read the current page aloud"), prefs.showSentencePlay) { viewModel.toggleSentencePlay() }
        SwitchSetting(AppIcons.Pause, tr("Auto-pause"), tr("Stop after each sentence"), prefs.autoPause) { viewModel.toggleAutoPause() }
        SwitchSetting(AppIcons.Abc, tr("Speak word on click"), tr("Hear a word when you select it"), prefs.speakWordOnClick) { viewModel.toggleSpeakWordOnClick() }
    }
    PaneCard(tr("Voice")) {
        ChoiceCard(
            icon = AppIcons.Globe,
            title = tr("Speech engine"),
            note = speechNote(engine),
            value = speechName(engine),
            options = viewModel.speechEngines,
            optionLabel = ::speechName,
            onSelect = viewModel::setSpeechEngine,
        )
        if (local) {
            if (voices.isNotEmpty()) {
                val chosen = prefs.speechVoices["${prefs.speechEngine.name}:${viewModel.speechLanguage}"]
                ChoiceCard(
                    icon = AppIcons.RecordVoiceOver,
                    title = if (languageName.isEmpty()) tr("Voice") else tr("{0} voice", tr(languageName)),
                    note = null,
                    value = (voices.firstOrNull { it.id == chosen } ?: voices.first()).name,
                    options = voices,
                    optionLabel = { it.name },
                    onSelect = { viewModel.setSpeechVoice(it.id) },
                )
            } else {
                Text(
                    tr("No voice for {0} is downloaded, so the system voice reads this text. Download one in Speech settings.", tr(languageName)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinkRow(AppIcons.Tune, tr("Speech settings"), onSpeechSettings)
    }
    if (local && viewModel.speechSpeedAdjustable()) {
        PaneCard(tr("Playback speed")) {
            val percent = (prefs.speechSpeed * 100).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$percent%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                Text(tr("Slower"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RoundSlider(value = prefs.speechSpeed, onValueChange = viewModel::setSpeechSpeed, valueRange = 0.5f..1.5f, modifier = Modifier.weight(1f).testTag("speech-speed"))
                Text(tr("Faster"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0.75f, 1f, 1.25f).forEach { speed ->
                    val active = kotlin.math.abs(prefs.speechSpeed - speed) < 0.01f
                    Text(
                        "${(speed * 100).roundToInt()}%",
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .border(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setSpeechSpeed(speed) }.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun speechName(engine: SpeechEngine): String = when (engine) {
    SpeechEngine.SYSTEM -> tr("System voices")
    SpeechEngine.PIPER -> "Piper"
    SpeechEngine.KOKORO -> "Kokoro"
}

private fun speechNote(engine: SpeechEngine): String = when (engine) {
    SpeechEngine.SYSTEM -> tr("Built into your device")
    SpeechEngine.PIPER -> tr("Offline · Downloadable voices")
    SpeechEngine.KOKORO -> tr("Offline · High quality")
}

// ---- Appearance

@Composable
private fun AppearanceTab(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    PaneCard(tr("Typography")) {
        val font = ReadingFont.byId(prefs.readingFont)
        var open by remember { mutableStateOf(false) }
        LabeledRow(null, tr("Font")) {
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)).clickable { open = true }
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp).testTag("font-choice"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(tr(font.label), style = MaterialTheme.typography.bodyLarge, fontFamily = font.fontFamily())
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                AppMenu(expanded = open, onDismissRequest = { open = false }) {
                    ReadingFont.choices.forEach { option ->
                        AppMenuItem(text = { Text(tr(option.label), fontFamily = option.fontFamily()) }, onClick = { open = false; viewModel.setReadingFont(option) })
                    }
                }
            }
        }
        SliderSetting(tr("Font size"), "${(prefs.readingFontScale * 100).roundToInt()}%", prefs.readingFontScale, 0.6f..2.5f, viewModel::setFontScale, "font-size")
        SliderSetting(tr("Line height"), "${(prefs.readingLineHeight * 10).roundToInt() / 10f}", prefs.readingLineHeight, 1.0f..3.0f, viewModel::setLineHeight, "line-height")
    }
    PaneCard(tr("Page layout")) {
        SliderSetting(tr("Text width"), tr("{0} px", prefs.readingColumnWidth), prefs.readingColumnWidth.toFloat(), 320f..2000f, { viewModel.setColumnWidth(it.roundToInt()) }, "text-width")
        SegmentedSetting(null, tr("Alignment"), listOf(false to tr("Left"), true to tr("Justified")), prefs.readingJustified, viewModel::setJustified)
    }
}

// ---- Bookmarks

@Composable
private fun BookmarksView(state: ReadingUiState, bookmarks: List<PageBookmark>, viewModel: ReadingViewModel, onOpen: (page: Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val page = state.pageNumber
    val marked = bookmarks.any { it.pageNumber == page }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.5.dp, colors.primary, RoundedCornerShape(12.dp))
            .clickable { viewModel.addBookmark(tr("Page {0}", page)) }.padding(vertical = 14.dp).testTag("add-bookmark"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary)
        Text(tr("Add bookmark to page {0}", page), style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.SemiBold)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.07f)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(if (marked) tr("Page {0} is bookmarked", page) else tr("Page {0} is not bookmarked yet", page), style = MaterialTheme.typography.bodyMedium, color = colors.primary)
    }
    if (bookmarks.isEmpty()) {
        Text(tr("No bookmarks yet. Bookmark a page to come back to it."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
    bookmarks.forEach { bookmark -> BookmarkCard(bookmark, current = bookmark.pageNumber == page, viewModel, onOpen = { onOpen(bookmark.pageNumber) }) }
}

@Composable
private fun BookmarkCard(bookmark: PageBookmark, current: Boolean, viewModel: ReadingViewModel, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (current) colors.primary.copy(alpha = 0.05f) else colors.surface)
            .border(1.dp, if (current) colors.primary.copy(alpha = 0.4f) else colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp).testTag("bookmark-${bookmark.pageNumber}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(AppIcons.Bookmark, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(bookmark.title.ifBlank { tr("Page {0}", bookmark.pageNumber) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The page it is on, when the title does not already say so.
            if (bookmark.title.isNotBlank() && bookmark.title != tr("Page {0}", bookmark.pageNumber)) {
                Text(tr("Page {0}", bookmark.pageNumber), style = MaterialTheme.typography.bodySmall, color = colors.primary)
            }
            if (bookmark.opening.isNotBlank()) {
                Text(bookmark.opening.replace(Regex("""\s+"""), " ").trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            bookmark.createdAt?.let { Text(tr("Added {0}", it.relativeTo()), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("Bookmark actions"), tint = colors.onSurfaceVariant) }
            AppMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AppMenuItem(text = { Text(tr("Rename")) }, onClick = { menu = false; renaming = true })
                AppMenuItem(text = { Text(tr("Delete"), color = colors.error) }, onClick = { menu = false; viewModel.deleteBookmark(bookmark.id) })
            }
        }
    }
    if (renaming) {
        TextInputDialog(
            title = tr("Rename bookmark"),
            label = tr("Title"),
            initial = bookmark.title,
            onConfirm = { renaming = false; viewModel.renameBookmark(bookmark.id, it.trim()) },
            onDismiss = { renaming = false },
        )
    }
}

// ---- Reader tools

@Composable
private fun ReaderTools(state: ReadingUiState, bookmarksOpen: Boolean, onBookmarks: () -> Unit, actions: ReaderPaneActions, onClose: () -> Unit, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Reader tools"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // A lesson's text belongs to its course, so it is not edited here.
            if (state.lesson == null) {
                var editing by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f)) {
                    ToolTile(AppIcons.MenuBook, tr("Edit"), tr("Book or page"), active = editing, tag = "tool-edit") { editing = true }
                    AppMenu(expanded = editing, onDismissRequest = { editing = false }) {
                        AppMenuItem(text = { Text(tr("Edit book")) }, onClick = { editing = false; onClose(); actions.onEditBook() })
                        AppMenuItem(text = { Text(tr("Edit current page")) }, onClick = { editing = false; onClose(); actions.onEditPage() })
                        if (!state.book?.sourceUri.isNullOrBlank()) {
                            AppMenuItem(text = { Text(tr("Show source URL")) }, onClick = { editing = false; onClose(); actions.onSource() })
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) { ToolTile(AppIcons.Bookmark, tr("Bookmarks"), tr("View or add"), active = bookmarksOpen, tag = "tool-bookmarks", onClick = onBookmarks) }
            Box(Modifier.weight(1f)) { ToolTile(Icons.AutoMirrored.Filled.List, tr("Vocabulary"), tr("Terms on this page"), active = false, tag = "tool-vocabulary") { onClose(); actions.onTermList() } }
        }
        Row(
            Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp)).clickable(onClick = onReset).padding(horizontal = 12.dp, vertical = 6.dp).testTag("reset-reader"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(tr("Reset reader settings"), style = MaterialTheme.typography.bodyLarge, color = colors.primary)
        }
    }
}

@Composable
private fun ToolTile(icon: ImageVector, label: String, note: String, active: Boolean, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (active) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .border(1.dp, if (active) colors.primary.copy(alpha = 0.4f) else colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
