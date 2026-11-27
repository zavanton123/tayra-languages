package com.idiomiko.feature.languages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.language.LanguageCatalog
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.language.LanguageOption
import com.idiomiko.core.ui.components.AppDialogSurface
import com.idiomiko.core.ui.components.AppScreenDialog
import com.idiomiko.core.ui.components.LanguagePickerSheet
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Asks, once the first language to learn is chosen, which language meanings are shown in and which
 * the app speaks. Both start at the device's language when the app has it; Continue saves them.
 */
@Composable
fun FirstRunLanguagesDialog(onDone: () -> Unit, viewModel: LanguagesViewModel = koinViewModel(key = "first-run-languages")) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val learningCode = LocalLearningLanguage.current?.currentName?.let(LanguageCodes::codeFor)
    val device = Locale.current.language.lowercase()
    var native by remember {
        mutableStateOf(listOf(device, settings.nativeLanguage).firstNotNullOfOrNull { code ->
            LanguageCatalog.nativeLanguages.firstOrNull { it.code == code && it.code != learningCode }
        })
    }
    var ui by remember { mutableStateOf(LanguageCatalog.interfaceLanguages.firstOrNull { it.code == device } ?: LanguageCatalog.interfaceOption(settings.uiLanguage)) }
    val save = { native?.let { viewModel.setLanguages(it.code, ui.code, learningCode) }; onDone() }
    val unavailable: (LanguageOption) -> String? = { if (it.code == learningCode) tr("You are learning it") else null }
    if (LocalWindowWidth.current.isCompact) {
        PhoneFirstRunLanguages(native, ui, unavailable, onNative = { native = it }, onUi = { ui = it }, onContinue = save)
        return
    }
    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.testTag("first-run-languages"), maxWidth = AppSizes.dialogWide, dismissible = false) {
        Column(Modifier.padding(AppSpacing.xxxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            FirstRunHeading()
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                LanguageTile(tr("Meanings"), tr("Show meanings in"), Modifier.weight(1f).fillMaxHeight()) {
                    FlagPicker(
                        options = LanguageCatalog.nativeLanguages, selected = native, name = { tr(it.name) }, onSelect = { native = it },
                        modifier = Modifier.testTag("native-language"), flagName = { it.name }, unavailable = unavailable,
                    )
                }
                LanguageTile(tr("App"), tr("Interface language"), Modifier.weight(1f).fillMaxHeight()) {
                    FlagPicker(
                        options = LanguageCatalog.interfaceLanguages, selected = ui, name = { it.name }, onSelect = { ui = it },
                        modifier = Modifier.testTag("interface-language"), flagName = { LanguageCatalog.nativeOption(it.code).name },
                    )
                }
            }
            PrimaryButton(tr("Continue"), onClick = save, modifier = Modifier.align(Alignment.End).testTag("first-run-continue"), enabled = native != null)
        }
    }
}

/** The question on a phone, on the whole screen: the two cards, each opening a sheet of languages, and Continue at the foot. */
@Composable
private fun PhoneFirstRunLanguages(
    native: LanguageOption?,
    ui: LanguageOption,
    unavailable: (LanguageOption) -> String?,
    onNative: (LanguageOption) -> Unit,
    onUi: (LanguageOption) -> Unit,
    onContinue: () -> Unit,
) {
    var nativeSheet by remember { mutableStateOf(false) }
    var uiSheet by remember { mutableStateOf(false) }
    AppScreenDialog(onDismissRequest = {}, color = MaterialTheme.colorScheme.surfaceContainerLow, dismissible = false) {
        Column(Modifier.fillMaxSize().padding(AppSpacing.l).testTag("first-run-languages"), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            FirstRunHeading()
            PhoneLanguageCard(tr("Meanings"), tr("Show meanings in")) {
                PhoneLanguageRow(native?.name, tag = "native-language") { nativeSheet = true }
            }
            PhoneLanguageCard(tr("App"), tr("Interface language")) {
                PhoneLanguageRow(LanguageCatalog.nativeOption(ui.code).name, label = ui.name, tag = "interface-language") { uiSheet = true }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("first-run-continue"), enabled = native != null)
        }
    }
    if (nativeSheet) {
        LanguagePickerSheet(
            tr("Show meanings in"), LanguageCatalog.nativeLanguages, selected = native,
            name = { tr(it.name) }, flagName = { it.name }, unavailable = unavailable,
            onDismiss = { nativeSheet = false },
            onPick = { nativeSheet = false; onNative(it) },
        )
    }
    if (uiSheet) {
        LanguagePickerSheet(
            tr("Interface language"), LanguageCatalog.interfaceLanguages, selected = ui,
            name = { it.name }, flagName = { LanguageCatalog.nativeOption(it.code).name },
            onDismiss = { uiSheet = false },
            onPick = { uiSheet = false; onUi(it) },
        )
    }
}

@Composable
private fun FirstRunHeading() {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(tr("Your languages"), style = AppText.screenTitle)
        Text(tr("You can change them later in Settings."), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
