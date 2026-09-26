package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.TermReference
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.TagInput
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
    onDuplicateClick: ((Long) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }

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
            .padding(12.dp)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Enter) {
                    viewModel.save()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.duplicateOf?.let { duplicate ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Term \"${duplicate.displayText}\" already exists.", color = MaterialTheme.colorScheme.error)
                if (onDuplicateClick != null) TextButton(onClick = { onDuplicateClick(duplicate.id) }) { Text("Open") }
            }
        } ?: ErrorMessage(state.error)

        if (state.showLanguageSelector) {
            Dropdown(
                options = state.languages,
                selected = language,
                onSelect = { l -> viewModel.update { it.copy(languageId = l.id) } },
                label = "Language",
                optionLabel = { it.name },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = draft.text,
            onValueChange = { text -> viewModel.update { it.copy(text = text) } },
            label = { Text("Term") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        TagInput(
            values = draft.parents,
            onValuesChange = viewModel::setParents,
            label = "Parents",
            suggestions = state.parentSuggestions.map { it.text.replace("​", "") },
            onQueryChange = viewModel::setParentQuery,
            onChipClick = viewModel::openParent,
            suggestionContent = { text ->
                val match = state.parentSuggestions.firstOrNull { it.text.replace("​", "") == text }
                Column {
                    Text(text)
                    if (match?.translation != null) Text(match.translation!!, style = MaterialTheme.typography.bodySmall)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (language?.showRomanization == true) {
            OutlinedTextField(
                value = draft.romanization,
                onValueChange = { v -> viewModel.update { it.copy(romanization = v) } },
                label = { Text("Pronunciation") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = draft.translation,
            onValueChange = { v -> viewModel.update { it.copy(translation = v) } },
            label = { Text("Translation") },
            supportingText = when {
                state.lookingUpTranslation -> ({ Text("Looking up translation...") })
                state.translationSuggested -> ({ Text("Suggested translation; edit as needed") })
                else -> null
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        StatusSelector(selected = draft.status, onSelect = viewModel::setStatus)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.effectiveSyncStatus,
                onCheckedChange = { v -> viewModel.update { it.copy(syncStatus = v) } },
                enabled = draft.parents.size == 1,
            )
            Text("Link to parent", style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!draft.isNew) OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
            Text(
                when {
                    state.saving -> "Saving..."
                    state.dirty -> "Changes are saved automatically"
                    state.saved -> "Saved"
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (language != null && language.termDictionaries.isNotEmpty() && draft.text.isNotBlank()) {
            HorizontalDivider()
            Text("Dictionaries", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                language.termDictionaries.forEach { dictionary ->
                    OutlinedButton(onClick = { uriHandler.openUri(dictionary.lookupUrl(draft.text.replace("​", "").encodeURLParameter())) }) {
                        Text(dictionary.displayName)
                    }
                }
            }
        }

        HorizontalDivider()
        ExamplesSection(state, language)
        HorizontalDivider()
        SentencesSection(state, language, onLoad = viewModel::loadReferences)
        if (embedded) Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete term?",
            text = "This cannot be undone. If this term has children, they will be orphaned.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Host name of a dictionary URL, for button labels. */
val LanguageDictionary.displayName: String
    get() = url.substringAfter("://").substringBefore("/").removePrefix("www.").ifEmpty { "Dictionary" }

@Composable
fun StatusSelector(selected: TermStatus, onSelect: (TermStatus) -> Unit) {
    val colors = TayraTheme.current.statusColors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TermStatus.selectable.forEach { status ->
            val isSelected = status == selected
            val background = colors.background(status).let { if (it == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else it }
            Text(
                text = status.abbreviation,
                color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else Color.Black,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(background)
                    .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { onSelect(status) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Example sentences from Tatoeba, with the term in bold. The title links to the Tatoeba search;
 * five examples are shown until expanded, and translations appear in a tooltip (hover or long press).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamplesSection(state: TermFormUiState, language: Language?) {
    val uriHandler = LocalUriHandler.current
    val term = state.draft.text.replace("\u200B", "")
    val searchUrl = "https://tatoeba.org/en/sentences/search?query=" + term.encodeURLParameter() +
        (language?.let { LanguageCodes.tatoebaCodeFor(it.name) }?.let { "&from=$it" } ?: "")
    var expanded by remember(term) { mutableStateOf(false) }
    val canExpand = state.examples.size > VISIBLE_EXAMPLES

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Examples",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(searchUrl) },
        )
        if (canExpand) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Show fewer examples" else "Show all ${state.examples.size} examples",
                )
            }
        }
    }
    when {
        state.loadingExamples -> Text("Looking up examples...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.examples.isEmpty() -> Text("No examples found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> {
            val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr
            val visible = if (expanded) state.examples else state.examples.take(VISIBLE_EXAMPLES)
            visible.forEach { example ->
                val sentence = @Composable {
                    Text(
                        emphasize(example.text, term),
                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = direction),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
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
}

private const val VISIBLE_EXAMPLES = 5

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
        if (matches) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(word) } else append(word)
        index = match.range.last + 1
    }
    append(sentence.substring(index))
}

@Composable
private fun SentencesSection(state: TermFormUiState, language: Language?, onLoad: () -> Unit) {
    val refs = state.references
    if (refs == null) {
        TextButton(onClick = onLoad, enabled = !state.loadingReferences && state.draft.text.isNotBlank()) {
            Text(if (state.loadingReferences) "Loading sentences..." else "Show sentences")
        }
        return
    }
    Text("Sentences", style = MaterialTheme.typography.labelLarge)
    if (refs.isEmpty) {
        Text("No references found.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr
    ReferenceGroup("\"${state.draft.text.replace("​", "")}\"", refs.term, direction)
    ReferenceGroup("Child terms", refs.children, direction)
    refs.parents.forEach { (parent, list) -> ReferenceGroup("\"$parent\"", list, direction) }
}

@Composable
private fun ReferenceGroup(title: String, references: List<TermReference>, direction: TextDirection) {
    if (references.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall)
    references.forEach { ref ->
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(highlighted(ref.sentence), style = MaterialTheme.typography.bodyMedium.copy(textDirection = direction))
            Text(ref.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Renders `**text**` markers as bold. */
private fun highlighted(sentence: String) = buildAnnotatedString {
    val parts = sentence.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
