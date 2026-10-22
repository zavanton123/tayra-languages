package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.Conjugations
import com.tayra.languages.core.domain.dictionary.InflectionRow
import com.tayra.languages.core.domain.dictionary.InflectionTable
import com.tayra.languages.core.domain.dictionary.WiktionaryCodes
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.ChipSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.grammarColumn
import com.tayra.languages.core.ui.components.grammarTag
import com.tayra.languages.core.ui.components.grammarTags
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * A word's [table] at full size: a verb's by mood and tense, each tense person by number with its
 * pronouns, another word's as its whole grid. A click on a form reads it aloud; the form the pane
 * is about ([term]) is tinted, and the verb opens on its tense.
 */
@Composable
internal fun ConjugationDialog(
    table: InflectionTable,
    lemma: String,
    term: String,
    languageName: String?,
    languageCode: String?,
    onSpeak: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val verb = table.pos == "verb"
    val clicked = WiktionaryCodes.plain(term)
    val isClicked = { form: String -> WiktionaryCodes.plain(form) == clicked || WiktionaryCodes.plain(form.substringBefore(" [")) == clicked }
    AppDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.testTag("conjugation-dialog")) {
        Column(Modifier.padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Text((if (verb) tr("Conjugation") else tr("Inflection")).uppercase(), style = AppText.overline, color = colors.primary)
                    Text(lemma, style = AppText.screenTitle)
                    val subtitle = listOfNotNull(languageName?.let { tr(it) }, table.pos.takeIf { it.isNotBlank() }?.let(::partOfSpeech)).joinToString(" · ")
                    if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                if (onSpeak != null) AppIconButton(AppIcons.VolumeUp, tr("Read aloud"), onClick = { onSpeak(lemma) }, style = IconButtonStyle.Tonal)
                AppIconButton(Icons.Default.Close, tr("Close"), onClick = onDismiss, style = IconButtonStyle.Tonal)
            }
            Column(Modifier.weight(1f, fill = false).heightIn(max = BODY_MAX_HEIGHT).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                if (verb) VerbBody(table, languageCode, isClicked, onSpeak) else GridBody(table, isClicked, onSpeak)
            }
            AppDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (onSpeak != null) {
                    IconTile(AppIcons.VolumeUp, size = 36)
                    Text(tr("Select a form to hear it"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PrimaryButton(tr("Done"), onClick = onDismiss)
            }
        }
    }
}

/** Mood tabs, the chosen mood's tenses as chips, and the chosen tense person by person. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerbBody(table: InflectionTable, languageCode: String?, isClicked: (String) -> Boolean, onSpeak: ((String) -> Unit)?) {
    val layout = remember(table) { Conjugations.layout(table) }
    val nonFinite = layout.nonFinite.isNotEmpty() || layout.other.isNotEmpty()
    val tabs = layout.moods.map { grammarTag(it.mood).replaceFirstChar(Char::uppercase) } +
        listOfNotNull(tr("Non-finite").takeIf { nonFinite })
    // The dialog opens where the pane's form is.
    val start = remember(layout) {
        layout.moods.withIndex().firstNotNullOfOrNull { (m, mood) ->
            mood.tenses.indexOfFirst { tense -> tense.row.cells.any { it.any(isClicked) } }.takeIf { it >= 0 }?.let { m to it }
        } ?: (if (layout.moods.isEmpty()) 0 else if (layout.other.any { isClicked(it.form) } || layout.nonFinite.any { r -> r.cells.any { it.any(isClicked) } }) layout.moods.size else 0) to 0
    }
    var tab by remember(layout) { mutableIntStateOf(start.first) }
    var tense by remember(layout) { mutableIntStateOf(start.second) }
    if (tabs.size > 1) UnderlineTabs(tabs, tab) { tab = it; tense = 0 }
    val mood = layout.moods.getOrNull(tab)
    if (mood != null) {
        if (mood.tenses.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                mood.tenses.forEachIndexed { index, t ->
                    val label = if (t.tags.isEmpty()) grammarTag(if (mood.mood == "imperative") "affirmative" else mood.mood) else grammarTags(t.tags)
                    SelectableChip(label.replaceFirstChar(Char::uppercase), selected = index == tense, onClick = { tense = index }, size = ChipSize.Small, modifier = Modifier.testTag("tense-$index"))
                }
            }
        }
        mood.tenses.getOrNull(tense)?.let { RowForms(table.columns, it.row, languageCode, isClicked, onSpeak) }
    } else {
        layout.nonFinite.forEach { row ->
            Text(grammarTags(row.tags).replaceFirstChar(Char::uppercase), style = AppText.smallTitle)
            RowForms(table.columns, row, languageCode, isClicked, onSpeak)
        }
        if (layout.other.isNotEmpty()) {
            Column {
                layout.other.forEach { form ->
                    AppDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        Text(grammarTags(form.tags).replaceFirstChar(Char::uppercase), Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium)
                        FormText(listOf(form.form), isClicked, onSpeak)
                    }
                }
            }
        }
    }
}

/** One tense: person by singular and plural with the pronouns where the columns allow, else column by column. */
@Composable
private fun RowForms(columns: List<List<String>>, row: InflectionRow, languageCode: String?, isClicked: (String) -> Boolean, onSpeak: ((String) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val persons = Conjugations.byPerson(columns, row, languageCode)
    // A phone leaves the forms more room beside the person.
    val labelWidth = if (LocalWindowWidth.current.isCompact) LABEL_WIDTH_COMPACT else LABEL_WIDTH
    Column {
        if (persons != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s)) {
                Text(tr("Person"), Modifier.width(labelWidth), style = AppText.labelStrong)
                Text(tr("Singular"), Modifier.weight(1f), style = AppText.labelStrong)
                Text(tr("Plural"), Modifier.weight(1f), style = AppText.labelStrong)
            }
            persons.forEach { p ->
                AppDivider()
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Text(personLabel(p.person), Modifier.width(labelWidth), style = MaterialTheme.typography.bodyMedium)
                    PersonCell(p.singularPronoun, p.singular, isClicked, onSpeak, Modifier.weight(1f))
                    PersonCell(p.pluralPronoun, p.plural, isClicked, onSpeak, Modifier.weight(1f))
                }
            }
        } else {
            columns.forEachIndexed { index, column ->
                if (index > 0) AppDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    Text(grammarTags(column), Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    FormText(row.cells[index], isClicked, onSpeak)
                }
            }
        }
    }
}

