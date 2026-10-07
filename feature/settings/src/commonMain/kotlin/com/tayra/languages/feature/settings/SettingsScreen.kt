package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.components.SliderStepper
import com.tayra.languages.core.ui.components.SwitchSetting
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppTheme
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.tayra.languages.core.domain.service.CommandLineStatus
import com.tayra.languages.core.domain.service.CommandLineTool
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints

class SettingsViewModel(
    private val settings: SettingsRepository,
    /** The desktop app's `tayra` command; other platforms have none. */
    val commandLine: CommandLineTool? = null,
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
            showStreakOnHome = defaults.showStreakOnHome,
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

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Settings"), tr("Personalize your reading experience.")) {
                HeaderButton(if (compact) tr("Reset") else tr("Reset to defaults"), Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AppearanceCard(settings, viewModel)
                        ReadingCard(settings, viewModel)
                        BehaviourCard(settings, viewModel)
                        if (viewModel.commandLine != null) CommandLineCard(viewModel)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PreviewCard(settings)
                        InfoBanner(tr("Changes are saved automatically."))
                    }
                }
            } else {
                AppearanceCard(settings, viewModel)
                ReadingCard(settings, viewModel)
                PreviewCard(settings)
                BehaviourCard(settings, viewModel)
                if (viewModel.commandLine != null) CommandLineCard(viewModel)
                InfoBanner(tr("Changes are saved automatically."))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset to defaults?")) },
            text = { Text(tr("The theme, highlighting, reading size and spacing, reading streak and statistics sample size go back to their defaults.")) },
            confirmButton = { Button(onClick = { confirmReset = false; viewModel.resetToDefaults() }) { Text(tr("Reset")) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
private fun AppearanceCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Appearance"), tr("Customize the look and feel of Tayra."), icon = AppIcons.Palette) {
        SettingRow(tr("Theme"), tr("Choose how Tayra looks."), stackOnCompact = true) {
            Dropdown(
                options = AppThemes.all,
                selected = AppThemes.byId(settings.themeId),
                onSelect = { theme -> viewModel.update { it.copy(themeId = theme.id) } },
                label = null,
                optionLabel = { it.label },
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(300.dp),
                optionContent = { theme ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeSwatch(theme)
                        Text(theme.label)
                    }
                },
            )
        }
        SwitchSetting(tr("Highlight terms by status"), tr("Use mastery colors while reading."), settings.showHighlights, divider = true) { v ->
            viewModel.update { it.copy(showHighlights = v) }
        }
    }
}

/** A theme's page with its text, accent and new and learning word colours, to tell the themes apart in the list. */
@Composable
private fun ThemeSwatch(theme: AppTheme) {
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(theme.readingBackground)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(
            theme.readingText,
            theme.colorScheme.primary,
            theme.statusColors.background(TermStatus.UNKNOWN),
            theme.statusColors.background(TermStatus.NEW_1),
        ).forEach { Box(Modifier.size(10.dp).clip(CircleShape).background(it)) }
    }
}

@Composable
private fun ReadingCard(settings: UserSettings, viewModel: SettingsViewModel) {
    val sliderWidth = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(450.dp)
    ContentCard(tr("Reading"), tr("Adjust the font, text size and spacing for a comfortable reading experience."), iconText = "Aa") {
        SettingRow(tr("Reading font"), tr("The typeface of the text you read."), stackOnCompact = true) {
            Dropdown(
                options = ReadingFont.choices,
                selected = ReadingFont.byId(settings.readingFont),
                onSelect = { font -> viewModel.update { it.copy(readingFont = font.id) } },
                label = null,
                optionLabel = { it.label },
                optionContent = { Text(it.label, fontFamily = it.fontFamily(), style = MaterialTheme.typography.bodyLarge) },
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(300.dp),
            )
        }
        SettingRow(tr("Reading font size"), divider = true, stackOnCompact = true) {
            SliderStepper(
                value = settings.readingFontScale,
                range = 0.6f..2.5f,
                step = 0.1f,
                label = "${kotlin.math.round(settings.readingFontScale * 100).toInt()}%",
                name = tr("reading font size"),
                onChange = { v -> viewModel.update { it.copy(readingFontScale = v) } },
                modifier = sliderWidth,
            )
        }
        SettingRow(tr("Reading line height"), divider = true, stackOnCompact = true) {
            SliderStepper(
                value = settings.readingLineHeight,
                range = 1.0f..3.0f,
                step = 0.1f,
                label = (kotlin.math.round(settings.readingLineHeight * 10) / 10f).toString(),
                name = tr("reading line height"),
                onChange = { v -> viewModel.update { it.copy(readingLineHeight = v) } },
                modifier = sliderWidth,
            )
        }
    }
}

@Composable
private fun BehaviourCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Behaviour"), tr("Control how Tayra behaves on different pages."), icon = Icons.Default.Settings) {
        SwitchSetting(tr("Show reading streak on home page"), tr("Display your current streak on the Home dashboard."), settings.showStreakOnHome) { v ->
            viewModel.update { it.copy(showStreakOnHome = v) }
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
            current == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(tr("Checking whether a terminal finds tayra…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            current.installed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { StatusPill(tr("On your PATH"), StatusTints.ok) }
                    if (current.removable) OutlinedButton(onClick = viewModel::uninstallCommandLine, enabled = !busy) { Text(tr("Remove")) }
                }
                current.location?.let { CommandBox(it) }
                Text(tr("Type tayra in a new terminal window, for example tayra --help."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            available -> SettingRow(tr("Not on your PATH"), tr("Puts tayra on the PATH, so any terminal finds it."), stackOnCompact = true) {
                Button(onClick = viewModel::installCommandLine, enabled = !busy, modifier = Modifier.testTag("install-cli")) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                    else Icon(AppIcons.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Install command-line tool"))
                }
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** A sentence in the language being learned, drawn as the reader draws it with the chosen theme, highlighting, size and spacing. */
@Composable
private fun PreviewCard(settings: UserSettings) {
    val theme = TayraTheme.current
    val learning = LocalLearningLanguage.current?.let { state -> state.languages.firstOrNull { it.first == state.currentId }?.second }
    val sample = PreviewSamples.forLanguage(learning?.let { LanguageCodes.codeFor(it) })
    ContentCard(tr("Reading preview"), tr("Preview updates as you adjust the controls."), icon = AppIcons.MenuBook) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(theme.readingBackground)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(horizontal = 28.dp, vertical = 24.dp),
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
                        else -> withStyle(SpanStyle(background = background, color = colors.onHighlight)) { append(part) }
                    }
                }
            }
            Text(
                text,
                style = TextStyle(
                    fontSize = (18 * settings.readingFontScale).sp,
                    lineHeight = (18 * settings.readingFontScale * settings.readingLineHeight).sp,
                    color = theme.readingText,
                    fontFamily = ReadingFont.byId(settings.readingFont).fontFamily(),
                ),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(tr("New"), colors.background(TermStatus.UNKNOWN), theme)
                LegendDot(tr("Learning"), colors.background(TermStatus.NEW_1), theme)
                LegendDot(tr("Known"), null, theme)
            }
        }
    }
}

/** A coloured dot and a name; known words have no colour, so their dot is an outline. */
@Composable
private fun LegendDot(label: String, color: Color?, theme: AppTheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val dot = Modifier.size(18.dp).clip(CircleShape)
        Box(if (color != null) dot.background(color) else dot.border(1.5.dp, theme.readingText.copy(alpha = 0.35f), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = theme.readingText.copy(alpha = 0.8f))
    }
}

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
