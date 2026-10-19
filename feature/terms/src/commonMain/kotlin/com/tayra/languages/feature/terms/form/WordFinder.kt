package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.layout.Arrangement
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.ChipSize
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.coroutines.delay

/** "Find from Russian": opens the word finder, which gives the learned language's words for a word of the reader's own. */
@Composable
internal fun WordFinderButton(state: TermFormUiState, viewModel: TermFormViewModel) {
    val finder = state.finderPack ?: return
    val learning = state.language?.name ?: return
    val native = LanguageCatalog.nativeOption(state.nativeLanguage).name
    var open by remember { mutableStateOf(false) }
    QuietButton(tr("Find a {0} word from {1}", tr(learning), tr(native)), onClick = { open = true }, modifier = Modifier.testTag("word-finder"), icon = Icons.Default.Search)
    if (open) {
        WordFinderDialog(
            finder, native, learning,
            onSearch = viewModel::findWord,
            onDownload = viewModel::downloadFinder,
            onPick = { word ->
                open = false
                viewModel.openFoundWord(word)
            },
            onDismiss = { open = false },
        )
    }
}

/** A search over the word finder: each meaning of the native word found, with the learned language's words for it to pick from. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordFinderDialog(
    finder: PackStatus,
    nativeName: String,
    learningName: String,
    onSearch: suspend (String) -> DictionaryLookup,
    onDownload: () -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Pair<String, DictionaryLookup>?>(null) }
    val installed = finder.state is PackState.Installed
    val focus = remember { FocusRequester() }
    LaunchedEffect(query, installed) {
        if (!installed) return@LaunchedEffect
        delay(SEARCH_DELAY_MS)
        result = query to onSearch(query)
    }
    LaunchedEffect(installed) { if (installed) runCatching { focus.requestFocus() } }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Find a {0} word", tr(learningName))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                when (val state = finder.state) {
                    is PackState.Installed -> {
                        AppTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text(tr("A word in {0}", tr(nativeName))) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("word-finder-query"),
                        )
                        val (searched, lookup) = result ?: ("" to DictionaryLookup.EMPTY)
                        Column(Modifier.heightIn(max = RESULTS_MAX_HEIGHT).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                            if (searched.isNotBlank() && lookup.isEmpty) {
                                Text(tr("The word finder does not know \"{0}\".", searched), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                            lookup.entries.forEachIndexed { index, entry ->
                                if (index > 0) AppDivider()
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                                    Text(entry.word, style = AppText.smallTitle)
                                    Text(entry.pos, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                entry.senses.forEach { sense ->
                                    Column(Modifier.padding(start = AppSpacing.s)) {
                                        sense.tags.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                                            sense.glosses.forEach { word ->
                                                SelectableChip(word, selected = false, onClick = { onPick(word) }, modifier = Modifier.testTag("found-$word"), size = ChipSize.Small)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is PackState.Downloading -> {
                        Text(tr("Downloading the word finder..."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        AppProgressBar(state.progress)
                    }
                    else -> {
                        Text(
                            if (state is PackState.Failed) tr("The download failed: {0}", state.message)
                            else tr("The word finder gives {0} words for {1} ones, from the translations in the {1} Wiktionary. Download it to use it offline.", tr(learningName), tr(nativeName)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state is PackState.Failed) colors.error else colors.onSurfaceVariant,
                        )
                        PrimaryButton(
                            if (state is PackState.Failed) tr("Try again") else tr("Download word finder"),
                            onClick = onDownload,
                            modifier = Modifier.testTag("word-finder-download"),
                            icon = AppIcons.Download,
                        )
                    }
                }
            }
        },
        confirmButton = { QuietButton(tr("Close"), onClick = onDismiss) },
    )
}

private const val SEARCH_DELAY_MS = 250L
private val RESULTS_MAX_HEIGHT = 380.dp
