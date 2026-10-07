package com.tayra.languages.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.int
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.language.LanguageCodes
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

fun coursesCommand() = group(
    "courses", "The courses of the language: list, show, create, change and delete them; `tayra lessons` handles their lessons",
    CoursesList(), CoursesShow(), CoursesCreate(), CoursesEdit(), CoursesDelete(),
)

fun lessonsCommand() = group(
    "lessons", "The lessons of a course: add, change, reorder, delete, and open one to read it",
    LessonsAdd(), LessonsEdit(), LessonsMove(), LessonsDelete(), LessonsOpen(),
)

private const val LEVEL_HELP = "A1, A2, B1, B2, C1 or C2"

private fun parseLevel(text: String): CourseLevel =
    CourseLevel.entries.firstOrNull { it.code.equals(text.trim(), ignoreCase = true) || it.name.equals(text.trim(), ignoreCase = true) }
        ?: invalid("Unknown level '$text'; use $LEVEL_HELP")

private suspend fun Cli.course(id: String): CourseProgress =
    get<CourseService>().observeCourse(id).first() ?: notFound("No course $id; `tayra courses list` shows their ids")

private class CoursesList : Command("list", "List the courses of the language, by level") {
    private val level by option("--level", help = "Only this level: $LEVEL_HELP")
    private val tags by option("--tag", help = "Only courses with this tag; repeat for courses with all of them").multiple()
    private val mine by option("--mine", help = "Only the courses you made, not the ready-made ones").flag()

    override fun run() = respond(cli, ListSerializer(CourseJson.serializer())) {
        val wanted = level?.let(::parseLevel)
        val courses = cli.get<CourseService>().observeCourses(cli.language().id).first()
            .filter { wanted == null || it.course.level == wanted }
            .filter { !mine || !it.course.builtIn }
            .filter { c -> tags.all { tag -> c.course.tags.any { it.equals(tag, ignoreCase = true) } } }
            .sortedWith(compareBy({ it.course.level.ordinal }, { it.course.rankUpTo ?: Int.MAX_VALUE }, { it.course.title }))
            .map { it.toJson() }
        Output(courses, table(
            listOf("ID", "TITLE", "LEVEL", "LESSONS", "WORDS", "TAGS"),
            courses.map { listOf(it.id, it.title, it.level, "${it.completedLessons}/${it.lessons}", "${it.words}", it.tags.joinToString(",")) },
        ).ifEmpty { "No courses. `tayra packs list` shows the ready-made ones to download." })
    }
}

private class CoursesShow : Command("show", "Show a course and its lessons") {
    private val id by argument(help = "Course id")

    override fun run() = respond(cli, CourseDetailJson.serializer()) {
        val progress = cli.course(id)
        val json = CourseDetailJson(progress.toJson(), progress.lessons.mapIndexed { i, l -> l.toJson(i + 1) })
        Output(json, buildString {
            appendLine("${json.course.title} (${json.course.level}, ${json.course.completedLessons} of ${json.course.lessons} lessons done)")
            if (json.course.description.isNotBlank()) appendLine(json.course.description)
            if (json.course.tags.isNotEmpty()) appendLine("Tags: ${json.course.tags.joinToString(", ")}")
            appendLine()
            append(table(listOf("#", "LESSON ID", "TITLE", "WORDS", "STATUS"), json.lessonList.map { listOf("${it.number}", it.id, it.title, "${it.words}", it.status.replace('_', ' ')) }))
        }.trimEnd())
    }
}

private class CoursesCreate : Command("create", "Create a course in the language; prints its id") {
    private val title by option("--title", help = "The course's title").required()
    private val description by option("--description", help = "What the course is about").default("")
    private val level by option("--level", help = LEVEL_HELP).default("A1")
    private val topic by option("--topic", help = "Its topic, such as travel").default("")
    private val tags by option("--tags", help = "Tags, separated by commas").split(",").default(emptyList())

