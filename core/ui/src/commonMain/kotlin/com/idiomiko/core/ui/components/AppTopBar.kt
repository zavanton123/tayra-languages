package com.idiomiko.core.ui.components

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.SubcomposeLayout
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.semantics.Role
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The main navigation areas, used to highlight the active tab; [label] is English, shown through `tr`. */
enum class NavSection(val label: String) { HOME("Home"), COURSES("Courses"), BOOKS("Books"), VIDEOS("Videos"), NEWS("News"), PLAYLISTS("Playlists"), FLASHCARDS("Flashcards"), TERMS("Vocabulary"), SETTINGS("Settings"), ACCOUNT("Account") }

/** How many flashcards wait today, shown beside the Flashcards tab; provided at the root of the app. */
val LocalFlashcardsDue = compositionLocalOf { 0 }

/** Whether the menus offer videos: an experimental feature, on unless turned off; provided at the root of the app. */
val LocalVideosEnabled = compositionLocalOf { false }

/** [label] is English, shown through `tr`. */
private data class MenuEntry(val label: String, val route: Route)

/**
 * [direct] groups have one destination, opened by the tab itself with no menu. [label] names the tab (English, shown
 * through `tr`), and [covers] are the areas whose screens light it up.
 */
private data class MenuGroup(
    val section: NavSection,
    val entries: List<MenuEntry>,
    val direct: Boolean = false,
    val label: String = section.label,
    val covers: Set<NavSection> = setOf(section),
)

private fun menuGroups(videos: Boolean) = listOfNotNull(
    MenuGroup(NavSection.HOME, listOf(MenuEntry("Home", Route.Home)), direct = true),
    // Everything to read in one menu: the courses, the books and the news.
    MenuGroup(NavSection.BOOKS, readPages.map { MenuEntry(it.label, it.route) }, label = "Read", covers = readSections),
    MenuGroup(NavSection.VIDEOS, listOf(MenuEntry("All videos", Route.Videos), MenuEntry("Add video", Route.NewVideo))).takeIf { videos },
    MenuGroup(NavSection.PLAYLISTS, listOf(MenuEntry("Playlists", Route.Playlists)), direct = true),
    // Their settings are with the others, beside the settings pages' sidebar.
    MenuGroup(NavSection.FLASHCARDS, flashcardPages.map { MenuEntry(it.label, it.route) }),
    MenuGroup(NavSection.TERMS, listOf(MenuEntry("Vocabulary", Route.Terms()), MenuEntry("Word frequency", Route.WordFrequency), MenuEntry("Statistics", Route.Stats))),
)

const val APP_NAME = "Idiomiko"

/** The name as the brand's mark, with a small ™: for About, where the trademark is noted. */
val BRAND_NAME: AnnotatedString = buildAnnotatedString {
    append(APP_NAME)
    withStyle(SpanStyle(fontSize = 0.75.em)) { append("™") }
}

/**
 * The application bar with the choice of the language being learned and the main menu (Home, Read with the
 * courses, books and news, Videos, Playlists, Flashcards, Vocabulary), shown on all screens except the reading pane.
 * On narrow screens the menu collapses into one overflow menu. The wide bar ends with the account menu, which leads
 * to the account, the settings, the backups and About: under the avatar when signed in, a person icon when not.
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
            // On every page, those with a way back too, and signed out too: the account and the settings are reached from here, as on the wide bar.
            AccountMenuButton(LocalAccountBadge.current, onNavigate)
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
                    Icon(AppIcons.Logo, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(AppSizes.iconXLarge))
                    Spacer(Modifier.width(AppSpacing.m))
                    Text(APP_NAME, style = AppText.sectionTitle)
                }
                Spacer(Modifier.weight(1f))
                actions()
                LocalSyncEntry.current?.let { sync ->
                    SyncStatusButton(sync)
                    Spacer(Modifier.width(AppSpacing.s))
                }
                // The language being learned sits just before the tabs it applies to.
                if (LocalLearningLanguage.current?.languages?.isNotEmpty() == true) {
                    LearningLanguageSelector(compact = false)
                    Spacer(Modifier.width(AppSpacing.l))
                }
                if (showMenu) {
                    val badge = LocalAccountBadge.current
                    WideMenu(section, onNavigate) {
                        Spacer(Modifier.width(AppSpacing.s))
                        AccountMenuButton(badge, onNavigate)
                    }
                }
            }
            AppDivider()
        }
    }
}

/**
 * The tabs of the wide bar, then [end], which always shows. Tabs that do not fit the window fold, from the last one
 * on, into a More tab before [end], whose menu lists their pages, so a narrower window still reaches them all.
 */
