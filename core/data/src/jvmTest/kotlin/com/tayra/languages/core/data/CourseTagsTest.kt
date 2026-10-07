package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.backup.BackupRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.model.Language
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** A tag is stored once and shared by the courses that have it; databases and backups from before keep their tags. */
class CourseTagsTest {

    private val file = File.createTempFile("tayra-course-tags", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val courses = CourseRepositoryImpl(provider)
    private val languageId = runBlocking { LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")) }

    private fun rows(sql: String): List<String> = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
        c.createStatement().executeQuery(sql).let { r -> buildList { while (r.next()) add(r.getString(1)) } }
    }

    @Test
    fun coursesShareTheirTags() = runBlocking {
        courses.createCourse("trip", languageId, CourseDraft("Uma viagem", tags = listOf("travel", "food")))
        courses.createCourse("market", languageId, CourseDraft("No mercado", tags = listOf("food")))
        assertEquals(listOf("food", "travel"), rows("SELECT text FROM course_tags ORDER BY text"))
        assertEquals(3, rows("SELECT course_id FROM course_tag_map").size)
        assertEquals(listOf("food", "travel"), courses.course("trip")?.tags)

        // A tag another course still has stays when one course drops it; the last course to go takes it along.
        courses.updateCourse("trip", CourseDraft("Uma viagem", tags = listOf("travel")))
        assertEquals(listOf("food", "travel"), rows("SELECT text FROM course_tags ORDER BY text"))
        courses.deleteCourse("market")
        assertEquals(listOf("travel"), rows("SELECT text FROM course_tags"))
        assertEquals(listOf("travel"), courses.course("trip")?.tags)
    }

    @Test
    fun aDatabaseWithTagsInTheCourseRowGetsThemLinked() = runBlocking {
        courses.createCourse("a", languageId, CourseDraft("A"))
        courses.createCourse("b", languageId, CourseDraft("B"))
        // A database from before the change, the tags as text in the course rows.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            for (sql in listOf(
                "DROP TABLE course_tag_map",
                "DROP TABLE course_tags",
                "ALTER TABLE courses ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
                "UPDATE courses SET tags = 'tayra  travel' WHERE id = 'a'",
                "UPDATE courses SET tags = 'tayra' WHERE id = 'b'",
                "PRAGMA user_version = 18",
            )) c.createStatement().execute(sql)
        }
        val reopened = CourseRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals(listOf("tayra", "travel"), reopened.course("a")?.tags)
        assertEquals(listOf("tayra"), reopened.course("b")?.tags)
        assertEquals(listOf("tayra", "travel"), rows("SELECT text FROM course_tags ORDER BY text"))
        assertEquals(0, rows("SELECT name FROM pragma_table_info('courses') WHERE name = 'tags'").size)
    }

    @Test
    fun aBackupWithTagsInTheCourseRowRestoresThem() = runBlocking {
        courses.createCourse("a", languageId, CourseDraft("A", tags = listOf("tayra")))
        val folder: File = Files.createTempDirectory("tayra-course-tag-backups").toFile()
        val backups = BackupRepositoryImpl(provider, SettingsRepositoryImpl(MapSettings()), BackupFiles(folder))
        val backup = backups.create()
        // The backup is made to look like one from schema 18: tags in the course rows, no tag tables.
        DriverManager.getConnection("jdbc:sqlite:${File(folder, backup.name).absolutePath}").use { c ->
            for (sql in listOf(
                "DROP TABLE course_tag_map",
                "DROP TABLE course_tags",
                "ALTER TABLE courses ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
                "UPDATE courses SET tags = 'tayra food' WHERE id = 'a'",
                "UPDATE backup_info SET value = '18' WHERE key = 'schema_version'",
            )) c.createStatement().execute(sql)
        }
        courses.updateCourse("a", CourseDraft("A"))
        backups.restore(backup.name)
        assertEquals(listOf("food", "tayra"), courses.course("a")?.tags)
    }
}
