package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 10ten Japanese Reader's deinflection cases, through Yomitan's Japanese rules and Tayra's additions to them. */
class TentenDeinflectTest {

    /** Cases left out, with why. */
    private val skipped = mapOf(
        ("到着になります" to "到着") to "a noun with になる would make every 〜になる one word where text is cut by the lexicon",
    )

    @Test
    fun everyFormFindsItsDictionaryForm() {
        val japanese = Morphology.forLanguage("ja")!!
        val missed = tentenDeinflectCases.filter { it !in skipped }.filter { (a, b) ->
            japanese.candidates(a).none { it.text == b } && japanese.candidates(b).none { it.text == a }
        }
        assertEquals(emptyList(), missed)
        assertTrue(skipped.keys.all { it in tentenDeinflectCases })
    }

    @Test
    fun theAddedFormsNameTheirInflections() {
        val japanese = Morphology.forLanguage("ja")!!
        val names = japanese.candidates("食べないで").first { it.text == "食べる" }.transforms.let(japanese::inflectionNames)
        assertEquals(listOf("negative", "-ないで"), names)
    }
}
