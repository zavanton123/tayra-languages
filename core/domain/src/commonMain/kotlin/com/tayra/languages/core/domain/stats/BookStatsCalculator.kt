package com.tayra.languages.core.domain.stats

import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.TextItem
import kotlin.math.roundToInt

object BookStatsCalculator {

    /** Count of distinct terms per status among the word items. */
    fun statusDistribution(items: List<TextItem>): Map<TermStatus, Int> {
        val byStatus = HashMap<TermStatus, HashSet<String>>()
        for (item in items) {
            if (!item.isWord) continue
            byStatus.getOrPut(item.status) { HashSet() }.add(item.textLc)
        }
        return TermStatus.entries.associateWith { byStatus[it]?.size ?: 0 }
    }

    fun calculate(items: List<TextItem>): BookStats {
        val distribution = statusDistribution(items)
        val unknowns = distribution[TermStatus.UNKNOWN] ?: 0
        val allUnique = distribution.values.sum()
        val percent = if (allUnique > 0) (100.0 * unknowns / allUnique).roundToInt() else 0
        return BookStats(
            distinctTerms = allUnique,
            distinctUnknowns = unknowns,
            unknownPercent = percent,
            statusDistribution = distribution,
        )
    }
}
