package com.tayra.languages.feature.terms.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import org.koin.compose.koinInject
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.components.HoverTranslatedText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.YesNo
import com.tayra.languages.core.ui.audio.AudioPlayback
import com.tayra.languages.core.ui.audio.rememberAudioPlayback
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import io.ktor.http.encodeURLParameter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Searches Tatoeba example sentences with every filter the API offers. */
@Composable
fun ExamplesSearchScreen(
    languageId: Long,
    text: String,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamplesSearchViewModel = koinViewModel(key = "examples-$languageId-$text") { parametersOf(languageId, text) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query = state.query
    val playback = rememberAudioPlayback()
    val compact = LocalWindowWidth.current.isCompact
    val wordTranslations = koinInject<WordTranslationService>()
    val translateWord: suspend (String) -> String? = { word -> state.language?.let { wordTranslations.translate(it, word) } }

    Scaffold(
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) },
    ) { padding ->
        if (state.loading || query == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val direction = if (query.language.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
        val gutter = if (compact) 16.dp else 32.dp
        // One scrolling list holds the filters and the results so both fit on small screens.
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp)) {
            item { PageHeader(query, compact, onBack, onNavigate) }
            item { ErrorMessage(state.error) }
            item { SearchBar(query, viewModel) }
            item { FilterRow(query, viewModel) }
            item {
                Text(
                    when {
                        state.searching -> "Searching..."
                        state.total != null -> "${state.total} sentence${if (state.total == 1) "" else "s"}"
                        else -> ""
                    },
                    Modifier.padding(top = 16.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.searching -> item { LoadingIndicator(Modifier.fillMaxWidth().padding(32.dp)) }
                state.results.isEmpty() -> item { EmptyMessage("No examples match these filters.", Modifier.fillMaxWidth()) }
                else -> {
                    items(state.results) { example ->
                        ExampleCard(
                            text = emphasize(example.text, query.text),
                            translateWord = translateWord,
                            translation = example.translation,
                            audioUrl = example.audioUrl,
                            direction = direction,
                            playback = playback,
                            compact = compact,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.hasMore) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore, shape = RoundedCornerShape(10.dp)) {
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

@Composable
private fun PageHeader(query: ExampleSearchQuery, compact: Boolean, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val target = LanguageCodes.option(query.targetLanguage)?.name ?: query.targetLanguage
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.Top) {
        if (!compact) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Terms", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.clickable { onNavigate(Route.Terms()) })
                Text("/", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                Text("Examples", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Text(
                "Examples for ‘${query.text}’",
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("${query.language.name} → $target", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                val from = LanguageCodes.tatoebaCodeFor(query.language.name)?.let { "&from=$it" } ?: ""
                uriHandler.openUri("https://tatoeba.org/en/sentences/search?query=${query.text.encodeURLParameter()}$from")
            }.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(AppIcons.Globe, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            if (!compact) Text("Powered by", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text("Tatoeba", style = MaterialTheme.typography.bodyMedium, color = colors.primary, fontWeight = FontWeight.Medium)
            Icon(AppIcons.OpenInNew, contentDescription = "Open on Tatoeba", tint = colors.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SearchBar(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query.text,
                onValueChange = { v -> viewModel.updateQuery { it.copy(text = v) } },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.text.isEmpty()) Text("Search sentences", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                },
            )
        }
        Button(onClick = viewModel::search, enabled = query.text.isNotBlank(), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(48.dp)) {
            Text("Search")
        }
    }
}

private val WORD_COUNTS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 50, 100)
private val PAGE_SIZES = listOf(5, 10, 20, 30, 50, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Select("Min words", (listOf<Int?>(null) + WORD_COUNTS), query.minWords, { it?.toString() ?: "Any" }, 110.dp) { v -> viewModel.updateFilters { it.copy(minWords = v) } }
        Select("Max words", (listOf<Int?>(null) + WORD_COUNTS), query.maxWords, { it?.toString() ?: "Any" }, 110.dp) { v -> viewModel.updateFilters { it.copy(maxWords = v) } }
        Select("Sort", ExampleSort.entries, query.sort, { it.label }, 170.dp) { v -> viewModel.updateFilters { it.copy(sort = v) } }
        Select("Per page", PAGE_SIZES, query.limit, { it.toString() }, 110.dp) { v -> viewModel.updateFilters { it.copy(limit = v) } }
        Select("Has audio", listOf<YesNo?>(null) + YesNo.entries, query.hasAudio, { it?.label ?: "Any" }, 150.dp) { v -> viewModel.updateFilters { it.copy(hasAudio = v) } }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = viewModel::resetFilters, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(44.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset filters")
        }
    }
}

/** A labelled compact select box. */
@Composable
private fun <T> Select(label: String, options: List<T>, selected: T, optionLabel: (T) -> String, width: androidx.compose.ui.unit.Dp, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Box {
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                    .clickable { open = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(optionLabel(selected), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            AppMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(text: AnnotatedString, translateWord: suspend (String) -> String?, translation: String?, audioUrl: String?, direction: TextDirection, playback: AudioPlayback, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val playing = audioUrl != null && playback.isPlaying(audioUrl)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (playing) colors.primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HoverTranslatedText(text, translate = translateWord, style = MaterialTheme.typography.bodyLarge.copy(textDirection = direction, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25))
            translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        }
        Spacer(Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                icon = Icons.Default.PlayArrow,
                description = if (playing) "Stop recording" else "Play recording",
                enabled = audioUrl != null,
                active = playing,
                onClick = { audioUrl?.let(playback::toggle) },
            )
            if (!compact) {
                ActionButton(icon = AppIcons.ContentCopy, description = "Copy sentence", onClick = { clipboard.setText(AnnotatedString(text.text)) })
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(Modifier.size(12.dp).background(colors.primary, RoundedCornerShape(2.dp)))
        } else {
            Icon(icon, contentDescription = description, tint = if (enabled) colors.primary else colors.outlineVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** Highlights the term and its inflections (same stem) in the sentence. */
@Composable
private fun emphasize(sentence: String, term: String): AnnotatedString {
    val highlight = SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF166534), background = Color(0xFF16A34A).copy(alpha = 0.16f))
    return remember(sentence, term) {
        buildAnnotatedString {
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
    }
}
