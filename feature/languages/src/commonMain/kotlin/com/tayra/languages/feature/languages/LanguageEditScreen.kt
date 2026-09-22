package com.tayra.languages.feature.languages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.parse.ParserRegistry
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LanguageEditScreen(
    languageId: Long?,
    predefinedName: String?,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LanguageEditViewModel = koinViewModel { parametersOf(languageId, predefinedName) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { if (it is LanguageEditEvent.Saved) onSaved() }

    Scaffold(
        topBar = {
            AppTopBar(title = if (state.isNew) "New language" else state.language.name, onNavigate = onNavigate, onBack = onBack)
        },
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val language = state.language
        val wide = LocalWindowWidth.current.isExpanded
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                .widthIn(max = if (wide) 900.dp else Int.MAX_VALUE.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ErrorMessage(state.error)
            OutlinedTextField(
                value = language.name,
                onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Dictionaries", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use [LUTE] as the placeholder for the looked-up text. Embedded dictionaries are opened in the reading pane; pop-ups open in a new window.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            language.dictionaries.forEachIndexed { index, dictionary ->
                DictionaryRow(
                    dictionary = dictionary,
                    canMoveUp = index > 0,
                    canMoveDown = index < language.dictionaries.lastIndex,
                    onChange = { changed -> viewModel.updateDictionary(index) { changed } },
                    onMove = { delta -> viewModel.moveDictionary(index, delta) },
                    onRemove = { viewModel.removeDictionary(index) },
                    compact = !wide,
                )
            }
            OutlinedButton(onClick = viewModel::addDictionary) { Text("Add dictionary") }

            Spacer(Modifier.height(8.dp))
            SwitchRow("Show pronunciation field", language.showRomanization) { v -> viewModel.update { it.copy(showRomanization = v) } }
            SwitchRow("Right-to-left", language.rightToLeft) { v -> viewModel.update { it.copy(rightToLeft = v) } }

            Dropdown(
                options = ParserRegistry.supported,
                selected = ParserRegistry.supported.firstOrNull { it.first == language.parserType },
                onSelect = { (key, _) -> viewModel.update { it.copy(parserType = key) } },
                label = "Parse as",
                optionLabel = { it.second },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = language.characterSubstitutions,
                onValueChange = { v -> viewModel.update { it.copy(characterSubstitutions = v) } },
                label = { Text("Character substitutions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = language.regexpSplitSentences,
                onValueChange = { v -> viewModel.update { it.copy(regexpSplitSentences = v) } },
                label = { Text("Split sentences at (default: all Unicode sentence terminators)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = language.exceptionsSplitSentences,
                onValueChange = { v -> viewModel.update { it.copy(exceptionsSplitSentences = v) } },
                label = { Text("Split sentence exceptions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = language.wordCharacters,
                onValueChange = { v -> viewModel.update { it.copy(wordCharacters = v) } },
                label = { Text("Word characters (default: all Unicode letters and marks)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save, enabled = !state.saving) { Text("Save") }
                OutlinedButton(onClick = onBack) { Text("Cancel") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun DictionaryRow(
    dictionary: LanguageDictionary,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (LanguageDictionary) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    compact: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val selectors: @Composable () -> Unit = {
        Dropdown(
            options = DictionaryUse.entries,
            selected = dictionary.useFor,
            onSelect = { onChange(dictionary.copy(useFor = it)) },
            label = "Use for",
            optionLabel = { it.key.replaceFirstChar { c -> c.uppercase() } },
            modifier = Modifier.width(150.dp),
        )
        Dropdown(
            options = DictionaryType.entries,
            selected = dictionary.type,
            onSelect = { onChange(dictionary.copy(type = it)) },
            label = "Show as",
            optionLabel = { if (it == DictionaryType.EMBEDDED) "Embedded" else "Pop-up window" },
            modifier = Modifier.width(170.dp),
        )
    }
    val urlField: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = dictionary.url,
            onValueChange = { onChange(dictionary.copy(url = it)) },
            label = { Text("URL") },
            singleLine = true,
            modifier = modifier,
        )
    }
    val controls: @Composable () -> Unit = {
        Checkbox(checked = dictionary.isActive, onCheckedChange = { onChange(dictionary.copy(isActive = it)) })
        Text("Active", style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
        OutlinedButton(onClick = { uriHandler.openUri(dictionary.lookupUrl("test")) }, enabled = dictionary.url.isNotBlank()) { Text("Test") }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
    }
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { selectors() }
            urlField(Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { controls() }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectors()
            urlField(Modifier.weight(1f))
            controls()
        }
    }
}
