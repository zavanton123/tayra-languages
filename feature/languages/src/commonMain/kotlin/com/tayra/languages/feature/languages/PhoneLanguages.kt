package com.tayra.languages.feature.languages

import com.tayra.languages.core.ui.components.ScrollColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LanguagePickerSheet
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * The Languages page on a phone: the three choices as cards, each with a row that opens a bottom
 * sheet of the languages with their flags; the learning one is tinted.
 */
@Composable
internal fun PhoneLanguagesScreen(settings: UserSettings, learning: LearningLanguageState?, viewModel: LanguagesViewModel, onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val native = LanguageCatalog.nativeOption(settings.nativeLanguage)
    val learningCode = learning?.currentName?.let(LanguageCodes::codeFor)
    val ui = LanguageCatalog.interfaceOption(settings.uiLanguage)
    var learningSheet by remember { mutableStateOf(false) }
    var nativeSheet by remember { mutableStateOf(false) }
    var uiSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Languages"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(tr("Choose what you learn and the language Tayra uses with you."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            PhoneLanguageCard(tr("Learning"), tr("I'm learning"), tr("Books, courses, vocabulary and flashcards."), highlighted = true) {
                PhoneLanguageRow(learning?.current?.label, tag = "learning-language") { learningSheet = true }
            }
            PhoneLanguageCard(tr("Meanings"), tr("Show meanings in"), tr("Translations, definitions and examples.")) {
                PhoneLanguageRow(native.name, tag = "native-language") { nativeSheet = true }
            }
            PhoneLanguageCard(tr("App"), tr("Interface language"), tr("Menus, buttons and messages.")) {
                PhoneLanguageRow(LanguageCatalog.nativeOption(ui.code).name, label = ui.name, tag = "interface-language") { uiSheet = true }
            }
            if (learning != null) MyLanguagesCard(learning, viewModel::askToRemove, Modifier.fillMaxWidth())
            InfoBanner(tr("Changes are saved automatically."))
        }
    }
    if (learningSheet && learning != null) {
        LanguagePickerSheet(
            tr("I'm learning"), learning.choices, selected = learning.current,
            name = { tr(it.label) }, flagName = { it.label },
            unavailable = { if (it.id in learning.nativeIds) tr("Your native language") else null },
            onDismiss = { learningSheet = false },
            // Chosen as in the header, which also asks for the vocabulary level of a language new to the reader.
            onPick = { learningSheet = false; learning.select(it) },
        )
    }
    if (nativeSheet) {
        LanguagePickerSheet(
            tr("Show meanings in"), LanguageCatalog.nativeLanguages, selected = native,
            name = { tr(it.name) }, flagName = { it.name },
            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
            onDismiss = { nativeSheet = false },
            onPick = { nativeSheet = false; viewModel.setNativeLanguage(it.code, learningCode) },
        )
    }
    if (uiSheet) {
        LanguagePickerSheet(
            tr("Interface language"), LanguageCatalog.interfaceLanguages, selected = ui,
            name = { it.name }, flagName = { LanguageCatalog.nativeOption(it.code).name },
            onDismiss = { uiSheet = false },
            onPick = { uiSheet = false; viewModel.setInterfaceLanguage(it.code) },
        )
    }
}

/** A small caps label, the title, the row, and what the choice is used for; the learning card is [highlighted]. */
@Composable
private fun PhoneLanguageCard(label: String, title: String, description: String, highlighted: Boolean = false, row: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (highlighted) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
            .border(AppBorders.hairline(if (highlighted) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .padding(AppSpacing.l),
    ) {
        Text(label.uppercase(), style = AppText.overline.copy(color = if (highlighted) colors.primary else colors.onSurfaceVariant))
        Spacer(Modifier.height(AppSpacing.xs))
        Text(title, style = AppText.sectionTitle)
        Spacer(Modifier.height(AppSpacing.l))
        row()
        Spacer(Modifier.height(AppSpacing.m))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** The chosen language with its flag and a chevron; [label] is what to show when the name shown is not the flag's. */
@Composable
private fun PhoneLanguageRow(flagName: String?, tag: String, label: String? = flagName?.let(::tr), onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.control).background(colors.surface).border(AppBorders.hairline(colors.outline.copy(alpha = AppAlpha.half)), AppShapes.control)
            .clickable(onClick = onClick).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flagName != null) {
            LanguageFlag(flagName, 30.dp)
            Spacer(Modifier.width(AppSpacing.l))
        }
        Text(label.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}
