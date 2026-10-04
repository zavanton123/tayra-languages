package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import com.tayra.languages.core.ui.components.HeaderButton
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.Tag
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.settings.HotkeyCategory
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.navigation.Route
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
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            ScreenHeader("Keyboard shortcuts", "Customize how you navigate and listen while reading.", onBackToSettings = onBack) {
                HeaderButton(if (compact) "Reset" else "Reset to defaults", Icons.Default.Refresh, onClick = { confirmReset = true })
            }
            SearchBanner(query, onQuery = { query = it }, assigned = hotkeys.values.count { it != null }, conflicts = conflicts.size)

            val groups = HotkeyAction.byCategory.mapValues { (_, actions) -> actions.filter { it.matches(query, hotkeys[it]) } }.filterValues { it.isNotEmpty() }
            if (groups.isEmpty()) {
                Text("No shortcuts match “${query.trim()}”.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    columns.forEach { categories ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset to defaults?") },
            text = { Text("Every keyboard shortcut goes back to its default key.") },
            confirmButton = { Button(onClick = { confirmReset = false; editing = null; viewModel.update { it.copy(hotkeys = HotkeyAction.defaults) } }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
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
    return description.contains(q, ignoreCase = true) || category.label.contains(q, ignoreCase = true) ||
        (hotkey != null && keyNames(hotkey).any { it.equals(q, ignoreCase = true) })
}

/** The action's name, and what the parenthesised part of its description adds, such as "Alternative shortcut". */
private fun HotkeyAction.titleAndNote(): Pair<String, String?> {
    val open = description.indexOf(" (")
    if (open < 0 || !description.endsWith(")")) return description to null
    val note = description.substring(open + 2, description.length - 1)
    val shown = if (note == "second key") "Alternative shortcut" else note.replaceFirstChar { it.uppercase() }
    return description.substring(0, open) to shown
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBanner(query: String, onQuery: (String) -> Unit, assigned: Int, conflicts: Int) {
    val compact = LocalWindowWidth.current.isCompact
    val tint = MaterialTheme.colorScheme.primary
    FlowRow(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).widthIn(min = 280.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppIcons.Keyboard, size = 44)
            Spacer(Modifier.width(16.dp))
            Text("Select a shortcut, then press a key combination. Press Backspace to clear it.", style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search shortcuts") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = if (compact) Modifier.fillMaxWidth() else Modifier.width(300.dp),
        )
        if (!compact) Box(Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ok = conflicts == 0
            val color = if (ok) StatusTints.ok else MaterialTheme.colorScheme.error
            Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "$assigned assigned · " + if (ok) "No conflicts" else "$conflicts in conflict",
                style = MaterialTheme.typography.bodyLarge,
                color = color,
            )
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(vertical = 20.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(category.icon())
            Spacer(Modifier.width(16.dp))
            Text(category.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Tag("${actions.count { hotkeys[it] != null }} assigned", StatusTints.ok)
        }
        Spacer(Modifier.height(10.dp))
        actions.forEachIndexed { i, action ->
            if (i > 0 && editing != action && editing != actions[i - 1]) {
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            ShortcutRow(
                action,
                hotkeys[action],
                conflictsWith = conflicts[action].orEmpty(),
                editing = editing == action,
                onEdit = { onEdit(action) },
                onDone = onDone,
                onAssign = { onAssign(action, it) },
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    action: HotkeyAction,
    hotkey: Hotkey?,
    conflictsWith: List<HotkeyAction>,
    editing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onAssign: (Hotkey?) -> Unit,
) {
    val (title, note) = action.titleAndNote()
    val compact = LocalWindowWidth.current.isCompact
    val primary = MaterialTheme.colorScheme.primary
    val text: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (conflictsWith.isNotEmpty()) {
                Text(
                    "Also used for: ${conflictsWith.joinToString { it.titleAndNote().first.lowercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    val control: @Composable () -> Unit = {
        when {
            editing -> KeyListener(onDone = onDone, onAssign = onAssign)
            hotkey == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                NotSet(onEdit)
                Spacer(Modifier.width(18.dp))
                Text(
                    "Add",
                    color = primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onEdit).semantics { contentDescription = "Add shortcut for $title" }.padding(4.dp),
                )
            }
            else -> Keycaps(hotkey, conflict = conflictsWith.isNotEmpty(), onClick = onEdit, description = "Change shortcut for $title${note?.let { ", $it" }.orEmpty()}")
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).then(if (editing) Modifier.background(primary.copy(alpha = 0.06f)) else Modifier)) {
        // The row being edited is marked by a bar on its leading edge.
        Box(Modifier.width(4.dp).heightIn(min = 56.dp).then(if (editing) Modifier.background(primary) else Modifier))
        if (compact) {
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                text(Modifier)
                control()
            }
        } else {
            Row(Modifier.weight(1f).padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                text(Modifier.weight(1f).padding(end = 16.dp))
                // Keys start at the same place on every row, so they read as a column; the listener needs more room.
                if (editing) control() else Box(Modifier.width(KEYS_WIDTH), contentAlignment = Alignment.CenterStart) { control() }
            }
        }
    }
}

/** Room for the longest combination, such as Ctrl + Shift + ←. */
private val KEYS_WIDTH = 250.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Keycaps(hotkey: Hotkey, conflict: Boolean, onClick: () -> Unit, description: String) {
    FlowRow(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).semantics { contentDescription = description }.padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        Modifier.heightIn(min = 38.dp).widthIn(min = 44.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            // A thicker bottom edge makes the label read as a key.
            .drawBehind {
                val edge = 3.dp.toPx()
                drawRoundRect(border.copy(alpha = 0.7f), topLeft = Offset(0f, size.height - edge), size = Size(size.width, edge), cornerRadius = CornerRadius(8.dp.toPx()))
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val arrow = ARROWS[label]
        // Arrow characters render small and low in some system fonts, so arrows are drawn as icons.
        if (arrow != null) Icon(arrow, contentDescription = label, modifier = Modifier.size(16.dp))
        else Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
        Modifier.width(128.dp).height(38.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Not set", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Modifier.width(260.dp).height(42.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, primary, RoundedCornerShape(8.dp))
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
                    .semantics { contentDescription = "Press a key combination" }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Press a key combination…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(18.dp))
            Text(
                "Cancel",
                color = primary,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onDone).padding(4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.width(260.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Backspace to clear", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(CircleShape).background(primary))
            Spacer(Modifier.width(6.dp))
            Text("Listening", style = MaterialTheme.typography.bodySmall, color = primary)
        }
    }
}
