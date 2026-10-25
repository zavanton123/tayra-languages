package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.AppUpdateService
import com.tayra.languages.core.domain.service.CommandLineStatus
import com.tayra.languages.core.domain.service.CommandLineTool
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.MarkedText
import com.tayra.languages.core.ui.components.pushFill
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NavigationRow
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppTheme
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.LocalThemePreview
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.core.ui.theme.fontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(
    private val settings: SettingsRepository,
    /** The desktop app's `tayra` command; other platforms have none. */
    val commandLine: CommandLineTool? = null,
    /** The desktop app's updates; other platforms have none. */
    val updates: AppUpdateService? = null,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    private val _commandLineStatus = MutableStateFlow<CommandLineStatus?>(null)
    val commandLineStatus: StateFlow<CommandLineStatus?> = _commandLineStatus.asStateFlow()

    private val _commandLineBusy = MutableStateFlow(false)
    val commandLineBusy: StateFlow<Boolean> = _commandLineBusy.asStateFlow()

    init {
        commandLine?.let { tool -> viewModelScope.launch { _commandLineStatus.value = tool.status() } }
    }

    fun installCommandLine() = changeCommandLine { it.install() }

    fun uninstallCommandLine() = changeCommandLine { it.uninstall() }

    private fun changeCommandLine(action: suspend (CommandLineTool) -> CommandLineStatus) {
        val tool = commandLine ?: return
        if (_commandLineBusy.value) return
        _commandLineBusy.value = true
        viewModelScope.launch {
            try {
                _commandLineStatus.value = action(tool)
            } finally {
                _commandLineBusy.value = false
            }
        }
    }

    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { settings.update(transform) }

    /** Puts back the defaults of what this screen shows; other screens keep their settings. */
    fun resetToDefaults() = update {
        val defaults = UserSettings()
        it.copy(
            themeId = defaults.themeId,
            showHighlights = defaults.showHighlights,
            readingFontScale = defaults.readingFontScale,
            readingLineHeight = defaults.readingLineHeight,
            readingFont = defaults.readingFont,
            reduceMotion = defaults.reduceMotion,
            showLearningLevels = defaults.showLearningLevels,
            statsSampleSize = defaults.statsSampleSize,
        )
    }
}

@Composable
fun SettingsScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    val wide = LocalWindowWidth.current.isExpanded
    val compact = LocalWindowWidth.current.isCompact

    if (compact) {
        PhoneSettingsScreen(settings, viewModel, onNavigate, onReset = { confirmReset = true })
    } else Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Settings"), tr("Personalise your reading experience.")) {
                HeaderButton(tr("Reset to defaults"), Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        AppearanceCard(settings, viewModel)
                        ReadingCard(settings, viewModel)
                        BehaviourCard(settings, viewModel)
                        if (viewModel.commandLine != null) CommandLineCard(viewModel)
                        viewModel.updates?.let { AppUpdatesCard(it, settings) { on -> viewModel.update { s -> s.copy(checkForUpdates = on) } } }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        PreviewCard(settings)
                        InfoBanner(tr("Changes are saved automatically."))
                        SettingsIndex(settings, onNavigate, compact = false)
                    }
                }
            } else {
                AppearanceCard(settings, viewModel)
                ReadingCard(settings, viewModel)
                PreviewCard(settings)
                BehaviourCard(settings, viewModel)
                SettingsIndex(settings, onNavigate, compact = false)
                if (viewModel.commandLine != null) CommandLineCard(viewModel)
                viewModel.updates?.let { AppUpdatesCard(it, settings) { on -> viewModel.update { s -> s.copy(checkForUpdates = on) } } }
                InfoBanner(tr("Changes are saved automatically."))
            }
        }
    }

    if (confirmReset) {
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset to defaults?")) },
            text = { Text(tr("The theme, highlighting, reading size and spacing and statistics sample size go back to their defaults.")) },
            confirmButton = { PrimaryButton(onClick = { confirmReset = false; viewModel.resetToDefaults() }) { Text(tr("Reset")) } },
            dismissButton = { QuietButton(onClick = { confirmReset = false }) { Text(tr("Cancel")) } },
        )
    }
}

/** A page of the settings index: its name, what it holds, where it leads, and its current value when it has one. */
private class SettingsPage(val title: String, val description: String, val route: Route, val icon: ImageVector, val value: String? = null)

