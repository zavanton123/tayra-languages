package com.tayra.languages.feature.settings

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.practice.RecognitionQuality
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import com.tayra.languages.core.domain.service.SetupStatus
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/** What the Downloads page can do, apart from the view model so the page can be shown on its own. */
internal class DownloadActions(
    val downloadCourses: (CoursePack) -> Unit = {},
    val removeCourses: (CoursePack) -> Unit = {},
    val downloadBooks: (BookPack) -> Unit = {},
    val removeBooks: (BookPack) -> Unit = {},
    val downloadDictionary: (DictionaryPack) -> Unit = {},
    val removeDictionary: (DictionaryPack) -> Unit = {},
    val install: (SetupItem) -> Unit = {},
    val canRemove: (SetupItem) -> Boolean = { false },
    val remove: (SetupItem) -> Unit = {},
    val downloadOcr: () -> Unit = {},
    val removeOcr: (OcrPack) -> Unit = {},
)

/** The Downloads page: everything that can be downloaded for the language being learned, in one place. */
@Composable
fun DownloadsScreen(onNavigate: (Route) -> Unit, viewModel: DownloadsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        DownloadActions(
            downloadCourses = { viewModel.download(it) },
            removeCourses = { viewModel.remove(it) },
            downloadBooks = { viewModel.download(it) },
            removeBooks = { viewModel.remove(it) },
            downloadDictionary = { viewModel.download(it) },
            removeDictionary = { viewModel.remove(it) },
            install = viewModel::install,
            canRemove = viewModel::canRemove,
            remove = { viewModel.remove(it) },
            downloadOcr = viewModel::downloadOcr,
            removeOcr = { viewModel.removeOcr(it) },
        )
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Downloads"), onNavigate = onNavigate, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PageColumn(padding) { DownloadsContent(state, progress, actions, onNavigate) }
    }
}

/** A removal waiting for the learner to confirm it. */
private class Confirmation(val question: String, val consequence: String, val remove: () -> Unit)

@Composable
internal fun ColumnScope.DownloadsContent(state: DownloadsUiState, progress: Map<String, SetupState>, actions: DownloadActions, onNavigate: (Route) -> Unit) {
    var confirming by remember { mutableStateOf<Confirmation?>(null) }
    val language = state.language
    ScreenHeader(
        tr("Downloads"),
        language?.let { tr("Everything you can download for {0}, to use without internet.", tr(it.name)) },
    )
    if (language == null || state.code == null) {
        Text(tr("Choose a language to learn to see what you can download for it."), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Summary(state)
    val confirm: (Confirmation) -> Unit = { confirming = it }
    val left: List<@Composable () -> Unit> = listOf(
        { CoursesSection(state, actions, confirm, onNavigate) },
        { BooksSection(state, actions, confirm, onNavigate) },
        { DictionariesSection(state, actions, confirm, onNavigate) },
    )
    val tools = state.tools
    val right: List<@Composable () -> Unit> = if (tools == null) {
        listOf({ LoadingSection() })
    } else {
        listOfNotNull(
            { VoicesSection(language.name, tools.filter { it.kind == SetupKind.VOICE }, progress, actions, confirm, onNavigate) },
            tools.filter { it.kind == SetupKind.TRANSLATION }.takeIf { it.isNotEmpty() }?.let { items -> { TranslationSection(items, progress, actions, confirm, onNavigate) } },
            tools.filter { it.kind == SetupKind.RECOGNITION }.takeIf { it.isNotEmpty() }?.let { items -> { RecognitionSection(items, progress, actions, confirm) } },
            state.ocr.takeIf { it.readiness is OcrReadiness.NeedsDownload || it.installed.isNotEmpty() || it.downloading }?.let { { TextRecognitionSection(language.name, state.ocr, actions, confirm) } },
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= TWO_COLUMNS) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) { left.forEach { it() } }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) { right.forEach { it() } }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) { (left + right).forEach { it() } }
        }
    }
    confirming?.let { c -> RemovePackDialog(c.question, c.consequence, onRemove = c.remove, onDismiss = { confirming = null }) }
}

