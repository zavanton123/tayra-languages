package com.tayra.languages.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** A group of subcommands with its help. */
fun group(name: String, help: String, vararg commands: CliktCommand): CliktCommand = object : NoOpCliktCommand(name = name) {
    override fun help(context: Context) = help
}.subcommands(*commands)

/** A command with its name and help, the shape of most commands here. */
abstract class Command(name: String, private val helpText: String) : CliktCommand(name = name) {
    protected val cli by requireObject<Cli>()
    override fun help(context: Context) = helpText
}

@Serializable
data class InitJson(val languages: Int, val books: Int, val currentLanguage: String?)

class InitCommand : Command("init", "Set up the library as the app does on its first start: the languages, the tutorial books and the courses") {
    override fun run() = respond(cli, InitJson.serializer(), changes = true) {
        cli.get<DemoDataService>().ensureLanguages()
        cli.get<CourseService>().seedSamples()
        val current = cli.get<LearningLanguageService>().ensure()
        val languages = cli.get<LanguageRepository>().getAll()
        val books = cli.get<BookRepository>().getBooks().size
        val name = languages.firstOrNull { it.id == current }?.name
        Output(InitJson(languages.size, books, name), "Ready: ${languages.size} languages, $books books" + (name?.let { ". Learning $it." } ?: "."))
    }
}

// ---- Languages

fun languagesCommand() = group(
    "languages", "The languages in the library, and the one being learned",
    LanguagesList(), LanguagesAdd(), LanguagesUse(),
)

private class LanguagesList : Command("list", "List the languages in the library; with --available, the ones that can be added") {
    private val available by option("--available", help = "List the languages that can be added instead").flag()

    override fun run() = respond(cli, ListSerializer(LanguageJson.serializer())) {
        val current = cli.get<SettingsRepository>().current.currentLanguageId
        val list = if (available) {
            val have = cli.get<LanguageRepository>().getAll().map { it.name }.toSet()
            cli.get<LanguageService>().predefined().filter { it.name !in have }.map { LanguageJson(0, it.name, LanguageCodes.codeFor(it.name), false) }
        } else {
            cli.get<LanguageRepository>().getAll().sortedBy { it.name }.map { LanguageJson(it.id, it.name, LanguageCodes.codeFor(it.name), it.id == current) }
        }
        Output(list, table(listOf("ID", "LANGUAGE", "CODE", ""), list.map { listOf(if (it.id == 0L) "-" else "${it.id}", it.name, it.code ?: "", if (it.current) "learning" else "") }))
    }
}

private class LanguagesAdd : Command("add", "Add a language the app knows, by name or ISO code") {
    private val name by argument(help = "Such as Portuguese or pt")
    private val tutorial by option("--with-tutorial", help = "Also add the tutorial book in this language").flag()

    override fun run() = respond(cli, LanguageJson.serializer(), changes = true) {
        val service = cli.get<LanguageService>()
        val definition = service.predefined().firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: service.predefined().firstOrNull { LanguageCodes.codeFor(it.name).equals(name, ignoreCase = true) }
            ?: notFound("The app has no language '$name'; `tayra languages list --available` lists them")
        cli.get<LanguageRepository>().findByName(definition.name)?.let { invalid("${definition.name} is in the library already (id ${it.id})") }
        val id = service.loadPredefined(definition.name, withTutorial = tutorial)
        Output(LanguageJson(id, definition.name, LanguageCodes.codeFor(definition.name), false), "Added ${definition.name} (id $id)")
    }
}

private class LanguagesUse : Command("use", "Make a language the one being learned, in the app too") {
    private val name by argument(help = "Name, ISO code or id")

    override fun run() = respond(cli, LanguageJson.serializer(), changes = true) {
        val all = cli.get<LanguageRepository>().getAll()
        val language = all.firstOrNull { it.name.equals(name, true) || "${it.id}" == name || LanguageCodes.codeFor(it.name).equals(name, true) }
            ?: notFound("No language '$name' in the library")
        if (!cli.get<LearningLanguageService>().select(language.id)) {
            invalid("${language.name} is your native language, so it cannot be the one you learn")
        }
        Output(LanguageJson(language.id, language.name, LanguageCodes.codeFor(language.name), true), "Learning ${language.name}")
    }
}

// ---- Books

fun booksCommand() = group(
    "books", "The books of the language: list, read, add, change and delete them",
    BooksList(), BooksShow(), BooksText(), BooksAdd(), BooksEdit(), BooksArchive(archive = true), BooksArchive(archive = false), BooksDelete(),
)

private suspend fun Cli.bookItem(id: Long, archived: Boolean? = null) =
    (if (archived == null) listOf(false, true) else listOf(archived)).firstNotNullOfOrNull { a -> get<BookRepository>().observeBooks(a).first().firstOrNull { it.id == id } }
        ?: notFound("No book $id")

private class BooksList : Command("list", "List the books of the language, last opened first") {
    private val archived by option("--archived", help = "List the archived books instead").flag()
    private val tags by option("--tag", help = "Only books with this tag; repeat for books with all of them").multiple()
    private val allLanguages by option("--all-languages", help = "Books of every language").flag()

