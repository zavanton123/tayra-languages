package com.idiomiko.core.data

import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.courses.SampleCourseSource

/** Three short Portuguese sample courses, A1 to B1, for the tests. */
object TestCourses {
    val ALL = listOf(
        course("pt-test-a1", "Primeiros passos", CourseLevel.A1, "Olá! Eu sou a Ana.", "Minha família é pequena.", "Minha casa tem dois quartos.", "Meu dia começa cedo.", "Um café, por favor."),
        course("pt-test-a2", "A vida na cidade", CourseLevel.A2, "No supermercado, tudo está caro.", "O ônibus chegou atrasado."),
        course("pt-test-b1", "Histórias curtas", CourseLevel.B1, "A carta chegou sem remetente.", "Alguém levou o meu guarda-chuva."),
    )

    val source = SampleCourseSource { code -> ALL.filter { it.languageCode == code } }

    private fun course(id: String, title: String, level: CourseLevel, vararg texts: String) = Course(
        id, "pt", title, "", level, "",
        texts.mapIndexed { i, text -> Lesson("$id-${i + 1}", "Lição ${i + 1}", "Lesson ${i + 1}", text) },
    )
}
