package com.tayra.languages.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ScreenHeader
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
    if (LocalWindowWidth.current.isCompact) {
        PhoneCoursePacksScreen(packs.filter { it.pack.languageCode == code }, language, onDownload = viewModel::download, onRemove = viewModel::remove, onNavigate = onNavigate, onBack = onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
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
    ScreenHeader(tr("Course packs"), tr("Download the ready-made courses of a language as one file, or remove them to free space."), onBackToSettings = onBack)
    InfoBanner(tr("Downloaded courses appear under Courses when you learn their language. Like the courses you make, they can be changed."))
    if (packs.isEmpty() && languageName != null) {
        Text(tr("There are no ready-made courses for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    packs.forEach { status -> PackCard(status.pack.info(), status.state, coursePackWords(), onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) }
    removing?.let { pack -> RemoveCoursePackDialog(pack, onRemove = { onRemove(pack) }, onDismiss = { removing = null }) }
}

internal fun CoursePack.info() = PackInfo(id, languageCode, title, summary, downloadSize)

internal fun coursePackWords() = PackWords(
    kind = tr("Course pack"),
    downloading = tr("Downloading and adding the courses…"),
    onDevice = tr("The courses are on this device"),
    downloadAction = tr("Download course pack"),
    removeAction = tr("Remove course pack"),
)

@Composable
internal fun RemoveCoursePackDialog(pack: CoursePack, onRemove: () -> Unit, onDismiss: () -> Unit) {
    RemovePackDialog(
        tr("Remove the {0} courses?", tr(pack.title)),
        tr("Their courses and lessons are deleted, with the lesson texts you opened and how far you read them. Courses you made yourself stay. You can download the courses again at any time."),
        onRemove = onRemove,
        onDismiss = onDismiss,
    )
}
