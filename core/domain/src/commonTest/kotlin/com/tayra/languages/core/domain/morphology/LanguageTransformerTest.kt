package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageTransformerTest {
    private val rules = listOf(
        listOf(
            "C\tv\t1\t",
            "C\tn\t1\tnp,ns",
            "C\tnp\t0\t",
            "C\tns\t1\t",
            "T\tplural\tplural\tplural of a noun",
            "R\ts$\t\t\tnp\tns\ts\t0\t1\t\t",
            "T\tpast\tpast\t",
            "R\ted$\t\t\t\tv\ts\t0\t2\t\t",
            "R\t^ge(.+)t$\t\t\t\tv\tr\t^ge(.+)t$\t\t$1en",
            "T\tirregular\tirregular\t",
            "R\t^went$\t\t\t\tv\tc\tgo",
            "R\tue(.*)o$\t\t+jue\t\tv\tr\tue\t\tu\to$\t\tar",
            "R\tue(.*)o$\t\t-jue\t\tv\tr\tue\t\to\to$\t\tar",
        ).joinToString("\n"),
    )
    private val transformer = LanguageTransformer(rules)

    private fun texts(word: String) = transformer.transform(word).map { it.text }

    @Test
    fun suffixAndPrefixRulesUndoInflections() {
        assertEquals(listOf("cats", "cat"), texts("cats"))
        assertTrue("walk" in texts("walked"))
        assertTrue("sagen" in texts("gesagt"))
        assertTrue("go" in texts("went"))
    }

    @Test
    fun guardedRulesFollowTheWordsBeginning() {
        assertTrue("jugar" in texts("juego"))
        assertTrue("contar" in texts("cuento"))
        assertFalse("jogar" in texts("juego"))
    }

    @Test
    fun conditionsChainOnlyWhereTheyMatch() {
        val cat = transformer.transform("cats").first { it.text == "cat" }
        assertEquals(listOf("plural"), cat.trace)
        assertTrue(LanguageTransformer.conditionsMatch(cat.conditions, transformer.dictionaryFormConditions(listOf("n"))))
        assertFalse(LanguageTransformer.conditionsMatch(cat.conditions, transformer.dictionaryFormConditions(listOf("v"))))
        // A past tense is never taken for a plural: "walkeds" has no reading.
        assertFalse("walk" in texts("walkeds"))
    }

    @Test
    fun javaScriptReplacementsReadAsKotlinOnes() {
        assertEquals("ab", Regex("(a)(b)").replace("ab", LanguageTransformer.javaScriptReplacement("$1$2")))
        assertEquals("[ab]", Regex("ab").replace("ab", LanguageTransformer.javaScriptReplacement("[$&]")))
        assertEquals("$1", Regex("ab").replace("ab", LanguageTransformer.javaScriptReplacement("$$1")))
        assertEquals("a\\b", Regex("x").replace("x", LanguageTransformer.javaScriptReplacement("a\\b")))
    }
}
