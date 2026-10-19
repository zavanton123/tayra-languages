package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** The main navigation areas, used to highlight the active tab; [label] is English, shown through `tr`. */
enum class NavSection(val label: String) { HOME("Home"), COURSES("Courses"), BOOKS("Books"), NEWS("News"), FLASHCARDS("Flashcards"), TERMS("Vocabulary"), SETTINGS("Settings") }

/** How many flashcards wait today, shown beside the Flashcards tab; provided at the root of the app. */
val LocalFlashcardsDue = compositionLocalOf { 0 }

/** [label] is English, shown through `tr`. */
private data class MenuEntry(val label: String, val route: Route)

/** [direct] groups have one destination, opened by the tab itself with no menu. */
private data class MenuGroup(val section: NavSection, val entries: List<MenuEntry>, val direct: Boolean = false)

private val menuGroups = listOf(
    MenuGroup(NavSection.HOME, listOf(MenuEntry("Home", Route.Home)), direct = true),
    MenuGroup(NavSection.COURSES, listOf(MenuEntry("Courses", Route.Courses)), direct = true),
    MenuGroup(NavSection.BOOKS, listOf(MenuEntry("All books", Route.Books), MenuEntry("Create new book", Route.NewBook), MenuEntry("Find free books", Route.FindBooks), MenuEntry("Book archive", Route.ArchivedBooks))),
    MenuGroup(NavSection.NEWS, listOf(MenuEntry("News", Route.News)), direct = true),
    MenuGroup(NavSection.FLASHCARDS, listOf(MenuEntry("Review flashcards", Route.Flashcards), MenuEntry("Browse flashcards", Route.BrowseFlashcards()), MenuEntry("Flashcard settings", Route.FlashcardSettings))),
    MenuGroup(NavSection.TERMS, listOf(MenuEntry("Vocabulary", Route.Terms()), MenuEntry("Word frequency", Route.WordFrequency), MenuEntry("Statistics", Route.Stats))),
    MenuGroup(
        NavSection.SETTINGS,
        listOfNotNull(
            MenuEntry("Settings", Route.Settings),
            MenuEntry("Languages", Route.Languages),
            MenuEntry("Translation", Route.OfflineTranslation),
            MenuEntry("Dictionaries", Route.OfflineDictionaries),
            MenuEntry("Courses", Route.CoursePacks),
            MenuEntry("Books", Route.BookPacks),
            MenuEntry("Speech", Route.Speech),
            MenuEntry("Flashcards", Route.FlashcardSettings),
            MenuEntry("Vocabulary", Route.VocabularySettings),
            MenuEntry("Keyboard shortcuts", Route.Shortcuts),
            MenuEntry("Backups", Route.Backups),
            MenuEntry("Clear data", Route.ClearData),
            MenuEntry("About", Route.About),
        ),
    ),
)

const val APP_NAME = "Tayra Languages"

/**
 * The application bar with the choice of the language being learned and the main menu (Home, Courses,
 * Books, News, Flashcards, Vocabulary, Settings), shown on all screens except the reading pane. On narrow screens the menu collapses into one overflow menu.
 *
 * Wide windows always show the logo with the app's name, which leads to the home screen. [title] names the
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
        // The page's own heading on the page's own background: a large title, the screen's actions and the language.
        // The phone's menu is its bottom bar (see PhoneNavBar), so the bar has none.
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(PHONE_BAR_HEIGHT)
                .padding(start = if (onBack != null) AppSpacing.xs else AppSpacing.xl, end = AppSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack)
            Text(
                title,
                Modifier.weight(1f).padding(start = if (onBack != null) AppSpacing.xs else 0.dp, end = AppSpacing.s),
                style = if (onBack != null) AppText.headline else AppText.screenTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) { actions() }
            Spacer(Modifier.width(AppSpacing.xs))
            LearningLanguageSelector(compact = true, onOpen = { onNavigate(Route.ChooseLanguage) })
        }
        return
    }
    BarSurface {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                // 16 + the link's own 8 keeps the logo 24 from the edge.
                Modifier.fillMaxWidth().height(WIDE_BAR_HEIGHT).padding(start = AppSpacing.l, end = AppSpacing.xxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack)
                    Spacer(Modifier.width(AppSpacing.xs))
                }
                // The logo and name lead home, as on most sites.
                Row(
                    Modifier.clip(AppShapes.control)
                        .clickable(onClickLabel = tr("Go to Home"), role = Role.Button) { onNavigate(Route.Home) }
                        .padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Otter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconXLarge))
                    Spacer(Modifier.width(AppSpacing.m))
                    Text(APP_NAME, style = AppText.sectionTitle)
                }
                Spacer(Modifier.weight(1f))
                actions()
                // The language being learned sits just before the tabs it applies to.
                if (LocalLearningLanguage.current?.currentName != null) {
                    LearningLanguageSelector(compact = false)
                    Spacer(Modifier.width(AppSpacing.l))
                }
                if (showMenu) WideMenu(section, onNavigate)
            }
            AppDivider()
        }
    }
}

@Composable
private fun WideMenu(section: NavSection?, onNavigate: (Route) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        menuGroups.forEach { group ->
            var open by remember { mutableStateOf(false) }
            // The dropdown anchors to its enclosing composable, so each tab gets its own Box.
            Box {
                NavTab(group.section, active = section == group.section, onClick = { if (group.direct) onNavigate(group.entries.single().route) else open = true })
                AppMenu(expanded = open, onDismissRequest = { open = false }) {
                    group.entries.forEach { entry ->
                        AppMenuItem(text = { Text(tr(entry.label)) }, onClick = { open = false; onNavigate(entry.route) })
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
        Modifier.width(IntrinsicSize.Max).clip(AppShapes.small).clickable(onClick = onClick).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr(section.label), style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = color)
            val due = LocalFlashcardsDue.current
            if (section == NavSection.FLASHCARDS && due > 0) {
                CountBadge(
                    due,
                    Modifier.semantics { contentDescription = trPlural(due, "{0} flashcard due", "{0} flashcards due") },
                    emphasis = BadgeEmphasis.Solid,
                )
            }
        }
        Spacer(Modifier.height(AppSpacing.xs))
        Box(
            Modifier.fillMaxWidth().height(TAB_INDICATOR_HEIGHT).clip(AppShapes.tiny)
                .background(if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

private val PHONE_BAR_HEIGHT = 72.dp
private val WIDE_BAR_HEIGHT = 64.dp
private val TAB_INDICATOR_HEIGHT = 2.dp
