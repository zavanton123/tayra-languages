package com.tayra.languages.core.domain.morphology

import kotlin.test.fail

/**
 * Checks a language's ported rules against Yomitan's own tests of them: each valid case's word
 * must be among the deinflections of its inflected text, in its condition and through its
 * transforms, and no invalid case's may be.
 */
internal fun checkTransformCases(rules: List<String>, cases: List<String>, preprocess: (String) -> String = { it }) {
    val transformer = LanguageTransformer(rules)
    val failed = cases.mapNotNull { line ->
        val (valid, source, term, rule, reasons) = line.split('\t')
        val expectedConditions = if (rule.isEmpty()) null else transformer.conditions(listOf(rule))
        val expectedReasons = if (reasons == "*") null else reasons.split(',').filter { it.isNotEmpty() }
        val has = transformer.transform(preprocess(source)).any { d ->
            d.text == preprocess(term) &&
                (expectedConditions == null || LanguageTransformer.conditionsMatch(d.conditions, expectedConditions)) &&
                (expectedReasons == null || d.trace == expectedReasons)
        }
        if (has == (valid == "1")) null else "$source ${if (valid == "1") "lacks" else "has"} $term ($rule, $reasons)"
    }
    if (failed.isNotEmpty()) fail("${failed.size} of ${cases.size} cases fail:\n" + failed.take(30).joinToString("\n"))
}

/** Checks rules Yomitan has no tests of against words they should lead back to their dictionary words. */
internal fun checkDeinflections(rules: List<String>, vararg cases: Pair<String, String>) {
    val transformer = LanguageTransformer(rules)
    val missing = cases.filter { (word, lemma) -> transformer.transform(word).none { it.text == lemma } }
    if (missing.isNotEmpty()) fail("not deinflected: $missing")
}
