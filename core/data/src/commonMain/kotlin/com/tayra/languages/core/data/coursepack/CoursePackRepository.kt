package com.tayra.languages.core.data.coursepack

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackStore
import com.tayra.languages.core.domain.courses.Lesson
import kotlinx.coroutines.withContext

/** Reads the courses of the installed packs; a file of another format reads as empty. */
class CoursePackRepository(private val files: CoursePackFiles) : CoursePackStore {
    override suspend fun installedSize(pack: CoursePack): Long? = files.installedSize(pack)

    override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) = files.install(pack, onProgress)

    override suspend fun remove(pack: CoursePack) = files.remove(pack)

    override suspend fun courseIds(pack: CoursePack): Set<String> =
        read(pack) { it.coursePackQueries.selectCourseIds().awaitAsList().toSet() }.orEmpty()

    override suspend fun courses(pack: CoursePack): List<Course> = read(pack) { database ->
        val lessons = database.coursePackQueries.selectLessons().awaitAsList().groupBy { it.course_id }
        database.coursePackQueries.selectCourses().awaitAsList().map { row ->
            Course(
                id = row.id,
                languageCode = pack.languageCode,
                title = row.title,
                description = row.description,
                level = CourseLevel.entries.firstOrNull { it.code == row.level } ?: CourseLevel.A1,
                topic = row.topic,
                lessons = lessons[row.id].orEmpty().map { Lesson(it.id, it.title, it.summary, it.text, words(it.new_words), words(it.tags)) },
                rankUpTo = row.rank_up_to?.toInt(),
                tags = words(row.tags),
            )
        }
    }.orEmpty()

    private fun words(spaced: String): List<String> = spaced.split(' ').filter { it.isNotBlank() }

    private suspend fun <T> read(pack: CoursePack, block: suspend (CoursePackDatabase) -> T): T? = withContext(databaseDispatcher) {
        val driver = try {
            files.openDriver(pack)
        } catch (e: Exception) {
            Logger.w(e) { "Could not open course pack ${pack.id}" }
            null
        } ?: return@withContext null
        try {
            val database = CoursePackDatabase(driver)
            val format = database.coursePackQueries.selectMeta("format").awaitAsOneOrNull()?.toIntOrNull()
            if (format == CoursePack.FORMAT) {
                block(database)
            } else {
                Logger.w { "Course pack ${pack.id} has format $format, expected ${CoursePack.FORMAT}" }
                null
            }
        } catch (e: Exception) {
            Logger.w(e) { "Course pack ${pack.id} is not readable" }
            null
        } finally {
            driver.close()
        }
    }
}