/**
 * Every other settings page, so none is found only through a menu: the preferences, the offline
 * resources, and the data, with clearing it last and in red. Each shows its current value where
 * it has one. A [compact] index leaves the descriptions out, the keyboard shortcuts, and the pages
 * the phone's More sheet already lists, so it holds only what More does not.
 */
@Composable
internal fun SettingsIndex(settings: UserSettings, onNavigate: (Route) -> Unit, compact: Boolean) {
    val learning = LocalLearningLanguage.current?.current?.label?.let { tr(it) }
    val preferences = listOfNotNull(
        SettingsPage(tr("Languages"), tr("Choose what you learn, how translations appear, and the language of the app."), Route.Languages, AppIcons.Globe, learning),
        SettingsPage(tr("Flashcards"), tr("Daily limits and how reviews are scheduled."), Route.FlashcardSettings, AppIcons.Style, trPlural(settings.flashcardNewPerDay, "{0} new a day", "{0} new a day")),
        SettingsPage(tr("Vocabulary"), tr("How much of the language you know already."), Route.VocabularySettings, AppIcons.Abc),
        if (compact) null else SettingsPage(tr("Keyboard shortcuts"), tr("Customise how you navigate and listen while reading."), Route.Shortcuts, AppIcons.Keyboard),
        SettingsPage(tr("About"), tr("The version of the app and where it comes from."), Route.About, Icons.Default.Info),
    )
    val resources = listOf(
        SettingsPage(tr("Dictionaries"), tr("Manage offline word definitions and base-form lookup."), Route.OfflineDictionaries, AppIcons.MenuBook),
        SettingsPage(tr("Speech"), tr("Choose how words and passages are spoken."), Route.Speech, AppIcons.VolumeUp, speechEngineName(settings.speechEngine)),
        SettingsPage(tr("Translation"), tr("Choose how translations are generated and stored."), Route.OfflineTranslation, AppIcons.Translate, translationEngineName(settings.translationEngine)),
        SettingsPage(tr("Course packs"), tr("Download the ready-made courses of a language as one file, or remove them to free space."), Route.CoursePacks, AppIcons.School),
        SettingsPage(tr("Book packs"), tr("Download a pack of free books in a language as one file, or remove them to free space."), Route.BookPacks, AppIcons.Download),
    )
    val data = listOf(
        SettingsPage(tr("Backups"), tr("Protect your library, vocabulary, progress, and preferences."), Route.Backups, AppIcons.UploadFile),
        SettingsPage(tr("Book archive"), tr("Books you put away, to bring back or delete."), Route.ArchivedBooks, AppIcons.BookClosed),
    )
    val card: @Composable (String, ImageVector, List<SettingsPage>, @Composable () -> Unit) -> Unit = { title, icon, all, extra ->
        val pages = if (compact) all.filter { it.route !in ON_MORE_SHEET } else all
        if (pages.isNotEmpty() || !compact) ContentCard(title, subtitle = null, icon = icon, dense = compact) {
            pages.forEach { page ->
                NavigationRow(
                    page.title,
                    onClick = { onNavigate(page.route) },
                    value = page.value,
                    description = if (compact) null else page.description,
                    icon = page.icon,
                    modifier = Modifier.testTag("settings-link-${page.route}"),
                )
            }
            extra()
        }
    }
    card(tr("Preferences"), Icons.Default.Settings, preferences) {}
    card(tr("Offline resources"), AppIcons.Download, resources) {}
    // The destructive action keeps to its own section, after the backups that would undo it.
    card(tr("Data"), AppIcons.UploadFile, data) {
        NavigationRow(
            tr("Clear local data"),
            onClick = { onNavigate(Route.ClearData) },
            description = if (compact) null else tr("Remove your learning data and downloaded files from this device. Your preferences stay."),
            icon = Icons.Default.Delete,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("settings-link-${Route.ClearData}"),
        )
    }
}

/** The pages the phone's More sheet lists, which a phone's settings index leaves out. */
private val ON_MORE_SHEET = setOf<Route>(
    Route.Languages, Route.About, Route.OfflineDictionaries, Route.Speech, Route.OfflineTranslation, Route.CoursePacks, Route.BookPacks, Route.Backups,
)

/** The speech engine's short name, for the index: the system voices, Piper or Kokoro. */
private fun speechEngineName(engine: SpeechEngine): String = when (engine) {
    SpeechEngine.SYSTEM -> tr("System voices")
    SpeechEngine.PIPER -> "Piper"
    SpeechEngine.KOKORO -> "Kokoro"
}

