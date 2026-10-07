package com.tayra.languages.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

// ---- Terms

fun termsCommand() = group(
    "terms", "The words and phrases of the language with their statuses and translations",
    TermsList(), TermsShow(), TermsAdd(), TermsSetStatus(), TermsDelete(),
)

private suspend fun Cli.term(text: String): Term {
    val language = language()
    return get<TermService>().find(language.id, text) ?: notFound("No term '$text' in ${language.name}")
}

private suspend fun Cli.terms(statuses: Set<TermStatus>?, search: String = "", limit: Int = Int.MAX_VALUE, offset: Int = 0): Pair<List<Term>, Int> {
    // Opening a page saves its new words with status 0 so each has an id; those are only listed when asked for.
    val filter = TermListFilter(
        languageId = language().id,
        search = search,
        minStatus = TermStatus.NEW_1,
        statuses = statuses,
        includeIgnored = statuses != null && TermStatus.IGNORED in statuses,
    )
    val page = get<TermRepository>().list(filter, TermListSort(TermSortField.TEXT, ascending = true), offset, limit)
    return page.items to page.totalCount
}

@Serializable
data class TermListJson(val total: Int, val offset: Int, val terms: List<TermJson>)

private class TermsList : Command("list", "List the saved terms of the language alphabetically: learning and known, unless --status says otherwise") {
    private val statuses by option("--status", help = "Only this status: $STATUS_HELP; repeat for several").multiple()
    private val search by option("--search", help = "Only terms or translations containing this").default("")
    private val limit by option("--limit", help = "At most this many").int().restrictTo(min = 1).default(100)
    private val offset by option("--offset", help = "Skip this many first, to page through").int().restrictTo(min = 0).default(0)

    override fun run() = respond(cli, TermListJson.serializer()) {
        val (terms, total) = cli.terms(statuses.map(::parseStatus).toSet().ifEmpty { null }, search, limit, offset)
        val json = TermListJson(total, offset, terms.map { it.toJson() })
        Output(json, if (terms.isEmpty()) "No terms." else table(
            listOf("TERM", "STATUS", "TRANSLATION"),
            json.terms.map { listOf(it.text, it.statusName, it.translation.orEmpty().replace('\n', ' ')) },
        ) + if (total > offset + terms.size) "\n… ${total - offset - terms.size} more; use --offset ${offset + terms.size}" else "")
    }
}

private class TermsShow : Command("show", "Show a term: status, translation, parents and the sentence it was learned in") {
    private val text by argument(help = "The term")

    override fun run() = respond(cli, TermJson.serializer()) {
        val term = cli.term(text).toJson()
        Output(term, buildString {
            appendLine("${term.text}  (${term.statusName})")
            term.translation?.takeIf { it.isNotBlank() }?.let { appendLine("  translation:  $it") }
            term.romanization?.takeIf { it.isNotBlank() }?.let { appendLine("  romanization: $it") }
            if (term.parents.isNotEmpty()) appendLine("  parents:      ${term.parents.joinToString(", ")}")
            term.sentence?.let { appendLine("  sentence:     $it") }
        }.trimEnd())
    }
}

private class TermsAdd : Command("add", "Save a term with its translation, or change a saved one") {
    private val text by argument(help = "The word or phrase")
    private val translation by option("--translation", help = "Its meaning")
    private val status by option("--status", help = STATUS_HELP).default("1")
    private val parents by option("--parent", help = "A parent term, such as the word's base form; repeat for several").multiple()

    override fun run() = respond(cli, TermJson.serializer(), changes = true) {
        val language = cli.language()
        val service = cli.get<TermService>()
        val draft = service.findOrNew(language.id, text)
        val id = service.save(draft.copy(
            translation = translation ?: draft.translation,
            status = parseStatus(status),
            statusExplicitlySet = true,
            parents = parents.ifEmpty { draft.parents },
        ))
        val saved = cli.get<TermRepository>().getById(id)!!.toJson()
        Output(saved, "Saved '${saved.text}' (${saved.statusName})")
    }
}

private class TermsSetStatus : Command("set-status", "Give words or phrases a status, saving the ones not saved yet") {
    private val status by argument(help = STATUS_HELP)
    private val texts by argument(help = "The words or phrases").multiple(required = true)

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val wanted = parseStatus(status)
        cli.get<ReadingService>().setStatusForTexts(cli.language(), texts, wanted)
        val message = "${texts.size} ${if (texts.size == 1) "term" else "terms"} now ${wanted.cliName}"
        Output(DoneJson(message = message), message)
    }
}

private class TermsDelete : Command("delete", "Delete saved terms; their words become unknown again") {
    private val texts by argument(help = "The terms").multiple(required = true)
    private val yes by option("--yes", help = "Confirm the deletion").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val terms = texts.map { cli.term(it) }
        if (!yes) invalid("Deleting ${terms.size} ${if (terms.size == 1) "term" else "terms"} cannot be undone; repeat with --yes")
        cli.get<TermService>().deleteAll(terms.map { it.id })
        Output(DoneJson(message = "Deleted ${terms.size}"), "Deleted ${terms.joinToString(", ") { "'${it.displayText}'" }}")
    }
}

// ---- Reading

fun readCommand() = group(
    "read", "A book's pages as the reader sees them: each word with its status",
    ReadPage(), ReadMarkRead(),
)

@Serializable
data class WordJson(val text: String, val status: Int, val statusName: String, val termId: Long?)

