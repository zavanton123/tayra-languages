package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppElevation
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/*
 * The design system's controls. Screens use these, not Material's buttons, text fields, dialogs and
 * chips, switches and sliders, so that every control has the same shape, sizes and colours;
 * `DesignSystemTest` holds them to it.
 */

/**
 * How much room a button takes: [Regular] for actions on a page, [Compact] inside rows, cards'
 * corners and dense lists, [Large] for the one big action of a card, a sheet or a phone screen.
 */
enum class ButtonSize(internal val padding: PaddingValues, internal val minHeight: Int) {
    Regular(PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.s), 40),
    Compact(PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.xs), 32),
    Large(PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.m), 52),
}

/** What a button does: [Normal], or [Danger] for an action that deletes or cannot be undone. */
enum class ButtonTone { Normal, Danger }

/** The page's main action: filled with the primary colour (the error colour for [ButtonTone.Danger]). */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val danger = tone == ButtonTone.Danger
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = size.minHeight.dp),
        enabled = enabled,
        shape = AppShapes.control,
        colors = colors ?: if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
        else ButtonDefaults.buttonColors(),
        contentPadding = size.padding,
        content = content,
    )
}

/** A further action beside the main one: outlined. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    colors: ButtonColors? = null,
    tint: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val accent = if (tone == ButtonTone.Danger) MaterialTheme.colorScheme.error else tint
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = size.minHeight.dp),
        enabled = enabled,
        shape = AppShapes.control,
        colors = colors ?: when {
            tone == ButtonTone.Danger -> ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            tint != null -> ButtonDefaults.outlinedButtonColors(containerColor = tint.copy(alpha = AppAlpha.wash), contentColor = tint)
            else -> ButtonDefaults.outlinedButtonColors()
        },
        border = if (accent != null && enabled) AppBorders.hairline(accent.copy(alpha = AppAlpha.half)) else ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding = size.padding,
        content = content,
    )
}

/** A light action: text only, as dialogs' buttons and links within a card. */
@Composable
fun QuietButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = size.minHeight.dp),
        enabled = enabled,
        shape = AppShapes.control,
        colors = colors ?: if (tone == ButtonTone.Danger) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors(),
        contentPadding = if (size == ButtonSize.Regular) ButtonDefaults.TextButtonContentPadding else size.padding,
        content = content,
    )
}

/**
 * [PrimaryButton] with a label, an optional leading [icon], and a spinner in its place while
 * [loading]; [trailingIcon] follows the label (an arrow) and [badge] a count.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    trailingIcon: ImageVector? = null,
    badge: Int? = null,
) = PrimaryButton(onClick, modifier, enabled && !loading, tone, size) { ButtonLabel(text, icon, loading, size, trailingIcon, badge, onColor = true) }

/**
 * [SecondaryButton] with a label, an optional leading [icon], and a spinner in its place while
 * [loading]; [trailingIcon] follows the label (an arrow), [badge] a count, and [tint] colours it.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    trailingIcon: ImageVector? = null,
    badge: Int? = null,
    tint: Color? = null,
) = SecondaryButton(onClick, modifier, enabled && !loading, tone, size, tint = tint) { ButtonLabel(text, icon, loading, size, trailingIcon, badge) }

/** [QuietButton] with a label, an optional leading [icon], and a spinner in its place while [loading]. */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: ButtonTone = ButtonTone.Normal,
    size: ButtonSize = ButtonSize.Regular,
    trailingIcon: ImageVector? = null,
) = QuietButton(onClick, modifier, enabled && !loading, tone, size) { ButtonLabel(text, icon, loading, size, trailingIcon, null) }

/** A [SecondaryButton] floating over a list: on a solid surface with a shadow, so what scrolls beneath never shows through it. */
@Composable
fun FloatingButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) = SecondaryButton(
    onClick,
    modifier.shadow(AppElevation.raised, AppShapes.control),
    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
) { ButtonLabel(text, icon, loading = false, ButtonSize.Regular, trailingIcon = null, badge = null) }

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, loading: Boolean, size: ButtonSize, trailingIcon: ImageVector?, badge: Int?, onColor: Boolean = false) {
    val iconSize = if (size == ButtonSize.Large) AppSizes.iconMedium else AppSizes.iconCompact
    when {
        loading -> CircularProgressIndicator(Modifier.size(AppSizes.iconSmall), strokeWidth = 2.dp, color = LocalContentColor.current)
        icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }
    if (loading || icon != null) Spacer(Modifier.width(AppSpacing.s))
    Text(text, style = if (size == ButtonSize.Large) AppText.emphasis else LocalTextStyle.current)
    if (badge != null) {
        Spacer(Modifier.width(AppSpacing.s))
        CountBadge(badge, emphasis = if (onColor) BadgeEmphasis.OnColor else BadgeEmphasis.Muted)
    }
    if (trailingIcon != null) {
        Spacer(Modifier.width(AppSpacing.s))
        Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(if (size == ButtonSize.Large) AppSizes.icon else AppSizes.iconCompact))
    }
}