/** The translator's short name, for the index: on this device, MyMemory or Google Translate. */
private fun translationEngineName(engine: TranslationEngine): String = when (engine) {
    TranslationEngine.ARGOS -> tr("Offline")
    TranslationEngine.MYMEMORY -> "MyMemory"
    TranslationEngine.GOOGLE -> "Google Translate"
}

@Composable
private fun AppearanceCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Appearance"), tr("Customise the look and feel of Tayra."), icon = AppIcons.Palette) {
        SettingRow(tr("Theme"), tr("Choose how Tayra looks."), stackOnCompact = true) {
            Dropdown(
                // Light themes, then dark, each under its heading.
                options = AppThemes.all.sortedBy { it.isDark },
                group = { if (it.isDark) tr("Dark") else tr("Light") },
                selected = AppThemes.byId(settings.themeId),
                onSelect = { theme -> viewModel.update { it.copy(themeId = theme.id) } },
                label = null,
                optionLabel = { it.label },
                // The app takes on each theme the pointer comes onto, to see it before choosing.
                onHover = LocalThemePreview.current.let { preview -> { theme -> preview(theme?.id) } },
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(DROPDOWN_WIDTH),
                optionContent = { theme ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                        ThemeSwatch(theme)
                        Text(theme.label)
                    }
                },
            )
        }
        SwitchSetting(tr("Highlight terms by status"), tr("Use status colours while reading."), settings.showHighlights, divider = true) { v ->
            viewModel.update { it.copy(showHighlights = v) }
        }
        SwitchSetting(tr("Reduce motion"), tr("Keep the screens still: no sliding, fading or turning over."), settings.reduceMotion, divider = true) { v ->
            viewModel.update { it.copy(reduceMotion = v) }
        }
    }
}

/** A theme's page with its text, accent and new and learning word colours, to tell the themes apart in the list. */
@Composable
internal fun ThemeSwatch(theme: AppTheme) {
    Row(
        Modifier.clip(AppShapes.small).background(theme.readingBackground)
            .border(AppBorders.hairline(), AppShapes.small).padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        listOf(
            theme.readingText,
            theme.colorScheme.primary,
            theme.statusColors.background(TermStatus.UNKNOWN),
            theme.statusColors.background(TermStatus.NEW_1),
        ).forEach { StatusDot(it) }
    }
}

@Composable
private fun ReadingCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Reading"), tr("Adjust the font, text size and spacing for a comfortable reading experience."), iconText = "Aa") {
        SettingRow(tr("Reading font"), tr("The typeface of the text you read."), stackOnCompact = true) {
            Dropdown(
                options = ReadingFont.choices,
                selected = ReadingFont.byId(settings.readingFont),
                onSelect = { font -> viewModel.update { it.copy(readingFont = font.id) } },
                label = null,
                optionLabel = { it.label },
                optionContent = { Text(it.label, fontFamily = it.fontFamily(), style = MaterialTheme.typography.bodyLarge) },
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(DROPDOWN_WIDTH),
            )
        }
        // Each number is a slider under its name and value, as on the phone.
        AppDivider()
        SliderSetting(
            tr("Reading font size"),
            "${kotlin.math.round(settings.readingFontScale * 100).toInt()}%",
            settings.readingFontScale,
            0.6f..2.5f,
            onChange = { v -> viewModel.update { it.copy(readingFontScale = v) } },
            modifier = Modifier.padding(vertical = AppSpacing.m),
            step = 0.1f,
            sliderModifier = Modifier.semantics { contentDescription = tr("reading font size") },
        )
        AppDivider()
        SliderSetting(
            tr("Reading line height"),
            (kotlin.math.round(settings.readingLineHeight * 10) / 10f).toString(),
            settings.readingLineHeight,
            1.0f..3.0f,
            onChange = { v -> viewModel.update { it.copy(readingLineHeight = v) } },
            modifier = Modifier.padding(top = AppSpacing.m),
            step = 0.1f,
            sliderModifier = Modifier.semantics { contentDescription = tr("reading line height") },
        )
    }
}

