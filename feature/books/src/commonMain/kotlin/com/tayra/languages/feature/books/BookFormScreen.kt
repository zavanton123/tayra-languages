package com.tayra.languages.feature.books

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.BarSurface
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.StatusTints
import com.tayra.languages.core.ui.components.TagInput
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.files.acceptsDroppedFiles
import com.tayra.languages.core.ui.files.rememberCameraCapture
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class ContentSource(val label: String) { PASTE("Paste text"), FILE("Import file"), WEB("Web page") }

/**
 * Creates a book from pasted text, a file or a web page's article, or edits a book: the same form, with the book's
 * whole text, title and tags filled in and no file import. Changing the text or the words per
 * page rebuilds the pages on save.
 */
@Composable
fun BookFormScreen(
    bookId: Long?,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: (bookId: Long, isNew: Boolean) -> Unit,
    viewModel: BookFormViewModel = koinViewModel(key = "book-form-$bookId") { parametersOf(bookId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { if (it is BookFormEvent.Saved) onSaved(it.bookId, it.isNew) }
    var source by remember { mutableStateOf(ContentSource.PASTE) }
    val scope = rememberCoroutineScope()
    // Picked or dropped, files are read the same way.
    val importFiles: (List<PlatformFile>) -> Unit = { files ->
        if (files.isNotEmpty()) scope.launch {
            viewModel.importFiles(files.map { it.name to it.readBytes() })
            // The imported text is shown where it can be checked and edited.
            source = ContentSource.PASTE
        }
    }
    val filePicker = rememberFilePickerLauncher(type = FileKitType.File(FileTextExtractor.pickerExtensions), mode = FileKitMode.Multiple()) { files ->
        files?.let(importFiles)
    }
    val camera = rememberCameraCapture { name, bytes ->
        viewModel.importPhotos(listOf(name to bytes))
        source = ContentSource.PASTE
    }.takeIf { FileTextExtractor.readsPhotos }
    val wide = LocalWindowWidth.current.isExpanded

    Scaffold(
        topBar = { AppTopBar(title = if (bookId == null) tr("New book") else tr("Edit book"), onNavigate = onNavigate, section = NavSection.BOOKS, onBack = onBack) },
        bottomBar = { if (!state.loading) ActionBar(state, onCancel = onBack, onSave = viewModel::save) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val languageName = state.language?.name
        PageColumn(padding) {
            ScreenHeader(
                if (state.isNew) tr("Create new book") else tr("Edit book"),
                when {
                    !state.isNew -> state.loaded?.title?.ifBlank { null }
                        ?.let { tr("Change the text, title, tags and pages of “{0}”.", it) }
                        ?: tr("Change the text, title, tags and pages of this book.")
                    languageName != null -> tr("Add a {0} text to your library.", languageInSentence(languageName, LanguageCase.PREPOSITIONAL))
                    else -> tr("Add a text to your library.")
                },
            )
            state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
            state.notice?.let { InfoBanner(it, tint = StatusTints.ok) }
            state.progress?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.primary) }
            state.ocrOffer?.let { OcrDownloadDialog(it, onDownload = viewModel::downloadRecognition, onCancel = viewModel::cancelPhotos) }
            when {
                wide -> Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    BookContentCard(state, source, onSource = { source = it }, onChooseFile = { filePicker.launch() }, importFiles, camera, viewModel, Modifier.weight(1.65f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        DetailsCard(state, viewModel)
                        PageSetupCard(state, viewModel)
                    }
                }
                else -> {
                    BookContentCard(state, source, onSource = { source = it }, onChooseFile = { filePicker.launch() }, importFiles, camera, viewModel)
                    DetailsCard(state, viewModel)
                    PageSetupCard(state, viewModel)
                }
            }
        }
    }
}

/** The text of the new book: typed or pasted, or read from a file. */
@Composable
private fun BookContentCard(
    state: BookFormUiState,
    source: ContentSource,
    onSource: (ContentSource) -> Unit,
    onChooseFile: () -> Unit,
    onDropFiles: (List<PlatformFile>) -> Unit,
    onTakePhoto: (() -> Unit)?,
    viewModel: BookFormViewModel,
    modifier: Modifier = Modifier,
) {
    val formats = FileTextExtractor.formatNames.joinToString(" · ")
    val rtl = state.language?.rightToLeft == true
    val subtitle = if (state.isNew) tr("Add the text for your book.") else tr("Edit the text of your book.")
    ContentCard(tr("Book content"), subtitle, icon = AppIcons.FileOutline, modifier = modifier) {
        // A file can only start a book; an existing book's text is edited in place.
        if (state.isNew) {
            // The formats are many: beside the toggle where there is room, under it on a phone; only while a file is the source.
            val showFormats = source == ContentSource.FILE
            if (LocalWindowWidth.current.isCompact) {
                SourceToggle(source, onSource, state.readsWebPages)
                if (showFormats) {
                    Spacer(Modifier.height(AppSpacing.m))
                    Text(formats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceToggle(source, onSource, state.readsWebPages)
                    Spacer(Modifier.width(AppSpacing.xl))
                    if (showFormats) Text(formats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(AppSpacing.l))
        }
        when (if (state.isNew) source else ContentSource.PASTE) {
            ContentSource.PASTE -> {
                FieldLabel(tr("Text"))
                AppTextField(
                    value = state.draft.text,
                    onValueChange = { text -> viewModel.update { it.copy(text = text) } },
                    placeholder = { Text(state.language?.name?.let { tr("Paste your {0} text here…", languageInSentence(it, LanguageCase.PREPOSITIONAL)) } ?: tr("Paste your text here…")) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth().height(CONTENT_HEIGHT),
                )
                Spacer(Modifier.height(AppSpacing.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("Use a line containing only --- to force a page break."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val words = wordCount(state.draft.text)
                    Text(trPlural(words, "{0} word", "{0} words"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ContentSource.FILE -> FileDrop(state, formats, onChooseFile, onDropFiles, onTakePhoto)
            ContentSource.WEB -> WebPageImport(state, onRead = { address -> viewModel.importWebPage(address) { onSource(ContentSource.PASTE) } }, onPaste = { onSource(ContentSource.PASTE) })
        }
    }
}

/** The number of words in [text], counted at spaces; a rough count for scripts written without them. */
private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }

@Composable
private fun SourceToggle(selected: ContentSource, onSelect: (ContentSource) -> Unit, webPages: Boolean) {
    // Three choices share a phone's width only without their icons.
    val compact = LocalWindowWidth.current.isCompact && webPages
    SegmentedToggle(
        ContentSource.entries.filter { it != ContentSource.WEB || webPages }.map { option ->
            val icon = when (option) {
                ContentSource.PASTE -> AppIcons.FileOutline
                ContentSource.FILE -> AppIcons.UploadFile
                ContentSource.WEB -> AppIcons.Globe
            }
            Segment(option, icon.takeUnless { compact }, tr(option.label), testTag = "source-${option.name}")
        },
        selected,
        onSelect,
        fill = compact,
    )
}

/** A web page's address, whose article is read into the text: without the menus, ads and comments around it. */
@Composable
private fun WebPageImport(state: BookFormUiState, onRead: (String) -> Unit, onPaste: () -> Unit) {
    var address by rememberSaveable { mutableStateOf(state.draft.sourceUri) }
    val read = { if (address.isNotBlank() && !state.busy) onRead(address) }
    val field: @Composable (Modifier) -> Unit = { modifier ->
        AppTextField(
            value = address,
            onValueChange = { address = it },
            placeholder = { Text("https://") },
            leadingIcon = { Icon(AppIcons.Link, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { read() }),
            modifier = modifier.testTag("web-address"),
        )
    }
    val button: @Composable (Modifier) -> Unit = { modifier ->
        PrimaryButton(tr("Get text"), onClick = read, modifier = modifier.testTag("web-read"), icon = AppIcons.Download, enabled = address.isNotBlank(), loading = state.busy)
    }
    Column(Modifier.fillMaxWidth().heightIn(min = CONTENT_HEIGHT)) {
        FieldLabel(tr("Page address"))
        if (LocalWindowWidth.current.isCompact) {
            field(Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppSpacing.m))
            button(Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                field(Modifier.weight(1f))
                button(Modifier)
            }
        }
        // A failure is told by the address it is about, with the way round it.
        state.webError?.let { message ->
            Spacer(Modifier.height(AppSpacing.s))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("web-error"))
            QuietButton(tr("Paste the text instead"), onClick = onPaste)
        }
        Spacer(Modifier.height(AppSpacing.m))
        Text(
            tr("The article's text, without the menus and ads around it, appears under Paste text, ready to check."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileDrop(state: BookFormUiState, formats: String, onChooseFile: () -> Unit, onDropFiles: (List<PlatformFile>) -> Unit, onTakePhoto: (() -> Unit)?) {
    // Files dragged in from the desktop are read like picked ones; the area lights up meanwhile.
    var dropping by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().height(CONTENT_HEIGHT).clip(AppShapes.panel)
            .background(if (dropping) colors.primary.copy(alpha = AppAlpha.wash) else colors.surfaceContainerLow)
            .border(if (dropping) AppBorders.selected() else AppBorders.hairline(), AppShapes.panel)
            .acceptsDroppedFiles(enabled = !state.busy, onHover = { dropping = it }, onFiles = onDropFiles)
            .clickable(enabled = !state.busy, onClick = onChooseFile)
            .padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(AppIcons.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconHuge))
        Spacer(Modifier.height(AppSpacing.l))
        Text(if (dropping) tr("Drop the files to read their text.") else tr("Choose a file to read its text"), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            tr("{0} — the text appears under Paste text, ready to check.", formats),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (FileTextExtractor.readsPhotos) {
            Text(
                tr("Photos of pages (JPG, PNG, HEIC) are read as text in the book's language; several photos make one text."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (acceptsDroppedFiles) {
            Text(tr("Or drop files here."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(AppSpacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            PrimaryButton(if (state.busy) tr("Reading…") else tr("Choose file"), onClick = onChooseFile, enabled = !state.busy)
            onTakePhoto?.let { take -> SecondaryButton(tr("Take photo"), onClick = take, enabled = !state.busy) }
        }
        state.importedFileName?.let {
            Spacer(Modifier.height(AppSpacing.m))
            Text(tr("Last imported: {0}", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailsCard(state: BookFormUiState, viewModel: BookFormViewModel, modifier: Modifier = Modifier) {
    val draft = state.draft
    val rtl = state.language?.rightToLeft == true
    ContentCard(tr("Book details"), tr("Give your book a title and optional tags."), icon = AppIcons.MenuBook, modifier = modifier) {
        // A new book is in the language being learned; the field is only for the case that none is chosen.
        if (state.isNew && draft.languageId == 0L) {
            FieldLabel(tr("Language"))
            Dropdown(
                options = state.languages,
                selected = state.language,
                onSelect = { language -> viewModel.update { it.copy(languageId = language.id) } },
                label = null,
                optionLabel = { tr(it.name) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.l))
        }
        FieldLabel(tr("Title"))
        AppTextField(
            value = draft.title,
            onValueChange = { title -> viewModel.update { it.copy(title = title) } },
            placeholder = { Text(tr("Enter a title")) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppSpacing.l))
        FieldLabel(tr("Tags"))
        TagInput(
            values = draft.tags,
            onValuesChange = { tags -> viewModel.update { it.copy(tags = tags) } },
            label = tr("Add tags"),
            suggestions = state.tagSuggestions,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            tr("Press Enter or a comma after each tag."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AppSpacing.xs),
        )
    }
}

@Composable
private fun PageSetupCard(state: BookFormUiState, viewModel: BookFormViewModel) {
    val draft = state.draft
    ContentCard(tr("Page setup"), tr("Control how your book is split into pages."), icon = Icons.Default.Settings) {
        FieldLabel(tr("Words per page"))
        AppTextField(
            value = draft.wordsPerPage.toString(),
            onValueChange = { v -> v.filter { it.isDigit() }.take(4).toIntOrNull()?.let { n -> viewModel.update { it.copy(wordsPerPage = n) } } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            tr("A page ends at the end of a sentence once it passes this many words. Maximum {0}.", BookDraft.MAX_WORDS_PER_PAGE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AppSpacing.xs),
        )
        Spacer(Modifier.height(AppSpacing.l))
        InfoBanner(
            when {
                state.isNew -> tr("Pages will be created automatically when you save.")
                state.rebuildsPages -> tr("Saving rebuilds the pages. Your place, bookmarks and read pages carry over.")
                else -> trPlural(
                    state.pageCount,
                    "The book has {0} page. Change the text or the words per page to rebuild them.",
                    "The book has {0} pages. Change the text or the words per page to rebuild them.",
                )
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    // As LabeledField draws it, so the book, course and lesson forms label their fields alike.
    Text(text, style = AppText.label, modifier = Modifier.padding(bottom = AppSpacing.s))
}

/** Cancel and save, kept in view at the bottom of the window. */
@Composable
private fun ActionBar(state: BookFormUiState, onCancel: () -> Unit, onSave: () -> Unit) {
    BarSurface {
        Column {
            AppDivider()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.widthIn(max = AppSizes.pageWidth).fillMaxWidth()
                        .padding(horizontal = if (LocalWindowWidth.current.isCompact) AppSpacing.l else AppSpacing.giant, vertical = AppSpacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(onClick = onCancel) { Text(tr("Cancel"), Modifier.padding(horizontal = AppSpacing.m)) }
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(onClick = onSave, enabled = !state.busy) {
                        Text(if (state.isNew) tr("Create book") else tr("Save changes"), Modifier.padding(horizontal = AppSpacing.m))
                    }
                }
            }
        }
    }
}

/** Asks before downloading the model that reading the language from photos needs. */
@Composable
private fun OcrDownloadDialog(offer: OcrOffer, onDownload: () -> Unit, onCancel: () -> Unit) {
    AppDialog(
        onDismissRequest = onCancel,
        title = { Text(tr("Download text recognition?")) },
        text = {
            Text(
                offer.sizeBytes?.let { tr("Reading {0} from photos needs its recognition model ({1}). It is downloaded once and kept on this device.", languageInSentence(offer.languageName, LanguageCase.PREPOSITIONAL), formatSize(it)) }
                    ?: tr("Reading {0} from photos needs a recognition model, which Google Play downloads once.", languageInSentence(offer.languageName, LanguageCase.PREPOSITIONAL)),
            )
        },
        confirmButton = { QuietButton(tr("Download"), onClick = onDownload) },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onCancel) },
    )
}

private val CONTENT_HEIGHT = 400.dp