/** A text field, outlined, with the controls' shape. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    maxLines = maxLines,
    minLines = minLines,
    shape = AppShapes.control,
    colors = colors,
)

/**
 * A text field for a whole text, outlined like [AppTextField] and as tall as [modifier] makes it: a
 * longer text scrolls inside, with [EdgeScrollbar] beside it. [fieldModifier] goes on the field itself.
 */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    fieldModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scroll = rememberScrollState()
    val border = if (focused) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outline)
    BoxWithConstraints(modifier.clip(AppShapes.control).border(border, AppShapes.control)) {
        val viewport = maxHeight
        Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                // As tall as the box at least, so a click anywhere in it starts typing.
                modifier = Modifier.fillMaxWidth().heightIn(min = viewport).padding(AppSpacing.l).then(fieldModifier),
                textStyle = textStyle.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                interactionSource = interaction,
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty() && placeholder != null) Text(placeholder, style = textStyle, color = colors.onSurfaceVariant)
                        field()
                    }
                },
            )
        }
        EdgeScrollbar(scroll)
    }
}

/** A dialog with a title, a body and its buttons (usually [QuietButton]s), shaped like the app's other large containers. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = confirmButton,
    modifier = modifier,
    dismissButton = dismissButton,
    icon = icon,
    title = title,
    text = text,
    shape = AppShapes.large,
    properties = properties,
)

/** A chip that filters a list, on or off. */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) = FilterChip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier,
    enabled = enabled,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    shape = AppShapes.control,
)

/** An on/off switch. */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
}

/** A slider with a round thumb on a thin track; [steps] stops in between, as Material counts them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = SliderDefaults.colors(activeTrackColor = primary, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant, thumbColor = primary)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        thumb = {
            Box(
                Modifier.size(SLIDER_THUMB_SIZE).shadow(AppElevation.low, AppShapes.circle).background(if (enabled) primary else MaterialTheme.colorScheme.outline, AppShapes.circle)
                    .border(3.dp, MaterialTheme.colorScheme.surface, AppShapes.circle),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                enabled = enabled,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(AppSizes.trackThick),
            )
        },
    )
}


/**
 * A button that is on or off, such as Filters while its panel is open: an optional [icon], its
 * [text], a [badge] counting what it holds and a [trailingIcon], tinted while [active].
 */
@Composable
fun ToggleButton(
    text: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badge: Int = 0,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.control
    val tint by animateColorAsState(if (active) colors.primary else colors.onSurface, AppMotion.spec(AppMotion.short))
    val fill by animateColorAsState(if (active) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface, AppMotion.spec(AppMotion.short))
    Row(
        modifier.height(AppSizes.control).clip(shape)
            .background(fill)
            .border(if (active) AppBorders.selected() else AppBorders.hairline(), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = AppSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.iconCompact))
        if (text != null) Text(text, style = AppText.emphasis, color = tint, maxLines = 1)
        if (badge > 0) CountBadge(badge, emphasis = BadgeEmphasis.Solid)
        if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconCompact))
    }
}

/**
 * What opens a choice: the chosen [value] and a ▾, at the controls' height; with a [label] it is
 * taller and names the choice above the value. [leading] is drawn before the value (a flag), or
 * else [icon]. [active] marks a choice that filters a list.
 */
@Composable
fun SelectButton(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.control
    Box(
        modifier.height(if (label != null) LABELED_SELECT_HEIGHT else AppSizes.control).clip(shape).background(colors.surface)
            .border(if (active) AppBorders.selected() else AppBorders.hairline(), shape)
            .clickable(enabled = enabled, role = Role.DropdownList, onClick = onClick)
            .padding(start = AppSpacing.m, end = AppSpacing.s),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.padding(end = AppSizes.iconMedium + AppSpacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            when {
                leading != null -> leading()
                icon != null -> Icon(icon, contentDescription = null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
            }
            Column {
                if (label != null) Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1)
                Text(
                    value,
                    style = AppText.emphasis,
                    color = if (!enabled) colors.onSurface.copy(alpha = AppAlpha.disabled) else if (active) colors.primary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterEnd).size(AppSizes.iconMedium))
    }
}

/**
 * A [SelectButton] with its menu of [options]: [value] shows instead of the chosen option's label
 * when given, and [optionContent] draws an option its own way (a font in itself).
 */
@Composable
fun <T> SelectMenu(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    value: String? = null,
    label: String? = null,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    buttonModifier: Modifier = Modifier,
    optionContent: (@Composable (T) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    // The button takes the box's width when the caller gives the box one, and its own otherwise.
    Box(modifier, propagateMinConstraints = true) {
        SelectButton(value ?: selected?.let(optionLabel).orEmpty(), onClick = { open = true }, modifier = buttonModifier, label = label, icon = icon, active = active, enabled = enabled)
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(
                    text = {
                        if (optionContent != null) optionContent(option)
                        else Text(optionLabel(option), color = if (option == selected) MaterialTheme.colorScheme.primary else Color.Unspecified, fontWeight = if (option == selected) FontWeight.SemiBold else null)
                    },
                    onClick = { open = false; onSelect(option) },
                )
            }
        }
    }
}

/** A link within a line or a header, such as a breadcrumb or "Powered by Tatoeba": text that acts, with an optional icon before or after. */
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Row(
        modifier.clip(AppShapes.small).clickable(role = Role.Button, onClick = onClick).padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(AppSizes.iconCompact))
        Text(text, Modifier.weight(1f, fill = false), style = style.copy(fontWeight = AppText.emphasis.fontWeight), color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = color, modifier = Modifier.size(AppSizes.iconSmall))
    }
}

/** A field's name above it, for fields and choices that carry no label of their own. */
@Composable
fun LabeledField(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(label, style = AppText.label)
        content()
    }
}

private val SLIDER_THUMB_SIZE = 22.dp
private val LABELED_SELECT_HEIGHT = 56.dp
