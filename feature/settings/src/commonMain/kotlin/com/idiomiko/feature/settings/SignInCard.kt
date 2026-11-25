package com.idiomiko.feature.settings

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.onFocusChanged
import com.idiomiko.core.domain.account.EmailAddress
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.account.SignInProvider
import com.idiomiko.core.ui.components.APP_NAME
import com.idiomiko.core.ui.components.AccountSyncIllustration
import com.idiomiko.core.ui.components.AppCard
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTextField
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.GoogleLogo
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * Signed out: what an account gives on a light blue panel, beside the form that signs in, makes an
 * account or sets a new password. A narrower card puts the panel above the form, and a phone keeps
 * only its headline and a small mascot.
 */
@Composable
internal fun SignedOutCard(state: AccountUiState, actions: AccountViewModel?) {
    var creating by rememberSaveable { mutableStateOf(false) }
    // Kept here, so the email is still filled in when a reset is cancelled.
    var email by rememberSaveable { mutableStateOf("") }
    val form: @Composable () -> Unit = {
        if (state.resetSentTo != null) ResetForm(state, actions)
        else SignInForm(state, actions, creating, email, onEmail = { email = it }, onSwitch = { creating = !creating })
    }
    if (LocalWindowWidth.current.isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            PitchBanner()
            AppCard(padding = PaddingValues(AppSpacing.l)) { form() }
        }
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("account-sign-in-card")) {
        val width = maxWidth
        val inner = width - AppSpacing.m * 2
        Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.m)) {
            if (width >= SIDE_BY_SIDE_WIDTH) {
                // A narrower window gives the form its room and the panel the rest, down to a panel without the mascot.
                val panelWidth = inner * if (width >= WIDE_CARD_WIDTH) WIDE_PANEL_SHARE else NARROW_PANEL_SHARE
                val mascot = minOf(MASCOT_SIZE, panelWidth * MASCOT_SHARE).takeIf { panelWidth >= MASCOT_PANEL_WIDTH }
                PanelBesideForm(
                    panelWidth,
                    panel = { PitchPanel(mascot) },
                    form = { Box(Modifier.padding(horizontal = AppSpacing.huge, vertical = AppSpacing.xxxl)) { form() } },
                )
            } else {
                Column {
                    PitchBanner()
                    Box(Modifier.padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xxl)) { form() }
                }
            }
        }
    }
}

/** The panel at [panelWidth] at least as tall as the form beside it, so its mascot sits at the card's foot; the form is centred on a taller panel. */
@Composable
private fun PanelBesideForm(panelWidth: Dp, panel: @Composable () -> Unit, form: @Composable () -> Unit) {
    Layout(contents = listOf(panel, form)) { (panelPart, formPart), constraints ->
        val panelPx = panelWidth.roundToPx()
        val formPx = (constraints.maxWidth - panelPx).coerceAtLeast(0)
        val formPlaced = formPart.first().measure(Constraints.fixedWidth(formPx))
        val panelPlaced = panelPart.first().measure(Constraints(minWidth = panelPx, maxWidth = panelPx, minHeight = formPlaced.height))
        val height = maxOf(formPlaced.height, panelPlaced.height)
        layout(constraints.maxWidth, height) {
            panelPlaced.place(0, 0)
            formPlaced.place(panelPx, (height - formPlaced.height) / 2)
        }
    }
}

/** The light blue wash behind what an account gives. */
@Composable
private fun pitchWash(): Brush = Brush.linearGradient(
    listOf(AppAccents.blue.copy(alpha = AppAlpha.soft), AppAccents.teal.copy(alpha = AppAlpha.wash)),
)

