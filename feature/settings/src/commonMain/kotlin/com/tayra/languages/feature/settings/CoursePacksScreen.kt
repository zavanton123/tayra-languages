package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** The downloadable course packs and their state on this device. */
class CoursePacksViewModel(private val service: CoursePackService) : ViewModel() {
    val packs: StateFlow<List<CoursePackStatus>> = service.packs

    fun download(pack: CoursePack) = viewModelScope.launch { service.download(pack) }
    fun remove(pack: CoursePack) = viewModelScope.launch { service.remove(pack) }
}

@Composable
fun CoursePacksScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: CoursePacksViewModel = koinViewModel()) {
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    // Only the pack of the language chosen in the top bar: the others are a tap of that bar away.
    val language = LocalLearningLanguage.current?.currentName
    val code = language?.let { LanguageCodes.codeFor(it) }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            CoursePacksContent(packs.filter { it.pack.languageCode == code }, onDownload = viewModel::download, onRemove = viewModel::remove, onBack = onBack, languageName = language)
        }
    }
}

@Composable
internal fun CoursePacksContent(
    packs: List<CoursePackStatus>,
    onDownload: (CoursePack) -> Unit,
    onRemove: (CoursePack) -> Unit,
    onBack: (() -> Unit)? = null,
    /** The language the packs are for, named when there is none. */
    languageName: String? = null,
) {
    var removing by remember { mutableStateOf<CoursePack?>(null) }
    ScreenHeader(tr("Courses"), tr("Download the ready-made courses of a language as one file, or remove them to free space."), onBackToSettings = onBack)
    InfoBanner(tr("Downloaded courses appear under Courses when you learn their language. Like the courses you make, they can be changed."))
    if (packs.isEmpty() && languageName != null) {
        Text(tr("There are no ready-made courses for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    packs.forEach { status -> PackCard(status, onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) }
    removing?.let { pack ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(tr("Remove the {0} courses?", tr(pack.title))) },
            text = {
                Text(
                    tr("Their courses and lessons are deleted, with the lesson texts you opened and how far you read them. Courses you made yourself stay. You can download the courses again at any time."),
                )
            },
            confirmButton = {
                TextButton(onClick = { onRemove(pack); removing = null }, modifier = Modifier.testTag("confirm-remove")) {
                    Text(tr("Remove"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
private fun PackCard(status: CoursePackStatus, onDownload: () -> Unit, onRemove: () -> Unit) {
    val pack = status.pack
    val state = status.state
    ContentCard(
        tr(pack.title),
        tr("Course pack"),
        iconText = pack.languageCode.uppercase(),
        modifier = Modifier.testTag("pack-${pack.id}"),
        titleExtra = {
            when (state) {
                is PackState.Installed -> StatusPill(tr("Installed"), StatusTints.ok)
                is PackState.Downloading -> StatusPill(tr("Downloading"), MaterialTheme.colorScheme.primary)
                is PackState.Failed -> StatusPill(tr("Download failed"), MaterialTheme.colorScheme.error)
                PackState.NotInstalled -> StatusPill(tr("Not downloaded"), MaterialTheme.colorScheme.outline)
            }
        },
    ) {
        Text(tr(pack.summary), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.padding(top = 12.dp))
        when (state) {
            is PackState.Downloading -> {
                val progress = state.progress
                if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(tr("Downloading and adding the courses…"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            is PackState.Failed -> Text(packFailure(state.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
        Spacer(Modifier.padding(top = 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        // No file size when the courses alone are on the device, restored from a backup without the file.
                        is PackState.Installed -> if (state.sizeBytes > 0) tr("{0} on this device", formatSize(state.sizeBytes)) else tr("The courses are on this device")
                        else -> tr("{0} to download", formatSize(pack.downloadSize))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state) {
                is PackState.Installed -> OutlinedButton(onClick = onRemove, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("remove-${pack.id}")) { Text(tr("Remove")) }
                is PackState.Downloading -> Unit
                else -> Button(onClick = onDownload, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("download-${pack.id}")) {
                    Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state is PackState.Failed) tr("Try again") else tr("Download"))
                }
            }
        }
    }
}
