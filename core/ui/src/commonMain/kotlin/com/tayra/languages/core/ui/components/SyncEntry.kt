package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tayra.languages.core.ui.i18n.tr

/** Syncing from anywhere in the app: whether a sync is under way, and asking whether to sync now. */
class SyncEntry(val running: Boolean, val open: () -> Unit)

/** Provided at the root of the app; null where the build has no account service. */
val LocalSyncEntry = compositionLocalOf<SyncEntry?> { null }

/** The wide bar's sync button, turning while a sync is under way. */
@Composable
internal fun SyncButton(entry: SyncEntry) {
    val label = tr("Sync")
    AppTooltip(label) {
        AppIconButton(
            Icons.Default.Refresh, contentDescription = null, onClick = entry.open, loading = entry.running,
            modifier = Modifier.testTag("sync-button").semantics { contentDescription = label },
        )
    }
}

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
