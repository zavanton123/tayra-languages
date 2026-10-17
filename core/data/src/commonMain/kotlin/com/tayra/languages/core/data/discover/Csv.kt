package com.tayra.languages.core.data.discover

/** Comma-separated values, as libraries publish their catalogs: a field in quotes can hold the separator, line breaks and doubled quotes. */
internal object Csv {
    fun rows(text: String, separator: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' && (quoted || field.isEmpty()) -> quoted = !quoted
                quoted -> field.append(c)
                c == separator -> { row += field.toString(); field.clear() }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += field.toString(); field.clear()
                    if (row.any { it.isNotEmpty() }) rows += row
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        row += field.toString()
        if (row.any { it.isNotEmpty() }) rows += row
        return rows
    }

    /** The rows after the header, each by the header's names. */
    fun records(text: String, separator: Char = ','): List<Map<String, String>> {
        val rows = rows(text.removePrefix("﻿"), separator)
        val header = rows.firstOrNull() ?: return emptyList()
        return rows.drop(1).map { row -> header.indices.associate { header[it] to row.getOrElse(it) { "" } } }
    }
}
