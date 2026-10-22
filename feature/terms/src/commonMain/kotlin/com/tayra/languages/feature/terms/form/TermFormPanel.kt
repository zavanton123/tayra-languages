package com.tayra.languages.feature.terms.form

import com.tayra.languages.core.ui.components.inLearnedScript
import androidx.compose.ui.text.style.TextAlign
import com.tayra.languages.core.ui.components.FitText
import com.tayra.languages.core.ui.components.chosenFor
import com.tayra.languages.core.ui.components.shownAs
import com.tayra.languages.core.ui.components.shownAbbreviation
import com.tayra.languages.core.ui.components.shownName
import com.tayra.languages.core.ui.components.shownStatuses
import com.tayra.languages.core.ui.components.learningLevels
import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.KanjiInfo
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WordFrequency
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.language.OnlineDictionaries
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.morphology.Chinese
import com.tayra.languages.core.domain.morphology.ChineseGrammarWiki
import com.tayra.languages.core.domain.service.WordRecording
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.audio.AudioPlayback
import com.tayra.languages.core.ui.audio.RecordingButton
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.HoverTranslatedText
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.KeyValueRow
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PaneHeader
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.components.toneColored
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.examples.PrepareSpeech
import com.tayra.languages.feature.terms.examples.rememberExampleAudio
import com.tayra.languages.feature.terms.examples.withSelected
import com.tayra.languages.feature.terms.examples.withStatuses
import io.ktor.http.encodeURLParameter
import kotlin.time.Clock
import org.koin.compose.koinInject

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
    val playback = rememberAudioPlayback()
    CompositionLocalProvider(LocalRecordingPlayback provides playback) { TermFormContent(state, viewModel, modifier, embedded, onClose, onDuplicateClick, onOpenExamples, onManageDictionaries) }
}

