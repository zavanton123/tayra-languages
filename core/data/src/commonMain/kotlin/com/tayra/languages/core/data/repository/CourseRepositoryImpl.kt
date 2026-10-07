package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.Courses
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.courses.CourseRepository
import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class CourseRepositoryImpl(
    private val provider: DatabaseProvider,
    private val clock: Clock = Clock.System,
) : CourseRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    // Lessons and tags change apart from courses; all three are watched, and lessons and tags read with the courses.
    override fun observeCourses(languageId: Long): Flow<List<Course>> = flow {
        val database = db()
        val courses = database.coursesQueries.selectCourses(languageId).asFlow().mapToList(databaseDispatcher)
        emitAll(combine(courses, database.lessonsChanges(), database.tagsChanges()) { rows, _, _ -> withContext(databaseDispatcher) { hydrate(database, rows) } })
    }

    override fun observeCourse(courseId: String): Flow<Course?> = flow {
        val database = db()
        val courses = database.coursesQueries.selectCourse(courseId).asFlow().mapToList(databaseDispatcher)
        emitAll(combine(courses, database.lessonsChanges(), database.tagsChanges()) { rows, _, _ -> withContext(databaseDispatcher) { hydrate(database, rows).firstOrNull() } })
    }

    override suspend fun course(courseId: String): Course? = withContext(databaseDispatcher) {
        val database = db()
        hydrate(database, database.coursesQueries.selectCourse(courseId).awaitAsList()).firstOrNull()
    }

    /** Emits whenever a lesson is added, changed or removed, for any course: a query on the table is told of every change to it. */
    private fun TayraDatabase.lessonsChanges(): Flow<Unit> =
        coursesQueries.selectLessons(listOf("")).asFlow().map { }

    /** Emits whenever a course's tags change, as [lessonsChanges] does for lessons. */
    private fun TayraDatabase.tagsChanges(): Flow<Unit> =
        coursesQueries.courseTags(listOf("")).asFlow().map { }

    private suspend fun hydrate(database: TayraDatabase, rows: List<Courses>): List<Course> {
        if (rows.isEmpty()) return emptyList()
        val lessons = database.coursesQueries.selectLessons(rows.map { it.id }).awaitAsList().groupBy { it.course_id }
        val tags = database.coursesQueries.courseTags(rows.map { it.id }).awaitAsList().groupBy({ it.course_id }, { it.text })
        return rows.map { row ->
            val language = database.languagesQueries.selectById(row.language_id).awaitAsOneOrNull()
            Course(
                id = row.id,
                languageCode = language?.name?.let { LanguageCodes.codeFor(it) }.orEmpty(),
                title = row.title,
                description = row.description,
                level = CourseLevel.entries.firstOrNull { it.name == row.level } ?: CourseLevel.A1,
                topic = row.topic,
                lessons = lessons[row.id].orEmpty().map { Lesson(it.id, it.title, it.summary, it.text, words(it.new_words), words(it.tags)) },
                languageId = row.language_id,
                builtIn = row.built_in,
                rankUpTo = row.rank_up_to?.toInt(),
                tags = tags[row.id].orEmpty(),
            )
        }
    }

    private fun words(spaced: String): List<String> = spaced.split(' ').filter { it.isNotBlank() }

    override suspend fun createCourse(id: String, languageId: Long, draft: CourseDraft) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.coursesQueries.insertCourse(id, languageId, draft.title, draft.description, draft.level.name, draft.topic, clock.now().toEpochMilliseconds(), false, null)
            setTags(database, id, draft.tags)
        }
    }

    override suspend fun updateCourse(courseId: String, draft: CourseDraft) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.coursesQueries.updateCourse(title = draft.title, description = draft.description, level = draft.level.name, topic = draft.topic, id = courseId)
            setTags(database, courseId, draft.tags)
        }
    }

    override suspend fun deleteCourse(courseId: String) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            database.coursesQueries.deleteCourse(courseId)
            database.coursesQueries.deleteUnusedTags()
        }
    }

    /** Links the course to exactly [tags], adding the tags not stored yet and dropping the ones no course has any more. */
    private suspend fun setTags(database: TayraDatabase, courseId: String, tags: List<String>) {
        database.coursesQueries.unlinkTags(courseId)
        linkTags(database, courseId, tags)
        database.coursesQueries.deleteUnusedTags()
    }

    private suspend fun linkTags(database: TayraDatabase, courseId: String, tags: List<String>) {
        for (tag in tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
            database.coursesQueries.insertTag(tag)
            database.coursesQueries.linkTag(courseId, database.coursesQueries.tagId(tag).awaitAsOne())
        }
    }

    override suspend fun addLesson(id: String, courseId: String, draft: LessonDraft) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            val position = database.coursesQueries.nextPosition(courseId).awaitAsOne()
            database.coursesQueries.insertLesson(id, courseId, draft.title, draft.summary, draft.text, position, "", "")
        }
    }

    override suspend fun updateLesson(lessonId: String, draft: LessonDraft) = withContext(databaseDispatcher) {
        db().coursesQueries.updateLesson(title = draft.title, summary = draft.summary, text = draft.text, id = lessonId)
        Unit
    }

    override suspend fun deleteLesson(lessonId: String) = withContext(databaseDispatcher) {
        db().coursesQueries.deleteLesson(lessonId)
        Unit
    }

    override suspend fun seededSamples(languageId: Long): Set<String> = withContext(databaseDispatcher) {
        db().coursesQueries.seededCourses(languageId).awaitAsList().toSet()
    }

    override suspend fun seedSamples(languageId: Long, courses: List<Course>) = withContext(databaseDispatcher) {
        val database = db()
        val now = clock.now().toEpochMilliseconds()
        database.transaction {
            courses.forEachIndexed { i, course ->
                // A millisecond apart and just before now, so the samples keep their order and come before courses made after them.
                // A course already there (seeded before seeding was noted course by course) is left as it is.
                if (database.coursesQueries.selectCourse(course.id).awaitAsOneOrNull() == null) {
                    database.coursesQueries.insertCourse(
                        course.id, languageId, course.title, course.description, course.level.name, course.topic, now - courses.size + i, true, course.rankUpTo?.toLong(),
                    )
                    linkTags(database, course.id, course.tags)
                    course.lessons.forEachIndexed { position, lesson ->
                        database.coursesQueries.insertLesson(lesson.id, course.id, lesson.title, lesson.summary, lesson.text, position.toLong(), lesson.newWords.joinToString(" "), lesson.tags.joinToString(" "))
                    }
                }
                database.coursesQueries.markSeeded(course.id, languageId)
            }
        }
    }

    override suspend fun forgetSeeded(courseIds: Collection<String>) = withContext(databaseDispatcher) {
        if (courseIds.isNotEmpty()) db().coursesQueries.forgetSeeded(courseIds)
        Unit
    }

    override suspend fun moveLesson(lessonId: String, by: Int) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            val lesson = database.coursesQueries.selectLesson(lessonId).awaitAsOneOrNull() ?: return@transaction
            val order = database.coursesQueries.selectLessons(listOf(lesson.course_id)).awaitAsList().map { it.id }.toMutableList()
            val from = order.indexOf(lessonId)
            val to = (from + by).coerceIn(0, order.lastIndex)
            if (from == to) return@transaction
            order.add(to, order.removeAt(from))
            order.forEachIndexed { position, id -> database.coursesQueries.setPosition(position.toLong(), id) }
        }
    }
}
