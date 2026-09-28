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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.tayra.languages.core.ui.navigation.Route

/** The main navigation areas, used to highlight the active tab. */
enum class NavSection(val label: String) { HOME("Home"), BOOKS("Books"), TERMS("Terms"), SETTINGS("Settings"), ABOUT("About") }

private data class MenuEntry(val label: String, val route: Route)

private data class MenuGroup(val section: NavSection, val entries: List<MenuEntry>)

private val menuGroups = listOf(
    MenuGroup(NavSection.BOOKS, listOf(MenuEntry("All books", Route.Home), MenuEntry("Create new book", Route.NewBook()), MenuEntry("Book archive", Route.ArchivedBooks))),
    MenuGroup(NavSection.TERMS, listOf(MenuEntry("Terms", Route.Terms()), MenuEntry("Import terms", Route.ImportTerms))),
    MenuGroup(NavSection.SETTINGS, listOf(MenuEntry("Languages", Route.Languages), MenuEntry("Settings", Route.Settings), MenuEntry("Keyboard shortcuts", Route.Shortcuts))),
    MenuGroup(NavSection.ABOUT, listOf(MenuEntry("Statistics", Route.Stats), MenuEntry("About", Route.About))),
)

/**
 * The application bar with the main menu (Books, Terms, Settings, About), shown on all
 * screens except the reading pane. On narrow screens the menu collapses into one overflow menu.
 *
 * @param section the area the current screen belongs to; its tab is highlighted.
 * @param centerContent an optional widget shown in the middle of the wide bar, such as a search box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)? = null,
    showMenu: Boolean = true,
    section: NavSection? = null,
    centerContent: (@Composable () -> Unit)? = null,
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
                if (showMenu) CompactMenu(onNavigate)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    Spacer(Modifier.width(4.dp))
                }
                Icon(AppIcons.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (centerContent != null) {
                    Box(Modifier.weight(2f).widthIn(max = 600.dp)) { centerContent() }
                    Spacer(Modifier.weight(1f))
                }
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
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    group.entries.forEach { entry ->
                        DropdownMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
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
        Text(section.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = color)
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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Home") }, onClick = { open = false; onNavigate(Route.Home) })
            menuGroups.forEach { group ->
                HorizontalDivider()
                group.entries.filter { it.route != Route.Home }.forEach { entry ->
                    DropdownMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
                }
            }
        }
    }
}