@Composable
private fun TermFormContent(
    state: TermFormUiState,
    viewModel: TermFormViewModel,
    modifier: Modifier,
    embedded: Boolean,
    onClose: (() -> Unit)?,
    onDuplicateClick: ((Long) -> Unit)?,
    onOpenExamples: ((languageId: Long, text: String) -> Unit)?,
    onManageDictionaries: ((languageId: Long) -> Unit)?,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResumed() }
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }
    var confirmDelete by remember { mutableStateOf(false) }

    if (state.loading) {
        LoadingIndicator(modifier.heightIn(min = LOADING_MIN_HEIGHT))
        return
    }
    val draft = state.draft
    val language = state.language
    val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr
    val empty = draft.text.isBlank()
    LaunchedEffect(Unit) { if (embedded && empty) runCatching { focusRequester.requestFocus() } }

    // A phone shows the embedded form in a bottom sheet, with no scrollbar and the sheet's handle just above.
    val sheet = embedded && LocalWindowWidth.current.isCompact
    ScrollColumn(
        modifier,
        // Inside the reader's rounded panel the bar runs in a gutter of its own, clear of the border, its corners and the cards.
        scrollbarModifier = if (embedded) Modifier.padding(top = AppSpacing.l, bottom = AppSpacing.l, end = AppSpacing.s) else Modifier,
        contentModifier = Modifier
            .padding(start = AppSpacing.l, top = if (sheet) AppSpacing.xs else AppSpacing.l, bottom = AppSpacing.l, end = if (embedded && !sheet) AppSpacing.xxxl else AppSpacing.l)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Enter) {
                    viewModel.save()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        if (embedded) PanelHeader(onClose)
        state.duplicateOf?.let { duplicate ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Term \"{0}\" already exists.", duplicate.displayText), color = MaterialTheme.colorScheme.error)
                if (onDuplicateClick != null) QuietButton(tr("Open"), onClick = { onDuplicateClick(duplicate.id) })
            }
        } ?: ErrorMessage(state.error)

        val pack = state.dictionaryPack
        val online = state.wiktionary
        val dictionaryBlock: @Composable () -> Unit = {
            val code = language?.let { LanguageCodes.codeFor(it.name) }
            // Wiktionary's table of the word opens from the Dictionary card, in a dialog of its own.
            val table = online.table.takeIf { !empty }
            val lemma = online.headword ?: draft.text.trim()
            var tableShown by remember(draft.text) { mutableStateOf(false) }
            val conjugations: (@Composable () -> Unit)? = table?.let { { ConjugationsRow(lemma, verb = it.pos == "verb") { tableShown = true } } }
            if (table != null && tableShown) {
                val speaker = rememberSpeaker(koinInject(), koinInject())
                ConjugationDialog(table, lemma, draft.text, language?.name, code, online.credit, onSpeak = { speaker.speak(it, code) }) { tableShown = false }
            }
            when {
                empty -> Unit
                !state.dictionary.isEmpty -> DictionarySection(state.dictionary, code, toneColors = state.toneColors, zhuyin = state.zhuyin, onAdd = viewModel::addGloss, extra = conjugations)
                else -> {
                    // Without an installed pack Wiktionary's meanings stand in, beside the offer to download one.
                    if (pack?.state !is PackState.Installed && !online.dictionary.isEmpty) {
                        DictionarySection(online.dictionary, code, toneColors = state.toneColors, zhuyin = state.zhuyin, onAdd = viewModel::addGloss, credit = online.page?.let { it to language?.name }, extra = conjugations)
                    } else {
                        conjugations?.invoke()
                    }
                    if (pack != null && pack.state !is PackState.Installed) DictionaryDownloadCard(pack, state.pairDictionary, draft.text.trim(), onDownload = viewModel::downloadDictionary)
                }
            }
            if (!empty && state.notes.isNotEmpty()) NotesSection(state.notes)
            // The pane plays a recording from the word's own speaker; the full list is left to the editor page.
            if (!embedded && !empty && state.recordings.isNotEmpty()) RecordingsSection(state.recordings, draft.text.trim())
            if (!empty && state.kanji.isNotEmpty()) KanjiSection(state.kanji)
        }
        val linksContent: @Composable () -> Unit = {
            if (language != null) {
                if (language.termDictionaries.isEmpty()) {
                    Text(tr("No online dictionaries enabled for {0}.", tr(language.name)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val source = LanguageOption(LanguageCodes.codeFor(language.name) ?: "en", language.name)
                val target = LanguageCatalog.nativeOption(state.nativeLanguage)
                val labels = OnlineDictionaries.labels(language.termDictionaries, source, target)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    language.termDictionaries.forEach { dictionary ->
                        LinkChip(labels[dictionary] ?: OnlineDictionaries.host(dictionary.url)) {
                            uriHandler.openUri(dictionary.lookupUrl(draft.text.replace("​", "").encodeURLParameter()))
                        }
                    }
                    // The Chinese Grammar Wiki's notes on the word, or on its dictionary form, as Zhongwen links them.
                    if (source.code == "zh") {
                        val words = listOf(draft.text.replace("​", "")) + state.dictionary.lemmas
                        words.firstNotNullOfOrNull(ChineseGrammarWiki::grammarUrl)?.let { url -> LinkChip(tr("Grammar notes")) { uriHandler.openUri(url) } }
                        words.firstNotNullOfOrNull(ChineseGrammarWiki::vocabularyUrl)?.let { url -> LinkChip(tr("Vocabulary notes")) { uriHandler.openUri(url) } }
                    }
                }
                if (onManageDictionaries != null) {
                    OutlineActionButton(tr("Manage dictionaries")) { onManageDictionaries(language.id) }
                }
            }
        }
        val examplesBlock: @Composable () -> Unit = {
            if (!empty) ExamplesSection(state, language, onOpenExamples, onMarkWord = viewModel::markExampleWord, onOpenWord = viewModel::openExampleTerm, onRetry = viewModel::retryExamples)
        }

        if (embedded) {
            // The pane is titled Term details and the field is labelled Term: the section needs no heading of its own.
            SectionCard({}, title = null, tint = null, filled = false) {
                LanguageSelector(state, viewModel)
                // A word opened from a text is shown as its heading, the meaning just under it; its pencil
                // turns it back into the field. A word still to be typed keeps the field.
                var editingText by remember(viewModel) { mutableStateOf(false) }
                if (viewModel.opensOnWord && !editingText && !empty) TermHeading(state, direction, onEdit = { editingText = true }, onOpenExamples = onOpenExamples)
                else TermField(state, viewModel, direction, focusRequester)
                if (empty) WordFinderButton(state, viewModel)
                if (empty) {
                    Text(tr("Type a word or phrase to look it up."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    TranslationField(state, viewModel, compact = true)
                    StatusSelector(selected = draft.status, onSelect = viewModel::setStatus, large = true)
                }
            }
            dictionaryBlock()
            if (language != null && !empty) SectionCard({ BadgeIcon(AppIcons.Link) }, tr("Dictionaries"), tint = null, filled = false) { linksContent() }
            examplesBlock()
        } else {
            // The editor page: what the term is and says on the left, its status and links on the right.
            val links: @Composable (Modifier) -> Unit = { m ->
                if (language != null && !empty) FormCard(m, AppIcons.Link, tr("Dictionaries")) { linksContent() }
            }
            val delete: @Composable (Modifier) -> Unit = { m -> if (!state.isNew) DeleteCard(m, onClick = { confirmDelete = true }) }
            CompositionLocalProvider(LocalPageSections provides true) {
            if (LocalWindowWidth.current.isCompact) {
                InformationCard(Modifier.fillMaxWidth(), state, viewModel, direction, focusRequester)
                StatusCard(Modifier.fillMaxWidth(), state, viewModel)
                FlashcardCard(Modifier.fillMaxWidth(), state, viewModel)
                dictionaryBlock()
                examplesBlock()
                links(Modifier.fillMaxWidth())
                delete(Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        InformationCard(Modifier.fillMaxWidth(), state, viewModel, direction, focusRequester)
                        dictionaryBlock()
                        examplesBlock()
                    }
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        StatusCard(Modifier.fillMaxWidth(), state, viewModel)
                        FlashcardCard(Modifier.fillMaxWidth(), state, viewModel)
                        links(Modifier.fillMaxWidth())
                        delete(Modifier.fillMaxWidth())
                    }
                }
            }
            }
        }
        if (embedded) Spacer(Modifier.height(AppSpacing.xxl))
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = tr("Delete \"{0}\"?", draft.text.replace("\u200B", "")),
            text = tr("The term and its translation will be removed. This cannot be undone."),
            confirmLabel = tr("Delete"),
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
        label = tr("Language"),
        optionLabel = { tr(it.name) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TermField(state: TermFormUiState, viewModel: TermFormViewModel, direction: TextDirection, focusRequester: FocusRequester) {
    val language = state.language
    AppTextField(
        value = state.draft.text,
        onValueChange = { text -> viewModel.update { it.copy(text = text) } },
        label = { Text(tr("Term")) },
        singleLine = true,
        trailingIcon = { TermSpeakButton(state, language?.let { LanguageCodes.codeFor(it.name) }) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).reportsTyping(),
    )
}

/** The word the pane is about, large, with its speaker, a way to its examples, and a pencil to change how it is written. */
@Composable
private fun TermHeading(state: TermFormUiState, direction: TextDirection, onEdit: () -> Unit, onOpenExamples: ((Long, String) -> Unit)?) {
    val language = state.language
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(state.draft.text, Modifier.weight(1f).testTag("term-heading"), style = AppText.headline.copy(textDirection = direction))
        TermSpeakButton(state, language?.let { LanguageCodes.codeFor(it.name) })
        if (onOpenExamples != null && language != null) {
            AppIconButton(AppIcons.Page, contentDescription = tr("Examples"), onClick = { onOpenExamples(language.id, state.draft.text.trim()) })
        }
        AppIconButton(Icons.Default.Edit, contentDescription = tr("Edit the word"), onClick = onEdit)
    }
}

/** Plays a person's recording of the term when there is one, and has the speech engine read it otherwise or when it fails. */
@Composable
private fun TermSpeakButton(state: TermFormUiState, languageCode: String?) {
    val speaker = rememberSpeaker(koinInject(), koinInject())
    val recording = state.recordings.firstOrNull { it.word == state.draft.text.trim() }
    if (recording != null) {
        RecordingButton(recording.url, LocalRecordingPlayback.current, onFailed = { speaker.speak(state.draft.text, languageCode) })
    } else {
        SpeakButton(state.draft.text, languageCode, speaker)
    }
}

/** People's recordings of the term from Wikimedia Commons, each naming who recorded it and linking to its page and licence. */
@Composable
internal fun RecordingsSection(recordings: List<WordRecording>, term: String) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val playback = LocalRecordingPlayback.current
    SectionCard({ BadgeIcon(AppIcons.VolumeUp) }, tr("Recordings"), count = recordings.size, tint = null, filled = false) {
        recordings.forEach { recording ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), modifier = Modifier.testTag("recording-${recording.speaker}")) {
                RecordingButton(recording.url, playback, Modifier.size(AUDIO_BUTTON))
                if (recording.word != term) Text(recording.word, style = AppText.labelStrong)
                Text(
                    "${recording.speaker} \u00b7 ${recording.source}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.clip(AppShapes.small).clickable(enabled = recording.pageUrl.isNotEmpty()) { uriHandler.openUri(recording.pageUrl) }.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
                )
            }
        }
    }
}

