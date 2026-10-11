package com.tayra.languages.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route

/**
 * The phone's bottom bar: the main areas as tabs, with the flashcards due on theirs, and More for
 * everything else. [section] is the area of the current screen; More counts as the settings area.
 */
@Composable
fun PhoneNavBar(section: NavSection?, onNavigate: (Route) -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val due = LocalFlashcardsDue.current
    NavigationBar(containerColor = colors.surface, modifier = Modifier.testTag("phone-nav")) {
        PhoneTab(NavSection.COURSES, AppIcons.School, section) { onNavigate(Route.Courses) }
        PhoneTab(NavSection.BOOKS, AppIcons.MenuBook, section) { onNavigate(Route.Books) }
        PhoneTab(NavSection.FLASHCARDS, AppIcons.Style, section, badge = due) { onNavigate(Route.Flashcards) }
        PhoneTab(NavSection.TERMS, AppIcons.ViewList, section) { onNavigate(Route.Terms()) }
        PhoneTab(NavSection.SETTINGS, AppIcons.MoreHoriz, section, label = tr("More"), onClick = onMore)
    }
}

@Composable
private fun RowScope.PhoneTab(
    tab: NavSection,
    icon: ImageVector,
    section: NavSection?,
    badge: Int = 0,
    label: String = tr(tab.label),
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    NavigationBarItem(
        selected = section == tab,
        onClick = onClick,
        icon = {
            if (badge > 0) {
                BadgedBox(badge = { Badge { Text(if (badge > 99) "99+" else "$badge") } }) { Icon(icon, contentDescription = null) }
            } else {
                Icon(icon, contentDescription = null)
            }
        },
        label = { Text(label, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primary.copy(alpha = 0.12f)),
        modifier = Modifier.testTag("phone-nav-${tab.name}"),
    )
}

/** What a More row leads to; [label] is English, shown through `tr`. */
private class MoreEntry(val label: String, val icon: ImageVector, val route: Route, val destructive: Boolean = false)

private class MoreGroup(val title: String, val entries: List<MoreEntry>)

private val moreGroups = listOf(
    MoreGroup("Insights", listOf(MoreEntry("Word frequency", AppIcons.BarChart, Route.WordFrequency), MoreEntry("Statistics", AppIcons.PieChart, Route.Stats))),
    MoreGroup(
        "Preferences",
        listOf(
            MoreEntry("Settings", Icons.Default.Settings, Route.Settings),
            MoreEntry("Languages", AppIcons.Globe, Route.Languages),
            MoreEntry("Translation", AppIcons.Translate, Route.OfflineTranslation),
            MoreEntry("Dictionaries", AppIcons.Book, Route.OfflineDictionaries),
            MoreEntry("Courses", AppIcons.MenuBook, Route.CoursePacks),
            MoreEntry("Speech", AppIcons.VolumeUp, Route.Speech),
            MoreEntry("Flashcards", AppIcons.Style, Route.FlashcardSettings),
            MoreEntry("Vocabulary", AppIcons.Abc, Route.VocabularySettings),
        ),
    ),
    MoreGroup(
        "Data",
        listOf(
            MoreEntry("Book archive", AppIcons.BookClosed, Route.ArchivedBooks),
            MoreEntry("Backups", AppIcons.UploadFile, Route.Backups),
            MoreEntry("Clear data", Icons.Default.Delete, Route.ClearData, destructive = true),
        ),
    ),
)

/** The phone's More sheet: the pages beyond the tabs, grouped, with the learning language shown on its row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = LocalLearningLanguage.current?.currentName
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = Dp.Unspecified,
        containerColor = colors.surface,
        modifier = Modifier.testTag("more-sheet"),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("More"), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
            }
            moreGroups.forEach { group ->
                Text(
                    tr(group.title).uppercase(),
                    Modifier.padding(top = 16.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 2,
                )
                group.entries.forEachIndexed { i, entry ->
                    if (i > 0) HorizontalDivider(Modifier.padding(end = 12.dp), color = colors.outlineVariant)
                    MoreRow(entry, value = language?.let(::tr).takeIf { entry.route == Route.Languages }) { onNavigate(entry.route) }
                }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp, end = 12.dp), color = colors.outlineVariant)
            MoreRow(MoreEntry("About", Icons.Default.Info, Route.About), value = null) { onNavigate(Route.About) }
        }
    }
}

@Composable
private fun MoreRow(entry: MoreEntry, value: String?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (entry.destructive) colors.error else colors.primary
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp).padding(end = 12.dp).testTag("more-${entry.label}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(entry.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(tr(entry.label), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (entry.destructive) colors.error else colors.onSurface)
        if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
    }
}
