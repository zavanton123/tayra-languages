package com.idiomiko.feature.languages

import com.idiomiko.core.ui.components.ScrollColumn
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
import androidx.compose.material.icons.filled.Add
import com.idiomiko.core.ui.components.SecondaryButton
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.idiomiko.core.domain.language.LanguageCatalog
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LanguageFlag
import com.idiomiko.core.ui.components.LanguagePickerSheet
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

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
    var nativeSheet by remember { mutableStateOf(false) }
    var uiSheet by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Languages"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            if (learning != null) MyLanguagesCard(learning, viewModel::askToRemove, Modifier.fillMaxWidth(), viewModel.removals.collectAsStateWithLifecycle().value)
            // A phone adds a language on the full-screen list, as from the language menu.
            SecondaryButton(tr("Add a language"), onClick = { onNavigate(Route.ChooseLanguage) }, icon = Icons.Default.Add, modifier = Modifier.testTag("add-language-button"))
            PhoneLanguageCard(tr("Meanings"), tr("Show meanings in")) {
                PhoneLanguageRow(native.name, tag = "native-language") { nativeSheet = true }
            }
            PhoneLanguageCard(tr("App"), tr("Interface language")) {
                PhoneLanguageRow(LanguageCatalog.nativeOption(ui.code).name, label = ui.name, tag = "interface-language") { uiSheet = true }
            }
        }
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

/** A small caps label, the title and the row. */
@Composable
internal fun PhoneLanguageCard(label: String, title: String, row: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(colors.surface)
            .border(AppBorders.hairline(colors.outlineVariant), shape)
            .padding(AppSpacing.l),
    ) {
        Text(label.uppercase(), style = AppText.overline.copy(color = colors.onSurfaceVariant))
        Spacer(Modifier.height(AppSpacing.xs))
        Text(title, style = AppText.sectionTitle)
        Spacer(Modifier.height(AppSpacing.l))
        row()
    }
}

/** The chosen language with its flag and a chevron; [label] is what to show when the name shown is not the flag's. */
@Composable
internal fun PhoneLanguageRow(flagName: String?, tag: String, label: String? = flagName?.let(::tr), onClick: () -> Unit) {
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
