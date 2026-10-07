package com.tayra.languages.cli

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.tayra.languages.core.data.runtime.AppInstanceLock
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.domain.frequency.VocabularyLevelRepository
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.service.StatsService
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

// ---- Backups

fun backupsCommand() = group(
    "backups", "Complete copies of the library and settings: make, list, restore, import and export them",
    BackupsList(), BackupsCreate(), BackupsRestore(), BackupsImport(), BackupsExport(), BackupsDelete(),
)

@Serializable
data class BackupJson(val name: String, val created: String?, val sizeBytes: Long)

private fun Backup.toJson() = BackupJson(name, createdAt?.toString(), sizeBytes)

private suspend fun Cli.backup(name: String): Backup =
    get<BackupRepository>().list().firstOrNull { it.name == name } ?: notFound("No backup $name; `tayra backups list` shows them")

private class BackupsList : Command("list", "List the backups, newest first") {
    override fun run() = respond(cli, ListSerializer(BackupJson.serializer())) {
        val list = cli.get<BackupRepository>().list().map { it.toJson() }
        Output(list, table(listOf("NAME", "CREATED", "SIZE"), list.map { listOf(it.name, it.created?.replace('T', ' ') ?: "", "${it.sizeBytes / 1024} KB") }).ifEmpty { "No backups." })
    }
}

private class BackupsCreate : Command("create", "Back up the library and settings now; prints the backup's name") {
    override fun run() = respond(cli, BackupJson.serializer()) {
        val backup = cli.get<BackupRepository>().create().toJson()
        Output(backup, "Created ${backup.name}")
    }
}

private class BackupsRestore : Command("restore", "Replace the library and settings with a backup's") {
    private val name by argument(help = "Backup name")
    private val yes by option("--yes", help = "Confirm: everything not in the backup is lost").flag()
    private val force by option("--force", help = "Restore although the app is open; close it right after without changing anything").flag()

    override fun run() = respond(cli, BackupJson.serializer(), changes = true) {
        val backup = cli.backup(name)
        if (!yes) invalid("Restoring replaces the whole library and settings with ${backup.name}; repeat with --yes")
        if (AppInstanceLock.isAppOpen() && !force) {
            throw CliFailure("Tayra Languages is open, and would write its own state over the restored one. Close it first, or add --force.", ExitCode.CONFLICT)
        }
        cli.get<BackupRepository>().restore(backup.name)
        Output(backup.toJson(), "Restored ${backup.name}")
    }
}

private class BackupsImport : Command("import", "Add a backup file to the list, to restore it later") {
    private val file by argument(help = "A backup file, such as one exported from another computer")

    override fun run() = respond(cli, BackupJson.serializer()) {
        val bytes = File(file).takeIf { it.isFile }?.readBytes() ?: notFound("No file $file")
        val backup = cli.get<BackupRepository>().import(bytes).toJson()
        Output(backup, "Imported as ${backup.name}; restore it with `tayra backups restore ${backup.name} --yes`")
    }
}

private class BackupsExport : Command("export", "Copy a backup to a file") {
    private val name by argument(help = "Backup name")
    private val out by option("--out", help = "Where to write it").required()

    override fun run() = respond(cli, DoneJson.serializer()) {
        val backup = cli.backup(name)
        File(out).writeBytes(cli.get<BackupRepository>().read(backup.name))
        Output(DoneJson(message = "Wrote $out"), "Wrote ${backup.name} to $out")
    }
}

private class BackupsDelete : Command("delete", "Delete a backup") {
    private val name by argument(help = "Backup name")
    private val yes by option("--yes", help = "Confirm the deletion").flag()

    override fun run() = respond(cli, DoneJson.serializer()) {
        val backup = cli.backup(name)
        if (!yes) invalid("Deleting ${backup.name} cannot be undone; repeat with --yes")
        cli.get<BackupRepository>().delete(backup.name)
        Output(DoneJson(message = "Deleted ${backup.name}"), "Deleted ${backup.name}")
    }
}

// ---- Statistics

@Serializable
data class StatsJson(
    val language: String,
    val wordsRead: Int,
    val wordsReadThisWeek: Int,
    val daysRead: Int,
    val wordsPerReadingDay: Int,
    val streak: Int,
    val longestStreak: Int,
    val knownTerms: Int,
    val learningTerms: Int,
    val ignoredTerms: Int,
    val termsByStatus: Map<String, Int>,
)

class StatsCommand : Command("stats", "Reading and vocabulary statistics of the language") {
    override fun run() = respond(cli, StatsJson.serializer()) {
        val language = cli.language()
        val o = cli.get<StatsService>().overview(language.id, language.name)
        val json = StatsJson(
            o.languageName, o.wordsRead, o.wordsReadThisWeek, o.daysRead, o.wordsPerReadingDay, o.streak, o.longestStreak,
            o.known, o.learning, o.ignored, o.termsByStatus.mapKeys { it.key.cliName },
        )
        Output(json, """
            ${json.language}
              words read:      ${json.wordsRead} (${json.wordsReadThisWeek} this week, ${json.wordsPerReadingDay} on a reading day)
              days with reading: ${json.daysRead}; streak ${json.streak}, longest ${json.longestStreak}
              terms:           ${json.knownTerms} known, ${json.learningTerms} learning, ${json.ignoredTerms} ignored
        """.trimIndent())
    }
}

