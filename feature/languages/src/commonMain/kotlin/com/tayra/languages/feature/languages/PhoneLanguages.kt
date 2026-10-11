package com.tayra.languages.feature.languages

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LanguageMenuRow
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route

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
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Choose what you learn and the language Tayra uses with you."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            PhoneLanguageCard(tr("Learning"), tr("I'm learning"), tr("Books, courses, vocabulary and flashcards."), highlighted = true) {
                PhoneLanguageRow(learning?.currentName, tag = "learning-language") { learningSheet = true }
            }
            PhoneLanguageCard(tr("Meanings"), tr("Show meanings in"), tr("Translations, definitions and examples.")) {
                PhoneLanguageRow(native.name, tag = "native-language") { nativeSheet = true }
            }
            PhoneLanguageCard(tr("App"), tr("Interface language"), tr("Menus, buttons and messages.")) {
                PhoneLanguageRow(LanguageCatalog.nativeOption(ui.code).name, label = ui.name, tag = "interface-language") { uiSheet = true }
            }
            InfoBanner(tr("Changes are saved automatically."))
        }
    }
    if (learningSheet && learning != null) {
        LanguageSheet(
            tr("I'm learning"), learning.languages, selected = learning.languages.firstOrNull { it.first == learning.currentId },
            name = { tr(it.second) }, flagName = { it.second },
            unavailable = { if (it.first in learning.nativeIds) tr("Your native language") else null },
            onDismiss = { learningSheet = false },
            // Chosen as in the header, which also asks for the vocabulary level of a language new to the reader.
            onPick = { (id, _) -> learningSheet = false; learning.onSelect(id) },
        )
    }
    if (nativeSheet) {
        LanguageSheet(
            tr("Show meanings in"), LanguageCatalog.nativeLanguages, selected = native,
            name = { tr(it.name) }, flagName = { it.name },
            unavailable = { if (it.code == learningCode) tr("You are learning it") else null },
            onDismiss = { nativeSheet = false },
            onPick = { nativeSheet = false; viewModel.setNativeLanguage(it.code, learningCode) },
        )
    }
    if (uiSheet) {
        LanguageSheet(
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
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (highlighted) colors.primary.copy(alpha = 0.07f) else colors.surface)
            .border(1.dp, if (highlighted) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, shape)
            .padding(16.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = if (highlighted) colors.primary else colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        row()
        Spacer(Modifier.height(12.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** The chosen language with its flag and a chevron; [label] is what to show when the name shown is not the flag's. */
@Composable
private fun PhoneLanguageRow(flagName: String?, tag: String, label: String? = flagName?.let(::tr), onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.surface).border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flagName != null) {
            LanguageFlag(flagName, 30.dp)
            Spacer(Modifier.width(14.dp))
        }
        Text(label.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}

/** A bottom sheet of the [options], each with its flag, the chosen one ticked and the ones that cannot be chosen dimmed with why. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LanguageSheet(
    title: String,
    options: List<T>,
    selected: T?,
    name: (T) -> String,
    flagName: (T) -> String,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
    unavailable: (T) -> String? = { null },
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetMaxWidth = Dp.Unspecified, containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp).padding(bottom = 16.dp)) {
            Text(title, Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            options.forEach { option ->
                val chosen = option == selected
                val note = if (chosen) null else unavailable(option)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = note == null) { if (!chosen) onPick(option) else onDismiss() }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    LanguageMenuRow(flagName(option), name(option), chosen, note)
                }
            }
        }
    }
}
