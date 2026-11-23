package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.domain.service.SpeechEngine
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
import androidx.compose.material.icons.filled.AccountCircle
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
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.service.AppUpdateService
import com.idiomiko.core.domain.bridge.BrowserExtensionBridge
import com.idiomiko.core.domain.service.CommandLineStatus
import com.idiomiko.core.domain.service.CommandLineTool
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.MarkedText
import com.idiomiko.core.ui.components.pushFill
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.HeaderButton
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.NavigationRow
import com.idiomiko.core.ui.components.NumberStepper
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.StatusDot
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.SwitchSetting
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.LocalThemePreview
import com.idiomiko.core.ui.theme.ReadingFont
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.idiomiko.core.ui.theme.fontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(
    private val settings: SettingsRepository,
    /** The desktop app's `idiomiko` command; other platforms have none. */
    val commandLine: CommandLineTool? = null,
    /** The desktop app's updates; other platforms have none. */
    val updates: AppUpdateService? = null,
    /** The desktop app's connection to the browser extension; other platforms have none. */
    val browserBridge: BrowserExtensionBridge? = null,
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
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("General"), null) {
                HeaderButton(tr("Reset to defaults"), Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        AppearanceCard(settings, viewModel)
                        ReadingCard(settings, viewModel)
                        BehaviourCard(settings, viewModel)
                        if (viewModel.commandLine != null) CommandLineCard(viewModel)
                        viewModel.browserBridge?.let { BrowserExtensionCard(it) }
                        viewModel.updates?.let { AppUpdatesCard(it, settings) { on -> viewModel.update { s -> s.copy(checkForUpdates = on) } } }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        PreviewCard(settings)
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
                viewModel.browserBridge?.let { BrowserExtensionCard(it) }
                viewModel.updates?.let { AppUpdatesCard(it, settings) { on -> viewModel.update { s -> s.copy(checkForUpdates = on) } } }
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
        if (compact) null else SettingsPage(tr("Hotkeys"), tr("Customise how you navigate and listen while reading."), Route.Shortcuts, AppIcons.Keyboard),
        SettingsPage(tr("About"), tr("The version of the app and where it comes from."), Route.About, Icons.Default.Info),
        SettingsPage(tr("Experiments"), tr("Turn on features that are still being built."), Route.ExperimentalFeatures, AppIcons.Lightbulb),
    )
    val card: @Composable (String, ImageVector, List<SettingsPage>, (@Composable () -> Unit)?) -> Unit = { title, icon, all, extra ->
        val pages = if (compact) all.filter { it.route !in ON_MORE_SHEET } else all
        if (pages.isNotEmpty() || !compact || extra != null) ContentCard(title, subtitle = null, icon = icon, dense = compact) {
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
            extra?.invoke()
        }
    }
    // The wide bar's Settings menu leads to these pages; a phone reaches its flashcard and vocabulary settings only from here.
    if (compact) card(tr("Preferences"), Icons.Default.Settings, preferences, null)
    // The destructive action keeps to its own section, on its own; the menus lead to Speech, Translation, the account, backups and the archive.
    card(tr("Data"), AppIcons.UploadFile, emptyList()) {
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
    Route.Languages, Route.About, Route.ExperimentalFeatures, Route.Speech, Route.OfflineTranslation, Route.Backups, Route.Account,
)



@Composable
private fun AppearanceCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Appearance"), null, icon = AppIcons.Palette) {
        SettingRow(tr("Choose a theme"), null, stackOnCompact = true) {
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
    ContentCard(tr("Reading"), null, iconText = "Aa") {
        SettingRow(tr("Reading font"), null, stackOnCompact = true) {
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
    ContentCard(tr("Behaviour"), tr("Control how Idiomiko behaves on different pages."), icon = Icons.Default.Settings) {
        SwitchSetting(tr("Show learning levels"), tr("Statuses 1 to 4 for words being learned, and Ignored, rather than only New, Learning and Known."), settings.showLearningLevels) { v ->
            viewModel.update { it.copy(showLearningLevels = v) }
        }
        SettingRow(tr("Daily goal"), tr("Minutes of reading and flashcards a day. Home shows how far today has got."), divider = true, modifier = Modifier.testTag("daily-goal")) {
            DailyGoalStepper(settings, viewModel)
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

/** The daily goal in minutes, five at a time. */
@Composable
internal fun DailyGoalStepper(settings: UserSettings, viewModel: SettingsViewModel) {
    NumberStepper(
        value = settings.dailyGoalMinutes,
        range = UserSettings.MIN_DAILY_GOAL..UserSettings.MAX_DAILY_GOAL,
        name = tr("daily goal"),
        step = DAILY_GOAL_STEP,
    ) { n -> viewModel.update { it.copy(dailyGoalMinutes = n) } }
}

private const val DAILY_GOAL_STEP = 5

/**
 * The `idiomiko` command of the desktop app: where a terminal finds it when it is on the PATH, else the
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
        tr("Use your library from a terminal with the idiomiko command, and let AI agents work with it."),
        icon = AppIcons.Terminal,
    ) {
        val current = status
        when {
            current == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                AppSpinner(size = SpinnerSize.Small)
                Text(tr("Checking whether a terminal finds idiomiko…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            current.installed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { StatusPill(tr("On your PATH"), StatusTints.ok) }
                    if (current.removable) SecondaryButton(onClick = viewModel::uninstallCommandLine, enabled = !busy) { Text(tr("Remove")) }
                }
                current.location?.let { CommandBox(it) }
                Text(tr("Type idiomiko in a new terminal window, for example idiomiko --help."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            available -> SettingRow(tr("Not on your PATH"), tr("Puts idiomiko on the PATH, so any terminal finds it."), stackOnCompact = true) {
                SecondaryButton(tr("Install command-line tool"), onClick = viewModel::installCommandLine, modifier = Modifier.testTag("install-cli"), icon = AppIcons.Terminal, enabled = !busy, loading = busy)
            }
            else -> Text(tr("The idiomiko command comes with the installed app. From the sources, run it with ./gradlew :cli:run."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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

/**
 * The Idiomiko browser extension: whether the app is ready for it, the browsers paired with it, and
 * the code that pairs another.
 */
@Composable
private fun BrowserExtensionCard(bridge: BrowserExtensionBridge) {
    val state by bridge.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    ContentCard(
        tr("Browser extension"),
        tr("Watch YouTube with your Idiomiko words: coloured subtitles, lookups and saving words, while the app is open."),
        icon = AppIcons.Link,
        modifier = Modifier.testTag("browser-extension"),
    ) {
        state.error?.let { Text(tr("Could not start: {0}", it), style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        val code = state.code
        if (code != null) {
            Text(tr("Type this code into the Idiomiko extension:"), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CommandBox(code.chunked(3).joinToString(" "), Modifier.testTag("pairing-code"), MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.width(AppSpacing.m))
                SecondaryButton(onClick = bridge::cancelPairing) { Text(tr("Cancel")) }
            }
            Text(tr("The code works once, for five minutes."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        } else if (state.port != null) {
            val automatic = state.automaticIn.joinToString(", ")
            SettingRow(
                tr("Ready"),
                if (automatic.isEmpty()) tr("Pair each browser once to let its extension use your words.")
                else tr("The extension connects by itself in {0}. Pair other browsers with a code.", automatic),
                stackOnCompact = true,
            ) {
                SecondaryButton(tr("Pair with a code"), onClick = bridge::startPairing, modifier = Modifier.testTag("pair-browser"), icon = AppIcons.Link)
            }
        }
        if (state.browsers.isNotEmpty()) {
            Text(tr("Paired browsers"), style = MaterialTheme.typography.titleSmall)
            state.browsers.forEach { browser ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(browser.browser, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    SecondaryButton(onClick = { bridge.unpair(browser.id) }) { Text(tr("Remove")) }
                }
            }
        }
    }
}

/** A path or command in a monospace box, selectable for copying. */
@Composable
private fun CommandBox(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    val colors = MaterialTheme.colorScheme
    SelectionContainer {
        Text(
            text,
            modifier.fillMaxWidth().clip(AppShapes.compact).background(colors.surfaceVariant.copy(alpha = AppAlpha.muted))
                .border(AppBorders.hairline(), AppShapes.compact).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
            style = style,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** A sentence in the language being learned, drawn as the reader draws it with the chosen theme, highlighting, size and spacing. */
@Composable
private fun PreviewCard(settings: UserSettings) {
    ContentCard(tr("Reading preview"), null, icon = AppIcons.MenuBook) { PreviewBox(settings) }
}

/** The preview itself: the sample sentence on the theme's page with the legend under it; [padding] is the page's inner margin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreviewBox(settings: UserSettings, padding: Dp = AppSpacing.xxl) {
    val theme = IdiomikoTheme.current
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
                val shown = status != null && status != TermStatus.WELL_KNOWN
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
