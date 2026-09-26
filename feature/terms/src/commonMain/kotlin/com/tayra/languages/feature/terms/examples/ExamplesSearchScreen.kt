package com.tayra.languages.feature.terms.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.YesNo
import com.tayra.languages.core.ui.audio.PlayButton
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.navigation.Route
import io.ktor.http.encodeURLParameter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Searches Tatoeba example sentences with every filter the API offers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamplesSearchScreen(
    languageId: Long,
    text: String,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamplesSearchViewModel = koinViewModel(key = "examples-$languageId-$text") { parametersOf(languageId, text) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val query = state.query
    val playback = rememberAudioPlayback()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Examples: ${query?.text ?: text}",
                onNavigate = onNavigate,
                onBack = onBack,
                showMenu = false,
                actions = {
                    if (query != null) {
                        TextButton(onClick = {
                            val from = LanguageCodes.tatoebaCodeFor(query.language.name)?.let { "&from=$it" } ?: ""
                            uriHandler.openUri("https://tatoeba.org/en/sentences/search?query=${query.text.encodeURLParameter()}$from")
                        }) { Text("Tatoeba") }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading || query == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val direction = if (query.language.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
        // One scrolling list holds the filters and the results so both fit on small screens.
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item { ErrorMessage(state.error, Modifier.padding(horizontal = 16.dp)) }
            item { FilterPanel(query, viewModel) }
            item {
                Text(
                    when {
                        state.searching -> "Searching..."
                        state.total != null -> "${state.total} sentences"
                        else -> ""
                    },
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.searching -> item { LoadingIndicator(Modifier.fillMaxWidth().padding(32.dp)) }
                state.results.isEmpty() -> item { EmptyMessage("No examples match these filters.", Modifier.fillMaxWidth()) }
                else -> {
                    items(state.results) { example ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (example.audioUrl != null) {
                                PlayButton(example.audioUrl!!, playback)
                            } else {
                                Spacer(Modifier.width(48.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(emphasize(example.text, query.text), style = MaterialTheme.typography.bodyLarge.copy(textDirection = direction))
                                example.translation?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    if (state.hasMore) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore) {
                                    Text(if (state.loadingMore) "Loading..." else "Load more")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query.text,
                onValueChange = { v -> viewModel.updateQuery { it.copy(text = v) } },
                label = { Text("Search (${query.language.name} → ${LanguageCodes.option(query.targetLanguage)?.name ?: query.targetLanguage})") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::search, enabled = query.text.isNotBlank()) { Text("Search") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Min words", query.minWords) { v -> viewModel.updateQuery { it.copy(minWords = v) } }
            NumberField("Max words", query.maxWords) { v -> viewModel.updateQuery { it.copy(maxWords = v) } }
            Dropdown(ExampleSort.entries, query.sort, { v -> viewModel.updateQuery { it.copy(sort = v) } }, "Sort", { it.label }, Modifier.width(190.dp))
            NumberField("Per page", query.limit) { v -> viewModel.updateQuery { it.copy(limit = (v ?: 30).coerceIn(1, 100)) } }
            YesNoField("Has audio", query.hasAudio) { v -> viewModel.updateQuery { it.copy(hasAudio = v) } }
        }
        HorizontalDivider()
    }
}

@Composable
private fun NumberField(label: String, value: Int?, onChange: (Int?) -> Unit) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { v -> if (v.isEmpty()) onChange(null) else v.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(120.dp),
    )
}

/** Any / Yes / No selector. */
@Composable
private fun YesNoField(label: String, value: YesNo?, onChange: (YesNo?) -> Unit) {
    Dropdown(
        options = listOf<YesNo?>(null) + YesNo.entries,
        selected = value,
        onSelect = onChange,
        label = label,
        optionLabel = { it?.label ?: "Any" },
        modifier = Modifier.width(200.dp),
    )
}

/** Bolds the term and its inflections (same stem) in the sentence. */
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
