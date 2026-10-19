package com.tayra.languages.feature.reading

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.PageBookmark
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.ui.components.AppCard
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppSlider
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavigationRow
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PaneHeader
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentStyle
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.components.Tab
import com.tayra.languages.core.ui.components.TextInputDialog
import com.tayra.languages.core.ui.components.UnderlineTabs
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
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
            onClose = onClose,
            modifier = Modifier.padding(start = AppSpacing.xl, end = AppSpacing.l, top = AppSpacing.xl, bottom = AppSpacing.l),
            onBack = if (showBookmarks) ({ showBookmarks = false }) else null,
            icon = AppIcons.Otter,
            closeDescription = tr("Close menu"),
        )
        AppDivider()
        if (!showBookmarks) {
            // A phone's pane has no room for the icons beside the names.
            val icons = !LocalWindowWidth.current.isCompact
            UnderlineTabs(
                listOf(
                    Tab(PaneTab.READING, tr("Reading"), icon = AppIcons.MenuBook.takeIf { icons }, testTag = "pane-tab-READING"),
                    Tab(PaneTab.AUDIO, tr("Audio"), icon = AppIcons.VolumeUp.takeIf { icons }, testTag = "pane-tab-AUDIO"),
                    Tab(PaneTab.APPEARANCE, tr("Appearance"), icon = AppIcons.FormatSize.takeIf { icons }, testTag = "pane-tab-APPEARANCE"),
                ),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.s),
                fill = true,
                tinted = true,
            )
        }
        ScrollColumn(
            Modifier.weight(1f).fillMaxWidth().background(colors.surfaceVariant.copy(alpha = AppAlpha.disabled)),
            // The bar runs in a gutter of its own at the pane's edge, clear of the cards.
            contentModifier = Modifier.padding(start = AppSpacing.l, top = AppSpacing.l, bottom = AppSpacing.l, end = AppSpacing.xxxl),
            scrollbarModifier = Modifier.padding(top = AppSpacing.s, bottom = AppSpacing.s, end = AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
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
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset reader settings?")) },
            text = { Text(tr("Reading, audio and appearance go back to their defaults. Your translation and speech engines and voices stay.")) },
            confirmButton = { PrimaryButton(tr("Reset"), onClick = { confirmReset = false; viewModel.resetReaderSettings() }) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = { confirmReset = false }) },
        )
    }
}

/** A white card with a heading and its settings. */
@Composable
private fun PaneCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(padding = PaddingValues(AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Text(title, style = AppText.itemTitle)
        content()
    }
}

/**
 * A setting named beside its [SegmentedToggle] choices when both fit the pane at their natural widths,
 * else above them, the choices then sharing the full width; always above them on a phone.
 */
@Composable
private fun <T> SegmentedSetting(icon: ImageVector?, title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    Layout(
        content = {
            SettingRow(title, icon = icon, dense = true) {}
            SegmentedToggle(options.map { (value, label) -> Segment(value, label = label) }, selected, onSelect, Modifier.fillMaxWidth(), fill = true, style = SegmentStyle.Solid)
        },
    ) { (label, control), constraints ->
        val gap = AppSpacing.l.roundToPx()
        val width = constraints.maxWidth
        val controlWidth = control.maxIntrinsicWidth(constraints.maxHeight)
        val beside = !compact && width != Constraints.Infinity && label.maxIntrinsicWidth(constraints.maxHeight) + gap + controlWidth <= width
        if (beside) {
            val labelWidth = width - gap - controlWidth
            val l = label.measure(Constraints(minWidth = labelWidth, maxWidth = labelWidth, maxHeight = constraints.maxHeight))
            val c = control.measure(Constraints(minWidth = controlWidth, maxWidth = controlWidth, maxHeight = constraints.maxHeight))
            val height = maxOf(l.height, c.height)
            layout(width, height) {
                l.placeRelative(0, (height - l.height) / 2)
                c.placeRelative(width - controlWidth, (height - c.height) / 2)
            }
        } else {
            val l = label.measure(constraints.copy(minHeight = 0))
            val c = control.measure(constraints.copy(minHeight = 0))
            val below = l.height + AppSpacing.s.roundToPx()
            layout(constraints.constrainWidth(maxOf(l.width, c.width)), below + c.height) {
                l.placeRelative(0, 0)
                c.placeRelative(0, below)
            }
        }
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
            Modifier.fillMaxWidth().clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel)
                .clickable(enabled = options.size > 1) { open = true }.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconMedium))
            if (LocalWindowWidth.current.isCompact) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                // Sized to the value, so the name and its note keep the rest of the row.
                Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = VALUE_MAX_WIDTH))
            }
            Icon(AppIcons.UnfoldMore, contentDescription = null, tint = colors.outline, modifier = Modifier.size(AppSizes.icon))
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

