package com.tayra.languages.core.domain.term

/** Minimal RFC 4180 CSV writer. */
object Csv {

    fun format(rows: List<List<String>>): String = rows.joinToString("\n") { row -> row.joinToString(",") { quote(it) } }

    private fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