/** The wide card's left half: the headline, what an account gives, and the mascot at the foot when [mascot] gives its size. */
@Composable
private fun PitchPanel(mascot: Dp?) {
    Column(
        Modifier.clip(AppShapes.card).background(pitchWash()).testTag("account-pitch"),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(AppSpacing.xxxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(tr("Your learning, everywhere"), style = AppText.screenTitle)
                Text(tr("Sign in to keep your books, vocabulary, progress and settings in sync."), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Feature(AppIcons.Devices, tr("Continue on any device"))
                Feature(AppIcons.CloudUpload, tr("Keep your vocabulary backed up"))
                Feature(AppIcons.Sync, tr("Pick up exactly where you left off"))
            }
        }
        if (mascot != null) {
            AccountSyncIllustration(Modifier.align(Alignment.End).padding(end = AppSpacing.l, bottom = AppSpacing.l).size(mascot).testTag("account-mascot"))
        }
    }
}

/** The narrow card's and the phone's head: the headline beside a small mascot. */
@Composable
private fun PitchBanner() {
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(pitchWash()).padding(AppSpacing.l).testTag("account-pitch"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr("Your learning, everywhere"), style = AppText.headline)
            Text(tr("Sign in to keep your books, vocabulary, progress and settings in sync."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AccountSyncIllustration(Modifier.size(BANNER_MASCOT_SIZE).testTag("account-mascot"))
    }
}

/** One thing an account gives: its icon on a small white tile, and a line. */
@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Box(Modifier.size(AppSizes.tile).clip(AppShapes.panel).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = AppAccents.onWash(AppAccents.green), modifier = Modifier.size(AppSizes.icon))
        }
        Text(text, style = AppText.emphasis)
    }
}

/** The form's title and the line under it. */
@Composable
private fun FormTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(title, style = if (LocalWindowWidth.current.isCompact) AppText.headline else AppText.screenTitle)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Signing in with an email and password or a provider's account, or, [creating], making an account. */
@Composable
private fun SignInForm(state: AccountUiState, actions: AccountViewModel?, creating: Boolean, email: String, onEmail: (String) -> Unit, onSwitch: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var shown by rememberSaveable { mutableStateOf(false) }
    val busy = state.busy != null
    val emailValid = EmailAddress.isValid(email)
    // A mistyped address is pointed out once the field is left, not while it is being typed.
    var emailFocused by remember { mutableStateOf(false) }
    var emailLeft by rememberSaveable { mutableStateOf(false) }
    val emailWrong = emailLeft && !emailFocused && email.isNotBlank() && !emailValid
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        if (creating) FormTitle(tr("Create your account"))
        else FormTitle(tr("Welcome back"))
        state.problem?.let { ProblemNotice(it, state.problemText) }
        FieldWithLabel(tr("Email")) {
            AppTextField(
                email, onEmail,
                Modifier.fillMaxWidth().testTag("account-email").onFocusChanged {
                    if (emailFocused && !it.isFocused) emailLeft = true
                    emailFocused = it.isFocused
                },
                placeholder = { Text("you@example.com") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailWrong,
                supportingText = if (emailWrong) ({ Text(tr("Enter a valid email address"), Modifier.testTag("account-email-error")) }) else null,
            )
        }
        FieldWithLabel(
            tr("Password"),
            extra = {
                if (!creating) {
                    LinkText(
                        tr("Forgot password?"), onClick = { actions?.forgotPassword(email) }, style = MaterialTheme.typography.bodyMedium,
                        enabled = emailValid && !busy, modifier = Modifier.testTag("account-forgot"),
                    )
                }
            },
        ) {
            AppTextField(
                password, { password = it }, Modifier.fillMaxWidth().testTag("account-password"),
                singleLine = true,
                visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    AppIconButton(
                        if (shown) AppIcons.VisibilityOff else AppIcons.Visibility,
                        contentDescription = if (shown) tr("Hide password") else tr("Show password"),
                        onClick = { shown = !shown }, size = IconButtonSize.Small,
                        modifier = Modifier.testTag("account-password-toggle"),
                    )
                },
                supportingText = if (creating) ({ Text(tr("At least 8 characters")) }) else null,
            )
        }
        PrimaryButton(
            if (creating) tr("Create account") else tr("Sign in"),
            onClick = { if (creating) actions?.register(email, password) else actions?.signIn(email, password) },
            enabled = emailValid && password.isNotEmpty() && !busy,
            loading = state.busy == AccountTask.SIGNING_IN,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth().testTag("account-submit"),
        )
        if (state.signInProviders.isNotEmpty()) {
            OrDivider()
            state.signInProviders.forEach { provider -> ProviderButton(provider, enabled = !busy) { actions?.signInWith(provider) } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (creating) tr("Already have an account?") else tr("New to {0}?", APP_NAME),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(AppSpacing.xxs))
            LinkText(
                if (creating) tr("Sign in") else tr("Create an account"), onClick = onSwitch, style = MaterialTheme.typography.bodyMedium,
                enabled = !busy, modifier = Modifier.testTag("account-switch"),
            )
        }
    }
}

