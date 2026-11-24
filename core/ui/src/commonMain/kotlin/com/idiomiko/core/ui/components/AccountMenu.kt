package com.idiomiko.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * The end of the wide bar: the avatar when signed in, a person in a circle when not, opening the account menu
 * with the account, the settings, the backups and About, and signing out when signed in.
 */
@Composable
internal fun AccountMenuButton(badge: AccountBadge?, onNavigate: (Route) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = tr("Account and settings")
    // The menu anchors to its enclosing composable.
    Box {
        AppTooltip(badge?.let { it.name ?: it.email } ?: label) {
            Box(
                Modifier.clip(AppShapes.circle).clickable(onClickLabel = label, role = Role.Button) { open = true }
                    .padding(AppSpacing.xs).testTag(if (badge != null) "account-avatar" else "account-button").semantics { contentDescription = label },
            ) {
                if (badge != null) AccountAvatar(badge.name, badge.email, badge.picture, AppSizes.iconXLarge)
                else NoAvatar(AppSizes.iconXLarge)
            }
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("account-menu")) {
            val go: (Route) -> Unit = { open = false; onNavigate(it) }
            MenuHeader(badge)
            AppDivider()
            if (badge != null) AppMenuItem(text = { Text(tr("Account")) }, onClick = { open = false; badge.open() })
            else AppMenuItem(text = { Text(tr("Sign in")) }, onClick = { go(Route.Account) })
            AppMenuItem(text = { Text(tr("Settings")) }, onClick = { go(Route.Settings) })
            AppMenuItem(text = { Text(tr("About")) }, onClick = { go(Route.About) })
            if (badge != null) {
                AppDivider()
                // As the Account page's Sign out, which asks nothing first either.
                AppMenuItem(
                    text = { Text(tr("Sign out"), color = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; badge.signOut() },
                    modifier = Modifier.testTag("account-menu-sign-out"),
                )
            }
        }
    }
}

/** Who is signed in, at the top of the menu: the avatar, the name and the email; or that no one is. */
@Composable
private fun MenuHeader(badge: AccountBadge?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.widthIn(max = HEADER_MAX_WIDTH).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        if (badge == null) {
            NoAvatar(AppSizes.tile)
            Text(tr("Not signed in"), style = AppText.labelStrong, color = colors.onSurfaceVariant)
            return@Row
        }
        AccountAvatar(badge.name, badge.email, badge.picture, AppSizes.tile, letterStyle = AppText.itemTitleBold)
        Column {
            badge.name?.takeIf { it.isNotBlank() }?.let { Text(it, style = AppText.labelBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(badge.email, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A person in a circle, where an avatar would be while no one is signed in. */
@Composable
internal fun NoAvatar(size: Dp) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(AppShapes.circle).background(colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Person, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(size * PERSON_SCALE))
    }
}

/** The person fills this much of its circle, as the letter does on an avatar. */
private const val PERSON_SCALE = 0.6f

/** A long name or email ends in an ellipsis rather than widening the menu past this. */
private val HEADER_MAX_WIDTH = 320.dp
