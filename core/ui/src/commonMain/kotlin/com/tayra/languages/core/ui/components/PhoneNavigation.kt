package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppText

/**
 * The phone's bottom bar: the main areas as tabs, with the flashcards due on theirs, and More for
 * everything else. [section] is the area of the current screen; More holds the news, vocabulary and settings areas.
 */
@Composable
fun PhoneNavBar(section: NavSection?, onNavigate: (Route) -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val due = LocalFlashcardsDue.current
    // The news and the vocabulary pages are reached through More.
    val area = if (section == NavSection.TERMS || section == NavSection.NEWS) NavSection.SETTINGS else section
    NavigationBar(containerColor = colors.surface, modifier = Modifier.testTag("phone-nav")) {
        PhoneTab(NavSection.HOME, Icons.Default.Home, area) { onNavigate(Route.Home) }
        PhoneTab(NavSection.COURSES, AppIcons.School, area) { onNavigate(Route.Courses) }
        PhoneTab(NavSection.BOOKS, AppIcons.MenuBook, area) { onNavigate(Route.Books) }
        PhoneTab(NavSection.FLASHCARDS, AppIcons.Style, area, badge = due) { onNavigate(Route.Flashcards) }
        PhoneTab(NavSection.SETTINGS, AppIcons.MoreHoriz, area, label = tr("More"), onClick = onMore)
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
            Box {
                Icon(icon, contentDescription = null)
                if (badge > 0) CountBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-6).dp), emphasis = BadgeEmphasis.Solid, max = 99)
            }
        },
        label = { Text(label, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primary.copy(alpha = AppAlpha.soft)),
        modifier = Modifier.testTag("phone-nav-${tab.name}"),
    )
}

/** What a More row leads to; [label] is English, shown through `tr`. */
private class MoreEntry(val label: String, val icon: ImageVector, val route: Route, val destructive: Boolean = false)

private class MoreGroup(val title: String, val entries: List<MoreEntry>)

private val moreGroups = listOf(
    MoreGroup("Reading", listOf(MoreEntry("News", AppIcons.Newspaper, Route.News), MoreEntry("Find books", AppIcons.Book, Route.FindBooks))),
    MoreGroup(
        "Words",
        listOf(
            MoreEntry("Vocabulary", AppIcons.ViewList, Route.Terms()),
            MoreEntry("Word frequency", AppIcons.BarChart, Route.WordFrequency),
            MoreEntry("Statistics", AppIcons.PieChart, Route.Stats),
        ),
    ),
    MoreGroup(
        "Preferences",
        listOf(
            MoreEntry("Settings", Icons.Default.Settings, Route.Settings),
            MoreEntry("Languages", AppIcons.Globe, Route.Languages),
            MoreEntry("Translation", AppIcons.Translate, Route.OfflineTranslation),
            MoreEntry("Dictionaries", AppIcons.Book, Route.OfflineDictionaries),
            MoreEntry("Courses", AppIcons.MenuBook, Route.CoursePacks),
            MoreEntry("Books", AppIcons.BookClosed, Route.BookPacks),
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
@Composable
fun MoreSheet(onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = LocalLearningLanguage.current?.currentName
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("more-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("More"), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            moreGroups.forEach { group ->
                Text(
                    tr(group.title).uppercase(),
                    Modifier.padding(top = 16.dp, bottom = 4.dp),
                    style = AppText.overline.copy(color = colors.onSurfaceVariant),
                )
                group.entries.forEachIndexed { i, entry ->
                    if (i > 0) AppDivider(Modifier.padding(end = 12.dp))
                    MoreRow(entry, value = language?.let(::tr).takeIf { entry.route == Route.Languages }) { onNavigate(entry.route) }
                }
            }
            AppDivider(Modifier.padding(top = 12.dp, end = 12.dp))
            MoreRow(MoreEntry("About", Icons.Default.Info, Route.About), value = null) { onNavigate(Route.About) }
        }
    }
}

@Composable
private fun MoreRow(entry: MoreEntry, value: String?, onClick: () -> Unit) {
    NavigationRow(
        tr(entry.label),
        onClick = onClick,
        modifier = Modifier.padding(end = 12.dp).testTag("more-${entry.label}"),
        value = value,
        icon = entry.icon,
        tint = if (entry.destructive) MaterialTheme.colorScheme.error else null,
    )
}