@Composable
private fun WideMenu(section: NavSection?, onNavigate: (Route) -> Unit, end: @Composable () -> Unit) {
    val groups = menuGroups(LocalVideosEnabled.current)
    SubcomposeLayout { constraints ->
        val free = Constraints()
        val last = subcompose(END) { Row(verticalAlignment = Alignment.CenterVertically) { end() } }.single().measure(free)
        val room = (constraints.maxWidth - last.width).coerceAtLeast(0)
        // Measured apart, hidden from screen readers: a tab that does not fit is never placed, and must not be read out either.
        fun width(slot: Any, tab: @Composable () -> Unit) =
            subcompose(slot) { Box(Modifier.clearAndSetSemantics {}) { tab() } }.single().measure(free).width
        val widths = groups.map { group -> width("measure-${group.section}") { GroupTab(group, section, onNavigate) } }
        var shown = groups.size
        if (widths.sum() > room) {
            val more = width(MORE_PROBE) { OverflowTab(emptyList(), section, onNavigate) }
            while (shown > 0 && widths.take(shown).sum() + more > room) shown--
        }
        val tabs = groups.take(shown).map { group -> subcompose(group.section) { GroupTab(group, section, onNavigate) }.single().measure(free) }
        val more = if (shown < groups.size) subcompose(MORE) { OverflowTab(groups.drop(shown), section, onNavigate) }.single().measure(free) else null
        val placed = tabs + listOfNotNull(more, last)
        val height = placed.maxOfOrNull { it.height } ?: 0
        layout(placed.sumOf { it.width }, height) {
            var x = 0
            placed.forEach { tab ->
                tab.placeRelative(x, (height - tab.height) / 2)
                x += tab.width
            }
        }
    }
}

/** A tab of the wide bar: its page, or the menu of its pages. */
@Composable
private fun GroupTab(group: MenuGroup, section: NavSection?, onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    // The dropdown anchors to its enclosing composable, so each tab gets its own Box.
    Box {
        val due = LocalFlashcardsDue.current
        NavTab(
            tr(group.label),
            active = section in group.covers,
            badge = due.takeIf { group.section == NavSection.FLASHCARDS && it > 0 },
            onClick = { if (group.direct) onNavigate(group.entries.single().route) else open = true },
        )
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            group.entries.forEach { entry ->
                AppMenuItem(text = { Text(tr(entry.label)) }, onClick = { open = false; onNavigate(entry.route) })
            }
        }
    }
}

/** The More tab of a narrow window: the pages of the tabs that did not fit, under their tabs' names. */
@Composable
private fun OverflowTab(hidden: List<MenuGroup>, section: NavSection?, onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        NavTab(tr("More"), active = hidden.any { section in it.covers }, badge = null, onClick = { open = true })
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            hidden.forEachIndexed { i, group ->
                if (i > 0) AppDivider()
                if (group.direct) {
                    AppMenuItem(text = { Text(tr(group.label)) }, onClick = { open = false; onNavigate(group.entries.single().route) })
                } else {
                    Text(
                        tr(group.label),
                        style = AppText.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.xs),
                    )
                    group.entries.forEach { entry ->
                        AppMenuItem(text = { Text(tr(entry.label)) }, onClick = { open = false; onNavigate(entry.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTab(label: String, active: Boolean, badge: Int?, onClick: () -> Unit) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.width(IntrinsicSize.Max).clip(AppShapes.small).clickable(onClick = onClick).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = color, maxLines = 1)
            if (badge != null) {
                CountBadge(
                    badge,
                    Modifier.semantics { contentDescription = trPlural(badge, "{0} flashcard due", "{0} flashcards due") },
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

/** The slots of the More tab: measured empty to see what room it needs, then with the tabs it holds. */
private const val MORE_PROBE = "more-probe"
private const val MORE = "more"

/** The slot of what ends the bar, after the tabs. */
private const val END = "end"

private val PHONE_BAR_HEIGHT = 72.dp
private val WIDE_BAR_HEIGHT = 64.dp
private val TAB_INDICATOR_HEIGHT = 2.dp
