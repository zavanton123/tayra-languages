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
            downloadSize = 1_196_510,
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
            downloadSize = 1_270_981,
        ),
        CoursePack(
            id = "courses-fr",
            languageCode = "fr",
            title = "French",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-fr.sqlite.gzip",
            downloadSize = 1_185_601,
        ),
        CoursePack(
            id = "courses-es",
            languageCode = "es",
            title = "Spanish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-es.sqlite.gzip",
            downloadSize = 1_203_414,
        ),
        CoursePack(
            id = "courses-ru",
            languageCode = "ru",
            title = "Russian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ru.sqlite.gzip",
            downloadSize = 1_420_606,
        ),
        CoursePack(
            id = "courses-it",
            languageCode = "it",
            title = "Italian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-it.sqlite.gzip",
            downloadSize = 1_203_497,
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
            downloadSize = 1_150_781,
        ),
        CoursePack(
            id = "courses-da",
            languageCode = "da",
            title = "Danish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-da.sqlite.gzip",
            downloadSize = 1_103_743,
        ),
        CoursePack(
            id = "courses-no",
            languageCode = "no",
            title = "Norwegian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-no.sqlite.gzip",
            downloadSize = 1_109_330,
        ),
        CoursePack(
            id = "courses-sv",
            languageCode = "sv",
            title = "Swedish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-sv.sqlite.gzip",
            downloadSize = 1_106_196,
        ),
        CoursePack(
            id = "courses-el",
            languageCode = "el",
            title = "Greek",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-el.sqlite.gzip",
            downloadSize = 1_461_852,
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
            downloadSize = 1_181_875,
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
            downloadSize = 1_166_887,
        ),
        CoursePack(
            id = "courses-id",
            languageCode = "id",
            title = "Indonesian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-id.sqlite.gzip",
            downloadSize = 1_143_665,
        ),
        CoursePack(
            id = "courses-ar",
            languageCode = "ar",
            title = "Arabic",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ar.sqlite.gzip",
            downloadSize = 1_163_057,
        ),
        CoursePack(
            id = "courses-sk",
            languageCode = "sk",
            title = "Slovak",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-sk.sqlite.gzip",
            downloadSize = 1_299_474,
        ),
        CoursePack(
            id = "courses-gl",
            languageCode = "gl",
            title = "Galician",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-gl.sqlite.gzip",
            downloadSize = 1_151_350,
        ),
        CoursePack(
            id = "courses-ro",
            languageCode = "ro",
            title = "Romanian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ro.sqlite.gzip",
            downloadSize = 1_176_099,
        ),
        CoursePack(
            id = "courses-mk",
            languageCode = "mk",
            title = "Macedonian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-mk.sqlite.gzip",
            downloadSize = 1_353_799,
        ),
        CoursePack(
            id = "courses-hr",
            languageCode = "hr",
            title = "Croatian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-hr.sqlite.gzip",
            downloadSize = 1_231_387,
        ),
        CoursePack(
            id = "courses-bg",
            languageCode = "bg",
            title = "Bulgarian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-bg.sqlite.gzip",
            downloadSize = 1_423_473,
        ),
        CoursePack(
            id = "courses-bs",
            languageCode = "bs",
            title = "Bosnian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-bs.sqlite.gzip",
            downloadSize = 1_222_892,
        ),
        CoursePack(
            id = "courses-sl",
            languageCode = "sl",
            title = "Slovene",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-sl.sqlite.gzip",
            downloadSize = 1_240_720,
        ),
        CoursePack(
            id = "courses-eo",
            languageCode = "eo",
            title = "Esperanto",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-eo.sqlite.gzip",
            downloadSize = 1_179_788,
        ),
        CoursePack(
            id = "courses-fi",
            languageCode = "fi",
            title = "Finnish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-fi.sqlite.gzip",
            downloadSize = 1_196_080,
        ),
        CoursePack(
            id = "courses-lt",
            languageCode = "lt",
            title = "Lithuanian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-lt.sqlite.gzip",
            downloadSize = 1_223_909,
        ),
        CoursePack(
            id = "courses-et",
            languageCode = "et",
            title = "Estonian",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-et.sqlite.gzip",
            downloadSize = 1_158_896,
        ),
        CoursePack(
            id = "courses-is",
            languageCode = "is",
            title = "Icelandic",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-is.sqlite.gzip",
            downloadSize = 1_222_579,
        ),
        CoursePack(
            id = "courses-la",
            languageCode = "la",
            title = "Latin",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-la.sqlite.gzip",
            downloadSize = 1_164_257,
        ),
        CoursePack(
            id = "courses-tl",
            languageCode = "tl",
            title = "Tagalog",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-tl.sqlite.gzip",
            downloadSize = 1_133_825,
        ),
        CoursePack(
            id = "courses-ga",
            languageCode = "ga",
            title = "Irish",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-ga.sqlite.gzip",
            downloadSize = 1_162_579,
        ),
        CoursePack(
            id = "courses-cy",
            languageCode = "cy",
            title = "Welsh",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, built on the 10,000 most common words.",
            url = "$BASE_URL/courses-cy.sqlite.gzip",
            downloadSize = 1_092_492,
        ),
        CoursePack(
            id = "courses-yue",
            languageCode = "yue",
            title = "Cantonese",
            summary = "100 courses, 1,000 lessons: graded mini stories from A1 to C2, in everyday written Cantonese.",
            url = "$BASE_URL/courses-yue.sqlite.gzip",
            downloadSize = 1_180_182,
        ),
        CoursePack(
            id = "courses-gd",
            languageCode = "gd",
            title = "Scottish Gaelic",
            summary = "71 courses, 710 lessons: graded mini stories from A1 to C1, built on the 7,100 most common words.",
            url = "$BASE_URL/courses-gd.sqlite.gzip",
            downloadSize = 728_768,
        ),
        CoursePack(
            id = "courses-hi",
            languageCode = "hi",
            title = "Hindi",
            summary = "96 courses, 960 lessons: graded mini stories from A1 to C2, built on the 9,600 most common words.",
            url = "$BASE_URL/courses-hi.sqlite.gzip",
            downloadSize = 1_397_523,
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
