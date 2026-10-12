package com.tayra.languages.core.domain.books

import com.tayra.languages.core.domain.dictionary.PackState

/**
 * A bundle of books in one language as one downloadable file: a gzip-compressed SQLite file
 * written by tools/build_books.py from free texts (public domain or CC BY-SA). Its books are
 * added to the library once installed, like books the reader makes.
 */
data class BookPack(
    val id: String,
    /** ISO 639-1 code of the language the books are in. */
    val languageCode: String,
    val title: String,
    val summary: String,
    val url: String,
    /** Size of the compressed file, shown before it is downloaded. */
    val downloadSize: Long,
) {
    companion object {
        /** The layout of the pack files, matching the "format" row of their meta table and PRAGMA user_version. */
        const val FORMAT = 1
    }
}

/** A book of a pack: the text with what the library needs to know about it. */
data class PackedBook(
    val id: String,
    val level: String,
    val title: String,
    val author: String,
    val summary: String,
    /** Where the text comes from, named in the library as the book's source. */
    val sourceUrl: String,
    val license: String,
    val tags: List<String>,
    val text: String,
)

/** The book packs the app knows how to download. */
object BookPacks {
    private const val BASE_URL = "https://github.com/zavanton123/tayra-languages/releases/download/books-v1"

    /** The source every book of a pack is marked with, followed by the pack's id and the book's: how the library tells them from the reader's own books. */
    const val SOURCE_PREFIX = "tayra-books://"

    val all: List<BookPack> = listOf(
        BookPack(
            id = "books-pt",
            languageCode = "pt",
            title = "Portuguese",
            summary = "Authentic texts from A2 to C2: children's books, Wikipedia reads, and the stories and novels of Machado de Assis and Lima Barreto.",
            url = "$BASE_URL/books-pt.sqlite.gzip",
            downloadSize = 1_166_895,
        ),
    )

    fun forLanguage(code: String): List<BookPack> = all.filter { it.languageCode == code }

    /** The source a book of [pack] with [bookId] is marked with. */
    fun sourceOf(pack: BookPack, bookId: String): String = "$SOURCE_PREFIX${pack.id}/$bookId"
}

data class BookPackStatus(val pack: BookPack, val state: PackState)

/** Keeps downloaded book packs on the device and reads their books. */
interface BookPackStore {
    /** Size of the installed pack in bytes, or null when it is not installed. */
    suspend fun installedSize(pack: BookPack): Long?
    /** Downloads and stores the pack, reporting progress 0..1 (or null when unknown). Throws when the download fails. */
    suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit)
    suspend fun remove(pack: BookPack)
    /** The installed pack's books in order; empty when it is not installed or not readable. */
    suspend fun books(pack: BookPack): List<PackedBook>
}
