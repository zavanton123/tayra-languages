package com.idiomiko.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Syncing from anywhere in the app: whether a sync is under way, when this device last synced since the
 * app started, whether the last sync failed, and asking whether to sync now.
 */
class SyncEntry(val running: Boolean, val lastSynced: Instant? = null, val failed: Boolean = false, val open: () -> Unit)

/** Provided at the root of the app; null where the build has no account service. */
val LocalSyncEntry = compositionLocalOf<SyncEntry?> { null }

/**
 * The wide bar's sync status as a single cloud: green once this device has synced, grey while it syncs, before
 * its first sync or after one failed. What it means ("Synced 2 minutes ago", "Syncing…", "Sync failed") is its
 * tooltip and its description; a click asks whether to sync now.
 */
@Composable
internal fun SyncStatusButton(entry: SyncEntry) {
    val colors = MaterialTheme.colorScheme
    // The time since the last sync moves on while nothing else changes.
    val now by produceState(Clock.System.now(), entry.lastSynced) {
        while (true) {
            value = Clock.System.now()
            delay(TICK)
        }
    }
    val synced = entry.lastSynced
    val label = when {
        entry.running -> tr("Syncing…")
        entry.failed -> tr("Sync failed")
        synced != null -> tr("Synced {0}", synced.relativeTo(now))
        else -> tr("Sync")
    }
    val good = synced != null && !entry.running && !entry.failed
    AppTooltip(label) {
        Box(
            Modifier.clip(AppShapes.control).clickable(onClickLabel = tr("Sync now"), role = Role.Button, onClick = entry.open)
                .semantics { contentDescription = label }
                .padding(AppSpacing.s).testTag("sync-button"),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.running) AppSpinner(size = SpinnerSize.Small, color = colors.onSurfaceVariant)
            else Icon(
                when {
                    entry.failed -> AppIcons.CloudOff
                    synced != null -> AppIcons.CloudDone
                    else -> AppIcons.Cloud
                },
                contentDescription = null,
                tint = if (good) AppAccents.green else colors.onSurfaceVariant,
                modifier = Modifier.size(AppSizes.icon),
            )
        }
    }
}

private val TICK = 30.seconds

/** A phone page pulled down past its top asks whether to sync, through [onPull]; the indicator never stays spinning. */
@Composable
fun PullToSync(enabled: Boolean, onPull: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberPullToRefreshState()
    Box(modifier.pullToRefresh(isRefreshing = false, state = state, enabled = enabled, onRefresh = onPull)) {
        content()
        if (enabled) {
            PullToRefreshDefaults.Indicator(state, isRefreshing = false, modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars))
        }
    }
}
