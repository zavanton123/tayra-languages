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
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.audio.rememberSpeechSynthesizer
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
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
    onDuplicateClick: ((Long) -> Unit)? = null,
    onOpenExamples: ((languageId: Long, text: String) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            trailingIcon = { SpeakButton(draft.text, language?.let { LanguageCodes.codeFor(it.name) }) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
            supportingText = if (state.lookingUpTranslation) ({ Text("Looking up translation...") }) else null,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        StatusSelector(selected = draft.status, onSelect = viewModel::setStatus)
        if (!state.dictionary.isEmpty) DictionarySection(state.dictionary, onAdd = viewModel::addGloss)

        if (language != null && language.termDictionaries.isNotEmpty() && draft.text.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle(AppIcons.Link, "Dictionaries")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                language.termDictionaries.forEach { dictionary ->
                    LinkChip(dictionary.displayName) { uriHandler.openUri(dictionary.lookupUrl(draft.text.replace("​", "").encodeURLParameter())) }
                }
            }
        }

        ExamplesSection(state, language, onOpenExamples)
        if (embedded) Spacer(Modifier.height(24.dp))
    }
}

/** Host name of a dictionary URL, for button labels. */
val LanguageDictionary.displayName: String
    get() = url.substringAfter("://").substringBefore("/").removePrefix("www.").ifEmpty { "Dictionary" }

@Composable
fun StatusSelector(selected: TermStatus, onSelect: (TermStatus) -> Unit) {
    val colors = TayraTheme.current.statusColors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // "Ignored" is set from the reading page, not from the form.
        TermStatus.selectable.filter { it != TermStatus.IGNORED }.forEach { status ->
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

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(colors.primary.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = colors.primary, modifier = Modifier.size(20.dp)) }
}

/** Meanings from the offline dictionary; the plus adds a meaning to the translation. */
@Composable
private fun DictionarySection(lookup: DictionaryLookup, onAdd: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(AppIcons.Book, "Dictionary", lookup.entries.size)
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
    val playback = rememberAudioPlayback()
    val synthesizer = rememberSpeechSynthesizer()
    val languageCode = language?.let { LanguageCodes.codeFor(it.name) }
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
                            Text(
                                emphasize(example.text, term),
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
                                modifier = Modifier.weight(1f).padding(end = 10.dp),
                            )
                            val audio = example.audioUrl
                            if (audio != null) {
                                RoundIconButton(if (playback.isPlaying(audio)) Icons.Default.Close else AppIcons.VolumeUp, "Play recording") { playback.toggle(audio) }
                            } else {
                                RoundIconButton(AppIcons.VolumeUp, "Pronounce") { synthesizer.speak(example.text, languageCode) }
                            }
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
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { onOpenExamples!!.invoke(languageId!!, term) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (state.examplesTotal != null) "View all $total examples" else "View more examples",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Text(
                "Open the full examples search",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
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

