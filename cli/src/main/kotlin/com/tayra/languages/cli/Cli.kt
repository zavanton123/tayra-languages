package com.tayra.languages.cli

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.di.dataModule
import com.tayra.languages.core.data.runtime.AppInstanceLock
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.courses.CourseValidationException
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookValidationException
import com.tayra.languages.core.domain.service.LanguageValidationException
import com.tayra.languages.core.domain.service.LearningLanguageService
import com.tayra.languages.feature.frequency.ResourceFrequencyLists
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File

/** Exit codes, the same for every command; they are listed in `tayra --help`. */
object ExitCode {
    const val OK = 0
    /** Bad arguments, or an error nobody planned for. */
    const val ERROR = 1
    const val NOT_FOUND = 2
    /** The input was refused, such as an empty title or a file that is not a backup. */
    const val INVALID = 3
    /** The action would clash with something, such as restoring while the app is open. */
    const val CONFLICT = 4
}

/** A failure the command reports as its message on stderr and [statusCode] as the exit code. */
class CliFailure(message: String, statusCode: Int) : CliktError(message, statusCode = statusCode)

fun notFound(message: String): Nothing = throw CliFailure(message, ExitCode.NOT_FOUND)
fun invalid(message: String): Nothing = throw CliFailure(message, ExitCode.INVALID)

/** What every command shares: the options of `tayra` itself and the app's services, started on first use. */
class Cli(val json: Boolean, private val languageOption: String?, dataDir: File?, verbose: Boolean) {
    init {
        if (dataDir != null) System.setProperty(DatabaseDriverFactory.DATA_DIR_PROPERTY, dataDir.absolutePath)
        // Logs would mix with the output that agents and scripts read, so they go to stderr, and only warnings.
        Logger.setLogWriters(StderrLogWriter)
        Logger.setMinSeverity(if (verbose) Severity.Debug else Severity.Warn)
    }

    private val application = lazy {
        koinApplication {
            modules(
                dataModule,
                module {
                    single<FrequencyLists> { ResourceFrequencyLists() }
                    single { VocabularyLevelService(get(), get(), get()) }
                },
            )
        }
    }

    val koin: Koin get() = application.value.koin

    /**
     * Koin keeps single instances in the module objects, so a second command in the same process,
     * as in tests, would get this one's database and settings unless they are let go of here.
     */
    fun close() {
        if (application.isInitialized()) application.value.close()
    }

    inline fun <reified T : Any> get(): T = koin.get()

    /** The language named by `--language`, by name or ISO code, else the one being learned in the app. */
    suspend fun language(): Language {
        val languages = get<LanguageRepository>()
        val wanted = languageOption?.trim()
        if (wanted != null) {
            val all = languages.getAll()
            return all.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
                ?: all.firstOrNull { LanguageCodes.codeFor(it.name).equals(wanted, ignoreCase = true) }
                ?: notFound("No language '$wanted'. `tayra languages list` shows them; `tayra languages add <name>` adds one.")
        }
        val id = get<LearningLanguageService>().ensure()
        return languages.getById(id) ?: notFound("No languages yet. Run `tayra init`, or add one with `tayra languages add <name>`.")
    }

    fun <T> encode(serializer: KSerializer<T>, value: T): String = JSON.encodeToString(serializer, value)

    companion object {
        val JSON = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}

/**
 * Runs a command's work: prints [value] as JSON with `--json`, else [human]'s text. Errors of the
 * app's services become exit codes, and a change made while the app is open gets a note that the
 * app shows it once reopened.
 */
fun <T> CliktCommand.respond(cli: Cli, serializer: KSerializer<T>, changes: Boolean = false, work: suspend () -> Output<T>) {
    val output = runBlocking {
        try {
            work()
        } catch (e: CliktError) {
            throw e
        } catch (e: NoSuchElementException) {
            notFound(e.message ?: "Not found")
        } catch (e: BookValidationException) {
            invalid(e.message ?: "Invalid book")
        } catch (e: CourseValidationException) {
            invalid(e.message ?: "Invalid course")
        } catch (e: LanguageValidationException) {
            invalid(e.message ?: "Invalid language")
        } catch (e: BackupException) {
            invalid(e.message ?: "Invalid backup")
        } catch (e: IllegalArgumentException) {
            invalid(e.message ?: "Invalid input")
        } catch (e: Exception) {
            Logger.d(e) { "Command failed" }
            throw CliFailure("${e::class.simpleName}: ${e.message}", ExitCode.ERROR)
        }
    }
    if (cli.json) echo(cli.encode(serializer, output.value)) else output.human?.let { if (it.isNotEmpty()) echo(it) }
    if (changes && AppInstanceLock.isAppOpen()) {
        echo("Note: Tayra Languages is open; it shows this change after you close and reopen it.", err = true)
    }
}

/** A command's result, and how it reads for a person; null text prints nothing. */
class Output<T>(val value: T, val human: String?)

private object StderrLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        System.err.println("[$severity] $message")
        throwable?.printStackTrace(System.err)
    }
}

/** Text from `--text`, or from `--file`, where `-` reads stdin. */
fun readInput(text: String?, file: String?): String? = when {
    text != null -> text
    file == "-" -> System.`in`.bufferedReader().readText()
    file != null -> File(file).takeIf { it.isFile }?.readText() ?: notFound("No file $file")
    else -> null
}

/** Columns padded to line up, for the lists people read. */
fun table(headers: List<String>, rows: List<List<String>>): String {
    if (rows.isEmpty()) return ""
    val widths = headers.indices.map { i -> (rows.map { it[i].length } + headers[i].length).max().coerceAtMost(60) }
    fun line(cells: List<String>) = cells.mapIndexed { i, cell ->
        val clipped = if (cell.length > widths[i]) cell.take(widths[i] - 1) + "…" else cell
        if (i == cells.lastIndex) clipped else clipped.padEnd(widths[i])
    }.joinToString("  ").trimEnd()
    return (listOf(line(headers)) + rows.map(::line)).joinToString("\n")
}
