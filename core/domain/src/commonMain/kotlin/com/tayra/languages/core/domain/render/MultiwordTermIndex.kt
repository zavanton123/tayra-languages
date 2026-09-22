package com.tayra.languages.core.domain.render

import com.tayra.languages.core.domain.model.ZWS_STRING

/**
 * Finds occurrences of multi-word terms in a list of lowercased tokens.
 *
 * Terms are indexed by their first token so that each token position only has to
 * be checked against the handful of terms starting with that token.
 */
class MultiwordTermIndex(termTextLcs: Collection<String>) {

    private class Entry(val textLc: String, val tokens: List<String>)

    private val byFirstToken: Map<String, List<Entry>> = termTextLcs
        .asSequence()
        .distinct()
        .map { Entry(it, it.split(ZWS_STRING)) }
        .filter { it.tokens.size > 1 }
        .groupBy { it.tokens.first() }

    val isEmpty: Boolean get() = byFirstToken.isEmpty()

    /** All matches as (term text_lc, starting token index), including overlapping ones. */
    fun findAll(tokensLc: List<String>): List<Pair<String, Int>> {
        if (byFirstToken.isEmpty()) return emptyList()
        val matches = mutableListOf<Pair<String, Int>>()
        for (start in tokensLc.indices) {
            val candidates = byFirstToken[tokensLc[start]] ?: continue
            for (candidate in candidates) {
                if (matchesAt(tokensLc, start, candidate.tokens)) {
                    matches.add(candidate.textLc to start)
                }
            }
        }
        return matches
    }

    /** Distinct term texts present in the tokens. */
    fun findTerms(tokensLc: List<String>): Set<String> = findAll(tokensLc).mapTo(LinkedHashSet()) { it.first }

    private fun matchesAt(tokens: List<String>, start: Int, termTokens: List<String>): Boolean {
        if (start + termTokens.size > tokens.size) return false
        for (i in termTokens.indices) {
            if (tokens[start + i] != termTokens[i]) return false
        }
        return true
    }
}
