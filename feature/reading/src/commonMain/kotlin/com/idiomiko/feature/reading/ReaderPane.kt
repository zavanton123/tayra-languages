package com.idiomiko.feature.reading

import com.idiomiko.core.ui.theme.readingFontFamily
import com.idiomiko.core.ui.theme.fontChoice
import com.idiomiko.core.ui.theme.fontChoices
import com.idiomiko.core.domain.service.SetupKind
import com.idiomiko.core.domain.service.SetupState
import com.idiomiko.core.ui.components.LocalPreparing
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.idiomiko.core.ui.components.NavigationRow
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.TagSize
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.render.RenderedSentence
import com.idiomiko.core.domain.service.SentenceTranslation
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.idiomiko.core.ui.components.inLearnedScript
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.PageBookmark
import com.idiomiko.core.domain.morphology.ChineseScript
import com.idiomiko.core.domain.service.SpeechEngine
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppSlider
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.Segment
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.SwitchSetting
import com.idiomiko.core.ui.components.TextInputDialog
import com.idiomiko.core.ui.components.pressScale
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import com.idiomiko.core.ui.theme.fontFamily
import kotlin.math.roundToInt

/** What the settings' links and tools open outside them. */
internal class ReaderPaneActions(
    val onEditBook: () -> Unit,
    val onEditPage: () -> Unit,
    val onTermList: () -> Unit,
    val onSource: () -> Unit,
)

internal enum class PaneTab { APPEARANCE, TRANSLATION, AUDIO }

/**
 * The reader's settings in three tabs (appearance, translation, audio), or the book's bookmarks: a
 * large dialog beyond a phone, with the cards in two columns, and a screen of its own on a phone.
 * Every change is saved as it is made; Done only closes them.
 */
@Composable
internal fun ReaderPane(
    state: ReadingUiState,
    viewModel: ReadingViewModel,
    actions: ReaderPaneActions,
    onClose: () -> Unit,
) {
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(PaneTab.APPEARANCE) }
    var confirmReset by remember { mutableStateOf(false) }
    val bookmarks by viewModel.bookmarks.collectAsState()
    val colors = MaterialTheme.colorScheme
    val phone = LocalWindowWidth.current.isCompact
    val title = state.book?.title.orEmpty().inLearnedScript()
    val gutter = if (phone) AppSpacing.l else AppSpacing.xxl

    Column(if (phone) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
        SettingsHeader(
            title = if (showBookmarks) tr("Bookmarks") else tr("Reader settings"),
            subtitle = if (showBookmarks) trPlural(bookmarks.size, "{1} · {0} saved", "{1} · {0} saved", title) else title.takeIf { !phone && it.isNotEmpty() },
            phone = phone,
            onBack = if (showBookmarks) ({ showBookmarks = false }) else null,
            onClose = onClose,
            modifier = Modifier.padding(start = gutter, end = gutter, top = if (phone) AppSpacing.m else AppSpacing.xl, bottom = AppSpacing.l),
        )
        if (!showBookmarks) {
            SegmentedToggle(
                listOf(
                    Segment(PaneTab.APPEARANCE, AppIcons.FormatSize, tr("Appearance"), testTag = "pane-tab-APPEARANCE"),
                    Segment(PaneTab.TRANSLATION, AppIcons.Translate, tr("Translation"), testTag = "pane-tab-TRANSLATION"),
                    Segment(PaneTab.AUDIO, AppIcons.VolumeUp, tr("Audio"), testTag = "pane-tab-AUDIO"),
                ),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = gutter),
                height = TAB_BAR_HEIGHT,
                fill = true,
            )
        }
        ScrollColumn(
            // Up to the window's height, beyond which the tab scrolls; a phone's screen is all of it.
            Modifier.weight(1f, fill = phone).fillMaxWidth(),
            contentModifier = Modifier.padding(start = gutter, end = gutter, top = AppSpacing.l, bottom = AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            if (showBookmarks) {
                BookmarksView(state, bookmarks, viewModel, onOpen = { viewModel.goToPage(it); onClose() })
            } else {
                TallestTab(
                    PaneTab.entries.indexOf(tab),
                    listOf(
                        { Columns(phone, { TypographyCard(state, viewModel); ReadingExperienceCard(state, viewModel, phone); AlignmentCard(state, viewModel, phone) }, { PreviewCard(state) }) },
                        {
                            Columns(
                                phone,
                                { TranslationsCard(state, viewModel); if (!phone) TranslationDisplayCard(state, viewModel, phone) },
                                { TranslationEngineCard(state, viewModel) },
                            )
                        },
                        {
                            Columns(
                                phone,
                                { PlaybackCard(state, viewModel); if (!phone) SpeedCard(state, viewModel) },
                                { VoiceCard(state, viewModel); if (phone) SpeedCard(state, viewModel) },
                            )
                        },
                    ),
                    // A lesson has none of the tools: no editing, no bookmarks, and its vocabulary is at its end.
                    after = { if (state.lesson == null) ReaderTools(state, bookmarksOpen = false, onBookmarks = { showBookmarks = true }, actions = actions, onClose = onClose) },
                )
            }
        }
        AppDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = AppSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!showBookmarks) LinkText(tr("Reset"), onClick = { confirmReset = true }, modifier = Modifier.testTag("reset-reader"), icon = Icons.Default.Refresh)
            Spacer(Modifier.weight(1f))
            PrimaryButton(tr("Done"), onClick = onClose, modifier = Modifier.widthIn(min = DONE_MIN_WIDTH).testTag("reader-settings-done"))
        }
    }
    if (confirmReset) {
        // Only the open tab's settings: the others keep theirs.
        val (question, detail) = when (tab) {
            PaneTab.TRANSLATION -> tr("Reset translation settings?") to tr("Translations and their layout go back to their defaults. Your translation engine stays.")
            PaneTab.AUDIO -> tr("Reset audio settings?") to tr("Playback and speed go back to their defaults. Your speech engine and voices stay.")
            PaneTab.APPEARANCE -> tr("Reset appearance settings?") to tr("The text's font, size, spacing and layout go back to their defaults.")
        }
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(question) },
            text = { Text(detail) },
            confirmButton = { PrimaryButton(tr("Reset"), onClick = { confirmReset = false; viewModel.resetReaderSettings(tab) }, modifier = Modifier.testTag("reset-confirm")) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = { confirmReset = false }) },
        )
    }
}

