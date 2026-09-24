package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.term.Csv

class BadImportFileException(message: String) : Exception(message)

data class TermImportOptions(
    val createTerms: Boolean = true,
    val updateTerms: Boolean = true,
    val newAsUnknown: Boolean = false,
)

data class TermImportResult(val created: Int, val updated: Int, val skipped: Int)

/**
 * Imports terms from CSV with columns `language`, `term` and optionally
 * `translation`, `parent`, `status`, `pronunciation`, `link_status`. Lute's `tags` and
 * `added` columns are accepted and ignored.
 */
class TermImportService(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val termService: TermService,
) {

    suspend fun import(csv: String, options: TermImportOptions = TermImportOptions()): TermImportResult {
        val rows = parse(csv)
        validate(rows)
        return doImport(rows, options)
    }

    fun parse(csv: String): List<Map<String, String>> {
        val records = Csv.parse(csv.removePrefix("﻿"))
        if (records.isEmpty()) throw BadImportFileException("No terms in file")
        val header = records.first().map { it.trim().lowercase() }
        validateFields(header)
        val rows = LinkedHashSet<Map<String, String>>()
        records.drop(1).forEachIndexed { index, values ->
            if (values.size == 1 && values[0].isBlank()) return@forEachIndexed
            if (values.size < header.size) throw BadImportFileException("Missing values on line ${index + 1}")
            if (values.size > header.size) throw BadImportFileException("Extra values on line ${index + 1}")
            rows.add(header.zip(values).toMap())
        }
        if (rows.isEmpty()) throw BadImportFileException("No terms in file")
        return rows.toList()
    }

    private fun validateFields(fields: List<String>) {
        for (required in REQUIRED_FIELDS) {
            if (required !in fields) throw BadImportFileException("Missing required field '$required'")
        }
        for (field in fields) {
            if (field !in ALLOWED_FIELDS && field !in IGNORED_FIELDS) throw BadImportFileException("Unknown field '$field'")
        }
    }

    private suspend fun validate(rows: List<Map<String, String>>) {
        for (name in rows.map { it.getValue("language").trim() }.distinct()) {
            if (languages.findByName(name) == null) throw BadImportFileException("Unknown language '$name'")
        }
        if (rows.any { it.getValue("term").isBlank() }) throw BadImportFileException("Term is required")
        for (status in rows.mapNotNull { it["status"]?.trim() }.distinct()) {
            if (statusOf(status) == null) throw BadImportFileException("Status must be one of 1, 2, 3, 4, 5, I, W, or blank")
        }
        val keys = rows.map { "${it.getValue("language")}: ${it.getValue("term").trim().split(Regex("\\s+")).joinToString(" ").lowercase()}" }
        val duplicates = keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) throw BadImportFileException("Duplicate terms in import: ${duplicates.joinToString(", ")}")
    }

    private fun statusOf(text: String): TermStatus? = when (text.trim()) {
        "", "1" -> TermStatus.NEW_1
        "2" -> TermStatus.NEW_2
        "3" -> TermStatus.LEARNING_3
        "4" -> TermStatus.LEARNING_4
        "5" -> TermStatus.LEARNED
        "W", "w" -> TermStatus.WELL_KNOWN
        "I", "i" -> TermStatus.IGNORED
        else -> null
    }

    private suspend fun doImport(rows: List<Map<String, String>>, options: TermImportOptions): TermImportResult {
        var created = 0
        var updated = 0
        var skipped = 0
        val touched = HashSet<String>()

        fun key(languageId: Long, term: String) = "$languageId-$term"

        // Pass 1: terms without parents, so a parent given in its own row keeps its data.
        for (row in rows) {
            val language = languages.findByName(row.getValue("language").trim()) ?: continue
            val text = row.getValue("term")
            val existing = termService.find(language.id, text)
            when {
                options.createTerms && existing == null -> {
                    var draft = termService.findOrNew(language.id, text)
                    draft = applyRow(draft, row)
                    if (options.newAsUnknown) draft = draft.copy(status = TermStatus.UNKNOWN, statusExplicitlySet = true)
                    termService.save(draft)
                    created++
                    touched.add(key(language.id, text))
                }
                options.updateTerms && existing != null -> {
                    termService.save(applyRow(termService.draftOf(existing), row))
                    updated++
                    touched.add(key(language.id, text))
                }
                else -> skipped++
            }
        }

        // Pass 2: parents.
        for (row in rows) {
            val parent = row["parent"]?.trim().orEmpty()
            if (parent.isEmpty()) continue
            val language = languages.findByName(row.getValue("language").trim()) ?: continue
            val text = row.getValue("term")
            if (key(language.id, text) !in touched) continue
            val existing = termService.find(language.id, text) ?: continue
            var draft = termService.draftOf(existing).copy(parents = parent.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            row["link_status"]?.let { draft = draft.copy(syncStatus = it.trim().lowercase() == "y") }
            row["status"]?.let { s -> statusOf(s)?.let { draft = draft.copy(status = it, statusExplicitlySet = true) } }
            termService.save(draft)
        }

        return TermImportResult(created, updated, skipped)
    }

    private fun applyRow(draft: com.tayra.languages.core.domain.model.TermDraft, row: Map<String, String>): com.tayra.languages.core.domain.model.TermDraft {
        var result = draft
        row["translation"]?.let { result = result.copy(translation = it) }
        row["status"]?.let { s -> statusOf(s)?.let { result = result.copy(status = it, statusExplicitlySet = true) } }
        row["pronunciation"]?.let { result = result.copy(romanization = it) }
        return result
    }

    companion object {
        val REQUIRED_FIELDS = listOf("language", "term")
        val ALLOWED_FIELDS = REQUIRED_FIELDS + listOf("translation", "parent", "status", "pronunciation", "link_status")
        val IGNORED_FIELDS = listOf("added", "tags")
        val EXPORT_HEADERS = listOf("term", "parent", "translation", "language", "status", "link_status", "pronunciation")
    }
}
