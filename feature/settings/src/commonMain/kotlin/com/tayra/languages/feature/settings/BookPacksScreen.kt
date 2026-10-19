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
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStatus
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

/** The downloadable book packs and their state on this device. */
class BookPacksViewModel(private val service: BookPackService) : ViewModel() {
    val packs: StateFlow<List<BookPackStatus>> = service.packs

    fun download(pack: BookPack) = viewModelScope.launch { service.download(pack) }
    fun remove(pack: BookPack) = viewModelScope.launch { service.remove(pack) }
}

@Composable
fun BookPacksScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: BookPacksViewModel = koinViewModel()) {
    val packs by viewModel.packs.collectAsStateWithLifecycle()
    // Only the pack of the language chosen in the top bar: the others are a tap of that bar away.
    val language = LocalLearningLanguage.current?.currentName
    val code = language?.let { LanguageCodes.codeFor(it) }
    if (LocalWindowWidth.current.isCompact) {
        PhoneBookPacksScreen(packs.filter { it.pack.languageCode == code }, language, onDownload = viewModel::download, onRemove = viewModel::remove, onNavigate = onNavigate, onBack = onBack)
        return
    }
    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) {
            BookPacksContent(packs.filter { it.pack.languageCode == code }, onDownload = viewModel::download, onRemove = viewModel::remove, onBack = onBack, languageName = language)
        }
    }
}

@Composable
internal fun BookPacksContent(
    packs: List<BookPackStatus>,
    onDownload: (BookPack) -> Unit,
    onRemove: (BookPack) -> Unit,
    onBack: (() -> Unit)? = null,
    /** The language the packs are for, named when there is none. */
    languageName: String? = null,
) {
    var removing by remember { mutableStateOf<BookPack?>(null) }
    ScreenHeader(tr("Books"), tr("Download a bundle of free books in a language as one file, or remove them to free space."), onBackToSettings = onBack)
    InfoBanner(tr("Downloaded books appear under Books when you learn their language, graded from A2 to C2. Like the books you add, they can be edited or archived."))
    if (packs.isEmpty() && languageName != null) {
        Text(tr("There is no book bundle for {0} yet.", tr(languageName)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    packs.forEach { status -> PackCard(status.pack.info(), status.state, bookPackWords(), onDownload = { onDownload(status.pack) }, onRemove = { removing = status.pack }) }
    removing?.let { pack -> RemoveBookPackDialog(pack, onRemove = { onRemove(pack) }, onDismiss = { removing = null }) }
}

internal fun BookPack.info() = PackInfo(id, languageCode, title, summary, downloadSize)

internal fun bookPackWords() = PackWords(
    kind = tr("Book bundle"),
    downloading = tr("Downloading and adding the books…"),
    onDevice = tr("The books are on this device"),
    downloadAction = tr("Download book bundle"),
    removeAction = tr("Remove book bundle"),
)

@Composable
internal fun RemoveBookPackDialog(pack: BookPack, onRemove: () -> Unit, onDismiss: () -> Unit) {
    RemovePackDialog(
        tr("Remove the {0} books?", tr(pack.title)),
        tr("The bundle's books are deleted, with how far you read them and your bookmarks in them. Books you added yourself stay. You can download the bundle again at any time."),
        onRemove = onRemove,
        onDismiss = onDismiss,
    )
}