/** The one player of the form's recordings, so starting one stops the other. */
internal val LocalRecordingPlayback = staticCompositionLocalOf<AudioPlayback> { error("no recording playback") }

@Composable
private fun RomanizationField(state: TermFormUiState, viewModel: TermFormViewModel) {
    if (state.language?.showRomanization != true) return
    AppTextField(
        value = state.draft.romanization,
        onValueChange = { v -> viewModel.update { it.copy(romanization = v) } },
        label = { Text(tr("Pronunciation")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().reportsTyping(),
    )
}

@Composable
private fun TranslationField(state: TermFormUiState, viewModel: TermFormViewModel, hint: String? = null, compact: Boolean = false) {
    AppTextField(
        value = state.draft.translation,
        onValueChange = { v -> viewModel.update { it.copy(translation = v) } },
        // The lookup is announced in the label, which is there anyway: a line under the field that comes and goes
        // would move everything below it (the examples, their button) while the user is about to click.
        label = { Text(if (state.lookingUpTranslation) tr("Looking up translation...") else tr("Translation")) },
        supportingText = hint?.let { { Text(it) } },
        minLines = if (compact) 1 else 3,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().reportsTyping(),
    )
}

/**
 * Told whether one of the form's text fields is being typed in, by the screen around the form,
 * whose hotkeys (plain letters and digits among them) must stay out of the typing.
 */
val LocalTypingReporter = staticCompositionLocalOf<(typing: Boolean) -> Unit> { {} }

@Composable
private fun Modifier.reportsTyping(): Modifier {
    val report = LocalTypingReporter.current
    return onFocusChanged { report(it.isFocused) }
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
        AppTextField(
            value = value,
            onValueChange = { v ->
                viewModel.setParents(v.split(',').map { it.trim() }.filter { it.isNotBlank() })
                viewModel.setParentQuery(v.substringAfterLast(',').trim())
                open = v.isNotBlank()
            },
            label = { Text(tr("Parent term (optional)")) },
            placeholder = { Text(tr("None")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().reportsTyping(),
        )
        AppMenu(expanded = open && state.parentSuggestions.isNotEmpty(), onDismissRequest = { open = false }) {
            state.parentSuggestions.take(8).forEach { match ->
                AppMenuItem(text = { Text(match.text) }, onClick = { open = false; viewModel.setParents(listOf(match.text)); viewModel.setParentQuery("") })
            }
        }
    }
}

/** Two cards side by side on wide screens: the term's fields and its learning status. */
/** The term itself: its text (with its parent beside it on wide screens), pronunciation and translation. */
@Composable
private fun InformationCard(modifier: Modifier, state: TermFormUiState, viewModel: TermFormViewModel, direction: TextDirection, focusRequester: FocusRequester) {
    val compact = LocalWindowWidth.current.isCompact
    FormCard(modifier, null, tr("Term information"), iconText = "Aa") {
        LanguageSelector(state, viewModel)
        if (compact || state.language == null) {
            TermField(state, viewModel, direction, focusRequester)
            if (state.language != null) ParentField(state, viewModel)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f)) { TermField(state, viewModel, direction, focusRequester) }
                Box(Modifier.weight(1f)) { ParentField(state, viewModel) }
            }
        }
        WordFinderButton(state, viewModel)
        RomanizationField(state, viewModel)
        TranslationField(state, viewModel, hint = tr("Use a concise meaning or contextual translation."), compact = true)
    }
}

