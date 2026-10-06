package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.User_courses
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.courses.UserCourseRepository
import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class UserCourseRepositoryImpl(
    private val provider: DatabaseProvider,
    private val clock: Clock = Clock.System,
) : UserCourseRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    // Lessons change more often than courses; both queries are watched, and the lessons read with them.
    override fun observeCourses(languageId: Long): Flow<List<Course>> = flow {
        val database = db()
        val courses = database.userCoursesQueries.selectCourses(languageId).asFlow().mapToList(databaseDispatcher)
        val lessons = database.userLessonsChanges()
        emitAll(combine(courses, lessons) { rows, _ -> withContext(databaseDispatcher) { hydrate(database, rows) } })
    }

    override fun observeCourse(courseId: String): Flow<Course?> = flow {
        val database = db()
        val courses = database.userCoursesQueries.selectCourse(courseId).asFlow().mapToList(databaseDispatcher)
        emitAll(combine(courses, database.userLessonsChanges()) { rows, _ -> withContext(databaseDispatcher) { hydrate(database, rows).firstOrNull() } })
    }

    override suspend fun course(courseId: String): Course? = withContext(databaseDispatcher) {
        val database = db()
        hydrate(database, database.userCoursesQueries.selectCourse(courseId).awaitAsList()).firstOrNull()
    }

    /** Emits whenever a lesson is added, changed or removed, for any course: a query on the table is told of every change to it. */
    private fun TayraDatabase.userLessonsChanges(): Flow<Unit> =
        userCoursesQueries.selectLessons(listOf("")).asFlow().map { }

    private suspend fun hydrate(database: TayraDatabase, rows: List<User_courses>): List<Course> {
        if (rows.isEmpty()) return emptyList()
        val lessons = database.userCoursesQueries.selectLessons(rows.map { it.id }).awaitAsList().groupBy { it.course_id }
        return rows.map { row ->
            val language = database.languagesQueries.selectById(row.language_id).awaitAsOneOrNull()
            Course(
                id = row.id,
                languageCode = language?.name?.let { LanguageCodes.codeFor(it) }.orEmpty(),
                title = row.title,
                description = row.description,
                level = CourseLevel.entries.firstOrNull { it.name == row.level } ?: CourseLevel.A1,
                topic = row.topic,
                lessons = lessons[row.id].orEmpty().map { Lesson(it.id, it.title, it.summary, it.text) },
                languageId = row.language_id,
            )
        }
    }

    override suspend fun createCourse(id: String, languageId: Long, draft: CourseDraft) = withContext(databaseDispatcher) {
        db().userCoursesQueries.insertCourse(id, languageId, draft.title, draft.description, draft.level.name, draft.topic, clock.now().toEpochMilliseconds())
        Unit
    }

    override suspend fun updateCourse(courseId: String, draft: CourseDraft) = withContext(databaseDispatcher) {
        db().userCoursesQueries.updateCourse(title = draft.title, description = draft.description, level = draft.level.name, topic = draft.topic, id = courseId)
        Unit
    }

    override suspend fun deleteCourse(courseId: String) = withContext(databaseDispatcher) {
        db().userCoursesQueries.deleteCourse(courseId)
        Unit
    }

    override suspend fun addLesson(id: String, courseId: String, draft: LessonDraft) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            val position = database.userCoursesQueries.nextPosition(courseId).awaitAsOne()
            database.userCoursesQueries.insertLesson(id, courseId, draft.title, draft.summary, draft.text, position)
        }
    }

    override suspend fun updateLesson(lessonId: String, draft: LessonDraft) = withContext(databaseDispatcher) {
        db().userCoursesQueries.updateLesson(title = draft.title, summary = draft.summary, text = draft.text, id = lessonId)
        Unit
    }

    override suspend fun deleteLesson(lessonId: String) = withContext(databaseDispatcher) {
        db().userCoursesQueries.deleteLesson(lessonId)
        Unit
    }

    override suspend fun moveLesson(lessonId: String, by: Int) = withContext(databaseDispatcher) {
        val database = db()
        database.transaction {
            val lesson = database.userCoursesQueries.selectLesson(lessonId).awaitAsOneOrNull() ?: return@transaction
            val order = database.userCoursesQueries.selectLessons(listOf(lesson.course_id)).awaitAsList().map { it.id }.toMutableList()
            val from = order.indexOf(lessonId)
            val to = (from + by).coerceIn(0, order.lastIndex)
            if (from == to) return@transaction
            order.add(to, order.removeAt(from))
            order.forEachIndexed { position, id -> database.userCoursesQueries.setPosition(position.toLong(), id) }
        }
    }
}
