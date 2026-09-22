package com.tayra.languages.core.domain.render

import kotlin.test.Test
import kotlin.test.assertEquals

class MultiwordTermIndexTest {
    private val z = "​"

    @Test
    fun findsOverlappingMatches() {
        val index = MultiwordTermIndex(listOf("b${z} ${z}b", "a${z} ${z}b"))
        val tokens = listOf("a", " ", "b", " ", "b", " ", "b")
        assertEquals(listOf("a$z $z" + "b" to 0, "b$z $z" + "b" to 2, "b$z $z" + "b" to 4), index.findAll(tokens))
    }

    @Test
    fun ignoresSingleTokenTerms() {
        assertEquals(true, MultiwordTermIndex(listOf("cat")).isEmpty)
    }
}
