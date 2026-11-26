package com.idiomiko.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.account.AccountInfo
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.CardHeader
import com.idiomiko.core.ui.components.CardHeaderSize
import com.idiomiko.core.ui.components.ConfirmDialog
import com.idiomiko.core.ui.components.IconTile
import com.idiomiko.core.ui.components.KeyValueRow
import com.idiomiko.core.ui.components.LocalAccountBadge
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.StatusDot
import com.idiomiko.core.ui.components.StatusPill
import com.idiomiko.core.ui.components.StatusTints
import com.idiomiko.core.ui.components.TintedSurface
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Signed in: the profile beside how syncing stands, of equal height, with deleting the account
 * below them. A phone, or a window too narrow for two cards, stacks them.
 */
@Composable
internal fun SignedInCards(state: AccountUiState, actions: AccountViewModel?) {
    val compact = LocalWindowWidth.current.isCompact
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (compact || maxWidth < SIDE_BY_SIDE_WIDTH) {
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xl)) {
                ProfileCard(state, actions, Modifier)
                SyncCard(state, actions, Modifier)
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                ProfileCard(state, actions, Modifier.weight(1f).fillMaxHeight())
                SyncCard(state, actions, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
    DeleteAccountCard(state, actions)
}

/** A card of the signed-in page: the card surface with the cards' padding, its header and [content] under it. */
@Composable
private fun AccountPart(title: String?, icon: ImageVector?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    Column(modifier.fillMaxWidth().cardSurface().padding(if (compact) AppSpacing.l else AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        if (title != null) CardHeader(title, icon = icon, size = if (compact) CardHeaderSize.Dense else CardHeaderSize.Card)
        content()
    }
}

@Composable
private fun ProfileCard(state: AccountUiState, actions: AccountViewModel?, modifier: Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val account = state.account
    val email = state.signedIn.orEmpty()
    // The bar's avatar has the picture already fetched.
    val badge = LocalAccountBadge.current
    val name = account?.name ?: badge?.name
    AccountPart(null, null, modifier.testTag("account-card")) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            ProfilePicture(
                state, actions, name, email, badge?.picture, if (compact) AVATAR_COMPACT else AVATAR,
                letterStyle = if (compact) AppText.headline else AppText.screenTitle,
            )
            Text(name ?: email, style = AppText.headline, textAlign = TextAlign.Center)
            if (name != null) Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            StatusPill(tr("Signed in"), AppAccents.onWash(StatusTints.ok), Modifier.padding(top = AppSpacing.xs).testTag("account-signed-in"))
        }
        AppDivider()
        signInMethods(account)?.let { KeyValueRow(tr("Signed in with"), it, Modifier.testTag("account-sign-in-methods")) }
        planText(state)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (account != null && !account.emailVerified) ConfirmEmail(state, actions)
        val plan = account?.plan
        if (state.storeAvailable && plan?.active == false) StorePlans(state, actions)
        val uri = LocalUriHandler.current
        if (plan?.checkoutUrl != null || plan?.manageUrl != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                plan.checkoutUrl?.let { url -> PrimaryButton(tr("Subscribe"), onClick = { uri.openUri(url) }, modifier = Modifier.testTag("account-subscribe")) }
                plan.manageUrl?.let { url -> SecondaryButton(tr("Manage subscription"), onClick = { uri.openUri(url) }, modifier = Modifier.testTag("account-manage")) }
            }
        }
        if (plan?.checkoutUrl != null) {
            Text(tr("The checkout opens in your browser. Come back here when it is done."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton(
            tr("Sign out"), onClick = { actions?.signOut() }, icon = Icons.AutoMirrored.Filled.ExitToApp, enabled = state.busy == null,
            modifier = Modifier.fillMaxWidth().testTag("account-sign-out"),
        )
    }
}

/** How the account can be signed in to: its providers, and its email when it has a password; null before it is read. */
@Composable
private fun signInMethods(account: AccountInfo?): String? {
    account ?: return null
    val providers = account.providers.mapNotNull { PROVIDER_NAMES[it.lowercase()] }.distinct()
    return (providers + listOfNotNull(tr("Email").takeIf { account.hasPassword })).takeIf { it.isNotEmpty() }?.joinToString(", ")
}

/** What the sync card says, the most pressing first. */
private enum class SyncShown { SYNCING, FAILED, UNSENT, SYNCED }

private fun AccountUiState.syncShown(): SyncShown = when {
    sync.running || busy == AccountTask.SYNCING -> SyncShown.SYNCING
    sync.problem != null -> SyncShown.FAILED
    unsent -> SyncShown.UNSENT
    else -> SyncShown.SYNCED
}

@Composable
private fun SyncCard(state: AccountUiState, actions: AccountViewModel?, modifier: Modifier) {
    val shown = state.syncShown()
    AccountPart(tr("Sync"), AppIcons.Cloud, modifier.testTag("account-sync-card")) {
        SyncStatusPanel(state, shown)
        Column {
            val words = state.knownWords?.let { n -> trPlural(n, "{0} word", "{0} words").replaceFirst(n.toString(), formatCount(n)) }
            SyncPartRow(AppIcons.Book, tr("Books and courses"), shown, state.unsent)
            SyncPartRow(AppIcons.Translate, tr("Vocabulary"), shown, state.unsent, upToDate = words)
            SyncPartRow(AppIcons.Tune, tr("Progress and settings"), shown, state.unsent)
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton(
            tr("Sync now"), onClick = { actions?.syncNow() }, icon = AppIcons.Sync, enabled = state.busy == null,
            loading = shown == SyncShown.SYNCING, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("account-sync-now"),
        )
    }
}

/** Whether everything is synced, what is not sent yet, the sync under way or why the last one failed, and when it last synced. */
@Composable
private fun SyncStatusPanel(state: AccountUiState, shown: SyncShown) {
    val colors = MaterialTheme.colorScheme
    val tint = when (shown) {
        SyncShown.SYNCED -> StatusTints.ok
        SyncShown.UNSENT -> StatusTints.warning
        SyncShown.FAILED -> colors.error
        SyncShown.SYNCING -> colors.primary
    }
    val (title, message) = when (shown) {
        SyncShown.SYNCED -> tr("Everything is synced") to null
        SyncShown.UNSENT -> tr("Changes not sent yet") to tr("Sync to send what changed on this device.")
        SyncShown.SYNCING -> tr("Syncing…") to (state.sync.progress?.let(::progressLine) ?: tr("Bringing your library up to date."))
        SyncShown.FAILED -> {
            val problem = state.sync.problem!!
            tr("Sync failed") to listOfNotNull(problemTitle(problem, null), problemMessage(problem, null)).joinToString("\n")
        }
    }
    TintedSurface(tint, Modifier.testTag("account-sync-status")) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), verticalAlignment = Alignment.CenterVertically) {
            if (shown == SyncShown.SYNCING) AppSpinner(size = SpinnerSize.Medium, color = tint)
            else Icon(
                when (shown) {
                    SyncShown.SYNCED -> Icons.Default.CheckCircle
                    SyncShown.UNSENT -> AppIcons.CloudUpload
                    else -> AppIcons.CloudOff
                },
                contentDescription = null, tint = AppAccents.onWash(tint), modifier = Modifier.size(AppSizes.iconXLarge),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(title, style = AppText.itemTitle)
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                val synced = state.sync.lastSynced
                if (synced != null && shown != SyncShown.SYNCING) {
                    // The time since the last sync moves on while nothing else changes.
                    val now by produceState(Clock.System.now(), synced) {
                        while (true) {
                            value = Clock.System.now()
                            delay(TICK)
                        }
                    }
                    Row(Modifier.padding(top = AppSpacing.xs), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(tint, size = AppSizes.dot)
                        Text(tr("Last synced {0}", synced.relativeTo(now)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.testTag("account-last-synced"))
                    }
                }
            }
        }
    }
}

/**
 * A part of the library and whether it is sent: there is one flag for all of it, so every part says the same.
 * [upToDate] says more than "Up to date" where it can, as the vocabulary's word count.
 */
@Composable
private fun SyncPartRow(icon: ImageVector, label: String, shown: SyncShown, unsent: Boolean, upToDate: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Box(Modifier.size(AppSizes.tileSmall).clip(AppShapes.compact).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconCompact))
        }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        when {
            shown == SyncShown.SYNCING -> PartState(null, tr("Syncing…"), colors.onSurfaceVariant)
            unsent -> PartState(null, tr("Not sent yet"), AppAccents.onWash(StatusTints.warning))
            else -> PartState(Icons.Default.Check, upToDate ?: tr("Up to date"), AppAccents.onWash(StatusTints.ok))
        }
    }
}

