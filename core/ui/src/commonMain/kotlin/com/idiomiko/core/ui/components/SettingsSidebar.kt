package com.idiomiko.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** The settings pages, in the order the sidebar lists them; [label] is English, shown through `tr`. */
enum class SettingsSection(val label: String, val route: Route, val icon: ImageVector) {
    ACCOUNT("Account", Route.Account, Icons.Default.AccountCircle),
    GENERAL("General", Route.Settings, Icons.Default.Settings),
    LANGUAGES("Languages", Route.Languages, AppIcons.Globe),
    FLASHCARDS("Flashcards", Route.FlashcardSettings, AppIcons.Style),
    VOCABULARY("Vocabulary", Route.VocabularySettings, AppIcons.Abc),
    HOTKEYS("Hotkeys", Route.Shortcuts, AppIcons.Keyboard),
    SPEECH("Speech", Route.Speech, AppIcons.VolumeUp),
    TRANSLATION("Translation", Route.OfflineTranslation, AppIcons.Translate),
    BACKUPS("Backups", Route.Backups, AppIcons.UploadFile),
    ABOUT("About", Route.About, Icons.Default.Info),
    EXPERIMENTS("Experiments", Route.ExperimentalFeatures, AppIcons.Lightbulb),
}

/** Whether the page sits beside the settings sidebar, which then stands in for [ScreenHeader]'s way back to Settings. */
val LocalSettingsSidebar = compositionLocalOf { false }

/**
 * Opens another settings page in place of the one shown, so going from section to section piles nothing up to go
 * back through; provided at the root of the app. Without it the sidebar opens the page as any link would.
 */
val LocalSettingsSwitch = compositionLocalOf<((Route) -> Unit)?> { null }

/**
 * A settings page's body below the bar: on a wide window, beside the sidebar of every settings page with [current]
 * lit, and otherwise alone. [content] takes the padding to apply. Beside the sidebar it is told the width it has
 * rather than the window's, so a page with columns lays them out for the room left.
 */
@Composable
fun SettingsPane(current: SettingsSection, padding: PaddingValues, onNavigate: (Route) -> Unit, content: @Composable (PaddingValues) -> Unit) {
    if (!LocalWindowWidth.current.isExpanded) return content(padding)
    val open = LocalSettingsSwitch.current ?: onNavigate
    Row(Modifier.padding(padding).fillMaxSize()) {
        SettingsSidebar(current) { if (it != current) open(it.route) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            // Never narrower than a medium window: the sidebar shows only where the page keeps its wide layout.
            val width = WindowWidth.of(maxWidth).takeIf { !it.isCompact } ?: WindowWidth.MEDIUM
            CompositionLocalProvider(LocalWindowWidth provides width, LocalSettingsSidebar provides true) {
                content(PaddingValues(0.dp))
            }
        }
    }
}

/** The settings pages as a card down the left, [current] lit as the Playlists sidebar lights its playlist. */
@Composable
fun SettingsSidebar(current: SettingsSection, onSelect: (SettingsSection) -> Unit) {
    Box(
        Modifier.width(SIDEBAR + AppSpacing.l + AppSpacing.l).fillMaxHeight()
            .padding(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.xxl, bottom = AppSpacing.xxl)
            .testTag("settings-sidebar"),
    ) {
        Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(vertical = AppSpacing.l)) {
            Text(tr("Settings"), Modifier.padding(horizontal = AppSpacing.l), style = AppText.itemTitleBold)
            Spacer(Modifier.height(AppSpacing.m))
            ScrollColumn(
                Modifier.weight(1f, fill = false).fillMaxWidth(),
                contentModifier = Modifier.padding(start = AppSpacing.s, end = AppSpacing.s + AppSizes.scrollbarRoom),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                SettingsSection.entries.forEach { section -> SidebarRow(section, section == current) { onSelect(section) } }
            }
        }
    }
}

@Composable
private fun SidebarRow(section: SettingsSection, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .then(if (selected) Modifier.background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.selected(colors.primary.copy(alpha = AppAlpha.line)), shape) else Modifier)
            .clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s + AppSpacing.xxs).testTag("settings-section-${section.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(section.icon, contentDescription = null, modifier = Modifier.size(AppSizes.icon), tint = tint)
        Text(
            tr(section.label),
            style = if (selected) AppText.labelStrong else AppText.label,
            color = if (selected) colors.primary else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The width of the sidebar's card, narrower than the Playlists one: its rows are a word each. */
private val SIDEBAR = 240.dp
