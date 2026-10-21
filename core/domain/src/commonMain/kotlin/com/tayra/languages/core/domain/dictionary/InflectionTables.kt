package com.tayra.languages.core.domain.dictionary

/**
 * A word's inflection table: rows named by their tags (mood and tense, or case), columns by
 * person and number, each cell the forms there. Forms that have no person or number
 * (an infinitive, a gerund), and every form of a table the grid does not fit, are in [other].
 */
data class InflectionTable(
    val pos: String,
    val columns: List<List<String>>,
    val rows: List<InflectionRow>,
    val other: List<WiktionaryForm>,
) {
    val isEmpty: Boolean get() = rows.isEmpty() && other.isEmpty()
}

data class InflectionRow(val tags: List<String>, val cells: List<List<String>>)

object InflectionTables {
    /** Rows of WiktApi's forms that are notes about the table rather than forms. */
    private val pseudoTags = setOf("table-tags", "inflection-template", "romanization", "canonical", "class")

    /** Tags that only say how a row was read, not which form it is. */
    private val noiseTags = setOf("error-unrecognized-form")

    private val persons = listOf("first-person", "second-person", "third-person")
    private val numbers = listOf("singular", "dual", "plural")
    private val definiteness = listOf("indefinite", "definite")
    private val columnTags = (persons + numbers + definiteness).toSet()

    /**
     * The order tags are named in within a row's label: tense and aspect before mood ("present
     * indicative"), then voice and polarity; tags not listed keep their order after these.
     */
    private val rowOrder = listOf(
        "negative", "positive", "affirmative", "polite", "formal", "informal",
        "present", "past", "preterite", "imperfect", "perfect", "pluperfect", "future", "aorist", "conditional",
        "indicative", "subjunctive", "imperative", "optative", "jussive",
        "infinitive", "participle", "gerund", "supine", "converb",
        "active", "passive", "reflexive",
        "nominative", "accusative", "genitive", "dative", "instrumental", "prepositional", "locative", "ablative", "vocative", "partitive",
        "masculine", "feminine", "neuter", "animate", "inanimate",
    )

    /** The forms that are forms: notes about the table left out, and spellings that are only a dash. */
    fun realForms(forms: List<WiktionaryForm>): List<WiktionaryForm> = forms
        .filter { form -> form.tags.none { it in pseudoTags } && form.form.isNotBlank() && form.form.any { it.isLetter() } }
        .map { form -> if (form.tags.any { it in noiseTags }) form.copy(tags = form.tags.filter { it !in noiseTags }) else form }

    /** The table of one part of speech's forms; the table rows win over the head line's when there are both. */
    fun table(pos: String, forms: List<WiktionaryForm>): InflectionTable {
        val real = realForms(forms)
        val tabled = real.filter { it.source != null }.ifEmpty { real }
        val candidates = tabled.filter { form -> form.tags.any { it in columnTags } && form.tags.any { it !in columnTags } }
        // Where the columns are persons, a form of no person (a participle's singular) is no cell of theirs.
        val personal = candidates.any { form -> form.tags.any { it in persons } }
        val inGrid = if (personal) candidates.filter { form -> form.tags.any { it in persons } } else candidates
        val columns = inGrid.map { columnOf(it) }.distinct().sortedWith(compareBy({ rank(it, numbers) }, { rank(it, definiteness) }, { rank(it, persons) }))
        val rowKeys = inGrid.map { rowOf(it) }.distinct()
        val rows = rowKeys.map { key ->
            InflectionRow(key, columns.map { column -> inGrid.filter { rowOf(it) == key && columnOf(it) == column }.map { it.form }.distinct() })
        }
        val fits = columns.size in 2..MAX_COLUMNS && rows.count { row -> row.cells.count { it.isNotEmpty() } >= 2 } >= 2
        if (!fits) return InflectionTable(pos, emptyList(), emptyList(), tabled.map { it.copy(tags = ordered(it.tags)) }.distinct())
        return InflectionTable(pos, columns, rows, tabled.filter { it !in inGrid }.map { it.copy(tags = ordered(it.tags)) }.distinct())
    }

    /** The tables of a page's entries, empty ones left out. */
    fun tables(page: WiktionaryPage): List<InflectionTable> = page.entries.map { table(it.pos, it.forms) }.filter { !it.isEmpty }

    private fun columnOf(form: WiktionaryForm): List<String> =
        (definiteness + persons + numbers).filter { it in form.tags }

    private fun rowOf(form: WiktionaryForm): List<String> = ordered(form.tags.filter { it !in columnTags })

    private fun ordered(tags: List<String>): List<String> =
        tags.sortedBy { tag -> rowOrder.indexOf(tag).let { if (it < 0) rowOrder.size else it } }

    private fun rank(column: List<String>, group: List<String>): Int =
        group.indexOfFirst { it in column }.let { if (it < 0) -1 else it }

    private const val MAX_COLUMNS = 8
}
