package com.tayra.languages.core.domain.book

import com.tayra.languages.core.domain.TestLanguages
import com.tayra.languages.core.domain.model.PageSplitMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PageSplitterTest {
    private val english = TestLanguages.english

    @Test
    fun smallTextIsOnePage() {
        val pages = PageSplitter.split("Here is a dog.  Here is a cat.", english, PageSplitMode.PARAGRAPHS, 250)
        assertEquals(listOf("Here is a dog. Here is a cat."), pages)
    }

    @Test
    fun splitsByParagraphWhenThresholdExceeded() {
        val text = "one two three.\nfour five six.\nseven eight nine."
        val pages = PageSplitter.split(text, english, PageSplitMode.PARAGRAPHS, 3)
        assertEquals(listOf("one two three.\nfour five six.", "seven eight nine."), pages)
    }

    @Test
    fun splitsBySentence() {
        val text = "one two three. four five six. seven eight nine."
        val pages = PageSplitter.split(text, english, PageSplitMode.SENTENCES, 3)
        assertEquals(listOf("one two three. four five six.", "seven eight nine."), pages)
    }

    @Test
    fun explicitPageBreaks() {
        val text = "one two.\n---\nthree four.\n\n---\nfive."
        val pages = PageSplitter.split(text, english, PageSplitMode.PARAGRAPHS, 250)
        assertEquals(listOf("one two.", "three four.", "five."), pages)
    }

    @Test
    fun sentencesAreBuiltWithZws() {
        val sentences = SentenceBuilder.build("Hi there. Bye.", english)
        assertEquals(listOf("​Hi​ ​there​.​", "​Bye​.​"), sentences.map { it.text })
        assertEquals(3, SentenceBuilder.wordCount("Hi there. Bye.", english))
    }
}
