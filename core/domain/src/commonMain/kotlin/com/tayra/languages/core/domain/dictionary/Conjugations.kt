package com.tayra.languages.core.domain.dictionary

/**
 * A verb's [InflectionTable] laid out by mood, as the conjugation dialog shows it: a tab per mood
 * holding its tenses, then the forms of no mood (infinitives, participles, the gerund).
 */
data class ConjugationLayout(val moods: List<ConjugationMood>, val nonFinite: List<InflectionRow>, val other: List<WiktionaryForm>)

/** A mood and its tenses, each a row of the table whose [ConjugationTense.tags] are the row's without the mood. */
data class ConjugationMood(val mood: String, val tenses: List<ConjugationTense>)

data class ConjugationTense(val tags: List<String>, val row: InflectionRow)

/** The singular and plural forms of one person, with the pronouns that go with them. */
data class PersonForms(val person: String, val singularPronoun: String?, val singular: List<String>, val pluralPronoun: String?, val plural: List<String>)

object Conjugations {
    private val moods = listOf("indicative", "subjunctive", "imperative", "optative", "jussive")
    private val nonFiniteTags = setOf("infinitive", "participle", "gerund", "supine", "converb")
    private val persons = listOf("first-person", "second-person", "third-person")

    /** The table by mood; a row of tense alone ("conditional", a Russian "past") goes with the indicative. */
    fun layout(table: InflectionTable): ConjugationLayout {
        val (nonFinite, finite) = table.rows.partition { row -> row.tags.any { it in nonFiniteTags } }
        val byMood = finite.groupBy { row -> row.tags.firstOrNull { it in moods } ?: moods.first() }
        val grouped = byMood.entries.sortedBy { moods.indexOf(it.key) }.map { (mood, rows) ->
            ConjugationMood(mood, rows.map { ConjugationTense(it.tags.filter { tag -> tag != mood }, it) })
        }
        return ConjugationLayout(grouped, nonFinite, table.other)
    }

    /**
     * A row as person by number, when the table's columns are a person and singular or plural
     * each; null for a table laid out otherwise, which is shown column by column.
     */
    fun byPerson(columns: List<List<String>>, row: InflectionRow, languageCode: String?): List<PersonForms>? {
        val keyed = columns.map { column -> column.filter { it in persons } to column.filter { it !in persons } }
        if (keyed.any { (person, number) -> person.size != 1 || number.singleOrNull() !in setOf("singular", "plural") }) return null
        val pronouns = pronouns[languageCode].orEmpty()
        fun cell(person: String, number: String) = keyed.indexOfFirst { it.first.single() == person && it.second.single() == number }.let { if (it < 0) emptyList() else row.cells[it] }
        return persons.filter { person -> keyed.any { it.first.single() == person } }.map { person ->
            val at = persons.indexOf(person)
            PersonForms(person, pronouns.getOrNull(at), cell(person, "singular"), pronouns.getOrNull(at + persons.size), cell(person, "plural"))
        }
    }

    /** The subject pronouns of 1st, 2nd and 3rd person singular, then plural, by language code. */
    private val pronouns = mapOf(
        "pt" to listOf("eu", "tu", "ele / ela / você", "nós", "vós", "eles / elas / vocês"),
        "es" to listOf("yo", "tú", "él / ella / usted", "nosotros", "vosotros", "ellos / ellas / ustedes"),
        "ca" to listOf("jo", "tu", "ell / ella", "nosaltres", "vosaltres", "ells / elles"),
        "fr" to listOf("je", "tu", "il / elle / on", "nous", "vous", "ils / elles"),
        "it" to listOf("io", "tu", "lui / lei", "noi", "voi", "loro"),
        "ro" to listOf("eu", "tu", "el / ea", "noi", "voi", "ei / ele"),
        "de" to listOf("ich", "du", "er / sie / es", "wir", "ihr", "sie / Sie"),
        "nl" to listOf("ik", "jij", "hij / zij / het", "wij", "jullie", "zij"),
        "cs" to listOf("já", "ty", "on / ona / ono", "my", "vy", "oni / ony"),
        "sk" to listOf("ja", "ty", "on / ona / ono", "my", "vy", "oni / ony"),
        "pl" to listOf("ja", "ty", "on / ona / ono", "my", "wy", "oni / one"),
        "ru" to listOf("я", "ты", "он / она / оно", "мы", "вы", "они"),
        "uk" to listOf("я", "ти", "він / вона / воно", "ми", "ви", "вони"),
    )
}
