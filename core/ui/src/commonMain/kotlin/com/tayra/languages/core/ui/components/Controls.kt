package com.tayra.languages.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.tayra.languages.core.ui.theme.AppShapes

/*
 * The design system's controls. Screens use these, not Material's buttons, text fields, dialogs and
 * chips, so that every control has the same shape, sizes and colours; `DesignSystemTest` holds them
 * to it. Material's components that keep their own look (icon buttons, switches, sliders, menus)
 * are used as they are, with the theme's shapes (AppShapes.material).
 */

/** How much room a button takes: [Regular] for actions on a page, [Compact] inside rows, cards' corners and dense lists. */
enum class ButtonSize(internal val padding: PaddingValues, internal val minHeight: Int) {
    Regular(PaddingValues(horizontal = 18.dp, vertical = 10.dp), 40),
    Compact(PaddingValues(horizontal = 12.dp, vertical = 4.dp), 32),
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
    content: @Composable RowScope.() -> Unit,
) {
    val danger = tone == ButtonTone.Danger
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = size.minHeight.dp),
        enabled = enabled,
        shape = AppShapes.control,
        colors = colors ?: if (danger) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors(),
        border = if (danger && enabled) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else ButtonDefaults.outlinedButtonBorder(enabled),
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
        contentPadding = if (size == ButtonSize.Compact) size.padding else ButtonDefaults.TextButtonContentPadding,
        content = content,
    )
}

/** [PrimaryButton] with a label, an optional leading [icon], and a spinner in its place while [loading]. */
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
) = PrimaryButton(onClick, modifier, enabled && !loading, tone, size) { ButtonLabel(text, icon, loading) }

/** [SecondaryButton] with a label, an optional leading [icon], and a spinner in its place while [loading]. */
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
) = SecondaryButton(onClick, modifier, enabled && !loading, tone, size) { ButtonLabel(text, icon, loading) }

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
) = QuietButton(onClick, modifier, enabled && !loading, tone, size) { ButtonLabel(text, icon, loading) }

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, loading: Boolean) {
    when {
        loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    if (loading || icon != null) Spacer(Modifier.width(8.dp))
    Text(text)
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
