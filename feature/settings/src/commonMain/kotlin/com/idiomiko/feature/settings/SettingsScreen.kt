package com.idiomiko.feature.settings

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
import androidx.compose.material.icons.filled.Refresh
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
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.MarkedText
import com.idiomiko.core.ui.components.pushFill
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.Dropdown
import com.idiomiko.core.ui.components.HeaderButton
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.NumberStepper
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.StatusDot
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { settings.update(transform) }

    /** Puts back the defaults of what this screen shows; other screens keep their settings. */
    fun resetToDefaults() = update {
        val defaults = UserSettings()
        it.copy(
            themeId = defaults.themeId,
            readingFontScale = defaults.readingFontScale,
            readingLineHeight = defaults.readingLineHeight,
            readingFont = defaults.readingFont,
            showLearningLevels = defaults.showLearningLevels,
        )
    }
}

@Composable
fun SettingsScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    val compact = LocalWindowWidth.current.isCompact

    if (compact) {
        PhoneSettingsScreen(settings, viewModel, onNavigate, onReset = { confirmReset = true })
    } else Scaffold(
        topBar = { AppTopBar(title = "Idiomiko", onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.GENERAL, padding, onNavigate) { inner ->
            // The room beside the settings sidebar, which may hold one column only.
            val wide = LocalWindowWidth.current.isExpanded
            PageColumn(inner) {
                ScreenHeader(tr("General"), null) {
                    HeaderButton(tr("Reset to defaults"), Icons.Default.Refresh, onClick = { confirmReset = true })
                }
                if (wide) {
                    // The settings and the preview share the width, so the preview's sentence reads as on a page.
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                            AppearanceCard(settings, viewModel)
                            ReadingCard(settings, viewModel)
                            WordStatusesCard(settings, viewModel)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                            PreviewCard(settings)
                        }
                    }
                } else {
                    AppearanceCard(settings, viewModel)
                    ReadingCard(settings, viewModel)
                    PreviewCard(settings)
                    WordStatusesCard(settings, viewModel)
                }
            }
        }
    }

    if (confirmReset) {
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset to defaults?")) },
            text = { Text(tr("The theme, highlighting, reading size and spacing go back to their defaults.")) },
            confirmButton = { PrimaryButton(onClick = { confirmReset = false; viewModel.resetToDefaults() }) { Text(tr("Reset")) } },
            dismissButton = { QuietButton(onClick = { confirmReset = false }) { Text(tr("Cancel")) } },
        )
    }
}

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

/** Which statuses words are shown with. */
@Composable
private fun WordStatusesCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard(tr("Display word statuses"), null, icon = AppIcons.Visibility, modifier = Modifier.testTag("word-statuses")) {
        SwitchSetting(tr("Show learning levels"), tr("Statuses 1 to 4 for words being learned, and Ignored, rather than only New, Learning and Known."), settings.showLearningLevels) { v ->
            viewModel.update { it.copy(showLearningLevels = v) }
        }
    }
}

/** How many pages a book's known share is counted from. */
@Composable
internal fun StatsSampleStepper(settings: UserSettings, viewModel: SettingsViewModel) {
    NumberStepper(
        value = settings.statsSampleSize,
        range = UserSettings.MIN_STATS_SAMPLE_SIZE..UserSettings.MAX_STATS_SAMPLE_SIZE,
        name = tr("book stats page sample size"),
    ) { n -> viewModel.update { it.copy(statsSampleSize = n) } }
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