// ---- Reading

@Composable
private fun ReadingTab(state: ReadingUiState, viewModel: ReadingViewModel, onTranslationSettings: () -> Unit) {
    val prefs = state.settings
    PaneCard(tr("Reading experience")) {
        SwitchSetting(tr("Focus mode"), tr("Hide navigation and distractions"), prefs.focusMode, icon = AppIcons.Fullscreen, dense = true) { viewModel.toggleFocusMode() }
        SwitchSetting(tr("Highlight terms"), tr("Color words by learning status"), prefs.showHighlights, icon = AppIcons.Palette, dense = true) { viewModel.toggleHighlights() }
        if (state.language?.let { LanguageCodes.codeFor(it.name) } in READING_LANGUAGES) {
            SwitchSetting(tr("Show readings"), tr("Furigana or pinyin above the words"), prefs.showReadings, icon = AppIcons.Abc, dense = true) { viewModel.toggleReadings() }
        }
        if (viewModel.canChooseChineseScript) ChineseScriptSetting(state.language?.chineseScript, viewModel::setChineseScript)
        if (state.language?.let { LanguageCodes.codeFor(it.name) } in TONE_LANGUAGES) {
            SwitchSetting(tr("Tone colours"), tr("Colour pinyin by its tone"), prefs.toneColors, icon = AppIcons.Palette, dense = true) { viewModel.toggleToneColors() }
            SwitchSetting(tr("Zhuyin"), tr("Readings in bopomofo instead of pinyin"), prefs.zhuyin, icon = AppIcons.Abc, dense = true) { viewModel.toggleZhuyin() }
        }
        if (prefs.showHighlights) {
            val statusColors = TayraTheme.current.statusColors
            Row(Modifier.padding(start = AppSpacing.huge), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                listOf(TermStatus.UNKNOWN to tr("Unknown"), TermStatus.NEW_1 to tr("Learning")).forEach { (status, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(statusColors.background(status), size = 14.dp)
                        Spacer(Modifier.width(AppSpacing.s))
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SegmentedSetting(AppIcons.LineSpacing, tr("Sentence layout"), listOf(false to tr("Continuous"), true to tr("One per line")), prefs.splitSentences, viewModel::setSplitSentences)
    }
    PaneCard(tr("Translations")) {
        SwitchSetting(tr("Show translations"), null, prefs.showTranslations, icon = AppIcons.Translate, dense = true) { viewModel.toggleShowTranslations() }
        SwitchSetting(tr("Practice translation"), tr("Blur the sentences; point at or tap one to show it"), prefs.practiceTranslation, icon = AppIcons.School, dense = true) { viewModel.togglePracticeTranslation() }
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
        NavigationRow(tr("Translation settings"), onTranslationSettings, icon = AppIcons.Tune, tint = MaterialTheme.colorScheme.primary)
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
        SwitchSetting(tr("Play audio"), tr("Read the current page aloud"), prefs.showSentencePlay, icon = AppIcons.PlayArrow, dense = true) { viewModel.toggleSentencePlay() }
        SwitchSetting(tr("Auto-pause"), tr("Stop after each sentence"), prefs.autoPause, icon = AppIcons.Pause, dense = true) { viewModel.toggleAutoPause() }
        SwitchSetting(tr("Speak word on click"), tr("Hear a word when you select it"), prefs.speakWordOnClick, icon = AppIcons.Abc, dense = true) { viewModel.toggleSpeakWordOnClick() }
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
        NavigationRow(tr("Speech settings"), onSpeechSettings, icon = AppIcons.Tune, tint = MaterialTheme.colorScheme.primary)
    }
    if (local && viewModel.speechSpeedAdjustable()) {
        PaneCard(tr("Playback speed")) {
            val percent = (prefs.speechSpeed * 100).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text("$percent%", style = AppText.screenTitle, modifier = Modifier.width(SPEED_FIGURE_WIDTH))
                Text(tr("Slower"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppSlider(value = prefs.speechSpeed, onValueChange = viewModel::setSpeechSpeed, valueRange = 0.5f..1.5f, modifier = Modifier.weight(1f).testTag("speech-speed"))
                Text(tr("Faster"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                listOf(0.75f, 1f, 1.25f).forEach { speed ->
                    val active = kotlin.math.abs(prefs.speechSpeed - speed) < 0.01f
                    Text(
                        "${(speed * 100).roundToInt()}%",
                        Modifier.weight(1f).clip(AppShapes.control)
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.soft) else Color.Transparent)
                            .border(AppBorders.hairline(if (active) MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.half) else MaterialTheme.colorScheme.outlineVariant), AppShapes.control)
                            .clickable { viewModel.setSpeechSpeed(speed) }.padding(vertical = AppSpacing.s),
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
        SettingRow(tr("Font"), dense = true) {
            SelectMenu(
                ReadingFont.choices,
                selected = font,
                onSelect = viewModel::setReadingFont,
                optionLabel = { tr(it.label) },
                buttonModifier = Modifier.testTag("font-choice"),
                optionContent = { Text(tr(it.label), fontFamily = it.fontFamily()) },
            )
        }
        SliderSetting(tr("Font size"), "${(prefs.readingFontScale * 100).roundToInt()}%", prefs.readingFontScale, 0.6f..2.5f, viewModel::setFontScale, sliderModifier = Modifier.testTag("font-size"))
        SliderSetting(tr("Line height"), "${(prefs.readingLineHeight * 10).roundToInt() / 10f}", prefs.readingLineHeight, 1.0f..3.0f, viewModel::setLineHeight, sliderModifier = Modifier.testTag("line-height"))
    }
    PaneCard(tr("Page layout")) {
        SliderSetting(tr("Text width"), tr("{0} px", prefs.readingColumnWidth), prefs.readingColumnWidth.toFloat(), 320f..2000f, { viewModel.setColumnWidth(it.roundToInt()) }, sliderModifier = Modifier.testTag("text-width"))
        SegmentedSetting(null, tr("Alignment"), listOf(false to tr("Left"), true to tr("Justified")), prefs.readingJustified, viewModel::setJustified)
    }
}

// ---- Bookmarks

@Composable
private fun BookmarksView(state: ReadingUiState, bookmarks: List<PageBookmark>, viewModel: ReadingViewModel, onOpen: (page: Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val page = state.pageNumber
    val marked = bookmarks.any { it.pageNumber == page }
    SecondaryButton(
        tr("Add bookmark to page {0}", page),
        onClick = { viewModel.addBookmark(tr("Page {0}", page)) },
        modifier = Modifier.fillMaxWidth().testTag("add-bookmark"),
        icon = Icons.Default.Add,
        size = ButtonSize.Large,
        tint = colors.primary,
    )
    InfoBanner(if (marked) tr("Page {0} is bookmarked", page) else tr("Page {0} is not bookmarked yet", page), dense = true)
    if (bookmarks.isEmpty()) {
        Text(tr("No bookmarks yet. Bookmark a page to come back to it."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
    bookmarks.forEach { bookmark -> BookmarkCard(bookmark, current = bookmark.pageNumber == page, viewModel, onOpen = { onOpen(bookmark.pageNumber) }) }
}

@Composable
private fun BookmarkCard(bookmark: PageBookmark, current: Boolean, viewModel: ReadingViewModel, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var renaming by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.panel).background(if (current) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .border(AppBorders.hairline(if (current) colors.primary.copy(alpha = AppAlpha.disabled) else colors.outlineVariant), AppShapes.panel)
            .clickable(onClick = onOpen).padding(start = AppSpacing.l, top = AppSpacing.l, bottom = AppSpacing.l, end = AppSpacing.xs).testTag("bookmark-${bookmark.pageNumber}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Icon(AppIcons.Bookmark, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(bookmark.title.ifBlank { tr("Page {0}", bookmark.pageNumber) }, style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The page it is on, when the title does not already say so.
            if (bookmark.title.isNotBlank() && bookmark.title != tr("Page {0}", bookmark.pageNumber)) {
                Text(tr("Page {0}", bookmark.pageNumber), style = MaterialTheme.typography.bodySmall, color = colors.primary)
            }
            if (bookmark.opening.isNotBlank()) {
                Text(bookmark.opening.replace(Regex("""\s+"""), " ").trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            bookmark.createdAt?.let { Text(tr("Added {0}", it.relativeTo()), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        OverflowMenu(tr("Bookmark actions")) { close ->
            AppMenuItem(text = { Text(tr("Rename")) }, onClick = { close(); renaming = true })
            AppMenuItem(text = { Text(tr("Delete"), color = colors.error) }, onClick = { close(); viewModel.deleteBookmark(bookmark.id) })
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
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(tr("Reader tools"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AppDivider(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
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
        LinkText(tr("Reset reader settings"), onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("reset-reader"), icon = Icons.Default.Refresh)
    }
}

@Composable
private fun ToolTile(icon: ImageVector, label: String, note: String, active: Boolean, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val press = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().pressScale(press).clip(AppShapes.panel).background(if (active) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (active) colors.primary.copy(alpha = AppAlpha.disabled) else colors.outlineVariant), AppShapes.panel)
            .clickable(press, LocalIndication.current, onClick = onClick).padding(vertical = AppSpacing.m, horizontal = AppSpacing.xs).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconMedium))
        Text(label, style = AppText.labelStrong, maxLines = 1)
        Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Languages whose words get their reading shown above them. */
private val READING_LANGUAGES = setOf("ja", "zh")

/** The languages whose readings are pinyin with tones. */
private val TONE_LANGUAGES = setOf("zh", "yue")

/**
 * Which characters a Mandarin text is shown in and its words are kept in. Choosing a script asks
 * first: the saved words are rewritten in it, and a word saved in both scripts becomes one.
 */
@Composable
private fun ChineseScriptSetting(current: ChineseScript?, onChoose: (ChineseScript?) -> Unit) {
    var asking by remember { mutableStateOf<ChineseScript?>(null) }
    val options = listOf<ChineseScript?>(null, ChineseScript.SIMPLIFIED, ChineseScript.TRADITIONAL)
    fun label(script: ChineseScript?) = when (script) {
        null -> tr("As written")
        ChineseScript.SIMPLIFIED -> tr("Simplified")
        ChineseScript.TRADITIONAL -> tr("Traditional")
    }
    SettingRow(tr("Characters"), tr("Texts shown and words kept in one script"), icon = AppIcons.Abc, dense = true) {
        Dropdown(options, current, { chosen -> if (chosen == null) onChoose(null) else if (chosen != current) asking = chosen }, null, ::label, Modifier.width(SCRIPT_DROPDOWN_WIDTH).testTag("chinese-script"))
    }
    asking?.let { script ->
        AppDialog(
            onDismissRequest = { asking = null },
            title = { Text(tr("Read in {0} characters?", label(script).lowercase())) },
            text = { Text(tr("Texts will be shown in these characters, and your saved words rewritten in them; a word you saved in both scripts becomes one, keeping its more advanced status.")) },
            confirmButton = { PrimaryButton(tr("Rewrite"), onClick = { asking = null; onChoose(script) }, modifier = Modifier.testTag("chinese-script-confirm")) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = { asking = null }) },
        )
    }
}

private val VALUE_MAX_WIDTH = 130.dp
private val SPEED_FIGURE_WIDTH = 96.dp
private val SCRIPT_DROPDOWN_WIDTH = 150.dp
