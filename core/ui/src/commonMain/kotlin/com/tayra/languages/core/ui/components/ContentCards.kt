package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Colours of status pills, tags and banners, readable on light and dark themes alike. */
object StatusTints {
    val ok = Color(0xFF2E9D57)
    val warning = Color(0xFFD68A00)
}

/** The screen's name as a heading, on wide windows only: phones show it in the top bar instead. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    if (LocalWindowWidth.current.isCompact) return
    Text(title, modifier, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

/** The page heading: an optional way back to Settings, the title and subtitle, and actions on the right. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    onBackToSettings: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val compact = LocalWindowWidth.current.isCompact
    Column(Modifier.fillMaxWidth()) {
        if (onBackToSettings != null && !compact) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBackToSettings).padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text("Settings", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions()
        }
    }
}

/** A white card with an icon tile, a title and a subtitle, and its content below. */
@Composable
fun ContentCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconText: String? = null,
    modifier: Modifier = Modifier,
    headerExtra: @Composable RowScope.() -> Unit = {},
    titleExtra: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, iconText)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    titleExtra()
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            headerExtra()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun IconTile(icon: ImageVector?, text: String? = null, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((size / 2).dp))
        else if (text != null) Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * One setting: its name and an optional description on the left, the control on the right. On
 * phones a wide control goes below the text.
 */
@Composable
fun SettingRow(
    title: String,
    description: String? = null,
    divider: Boolean = false,
    stackOnCompact: Boolean = false,
    control: @Composable () -> Unit,
) {
    if (divider) HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
    val text: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (stackOnCompact && LocalWindowWidth.current.isCompact) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            text(Modifier)
            control()
        }
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            text(Modifier.weight(1f).padding(end = 16.dp))
            control()
        }
    }
}

@Composable
fun SwitchSetting(title: String, description: String?, checked: Boolean, divider: Boolean = false, onChange: (Boolean) -> Unit) {
    SettingRow(title, description, divider) { Switch(checked = checked, onCheckedChange = onChange) }
}

/** A value with minus and plus buttons around a slider, moving in [step]s within [range]. */
@Composable
fun SliderStepper(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    label: String,
    name: String,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dividing by the number of steps per unit, not multiplying by the step, lands on the nearest float of a value such as 1.3.
    val perUnit = kotlin.math.round(1 / step)
    fun snap(v: Float) = (kotlin.math.round(v * perUnit) / perUnit).coerceIn(range.start, range.endInclusive)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ValueChip(label)
        StepButton("−", "Decrease $name", enabled = value > range.start + step / 2) { onChange(snap(value - step)) }
        RoundSlider(value = value, onValueChange = { onChange(snap(it)) }, valueRange = range, modifier = Modifier.weight(1f))
        StepButton("+", "Increase $name", enabled = value < range.endInclusive - step / 2) { onChange(snap(value + step)) }
    }
}

/** A slider with a round thumb on a thin track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = SliderDefaults.colors(activeTrackColor = primary, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant, thumbColor = primary)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = colors,
        thumb = {
            Box(
                Modifier.size(22.dp).shadow(2.dp, CircleShape).background(primary, CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(8.dp),
            )
        },
    )
}

/** A whole number with minus and plus buttons on either side. */
@Composable
fun NumberStepper(value: Int, range: IntRange, name: String, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", "Decrease $name", enabled = value > range.first, bordered = false) { onChange(value - 1) }
        Box(Modifier.height(44.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(value.toString(), Modifier.widthIn(min = 64.dp).padding(horizontal = 12.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Box(Modifier.height(44.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        StepButton("+", "Increase $name", enabled = value < range.last, bordered = false) { onChange(value + 1) }
    }
}

@Composable
private fun ValueChip(text: String) {
    Box(
        Modifier.widthIn(min = 72.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, bordered: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(44.dp).clip(shape)
            .then(if (bordered) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (symbol == "+") {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f))
        } else {
            Text(
                symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f),
            )
        }
    }
}

/** A tinted note with an info icon. */
@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, icon: ImageVector = Icons.Default.Info) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.07f))
            .border(1.dp, tint.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

/** A rounded label with a coloured dot, such as "Offline translation ready". */
@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** A small tinted tag, such as "Offline" or "Free". */
@Composable
fun Tag(text: String, color: Color) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

/** The outlined button with an icon used for page actions. */
@Composable
fun HeaderButton(text: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "${(bytes / 10_000_000) / 100.0} GB"
    bytes >= 1_000_000 -> "${(bytes / 100_000) / 10.0} MB"
    bytes >= 1_000 -> "${bytes / 1_000} kB"
    else -> "$bytes B"
}

/** The scrolling page body, centred and kept to a readable width on large windows. */
@Composable
fun PageColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    Box(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 1480.dp).fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 48.dp, vertical = if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

enum class PackageTab(val label: String) { INSTALLED("Installed"), AVAILABLE("Available") }

/** The Installed / Available switch above a list of downloads. */
@Composable
fun TabToggle(selected: PackageTab, onSelect: (PackageTab) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(3.dp)) {
        PackageTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

private val codeTints = listOf(
    Color(0xFF3B6FE0), Color(0xFF1FA463), Color(0xFF7C4DDB), Color(0xFFDC4A4A), Color(0xFFC98A05), Color(0xFF0E8FA3), Color(0xFFD9488B),
)

/** A language code on a square tinted per code, as on download tiles. */
@Composable
fun CodeTile(languageCode: String, size: Int = 44) {
    val code = languageCode.uppercase().take(3)
    val tint = codeTints[(code.hashCode() and Int.MAX_VALUE) % codeTints.size]
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Text(code, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
