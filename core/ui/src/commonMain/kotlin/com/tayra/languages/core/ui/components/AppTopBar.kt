package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tayra.languages.core.ui.navigation.Route

private data class MenuEntry(val label: String, val route: Route)

private data class MenuGroup(val label: String, val entries: List<MenuEntry>)

private val menuGroups = listOf(
    MenuGroup("Books", listOf(MenuEntry("Create new book", Route.NewBook()), MenuEntry("Book archive", Route.ArchivedBooks))),
    MenuGroup("Terms", listOf(MenuEntry("Terms", Route.Terms()), MenuEntry("Import terms", Route.ImportTerms), MenuEntry("Term tags", Route.TermTags))),
    MenuGroup("Settings", listOf(MenuEntry("Languages", Route.Languages), MenuEntry("Settings", Route.Settings), MenuEntry("Keyboard shortcuts", Route.Shortcuts))),
    MenuGroup("About", listOf(MenuEntry("Statistics", Route.Stats), MenuEntry("About", Route.About))),
)

/**
 * The application bar with the main menu (Books, Terms, Settings, About), shown on all
 * screens except the reading pane. On narrow screens the menu collapses into one overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)? = null,
    showMenu: Boolean = true,
    actions: @Composable () -> Unit = {},
) {
    val width = LocalWindowWidth.current
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        },
        actions = {
            actions()
            if (showMenu) {
                if (width.isCompact) CompactMenu(onNavigate) else WideMenu(onNavigate)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun WideMenu(onNavigate: (Route) -> Unit) {
    Row {
        TextButton(onClick = { onNavigate(Route.Home) }) { Text("Home") }
        menuGroups.forEach { group ->
            var open by remember { mutableStateOf(false) }
            TextButton(onClick = { open = true }) { Text(group.label) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                group.entries.forEach { entry ->
                    DropdownMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
                }
            }
        }
    }
}

@Composable
private fun CompactMenu(onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(text = { Text("Home") }, onClick = { open = false; onNavigate(Route.Home) })
        menuGroups.forEach { group ->
            HorizontalDivider()
            group.entries.forEach { entry ->
                DropdownMenuItem(text = { Text(entry.label) }, onClick = { open = false; onNavigate(entry.route) })
            }
        }
    }
}