@Composable
private fun BehaviourCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Behaviour"), tr("Control how Tayra behaves on different pages."), icon = Icons.Default.Settings) {
        SwitchSetting(tr("Show learning levels"), tr("Statuses 1 to 4 for words being learned, and Ignored, rather than only New, Learning and Known."), settings.showLearningLevels) { v ->
            viewModel.update { it.copy(showLearningLevels = v) }
        }
        SettingRow(tr("Book stats page sample size"), tr("Number of pages used for book statistics."), divider = true) {
            NumberStepper(
                value = settings.statsSampleSize,
                range = UserSettings.MIN_STATS_SAMPLE_SIZE..UserSettings.MAX_STATS_SAMPLE_SIZE,
                name = tr("book stats page sample size"),
            ) { n -> viewModel.update { it.copy(statsSampleSize = n) } }
        }
    }
}

/**
 * The `tayra` command of the desktop app: where a terminal finds it when it is on the PATH, else the
 * button that puts it there.
 */
@Composable
private fun CommandLineCard(viewModel: SettingsViewModel) {
    val status by viewModel.commandLineStatus.collectAsStateWithLifecycle()
    val busy by viewModel.commandLineBusy.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val available = viewModel.commandLine?.available == true
    ContentCard(
        tr("Command-line tool"),
        tr("Use your library from a terminal with the tayra command, and let AI agents work with it."),
        icon = AppIcons.Terminal,
    ) {
        val current = status
        when {
            current == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                AppSpinner(size = SpinnerSize.Small)
                Text(tr("Checking whether a terminal finds tayra…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            current.installed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { StatusPill(tr("On your PATH"), StatusTints.ok) }
                    if (current.removable) SecondaryButton(onClick = viewModel::uninstallCommandLine, enabled = !busy) { Text(tr("Remove")) }
                }
                current.location?.let { CommandBox(it) }
                Text(tr("Type tayra in a new terminal window, for example tayra --help."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            available -> SettingRow(tr("Not on your PATH"), tr("Puts tayra on the PATH, so any terminal finds it."), stackOnCompact = true) {
                SecondaryButton(tr("Install command-line tool"), onClick = viewModel::installCommandLine, modifier = Modifier.testTag("install-cli"), icon = AppIcons.Terminal, enabled = !busy, loading = busy)
            }
            else -> Text(tr("The tayra command comes with the installed app. From the sources, run it with ./gradlew :cli:run."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        current?.pathChangedIn?.let { changed ->
            Text(
                if (changed == "PATH") tr("Added to your PATH. Open a new terminal window to use it.")
                else tr("Added ~/.local/bin to your PATH in {0}. Open a new terminal window to use it.", changed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
            )
        }
        current?.error?.let { Text(tr("Could not change the PATH: {0}", it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
    }
}

/** A path or command in a monospace box, selectable for copying. */
@Composable
private fun CommandBox(text: String) {
    val colors = MaterialTheme.colorScheme
    SelectionContainer {
        Text(
            text,
            Modifier.fillMaxWidth().clip(AppShapes.compact).background(colors.surfaceVariant.copy(alpha = AppAlpha.muted))
                .border(AppBorders.hairline(), AppShapes.compact).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** A sentence in the language being learned, drawn as the reader draws it with the chosen theme, highlighting, size and spacing. */
@Composable
private fun PreviewCard(settings: UserSettings) {
    ContentCard(tr("Reading preview"), tr("Preview updates as you adjust the controls."), icon = AppIcons.MenuBook) { PreviewBox(settings) }
}

/** The preview itself: the sample sentence on the theme's page with the legend under it; [padding] is the page's inner margin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreviewBox(settings: UserSettings, padding: Dp = AppSpacing.xxl) {
    val theme = TayraTheme.current
    val learning = LocalLearningLanguage.current?.let { state -> state.languages.firstOrNull { it.first == state.currentId }?.second }
    val sample = PreviewSamples.forLanguage(learning?.let { LanguageCodes.codeFor(it) })
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.panel).background(theme.readingBackground)
            .border(AppBorders.hairline(), AppShapes.panel).padding(horizontal = padding + AppSpacing.xs, vertical = padding),
    ) {
        val colors = theme.statusColors
        val text = buildAnnotatedString {
            sample.forEach { (part, status) ->
                // As in the reader, known words are never coloured.
                val shown = status != null && settings.showHighlights && status != TermStatus.WELL_KNOWN
                val background = if (shown) colors.background(status!!) else Color.Transparent
                when {
                    background == Color.Transparent -> append(part)
                    status == TermStatus.UNKNOWN && colors.unknownAsText -> withStyle(SpanStyle(color = background)) { append(part) }
                    else -> {
                        pushFill(background)
                        withStyle(SpanStyle(color = colors.onHighlight)) { append(part) }
                        pop()
                    }
                }
            }
        }
        MarkedText(
            text,
            style = TextStyle(
                fontSize = (18 * settings.readingFontScale).sp,
                lineHeight = (18 * settings.readingFontScale * settings.readingLineHeight).sp,
                color = theme.readingText,
                fontFamily = ReadingFont.byId(settings.readingFont).fontFamily(),
            ),
            modifier = Modifier.padding(vertical = AppSpacing.s),
        )
        AppDivider(Modifier.padding(vertical = AppSpacing.l))
        // Whole items wrap to a second line in large text, never a word inside its label.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            LegendDot(tr("New"), colors.background(TermStatus.UNKNOWN), theme)
            LegendDot(tr("Learning"), colors.background(TermStatus.NEW_1), theme)
            LegendDot(tr("Known"), null, theme)
        }
    }
}

/** A coloured dot and a name; known words have no colour, so their dot is an outline. */
@Composable
private fun LegendDot(label: String, color: Color?, theme: AppTheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (color != null) StatusDot(color, size = LEGEND_DOT)
        else Box(Modifier.size(LEGEND_DOT).border(AppBorders.selected(theme.readingText.copy(alpha = AppAlpha.disabled)), AppShapes.circle))
        Spacer(Modifier.width(AppSpacing.s))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = theme.readingText.copy(alpha = AppAlpha.strong), maxLines = 1, softWrap = false)
    }
}

private val DROPDOWN_WIDTH = 300.dp
private val LEGEND_DOT = 18.dp

/** Preview sentences per language code: the text in parts, each word with the status it is shown with. */
private object PreviewSamples {
    private val U = TermStatus.UNKNOWN
    private val L1 = TermStatus.NEW_1
    private val L3 = TermStatus.LEARNING_3
    private val K = TermStatus.WELL_KNOWN

    private fun sentence(vararg parts: Pair<String, TermStatus?>) = parts.toList()

    private val samples: Map<String, List<Pair<String, TermStatus?>>> = mapOf(
        "pt" to sentence("Durante a " to null, "semana" to U, ", eu " to null, "acordo" to L1, " " to null, "cedo" to K, " e tomo " to null, "café" to L3, " da manhã em casa." to null),
        "es" to sentence("Durante la " to null, "semana" to U, ", me " to null, "levanto" to L1, " " to null, "temprano" to K, " y " to null, "desayuno" to L3, " en casa." to null),
        "fr" to sentence("Pendant la " to null, "semaine" to U, ", je me " to null, "lève" to L1, " " to null, "tôt" to K, " et je prends le " to null, "petit-déjeuner" to L3, " à la maison." to null),
        "it" to sentence("Durante la " to null, "settimana" to U, " mi " to null, "sveglio" to L1, " " to null, "presto" to K, " e faccio " to null, "colazione" to L3, " a casa." to null),
        "de" to sentence("Unter der " to null, "Woche" to U, " stehe ich " to null, "früh" to L1, " auf und " to null, "frühstücke" to L3, " " to null, "zu Hause" to K, "." to null),
        "nl" to sentence("Door de " to null, "week" to U, " sta ik " to null, "vroeg" to L1, " op en " to null, "ontbijt" to L3, " ik " to null, "thuis" to K, "." to null),
        "ru" to sentence("По " to null, "будням" to U, " я " to null, "встаю" to L1, " " to null, "рано" to K, " и " to null, "завтракаю" to L3, " дома." to null),
        "pl" to sentence("W " to null, "tygodniu" to U, " " to null, "wstaję" to L1, " " to null, "wcześnie" to K, " i jem " to null, "śniadanie" to L3, " w domu." to null),
        "cs" to sentence("Přes " to null, "týden" to U, " " to null, "vstávám" to L1, " " to null, "brzy" to K, " a " to null, "snídám" to L3, " doma." to null),
        "en" to sentence("During the " to null, "week" to U, ", I " to null, "wake up" to L1, " " to null, "early" to K, " and have " to null, "breakfast" to L3, " at home." to null),
    )

    fun forLanguage(code: String?): List<Pair<String, TermStatus?>> = samples[code] ?: samples.getValue("en")
}
