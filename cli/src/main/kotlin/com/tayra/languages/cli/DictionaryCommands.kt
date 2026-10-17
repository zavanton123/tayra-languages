package com.tayra.languages.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.tayra.languages.core.data.mdict.DictionaryFiles
import com.tayra.languages.core.data.mdict.MdictException
import com.tayra.languages.core.data.stardict.StardictException
import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.serialization.Serializable
import java.io.File

// ---- Dictionaries

fun dictionariesCommand() = group(
    "dictionaries", "Dictionary files: turn a Yomitan, MDict or StarDict dictionary into the app's offline dictionary pack",
    DictionariesConvert(),
)

@Serializable
data class ConvertedDictionaryJson(val title: String, val revision: String, val attribution: String?, val out: String, val sizeBytes: Long)

private class DictionariesConvert : Command(
    "convert",
    "Write a dictionary's pack, the SQLite file the app imports it as, without adding it to the library",
) {
    private val files by argument(help = "The dictionary: a Yomitan .zip, an MDict .mdx, or a StarDict archive or its .ifo, .idx and .dict files").multiple(required = true)
    private val from by option("--from", help = "The ISO code of the language of its headwords").required()
    private val to by option("--to", help = "The ISO code of the language it explains them in").required()
    private val out by option("--out", help = "Where to write the pack").required()

    override fun run() = respond(cli, ConvertedDictionaryJson.serializer()) {
        for (code in listOf(from, to)) if (LanguageCodes.option(code) == null) invalid("No language with the ISO code '$code'")
        val file = DictionaryFiles.bundle(files.map { path -> File(path).let { it.name to (it.takeIf(File::isFile)?.readBytes() ?: notFound("No file $path")) } })
        val importer = DictionaryFiles()
        try {
            val info = importer.inspect(file)
            File(out).writeBytes(importer.convert(file, from, to))
            val json = ConvertedDictionaryJson(info.title, info.revision, info.attribution, out, File(out).length())
            Output(json, "Wrote ${info.title} to $out (${json.sizeBytes / 1_000_000} MB)")
        } catch (e: MdictException) {
            invalid(e.message ?: "Not a dictionary this app can import")
        } catch (e: StardictException) {
            invalid(e.message ?: "Not a dictionary this app can import")
        }
    }
}
