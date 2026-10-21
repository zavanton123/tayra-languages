package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.i18n.tr

/**
 * A grammatical tag as Wiktionary's data names it ("third-person", "indicative"), in the interface
 * language; a tag with no translation is shown as written, its hyphens as spaces.
 */
fun grammarTag(tag: String): String = when (tag) {
    "first-person" -> tr("1st")
    "second-person" -> tr("2nd")
    "third-person" -> tr("3rd")
    "singular" -> tr("singular")
    "plural" -> tr("plural")
    "dual" -> tr("dual")
    "definite" -> tr("definite")
    "indefinite" -> tr("indefinite")
    "present" -> tr("present")
    "past" -> tr("past")
    "preterite" -> tr("preterite")
    "imperfect" -> tr("imperfect")
    "perfect" -> tr("perfect")
    "pluperfect" -> tr("pluperfect")
    "future" -> tr("future")
    "conditional" -> tr("conditional")
    "indicative" -> tr("indicative")
    "subjunctive" -> tr("subjunctive")
    "imperative" -> tr("imperative")
    "infinitive" -> tr("infinitive")
    "participle" -> tr("participle")
    "gerund" -> tr("gerund")
    "negative" -> tr("negative")
    "affirmative" -> tr("affirmative")
    "polite" -> tr("polite")
    "active" -> tr("active")
    "passive" -> tr("passive")
    "nominative" -> tr("nominative")
    "accusative" -> tr("accusative")
    "genitive" -> tr("genitive")
    "dative" -> tr("dative")
    "instrumental" -> tr("instrumental")
    "prepositional" -> tr("prepositional")
    "locative" -> tr("locative")
    "vocative" -> tr("vocative")
    "ablative" -> tr("ablative")
    "partitive" -> tr("partitive")
    "masculine" -> tr("masculine")
    "feminine" -> tr("feminine")
    "neuter" -> tr("neuter")
    "animate" -> tr("animate")
    "inanimate" -> tr("inanimate")
    "comparative" -> tr("comparative")
    "superlative" -> tr("superlative")
    else -> tag.replace('-', ' ')
}

/** A column of an inflection table, short enough for a narrow cell: "1st sg.", "pl.". */
fun grammarColumn(tags: List<String>): String = tags.joinToString(" ") { tag ->
    when (tag) {
        "singular" -> tr("sg.")
        "plural" -> tr("pl.")
        "dual" -> tr("du.")
        else -> grammarTag(tag)
    }
}

fun grammarTags(tags: List<String>): String = tags.joinToString(" ") { grammarTag(it) }