/** The settings' head: the app's mark, the title and the text's name, and a close button; on a phone a way back and the title. */
@Composable
private fun SettingsHeader(title: String, subtitle: String?, phone: Boolean, onBack: (() -> Unit)?, onClose: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        when {
            onBack != null -> AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            phone -> AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Close menu"), onClick = onClose, tint = colors.primary)
            else -> IconTile(AppIcons.Logo, size = 56, iconTint = Color.Unspecified)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = AppText.screenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!phone) AppIconButton(Icons.Default.Close, contentDescription = tr("Close menu"), onClick = onClose, style = IconButtonStyle.Tonal, tint = colors.onSurface)
    }
}

/**
 * The chosen one of the [tabs] with [after] right under it, as tall as the tallest tab with it: all
 * are measured and only the chosen one placed, so the dialog keeps one height whichever tab is open,
 * its spare room at the bottom rather than between the tab and what follows it.
 */
@Composable
private fun TallestTab(chosen: Int, tabs: List<@Composable () -> Unit>, after: @Composable () -> Unit) {
    // The others are measured only: out of sight, and out of reach of screen readers too.
    val wrapped = tabs.mapIndexed { index, tab -> @Composable { Box(if (index == chosen) Modifier else Modifier.clearAndSetSemantics {}) { tab() } } }
    Layout(contents = wrapped + listOf(after)) { measurables, constraints ->
        val free = constraints.copy(minHeight = 0)
        val placeables = measurables.dropLast(1).map { tab -> tab.map { it.measure(free) } }
        val footer = measurables.last().map { it.measure(free) }
        val footerHeight = footer.sumOf { it.height }
        val gap = if (footerHeight > 0) AppSpacing.l.roundToPx() else 0
        val height = placeables.maxOf { tab -> tab.sumOf { it.height } } + gap + footerHeight
        layout(constraints.maxWidth, height) {
            var y = 0
            placeables[chosen].forEach { it.place(0, y); y += it.height }
            y += gap
            footer.forEach { it.place(0, y); y += it.height }
        }
    }
}

/** A tab's cards: in two columns beyond a phone, the [left] ones above the [right] on it. */
@Composable
private fun Columns(phone: Boolean, left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit) {
    if (phone) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) { left(); right() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.l), content = left)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.l), content = right)
        }
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

/** One of a few ways to lay something out, with its picture. */
private class LayoutOption<T>(val value: T, val icon: ImageVector, val label: String, val tag: String)

