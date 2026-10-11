package com.tayra.languages.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route

/** The course packs page on a phone: each pack a card with its figures, its size and one full-width button. */
@Composable
internal fun PhoneCoursePacksScreen(
    packs: List<CoursePackStatus>,
    languageName: String?,
    onDownload: (CoursePack) -> Unit,
    onRemove: (CoursePack) -> Unit,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var removing by remember { mutableStateOf<CoursePack?>(null) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Course downloads"), onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Download ready-made courses for offline use."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            InfoBanner(tr("Downloaded packs appear in Courses and can be edited."))
            if (packs.isEmpty() && languageName != null) {
                Text(tr("There are no ready-made courses for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            packs.forEach { status -> PhonePackCard(status, onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) }
        }
    }
    removing?.let { pack ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(tr("Remove the {0} courses?", tr(pack.title))) },
            text = { Text(tr("Their courses and lessons are deleted, with the lesson texts you opened and how far you read them. Courses you made yourself stay. You can download the courses again at any time.")) },
            confirmButton = {
                TextButton(onClick = { onRemove(pack); removing = null }, modifier = Modifier.testTag("confirm-remove")) { Text(tr("Remove"), color = colors.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
private fun PhonePackCard(status: CoursePackStatus, onDownload: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pack = status.pack
    val state = status.state
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(16.dp).testTag("pack-${pack.id}"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(null, pack.languageCode.uppercase(), size = 52)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr(pack.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    when (state) {
                        is PackState.Installed -> StatusPill(tr("Installed"), StatusTints.ok)
                        is PackState.Downloading -> StatusPill(tr("Downloading"), colors.primary)
                        is PackState.Failed -> StatusPill(tr("Download failed"), colors.error)
                        PackState.NotInstalled -> StatusPill(tr("Not downloaded"), colors.outline)
                    }
                }
                Text(tr("Course pack"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        // Every pack is cut the same way: 100 courses of 10 lessons, graded A1 to C2 (see CoursePack.summary).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Figure("100", tr("courses"), Modifier.weight(1f))
            Figure("1,000", tr("lessons"), Modifier.weight(1f))
            Figure("A1–C2", null, Modifier.weight(1f))
        }
        Text(tr("Graded mini stories built on the 10,000 most common words."), style = MaterialTheme.typography.bodyLarge)
        when (state) {
            is PackState.Downloading -> {
                val progress = state.progress
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(tr("Downloading and adding the courses…"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            is PackState.Failed -> Text(packFailure(state.message), style = MaterialTheme.typography.bodyMedium, color = colors.error)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state is PackState.Installed) AppIcons.Storage else AppIcons.Download, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        // No file size when the courses alone are on the device, restored from a backup without the file.
                        state is PackState.Installed && state.sizeBytes > 0 -> tr("{0} on this device", formatSize(state.sizeBytes))
                        state is PackState.Installed -> tr("The courses are on this device")
                        else -> formatSize(pack.downloadSize)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        when (state) {
            is PackState.Installed -> OutlinedButton(onClick = onRemove, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp).testTag("remove-${pack.id}")) {
                Text(tr("Remove course pack"), style = MaterialTheme.typography.titleMedium)
            }
            is PackState.Downloading -> Unit
            else -> Button(onClick = onDownload, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp).testTag("download-${pack.id}")) {
                Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (state is PackState.Failed) tr("Try again") else tr("Download course pack"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** A figure of the pack in a tinted box: the number large, what it counts under it. */
@Composable
private fun Figure(value: String, label: String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainerLow).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (label != null) Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else Spacer(Modifier.height(0.dp))
    }
}