@Composable
private fun StatusCard(modifier: Modifier, state: TermFormUiState, viewModel: TermFormViewModel) {
    FormCard(modifier, AppIcons.BarChart, tr("Learning status")) {
        Text(tr("How well do you know this term?"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusSelector(selected = state.draft.status, onSelect = viewModel::setStatus, expanded = true)
        AppDivider()
        KeyValueRow(tr("Added"), state.createdAt?.let { it.formatDate() } ?: tr("Not saved yet"))
        KeyValueRow(tr("Last updated"), if (state.saved) tr("Just now") else if (state.dirty) tr("Unsaved changes") else "\u2014")
    }
}

/** The term's flashcard: where it stands, with the ways to set it aside or start it over. */
@Composable
private fun FlashcardCard(modifier: Modifier, state: TermFormUiState, viewModel: TermFormViewModel) {
    if (!state.flashcardsShown || state.isNew) return
    var confirmReset by remember { mutableStateOf(false) }
    FormCard(modifier, AppIcons.Bookmark, tr("Flashcard")) {
        val card = state.flashcard
        if (card == null) {
            Text(
                tr("This term has no flashcard. Terms at status 1 to 4 get one."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@FormCard
        }
        val schedule = card.schedule
        val stateName = when (schedule.state) {
            CardState.NEW -> tr("New card")
            CardState.LEARNING -> tr("Being learned")
            CardState.REVIEW -> tr("In review")
            CardState.RELEARNING -> tr("Being relearned")
        }
        KeyValueRow(tr("State"), if (card.suspended) tr("{0}, suspended", stateName) else stateName)
        KeyValueRow(
            tr("Next review"),
            when {
                card.suspended -> tr("Suspended")
                schedule.state == CardState.NEW -> tr("Not shown yet")
                schedule.due <= Clock.System.now() -> tr("Due now")
                else -> schedule.due.formatDate()
            },
        )
        if (schedule.state == CardState.REVIEW) KeyValueRow(tr("Interval"), trPlural(schedule.intervalDays, "{0} day", "{0} days"))
        KeyValueRow(tr("Answers"), if (schedule.lapses > 0) trPlural(schedule.lapses, "{1}, forgotten {0} time", "{1}, forgotten {0} times", schedule.reps) else "${schedule.reps}")
        AppDivider()
        SwitchSetting(tr("Suspend flashcard"), tr("Keep the card but leave it out of reviews."), checked = card.suspended, dense = true, onChange = viewModel::setFlashcardSuspended)
        if (schedule.state != CardState.NEW) OutlineActionButton(tr("Reset flashcard progress")) { confirmReset = true }
    }
    if (confirmReset) {
        ConfirmDialog(
            title = tr("Reset this flashcard?"),
            text = tr("The card becomes new again and its review schedule is forgotten. The term's status stays as it is."),
            confirmLabel = tr("Reset"),
            destructive = true,
            onConfirm = { viewModel.restartFlashcard(); confirmReset = false },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Deleting gets a card of its own, apart from the fields, so it is not pressed by accident. */
@Composable
private fun DeleteCard(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.cardSurface(AppShapes.card)
            .clickable(onClick = onClick).padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(AppSizes.iconLarge))
        Text(tr("Delete term"), style = AppText.itemTitle, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FormCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    iconText: String? = null,
    content: @Composable () -> Unit,
) {
    ContentCard(title, subtitle = null, icon = icon, iconText = iconText, modifier = modifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) { content() }
    }
}

/** Host name of a dictionary URL, for button labels. */
val LanguageDictionary.displayName: String
    get() = url.substringAfter("://").substringBefore("/").removePrefix("www.").ifEmpty { tr("Dictionary") }

/**
 * Status buttons U (unknown), 1–4, W and I. [expanded] stretches them to fill the row; [large] also fills
 * the row, with taller buttons, instead of compact chips.
 */
@Composable
fun StatusSelector(selected: TermStatus, onSelect: (TermStatus) -> Unit, expanded: Boolean = false, large: Boolean = false) {
    val colors = TayraTheme.current.statusColors
    val wide = expanded || large
    // Without the learning levels the row is the three statuses by name.
    val words = !learningLevels
    Column(if (wide) Modifier.fillMaxWidth() else Modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(if (wide) Modifier.fillMaxWidth() else Modifier, horizontalArrangement = Arrangement.spacedBy(if (wide) AppSpacing.s else AppSpacing.xs)) {
            shownStatuses(TermStatus.paneButtons).forEach { status ->
                val isSelected = status.shownAs(selected)
                val chosen = status.chosenFor(selected)
                val named = words
                val background = colors.background(status).let { if (it == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else it }
                // The ring moves to the chosen status by fading out on the old one and in on the new; the others fade back,
                // so the chosen one stands out by more than its ring.
                val ring by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, AppMotion.spec(AppMotion.short))
                val shown by animateFloatAsState(if (isSelected) 1f else AppAlpha.muted, AppMotion.spec(AppMotion.short))
                val name = status.shownName()
                Box(
                    Modifier
                        .then(if (expanded) Modifier.weight(if (named) NAMED_STATUS_WEIGHT else 1f).height(AppSizes.touch) else if (large) Modifier.weight(if (named) NAMED_STATUS_WEIGHT else 1f).height(LARGE_STATUS_BUTTON_HEIGHT) else Modifier)
                        .alpha(shown)
                        .clip(if (wide) AppShapes.compact else AppShapes.tiny)
                        .background(background)
                        .border(AppBorders.selected(ring), if (wide) AppShapes.compact else AppShapes.tiny)
                        .clickable { onSelect(chosen) }
                        .semantics { this.selected = isSelected; contentDescription = name }
                        .then(if (wide) Modifier else Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.s)),
                    contentAlignment = Alignment.Center,
                ) {
                    val ink = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else AppAccents.ink
                    if (named) FitText(name, Modifier.padding(horizontal = AppSpacing.xs), style = (if (wide) AppText.itemTitle else AppText.badge).copy(textAlign = TextAlign.Center), color = ink)
                    else Text(text = status.shownAbbreviation(), color = ink, style = if (wide) AppText.itemTitle else AppText.badge)
                }
            }
        }
        // The letters are short; the chosen status is named in full under them. Named buttons need no caption.
        if (!words) {
            Text(selected.shownName(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("status-name"))
        }
    }
}

@Composable
private fun PanelHeader(onClose: (() -> Unit)?) {
    PaneHeader(tr("Dictionary"), subtitle = null, onClose, Modifier.testTag("term-pane-header"), icon = AppIcons.Book)
}

/**
 * A rounded section of the pane. [tint] gives a lightly coloured background; null gives the
 * neutral variant; [filled] false leaves the card white with only a border.
 */
@Composable
internal fun SectionCard(
    icon: @Composable () -> Unit,
    title: String?,
    count: Int? = null,
    tint: Color? = null,
    filled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        !filled -> colors.surface
        tint != null -> tint.copy(alpha = AppAlpha.wash)
        else -> colors.surfaceVariant.copy(alpha = AppAlpha.disabled)
    }
    val border = if (tint != null) tint.copy(alpha = AppAlpha.soft) else colors.outlineVariant
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(background).border(AppBorders.hairline(border), AppShapes.card).padding(sectionPadding()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        if (title != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (LocalPageSections.current) AppSpacing.l else AppSpacing.m)) {
            if (LocalPageSections.current) {
                Box(Modifier.size(AppSizes.tileMedium).clip(AppShapes.panel).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) { icon() }
            } else {
                icon()
            }
            Text(title, style = sectionTitleStyle())
            if (count != null) Tag(count.toString(), size = TagSize.Small, shape = AppShapes.compact)
        }
        content()
    }
}

/** A closed book, the mark of the offline dictionary section. */
@Composable
private fun DictionaryBadge() = BadgeIcon(AppIcons.BookClosed)

@Composable
internal fun BadgeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSizes.iconMedium))
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, count: Int? = null) {
    val colors = MaterialTheme.colorScheme
    val page = LocalPageSections.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (page) AppSpacing.l else AppSpacing.m)) {
        if (page) IconTile(icon, size = 44) else Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconMedium))
        Text(title, style = sectionTitleStyle())
        if (count != null) Tag(count.toString(), size = TagSize.Small, shape = AppShapes.compact)
    }
}

