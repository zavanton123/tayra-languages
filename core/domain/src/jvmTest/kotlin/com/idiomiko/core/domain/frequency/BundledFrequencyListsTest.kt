package com.idiomiko.core.domain.frequency

import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.parse.LexiconParser
import com.idiomiko.core.domain.parse.parser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Android app downloads most frequency lists but keeps those of the languages its lexicons cut into words, so their
 * texts are cut the same before any download: every such language with a list is in androidApp/bundled-frequency-lists.txt.
 */
class BundledFrequencyListsTest {
    private val root = File("../..")

    @Test
    fun theListsThatCutWordsAreBundledOnAndroid() {
        val bundled = File(root, "androidApp/bundled-frequency-lists.txt").readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toSet()
        val lists = File(root, "feature/frequency/src/commonMain/composeResources/files/frequency")
        val cutting = LanguageDefinitions.all.map { it.language }
            .filter { runCatching { it.parser }.getOrNull() is LexiconParser }
            .mapNotNull { LanguageCodes.codeFor(it.name) }
            .filter { File(lists, "$it.tsv").isFile }
            .toSet()
        assertEquals(cutting.sorted(), bundled.sorted())
    }
}
