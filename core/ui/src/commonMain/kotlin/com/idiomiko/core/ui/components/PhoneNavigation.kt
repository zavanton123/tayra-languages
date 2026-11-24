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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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

/** The flashcard pages, the same on the wide bar's Flashcards menu and the phone's Flashcards sheet. */
internal val flashcardPages = listOf(
    MoreEntry("Review flashcards", AppIcons.Style, Route.Flashcards()),
    MoreEntry("Browse flashcards", AppIcons.ViewList, Route.BrowseFlashcards()),
    MoreEntry("Flashcard statistics", AppIcons.BarChart, Route.FlashcardStats),
)

/** The areas whose screens belong to Read. */
internal val readSections = setOf(NavSection.COURSES, NavSection.BOOKS, NavSection.NEWS)

/**
 * The phone's bottom bar: the main areas as tabs (Home, Read, Playlists, Flashcards), with the flashcards due on theirs,
 * Read and Flashcards opening sheets of their pages, and More for everything else. [section] is the area of the current screen.
 */
@Composable
fun PhoneNavBar(section: NavSection?, onNavigate: (Route) -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val due = LocalFlashcardsDue.current
    // The Read or the Flashcards tab whose sheet is open.
    var sheet by remember { mutableStateOf<NavSection?>(null) }
    // The videos and the vocabulary pages are reached through More.
    val area = when (section) {
        in readSections -> NavSection.BOOKS
        NavSection.TERMS, NavSection.VIDEOS, NavSection.ACCOUNT -> NavSection.SETTINGS
        else -> section
    }
    NavigationBar(containerColor = colors.surface, modifier = Modifier.testTag("phone-nav")) {
        PhoneTab(NavSection.HOME, AppIcons.NavHome, area) { onNavigate(Route.Home) }
        PhoneTab(NavSection.BOOKS, AppIcons.NavRead, area, label = tr("Read"), tag = "READ") { sheet = NavSection.BOOKS }
        PhoneTab(NavSection.PLAYLISTS, AppIcons.NavPlaylists, area) { onNavigate(Route.Playlists) }
        PhoneTab(NavSection.FLASHCARDS, AppIcons.NavFlashcards, area, badge = due) { sheet = NavSection.FLASHCARDS }
        PhoneTab(NavSection.SETTINGS, AppIcons.NavMore, area, label = tr("More"), onClick = onMore)
    }
    val go: (Route) -> Unit = { sheet = null; onNavigate(it) }
    when (sheet) {
        NavSection.BOOKS -> PagesSheet(tr("Read"), readPages, "read", go) { sheet = null }
        NavSection.FLASHCARDS -> PagesSheet(tr("Flashcards"), flashcardPages, "flashcards", go) { sheet = null }
        else -> {}
    }
}

/**
 * A tab's pages in a sheet, as the wide bar's menu of the same name has them. The sheet is tagged "[tag]-sheet"
 * and each row "[tag]-" and its English label.
 */
@Composable
private fun PagesSheet(title: String, pages: List<MoreEntry>, tag: String, onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("$tag-sheet")) {
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(bottom = AppSpacing.l)) {
            Row(Modifier.padding(start = AppSpacing.xl, end = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            pages.forEachIndexed { i, entry ->
                if (i > 0) AppDivider(Modifier.padding(horizontal = AppSpacing.xl))
                // Taller rows than More's: few of them, each an easy target for the thumb, lit across the sheet's width when pressed.
                NavigationRow(
                    tr(entry.label),
                    onClick = { onNavigate(entry.route) },
                    modifier = Modifier.testTag("$tag-${entry.label}"),
                    icon = entry.icon,
                    verticalPadding = AppSpacing.xxl,
                    edgePadding = AppSpacing.xl,
                )
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

/** What a More row leads to; [label] is English, shown through `tr`, and [tag] tells apart rows of the same name. */
internal class MoreEntry(val label: String, val icon: ImageVector, val route: Route, val tag: String = label)

private class MoreGroup(val title: String, val entries: List<MoreEntry>)

/**
 * The More sheet's destinations, in groups: the videos when they are on, the words and how they go,
 * what is downloaded for use offline, and the settings pages the wide window's sidebar lists, less
 * the hotkeys and the updates a phone has no use for, with About. The account is the bar's avatar.
 */
private fun moreGroups(videos: Boolean) = listOfNotNull(
    // What there is to read is under Read, the playlists have a tab.
    MoreGroup("Watch", listOf(MoreEntry("Videos", AppIcons.Movie, Route.Videos))).takeIf { videos },
    MoreGroup(
        "Words and progress",
        listOf(
            MoreEntry("Vocabulary", AppIcons.ViewList, Route.Terms()),
            MoreEntry("Word frequency", AppIcons.BarChart, Route.WordFrequency),
            MoreEntry("Statistics", AppIcons.PieChart, Route.Stats),
        ),
    ),
    MoreGroup(
        "Offline resources",
        listOf(
            // Named apart from the tabs: these set things up rather than open them.
            MoreEntry("Speech", AppIcons.VolumeUp, Route.Speech),
            MoreEntry("Translation", AppIcons.Translate, Route.OfflineTranslation),
        ),
    ),
    MoreGroup(
        "Settings",
        listOf(
            MoreEntry("General", Icons.Default.Settings, Route.Settings),
            MoreEntry("Goals", AppIcons.Target, Route.Goals),
            MoreEntry("Languages", AppIcons.Globe, Route.Languages),
            MoreEntry("Flashcards", AppIcons.Style, Route.FlashcardSettings, tag = "Flashcard settings"),
            MoreEntry("Vocabulary", AppIcons.Abc, Route.VocabularySettings, tag = "Vocabulary settings"),
            MoreEntry("Backups", AppIcons.UploadFile, Route.Backups),
            MoreEntry("Clear data", AppIcons.DeleteOutline, Route.ClearData),
            MoreEntry("About", Icons.Default.Info, Route.About),
            MoreEntry("Experiments", AppIcons.Lightbulb, Route.ExperimentalFeatures),
        ),
    ),
)

/** The phone's More sheet: the pages beyond the tabs, grouped, with the learning language shown on its row. */
@Composable
fun MoreSheet(onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val language = LocalLearningLanguage.current?.currentName
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("more-sheet")) {
        // The rows run to the sheet's edges, so a pressed one is lit across its width; the rest keeps the margins.
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(bottom = AppSpacing.l)) {
            Row(Modifier.padding(start = AppSpacing.xl, end = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("More"), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            moreGroups(LocalVideosEnabled.current).forEach { group ->
                Text(
                    tr(group.title).uppercase(),
                    Modifier.padding(start = AppSpacing.xl, end = AppSpacing.xl, top = AppSpacing.l, bottom = AppSpacing.xs),
                    style = AppText.overline.copy(color = colors.onSurfaceVariant),
                )
                group.entries.forEachIndexed { i, entry ->
                    if (i > 0) AppDivider(Modifier.padding(horizontal = AppSpacing.xl))
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
        modifier = Modifier.testTag("more-${entry.tag}"),
        value = value,
        icon = entry.icon,
        edgePadding = AppSpacing.xl,
    )
}
