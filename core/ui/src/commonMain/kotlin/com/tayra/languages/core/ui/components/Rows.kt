package com.tayra.languages.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing

/*
 * The design system's rows of settings and details: a setting with its control, a switch, a tick,
 * a row that opens something, a slider with its value, and a label with its value.
 */

@Composable
private fun RowText(title: String, description: String?, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
        if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One setting: an optional [icon], its name and description on the left, the control on the right.
 * On phones a wide control goes below the text when [stackOnCompact]. [onClick] makes the whole row
 * act; [dense] drops its minimum height, for rows in a list; [indent] sets it under another.
 */
@Composable
fun SettingRow(
    title: String,
    description: String? = null,
    divider: Boolean = false,
    stackOnCompact: Boolean = false,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dense: Boolean = false,
    indent: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    control: @Composable RowScope.() -> Unit,
) {
    if (divider) AppDivider(Modifier.padding(vertical = AppSpacing.xs))
    if (stackOnCompact && LocalWindowWidth.current.isCompact) {
        Column(modifier.fillMaxWidth().padding(start = indent, top = AppSpacing.s, bottom = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            RowText(title, description, Modifier)
            Row(verticalAlignment = Alignment.CenterVertically, content = control)
        }
    } else {
        Row(
            modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clip(AppShapes.compact).clickable(onClick = onClick) else Modifier)
                .heightIn(min = if (dense) 0.dp else AppSizes.row)
                .padding(start = indent, top = AppSpacing.s, bottom = AppSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconMedium))
                Spacer(Modifier.width(AppSpacing.l))
            }
            RowText(title, description, Modifier.weight(1f).padding(end = AppSpacing.l))
            control()
        }
    }
}

/** A setting that is on or off; the whole row toggles it. */
@Composable
fun SwitchSetting(
    title: String,
    description: String?,
    checked: Boolean,
    divider: Boolean = false,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dense: Boolean = false,
    enabled: Boolean = true,
    switchModifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    SettingRow(title, description, divider, modifier = modifier, icon = icon, dense = dense, onClick = if (enabled) ({ onChange(!checked) }) else null) {
        AppSwitch(checked = checked, onCheckedChange = onChange, modifier = switchModifier, enabled = enabled)
    }
}

/** A choice that is ticked or not, its tick before its name; the whole row ticks it. */
@Composable
fun CheckSetting(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    checkboxModifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clip(AppShapes.compact).clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        AppCheckbox(checked = checked, onCheckedChange = onChange, modifier = checkboxModifier, enabled = enabled)
        RowText(title, description, Modifier.weight(1f))
    }
}

/**
 * A row that opens something: an optional [icon], its name, an optional [value] and a chevron.
 * [tint] colours the name and icon, for a link (primary) or a destructive action (error).
 */
@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    description: String? = null,
    icon: ImageVector? = null,
    tint: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(AppShapes.compact).clickable(onClick = onClick).padding(vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint ?: colors.primary, modifier = Modifier.size(AppSizes.iconMedium))
        RowText(title, description, Modifier.weight(1f), color = tint ?: colors.onSurface)
        if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconLarge))
    }
}

/**
 * A setting on a slider: its name and [valueLabel] above the slider. [step], when given, snaps the
 * value to it; [sliderModifier] is the slider's, for its test tag or name.
 */
@Composable
fun SliderSetting(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float? = null,
    sliderModifier: Modifier = Modifier,
    description: String? = null,
) {
    // Dividing by the number of steps per unit, not multiplying by the step, lands on the nearest float of a value such as 1.3.
    val perUnit = step?.let { kotlin.math.round(1 / it) }
    fun snap(v: Float) = if (perUnit == null) v else (kotlin.math.round(v * perUnit) / perUnit).coerceIn(range.start, range.endInclusive)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RowText(title, description, Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppSlider(
            value = value.coerceIn(range),
            onValueChange = { v -> snap(v).let { if (it != value) onChange(it) } },
            valueRange = range,
            modifier = sliderModifier.fillMaxWidth(),
        )
    }
}

/** A detail: its label on the left, its value after it. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
