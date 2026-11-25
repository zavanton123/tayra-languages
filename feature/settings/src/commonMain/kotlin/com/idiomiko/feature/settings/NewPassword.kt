package com.idiomiko.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.idiomiko.core.domain.account.PasswordStrength
import com.idiomiko.core.domain.account.PasswordStrength.Level
import com.idiomiko.core.domain.account.PasswordStrength.Rule
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTextField
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/** Whether a new password may be sent: every rule met, and typed the same twice. */
internal fun newPasswordReady(password: String, confirmation: String): Boolean =
    PasswordStrength.meetsAll(password) && password == confirmation

/**
 * A new password and its confirmation, for making an account or setting a forgotten password: the
 * password's strength as a bar and a word, the rules it is held to ticking off as they are met, and,
 * once the confirmation is left or as long as the password, whether the two match. [tag] names the fields for tests.
 */
@Composable
internal fun NewPasswordFields(
    password: String,
    onPassword: (String) -> Unit,
    confirmation: String,
    onConfirmation: (String) -> Unit,
    tag: String,
    label: String = tr("Password"),
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    var confirmationFocused by remember { mutableStateOf(false) }
    var confirmationLeft by rememberSaveable { mutableStateOf(false) }
    val mismatch = confirmation.isNotEmpty() && confirmation != password && (confirmationLeft && !confirmationFocused || confirmation.length >= password.length)
    val transformation = if (shown) VisualTransformation.None else PasswordVisualTransformation()
    val showToggle: @Composable (String) -> Unit = { toggleTag ->
        AppIconButton(
            if (shown) AppIcons.VisibilityOff else AppIcons.Visibility,
            contentDescription = if (shown) tr("Hide password") else tr("Show password"),
            onClick = { shown = !shown }, size = IconButtonSize.Small,
            modifier = Modifier.testTag(toggleTag),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(label, style = AppText.label)
            AppTextField(
                password, onPassword, Modifier.fillMaxWidth().testTag(tag), singleLine = true,
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { showToggle("$tag-toggle") },
            )
            if (password.isNotEmpty()) StrengthBar(PasswordStrength.level(password))
            PasswordRules(password, Modifier.testTag("$tag-rules"))
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr("Confirm password"), style = AppText.label)
            AppTextField(
                confirmation, onConfirmation,
                Modifier.fillMaxWidth().testTag("$tag-confirm").onFocusChanged {
                    if (confirmationFocused && !it.isFocused) confirmationLeft = true
                    confirmationFocused = it.isFocused
                },
                singleLine = true,
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { showToggle("$tag-confirm-toggle") },
                isError = mismatch,
                supportingText = if (mismatch) ({ Text(tr("The passwords don't match"), Modifier.testTag("$tag-mismatch")) }) else null,
            )
        }
    }
}

/** Four segments filling as the password grows stronger, with the level's name after them. */
@Composable
private fun StrengthBar(level: Level) {
    val colors = MaterialTheme.colorScheme
    val tint = level.tint()
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("password-strength"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            repeat(Level.entries.size - 1) { i ->
                val filled = i < level.ordinal.coerceAtLeast(1)
                Box(Modifier.weight(1f).height(AppSpacing.xs).clip(AppShapes.circle).background(if (filled) tint else colors.surfaceVariant))
            }
        }
        Text(level.label(), style = AppText.labelBold, color = tint)
    }
}

/** The rules, each ticked green once the password meets it. */
@Composable
private fun PasswordRules(password: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Rule.entries.forEach { rule ->
            val met = PasswordStrength.meets(password, rule)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                if (met) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconTiny))
                else Box(Modifier.size(AppSizes.iconTiny).border(AppBorders.hairline(colors.outline), AppShapes.circle))
                Text(rule.label(), style = MaterialTheme.typography.bodySmall, color = if (met) StatusTints.ok else colors.onSurfaceVariant)
            }
        }
    }
}

private fun Rule.label(): String = when (this) {
    Rule.LENGTH -> tr("At least {0} characters", PasswordStrength.MIN_LENGTH)
    Rule.DIGIT -> tr("At least one digit")
    Rule.LOWERCASE -> tr("At least one lowercase letter")
    Rule.UPPERCASE -> tr("At least one uppercase letter")
    Rule.SYMBOL -> tr("At least one special character, such as @ # $ %")
}

private fun Level.label(): String = when (this) {
    Level.VERY_WEAK -> tr("Very weak")
    Level.WEAK -> tr("Weak")
    Level.FAIR -> tr("Fair")
    Level.GOOD -> tr("Good")
    Level.STRONG -> tr("Strong")
}

private fun Level.tint(): Color = when (this) {
    Level.VERY_WEAK, Level.WEAK -> AppAccents.red
    Level.FAIR -> AppAccents.amber
    Level.GOOD -> AppAccents.lime
    Level.STRONG -> StatusTints.ok
}
