package com.tayra.languages.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.NumberStepper
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.RoundSlider
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily

/**
 * The settings page on a phone: compact cards of plain rows, the theme and the font picked from a
 * bottom sheet, the sliders under their labels, and Reset in the top bar.
 */
@Composable
internal fun PhoneSettingsScreen(settings: UserSettings, viewModel: SettingsViewModel, onNavigate: (Route) -> Unit, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var themeSheet by remember { mutableStateOf(false) }
    var fontSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            AppTopBar(title = tr("Settings"), onNavigate = onNavigate, section = NavSection.SETTINGS, actions = {
                QuietButton(onClick = onReset) { Text(tr("Reset"), style = MaterialTheme.typography.titleMedium) }
            })
        },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Personalize your reading experience."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            ContentCard(tr("Appearance"), subtitle = null, icon = AppIcons.Palette, dense = true) {
                PickerRow(tr("Theme"), AppThemes.byId(settings.themeId).label, tag = "phone-theme") { themeSheet = true }
                AppDivider()
                SwitchRow(tr("Highlight terms by status"), settings.showHighlights) { v -> viewModel.update { it.copy(showHighlights = v) } }
            }
            ContentCard(tr("Reading"), subtitle = null, iconText = "Aa", dense = true) {
                PickerRow(tr("Reading font"), ReadingFont.byId(settings.readingFont).label, tag = "phone-font") { fontSheet = true }
                AppDivider()
                SliderRow(
                    tr("Font size"),
                    "${kotlin.math.round(settings.readingFontScale * 100).toInt()}%",
                    settings.readingFontScale,
                    0.6f..2.5f,
                    step = 0.1f,
                    name = tr("reading font size"),
                ) { v -> viewModel.update { it.copy(readingFontScale = v) } }
                AppDivider()
                SliderRow(
                    tr("Line height"),
                    (kotlin.math.round(settings.readingLineHeight * 10) / 10f).toString(),
                    settings.readingLineHeight,
                    1.0f..3.0f,
                    step = 0.1f,
                    name = tr("reading line height"),
                ) { v -> viewModel.update { it.copy(readingLineHeight = v) } }
            }
            ContentCard(tr("Reading preview"), subtitle = null, icon = AppIcons.MenuBook, dense = true) { PreviewBox(settings, padding = 16.dp) }
            ContentCard(tr("Behaviour"), subtitle = null, icon = Icons.Default.Settings, dense = true) {
                SwitchRow(tr("Show reading streak on home page"), settings.showStreakOnHome) { v -> viewModel.update { it.copy(showStreakOnHome = v) } }
                AppDivider()
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Book stats page sample size"), style = MaterialTheme.typography.bodyLarge)
                    NumberStepper(
                        value = settings.statsSampleSize,
                        range = UserSettings.MIN_STATS_SAMPLE_SIZE..UserSettings.MAX_STATS_SAMPLE_SIZE,
                        name = tr("book stats page sample size"),
                    ) { n -> viewModel.update { it.copy(statsSampleSize = n) } }
                }
            }
        }
    }
    if (themeSheet) {
        PickerSheet(tr("Theme"), AppThemes.all, selected = AppThemes.byId(settings.themeId), label = { it.label }, onDismiss = { themeSheet = false }, onPick = { theme ->
            themeSheet = false
            viewModel.update { it.copy(themeId = theme.id) }
        }) { theme -> ThemeSwatch(theme) }
    }
    if (fontSheet) {
        PickerSheet(
            tr("Reading font"), ReadingFont.choices, selected = ReadingFont.byId(settings.readingFont), label = { it.label },
            labelContent = { font -> Text(font.label, fontFamily = font.fontFamily(), style = MaterialTheme.typography.bodyLarge) },
            onDismiss = { fontSheet = false },
            onPick = { font -> fontSheet = false; viewModel.update { it.copy(readingFont = font.id) } },
        )
    }
}

/** A row that opens a picker: the label, the current value and a chevron. */
@Composable
private fun PickerRow(label: String, value: String, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** The label with the value at the end, and the slider under them, moving in [step]s. */
@Composable
private fun SliderRow(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, step: Float, name: String, onChange: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Dividing by the number of steps per unit, not multiplying by the step, lands on the nearest float of a value such as 1.3.
    val perUnit = kotlin.math.round(1 / step)
    fun snap(v: Float) = (kotlin.math.round(v * perUnit) / perUnit).coerceIn(range.start, range.endInclusive)
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        RoundSlider(
            value = current,
            onValueChange = { v -> snap(v).let { if (it != current) onChange(it) } },
            valueRange = range,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = name }.testTag("phone-slider-$name"),
        )
    }
}
