package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.semantics.Role
import com.tayra.languages.core.ui.navigation.Route

/** The main navigation areas, used to highlight the active tab. */
enum class NavSection(val label: String) { HOME("Home"), BOOKS("Books"), TERMS("Vocabulary"), FLASHCARDS("Flashcards"), SETTINGS("Settings"), ABOUT("About") }

/** How many flashcards wait today, shown beside the Flashcards tab; provided at the root of the app. */
val LocalFlashcardsDue = compositionLocalOf { 0 }

private data class MenuEntry(val label: String, val route: Route)

private data class MenuGroup(val section: NavSection, val entries: List<MenuEntry>)

private val menuGroups = listOf(
    MenuGroup(NavSection.BOOKS, listOf(MenuEntry("All books", Route.Home), MenuEntry("Create new book", Route.NewBook), MenuEntry("Book archive", Route.ArchivedBooks))),
    MenuGroup(NavSection.TERMS, listOf(MenuEntry("Vocabulary", Route.Terms()))),
    MenuGroup(NavSection.FLASHCARDS, listOf(MenuEntry("Review flashcards", Route.Flashcards), MenuEntry("Flashcard settings", Route.FlashcardSettings))),
    MenuGroup(
        NavSection.SETTINGS,
        listOfNotNull(
            MenuEntry("Languages", Route.Languages),
            MenuEntry("Settings", Route.Settings),
            MenuEntry("Translation", Route.OfflineTranslation),
            MenuEntry("Dictionaries", Route.OfflineDictionaries),
            MenuEntry("Speech", Route.Speech),
            MenuEntry("Flashcards", Route.FlashcardSettings),
            MenuEntry("Keyboard shortcuts", Route.Shortcuts),
            MenuEntry("Backups", Route.Backups),
        ),
    ),
    MenuGroup(NavSection.ABOUT, listOf(MenuEntry("Statistics", Route.Stats), MenuEntry("About", Route.About))),
)

const val APP_NAME = "Tayra Languages"

/**
 * The application bar with the choice of the language being learned and the main menu (Books,
 * Vocabulary, Settings, About), shown on all screens except the reading pane. On narrow screens the menu collapses into one overflow menu.
 *
 * Wide windows always show the logo with the app's name, which leads home. [title] names the
 * screen only in the compact bar on phones, so screens show their own heading on wide windows
 * (see [ScreenTitle]).
 *
 * @param section the area the current screen belongs to; its tab is highlighted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)? = null,
    showMenu: Boolean = true,
    section: NavSection? = null,
    actions: @Composable () -> Unit = {},
) {
    val width = LocalWindowWidth.current
    if (width.isCompact) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            },
            actions = {
                actions()
                LearningLanguageSelector(compact = true)
                if (showMenu) CompactMenu(onNavigate)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                // 18 + the link's own 6 keeps the logo 24 from the edge.
                Modifier.fillMaxWidth().height(64.dp).padding(start = 18.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Spacer(Modifier.width(4.dp))
                }
                // The logo and name lead home, as on most sites.
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = "Go to Home", role = Role.Button) { onNavigate(Route.Home) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Otter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(APP_NAME, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                if (LocalLearningLanguage.current?.currentName != null) {
                    Spacer(Modifier.width(24.dp))
                    LearningLanguageSelector(compact = false)
                }
                Spacer(Modifier.weight(1f))
                actions()
                if (showMenu) WideMenu(section, onNavigate)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun WideMenu(section: NavSection?, onNavigate: (Route) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NavTab(NavSection.HOME, active = section == NavSection.HOME, onClick = { onNavigate(Route.Home) })
        menuGroups.forEach { group ->
            var open by remember { mutableStateOf(false) }
            // The dropdown anchors to its enclosing composable, so each tab gets its own Box.
            Box {
                NavTab(group.section, active = section == group.section, onClick = { open = true })
                AppMenu(expanded = open, onDismissRequest = { open = false }) {
                    group.entries.forEach { entry ->
                        AppMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTab(section: NavSection, active: Boolean, onClick: () -> Unit) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.width(IntrinsicSize.Max).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(section.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = color)
            val due = LocalFlashcardsDue.current
            if (section == NavSection.FLASHCARDS && due > 0) {
                Text(
                    if (due > 999) "999+" else "$due",
                    Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 7.dp, vertical = 1.dp)
                        .semantics { contentDescription = "$due flashcards due" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp))
                .background(if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun CompactMenu(onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text("Home") }, onClick = { open = false; onNavigate(Route.Home) })
            menuGroups.forEach { group ->
                HorizontalDivider()
                group.entries.filter { it.route != Route.Home }.forEach { entry ->
                    AppMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
                }
            }
        }
    }
}
