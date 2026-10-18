package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.service.AppUpdate
import com.tayra.languages.core.domain.service.AppUpdateService
import com.tayra.languages.core.domain.service.UpdateState
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

/**
 * The offer of a new release across the top of the desktop app, from the check it makes when it
 * starts: install it and restart, see what is new, put it off, or skip the version. Shows the
 * download and the install as they happen, and what went wrong. Nothing on other platforms.
 */
@Composable
fun AppUpdateBanner(service: AppUpdateService? = getKoin().getOrNull()) {
    if (service == null) return
    LaunchedEffect(service) { service.checkOnStart() }
    val state by service.state.collectAsStateWithLifecycle()
    val dismissed by service.dismissed.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val update = when (val s = state) {
        is UpdateState.Available -> s.update
        is UpdateState.Downloading -> s.update
        is UpdateState.Installing -> s.update
        is UpdateState.Failed -> s.update
        else -> null
    }
    if (update == null || dismissed && (state is UpdateState.Available || state is UpdateState.Failed)) return
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().background(colors.primaryContainer).padding(horizontal = 20.dp, vertical = 10.dp).testTag("update-banner"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        UpdateProgress(state, service, update, onInstall = { scope.launch { service.install() } }, onSkip = { scope.launch { service.skip(update) } }, inBanner = true)
    }
}

/** The app's version and its updates, in the desktop app's settings. */
@Composable
internal fun AppUpdatesCard(service: AppUpdateService, settings: UserSettings, onCheckAtStart: (Boolean) -> Unit) {
    val state by service.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val version = service.currentVersion
    ContentCard(tr("App updates"), tr("New versions of Tayra Languages, installed from its GitHub releases."), icon = AppIcons.Download) {
        Text(
            if (version != null) tr("Version {0}", version) else tr("A run from the sources; update it with git."),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (version != null) {
            SwitchSetting(tr("Check for updates when the app starts"), tr("At most once a day."), settings.checkForUpdates, onChange = onCheckAtStart)
            when (val s = state) {
                UpdateState.Idle -> Unit
                UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(tr("Checking for updates…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                is UpdateState.UpToDate -> StatusPill(tr("You have the latest version"), StatusTints.ok)
                is UpdateState.Available -> UpdateProgress(s, service, s.update, onInstall = { scope.launch { service.install() } }, onSkip = { scope.launch { service.skip(s.update) } }, inBanner = false)
                is UpdateState.Downloading -> UpdateProgress(s, service, s.update, onInstall = {}, onSkip = {}, inBanner = false)
                is UpdateState.Installing -> UpdateProgress(s, service, s.update, onInstall = {}, onSkip = {}, inBanner = false)
                is UpdateState.Failed -> {
                    val failed = s.update
                    if (failed != null) UpdateProgress(s, service, failed, onInstall = { scope.launch { service.install() } }, onSkip = { scope.launch { service.skip(failed) } }, inBanner = false)
                    else Text(tr("Could not check for updates: {0}", s.message), style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
            }
            if (state !is UpdateState.Checking && state !is UpdateState.Downloading && state !is UpdateState.Installing) {
                OutlinedButton(onClick = { scope.launch { service.check() } }, modifier = Modifier.testTag("check-updates")) { Text(tr("Check for updates")) }
            }
        }
    }
}

/** What there is to do about [update] in [state]: its offer, its download, its install, or what went wrong. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateProgress(state: UpdateState, service: AppUpdateService, update: AppUpdate, onInstall: () -> Unit, onSkip: () -> Unit, inBanner: Boolean) {
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val text = if (inBanner) colors.onPrimaryContainer else colors.onSurface
    when (state) {
        is UpdateState.Downloading -> {
            Text(tr("Downloading Tayra Languages {0}…", update.version), style = MaterialTheme.typography.bodyMedium, color = text)
            val progress = state.progress
            if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        is UpdateState.Installing -> Text(tr("Installing the update; Tayra Languages will open again in a moment."), style = MaterialTheme.typography.bodyMedium, color = text)
        else -> {
            val failed = state as? UpdateState.Failed
            Text(
                if (failed != null) tr("Could not install Tayra Languages {0}: {1}", update.version, failed.message)
                else listOfNotNull(tr("Tayra Languages {0} is available.", update.version), service.currentVersion?.let { tr("You have {0}.", it) }).joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (failed != null) colors.error else text,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (update.installer != null) {
                    Button(onClick = onInstall, modifier = Modifier.testTag("install-update")) {
                        Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (failed != null) tr("Try again") else tr("Install and restart"))
                    }
                } else {
                    Button(onClick = { uri.openUri(update.pageUrl) }) { Text(tr("Download")) }
                }
                if (update.pageUrl.isNotEmpty()) TextButton(onClick = { uri.openUri(update.pageUrl) }) { Text(tr("What's new")) }
                if (inBanner) TextButton(onClick = service::later, modifier = Modifier.testTag("update-later")) { Text(tr("Later")) }
                if (failed == null) TextButton(onClick = onSkip) { Text(tr("Skip this version")) }
            }
        }
    }
}