/** True on the editor page, whose cards have the page's larger headers; the reader's pane keeps them small. */
internal val LocalPageSections = staticCompositionLocalOf { false }

@Composable
private fun sectionPadding() = if (LocalPageSections.current) AppSpacing.xl else AppSpacing.l

@Composable
private fun sectionTitleStyle() = if (LocalPageSections.current) AppText.sectionTitle else AppText.itemTitle

@Composable
private fun LinkChip(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control).clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(label, style = AppText.label, color = colors.primary)
        Icon(AppIcons.OpenInNew, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconSmall))
    }
}

/** A full-width outlined button with a trailing arrow, used for links to other screens. */
@Composable
private fun OutlineActionButton(label: String, onClick: () -> Unit) {
    SecondaryButton(label, onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.xs), trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
}

/**
 * Offers to download the offline dictionary for the language pair when it is not on the device: a
 * quiet note with a text button, so it does not outweigh the parts of the pane that save the word.
 */
@Composable
private fun DictionaryDownloadCard(status: PackStatus, installed: String?, word: String, onDownload: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = status.state
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).border(AppBorders.hairline(), AppShapes.card).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Icon(AppIcons.Book, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconSmall))
            Text(
                when (state) {
                    is PackState.Downloading -> tr("Downloading the {0} dictionary...", packTitle(status))
                    is PackState.Failed -> tr("The download failed: {0}", state.message)
                    // Another dictionary of the pair is on the device but has no entry: the pack is offered for its larger list, not as missing.
                    else -> if (installed != null) tr("No entry for “{0}” in {1}. The {2} dictionary from Wiktionary has more words.", word, installed, packTitle(status))
                    else tr("The offline {0} dictionary is not downloaded. Get it to see meanings here without a network connection.", packTitle(status))
                },
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (state is PackState.Failed) colors.error else colors.onSurfaceVariant,
            )
        }
        if (state is PackState.Downloading) {
            AppProgressBar(state.progress)
        } else {
            QuietButton(if (state is PackState.Failed) tr("Try again") else if (installed != null) tr("Download Wiktionary dictionary") else tr("Download dictionary"), onClick = onDownload, icon = AppIcons.Download)
        }
    }
}

