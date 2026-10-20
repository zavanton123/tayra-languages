package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.settings.HotkeyCategory
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.CardHeader
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.StatusDot
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Keyboard shortcut editor: every action grouped by what it does, with its keys drawn as keycaps.
 * Selecting one listens for a key combination; Backspace clears it and Escape keeps it as it was.
 */
@Composable
fun ShortcutsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val hotkeys = settings.hotkeys
    var editing by remember { mutableStateOf<HotkeyAction?>(null) }
    var query by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    val compact = LocalWindowWidth.current.isCompact
    val conflicts = shortcutConflicts(hotkeys)
    val assign: (HotkeyAction, Hotkey?) -> Unit = { action, hotkey -> viewModel.update { it.copy(hotkeys = it.hotkeys + (action to hotkey)) } }

    Scaffold(
        topBar = { AppTopBar(title = tr("Keyboard shortcuts"), onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader(tr("Keyboard shortcuts"), tr("Customise how you navigate and listen while reading."), onBackToSettings = onBack) {
                HeaderButton(if (compact) tr("Reset") else tr("Reset to defaults"), Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            SearchBanner(query, onQuery = { query = it }, assigned = hotkeys.values.count { it != null }, conflicts = conflicts.size)

            val groups = HotkeyAction.byCategory.mapValues { (_, actions) -> actions.filter { it.matches(query, hotkeys[it]) } }.filterValues { it.isNotEmpty() }
            if (groups.isEmpty()) {
                Text(tr("No shortcuts match “{0}”.", query.trim()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val card: @Composable (HotkeyCategory, List<HotkeyAction>) -> Unit = { category, actions ->
                CategoryCard(category, actions, hotkeys, conflicts, editing, onEdit = { editing = it }, onDone = { editing = null }, onAssign = assign)
            }
            if (LocalWindowWidth.current.isExpanded) {
                // Two columns of about the same height: each group goes to the shorter one.
                val columns = listOf(mutableListOf<HotkeyCategory>(), mutableListOf())
                val heights = intArrayOf(0, 0)
                groups.forEach { (category, actions) ->
                    val column = if (heights[0] <= heights[1]) 0 else 1
                    columns[column] += category
                    heights[column] += actions.size + 2
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    columns.forEach { categories ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                            categories.forEach { card(it, groups.getValue(it)) }
                        }
                    }
                }
            } else {
                groups.forEach { (category, actions) -> card(category, actions) }
            }
        }
    }

    if (confirmReset) {
        AppDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Reset to defaults?")) },
            text = { Text(tr("Every keyboard shortcut goes back to its default key.")) },
            confirmButton = { PrimaryButton(onClick = { confirmReset = false; editing = null; viewModel.update { it.copy(hotkeys = HotkeyAction.defaults) } }) { Text(tr("Reset")) } },
            dismissButton = { QuietButton(onClick = { confirmReset = false }) { Text(tr("Cancel")) } },
        )
    }
}

/**
 * Actions sharing a key with another one that can fire in the same moment. A word shortcut and a
 * listening one may share a key, since the reader picks by whether a word is selected.
 */
internal fun shortcutConflicts(hotkeys: Map<HotkeyAction, Hotkey?>): Map<HotkeyAction, List<HotkeyAction>> =
    hotkeys.entries.mapNotNull { (action, key) ->
        if (key == null) return@mapNotNull null
        val listening = action.category == HotkeyCategory.LISTENING
        val others = hotkeys.filter { (other, otherKey) -> other != action && otherKey == key && (other.category == HotkeyCategory.LISTENING) == listening }.keys
        if (others.isEmpty()) null else action to others.toList()
    }.toMap()

private fun HotkeyAction.matches(query: String, hotkey: Hotkey?): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val (title, note) = titleAndNote()
    return description.contains(q, ignoreCase = true) || title.contains(q, ignoreCase = true) || note?.contains(q, ignoreCase = true) == true ||
        category.label.contains(q, ignoreCase = true) || category.title().contains(q, ignoreCase = true) ||
        (hotkey != null && keyNames(hotkey).any { it.equals(q, ignoreCase = true) })
}

/** The action's name, and what the parenthesised part of its description adds, such as "Alternative shortcut", in the interface language. */
private fun HotkeyAction.titleAndNote(): Pair<String, String?> {
    val open = description.indexOf(" (")
    if (open < 0 || !description.endsWith(")")) return tr(description) to null
    val note = description.substring(open + 2, description.length - 1)
    val shown = if (note == "second key") "Alternative shortcut" else note.replaceFirstChar { it.uppercase() }
    return tr(description.substring(0, open)) to tr(shown)
}

/** The group's heading: nouns such as "Translation" rather than the labels' verbs, which translate as commands. */
private fun HotkeyCategory.title(): String = when (this) {
    HotkeyCategory.NAVIGATION -> tr("Navigation")
    HotkeyCategory.LISTENING -> tr("Listening")
    HotkeyCategory.STATUS -> tr("Update status")
    HotkeyCategory.PAGING -> tr("Paging")
    HotkeyCategory.TRANSLATE -> tr("Translation")
    HotkeyCategory.DISPLAY -> tr("Display")
    HotkeyCategory.COPY -> tr("Copying")
    HotkeyCategory.MISC -> tr("Misc")
    HotkeyCategory.FLASHCARDS -> tr("Flashcards")
}

/** The keys of [hotkey] as keycap labels, modifiers first. */
private fun keyNames(hotkey: Hotkey): List<String> = buildList {
    if (hotkey.ctrl) add("Ctrl")
    if (hotkey.alt) add("Alt")
    if (hotkey.shift) add("Shift")
    add(
        when (hotkey.key) {
            "Left" -> "←"
            "Right" -> "→"
            "Up" -> "↑"
            "Down" -> "↓"
            "Escape" -> "Esc"
            "Plus" -> "+"
            "Minus" -> "−"
            else -> hotkey.key
        },
    )
}

private fun HotkeyCategory.icon(): ImageVector = when (this) {
    HotkeyCategory.NAVIGATION -> AppIcons.Explore
    HotkeyCategory.LISTENING -> AppIcons.VolumeUp
    HotkeyCategory.STATUS -> AppIcons.Flag
    HotkeyCategory.PAGING -> AppIcons.Page
    HotkeyCategory.TRANSLATE -> AppIcons.Translate
    HotkeyCategory.DISPLAY -> AppIcons.FormatSize
    HotkeyCategory.COPY -> AppIcons.ContentCopy
    HotkeyCategory.MISC -> AppIcons.Tune
    HotkeyCategory.FLASHCARDS -> AppIcons.School
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBanner(query: String, onQuery: (String) -> Unit, assigned: Int, conflicts: Int) {
    val compact = LocalWindowWidth.current.isCompact
    TintedSurface(MaterialTheme.colorScheme.primary, shape = AppShapes.card, padding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.m)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).widthIn(min = BANNER_TEXT_MIN_WIDTH), verticalAlignment = Alignment.CenterVertically) {
                IconTile(AppIcons.Keyboard, size = 44)
                Spacer(Modifier.width(AppSpacing.l))
                Text(tr("Select a shortcut, then press a key combination. Press Backspace to clear it."), style = MaterialTheme.typography.bodyLarge)
            }
            SearchField(query, onQuery, tr("Search shortcuts"), if (compact) Modifier.fillMaxWidth() else Modifier.width(SEARCH_WIDTH))
            if (!compact) AppVerticalDivider(Modifier.height(BANNER_DIVIDER_HEIGHT))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val ok = conflicts == 0
                val color = if (ok) StatusTints.ok else MaterialTheme.colorScheme.error
                Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(AppSizes.iconLarge))
                Spacer(Modifier.width(AppSpacing.s))
                Text(
                    tr("{0} assigned", assigned) + " · " + if (ok) tr("No conflicts") else tr("{0} in conflict", conflicts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: HotkeyCategory,
    actions: List<HotkeyAction>,
    hotkeys: Map<HotkeyAction, Hotkey?>,
    conflicts: Map<HotkeyAction, List<HotkeyAction>>,
    editing: HotkeyAction?,
    onEdit: (HotkeyAction) -> Unit,
    onDone: () -> Unit,
    onAssign: (HotkeyAction, Hotkey?) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().cardSurface().padding(vertical = AppSpacing.xl),
    ) {
        CardHeader(category.title(), modifier = Modifier.padding(horizontal = AppSpacing.xl), icon = category.icon(), titleExtra = {
            Tag(tr("{0} assigned", actions.count { hotkeys[it] != null }), StatusTints.ok)
        })
        Spacer(Modifier.height(AppSpacing.m))
        // An action's second key shares its row: one row an action, with a slot for each key.
        val seconds = actions.filter { it.isSecondKey }.associateBy { it.description.removeSuffix(SECOND_KEY) }
        val firsts = actions.filterNot { it.isSecondKey }.map { it.description }.toSet()
        val rows = actions.filter { !it.isSecondKey || it.description.removeSuffix(SECOND_KEY) !in firsts }
        rows.forEachIndexed { i, action ->
            val second = if (action.isSecondKey) null else seconds[action.description]
            val here = editing != null && (editing == action || editing == second)
            if (i > 0 && !here) AppDivider(Modifier.padding(horizontal = AppSpacing.xl))
            ShortcutRow(
                KeySlot(action, hotkeys[action], conflicts[action].orEmpty()),
                second?.let { KeySlot(it, hotkeys[it], conflicts[it].orEmpty()) },
                editing = editing,
                onEdit = onEdit,
                onDone = onDone,
                onAssign = onAssign,
            )
        }
    }
}

/** One key of an action: the action, its key if it has one, and the actions that share it. */
private class KeySlot(val action: HotkeyAction, val hotkey: Hotkey?, val conflictsWith: List<HotkeyAction>)

private const val SECOND_KEY = " (second key)"

private val HotkeyAction.isSecondKey: Boolean get() = description.endsWith(SECOND_KEY)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutRow(
    first: KeySlot,
    second: KeySlot?,
    editing: HotkeyAction?,
    onEdit: (HotkeyAction) -> Unit,
    onDone: () -> Unit,
    onAssign: (HotkeyAction, Hotkey?) -> Unit,
) {
    val (title, note) = first.action.titleAndNote()
    val compact = LocalWindowWidth.current.isCompact
    val primary = MaterialTheme.colorScheme.primary
    val conflictsWith = (first.conflictsWith + second?.conflictsWith.orEmpty()).distinct()
    val editingHere = editing != null && (editing == first.action || editing == second?.action)
    val text: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (conflictsWith.isNotEmpty()) {
                Text(
                    tr("Also used for: {0}", conflictsWith.joinToString { it.titleAndNote().first.lowercase() }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    val slot: @Composable (KeySlot, Boolean) -> Unit = { key, alternative ->
        val (_, keyNote) = key.action.titleAndNote()
        when {
            editing == key.action -> KeyListener(onDone = onDone, onAssign = { onAssign(key.action, it) })
            key.hotkey == null && alternative -> NotSet { onEdit(key.action) }
            key.hotkey == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                NotSet { onEdit(key.action) }
                Spacer(Modifier.width(AppSpacing.xl))
                Text(
                    tr("Add"),
                    color = primary,
                    style = AppText.emphasis,
                    modifier = Modifier.clickable { onEdit(key.action) }.semantics { contentDescription = tr("Add shortcut for {0}", title) }.padding(AppSpacing.xs),
                )
            }
            else -> Keycaps(
                key.hotkey,
                conflict = key.conflictsWith.isNotEmpty(),
                onClick = { onEdit(key.action) },
                description = if (keyNote == null) tr("Change shortcut for {0}", title) else tr("Change shortcut for {0}, {1}", title, keyNote),
            )
        }
    }
    // The keys of an action side by side, the second under the first when there is no room.
    val control: @Composable () -> Unit = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.s), itemVerticalAlignment = Alignment.CenterVertically) {
            if (editing != second?.action || second == null) slot(first, false)
            if (second != null && editing != first.action) slot(second, true)
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = AppSizes.row).then(if (editingHere) Modifier.background(primary.copy(alpha = AppAlpha.wash)) else Modifier)) {
        // The row being edited is marked by a bar on its leading edge.
        Box(Modifier.width(EDIT_BAR_WIDTH).heightIn(min = AppSizes.row).then(if (editingHere) Modifier.background(primary) else Modifier))
        if (compact) {
            Column(Modifier.weight(1f).padding(start = AppSpacing.l, end = AppSpacing.xl, top = AppSpacing.m, bottom = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                text(Modifier)
                control()
            }
        } else {
            Row(Modifier.weight(1f).padding(start = AppSpacing.l, end = AppSpacing.xl, top = AppSpacing.s, bottom = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                text(Modifier.weight(1f).padding(end = AppSpacing.l))
                // Keys start at the same place on every row, so they read as a column; the listener needs more room.
                if (editingHere) control() else Box(Modifier.widthIn(min = KEYS_WIDTH), contentAlignment = Alignment.CenterStart) { control() }
            }
        }
    }
}

/** Room for the longest combination, such as Ctrl + Shift + ←. */
private val KEYS_WIDTH = 250.dp
private val KEY_HEIGHT = 38.dp
private val KEY_MIN_WIDTH = 44.dp
private val NOT_SET_WIDTH = 128.dp
private val LISTENER_WIDTH = 260.dp
private val EDIT_BAR_WIDTH = 4.dp
private val SEARCH_WIDTH = 300.dp
private val BANNER_TEXT_MIN_WIDTH = 280.dp
private val BANNER_DIVIDER_HEIGHT = 36.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Keycaps(hotkey: Hotkey, conflict: Boolean, onClick: () -> Unit, description: String) {
    // A set key shows on hover that it can be changed.
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    FlowRow(
        Modifier.clip(AppShapes.compact).hoverable(interaction).background(if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.tint) else Color.Transparent)
            .clickable(onClick = onClick).semantics { contentDescription = description }.padding(AppSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        keyNames(hotkey).forEachIndexed { i, name ->
            if (i > 0) Text("+", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Keycap(name, conflict)
        }
    }
}

@Composable
private fun Keycap(label: String, conflict: Boolean) {
    val border = if (conflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier.heightIn(min = KEY_HEIGHT).widthIn(min = KEY_MIN_WIDTH).clip(AppShapes.compact)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(AppBorders.hairline(border), AppShapes.compact)
            // A thicker bottom edge makes the label read as a key.
            .drawBehind {
                val edge = 3.dp.toPx()
                drawRoundRect(border.copy(alpha = AppAlpha.muted), topLeft = Offset(0f, size.height - edge), size = Size(size.width, edge), cornerRadius = CornerRadius(8.dp.toPx()))
            }
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        contentAlignment = Alignment.Center,
    ) {
        val arrow = ARROWS[label]
        // Arrow characters render small and low in some system fonts, so arrows are drawn as icons.
        if (arrow != null) Icon(arrow, contentDescription = label, modifier = Modifier.size(AppSizes.iconSmall))
        else Text(label, style = AppText.label)
    }
}

private val ARROWS: Map<String, ImageVector> by lazy {
    mapOf("\u2190" to AppIcons.ArrowLeft, "\u2192" to AppIcons.ArrowRight, "\u2191" to AppIcons.ArrowUp, "\u2193" to AppIcons.ArrowDown)
}

/** A dashed empty key for an action with no shortcut. */
@Composable
private fun NotSet(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.outline
    Box(
        Modifier.width(NOT_SET_WIDTH).height(KEY_HEIGHT).clip(AppShapes.compact).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color.copy(alpha = AppAlpha.muted),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(tr("Not set"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Waits for the next key combination: Escape or Cancel keeps the old one, Backspace clears it. */
@Composable
private fun KeyListener(onDone: () -> Unit, onAssign: (Hotkey?) -> Unit) {
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    LaunchedEffect(Unit) { requester.requestFocus() }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.width(LISTENER_WIDTH).height(AppSizes.control).clip(AppShapes.compact).background(MaterialTheme.colorScheme.surface)
                    .border(AppBorders.selected(primary), AppShapes.compact)
                    .focusRequester(requester)
                    .onFocusChanged {
                        // Clicking elsewhere ends the listening, as Cancel does.
                        if (focused && !it.isFocused) onDone()
                        focused = it.isFocused
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        // A modifier on its own is part of a combination still being pressed.
                        val pressed = HotkeyMatcher.fromEvent(event) ?: return@onPreviewKeyEvent true
                        when (pressed.key) {
                            "Escape" -> Unit
                            "Backspace", "Delete" -> onAssign(null)
                            else -> onAssign(pressed)
                        }
                        onDone()
                        true
                    }
                    .focusable()
                    .semantics { contentDescription = tr("Press a key combination") }
                    .padding(horizontal = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
                Spacer(Modifier.width(AppSpacing.s))
                Text(tr("Press a key combination…"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(AppSpacing.xl))
            Text(
                tr("Cancel"),
                color = primary,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onDone).padding(AppSpacing.xs),
            )
        }
        Spacer(Modifier.height(AppSpacing.s))
        Row(Modifier.width(LISTENER_WIDTH), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Backspace to clear"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            StatusDot(primary, size = AppSizes.dot)
            Spacer(Modifier.width(AppSpacing.xs))
            Text(tr("Listening for keys"), style = MaterialTheme.typography.bodySmall, color = primary)
        }
    }
}