    override fun run() = respond(cli, ListSerializer(BookJson.serializer())) {
        val languageId = if (allLanguages) null else cli.language().id
        val books = cli.get<BookRepository>().observeBooks(archived).first()
            .filter { languageId == null || it.languageId == languageId }
            .filter { book -> tags.all { tag -> book.tags.any { it.equals(tag, ignoreCase = true) } } }
            .map { it.toJson() }
        Output(books, table(
            listOf("ID", "TITLE", "PAGE", "WORDS", "NEW", "TAGS") + if (allLanguages) listOf("LANGUAGE") else emptyList(),
            books.map { b ->
                listOf("${b.id}", b.title, "${b.currentPage}/${b.pages}", "${b.words}", b.unknownPercent?.let { "$it%" } ?: "", b.tags.joinToString(",")) +
                    if (allLanguages) listOf(b.language) else emptyList()
            },
        ).ifEmpty { "No books." })
    }
}

private class BooksShow : Command("show", "Show a book: its pages, progress, tags and source") {
    private val id by argument(help = "Book id").long()

    override fun run() = respond(cli, BookJson.serializer()) {
        val book = cli.bookItem(id).toJson()
        Output(book, buildString {
            appendLine(book.title)
            appendLine("  id:        ${book.id}")
            appendLine("  language:  ${book.language}")
            appendLine("  page:      ${book.currentPage} of ${book.pages}${if (book.completed) " (finished)" else ""}")
            appendLine("  words:     ${book.words}")
            book.unknownPercent?.let { appendLine("  new words: $it%") }
            if (book.tags.isNotEmpty()) appendLine("  tags:      ${book.tags.joinToString(", ")}")
            book.source?.let { appendLine("  source:    $it") }
            if (book.archived) appendLine("  archived")
        }.trimEnd())
    }
}

private class BooksText : Command("text", "Print a book's text, or one page of it") {
    private val id by argument(help = "Book id").long()
    private val page by option("--page", help = "Only this page, counted from 1").int().restrictTo(min = 1)

    override fun run() = respond(cli, PageJson.serializer()) {
        val books = cli.get<BookRepository>()
        books.getBook(id) ?: notFound("No book $id")
        val count = books.pageCount(id)
        val result = page?.let { n ->
            val p = books.getPage(id, n) ?: notFound("Book $id has $count pages")
            PageJson(id, n, count, p.text)
        } ?: PageJson(id, 0, count, cli.get<BookService>().text(id))
        Output(result, result.text)
    }
}

private class BooksAdd : Command("add", "Add a book to the language from text, a file or stdin; prints its id") {
    private val title by option("--title", help = "The book's title").required()
    private val text by option("--text", help = "The text itself")
    private val file by option("--file", help = "A text file to read; - reads stdin")
    private val tags by option("--tags", help = "Tags, separated by commas").split(",").default(emptyList())
    private val source by option("--source", help = "Where the text comes from, such as a web address").default("")
    private val wordsPerPage by option("--words-per-page", help = "How many words make a page").int().default(BookDraft.DEFAULT_WORDS_PER_PAGE)

    override fun run() = respond(cli, CreatedJson.serializer(), changes = true) {
        val body = readInput(text, file) ?: invalid("Give the text with --text, or --file (- for stdin)")
        val id = cli.get<BookService>().create(BookDraft(languageId = cli.language().id, title = title, text = body, sourceUri = source, tags = tags, wordsPerPage = wordsPerPage))
        Output(CreatedJson("$id"), "Added book $id")
    }
}

private class BooksEdit : Command("edit", "Change a book's title, tags or source; with new text its pages are made again") {
    private val id by argument(help = "Book id").long()
    private val title by option("--title", help = "New title")
    private val tags by option("--tags", help = "New tags, separated by commas; empty removes them").split(",")
    private val source by option("--source", help = "New source")
    private val text by option("--text", help = "New text")
    private val file by option("--file", help = "A file with the new text; - reads stdin")
    private val wordsPerPage by option("--words-per-page", help = "Words per page when the pages are made again").int()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val service = cli.get<BookService>()
        val book = cli.get<BookRepository>().getBook(id) ?: notFound("No book $id")
        val newText = readInput(text, file)
        val draft = BookDraft(
            id = id,
            languageId = book.languageId,
            title = title ?: book.title,
            text = newText ?: "",
            sourceUri = source ?: book.sourceUri.orEmpty(),
            tags = tags?.filter { it.isNotBlank() } ?: book.tags,
            wordsPerPage = wordsPerPage ?: service.estimatedWordsPerPage(id),
            audioFilename = book.audioFilename,
        )
        service.update(draft, rebuildPages = newText != null)
        Output(DoneJson(message = "Saved book $id"), "Saved book $id")
    }
}

private class BooksArchive(private val archive: Boolean) : Command(
    if (archive) "archive" else "unarchive",
    if (archive) "Move a book to the archive" else "Bring a book back from the archive",
) {
    private val id by argument(help = "Book id").long()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        cli.get<BookRepository>().getBook(id) ?: notFound("No book $id")
        val service = cli.get<BookService>()
        if (archive) service.archive(id) else service.unarchive(id)
        val message = if (archive) "Archived book $id" else "Book $id is back on the shelf"
        Output(DoneJson(message = message), message)
    }
}

private class BooksDelete : Command("delete", "Delete a book and its pages for good") {
    private val id by argument(help = "Book id").long()
    private val yes by option("--yes", help = "Delete without being asked to confirm with this flag").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val book = cli.get<BookRepository>().getBook(id) ?: notFound("No book $id")
        if (!yes) invalid("Deleting \"${book.title}\" cannot be undone; repeat with --yes")
        cli.get<BookService>().delete(id)
        Output(DoneJson(message = "Deleted book $id"), "Deleted \"${book.title}\"")
    }
}

/** An optional page number argument, shared by the reading commands. */
internal fun CliktCommand.pageArgument() = argument(help = "Page number, counted from 1; default: the page the book is open at").int().optional()
