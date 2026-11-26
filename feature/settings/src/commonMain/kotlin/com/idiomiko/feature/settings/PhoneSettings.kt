package com.idiomiko.feature.settings

import com.idiomiko.core.ui.theme.fontChoice
import com.idiomiko.core.ui.theme.fontChoices
import com.idiomiko.core.ui.components.ScrollColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.NavigationRow
import com.idiomiko.core.ui.components.NumberStepper
import com.idiomiko.core.ui.components.PickerSheet
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SliderSetting
import com.idiomiko.core.ui.components.SwitchSetting
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.fontFamily

/**
 * The settings page on a phone: compact cards of plain rows, the theme and the font picked from a
 * bottom sheet, the sliders under their labels, and Reset in the top bar.
 */
@Composable
internal fun PhoneSettingsScreen(settings: UserSettings, viewModel: SettingsViewModel, onNavigate: (Route) -> Unit, onBack: (() -> Unit)?, onReset: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var themeSheet by remember { mutableStateOf(false) }
    var fontSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            AppTopBar(title = tr("General"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS, actions = {
                QuietButton(onClick = onReset) { Text(tr("Reset"), style = MaterialTheme.typography.titleMedium) }
            })
        },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            ContentCard(tr("Appearance"), subtitle = null, icon = AppIcons.Palette, dense = true) {
                NavigationRow(tr("Choose a theme"), onClick = { themeSheet = true }, modifier = Modifier.testTag("phone-theme"), value = AppThemes.byId(settings.themeId).label)
            }
            ContentCard(tr("Reading"), subtitle = null, iconText = "Aa", dense = true) {
                // The preview comes first, so the font and spacing are seen changing while they are set.
                PreviewBox(settings, padding = AppSpacing.l)
                Spacer(Modifier.height(AppSpacing.s))
                NavigationRow(tr("Reading font"), onClick = { fontSheet = true }, modifier = Modifier.testTag("phone-font"), value = fontChoice(settings.readingFont).label)
                OwnFontsSetting(viewModel, dense = true)
                AppDivider()
                PhoneSlider(
                    tr("Font size"),
                    "${kotlin.math.round(settings.readingFontScale * 100).toInt()}%",
                    settings.readingFontScale,
                    0.6f..2.5f,
                    name = tr("reading font size"),
                ) { v -> viewModel.update { it.copy(readingFontScale = v) } }
                AppDivider()
                PhoneSlider(
                    tr("Line height"),
                    (kotlin.math.round(settings.readingLineHeight * 10) / 10f).toString(),
                    settings.readingLineHeight,
                    1.0f..3.0f,
                    name = tr("reading line height"),
                ) { v -> viewModel.update { it.copy(readingLineHeight = v) } }
            }
            ContentCard(tr("Display word statuses"), subtitle = null, icon = AppIcons.Visibility, dense = true, modifier = Modifier.testTag("word-statuses")) {
                SwitchSetting(tr("Show learning levels"), description = tr("Statuses 1 to 4 for words being learned, and Ignored, rather than only New, Learning and Known."), checked = settings.showLearningLevels, dense = true) { v -> viewModel.update { it.copy(showLearningLevels = v) } }
            }
        }
    }
    if (themeSheet) {
        PickerSheet(tr("Theme"), AppThemes.all.sortedBy { it.isDark }, selected = AppThemes.byId(settings.themeId), label = { it.label }, group = { if (it.isDark) tr("Dark") else tr("Light") }, onDismiss = { themeSheet = false }, onPick = { theme ->
            themeSheet = false
            viewModel.update { it.copy(themeId = theme.id) }
        }) { theme -> ThemeSwatch(theme) }
    }
    if (fontSheet) {
        PickerSheet(
            tr("Reading font"), fontChoices(), selected = fontChoice(settings.readingFont), label = { it.label },
            labelContent = { font -> Text(font.label, fontFamily = font.fontFamily(), style = MaterialTheme.typography.bodyLarge) },
            onDismiss = { fontSheet = false },
            onPick = { font -> fontSheet = false; viewModel.update { it.copy(readingFont = font.id) } },
            group = { it.group },
        )
    }
}

/** A reading slider in tenths, named [name] for screen readers and tagged by it. */
@Composable
private fun PhoneSlider(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, name: String, onChange: (Float) -> Unit) {
    SliderSetting(
        title, valueLabel, value, range, onChange,
        modifier = Modifier.padding(top = AppSpacing.m, bottom = AppSpacing.xs),
        step = 0.1f,
        sliderModifier = Modifier.semantics { contentDescription = name }.testTag("phone-slider-$name"),
    )
}
