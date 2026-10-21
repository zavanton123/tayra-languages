package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScripts

/** Example sentences of a Chinese language read in a script, shown in that script whichever they were written in. */
class ChineseScriptExamples(private val examples: ExampleSentencesProvider) : ExampleSentencesProvider {
    // The script of the search each next page continues.
    private val scripts = HashMap<String, ChineseScript>()

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult =
        examples.search(query).inScript(query.language.chineseScript)

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult =
        examples.nextPage(nextPage, targetLanguage).inScript(scripts[nextPage])

    override suspend fun sources(language: Language): List<ExampleSourceFeatures> = examples.sources(language)

    private fun ExampleSearchResult.inScript(script: ChineseScript?): ExampleSearchResult {
        if (script == null) return this
        nextPage?.let { scripts[it] = script }
        return copy(sentences = sentences.map { it.copy(text = ChineseScripts.convert(it.text, script)) })
    }
}