/** The pack's language pair, "Spanish → English", with the names in the interface language. */
private fun packTitle(status: PackStatus): String = status.pack.title.split(" \u2192 ").joinToString(" \u2192 ") { tr(it) }

/** Each kanji of a Japanese term, large, with its meanings, on and kun readings and how common it is, as Yomitan's kanji entries. */
@Composable
internal fun KanjiSection(kanji: List<KanjiInfo>) {
    val colors = MaterialTheme.colorScheme
    SectionCard({ KanjiBadge() }, tr("Kanji"), count = kanji.size, tint = null, filled = false) {
        kanji.forEachIndexed { index, info ->
            if (index > 0) AppDivider(Modifier.padding(vertical = AppSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l), modifier = Modifier.testTag("kanji-${info.character}")) {
                Text(info.character, style = MaterialTheme.typography.displaySmall)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    if (info.meanings.isNotEmpty()) Text(info.meanings.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                    if (info.onyomi.isNotEmpty()) KanjiReadings(tr("On"), info.onyomi)
                    if (info.kunyomi.isNotEmpty()) KanjiReadings(tr("Kun"), info.kunyomi)
                    val facts = listOfNotNull(
                        info.strokes?.let { trPlural(it, "{0} stroke", "{0} strokes") },
                        info.grade?.let { schoolGrade(it) },
                        info.jlpt?.let { tr("Old JLPT {0}", it) },
                        info.frequency?.let { tr("Frequency #{0}", it) },
                    )
                    if (facts.isNotEmpty()) Text(facts.joinToString(" \u00b7 "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

/** KANJIDIC's grades: 1 to 6 are taught in primary school, 8 later among the jōyō kanji, 9 and 10 are for names only. */
private fun schoolGrade(grade: Int): String = when {
    grade <= 6 -> tr("Grade {0}", grade)
    grade == 8 -> tr("Secondary school")
    else -> tr("Used in names")
}

@Composable
private fun KanjiReadings(label: String, readings: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(KANJI_READING_LABEL))
        Text(readings.joinToString("\u3001"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** A filled square with 字 ("character"), the mark of the kanji section. */
@Composable
private fun KanjiBadge() {
    Box(Modifier.size(AppSizes.iconMedium).clip(AppShapes.tiny).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Text("\u5b57", color = MaterialTheme.colorScheme.onPrimary, style = AppText.badge)
    }
}

/** What the frequency dictionaries say of a word, each as its title and its rank or count, as Yomitan tags them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FrequencyLabels(frequencies: List<WordFrequency>) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs), modifier = Modifier.padding(start = AppSpacing.xxs, top = AppSpacing.xxs)) {
        frequencies.forEach { frequency ->
            Row(
                Modifier.clip(AppShapes.small).border(AppBorders.hairline(), AppShapes.small).testTag("frequency-${frequency.dictionary}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(frequency.dictionary, style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer, modifier = Modifier.background(colors.secondaryContainer).padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs))
                Text(frequency.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs))
            }
        }
    }
}

/** Parts of speech whose downstep moves as they inflect, so Japanese calls their accented pattern kifuku. */
private val VERB_OR_ADJECTIVE = setOf("verb", "adj")

/** Meanings from the offline dictionary, or Wiktionary's online; the speaker reads a headword aloud, the plus adds a meaning to the translation. */
@Composable
internal fun DictionarySection(
    lookup: DictionaryLookup,
    languageCode: String?,
    toneColors: Boolean = false,
    zhuyin: Boolean = false,
    onAdd: (String) -> Unit,
    /** The Wiktionary page the entries come from, with the language's English name, when they come from it online. */
    credit: Pair<WiktionaryPage, String?>? = null,
    /** What follows the entries, such as the way to the word's conjugation. */
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val speaker = rememberSpeaker(koinInject(), koinInject())
    SectionCard({ DictionaryBadge() }, tr("Dictionary"), count = lookup.entries.size, tint = null, filled = false) {
        lookup.entries.forEachIndexed { index, entry ->
            if (index > 0) AppDivider(Modifier.padding(vertical = AppSpacing.s))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), modifier = Modifier.padding(top = AppSpacing.xs)) {
                Text(entry.word, style = AppText.itemTitle)
                entry.reading?.let { written ->
                    val chinese = languageCode in setOf("zh", "yue")
                    val reading = if (chinese && zhuyin) Chinese.zhuyin(written) else written
                    if (toneColors && chinese) Text(toneColored(reading), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("tone-reading"))
                    else Text(reading, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                }
                Text(entry.pos, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                // JMdict's mark of a common word, as 10ten Japanese Reader shows it.
                if (entry.common) Tag(tr("common"), color = colors.tertiary, modifier = Modifier.testTag("common-word"), size = TagSize.Small, shape = AppShapes.small)
                if (entry.ipa != null) Text(entry.ipa!!, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                SpeakButton(entry.word, languageCode, speaker, Modifier.size(AUDIO_BUTTON))
            }
            entry.dictionary?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = colors.tertiary, modifier = Modifier.padding(start = AppSpacing.xxs).testTag("entry-dictionary"))
            }
            if (entry.frequencies.isNotEmpty()) FrequencyLabels(entry.frequencies)
            if (entry.pitch.isNotEmpty()) {
                PitchAccents(entry.pitch, isVerbOrAdjective = entry.pos in VERB_OR_ADJECTIVE, modifier = Modifier.padding(start = AppSpacing.xxs, top = AppSpacing.xxs, bottom = AppSpacing.xxs))
            }
            if (entry.inflection.isNotEmpty()) {
                // The inflections the language's rules undid to reach this word, as Yomitan lists them.
                Text(
                    entry.inflection.joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(start = AppSpacing.xxs, top = AppSpacing.xxs).testTag("inflection-${entry.word}"),
                )
            }
            // The meanings sit close together, apart from the card's wider gaps between its parts.
            Column {
                entry.senses.forEachIndexed { senseIndex, sense ->
                    val gloss = sense.glosses.joinToString("; ")
                    if (senseIndex > 0) AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                    // The meanings line up with the headword above them.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs)) {
                        Column(Modifier.weight(1f)) {
                            Text(gloss, style = MaterialTheme.typography.bodyLarge)
                            if (sense.tags.isNotEmpty()) {
                                Text(sense.tags.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            // The dictionary's own examples of this sense, each with its translation below it.
                            sense.examples.forEach { example ->
                                Row(Modifier.padding(top = AppSpacing.s).height(IntrinsicSize.Min)) {
                                    Box(Modifier.width(QUOTE_RULE_WIDTH).fillMaxHeight().background(colors.primary.copy(alpha = AppAlpha.disabled)))
                                    Column(Modifier.padding(start = AppSpacing.s)) {
                                        Text(example.text.inLearnedScript(), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                                        example.translation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.size(AppSpacing.s))
                        AppTooltip(tr("Add to translation")) {
                            AppIconButton(Icons.Default.Add, tr("Add to translation"), onClick = { onAdd(gloss) }, style = IconButtonStyle.Tonal, size = IconButtonSize.Small)
                        }
                    }
                }
            }
        }
        if (extra != null) {
            AppDivider()
            extra()
        }
        credit?.let { (page, languageName) -> WiktionaryCredit(page, languageName) }
    }
}

/**
 * Example sentences from Tatoeba in a tinted card, with the term highlighted. Three are shown
 * until expanded; translations appear in a tooltip (hover or long press) and the button at the
 * bottom opens the full example search.
 */
@Composable
private fun ExamplesSection(
    state: TermFormUiState,
    language: Language?,
    onOpenExamples: ((Long, String) -> Unit)?,
    /** A right click on a word of an example, with the example. */
    onMarkWord: (String, String) -> Unit,
    /** A click on a word, or a phrase dragged over, with the example. */
    onOpenWord: (String, String) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val term = state.draft.text.replace("\u200B", "")
    var expanded by remember(term) { mutableStateOf(false) }
    val canExpand = state.examples.size > VISIBLE_EXAMPLES
    val languageId = language?.id
    val audio = rememberExampleAudio()
    val wordTranslations = koinInject<WordTranslationService>()
    val translateWord: suspend (String) -> String? = { word -> language?.let { wordTranslations.translate(it, word) } }
    val showHighlights = koinInject<SettingsRepository>().settings.collectAsStateWithLifecycle().value.showHighlights
    val languageCode = language?.let { LanguageCodes.codeFor(it.name) }
    audio.PrepareSpeech(if (expanded) state.examples else state.examples.take(VISIBLE_EXAMPLES), languageCode)
    val total = state.examplesTotal ?: state.examples.size
    val canOpen = onOpenExamples != null && languageId != null && term.isNotBlank()

    // The same plain card as the pane's other lookups.
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surface).border(AppBorders.hairline(), AppShapes.card).padding(sectionPadding()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionTitle(AppIcons.Page, tr("Examples"), if (state.loadingExamples) null else total) }
            if (canExpand) {
                AppIconButton(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) tr("Show fewer examples") else tr("Show all {0} examples", state.examples.size),
                    onClick = { expanded = !expanded },
                    size = IconButtonSize.Small,
                    tint = colors.primary,
                )
            }
        }
        if (state.examplesFailed && !state.loadingExamples) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Tatoeba isn't responding right now."), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                QuietButton(tr("Retry"), onClick = onRetry, size = ButtonSize.Compact)
            }
        }
        when {
            state.loadingExamples -> Text(tr("Looking up examples..."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            state.examples.isEmpty() -> if (!state.examplesFailed) Text(tr("No examples found."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            else -> {
                if (state.examplesFromCourses) Text(tr("From your courses"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                val direction = if (language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr
                val visible = if (expanded) state.examples else state.examples.take(VISIBLE_EXAMPLES)
                visible.forEachIndexed { index, example ->
                    if (index > 0) AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                    val sentence = @Composable {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs)) {
                            HoverTranslatedText(
                                emphasize(example.text, term, SpanStyle(fontWeight = AppText.labelStrong.fontWeight))
                                    .withStatuses(state.exampleWords[example.text].orEmpty(), TayraTheme.current.statusColors, showHighlights)
                                    .withSelected(term, TayraTheme.current.selectedText),
                                translate = translateWord,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
                                modifier = Modifier.weight(1f).padding(end = AppSpacing.m),
                                onWordClick = { word -> onOpenWord(word, example.text) },
                                onWordSecondaryClick = { word -> onMarkWord(word, example.text) },
                                onPhraseSelect = { phrase -> onOpenWord(phrase, example.text) },
                            )
                            val sound = audio.soundOf(example)
                            // The description sits on the button so that it stays while the spinner replaces the icon.
                            AppIconButton(
                                if (sound.playing) AppIcons.Stop else sound.icon,
                                contentDescription = null,
                                onClick = { audio.toggle(example, languageCode) },
                                modifier = Modifier.semantics { contentDescription = sound.description },
                                style = IconButtonStyle.Tonal,
                                selected = sound.playing,
                                loading = sound.loading,
                            )
                        }
                    }
                    val translation = example.translation
                    if (translation == null) {
                        sentence()
                    } else {
                        AppTooltip(translation, positionProvider = rememberLeftTooltipPositionProvider()) { sentence() }
                    }
                }
            }
        }
        if (canOpen && (state.examples.isNotEmpty() || state.examplesTotal != null)) {
            OutlineActionButton(if (state.examplesTotal != null) tr("View all {0} examples", total) else tr("View more examples")) { onOpenExamples!!.invoke(languageId!!, term) }
        }
    }
}

private const val VISIBLE_EXAMPLES = 3
private val LOADING_MIN_HEIGHT = 200.dp
private val AUDIO_BUTTON = 32.dp
/** A status named in full takes this much of the row against a lettered one's share. */
private const val NAMED_STATUS_WEIGHT = 2.6f
private val LARGE_STATUS_BUTTON_HEIGHT = 52.dp
private val KANJI_READING_LABEL = 32.dp
private val QUOTE_RULE_WIDTH = 2.dp

/**
 * Places a tooltip to the left of its anchor, vertically centred, so the translation sits
 * beside the sentence instead of covering the examples above it. Falls back to above the
 * anchor when there is no room on the left.
 */
@Composable
private fun rememberLeftTooltipPositionProvider(): PopupPositionProvider {
    val gap = with(LocalDensity.current) { AppSpacing.s.roundToPx() }
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
private fun emphasize(sentence: String, term: String, highlight: SpanStyle) = buildAnnotatedString {
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
        if (matches) withStyle(highlight) { append(word) } else append(word)
        index = match.range.last + 1
    }
    append(sentence.substring(index))
}

