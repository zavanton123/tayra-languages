package com.idiomiko.core.ui.components

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
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** What there is to read, the same on the wide bar's Read menu and the phone's Read sheet; [MoreEntry.label] is English. */
internal val readPages = listOf(
    MoreEntry("Courses", AppIcons.School, Route.Courses),
    MoreEntry("Books", AppIcons.MenuBook, Route.Books),
    MoreEntry("Find free books", AppIcons.Book, Route.FindBooks),
    MoreEntry("Book archive", AppIcons.Archive, Route.ArchivedBooks),
    MoreEntry("News", AppIcons.Newspaper, Route.News),
)

/** The areas whose screens belong to Read. */
internal val readSections = setOf(NavSection.COURSES, NavSection.BOOKS, NavSection.NEWS)

/**
 * The phone's bottom bar: the main areas as tabs, with the flashcards due on theirs, Read opening a sheet of
 * what there is to read, and More for everything else. [section] is the area of the current screen.
 */
@Composable
fun PhoneNavBar(section: NavSection?, onNavigate: (Route) -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val due = LocalFlashcardsDue.current
    var reading by remember { mutableStateOf(false) }
    // The videos, the playlists and the vocabulary pages are reached through More.
    val area = when (section) {
        in readSections -> NavSection.BOOKS
        NavSection.TERMS, NavSection.PLAYLISTS, NavSection.VIDEOS, NavSection.ACCOUNT -> NavSection.SETTINGS
        else -> section
    }
    NavigationBar(containerColor = colors.surface, modifier = Modifier.testTag("phone-nav")) {
        PhoneTab(NavSection.HOME, Icons.Default.Home, area) { onNavigate(Route.Home) }
        PhoneTab(NavSection.BOOKS, AppIcons.MenuBook, area, label = tr("Read"), tag = "READ") { reading = true }
        PhoneTab(NavSection.FLASHCARDS, AppIcons.Style, area, badge = due) { onNavigate(Route.Flashcards) }
        PhoneTab(NavSection.SETTINGS, AppIcons.MoreHoriz, area, label = tr("More"), onClick = onMore)
    }
    if (reading) ReadSheet(onNavigate = { reading = false; onNavigate(it) }, onDismiss = { reading = false })
}

/** The phone's Read sheet: the courses, the books and the news, as the wide bar's Read menu has them. */
@Composable
fun ReadSheet(onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("read-sheet")) {
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(start = AppSpacing.xl, end = AppSpacing.s, bottom = AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Read"), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            readPages.forEachIndexed { i, entry ->
                if (i > 0) AppDivider(Modifier.padding(end = AppSpacing.m))
                NavigationRow(tr(entry.label), onClick = { onNavigate(entry.route) }, modifier = Modifier.padding(end = AppSpacing.m).testTag("read-${entry.label}"), icon = entry.icon)
            }
        }
    }
}

@Composable
private fun RowScope.PhoneTab(
    tab: NavSection,
    icon: ImageVector,
    section: NavSection?,
    badge: Int = 0,
    label: String = tr(tab.label),
    tag: String = tab.name,
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
        label = { FitText(label) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primary.copy(alpha = AppAlpha.soft)),
        modifier = Modifier.testTag("phone-nav-$tag"),
    )
}

/** What a More row leads to; [label] is English, shown through `tr`. */
internal class MoreEntry(val label: String, val icon: ImageVector, val route: Route)

private class MoreGroup(val title: String, val entries: List<MoreEntry>)

/**
 * The More sheet's destinations, in groups: listening and watching, the words and how they go,
 * what is downloaded for use offline, and the preferences and data. The settings pages of a single
 * feature and clearing the data are found from Settings, which indexes them.
 */
private fun moreGroups(videos: Boolean) = listOf(
    // What there is to read is under Read.
    MoreGroup(
        "Listen and watch",
        listOfNotNull(
            MoreEntry("Playlists", AppIcons.QueueMusic, Route.Playlists),
            MoreEntry("Videos", AppIcons.Movie, Route.Videos).takeIf { videos },
        ),
    ),
    MoreGroup(
        "Words and progress",
        listOf(
            MoreEntry("Vocabulary", AppIcons.ViewList, Route.Terms()),
            // The Flashcards tab opens the review; the cards themselves are browsed from here.
            MoreEntry("Browse flashcards", AppIcons.Style, Route.BrowseFlashcards()),
            MoreEntry("Word frequency", AppIcons.BarChart, Route.WordFrequency),
            MoreEntry("Statistics", AppIcons.PieChart, Route.Stats),
        ),
    ),
    MoreGroup(
        "Offline resources",
        listOf(
            // Named apart from the tabs: these set things up rather than open them.
            MoreEntry("Downloads", AppIcons.Download, Route.Downloads),
            MoreEntry("Dictionaries", AppIcons.MenuBook, Route.OfflineDictionaries),
            MoreEntry("Speech", AppIcons.VolumeUp, Route.Speech),
            MoreEntry("Translation", AppIcons.Translate, Route.OfflineTranslation),
        ),
    ),
    MoreGroup("Account", listOf(MoreEntry("Account", Icons.Default.AccountCircle, Route.Account))),
    MoreGroup(
        "Preferences and data",
        listOfNotNull(
            MoreEntry("Settings", Icons.Default.Settings, Route.Settings),
            MoreEntry("Languages", AppIcons.Globe, Route.Languages),
            MoreEntry("Backups", AppIcons.UploadFile, Route.Backups),
            MoreEntry("About", Icons.Default.Info, Route.About),
            MoreEntry("Experimental features", AppIcons.Lightbulb, Route.ExperimentalFeatures),
        ),
    ),
)

/** The phone's More sheet: the pages beyond the tabs, grouped, with the learning language shown on its row. */
@Composable
fun MoreSheet(onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = LocalLearningLanguage.current?.currentName
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("more-sheet")) {
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(start = AppSpacing.xl, end = AppSpacing.s, bottom = AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("More"), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            moreGroups(LocalVideosEnabled.current).forEach { group ->
                Text(
                    tr(group.title).uppercase(),
                    Modifier.padding(top = AppSpacing.l, bottom = AppSpacing.xs),
                    style = AppText.overline.copy(color = colors.onSurfaceVariant),
                )
                group.entries.forEachIndexed { i, entry ->
                    if (i > 0) AppDivider(Modifier.padding(end = AppSpacing.m))
                    MoreRow(entry, value = language?.let(::tr).takeIf { entry.route == Route.Languages }) { onNavigate(entry.route) }
                }
            }
        }
    }
}

@Composable
private fun MoreRow(entry: MoreEntry, value: String?, onClick: () -> Unit) {
    NavigationRow(
        tr(entry.label),
        onClick = onClick,
        modifier = Modifier.padding(end = AppSpacing.m).testTag("more-${entry.label}"),
        value = value,
        icon = entry.icon,
    )
}
