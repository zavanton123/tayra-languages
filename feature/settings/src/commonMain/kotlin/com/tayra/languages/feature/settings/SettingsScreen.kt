package com.tayra.languages.feature.settings

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
import androidx.compose.ui.text.font.FontFamily
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
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
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
            showHighlights = defaults.showHighlights,
            readingFontScale = defaults.readingFontScale,
            readingLineHeight = defaults.readingLineHeight,
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
            ScreenHeader("Settings", "Personalize your reading experience.") {
                HeaderButton(if (compact) "Reset" else "Reset to defaults", Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.45f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        AppearanceCard(settings, viewModel)
                        ReadingCard(settings, viewModel)
                        BehaviourCard(settings, viewModel)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PreviewCard(settings)
                        InfoBanner("Changes are saved automatically.")
                    }
                }
            } else {
                AppearanceCard(settings, viewModel)
                ReadingCard(settings, viewModel)
                PreviewCard(settings)
                BehaviourCard(settings, viewModel)
                InfoBanner("Changes are saved automatically.")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset to defaults?") },
            text = { Text("The theme, highlighting, reading size and spacing, reading streak and statistics sample size go back to their defaults.") },
            confirmButton = { Button(onClick = { confirmReset = false; viewModel.resetToDefaults() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AppearanceCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard("Appearance", "Customize the look and feel of Tayra.", icon = AppIcons.Palette) {
        SettingRow("Theme", "Choose how Tayra looks.", stackOnCompact = true) {
            Dropdown(
                options = AppThemes.all,
                selected = AppThemes.byId(settings.themeId),
                onSelect = { theme -> viewModel.update { it.copy(themeId = theme.id) } },
                label = null,
                optionLabel = { it.label },
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(250.dp),
            )
        }
        SwitchSetting("Highlight terms by status", "Use mastery colors while reading.", settings.showHighlights, divider = true) { v ->
            viewModel.update { it.copy(showHighlights = v) }
        }
    }
}

@Composable
private fun ReadingCard(settings: UserSettings, viewModel: SettingsViewModel) {
    val sliderWidth = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(450.dp)
    ContentCard("Reading", "Adjust the text size and spacing for a comfortable reading experience.", iconText = "Aa") {
        SettingRow("Reading font size", stackOnCompact = true) {
            SliderStepper(
                value = settings.readingFontScale,
                range = 0.6f..2.5f,
                step = 0.1f,
                label = "${kotlin.math.round(settings.readingFontScale * 100).toInt()}%",
                name = "reading font size",
                onChange = { v -> viewModel.update { it.copy(readingFontScale = v) } },
                modifier = sliderWidth,
            )
        }
        SettingRow("Reading line height", divider = true, stackOnCompact = true) {
            SliderStepper(
                value = settings.readingLineHeight,
                range = 1.0f..3.0f,
                step = 0.1f,
                label = (kotlin.math.round(settings.readingLineHeight * 10) / 10f).toString(),
                name = "reading line height",
                onChange = { v -> viewModel.update { it.copy(readingLineHeight = v) } },
                modifier = sliderWidth,
            )
        }
    }
}

@Composable
private fun BehaviourCard(settings: UserSettings, viewModel: SettingsViewModel) {
    ContentCard("Behaviour", "Control how Tayra behaves on different pages.", icon = Icons.Default.Settings) {
        SwitchSetting("Show reading streak on home page", "Display your current streak on the Home dashboard.", settings.showStreakOnHome) { v ->
            viewModel.update { it.copy(showStreakOnHome = v) }
        }
        SettingRow("Book stats page sample size", "Number of pages used for book statistics.", divider = true) {
            NumberStepper(
                value = settings.statsSampleSize,
                range = UserSettings.MIN_STATS_SAMPLE_SIZE..UserSettings.MAX_STATS_SAMPLE_SIZE,
                name = "book stats page sample size",
            ) { n -> viewModel.update { it.copy(statsSampleSize = n) } }
        }
    }
}

/** A sentence in the language being learned, drawn as the reader draws it with the chosen theme, highlighting, size and spacing. */
@Composable
private fun PreviewCard(settings: UserSettings) {
    val theme = TayraTheme.current
    val learning = LocalLearningLanguage.current?.let { state -> state.languages.firstOrNull { it.first == state.currentId }?.second }
    val sample = PreviewSamples.forLanguage(learning?.let { LanguageCodes.codeFor(it) })
    ContentCard("Reading preview", "Preview updates as you adjust the controls.", icon = AppIcons.MenuBook) {
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
                    fontFamily = FontFamily.Serif,
                ),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot("New", colors.background(TermStatus.UNKNOWN), theme)
                LegendDot("Learning", colors.background(TermStatus.NEW_1), theme)
                LegendDot("Known", null, theme)
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
