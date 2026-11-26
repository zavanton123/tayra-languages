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
    MoreEntry("Courses", AppIcons.ReadCourses, Route.Courses),
    MoreEntry("Books", AppIcons.ReadBooks, Route.Books),
    MoreEntry("Find free books", AppIcons.ReadFindBooks, Route.FindBooks),
    MoreEntry("Book archive", AppIcons.ReadArchive, Route.ArchivedBooks),
    MoreEntry("News", AppIcons.ReadNews, Route.News),
)

/** The vocabulary pages, the same on the wide bar's Vocabulary menu and the phone's Vocabulary sheet. */
internal val vocabularyPages = listOf(
    MoreEntry("Vocabulary", AppIcons.MoreVocabulary, Route.Terms()),
    MoreEntry("Word frequency", AppIcons.MoreWordFrequency, Route.WordFrequency),
    MoreEntry("Statistics", AppIcons.MoreStatistics, Route.Stats),
)

/** The flashcard pages, the same on the wide bar's Flashcards menu and the phone's Flashcards sheet. */
internal val flashcardPages = listOf(
    MoreEntry("Review flashcards", AppIcons.FlashcardsReview, Route.Flashcards()),
    MoreEntry("Browse flashcards", AppIcons.FlashcardsBrowse, Route.BrowseFlashcards()),
    MoreEntry("Flashcard statistics", AppIcons.FlashcardsStatistics, Route.FlashcardStats),
)

/** The areas whose screens belong to Read. */
internal val readSections = setOf(NavSection.COURSES, NavSection.BOOKS, NavSection.NEWS)

/**
 * The phone's bottom bar: the main areas as tabs (Home, Read, Playlists, Flashcards, Vocabulary), with the flashcards
 * due on theirs, and Read, Flashcards and Vocabulary opening sheets of their pages. The settings and the account are
 * reached from the top bar's avatar. [section] is the area of the current screen.
 */
@Composable
fun PhoneNavBar(section: NavSection?, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val due = LocalFlashcardsDue.current
    val videos = LocalVideosEnabled.current
    // The Read, Flashcards or Vocabulary tab whose sheet is open.
    var sheet by remember { mutableStateOf<NavSection?>(null) }
    // The videos are in the Read sheet; the settings and the account light no tab.
    val area = when (section) {
        in readSections, NavSection.VIDEOS -> NavSection.BOOKS
        NavSection.SETTINGS, NavSection.ACCOUNT -> null
        else -> section
    }
    NavigationBar(containerColor = colors.surface, modifier = Modifier.testTag("phone-nav")) {
        PhoneTab(NavSection.HOME, AppIcons.NavHome, area) { onNavigate(Route.Home) }
        PhoneTab(NavSection.BOOKS, AppIcons.NavRead, area, label = tr("Read"), tag = "READ") { sheet = NavSection.BOOKS }
        PhoneTab(NavSection.PLAYLISTS, AppIcons.NavPlaylists, area) { onNavigate(Route.Playlists) }
        PhoneTab(NavSection.FLASHCARDS, AppIcons.Flashcards, area, badge = due) { sheet = NavSection.FLASHCARDS }
        PhoneTab(NavSection.TERMS, AppIcons.MoreVocabulary, area) { sheet = NavSection.TERMS }
    }
    val go: (Route) -> Unit = { sheet = null; onNavigate(it) }
    when (sheet) {
        NavSection.BOOKS -> PagesSheet(tr("Read"), readPages + listOfNotNull(MoreEntry("Videos", AppIcons.Movie, Route.Videos).takeIf { videos }), "read", go) { sheet = null }
        // The cards due today, as on the tab, beside the review that takes them.
        NavSection.FLASHCARDS -> PagesSheet(tr("Flashcards"), flashcardPages, "flashcards", go, badges = mapOf(flashcardPages.first().route to due)) { sheet = null }
        NavSection.TERMS -> PagesSheet(tr("Vocabulary"), vocabularyPages, "vocabulary", go) { sheet = null }
        else -> {}
    }
}

/**
 * A tab's pages in a sheet, as the wide bar's menu of the same name has them. The sheet is tagged "[tag]-sheet"
 * and each row "[tag]-" and its English label.
 */
@Composable
private fun PagesSheet(title: String, pages: List<MoreEntry>, tag: String, onNavigate: (Route) -> Unit, badges: Map<Route, Int> = emptyMap(), onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("$tag-sheet")) {
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(bottom = AppSpacing.l)) {
            Row(Modifier.padding(start = AppSpacing.xl, end = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss)
            }
            pages.forEachIndexed { i, entry ->
                if (i > 0) AppDivider(Modifier.padding(horizontal = AppSpacing.xl))
                // Tall rows: few of them, each an easy target for the thumb, lit across the sheet's width when pressed.
                NavigationRow(
                    tr(entry.label),
                    onClick = { onNavigate(entry.route) },
                    modifier = Modifier.testTag("$tag-${entry.label}"),
                    icon = entry.icon,
                    verticalPadding = AppSpacing.xxl,
                    edgePadding = AppSpacing.xl,
                    badge = badges[entry.route] ?: 0,
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