@Serializable
data class ReadPageJson(
    val bookId: Long,
    val title: String,
    val page: Int,
    val pages: Int,
    /** Word count, counting each repeat. */
    val words: Int,
    val distinctWords: Int,
    /** Distinct words with no status yet. */
    val unknown: Int,
    val unknownPercent: Int,
    val byStatus: Map<String, Int>,
    val items: List<WordJson>,
)

private class ReadPage : Command("page", "Show a page's words by status: how much of it is new, and which words") {
    private val book by argument(help = "Book id").long()
    private val page by pageArgument()
    private val all by option("--all", help = "List every distinct word with its status, not just the new ones").flag()

    override fun run() = respond(cli, ReadPageJson.serializer()) {
        val books = cli.get<BookRepository>()
        val found = books.getBook(book) ?: notFound("No book $book")
        val reading = cli.get<ReadingService>()
        val number = page ?: reading.currentPageNumber(found)
        val count = books.pageCount(book)
        if (number !in 1..count) notFound("Book $book has $count pages")
        val opened = reading.openPage(book, number, trackOpen = false)
        val words = opened.rendered.words
        val items = words.map { WordJson(it.text.replace("​", ""), it.status.value, it.status.cliName, it.termId) }
        val distinct = items.distinctBy { it.text.lowercase() }
        val unknown = distinct.count { it.status == TermStatus.UNKNOWN.value }
        val json = ReadPageJson(
            book, found.title, number, count, items.size, distinct.size, unknown,
            if (distinct.isEmpty()) 0 else unknown * 100 / distinct.size,
            distinct.groupingBy { it.statusName }.eachCount(),
            items,
        )
        Output(json, buildString {
            appendLine("${found.title}, page $number of $count: ${json.words} words, ${json.distinctWords} different, ${json.unknown} new (${json.unknownPercent}%)")
            appendLine(json.byStatus.entries.sortedBy { it.key }.joinToString("  ") { "${it.key}: ${it.value}" })
            val shown = if (all) distinct else distinct.filter { it.status == TermStatus.UNKNOWN.value }
            if (shown.isNotEmpty()) {
                appendLine()
                if (all) append(table(listOf("WORD", "STATUS"), shown.map { listOf(it.text, it.statusName) }))
                else append("New: " + shown.joinToString(", ") { it.text })
            }
        }.trimEnd())
    }
}

private class ReadMarkRead : Command("mark-read", "Mark a page as read, as Next page does in the app, counting its words as read") {
    private val book by argument(help = "Book id").long()
    private val page by pageArgument()
    private val restKnown by option("--rest-known", help = "Also make the words still without a status known").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val found = cli.get<BookRepository>().getBook(book) ?: notFound("No book $book")
        val reading = cli.get<ReadingService>()
        val number = page ?: reading.currentPageNumber(found)
        reading.markPageRead(book, number, markRestAsKnown = restKnown)
        val message = "Page $number of \"${found.title}\" read" + if (restKnown) ", its other words known" else ""
        Output(DoneJson(message = message), message)
    }
}

// ---- Export

fun exportCommand() = group(
    "export", "Take the terms out of the app: to Anki, or as CSV or JSON",
    ExportAnki(), ExportTerms(),
)

@Serializable
data class ExportJson(val file: String, val terms: Int, val skipped: Int)

private class ExportAnki : Command("anki", "Export the terms being learned (1 to 4) to an Anki deck, leaving out ones exported before") {
    private val out by option("--out", help = "The .apkg file to write; default: the app's file name in this folder")
    private val keepUnmarked by option("--no-mark", help = "Do not remember these terms as exported").flag()

    override fun run() = respond(cli, ExportJson.serializer(), changes = true) {
        val learning = setOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4)
        val ids = cli.terms(learning).first.map { it.id }
        val service = cli.get<AnkiExportService>()
        val export = service.export(ids) ?: notFound("No terms to export: none being learned, or all exported before")
        val file = File(out ?: export.fileName)
        file.writeBytes(export.bytes)
        if (!keepUnmarked) service.markExported(export)
        Output(ExportJson(file.absolutePath, export.termIds.size, export.skipped), "Wrote ${export.termIds.size} terms to ${file.path}")
    }
}

private class ExportTerms : Command("terms", "Write the terms of the language as CSV or JSON, to stdout or a file") {
    private val format by option("--format", help = "csv or json").choice("csv", "json").default("csv")
    private val statuses by option("--status", help = "Only this status: $STATUS_HELP; repeat for several").multiple()
    private val out by option("--out", help = "A file to write; default: stdout")

    override fun run() {
        val terms = kotlinx.coroutines.runBlocking { cli.terms(statuses.map(::parseStatus).toSet().ifEmpty { null }).first }
        val text = if (format == "json") {
            cli.encode(ListSerializer(TermJson.serializer()), terms.map { it.toJson() })
        } else {
            fun cell(value: String?) = (value ?: "").let { if (it.any { c -> c == ',' || c == '"' || c == '\n' }) "\"" + it.replace("\"", "\"\"") + "\"" else it }
            (listOf("term,status,translation,romanization,parents,sentence") + terms.map { t ->
                listOf(t.displayText, t.status.cliName, t.translation, t.romanization, t.parents.joinToString(";") { it.text }, t.sentence).joinToString(",") { cell(it) }
            }).joinToString("\n")
        }
        val target = out
        if (target == null) echo(text) else {
            File(target).writeText(text + "\n")
            echo("Wrote ${terms.size} terms to $target", err = true)
        }
    }
}