@Composable
private fun PartState(icon: ImageVector?, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(AppSizes.iconSmall))
        Text(text, style = AppText.label, color = color)
    }
}

@Composable
private fun DeleteAccountCard(state: AccountUiState, actions: AccountViewModel?) {
    var confirming by remember { mutableStateOf(false) }
    val compact = LocalWindowWidth.current.isCompact
    val red = MaterialTheme.colorScheme.error
    val button: @Composable (Modifier) -> Unit = { modifier ->
        SecondaryButton(tr("Delete"), onClick = { confirming = true }, enabled = state.busy == null, tone = ButtonTone.Danger, modifier = modifier.testTag("account-delete"))
    }
    TintedSurface(red, Modifier.testTag("account-delete-card"), shape = AppShapes.card, padding = PaddingValues(if (compact) AppSpacing.l else AppSpacing.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(Icons.Default.Warning, size = if (compact) DELETE_TILE_COMPACT else DELETE_TILE, tint = red)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Delete account"), style = AppText.sectionTitle)
                Text(tr("This action cannot be undone."), style = AppText.label, color = red)
            }
            if (!compact) button(Modifier)
        }
        if (compact) button(Modifier.fillMaxWidth())
    }
    if (confirming) {
        ConfirmDialog(
            title = tr("Delete your account?"),
            text = tr("Your account and everything kept with it are deleted for good. This device is signed out; its library stays."),
            confirmLabel = tr("Delete account"),
            destructive = true,
            onConfirm = { confirming = false; actions?.deleteAccount() },
            onDismiss = { confirming = false },
        )
    }
}

/** The providers' names, which stay as the brands write them. */
private val PROVIDER_NAMES = mapOf("google" to "Google", "apple" to "Apple")

/** The page's width from which the profile and the sync card stand side by side. */
private val SIDE_BY_SIDE_WIDTH = 760.dp

/** The profile's picture on a wide window and on a phone. */
private val AVATAR = 96.dp
private val AVATAR_COMPACT = 72.dp

/** The size of the delete card's warning tile, on a wide window and on a phone. */
private const val DELETE_TILE = 48
private const val DELETE_TILE_COMPACT = 40

private val TICK = 30.seconds