    override fun run() = respond(cli, CreatedJson.serializer(), changes = true) {
        val id = cli.get<CourseService>().createCourse(cli.language().id, CourseDraft(title, description, parseLevel(level), topic, tags))
        Output(CreatedJson(id), "Created course $id")
    }
}

private class CoursesEdit : Command("edit", "Change a course's title, description, level, topic or tags") {
    private val id by argument(help = "Course id")
    private val title by option("--title")
    private val description by option("--description")
    private val level by option("--level", help = LEVEL_HELP)
    private val topic by option("--topic")
    private val tags by option("--tags", help = "New tags, separated by commas; empty removes them").split(",")

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val course = cli.course(id).course
        if (course.builtIn) invalid("$id is a ready-made course; only your own courses can be changed")
        cli.get<CourseService>().updateCourse(id, CourseDraft(
            title = title ?: course.title,
            description = description ?: course.description,
            level = level?.let(::parseLevel) ?: course.level,
            topic = topic ?: course.topic,
            tags = tags?.filter { it.isNotBlank() } ?: course.tags,
        ))
        Output(DoneJson(message = "Saved course $id"), "Saved course $id")
    }
}

private class CoursesDelete : Command("delete", "Delete a course you made, with its lessons") {
    private val id by argument(help = "Course id")
    private val yes by option("--yes", help = "Confirm the deletion").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val course = cli.course(id).course
        if (!yes) invalid("Deleting \"${course.title}\" cannot be undone; repeat with --yes")
        cli.get<CourseService>().deleteCourse(id)
        Output(DoneJson(message = "Deleted course $id"), "Deleted \"${course.title}\"")
    }
}

private class LessonsAdd : Command("add", "Add a lesson at the end of a course; prints its id") {
    private val course by argument(help = "Course id")
    private val title by option("--title", help = "The lesson's title").required()
    private val summary by option("--summary", help = "A line about the lesson").default("")
    private val text by option("--text", help = "The lesson's text")
    private val file by option("--file", help = "A file with the text; - reads stdin")

    override fun run() = respond(cli, CreatedJson.serializer(), changes = true) {
        cli.course(course)
        val body = readInput(text, file) ?: invalid("Give the text with --text, or --file (- for stdin)")
        val id = cli.get<CourseService>().addLesson(course, LessonDraft(title, summary, body))
        Output(CreatedJson(id), "Added lesson $id")
    }
}

private class LessonsEdit : Command("edit", "Change a lesson's title, summary or text") {
    private val course by argument(help = "Course id")
    private val lesson by argument(help = "Lesson id")
    private val title by option("--title")
    private val summary by option("--summary")
    private val text by option("--text")
    private val file by option("--file", help = "A file with the new text; - reads stdin")

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val current = cli.course(course).course.lessons.firstOrNull { it.id == lesson } ?: notFound("Course $course has no lesson $lesson")
        cli.get<CourseService>().updateLesson(course, lesson, LessonDraft(title ?: current.title, summary ?: current.summary, readInput(text, file) ?: current.text))
        Output(DoneJson(message = "Saved lesson $lesson"), "Saved lesson $lesson")
    }
}

private class LessonsMove : Command("move", "Move a lesson up or down in its course") {
    private val course by argument(help = "Course id")
    private val lesson by argument(help = "Lesson id")
    private val by by option("--by", help = "Places to move: negative moves it earlier").int().required()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        cli.course(course).course.lessons.firstOrNull { it.id == lesson } ?: notFound("Course $course has no lesson $lesson")
        cli.get<CourseService>().moveLesson(course, lesson, by)
        Output(DoneJson(message = "Moved lesson $lesson"), "Moved lesson $lesson")
    }
}

