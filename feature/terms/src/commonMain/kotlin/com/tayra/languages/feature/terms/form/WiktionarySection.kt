package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.InflectionTable
import com.tayra.languages.core.domain.dictionary.WiktionaryCodes
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.grammarColumn
import com.tayra.languages.core.ui.components.grammarTags
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * What Wiktionary says of the term online: which form of which lemma it is, its
 * pronunciations, its synonyms, the lemma's inflection table and the expressions made with it, the
 * last two collapsible. The pane shows it only for a word with a table or expressions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WiktionarySection(
    wiktionary: WiktionaryUi,
    term: String,
    languageName: String?,
    onOpenWord: (String) -> Unit,
    onToggleTable: () -> Unit,
    onToggleExpressions: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val page = wiktionary.page
    SectionCard({ BadgeIcon(AppIcons.MenuBook) }, tr("Wiktionary"), tint = null, filled = false) {
        page?.formOf?.forEach { sense -> FormOfLine(sense, onOpenWord) }
        val pronunciations = page?.pronunciations.orEmpty().take(MAX_PRONUNCIATIONS)
        if (pronunciations.isNotEmpty()) {
            Column(Modifier.testTag("wiktionary-ipa"), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                pronunciations.forEach { pronunciation ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Text(pronunciation.ipa, style = MaterialTheme.typography.bodyMedium)
                        if (pronunciation.tags.isNotEmpty()) Text(pronunciation.tags.joinToString(", ") { it.replace('-', ' ') }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        val synonyms = page?.synonyms.orEmpty().take(MAX_SYNONYMS)
        if (synonyms.isNotEmpty()) {
            Text(tr("Synonyms"), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                synonyms.forEach { WordChip(it, Modifier.testTag("synonym-$it")) { onOpenWord(it) } }
            }
        }
        val tables = wiktionary.tables
        if (tables.isNotEmpty()) {
            val verb = tables.any { it.pos == "verb" } || page?.entries?.any { it.pos == "verb" } == true
            val title = if (verb) tr("Conjugation") else tr("Inflection")
            val lemma = wiktionary.tableWord?.takeIf { it != wiktionary.headword }
            Collapsible(title, lemma, wiktionary.tableOpen, if (wiktionary.tableOpen) tr("Hide the table") else tr("Show the table"), "wiktionary-table", onToggleTable) {
                tables.forEach { table ->
                    if (tables.size > 1 && table.pos.isNotEmpty()) Text(table.pos, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    InflectionTableView(table, term)
                }
            }
        }
        if (wiktionary.expressions.isNotEmpty()) {
            val action = if (wiktionary.expressionsOpen) tr("Hide expressions") else tr("Show expressions")
            Collapsible(tr("Expressions"), null, wiktionary.expressionsOpen, action, "wiktionary-expressions", onToggleExpressions) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    wiktionary.expressions.forEach { WordChip(it, Modifier.testTag("expression-$it")) { onOpenWord(it) } }
                }
            }
        }
        wiktionary.credit?.let { WiktionaryCredit(it, languageName) }
    }
}

/** "third-person plural present indicative of comer": a click opens the lemma, which the line underlines. */
@Composable
private fun FormOfLine(sense: WiktionarySense, onOpenWord: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val lemma = sense.formOf.first()
    val text = sense.text
    val line = buildAnnotatedString {
        val at = text.indexOf(lemma.shown)
        if (at < 0) {
            append(text)
            append(" → ")
            withStyle(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)) { append(lemma.shown) }
        } else {
            append(text.substring(0, at))
            withStyle(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)) { append(lemma.shown) }
            append(text.substring(at + lemma.shown.length))
        }
    }
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.small).clickable { onOpenWord(lemma.title) }.padding(vertical = AppSpacing.xxs).testTag("form-of-${lemma.title}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(line, Modifier.weight(1f), style = AppText.labelStrong)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconSmall))
    }
}

