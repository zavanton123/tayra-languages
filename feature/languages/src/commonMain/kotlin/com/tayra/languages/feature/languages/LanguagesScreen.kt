package com.tayra.languages.feature.languages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SettingRow
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/** The language being learned, the native language translations are shown in, and the language of the interface. */
@Composable
fun LanguagesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: LanguagesViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val learning = LocalLearningLanguage.current
    val field = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth() else Modifier.width(250.dp)

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader("Languages", "Choose the language you learn and the languages Tayra uses with you.", onBackToSettings = onBack)
            ContentCard("Your languages", "Changes are saved automatically.", icon = AppIcons.Translate) {
                SettingRow("Learning language", "The language of your books, courses and vocabulary; also chosen in the header.", stackOnCompact = true) {
                    // Chosen as in the header, which also asks for the vocabulary level of a language new to the reader.
                    Dropdown(
                        options = learning?.languages.orEmpty(),
                        selected = learning?.languages?.firstOrNull { it.first == learning.currentId },
                        onSelect = { (id, _) -> learning?.onSelect?.invoke(id) },
                        label = null,
                        optionLabel = { it.second },
                        enabled = learning != null,
                        modifier = field,
                    )
                }
                SettingRow("Native language", "Translations, meanings and example sentences are shown in it.", divider = true, stackOnCompact = true) {
                    LanguageDropdown(LanguageCatalog.nativeLanguages, LanguageCatalog.nativeOption(settings.nativeLanguage), viewModel::setNativeLanguage, field)
                }
                SettingRow("Interface language", "The language of menus, buttons and messages.", divider = true, stackOnCompact = true) {
                    LanguageDropdown(LanguageCatalog.interfaceLanguages, LanguageCatalog.interfaceOption(settings.uiLanguage), viewModel::setInterfaceLanguage, field)
                }
            }
            InfoBanner("The interface is in English for now; the interface language you choose takes effect once Tayra is translated into it.")
        }
    }
}

@Composable
private fun LanguageDropdown(options: List<LanguageOption>, selected: LanguageOption, onSelect: (String) -> Unit, modifier: Modifier) {
    Dropdown(
        options = options,
        selected = selected,
        onSelect = { onSelect(it.code) },
        label = null,
        optionLabel = { it.name },
        modifier = modifier,
    )
}