/** How many downloads are on the device and the space they take. */
@Composable
private fun Summary(state: DownloadsUiState) {
    val packs = state.courses.map { it.state } + state.books.map { it.state } + state.dictionaries.map { it.state }
    val installedPacks = packs.filterIsInstance<PackState.Installed>()
    val installedTools = state.tools.orEmpty().filter { it.status == SetupStatus.INSTALLED }
    val count = installedPacks.size + installedTools.size + state.ocr.installed.size
    val bytes = installedPacks.sumOf { it.sizeBytes } + installedTools.sumOf { it.onDeviceBytes ?: 0 } + state.ocr.installed.sumOf { it.sizeBytes }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), modifier = Modifier.testTag("downloads-summary")) {
        Icon(AppIcons.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
        Text(
            if (count == 0) tr("Nothing downloaded yet") else tr("{0} downloaded · {1} on this device", count, formatSize(bytes)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CoursesSection(state: DownloadsUiState, actions: DownloadActions, confirm: (Confirmation) -> Unit, onNavigate: (Route) -> Unit) {
    Section(tr("Courses"), tr("Graded courses of mini stories, ready to read and listen to."), AppIcons.School, link = tr("Go to Courses") to { onNavigate(Route.Courses) }) {
        if (state.courses.isEmpty()) NotYet(tr("There are no ready-made courses for {0} yet.", tr(state.language?.name.orEmpty())))
        state.courses.forEachIndexed { i, status ->
            if (i > 0) AppDivider()
            PackRow(tr(status.pack.title), tr("100 graded courses of mini stories, from A1 to C2"), status.state, "courses-${status.pack.id}",
                size = status.pack.downloadSize,
                onDownload = { actions.downloadCourses(status.pack) },
                onRemove = {
                    confirm(Confirmation(
                        tr("Remove the {0} courses?", tr(status.pack.title)),
                        tr("Their courses and lessons are deleted, with the lesson texts you opened and how far you read them. Courses you made yourself stay. You can download the courses again at any time."),
                    ) { actions.removeCourses(status.pack) })
                },
            )
        }
    }
}

@Composable
private fun BooksSection(state: DownloadsUiState, actions: DownloadActions, confirm: (Confirmation) -> Unit, onNavigate: (Route) -> Unit) {
    Section(tr("Books"), tr("Free books graded by level, added to your library."), AppIcons.Book, link = tr("Go to Books") to { onNavigate(Route.Books) }) {
        if (state.books.isEmpty()) NotYet(tr("There is no book pack for {0} yet.", tr(state.language?.name.orEmpty())))
        state.books.forEachIndexed { i, status ->
            if (i > 0) AppDivider()
            PackRow(tr(status.pack.title), tr(status.pack.summary), status.state, "books-${status.pack.id}",
                size = status.pack.downloadSize,
                onDownload = { actions.downloadBooks(status.pack) },
                onRemove = {
                    confirm(Confirmation(
                        tr("Remove the {0} books?", tr(status.pack.title)),
                        tr("The pack's books are deleted, with how far you read them and your bookmarks in them. Books you added yourself stay. You can download the pack again at any time."),
                    ) { actions.removeBooks(status.pack) })
                },
            )
        }
    }
}

@Composable
private fun DictionariesSection(state: DownloadsUiState, actions: DownloadActions, confirm: (Confirmation) -> Unit, onNavigate: (Route) -> Unit) {
    val name = state.language?.name.orEmpty()
    Section(
        tr("Dictionaries"),
        tr("Look up {0} words without internet", languageInSentence(name, LanguageCase.PREPOSITIONAL)),
        AppIcons.MenuBook,
        link = tr("Dictionary settings") to { onNavigate(Route.OfflineDictionaries) },
    ) {
        if (state.dictionaries.isEmpty()) NotYet(tr("Not available for {0} yet", tr(name)))
        state.dictionaries.forEachIndexed { i, status ->
            if (i > 0) AppDivider()
            PackRow(status.pack.displayTitle, null, status.state, "dictionary-${status.pack.id.name}",
                size = status.pack.downloadSize,
                onDownload = { actions.downloadDictionary(status.pack) },
                onRemove = {
                    confirm(Confirmation(
                        tr("Remove {0}?", status.pack.displayTitle),
                        tr("Its words are no longer looked up offline. You can download it again at any time."),
                    ) { actions.removeDictionary(status.pack) })
                },
            )
        }
    }
}

/**
 * The voices the speech engines have for the language: one row a voice, or one row for an
 * engine whose voices share a model, downloaded and removed together.
 */
@Composable
private fun VoicesSection(language: String, voices: List<SetupItem>, progress: Map<String, SetupState>, actions: DownloadActions, confirm: (Confirmation) -> Unit, onNavigate: (Route) -> Unit) {
    Section(tr("Voices"), tr("Hear books and words read aloud"), AppIcons.VolumeUp, link = tr("Speech settings") to { onNavigate(Route.Speech) }) {
        val available = voices.filter { it.status != SetupStatus.UNAVAILABLE }
        if (available.isEmpty()) NotYet(tr("Not available for {0} yet", tr(language)))
        var first = true
        available.groupBy { it.engine.orEmpty() }.forEach { (engine, items) ->
            if (items.first().sharedModel) {
                if (!first) AppDivider()
                first = false
                val installed = items.firstOrNull { it.status == SetupStatus.INSTALLED }
                val fetching = items.firstNotNullOfOrNull { progress[it.id]?.takeIf { state -> state !is SetupState.Done } }
                val chosen = installed ?: items.firstOrNull { it.recommended } ?: items.first()
                ToolRow(
                    tr("{0} voices", engine),
                    items.joinToString(", ") { localizedVoiceName(it.name.substringAfter(" · ")) },
                    chosen.copy(status = if (installed != null) SetupStatus.INSTALLED else chosen.status),
                    fetching ?: progress[chosen.id],
                    "voice-$engine",
                    onDownload = { actions.install(chosen) },
                    onRemove = installed?.takeIf(actions.canRemove)?.let { item ->
                        {
                            confirm(Confirmation(
                                tr("Remove the {0} model?", engine),
                                tr("All its voices are removed with it. You can download them again at any time."),
                            ) { actions.remove(item) })
                        }
                    },
                )
            } else {
                items.forEach { item ->
                    if (!first) AppDivider()
                    first = false
                    ToolRow(
                        localizedVoiceName(item.name.substringAfter(" · ")),
                        engine,
                        item,
                        progress[item.id],
                        "voice-${item.id}",
                        onDownload = { actions.install(item) },
                        onRemove = item.takeIf(actions.canRemove)?.let {
                            { confirm(Confirmation(tr("Remove the voice {0}?", localizedVoiceName(item.name.substringAfter(" · "))), tr("You can download it again at any time.")) { actions.remove(item) }) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationSection(items: List<SetupItem>, progress: Map<String, SetupState>, actions: DownloadActions, confirm: (Confirmation) -> Unit, onNavigate: (Route) -> Unit) {
    Section(tr("Offline translation"), tr("Translate sentences without internet"), AppIcons.Translate, link = tr("Translation settings") to { onNavigate(Route.OfflineTranslation) }) {
        items.forEachIndexed { i, item ->
            if (i > 0) AppDivider()
            ToolRow(
                localizedNames(item.name),
                if (item.switchesEngine && item.status == SetupStatus.MISSING) tr("Becomes your translation engine") else null,
                item,
                progress[item.id],
                "translation",
                onDownload = { actions.install(item) },
                onRemove = item.takeIf(actions.canRemove)?.let {
                    { confirm(Confirmation(tr("Remove the translation models?"), tr("Sentences are no longer translated offline. You can download the models again at any time.")) { actions.remove(item) }) }
                },
            )
        }
    }
}

@Composable
private fun RecognitionSection(items: List<SetupItem>, progress: Map<String, SetupState>, actions: DownloadActions, confirm: (Confirmation) -> Unit) {
    Section(tr("Speech recognition"), tr("Answer practice aloud"), AppIcons.Mic) {
        items.forEachIndexed { i, item ->
            if (i > 0) AppDivider()
            val high = item.name == RecognitionQuality.HIGH.name
            ToolRow(
                if (high) tr("High quality") else tr("Medium quality"),
                if (high) tr("Answer practice aloud: hears a learner's accent best, in a few seconds") else tr("Answer practice aloud: quick, and hears clear speech well"),
                item,
                progress[item.id],
                "recognition-${item.name.lowercase()}",
                onDownload = { actions.install(item) },
                onRemove = item.takeIf(actions.canRemove)?.let {
                    { confirm(Confirmation(tr("Remove this speech recognition model?"), tr("You can download it again at any time.")) { actions.remove(item) }) }
                },
            )
        }
    }
}

@Composable
private fun TextRecognitionSection(language: String, ocr: OcrDownloads, actions: DownloadActions, confirm: (Confirmation) -> Unit) {
    Section(tr("Text recognition"), tr("Read the text of photos of pages"), AppIcons.UploadFile) {
        var first = true
        val needs = ocr.readiness as? OcrReadiness.NeedsDownload
        if (needs != null || ocr.downloading) {
            first = false
            val state = when {
                ocr.downloading -> RowState.Working(ocr.progress, tr("Downloading…"))
                ocr.error != null -> RowState.Failed(downloadFailure(ocr.error))
                else -> RowState.Available(needs?.sizeBytes?.let(::formatSize))
            }
            DownloadRow(tr(language), null, state, "ocr-language", onDownload = actions.downloadOcr, onRemove = null)
        }
        ocr.installed.forEach { pack ->
            if (!first) AppDivider()
            first = false
            DownloadRow(tr(pack.title), null, RowState.Installed(pack.sizeBytes), "ocr-${pack.id}", onDownload = null, onRemove = {
                confirm(Confirmation(tr("Remove {0}?", tr(pack.title)), tr("You can download it again at any time.")) { actions.removeOcr(pack) })
            })
        }
    }
}

@Composable
private fun LoadingSection() {
    Row(Modifier.fillMaxWidth().padding(AppSpacing.xl), horizontalArrangement = Arrangement.Center) { AppSpinner(size = SpinnerSize.Medium) }
}

/** A card of one kind of download, with a way to its settings or to what it brings. */
@Composable
private fun Section(title: String, subtitle: String, icon: ImageVector, link: Pair<String, () -> Unit>? = null, content: @Composable ColumnScope.() -> Unit) {
    ContentCard(title, subtitle, icon = icon, modifier = Modifier.testTag("downloads-$title")) {
        Column(Modifier.fillMaxWidth().padding(top = AppSpacing.s)) { content() }
        link?.let { (text, open) -> LinkText(text, open, modifier = Modifier.padding(top = AppSpacing.m)) }
    }
}

@Composable
private fun NotYet(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = AppSpacing.s))
}

/** Where a download stands, as its row shows it. */
internal sealed interface RowState {
    data class Available(val size: String?) : RowState
    data class Working(val progress: Float?, val label: String) : RowState
    data class Installed(val bytes: Long?) : RowState
    data class Failed(val message: String) : RowState
    data object Unavailable : RowState
}

@Composable
private fun PackRow(title: String, detail: String?, state: PackState, tag: String, onDownload: () -> Unit, onRemove: () -> Unit, size: Long? = null) {
    DownloadRow(
        title,
        detail,
        when (state) {
            PackState.NotInstalled -> RowState.Available(size?.let(::formatSize))
            is PackState.Downloading -> RowState.Working(state.progress, tr("Downloading…"))
            is PackState.Installed -> RowState.Installed(state.sizeBytes.takeIf { it > 0 })
            is PackState.Failed -> RowState.Failed(packFailure(state.message))
        },
        tag,
        onDownload,
        onRemove,
    )
}

@Composable
private fun ToolRow(title: String, detail: String?, item: SetupItem, progress: SetupState?, tag: String, onDownload: () -> Unit, onRemove: (() -> Unit)?) {
    val installed = item.status == SetupStatus.INSTALLED
    val state = when {
        item.status == SetupStatus.UNAVAILABLE -> RowState.Unavailable
        progress is SetupState.Waiting -> RowState.Working(null, tr("Queued"))
        progress is SetupState.Running -> RowState.Working(null, if (installed) tr("Removing…") else tr("Downloading…"))
        progress is SetupState.Failed && !installed -> RowState.Failed(downloadFailure(progress.message))
        installed -> RowState.Installed(item.onDeviceBytes)
        else -> RowState.Available(item.sizeBytes?.let { sizeText(it, item.sizeEstimated) })
    }
    DownloadRow(title, detail, state, tag, onDownload, onRemove)
}

/** One download: what it is, where it stands, and the button to fetch or remove it. */
@Composable
private fun DownloadRow(title: String, detail: String?, state: RowState, tag: String, onDownload: (() -> Unit)?, onRemove: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val about: @Composable ColumnScope.() -> Unit = {
        Text(title, style = AppText.itemTitle)
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        when (state) {
            is RowState.Working -> {
                AppProgressBar(state.progress, Modifier.padding(top = AppSpacing.xs))
                Text(state.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            is RowState.Failed -> Text(state.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
            else -> Unit
        }
    }
    val status: @Composable () -> Unit = {
        when (state) {
            is RowState.Installed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusTints.ok, modifier = Modifier.size(AppSizes.iconSmall))
                Text(state.bytes?.let { tr("{0} on this device", formatSize(it)) } ?: tr("On this device"), style = MaterialTheme.typography.bodyMedium, color = StatusTints.ok)
            }
            is RowState.Available -> state.size?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            RowState.Unavailable -> Text(tr("Not available"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            else -> Unit
        }
    }
    val action: @Composable () -> Unit = {
        when (state) {
            is RowState.Available, is RowState.Failed -> onDownload?.let {
                SecondaryButton(
                    if (state is RowState.Failed) tr("Try again") else tr("Download"),
                    onClick = it,
                    icon = AppIcons.Download,
                    size = ButtonSize.Compact,
                    modifier = Modifier.testTag("download-$tag"),
                )
            }
            is RowState.Installed -> onRemove?.let { QuietButton(tr("Remove"), onClick = it, tone = ButtonTone.Danger, size = ButtonSize.Compact, modifier = Modifier.testTag("remove-$tag")) }
            else -> Unit
        }
    }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.m).testTag("row-$tag"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            about()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) { status() }
                action()
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(vertical = AppSpacing.m).testTag("row-$tag"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) { about() }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                status()
                action()
            }
        }
    }
}

/**
 * Where a settings page sends the learner to download or remove what it uses: the Downloads page
 * holds every download, so the settings pages hold none. [text] says what is found there.
 */
@Composable
internal fun DownloadsLinkCard(text: String, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    ContentCard(tr("Downloads"), text, icon = AppIcons.Download, modifier = modifier.testTag("downloads-link")) {
        SecondaryButton(
            tr("Go to Downloads"),
            onClick = { onNavigate(Route.Downloads) },
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            modifier = Modifier.padding(top = AppSpacing.m).testTag("open-downloads"),
        )
    }
}

/** Asks before a download is removed: [question] names it, [consequence] says what goes with it. */
@Composable
private fun RemovePackDialog(question: String, consequence: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(question) },
        text = { Text(consequence) },
        confirmButton = {
            QuietButton(tr("Remove"), onClick = { onRemove(); onDismiss() }, modifier = Modifier.testTag("confirm-remove"), tone = ButtonTone.Danger)
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}

/** The width from which the page sets its cards side by side. */
private val TWO_COLUMNS = 1000.dp
