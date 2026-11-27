package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing

/** The settings pages a phone lists, in the wide window's sidebar order, less the hotkeys a phone has no use for. */
internal val phoneSettingsPages = listOf(
    MoreEntry("General", AppIcons.MoreGeneral, Route.Settings),
    MoreEntry("Languages", AppIcons.MoreLanguages, Route.Languages),
    MoreEntry("Goals", AppIcons.MoreGoals, Route.Goals),
    MoreEntry("Flashcards", AppIcons.Flashcards, Route.FlashcardSettings, tag = "Flashcard settings"),
    MoreEntry("Vocabulary", AppIcons.MoreVocabularySettings, Route.VocabularySettings, tag = "Vocabulary settings"),
    MoreEntry("Speech", AppIcons.MoreSpeech, Route.Speech),
    MoreEntry("Playlists", AppIcons.NavPlaylists, Route.PlaylistSettings, tag = "Playlist settings"),
    MoreEntry("Translation", AppIcons.MoreTranslation, Route.OfflineTranslation),
    MoreEntry("Backups", AppIcons.MoreBackups, Route.Backups),
    MoreEntry("Clear data", AppIcons.MoreClearData, Route.ClearData),
    MoreEntry("Updates", AppIcons.Download, Route.Updates),
    MoreEntry("Experiments", AppIcons.MoreExperiments, Route.ExperimentalFeatures),
)

/**
 * A phone's Settings, opened from the account menu: the settings pages as one card of rows, the language being learned
 * shown on its own. Pages the app hides, such as Updates where the app does not update itself, are left out.
 */
@Composable
fun SettingsMenuScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit) {
    val hidden = LocalHiddenSettingsSections.current.map { it.route }
    val language = LocalLearningLanguage.current?.currentName
    val pages = phoneSettingsPages.filter { it.route !in hidden }
    Scaffold(
        topBar = { AppTopBar(title = tr("Settings"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        ScrollColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        ) {
            Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).testTag("settings-menu")) {
                pages.forEachIndexed { i, entry ->
                    if (i > 0) AppDivider(Modifier.padding(horizontal = AppSpacing.l))
                    NavigationRow(
                        tr(entry.label),
                        onClick = { onNavigate(entry.route) },
                        modifier = Modifier.testTag("settings-menu-${entry.tag}"),
                        value = language?.let(::tr).takeIf { entry.route == Route.Languages },
                        icon = entry.icon,
                        edgePadding = AppSpacing.l,
                    )
                }
            }
        }
    }
}
