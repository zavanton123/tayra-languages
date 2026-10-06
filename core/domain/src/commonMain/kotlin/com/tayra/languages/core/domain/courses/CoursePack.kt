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