/** A heading that opens and closes what is under it, with an optional word beside it ("Conjugation · comer"). */
@Composable
private fun Collapsible(title: String, word: String?, open: Boolean, action: String, tag: String, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.small).clickable(onClick = onToggle).padding(vertical = AppSpacing.xs).testTag(tag)
            .semantics { contentDescription = action },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(title, style = AppText.labelStrong)
        if (word != null) Text(word, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
        Spacer(Modifier.weight(1f))
        Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconMedium))
    }
    if (open) content()
}

/**
 * A table of forms: rows by mood and tense or by case, columns by person and number, the cells
 * holding the clicked form tinted; wider than the pane it scrolls sideways. The forms outside the
 * grid follow as a list, each with its tags.
 */
@Composable
internal fun InflectionTableView(table: InflectionTable, term: String) {
    val colors = MaterialTheme.colorScheme
    val clicked = WiktionaryCodes.plain(term)
    fun isClicked(form: String) = WiktionaryCodes.plain(form) == clicked || WiktionaryCodes.plain(form.substringBefore(" [")) == clicked
    if (table.rows.isNotEmpty()) {
        val scroll = rememberScrollState()
        // The clicked form's column is scrolled just into view, as the pane is often narrower than the table.
        val column = table.columns.indices.firstOrNull { index -> table.rows.any { row -> row.cells[index].any(::isClicked) } }
        val end = with(LocalDensity.current) { column?.let { (ROW_LABEL_WIDTH + CELL_WIDTH * (it + 1)).roundToPx() } }
        LaunchedEffect(table, end, scroll.viewportSize) { end?.let { scroll.scrollTo((it - scroll.viewportSize).coerceAtLeast(0)) } }
        Column(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            Row {
                Cell("", ROW_LABEL_WIDTH, header = true)
                table.columns.forEach { Cell(grammarColumn(it), CELL_WIDTH, header = true) }
            }
            table.rows.forEach { row ->
                AppDivider(Modifier.width(ROW_LABEL_WIDTH + CELL_WIDTH * table.columns.size), color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                Row {
                    Cell(grammarTags(row.tags), ROW_LABEL_WIDTH, header = true)
                    row.cells.forEach { forms ->
                        val highlighted = forms.any(::isClicked)
                        Cell(forms.joinToString(", "), CELL_WIDTH, highlighted = highlighted, modifier = Modifier.testTag(if (highlighted) "cell-clicked" else "cell"))
                    }
                }
            }
        }
    }
    if (table.other.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            table.other.take(MAX_LISTED_FORMS).forEach { form ->
                val highlighted = isClicked(form.form)
                Row(
                    Modifier.clip(AppShapes.small).then(if (highlighted) Modifier.background(colors.primary.copy(alpha = AppAlpha.tint)) else Modifier)
                        .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs).testTag(if (highlighted) "form-clicked" else "form"),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(form.form, style = MaterialTheme.typography.bodySmall)
                    if (form.tags.isNotEmpty()) Text(grammarTags(form.tags), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Cell(text: String, width: androidx.compose.ui.unit.Dp, header: Boolean = false, highlighted: Boolean = false, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(
        text,
        modifier.width(width).then(if (highlighted) Modifier.background(colors.primary.copy(alpha = AppAlpha.tint)) else Modifier)
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xs),
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        color = if (header) colors.onSurfaceVariant else if (highlighted) colors.primary else colors.onSurface,
    )
}

/** A word Wiktionary names, opened in the pane by a click. */
@Composable
private fun WordChip(word: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        word,
        modifier.clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control).clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        style = AppText.label,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Wiktionary's licence, as its content requires wherever it shows, linking the entry it comes from. */
@Composable
internal fun WiktionaryCredit(page: WiktionaryPage, languageName: String?) {
    val uriHandler = LocalUriHandler.current
    Text(
        tr("Wiktionary contributors · CC BY-SA 4.0"),
        Modifier.clip(AppShapes.small).clickable { uriHandler.openUri(WiktionaryCodes.entryUrl(page, languageName)) }
            .padding(vertical = AppSpacing.xxs).testTag("wiktionary-credit"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val MAX_PRONUNCIATIONS = 8
private const val MAX_SYNONYMS = 12
private const val MAX_LISTED_FORMS = 60
private val ROW_LABEL_WIDTH = 132.dp
private val CELL_WIDTH = 92.dp