@Composable
private fun PersonCell(pronoun: String?, forms: List<String>, isClicked: (String) -> Boolean, onSpeak: ((String) -> Unit)?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        if (forms.isEmpty()) {
            Text("—", style = AppText.itemTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (pronoun != null) Text(pronoun, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FormText(forms, isClicked, onSpeak)
        }
    }
}

/** The forms of a cell, each read aloud on a click; the pane's own form is tinted. */
@Composable
private fun FormText(forms: List<String>, isClicked: (String) -> Boolean, onSpeak: ((String) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        forms.forEach { form ->
            val highlighted = isClicked(form)
            Text(
                form,
                Modifier.clip(AppShapes.small).then(if (highlighted) Modifier.background(colors.primary.copy(alpha = AppAlpha.tint)) else Modifier)
                    .then(if (onSpeak != null) Modifier.clickable { onSpeak(form.substringBefore(" [")) } else Modifier)
                    .padding(horizontal = AppSpacing.xxs).testTag(if (highlighted) "dialog-form-clicked" else "dialog-form"),
                style = AppText.itemTitle,
                color = colors.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Wiktionary's name of a part of speech ("adj") in words. */
private fun partOfSpeech(pos: String): String = when (pos) {
    "verb" -> tr("verb")
    "noun" -> tr("noun")
    "adj" -> tr("adjective")
    "adv" -> tr("adverb")
    "pron" -> tr("pronoun")
    "num" -> tr("numeral")
    else -> pos
}

private fun personLabel(person: String): String = when (person) {
    "first-person" -> tr("1st person")
    "second-person" -> tr("2nd person")
    else -> tr("3rd person")
}

/** A noun's or an adjective's grid as it is: rows by case, columns by number. */
@Composable
private fun GridBody(table: InflectionTable, isClicked: (String) -> Boolean, onSpeak: ((String) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    if (table.rows.isNotEmpty()) {
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column {
                Row(Modifier.padding(vertical = AppSpacing.s)) {
                    Spacer(Modifier.width(LABEL_WIDTH))
                    table.columns.forEach { Text(grammarColumn(it), Modifier.width(GRID_CELL_WIDTH), style = AppText.labelStrong) }
                }
                table.rows.forEach { row ->
                    AppDivider(Modifier.width(LABEL_WIDTH + GRID_CELL_WIDTH * table.columns.size))
                    Row(Modifier.padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Text(grammarTags(row.tags).replaceFirstChar(Char::uppercase), Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium)
                        row.cells.forEach { forms -> Box(Modifier.width(GRID_CELL_WIDTH)) { if (forms.isEmpty()) Text("—", color = colors.onSurfaceVariant) else FormText(forms, isClicked, onSpeak) } }
                    }
                }
            }
        }
    }
    table.other.forEach { form ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Text(grammarTags(form.tags).replaceFirstChar(Char::uppercase), Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.bodyMedium)
            FormText(listOf(form.form), isClicked, onSpeak)
        }
    }
}

/** Tabs as words over a line, the chosen one in the primary colour and underlined. */
@Composable
private fun UnderlineTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            labels.forEachIndexed { index, label ->
                val chosen = index == selected
                Column(Modifier.width(IntrinsicSize.Max).clip(AppShapes.small).clickable { onSelect(index) }.testTag("mood-$index")) {
                    Text(label, Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), style = AppText.emphasis, color = if (chosen) colors.primary else colors.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().height(TAB_LINE).background(if (chosen) colors.primary else Color.Transparent))
                }
            }
        }
        AppDivider()
    }
}

private val LABEL_WIDTH = 140.dp
private val LABEL_WIDTH_COMPACT = 96.dp
private val GRID_CELL_WIDTH = 150.dp
private val TAB_LINE = 2.dp
private val BODY_MAX_HEIGHT = 520.dp