/**
 * A choice of layouts as cards side by side, each with its picture and a mark that fills in when
 * chosen; on a phone the picture sits above the name.
 */
@Composable
private fun <T> LayoutChoice(options: List<LayoutOption<T>>, selected: T, phone: Boolean, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        options.forEach { option ->
            val chosen = option.value == selected
            val modifier = Modifier.weight(1f).clip(AppShapes.panel)
                .background(if (chosen) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
                .border(AppBorders.hairline(if (chosen) colors.primary else colors.outlineVariant), AppShapes.panel)
                .selectable(chosen, role = Role.RadioButton) { onSelect(option.value) }
                .padding(horizontal = AppSpacing.m, vertical = AppSpacing.l)
                .testTag(option.tag)
            val picture = @Composable { Icon(option.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge)) }
            if (phone) {
                Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { picture(); Spacer(Modifier.weight(1f)); ChosenMark(chosen) }
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    picture()
                    // A long name, as in Russian, takes a second line rather than being cut short.
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    ChosenMark(chosen)
                }
            }
        }
    }
}

/** A round mark: filled with a tick when its choice is the chosen one, an empty ring otherwise. */
@Composable
private fun ChosenMark(chosen: Boolean) {
    val colors = MaterialTheme.colorScheme
    if (chosen) {
        Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).background(colors.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(AppSizes.iconSmall))
        }
    } else {
        Box(Modifier.size(AppSizes.iconLarge).border(AppBorders.selected(colors.outline), AppShapes.circle))
    }
}

/**
 * A choice shown as a row (its icon on a tinted tile, its name, a note and the current value) that
 * opens a menu of the options.
 */
@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    title: String,
    value: String?,
    options: List<T>,
    optionLabel: (T) -> String,
    optionEnabled: (T) -> Boolean = { true },
    note: @Composable (() -> Unit)? = null,
    onSelect: (T) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(AppShapes.compact).clickable(enabled = options.size > 1) { open = true }.padding(vertical = AppSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            IconTile(icon, size = 44)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                note?.invoke()
            }
            // Sized to the value, so the name and its note keep the rest of the row.
            if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = VALUE_MAX_WIDTH))
            if (options.size > 1) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconLarge))
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) }, enabled = optionEnabled(option))
            }
        }
    }
}

/** A setting chosen from a drop-down menu: its icon on a tinted tile, its name and an optional note, and the menu's button. */
@Composable
private fun <T> DropDownRow(icon: ImageVector, title: String, note: String?, options: List<T>, selected: T, optionLabel: (T) -> String, tag: String, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        IconTile(icon, size = 44)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SelectMenu(options, selected, onSelect, optionLabel, Modifier.widthIn(max = SELECT_MAX_WIDTH), buttonModifier = Modifier.testTag(tag))
    }
}

/** A row that leaves for a settings screen, as a link: its icon on a tinted tile, its name in the primary colour. */
@Composable
private fun LinkRow(title: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.compact).clickable(onClick = onClick).padding(vertical = AppSpacing.s).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        IconTile(icon, size = 44)
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.primary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconLarge))
    }
}