/** A field with its [label] above it, and [extra] (a link) at the label's end. */
@Composable
private fun FieldWithLabel(label: String, extra: @Composable () -> Unit = {}, field: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = AppText.label)
            extra()
        }
        field()
    }
}

/** Hairlines either side of "or", between the email form and the providers. */
@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        AppDivider(Modifier.weight(1f))
        Text(tr("or"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppDivider(Modifier.weight(1f))
    }
}

/** Signing in with Google, under its "G", or with Apple. */
@Composable
private fun ProviderButton(provider: SignInProvider, enabled: Boolean, onClick: () -> Unit) {
    SecondaryButton(onClick, Modifier.fillMaxWidth().testTag("account-${provider.name.lowercase()}"), enabled = enabled, size = ButtonSize.Large) {
        if (provider == SignInProvider.GOOGLE) {
            GoogleLogo(Modifier.size(AppSizes.icon))
            Spacer(Modifier.width(AppSpacing.m))
        }
        Text(if (provider == SignInProvider.GOOGLE) tr("Continue with Google") else tr("Continue with Apple"), style = AppText.emphasis)
    }
}

/** After a reset code was sent: the code and a new password. */
@Composable
private fun ResetForm(state: AccountUiState, actions: AccountViewModel?) {
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        FormTitle(tr("Choose a new password"), tr("We sent a code to {0}. Enter it with your new password.", state.resetSentTo.orEmpty()))
        state.problem?.let { ProblemNotice(it, state.problemText) }
        FieldWithLabel(tr("Code")) {
            AppTextField(code, { code = it }, Modifier.fillMaxWidth().testTag("reset-code"), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        FieldWithLabel(tr("New password")) {
            AppTextField(
                password, { password = it }, Modifier.fillMaxWidth().testTag("reset-password"), singleLine = true,
                visualTransformation = PasswordVisualTransformation(), supportingText = { Text(tr("At least 8 characters")) },
            )
        }
        PrimaryButton(
            tr("Set password and sign in"), onClick = { actions?.resetPassword(code, password) },
            enabled = code.isNotBlank() && password.length >= 8 && state.busy == null, loading = state.busy != null,
            size = ButtonSize.Large, modifier = Modifier.fillMaxWidth().testTag("reset-submit"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LinkText(tr("Cancel"), onClick = { actions?.cancelReset() }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("reset-cancel"))
        }
    }
}

/** The card's width from which the panel stands beside the form, and from which the panel takes its wider share. */
private val SIDE_BY_SIDE_WIDTH = 720.dp
private val WIDE_CARD_WIDTH = 1000.dp
private const val WIDE_PANEL_SHARE = 0.48f
private const val NARROW_PANEL_SHARE = 0.42f

/** The mascot at the panel's foot: at most this big, a share of the panel's width, and left out of a panel narrower than [MASCOT_PANEL_WIDTH]. */
private val MASCOT_SIZE = 300.dp
private const val MASCOT_SHARE = 0.75f
private val MASCOT_PANEL_WIDTH = 320.dp

/** The mascot beside the headline on a phone or a narrow card. */
private val BANNER_MASCOT_SIZE = 96.dp
