package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.CircularProgressIndicator
import com.tayra.languages.feature.terms.examples.PrepareSpeech
import com.tayra.languages.feature.terms.examples.rememberExampleAudio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.language.OnlineDictionaries
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.components.HoverTranslatedText
import org.koin.compose.koinInject
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.theme.TayraTheme
import io.ktor.http.encodeURLParameter

/**
 * The term editing form, used both standalone and embedded in the reading pane.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermFormPanel(
    viewModel: TermFormViewModel,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    onClose: (() -> Unit)? = null,
    /** Called after the term was deleted from the standalone editor. */
    onDuplicateClick: ((Long) -> Unit)? = null,
    onOpenExamples: ((languageId: Long, text: String) -> Unit)? = null,
    onManageDictionaries: ((languageId: Long) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }
    var confirmDelete by remember { mutableStateOf(false) }

    if (state.loading) {
        LoadingIndicator(modifier.heightIn(min = 200.dp))
        return
    }
    val draft = state.draft
    val language = state.language
    val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Enter) {
                    viewModel.save()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (embedded) PanelHeader(language, state.nativeLanguage, onClose)
        state.duplicateOf?.let { duplicate ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Term \"${duplicate.displayText}\" already exists.", color = MaterialTheme.colorScheme.error)
                if (onDuplicateClick != null) TextButton(onClick = { onDuplicateClick(duplicate.id) }) { Text("Open") }
            }
        } ?: ErrorMessage(state.error)

        if (embedded) {
            SectionCard({ TermBadge() }, "Term", tint = MaterialTheme.colorScheme.primary) {
                LanguageSelector(state, viewModel)
                TermField(state, viewModel, direction, focusRequester)
                TranslationField(state, viewModel, compact = true)
                StatusSelector(selected = draft.status, onSelect = viewModel::setStatus, large = true)
            }
        } else {
            StandaloneFields(state, viewModel, direction, focusRequester, onConfirmDelete = { confirmDelete = true })
        }
        val pack = state.dictionaryPack
        when {
            !state.dictionary.isEmpty -> DictionarySection(state.dictionary, onAdd = viewModel::addGloss)
            pack != null && pack.state !is PackState.Installed -> DictionaryDownloadCard(pack, onDownload = viewModel::downloadDictionary)
        }

        if (language != null && draft.text.isNotBlank()) SectionCard({ BadgeIcon(AppIcons.Link) }, "Dictionaries", tint = null) {
            if (language.termDictionaries.isEmpty()) {
                Text("No online dictionaries enabled for ${language.name}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val source = LanguageOption(LanguageCodes.codeFor(language.name) ?: "en", language.name)
            val target = LanguageCatalog.nativeOption(state.nativeLanguage)
            val labels = OnlineDictionaries.labels(language.termDictionaries, source, target)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                language.termDictionaries.forEach { dictionary ->
                    LinkChip(labels[dictionary] ?: OnlineDictionaries.host(dictionary.url)) {
                        uriHandler.openUri(dictionary.lookupUrl(draft.text.replace("​", "").encodeURLParameter()))
                    }
                }
            }
            if (onManageDictionaries != null) {
                OutlineActionButton("Manage dictionaries") { onManageDictionaries(language.id) }
            }
        }

        ExamplesSection(state, language, onOpenExamples)
        if (embedded) Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete \"${draft.text.replace("\u200B", "")}\"?",
            text = "The term and its translation will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.delete(); confirmDelete = false },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun LanguageSelector(state: TermFormUiState, viewModel: TermFormViewModel) {
    if (!state.showLanguageSelector) return
    Dropdown(
        options = state.languages,
        selected = state.language,
        onSelect = { l -> viewModel.update { it.copy(languageId = l.id) } },
        label = "Language",
        optionLabel = { it.name },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TermField(state: TermFormUiState, viewModel: TermFormViewModel, direction: TextDirection, focusRequester: FocusRequester) {
    val language = state.language
    OutlinedTextField(
        value = state.draft.text,
        onValueChange = { text -> viewModel.update { it.copy(text = text) } },
        label = { Text("Term") },
        singleLine = true,
        trailingIcon = { SpeakButton(state.draft.text, language?.let { LanguageCodes.codeFor(it.name) }, rememberSpeaker(koinInject(), koinInject())) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

@Composable
private fun RomanizationField(state: TermFormUiState, viewModel: TermFormViewModel) {
    if (state.language?.showRomanization != true) return
    OutlinedTextField(
        value = state.draft.romanization,
        onValueChange = { v -> viewModel.update { it.copy(romanization = v) } },
        label = { Text("Pronunciation") },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TranslationField(state: TermFormUiState, viewModel: TermFormViewModel, hint: String? = null, compact: Boolean = false) {
    OutlinedTextField(
        value = state.draft.translation,
        onValueChange = { v -> viewModel.update { it.copy(translation = v) } },
        label = { Text("Translation") },
        supportingText = when {
            state.lookingUpTranslation -> ({ Text("Looking up translation...") })
            hint != null -> ({ Text(hint) })
            else -> null
        },
        minLines = if (compact) 1 else 3,
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Text fields stay white even inside a tinted card. */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)

/** Parent term with suggestions from the language's existing terms. */
@Composable
private fun ParentField(state: TermFormUiState, viewModel: TermFormViewModel) {
    var open by remember { mutableStateOf(false) }
    val value = state.draft.parents.joinToString(", ")
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { v ->
                viewModel.setParents(v.split(',').map { it.trim() }.filter { it.isNotBlank() })
                viewModel.setParentQuery(v.substringAfterLast(',').trim())
                open = v.isNotBlank()
            },
            label = { Text("Parent term (optional)") },
            placeholder = { Text("None") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        AppMenu(expanded = open && state.parentSuggestions.isNotEmpty(), onDismissRequest = { open = false }) {
            state.parentSuggestions.take(8).forEach { match ->
                AppMenuItem(text = { Text(match.text) }, onClick = { open = false; viewModel.setParents(listOf(match.text)); viewModel.setParentQuery("") })
            }
        }
    }
}

/** Two cards side by side on wide screens: the term's fields and its learning status. */
@Composable
private fun StandaloneFields(state: TermFormUiState, viewModel: TermFormViewModel, direction: TextDirection, focusRequester: FocusRequester, onConfirmDelete: () -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    val information = @Composable { modifier: Modifier ->
        FormCard(modifier, AppIcons.Book, "Term information") {
            LanguageSelector(state, viewModel)
            TermField(state, viewModel, direction, focusRequester)
            RomanizationField(state, viewModel)
            TranslationField(state, viewModel, hint = "Use a concise meaning or contextual translation.")
            val language = state.language
            if (language != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = language.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.weight(1f)) { ParentField(state, viewModel) }
                }
            }
        }
    }
    val status = @Composable { modifier: Modifier ->
        FormCard(modifier, AppIcons.BarChart, "Learning status") {
            Text("How well do you know this term?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusSelector(selected = state.draft.status, onSelect = viewModel::setStatus, expanded = true)
            Row(Modifier.fillMaxWidth()) {
                Text("1 New", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("K Known", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("Added", state.createdAt?.let { it.formatDate() } ?: "Not saved yet")
            InfoRow("Last updated", if (state.saved) "Just now" else if (state.dirty) "Unsaved changes" else "\u2014")
            if (!state.isNew) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onConfirmDelete).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text("Delete term", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    if (compact) {
        information(Modifier.fillMaxWidth())
        status(Modifier.fillMaxWidth())
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
            information(Modifier.weight(2f))
            status(Modifier.weight(1.1f))
        }
    }
}

@Composable
private fun FormCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle(icon, title)
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Host name of a dictionary URL, for button labels. */
val LanguageDictionary.displayName: String
    get() = url.substringAfter("://").substringBefore("/").removePrefix("www.").ifEmpty { "Dictionary" }

/**
 * Status buttons U (unknown), 1–4, W and I. [expanded] stretches them to fill the row; [large] also fills
 * the row, with taller buttons, instead of compact chips.
 */
@Composable
fun StatusSelector(selected: TermStatus, onSelect: (TermStatus) -> Unit, expanded: Boolean = false, large: Boolean = false) {
    val colors = TayraTheme.current.statusColors
    Row(if (expanded || large) Modifier.fillMaxWidth() else Modifier, horizontalArrangement = Arrangement.spacedBy(if (expanded || large) 8.dp else 4.dp)) {
        TermStatus.paneButtons.forEach { status ->
            val isSelected = status == selected
            val background = colors.background(status).let { if (it == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else it }
            Box(
                Modifier
                    .then(if (expanded) Modifier.weight(1f).height(48.dp) else if (large) Modifier.weight(1f).height(52.dp) else Modifier)
                    .clip(RoundedCornerShape(if (expanded || large) 8.dp else 4.dp))
                    .background(background)
                    .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(if (expanded || large) 8.dp else 4.dp))
                    .clickable { onSelect(status) }
                    .semantics { this.selected = isSelected }
                    .then(if (expanded || large) Modifier else Modifier.padding(horizontal = 12.dp, vertical = 8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = status.abbreviation,
                    color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else Color.Black,
                    style = if (expanded || large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PanelHeader(language: Language?, nativeLanguage: String, onClose: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val native = LanguageCodes.option(nativeLanguage)?.name ?: nativeLanguage
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Book, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Term details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (language != null) Text("${language.name} \u2192 $native", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (onClose != null) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp)) }
        }
    }
}

/**
 * A rounded section of the pane. [tint] gives a lightly coloured background; null gives the
 * neutral variant; [filled] false leaves the card white with only a border.
 */
@Composable
private fun SectionCard(
    icon: @Composable () -> Unit,
    title: String,
    count: Int? = null,
    tint: Color? = null,
    filled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        !filled -> colors.surface
        tint != null -> tint.copy(alpha = 0.05f)
        else -> colors.surfaceVariant.copy(alpha = 0.4f)
    }
    val border = if (tint != null) tint.copy(alpha = 0.15f) else colors.outlineVariant
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background).border(1.dp, border, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (count != null) CountBadge(count)
        }
        content()
    }
}

/** A filled blue label reading "Aa", the mark of the term section. */
@Composable
private fun TermBadge() {
    Box(
        Modifier.size(width = 28.dp, height = 22.dp).clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 9.dp, bottomEnd = 9.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) { Text("Aa", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

/** A closed book, the mark of the offline dictionary section. */
@Composable
private fun DictionaryBadge() = BadgeIcon(AppIcons.BookClosed)

@Composable
private fun BadgeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, count: Int? = null) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (count != null) CountBadge(count)
    }
}

@Composable
private fun CountBadge(count: Int) {
    val colors = MaterialTheme.colorScheme
    Text(
        count.toString(),
        Modifier.clip(RoundedCornerShape(8.dp)).background(colors.primary.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = colors.primary,
    )
}

@Composable
private fun LinkChip(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.primary, fontWeight = FontWeight.Medium)
        Icon(AppIcons.OpenInNew, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
    }
}

/** A full-width outlined button with a trailing arrow, used for links to other screens. */
@Composable
private fun OutlineActionButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.primary,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
    }
}

/** A round icon button; [active] shows a stop square and [loading] a spinner instead of the icon. */
@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(colors.primary.copy(alpha = if (active) 0.18f else 0.1f))
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
            active -> Box(Modifier.size(12.dp).background(colors.primary, RoundedCornerShape(2.dp)))
            else -> Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Offers to download the offline dictionary for the language pair when it is not on the device. */
@Composable
private fun DictionaryDownloadCard(status: PackStatus, onDownload: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = status.state
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.05f))
            .border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(AppIcons.Book, "Dictionary")
        Text(
            when (state) {
                is PackState.Downloading -> "Downloading the ${status.pack.title} dictionary..."
                is PackState.Failed -> "The download failed: ${state.message}"
                else -> "The offline ${status.pack.title} dictionary is not downloaded. Get it to see meanings here without a network connection."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is PackState.Failed) colors.error else colors.onSurfaceVariant,
        )
        if (state is PackState.Downloading) {
            val progress = state.progress
            if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            Button(onClick = onDownload, shape = RoundedCornerShape(10.dp)) {
                Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (state is PackState.Failed) "Try again" else "Download dictionary")
            }
        }
    }
}

/** Meanings from the offline dictionary; the plus adds a meaning to the translation. */
@Composable
private fun DictionarySection(lookup: DictionaryLookup, onAdd: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    SectionCard({ DictionaryBadge() }, "Dictionary", count = lookup.entries.size, tint = null, filled = false) {
        lookup.entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text(entry.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(entry.pos, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (entry.ipa != null) Text(entry.ipa!!, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            entry.senses.forEachIndexed { senseIndex, sense ->
                val gloss = sense.glosses.joinToString("; ")
                if (senseIndex > 0) HorizontalDivider(Modifier.padding(start = 12.dp), color = colors.outlineVariant.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(gloss, style = MaterialTheme.typography.bodyLarge)
                        if (sense.tags.isNotEmpty()) {
                            Text(sense.tags.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    RoundIconButton(Icons.Default.Add, "Add to translation") { onAdd(gloss) }
                }
            }
        }
    }
}

/**
 * Example sentences from Tatoeba in a tinted card, with the term highlighted. Three are shown
 * until expanded; translations appear in a tooltip (hover or long press) and the button at the
 * bottom opens the full example search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamplesSection(state: TermFormUiState, language: Language?, onOpenExamples: ((Long, String) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val term = state.draft.text.replace("\u200B", "")
    var expanded by remember(term) { mutableStateOf(false) }
    val canExpand = state.examples.size > VISIBLE_EXAMPLES
    val languageId = language?.id
    val audio = rememberExampleAudio()
    val wordTranslations = koinInject<WordTranslationService>()
    val translateWord: suspend (String) -> String? = { word -> language?.let { wordTranslations.translate(it, word) } }
    val languageCode = language?.let { LanguageCodes.codeFor(it.name) }
    audio.PrepareSpeech(if (expanded) state.examples else state.examples.take(VISIBLE_EXAMPLES), languageCode)
    val total = state.examplesTotal ?: state.examples.size
    val canOpen = onOpenExamples != null && languageId != null && term.isNotBlank()

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.05f))
            .border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(AppIcons.Page, "Examples", if (state.loadingExamples) null else total) }
            if (canExpand) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Show fewer examples" else "Show all ${state.examples.size} examples",
                        tint = colors.primary,
                    )
                }
            }
        }
        when {
            state.loadingExamples -> Text("Looking up examples...", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            state.examples.isEmpty() -> Text("No examples found.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            else -> {
                val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr
                val visible = if (expanded) state.examples else state.examples.take(VISIBLE_EXAMPLES)
                visible.forEachIndexed { index, example ->
                    if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
                    val sentence = @Composable {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            HoverTranslatedText(
                                emphasize(example.text, term),
                                translate = translateWord,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
                                modifier = Modifier.weight(1f).padding(end = 10.dp),
                            )
                            val sound = audio.soundOf(example)
                            RoundIconButton(sound.icon, sound.description, active = sound.playing, loading = sound.loading) { audio.toggle(example, languageCode) }
                        }
                    }
                    val translation = example.translation
                    if (translation == null) {
                        sentence()
                    } else {
                        TooltipBox(
                            positionProvider = rememberLeftTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(translation) } },
                            state = rememberTooltipState(),
                        ) { sentence() }
                    }
                }
            }
        }
        if (canOpen && (state.examples.isNotEmpty() || state.examplesTotal != null)) {
            OutlineActionButton(if (state.examplesTotal != null) "View all $total examples" else "View more examples") { onOpenExamples!!.invoke(languageId!!, term) }
        }
    }
}

private const val VISIBLE_EXAMPLES = 3

/**
 * Places a tooltip to the left of its anchor, vertically centred, so the translation sits
 * beside the sentence instead of covering the examples above it. Falls back to above the
 * anchor when there is no room on the left.
 */
@Composable
private fun rememberLeftTooltipPositionProvider(): PopupPositionProvider {
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    return remember(gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left - popupContentSize.width - gap
                val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                return if (x >= 0) {
                    IntOffset(x, y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)))
                } else {
                    IntOffset(anchorBounds.left.coerceAtMost((windowSize.width - popupContentSize.width).coerceAtLeast(0)), (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0))
                }
            }
        }
    }
}

/**
 * Bolds the words of the sentence that are the term or an inflection of it: a word matches
 * when it starts with the term, or with the term minus its last letter for terms of five
 * letters or more (extranjero → extranjeras).
 */
private val TERM_HIGHLIGHT = SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF166534), background = Color(0xFF16A34A).copy(alpha = 0.16f))

private fun emphasize(sentence: String, term: String) = buildAnnotatedString {
    val needle = term.trim().lowercase()
    if (needle.isEmpty()) {
        append(sentence)
        return@buildAnnotatedString
    }
    val stem = if (needle.length >= 5) needle.dropLast(1) else needle
    val words = Regex("""[\p{L}\p{M}\p{Nd}'’-]+""")
    var index = 0
    for (match in words.findAll(sentence)) {
        append(sentence.substring(index, match.range.first))
        val word = match.value
        val lower = word.lowercase()
        val matches = lower == needle || lower.startsWith(needle) || (needle != stem && lower.startsWith(stem))
        if (matches) withStyle(TERM_HIGHLIGHT) { append(word) } else append(word)
        index = match.range.last + 1
    }
    append(sentence.substring(index))
}

