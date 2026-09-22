package com.tayra.languages.core.domain.term

/** Minimal RFC 4180 CSV reader/writer. */
object Csv {

    fun parse(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(ch)
                    }
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    fields.add(field.toString())
                    field.clear()
                }
                ch == '\r' -> Unit
                ch == '\n' -> {
                    fields.add(field.toString())
                    field.clear()
                    records.add(fields)
                    fields = mutableListOf()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            records.add(fields)
        }
        return records.filterNot { it.size == 1 && it[0].isEmpty() }
    }

    fun format(rows: List<List<String>>): String = rows.joinToString("\n") { row -> row.joinToString(",") { quote(it) } }

    private fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