private class LessonsDelete : Command("delete", "Delete a lesson from a course you made") {
    private val course by argument(help = "Course id")
    private val lesson by argument(help = "Lesson id")
    private val yes by option("--yes", help = "Confirm the deletion").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val found = cli.course(course).course.lessons.firstOrNull { it.id == lesson } ?: notFound("Course $course has no lesson $lesson")
        if (!yes) invalid("Deleting \"${found.title}\" cannot be undone; repeat with --yes")
        cli.get<CourseService>().deleteLesson(course, lesson)
        Output(DoneJson(message = "Deleted lesson $lesson"), "Deleted \"${found.title}\"")
    }
}

@Serializable
data class OpenedLessonJson(val course: String, val lesson: String, val bookId: Long)

private class LessonsOpen : Command("open", "Make a lesson readable and print the book id to use with `tayra read`") {
    private val course by argument(help = "Course id")
    private val lesson by argument(help = "Lesson id")

    override fun run() = respond(cli, OpenedLessonJson.serializer(), changes = true) {
        cli.course(course).course.lessons.firstOrNull { it.id == lesson } ?: notFound("Course $course has no lesson $lesson")
        val bookId = cli.get<CourseService>().openLesson(course, lesson) ?: notFound("Lesson $lesson could not be opened")
        Output(OpenedLessonJson(course, lesson, bookId), "Lesson $lesson is book $bookId: `tayra read page $bookId`")
    }
}

// ---- Course packs

fun packsCommand() = group(
    "packs", "The ready-made course packs to download, one per language",
    PacksList(), PacksDownload(), PacksRemove(),
)

@Serializable
data class PackJson(val id: String, val language: String, val title: String, val summary: String, val downloadBytes: Long, val state: String)

private suspend fun Cli.packs(): List<PackJson> {
    val service = get<CoursePackService>()
    service.refresh()
    return service.packs.value.map { status ->
        val state = when (val s = status.state) {
            PackState.NotInstalled -> "not-installed"
            is PackState.Installed -> "installed"
            is PackState.Downloading -> "downloading"
            is PackState.Failed -> "failed: ${s.message}"
        }
        PackJson(status.pack.id, status.pack.languageCode, status.pack.title, status.pack.summary, status.pack.downloadSize, state)
    }
}

private fun Cli.pack(name: String) = CoursePacks.all.firstOrNull { it.id.equals(name, true) || it.languageCode.equals(name, true) || LanguageCodes.option(it.languageCode)?.name.equals(name, true) }
    ?: notFound("No course pack '$name'; `tayra packs list` shows them")

private class PacksList : Command("list", "List the course packs and whether each is installed") {
    override fun run() = respond(cli, ListSerializer(PackJson.serializer())) {
        val packs = cli.packs()
        Output(packs, table(listOf("ID", "LANGUAGE", "TITLE", "SIZE", "STATE"), packs.map { listOf(it.id, it.language, it.title, "${it.downloadBytes / 1_000_000} MB", it.state) }))
    }
}

private class PacksDownload : Command("download", "Download a course pack and add its courses") {
    private val name by argument(help = "Pack id or language code")

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val pack = cli.pack(name)
        val service = cli.get<CoursePackService>()
        service.refresh()
        echo("Downloading ${pack.title} (${pack.downloadSize / 1_000_000} MB)…", err = true)
        service.download(pack)
        (service.packs.value.first { it.pack.id == pack.id }.state as? PackState.Failed)?.let { throw CliFailure("Download failed: ${it.message}", ExitCode.ERROR) }
        Output(DoneJson(message = "Installed ${pack.title}"), "Installed ${pack.title}")
    }
}

private class PacksRemove : Command("remove", "Remove a course pack and its courses") {
    private val name by argument(help = "Pack id or language code")
    private val yes by option("--yes", help = "Confirm the removal").flag()

    override fun run() = respond(cli, DoneJson.serializer(), changes = true) {
        val pack = cli.pack(name)
        if (!yes) invalid("Removing ${pack.title} also removes its courses; repeat with --yes")
        val service = cli.get<CoursePackService>()
        service.refresh()
        service.remove(pack)
        Output(DoneJson(message = "Removed ${pack.title}"), "Removed ${pack.title}")
    }
}