/** A short note under a choice's name, as "Offline · Free": its first part as a pill, green when it works offline. */
@Composable
private fun EngineNote(note: String) {
    val parts = note.split(" · ", limit = 2)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        StatusPill(parts[0], if (note.startsWith(tr("Offline"))) AppAccents.green else AppAccents.blue, size = TagSize.Small)
        if (parts.size > 1) Text("· ${parts[1]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---- Reading

@Composable
private fun ReadingExperienceCard(state: ReadingUiState, viewModel: ReadingViewModel, phone: Boolean) {
    val prefs = state.settings
    val code = state.language?.let { LanguageCodes.codeFor(it.name) }
    // A phone reading aloud sets each sentence on its own line, whatever the layout says.
    val layout = !(phone && prefs.showSentencePlay)
    if (!layout && code !in READING_LANGUAGES && !viewModel.canChooseChineseScript && code !in TONE_LANGUAGES) return
    PaneCard(tr("Sentence layout")) {
        if (code in READING_LANGUAGES) {
            SwitchSetting(tr("Show readings"), tr("Furigana or pinyin above the words"), prefs.showReadings, icon = AppIcons.Abc, dense = true) { viewModel.toggleReadings() }
        }
        if (viewModel.canChooseChineseScript) ChineseScriptSetting(state.language?.chineseScript, viewModel::setChineseScript)
        if (code in TONE_LANGUAGES) {
            SwitchSetting(tr("Tone colours"), tr("Colour pinyin by its tone"), prefs.toneColors, icon = AppIcons.Palette, dense = true) { viewModel.toggleToneColors() }
            SwitchSetting(tr("Zhuyin"), tr("Readings in bopomofo instead of pinyin"), prefs.zhuyin, icon = AppIcons.Abc, dense = true) { viewModel.toggleZhuyin() }
        }
        if (layout) LayoutChoice(
            listOf(
                LayoutOption(false, AppIcons.FormatAlignLeft, tr("Continuous"), "layout-continuous"),
                LayoutOption(true, Icons.AutoMirrored.Filled.List, tr("One per line"), "layout-one-per-line"),
            ),
            prefs.splitSentences, phone, viewModel::setSplitSentences,
        )
    }
}

@Composable
private fun AlignmentCard(state: ReadingUiState, viewModel: ReadingViewModel, phone: Boolean) {
    PaneCard(tr("Alignment")) {
        LayoutChoice(
            listOf(
                LayoutOption(false, AppIcons.FormatAlignLeft, tr("Left"), "align-left"),
                LayoutOption(true, AppIcons.FormatAlignJustify, tr("Justified"), "align-justified"),
            ),
            state.settings.justifyText, phone, viewModel::setJustifyText,
        )
    }
}

@Composable
private fun TranslationsCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    PaneCard(tr("Translations")) {
        SwitchSetting(tr("Show translations"), null, prefs.showTranslations, icon = AppIcons.Translate, dense = true) { viewModel.toggleShowTranslations() }
        AppDivider()
        SwitchSetting(tr("Blur translations"), null, prefs.blurTranslations, icon = AppIcons.School, dense = true) { viewModel.toggleBlurTranslations() }
    }
}

@Composable
private fun TranslationDisplayCard(state: ReadingUiState, viewModel: ReadingViewModel, phone: Boolean) {
    PaneCard(tr("Translation layout")) {
        LayoutChoice(
            listOf(
                LayoutOption(true, AppIcons.ViewColumn, tr("Side by side"), "layout-side-by-side"),
                LayoutOption(false, AppIcons.ViewStream, tr("Below"), "layout-below"),
            ),
            state.settings.sideBySideTranslations, phone, viewModel::setSideBySideTranslations,
        )
    }
}

@Composable
private fun TranslationEngineCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    val keyed = prefs.googleTranslateApiKey.isNotBlank()
    fun usable(engine: TranslationEngine) = engine != TranslationEngine.GOOGLE || keyed
    PaneCard(tr("Translation engine")) {
        ChoiceRow(
            icon = AppIcons.Globe,
            title = translationName(prefs.translationEngine, viewModel.localTranslatorName),
            value = null,
            options = viewModel.availableEngines,
            optionLabel = { if (usable(it)) translationName(it, viewModel.localTranslatorName) else tr("{0} – add a key in Settings", translationName(it, viewModel.localTranslatorName)) },
            optionEnabled = ::usable,
            note = { EngineNote(translationNote(prefs.translationEngine)) },
            onSelect = viewModel::setTranslationEngine,
        )
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
private fun PlaybackCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    PaneCard(tr("Playback")) {
        SwitchSetting(tr("Read-aloud controls"), tr("Show the play button and a speaker before each sentence"), prefs.showSentencePlay, icon = AppIcons.PlayArrow, dense = true) { viewModel.toggleSentencePlay() }
        AppDivider()
        SwitchSetting(tr("Speak selected words"), tr("Hear a word when you select it"), prefs.speakWordOnClick, icon = AppIcons.Abc, dense = true) { viewModel.toggleSpeakWordOnClick() }
        AppDivider()
        SwitchSetting(tr("Auto-pause"), tr("Pause after each sentence"), prefs.autoPause, icon = AppIcons.PauseAfterSentence, dense = true, modifier = Modifier.testTag("auto-pause-setting")) { viewModel.toggleAutoPause() }
    }
}

@Composable
private fun VoiceCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    val voices by viewModel.speechVoices.collectAsState()
    val download by viewModel.voiceDownload.collectAsState()
    val downloads by viewModel.setupStates.collectAsState()
    LaunchedEffect(prefs.speechEngine, state.language?.id) { viewModel.loadSpeechVoices() }
    val engine = prefs.speechEngine.takeIf { it in viewModel.speechEngines } ?: SpeechEngine.SYSTEM
    val languageName = state.language?.name.orEmpty()
    PaneCard(tr("Voice")) {
        DropDownRow(AppIcons.Globe, tr("Speech engine"), speechNote(engine), viewModel.speechEngines, engine, ::speechName, "speech-engine-choice", viewModel::setSpeechEngine)
        if (engine != SpeechEngine.SYSTEM) {
            AppDivider()
            if (voices.isNotEmpty()) {
                val chosen = prefs.speechVoices["${prefs.speechEngine.name}:${viewModel.speechLanguage}"]
                DropDownRow(
                    AppIcons.RecordVoiceOver,
                    if (languageName.isEmpty()) tr("Voice") else tr("{0} voice", tr(languageName)),
                    note = null,
                    voices,
                    voices.firstOrNull { it.id == chosen } ?: voices.first(),
                    { it.name },
                    "voice-choice",
                ) { viewModel.setSpeechVoice(it.id) }
            } else {
                val downloading = download?.let { downloads[it.id] }
                val running = downloading is SetupState.Waiting || downloading is SetupState.Running
                Text(
                    if (SetupKind.VOICE in LocalPreparing.current || running) tr("Getting a voice for {0} ready. Until then, the system voice reads this text.", tr(languageName))
                    else tr("No voice for {0} is downloaded, so the system voice reads this text.", tr(languageName)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (download != null && SetupKind.VOICE !in LocalPreparing.current) {
                    if (downloading is SetupState.Failed) Text(tr("Download failed"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    SecondaryButton(
                        if (running) tr("Downloading…") else if (downloading is SetupState.Failed) tr("Try again") else tr("Download"),
                        onClick = viewModel::downloadVoice,
                        enabled = !running,
                        icon = AppIcons.Download,
                        modifier = Modifier.testTag("download-voice"),
                    )
                }
            }
        }
    }
}

/** The speed of the local voices, which the system voice does not take. */
@Composable
private fun SpeedCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    val engine = prefs.speechEngine.takeIf { it in viewModel.speechEngines } ?: SpeechEngine.SYSTEM
    if (engine == SpeechEngine.SYSTEM || !viewModel.speechSpeedAdjustable()) return
    PaneCard(tr("Playback speed")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text("${(prefs.speechSpeed * 100).roundToInt()}%", style = AppText.screenTitle, modifier = Modifier.width(SPEED_FIGURE_WIDTH))
            Column(Modifier.weight(1f)) {
                AppSlider(value = prefs.speechSpeed, onValueChange = viewModel::setSpeechSpeed, valueRange = 0.5f..1.5f, modifier = Modifier.fillMaxWidth().testTag("speech-speed"))
                Row(Modifier.fillMaxWidth()) {
                    Text(tr("Slower"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(tr("Faster"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun TypographyCard(state: ReadingUiState, viewModel: ReadingViewModel) {
    val prefs = state.settings
    PaneCard(tr("Typography")) {
        SettingRow(tr("Font"), dense = true) {
            SelectMenu(
                fontChoices(),
                selected = fontChoice(prefs.readingFont),
                onSelect = { viewModel.setReadingFont(it.id) },
                optionLabel = { it.label },
                buttonModifier = Modifier.testTag("font-choice"),
                optionContent = { Text(it.label, fontFamily = it.fontFamily()) },
                group = { it.group },
            )
        }
        AppDivider()
        SliderSetting(tr("Font size"), "${(prefs.readingFontScale * 100).roundToInt()}%", prefs.readingFontScale, 0.6f..2.5f, viewModel::setFontScale, sliderModifier = Modifier.testTag("font-size"))
        AppDivider()
        SliderSetting(tr("Line height"), "${(prefs.readingLineHeight * 10).roundToInt() / 10f}", prefs.readingLineHeight, 1.0f..3.0f, viewModel::setLineHeight, sliderModifier = Modifier.testTag("line-height"))
    }
}

/**
 * The page's first sentences as the reader shows them, in the chosen font, size and spacing and with
 * their words in their status colours, above their translation when there is one.
 */
@Composable
private fun PreviewCard(state: ReadingUiState) {
    val prefs = state.settings
    val theme = IdiomikoTheme.current
    val sentences = previewSentences(state)
    if (sentences.isEmpty()) return
    val colors = theme.statusColors
    val text = remember(sentences, colors) {
        buildAnnotatedString {
            sentences.forEach { sentence ->
                // Sentences from different paragraphs, or written without a space, keep one between them.
                val first = sentence.items.firstOrNull()?.renderText?.firstOrNull()
                if (length > 0 && !toString().last().isWhitespace() && first?.isWhitespace() == false) append(' ')
                sentence.items.forEach { item ->
                    val start = length
                    append(item.renderText)
                    if (!item.isWord || item.status == TermStatus.WELL_KNOWN || item.status == TermStatus.IGNORED) return@forEach
                    val background = colors.background(item.status)
                    when {
                        item.status == TermStatus.UNKNOWN && colors.unknownAsText -> addStyle(SpanStyle(color = background), start, length)
                        background != Color.Transparent -> addStyle(SpanStyle(background = background, color = colors.onHighlight), start, length)
                    }
                }
            }
        }.let { it.subSequence(it.indexOfFirst { c -> !c.isWhitespace() }.coerceAtLeast(0), it.length) }
    }
    val translation = sentences.map { (state.translations[it.displayText] as? SentenceTranslation.Done)?.text }
        .takeIf { all -> all.all { it != null } }?.joinToString(" ")
    val family = readingFontFamily(prefs.readingFont)
    val size = 18 * prefs.readingFontScale
    PaneCard(tr("Preview")) {
        Column(
            Modifier.fillMaxWidth().testTag("reader-preview"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            val align = if (prefs.justifyText) TextAlign.Justify else TextAlign.Start
            Text(text, style = TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = (size * prefs.readingLineHeight).sp, color = MaterialTheme.colorScheme.onSurface, textAlign = align))
            if (translation != null) {
                Text(translation, style = TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = (size * prefs.readingLineHeight).sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.half)))
            }
        }
    }
}

/** Enough of the page's opening sentences to fill a few lines of the preview. */
private fun previewSentences(state: ReadingUiState): List<RenderedSentence> {
    val spoken = state.page.paragraphs.asSequence().flatMap { it.sentences }.filter { it.displayText.isSpoken() }
    val chosen = mutableListOf<RenderedSentence>()
    for (sentence in spoken) {
        chosen += sentence
        if (chosen.size == PREVIEW_MAX_SENTENCES || chosen.sumOf { it.displayText.length } >= PREVIEW_CHARACTERS) break
    }
    return chosen
}

private const val PREVIEW_CHARACTERS = 220
private const val PREVIEW_MAX_SENTENCES = 4

// ---- Bookmarks

@Composable
private fun BookmarksView(state: ReadingUiState, bookmarks: List<PageBookmark>, viewModel: ReadingViewModel, onOpen: (page: Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val page = state.pageNumber
    // One button that says what it will do to this page: add its bookmark, or take it away.
    val marked = bookmarks.firstOrNull { it.pageNumber == page }
    if (marked == null) {
        SecondaryButton(
            tr("Add bookmark to page {0}", page),
            onClick = { viewModel.addBookmark(tr("Page {0}", page)) },
            modifier = Modifier.fillMaxWidth().testTag("add-bookmark"),
            icon = Icons.Default.Add,
            size = ButtonSize.Large,
            tint = colors.primary,
        )
    } else {
        SecondaryButton(
            tr("Remove bookmark from page {0}", page),
            onClick = { viewModel.deleteBookmark(marked.id) },
            modifier = Modifier.fillMaxWidth().testTag("remove-bookmark"),
            icon = Icons.Default.Close,
            size = ButtonSize.Large,
        )
    }
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
private fun ReaderTools(state: ReadingUiState, bookmarksOpen: Boolean, onBookmarks: () -> Unit, actions: ReaderPaneActions, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A phone keeps the tools to one short row.
    val compact = LocalWindowWidth.current.isCompact
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
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
            if (state.bookmarkable) {
                Box(Modifier.weight(1f)) { ToolTile(AppIcons.Bookmark, tr("Bookmarks"), tr("View or add"), active = bookmarksOpen, tag = "tool-bookmarks", onClick = onBookmarks) }
            }
            Box(Modifier.weight(1f)) { ToolTile(Icons.AutoMirrored.Filled.List, tr("Vocabulary"), tr("Terms on this page"), active = false, tag = "tool-vocabulary") { onClose(); actions.onTermList() } }
        }
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
        if (!LocalWindowWidth.current.isCompact) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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

private val VALUE_MAX_WIDTH = 160.dp
private val TAB_BAR_HEIGHT = 52.dp
private val SELECT_MAX_WIDTH = 260.dp
private val DONE_MIN_WIDTH = 120.dp
private val SPEED_FIGURE_WIDTH = 96.dp
private val SCRIPT_DROPDOWN_WIDTH = 150.dp
