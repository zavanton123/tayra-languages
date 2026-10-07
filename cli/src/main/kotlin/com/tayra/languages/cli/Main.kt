package com.tayra.languages.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    tayra().main(args)
    // Database and network threads would otherwise keep the JVM alive after the command is done.
    exitProcess(ExitCode.OK)
}

/** The whole command tree. */
fun tayra(): CliktCommand = Tayra().subcommands(
    InitCommand(),
    languagesCommand(),
    booksCommand(),
    coursesCommand(),
    lessonsCommand(),
    packsCommand(),
    termsCommand(),
    readCommand(),
    exportCommand(),
    backupsCommand(),
    StatsCommand(),
    settingsCommand(),
    levelCommand(),
    CommandsCommand(),
)

class Tayra : CliktCommand(name = "tayra") {
    private val json by option("--json", help = "Print results as JSON, for scripts and AI agents").flag()
    private val language by option("-l", "--language", help = "The language to work in, by name or ISO code; default: the one being learned in the app")
    private val dataDir by option("--data-dir", envvar = "TAYRA_DATA_DIR", help = "Use this data folder instead of the app's, such as a copy to try things on").file(canBeFile = false)
    private val verbose by option("-v", "--verbose", help = "Log what the app does, on stderr").flag()

    override fun help(context: Context) = """
        Tayra Languages from the command line: the same library, courses, words and backups as the app.

        Results go to stdout, messages and errors to stderr. Exit codes: 0 done, 1 bad arguments or unexpected error,
        2 not found, 3 input refused, 4 conflicts with the open app.

        Start with `tayra init` on a new computer, then `tayra books list`. `tayra commands --json` describes every command.
    """.trimIndent()

    override fun run() {
        val cli = Cli(json, language, dataDir, verbose)
        currentContext.obj = cli
        currentContext.callOnClose { cli.close() }
    }
}