// ---- Settings

fun settingsCommand() = group(
    "settings", "The app's settings, by the names they are stored under",
    SettingsGet(), SettingsSet(),
)

private class SettingsGet : Command("get", "Print one setting, or all of them") {
    private val key by argument(help = "Setting name; default: all").optional()

    override fun run() = respond(cli, MapSerializer(String.serializer(), String.serializer())) {
        val all = cli.get<SettingsRepositoryImpl>().backupValues().toSortedMap()
        val shown = key?.let { k -> mapOf(k to (all[k] ?: notFound("No setting '$k'; `tayra settings get` lists them"))) } ?: all
        Output(shown, if (key != null) shown.values.single() else table(listOf("SETTING", "VALUE"), shown.map { listOf(it.key, it.value) }))
    }
}

private class SettingsSet : Command("set", "Change a setting; the value must have the type the setting has") {
    private val key by argument(help = "Setting name, as `tayra settings get` lists them")
    private val value by argument(help = "New value")

    override fun run() = respond(cli, MapSerializer(String.serializer(), String.serializer()), changes = true) {
        val settings = cli.get<SettingsRepositoryImpl>()
        val values = settings.backupValues().toMutableMap()
        val old = values[key] ?: notFound("No setting '$key'; `tayra settings get` lists them")
        when {
            old == "true" || old == "false" -> if (value != "true" && value != "false") invalid("$key is true or false")
            old.toLongOrNull() != null -> if (value.toLongOrNull() == null) invalid("$key is a whole number")
            old.toDoubleOrNull() != null -> if (value.toDoubleOrNull() == null) invalid("$key is a number")
        }
        values[key] = value
        settings.restoreBackupValues(values)
        val now = settings.backupValues()[key] ?: value
        Output(mapOf(key to now), "$key = $now")
    }
}

// ---- Vocabulary level

fun levelCommand() = group(
    "level", "The vocabulary level: the most common words of the language counted as known, up to a rank",
    LevelGet(), LevelSet(),
)

@Serializable
data class LevelJson(val language: String, val level: Int, val added: Int? = null, val removed: Int? = null)

private class LevelGet : Command("get", "Print the language's vocabulary level") {
    override fun run() = respond(cli, LevelJson.serializer()) {
        val language = cli.language()
        val level = cli.get<VocabularyLevelRepository>().level(language.id) ?: 0
        Output(LevelJson(language.name, level), "${language.name}: the $level most common words are known")
    }
}

private class LevelSet : Command("set", "Count the most common words up to a rank as known; a lower level takes back the words it no longer covers") {
    private val level by argument(help = "Rank, such as 1000; 0 for a beginner").int().restrictTo(min = 0)

    override fun run() = respond(cli, LevelJson.serializer(), changes = true) {
        val language = cli.language()
        val change = cli.get<VocabularyLevelService>().setLevel(language.id, level) ?: notFound("The app has no word frequency list for ${language.name}")
        Output(LevelJson(language.name, change.to, change.added, change.removed), "${language.name}: level ${change.to}, ${change.added} words added as known, ${change.removed} taken back")
    }
}

// ---- Self-description

@Serializable
data class ParameterJson(val names: List<String>, val help: String)

@Serializable
data class CommandJson(val command: String, val help: String, val arguments: List<ParameterJson>, val options: List<ParameterJson>, val subcommands: List<CommandJson>)

class CommandsCommand : CliktCommand(name = "commands") {
    private val cli by requireObject<Cli>()

    override fun help(context: Context) = "Describe every command, argument and option; with --json, for AI agents to read"

    override fun run() {
        val root = currentContext.findRoot().command
        fun describe(command: BaseCliktCommand<*>, path: String): CommandJson {
            val context = currentContext
            return CommandJson(
                path,
                command.help(context).lines().first(),
                command.registeredArguments().map { ParameterJson(listOf(it.name.lowercase()), it.getArgumentHelp(context)) },
                command.registeredOptions().map { ParameterJson(it.names.sortedBy { n -> n.length }, it.optionHelp(context)) },
                command.registeredSubcommands().map { describe(it, "$path ${it.commandName}") },
            )
        }
        val tree = describe(root, root.commandName)
        if (cli.json) {
            echo(cli.encode(CommandJson.serializer(), tree))
        } else {
            fun print(c: CommandJson, depth: Int) {
                if (c.subcommands.isEmpty() || depth == 0) echo("${c.command.padEnd(32)} ${c.help}")
                c.subcommands.forEach { print(it, depth + 1) }
            }
            print(tree, 0)
        }
    }
}
