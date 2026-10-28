package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.dictionary.PackState

/**
 * The sample courses of one language as one downloadable file: a gzip-compressed SQLite file
 * written by tools/build_courses.py. Its courses are written into the database once installed.
 */
data class CoursePack(
    val id: String,
    /** ISO 639-1 code of the language the courses teach. */
    val languageCode: String,
    val title: String,
    val summary: String,
    val url: String,
    /** Size of the compressed file, shown before it is downloaded. */
    val downloadSize: Long,
) {
    companion object {
        /** The layout of the pack files, matching the "format" row of their meta table and PRAGMA user_version. */
        const val FORMAT = 2
    }
}

/** The course packs the app knows how to download. */
object CoursePacks {
    private const val BASE_URL = "https://github.com/zavanton123/tayra-languages/releases/download/courses-v1"

    val all: List<CoursePack> = listOf(
        CoursePack(
            id = "courses-pt",
            languageCode = "pt",
            title = "Portuguese (Brazil)",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-pt.sqlite.gzip",
            downloadSize = 1_208_672,
        ),
        CoursePack(
            id = "courses-en",
            languageCode = "en",
            title = "English",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-en.sqlite.gzip",
            downloadSize = 1_156_031,
        ),
        CoursePack(
            id = "courses-de",
            languageCode = "de",
            title = "German",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-de.sqlite.gzip",
            downloadSize = 1_290_487,
        ),
        CoursePack(
            id = "courses-fr",
            languageCode = "fr",
            title = "French",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-fr.sqlite.gzip",
            downloadSize = 1_196_300,
        ),
        CoursePack(
            id = "courses-es",
            languageCode = "es",
            title = "Spanish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-es.sqlite.gzip",
            downloadSize = 1_212_335,
        ),
        CoursePack(
            id = "courses-ru",
            languageCode = "ru",
            title = "Russian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ru.sqlite.gzip",
            downloadSize = 1_423_874,
        ),
        CoursePack(
            id = "courses-it",
            languageCode = "it",
            title = "Italian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-it.sqlite.gzip",
            downloadSize = 1_207_602,
        ),
        CoursePack(
            id = "courses-sr",
            languageCode = "sr",
            title = "Serbian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-sr.sqlite.gzip",
            downloadSize = 1_144_990,
        ),
        CoursePack(
            id = "courses-nl",
            languageCode = "nl",
            title = "Dutch",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-nl.sqlite.gzip",
            downloadSize = 1_157_398,
        ),
        CoursePack(
            id = "courses-da",
            languageCode = "da",
            title = "Danish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-da.sqlite.gzip",
            downloadSize = 1_131_193,
        ),
        CoursePack(
            id = "courses-no",
            languageCode = "no",
            title = "Norwegian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-no.sqlite.gzip",
            downloadSize = 1_110_630,
        ),
        CoursePack(
            id = "courses-sv",
            languageCode = "sv",
            title = "Swedish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-sv.sqlite.gzip",
            downloadSize = 1_123_772,
        ),
        CoursePack(
            id = "courses-el",
            languageCode = "el",
            title = "Greek",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-el.sqlite.gzip",
            downloadSize = 1_464_113,
        ),
        CoursePack(
            id = "courses-zh",
            languageCode = "zh",
            title = "Mandarin Chinese",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-zh.sqlite.gzip",
            downloadSize = 1_186_696,
        ),
        CoursePack(
            id = "courses-ja",
            languageCode = "ja",
            title = "Japanese",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ja.sqlite.gzip",
            downloadSize = 1_140_703,
        ),
        CoursePack(
            id = "courses-tr",
            languageCode = "tr",
            title = "Turkish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-tr.sqlite.gzip",
            downloadSize = 1_181_880,
        ),
        CoursePack(
            id = "courses-uk",
            languageCode = "uk",
            title = "Ukrainian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-uk.sqlite.gzip",
            downloadSize = 1_400_024,
        ),
        CoursePack(
            id = "courses-pl",
            languageCode = "pl",
            title = "Polish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-pl.sqlite.gzip",
            downloadSize = 1_334_569,
        ),
        CoursePack(
            id = "courses-cs",
            languageCode = "cs",
            title = "Czech",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-cs.sqlite.gzip",
            downloadSize = 1_295_796,
        ),
        CoursePack(
            id = "courses-ko",
            languageCode = "ko",
            title = "Korean",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ko.sqlite.gzip",
            downloadSize = 1_228_327,
        ),
        CoursePack(
            id = "courses-hu",
            languageCode = "hu",
            title = "Hungarian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-hu.sqlite.gzip",
            downloadSize = 1_146_684,
        ),
        CoursePack(
            id = "courses-ca",
            languageCode = "ca",
            title = "Catalan",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ca.sqlite.gzip",
            downloadSize = 1_167_294,
        ),
        CoursePack(
            id = "courses-id",
            languageCode = "id",
            title = "Indonesian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-id.sqlite.gzip",
            downloadSize = 1_143_912,
        ),
        CoursePack(
            id = "courses-ar",
            languageCode = "ar",
            title = "Arabic",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ar.sqlite.gzip",
            downloadSize = 1_163_164,
        ),
    )

    fun forLanguage(code: String): List<CoursePack> = all.filter { it.languageCode == code }
}

data class CoursePackStatus(val pack: CoursePack, val state: PackState)

/** Keeps downloaded course packs on the device and reads their courses. */
interface CoursePackStore {
    /** Size of the installed pack in bytes, or null when it is not installed. */
    suspend fun installedSize(pack: CoursePack): Long?
    /** Downloads and stores the pack, reporting progress 0..1 (or null when unknown). Throws when the download fails. */
    suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit)
    suspend fun remove(pack: CoursePack)
    /** The ids of the installed pack's courses; empty when it is not installed or not readable. */
    suspend fun courseIds(pack: CoursePack): Set<String>
    /** The installed pack's courses with their lessons, in order; empty when it is not installed or not readable. */
    suspend fun courses(pack: CoursePack): List<Course>
}

/** The sample courses are the ones in the installed packs. */
class InstalledCoursePacks(private val store: CoursePackStore) : SampleCourseSource {
    override suspend fun courseIds(languageCode: String): Set<String> =
        CoursePacks.forLanguage(languageCode).flatMap { store.courseIds(it) }.toSet()

    override suspend fun courses(languageCode: String): List<Course> =
        CoursePacks.forLanguage(languageCode).flatMap { store.courses(it) }
}
